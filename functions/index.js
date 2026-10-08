// functions/index.js

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

/**
 * CHOSEN ENTRANTS
 *  - If only eventId: notify all docs in events/{eventId}/selected
 *  - If eventId + entrantId: only that doc
 */
exports.sendChosenNotifications = onCall(async (request) => {
  const data = request.data || {};
  const eventId = data.eventId;
  const singleEntrantId = data.entrantId || null;

  if (!eventId || typeof eventId !== "string") {
    throw new HttpsError("invalid-argument", "eventId is required");
  }

  const db = getFirestore();
  const eventRef = db.collection("events").doc(eventId);

  const eventSnap = await eventRef.get();
  if (!eventSnap.exists) {
    throw new HttpsError("not-found", "Event not found");
  }
  const event = eventSnap.data() || {};

  const eventName = event.name || "Your event";
  let eventDateText = "";
  if (event.eventDate && typeof event.eventDate.toDate === "function") {
    const d = event.eventDate.toDate();
    eventDateText = d.toLocaleDateString("en-CA");
  }

  const title = `Update: ${eventName}`;
  const body = eventDateText
    ? `You are selected for ${eventName} on ${eventDateText}. Please open the app and confirm your participation.`
    : `You are selected for ${eventName}. Please open the app and confirm your participation.`;

  let selectedDocs = [];
  if (singleEntrantId) {
    const docSnap = await eventRef
      .collection("selected")
      .doc(singleEntrantId)
      .get();
    if (docSnap.exists) {
      selectedDocs = [docSnap];
    }
  } else {
    const selectedSnap = await eventRef.collection("selected").get();
    selectedDocs = selectedSnap.docs;
  }

  if (!selectedDocs.length) {
    return { sentCount: 0, failureCount: 0 };
  }

  const messaging = getMessaging();
  const batch = db.batch();

  let sentCount = 0;
  let failureCount = 0;

  const tokens = [];
  const docsToMarkNotified = [];

  for (const doc of selectedDocs) {
    const selData = doc.data() || {};
    const status = selData.status || "pending";
    const userId = selData.userId || doc.id;

    if (status !== "pending" && status !== "selected") continue;

    docsToMarkNotified.push(doc.ref);

    if (!userId) {
      sentCount++;
      continue;
    }

    const userSnap = await db.collection("users").doc(userId).get();
    const user = userSnap.exists ? (userSnap.data() || {}) : {};

    const prefs = user.notificationPreferences || {};
    if (prefs.lotteryResults === false && prefs.organizerUpdates === false) {
      continue;
    }

    const token = user.fcmToken;
    if (!token) {
      sentCount++;
      continue;
    }

    tokens.push(token);
  }

  if (tokens.length > 0) {
    const message = {
      notification: { title, body },
      tokens,
      data: {
        eventId,
        eventName,
        eventDate: eventDateText,
        confirmationInstructions:
          "Please open the app and confirm your participation.",
      },
    };

    const resp = await messaging.sendEachForMulticast(message);
    sentCount += resp.successCount;
    failureCount += resp.failureCount;
  }

  docsToMarkNotified.forEach((ref) => {
    batch.update(ref, {
      status: "notified",
      notifiedAt: FieldValue.serverTimestamp(),
      eventName,
      eventDate: eventDateText,
      confirmationInstructions:
        "Please open the app and confirm your participation.",
    });
  });

  await batch.commit();

  return { sentCount, failureCount };
});

/**
 * WAITING LIST – notify all docs in events/{eventId}/waitingList
 */
exports.sendWaitingListNotifications = onCall(async (request) => {
  const data = request.data || {};
  const eventId = data.eventId;
  const customMessage = (data.message || "").toString().trim();

  if (!eventId || typeof eventId !== "string") {
    throw new HttpsError("invalid-argument", "eventId is required");
  }

  const db = getFirestore();
  const eventRef = db.collection("events").doc(eventId);

  const eventSnap = await eventRef.get();
  if (!eventSnap.exists) {
    throw new HttpsError("not-found", "Event not found");
  }
  const event = eventSnap.data() || {};
  const eventName = event.name || "Your event";

  const title = `Update about ${eventName}`;
  const body =
    customMessage ||
    `There is an update regarding the waiting list for ${eventName}. Please open the app for details.`;

  const waitingSnap = await eventRef.collection("waitingList").get();
  if (waitingSnap.empty) {
    return { sentCount: 0, failureCount: 0 };
  }

  const messaging = getMessaging();
  const tokens = [];

  let sentCount = 0;
  let failureCount = 0;

  for (const doc of waitingSnap.docs) {
    const userId = doc.id;
    const userSnap = await db.collection("users").doc(userId).get();
    const user = userSnap.exists ? (userSnap.data() || {}) : {};

    const prefs = user.notificationPreferences || {};
    if (prefs.organizerUpdates === false) {
      continue;
    }

    const token = user.fcmToken;
    if (!token) {
      sentCount++;
      continue;
    }

    tokens.push(token);
  }

  if (tokens.length > 0) {
    const message = {
      notification: { title, body },
      tokens,
      data: {
        eventId,
        eventName,
        notificationType: "waitingList",
      },
    };

    const resp = await messaging.sendEachForMulticast(message);
    sentCount += resp.successCount;
    failureCount += resp.failureCount;
  }

  const logRef = eventRef.collection("notificationLogs").doc();
  await logRef.set({
    type: "waitingList",
    message: body,
    eventId,
    sentCount,
    failureCount,
    createdAt: FieldValue.serverTimestamp(),
  });

  return { sentCount, failureCount };
});

/**
 * CANCELLED – notify all docs in events/{eventId}/cancelled
 */
exports.sendCancelledNotifications = onCall(async (request) => {
  const data = request.data || {};
  const eventId = data.eventId;
  const customMessage = (data.message || "").toString().trim();

  if (!eventId || typeof eventId !== "string") {
    throw new HttpsError("invalid-argument", "eventId is required");
  }

  const db = getFirestore();
  const eventRef = db.collection("events").doc(eventId);

  const eventSnap = await eventRef.get();
  if (!eventSnap.exists) {
    throw new HttpsError("not-found", "Event not found");
  }
  const event = eventSnap.data() || {};
  const eventName = event.name || "Your event";

  const title = `Update about ${eventName}`;
  const body =
    customMessage ||
    `There is an update regarding your cancelled entry for ${eventName}. Please open the app for details.`;

  const cancelledSnap = await eventRef.collection("cancelled").get();
  if (cancelledSnap.empty) {
    return { sentCount: 0, failureCount: 0 };
  }

  const messaging = getMessaging();
  const tokens = [];

  let sentCount = 0;
  let failureCount = 0;

  for (const doc of cancelledSnap.docs) {
    const userId = doc.id;
    const userSnap = await db.collection("users").doc(userId).get();
    const user = userSnap.exists ? (userSnap.data() || {}) : {};

    const prefs = user.notificationPreferences || {};
    if (prefs.organizerUpdates === false) {
      continue;
    }

    const token = user.fcmToken;
    if (!token) {
      sentCount++;
      continue;
    }

    tokens.push(token);
  }

  if (tokens.length > 0) {
    const message = {
      notification: { title, body },
      tokens,
      data: {
        eventId,
        eventName,
        notificationType: "cancelled",
      },
    };

    const resp = await messaging.sendEachForMulticast(message);
    sentCount += resp.successCount;
    failureCount += resp.failureCount;
  }

  const logRef = eventRef.collection("notificationLogs").doc();
  await logRef.set({
    type: "cancelled",
    message: body,
    eventId,
    sentCount,
    failureCount,
    createdAt: FieldValue.serverTimestamp(),
  });

  return { sentCount, failureCount };
});
