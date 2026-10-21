"use strict";

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { FieldValue, GeoPoint, getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { getStorage } = require("firebase-admin/storage");
const {
  availableEventSlots,
  canCancelSelection,
  canDrawReplacement,
  canMarkNotified,
  canRespondToSelection,
  ownedPosterPath,
} = require("./workflow");

initializeApp();

const MAX_NOTIFICATION_MESSAGE_LENGTH = 1_000;
const MAX_SAMPLE_SIZE = 100;
const FCM_BATCH_SIZE = 500;

function requireString(value, fieldName) {
  if (typeof value !== "string" || !value.trim()) {
    throw new HttpsError("invalid-argument", `${fieldName} is required.`);
  }
  return value.trim();
}

function sanitizeMessage(value) {
  if (value == null) return "";
  const message = String(value).trim();
  if (message.length > MAX_NOTIFICATION_MESSAGE_LENGTH) {
    throw new HttpsError(
      "invalid-argument",
      `message must be ${MAX_NOTIFICATION_MESSAGE_LENGTH} characters or fewer.`,
    );
  }
  return message;
}

function timestampToDate(value) {
  return value && typeof value.toDate === "function" ? value.toDate() : null;
}

function eventDateText(event) {
  const date = timestampToDate(event.eventDate);
  return date ? date.toLocaleDateString("en-CA") : "";
}

function isEventManager(event, userId) {
  return event.organizerId === userId || event.createdById === userId;
}

async function requireAuthenticatedUser(request) {
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError("unauthenticated", "Sign in before performing this action.");
  }

  const db = getFirestore();
  const userRef = db.collection("users").doc(request.auth.uid);
  const userSnap = await userRef.get();
  if (!userSnap.exists) {
    throw new HttpsError("permission-denied", "Your Ticketjack profile was not found.");
  }

  const user = userSnap.data() || {};
  if (user.isActive === false || user.roleActive === false) {
    throw new HttpsError("permission-denied", "Your account is not active.");
  }

  return { db, user, userId: request.auth.uid, userRef };
}

async function requireEntrant(request) {
  const context = await requireAuthenticatedUser(request);
  if (context.user.role !== "entrant") {
    throw new HttpsError("permission-denied", "Only entrants can manage waitlist entries.");
  }
  return context;
}

async function requireAdministrator(request) {
  const context = await requireAuthenticatedUser(request);
  if (context.user.role !== "admin") {
    throw new HttpsError("permission-denied", "Administrator access is required.");
  }
  return context;
}

async function requireEventManager(request, eventId) {
  const context = await requireAuthenticatedUser(request);
  const eventRef = context.db.collection("events").doc(eventId);
  const eventSnap = await eventRef.get();
  if (!eventSnap.exists) {
    throw new HttpsError("not-found", "Event not found.");
  }

  const event = eventSnap.data() || {};
  const isAdmin = context.user.role === "admin";
  const isOrganizer = context.user.role === "organizer" &&
    isEventManager(event, context.userId);

  if (!isAdmin && !isOrganizer) {
    throw new HttpsError(
      "permission-denied",
      "Only the event organizer or an administrator can manage this event.",
    );
  }

  return { ...context, event, eventRef };
}

function assertRegistrationOpen(event) {
  const now = new Date();
  const startsAt = timestampToDate(event.registrationStart);
  const endsAt = timestampToDate(event.registrationEnd);
  if ((startsAt && startsAt > now) || (endsAt && endsAt <= now)) {
    throw new HttpsError(
      "failed-precondition",
      "Registration is not currently open for this event.",
    );
  }
  if (event.isActive === false) {
    throw new HttpsError("failed-precondition", "This event is no longer active.");
  }
}

function shuffle(values) {
  const result = [...values];
  for (let index = result.length - 1; index > 0; index -= 1) {
    const nextIndex = Math.floor(Math.random() * (index + 1));
    [result[index], result[nextIndex]] = [result[nextIndex], result[index]];
  }
  return result;
}

async function sendToTokens(tokens, message) {
  let sentCount = 0;
  let failureCount = 0;
  const messaging = getMessaging();

  for (let start = 0; start < tokens.length; start += FCM_BATCH_SIZE) {
    const chunk = tokens.slice(start, start + FCM_BATCH_SIZE);
    const response = await messaging.sendEachForMulticast({ ...message, tokens: chunk });
    sentCount += response.successCount;
    failureCount += response.failureCount;
  }

  return { failureCount, sentCount };
}

async function collectTokens(db, recipientDocs, preferenceKey) {
  const users = await Promise.all(
    recipientDocs.map(async (recipient) => {
      const data = recipient.data() || {};
      const userId = data.userId || recipient.id;
      if (!userId) return null;
      const userSnap = await db.collection("users").doc(userId).get();
      return userSnap.exists ? userSnap.data() || {} : null;
    }),
  );

  const tokens = [];
  let skippedCount = 0;
  for (const user of users) {
    if (!user || user.notificationPreferences?.[preferenceKey] === false || !user.fcmToken) {
      skippedCount += 1;
      continue;
    }
    tokens.push(user.fcmToken);
  }
  return { skippedCount, tokens };
}

async function writeNotificationLog(db, eventRef, details) {
  const log = {
    ...details,
    createdAt: FieldValue.serverTimestamp(),
  };
  const batch = db.batch();
  batch.set(eventRef.collection("notificationLogs").doc(), log);
  batch.set(db.collection("notificationLogs").doc(), log);
  await batch.commit();
}

async function selectEntrantsInTransaction(transaction, db, eventRef, event, count) {
  const waitingSnap = await transaction.get(eventRef.collection("waitingList"));
  if (waitingSnap.empty) return [];

  const selectedIds = shuffle(waitingSnap.docs.map((document) => document.id))
    .slice(0, Math.min(count, waitingSnap.size));
  const eventDate = event.eventDate || null;

  for (const userId of selectedIds) {
    const selectedRef = eventRef.collection("selected").doc(userId);
    const historyRef = db.collection("users").doc(userId).collection("eventHistory").doc(eventRef.id);
    const waitingRef = eventRef.collection("waitingList").doc(userId);

    transaction.set(selectedRef, {
      status: "pending",
      sampledAt: FieldValue.serverTimestamp(),
    }, { merge: true });
    transaction.set(historyRef, {
      eventName: event.name || "Untitled event",
      eventDate,
      status: "Selected",
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
    transaction.delete(waitingRef);
  }
  return selectedIds;
}

exports.joinWaitingList = onCall(async (request) => {
  const eventId = requireString(request.data?.eventId, "eventId");
  const { db, userId } = await requireEntrant(request);
  const latitude = request.data?.latitude;
  const longitude = request.data?.longitude;

  if ((latitude != null || longitude != null) &&
      (!Number.isFinite(latitude) || !Number.isFinite(longitude) ||
       latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)) {
    throw new HttpsError("invalid-argument", "A valid latitude and longitude are required.");
  }

  const eventRef = db.collection("events").doc(eventId);
  await db.runTransaction(async (transaction) => {
    const [eventSnap, waitingSnap, historySnap, waitlistSnap] = await Promise.all([
      transaction.get(eventRef),
      transaction.get(eventRef.collection("waitingList").doc(userId)),
      transaction.get(db.collection("users").doc(userId).collection("eventHistory").doc(eventId)),
      transaction.get(eventRef.collection("waitingList")),
    ]);

    if (!eventSnap.exists) throw new HttpsError("not-found", "Event not found.");
    const event = eventSnap.data() || {};
    assertRegistrationOpen(event);
    if (waitingSnap.exists || historySnap.exists) {
      throw new HttpsError("already-exists", "You already have an entry for this event.");
    }
    if (event.waitingListLimit > 0 && waitlistSnap.size >= event.waitingListLimit) {
      throw new HttpsError("resource-exhausted", "The waiting list is full.");
    }
    if (event.geolocationRequired && (latitude == null || longitude == null)) {
      throw new HttpsError("failed-precondition", "This event requires your location.");
    }

    const waitingData = { joinedAt: FieldValue.serverTimestamp() };
    if (latitude != null && longitude != null) {
      waitingData.location = new GeoPoint(latitude, longitude);
    }

    transaction.set(eventRef.collection("waitingList").doc(userId), waitingData);
    transaction.set(
      db.collection("users").doc(userId).collection("eventHistory").doc(eventId),
      {
        eventName: event.name || "Untitled event",
        eventDate: event.eventDate || null,
        status: "Waiting",
        updatedAt: FieldValue.serverTimestamp(),
      },
    );
  });

  return { eventId, status: "Waiting" };
});

exports.leaveWaitingList = onCall(async (request) => {
  const eventId = requireString(request.data?.eventId, "eventId");
  const { db, userId } = await requireEntrant(request);
  const eventRef = db.collection("events").doc(eventId);

  await db.runTransaction(async (transaction) => {
    const waitingRef = eventRef.collection("waitingList").doc(userId);
    const historyRef = db.collection("users").doc(userId).collection("eventHistory").doc(eventId);
    const [eventSnap, waitingSnap, historySnap] = await Promise.all([
      transaction.get(eventRef),
      transaction.get(waitingRef),
      transaction.get(historyRef),
    ]);

    if (!eventSnap.exists) throw new HttpsError("not-found", "Event not found.");
    if (!waitingSnap.exists || !historySnap.exists ||
        historySnap.data()?.status !== "Waiting") {
      throw new HttpsError("failed-precondition", "There is no waiting-list entry to remove.");
    }

    transaction.delete(waitingRef);
    transaction.delete(historyRef);
  });

  return { eventId, status: "removed" };
});

exports.respondToInvitation = onCall(async (request) => {
  const eventId = requireString(request.data?.eventId, "eventId");
  const accepted = request.data?.accepted;
  if (typeof accepted !== "boolean") {
    throw new HttpsError("invalid-argument", "accepted must be a boolean.");
  }

  const { db, userId } = await requireEntrant(request);
  const eventRef = db.collection("events").doc(eventId);
  let replacementUserId = null;

  await db.runTransaction(async (transaction) => {
    const selectedRef = eventRef.collection("selected").doc(userId);
    const historyRef = db.collection("users").doc(userId).collection("eventHistory").doc(eventId);
    const [eventSnap, selectedSnap, historySnap, waitingSnap] = await Promise.all([
      transaction.get(eventRef),
      transaction.get(selectedRef),
      transaction.get(historyRef),
      transaction.get(eventRef.collection("waitingList")),
    ]);

    if (!eventSnap.exists) throw new HttpsError("not-found", "Event not found.");
    if (!selectedSnap.exists || !historySnap.exists ||
        historySnap.data()?.status !== "Selected" ||
        !canRespondToSelection(selectedSnap.data())) {
      throw new HttpsError("failed-precondition", "There is no pending invitation to respond to.");
    }

    const event = eventSnap.data() || {};
    transaction.update(selectedRef, {
      status: accepted ? "accepted" : "declined",
      respondedAt: FieldValue.serverTimestamp(),
    });
    transaction.set(historyRef, {
      status: accepted ? "Confirmed" : "Declined",
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });

    if (accepted) {
      transaction.set(eventRef.collection("confirmedAttendees").doc(userId), {
        confirmedAt: FieldValue.serverTimestamp(),
      });
      return;
    }

    const candidates = shuffle(waitingSnap.docs.map((document) => document.id));
    replacementUserId = candidates[0] || null;
    if (replacementUserId) {
      const replacementHistoryRef = db.collection("users").doc(replacementUserId)
        .collection("eventHistory").doc(eventId);
      transaction.set(eventRef.collection("selected").doc(replacementUserId), {
        status: "pending",
        sampledAt: FieldValue.serverTimestamp(),
        replacementFor: userId,
      }, { merge: true });
      transaction.set(replacementHistoryRef, {
        eventName: event.name || "Untitled event",
        eventDate: event.eventDate || null,
        status: "Selected",
        updatedAt: FieldValue.serverTimestamp(),
      }, { merge: true });
      transaction.delete(eventRef.collection("waitingList").doc(replacementUserId));
    }
  });

  return {
    replacementUserId,
    status: accepted ? "Confirmed" : "Declined",
  };
});

exports.sampleAttendees = onCall(async (request) => {
  const eventId = requireString(request.data?.eventId, "eventId");
  const count = Number(request.data?.count);
  if (!Number.isInteger(count) || count < 1 || count > MAX_SAMPLE_SIZE) {
    throw new HttpsError(
      "invalid-argument",
      `count must be an integer between 1 and ${MAX_SAMPLE_SIZE}.`,
    );
  }

  const { db, event, eventRef } = await requireEventManager(request, eventId);
  const selectedIds = await db.runTransaction(async (transaction) => {
    const [selectedSnap, confirmedSnap] = await Promise.all([
      transaction.get(eventRef.collection("selected")),
      transaction.get(eventRef.collection("confirmedAttendees")),
    ]);
    const availableSlots = availableEventSlots(
      event,
      selectedSnap.docs.map((document) => ({ id: document.id, ...document.data() })),
      confirmedSnap.docs.map((document) => document.id),
    );
    if (availableSlots < 1) {
      throw new HttpsError("failed-precondition", "The event has reached capacity.");
    }
    return selectEntrantsInTransaction(
      transaction,
      db,
      eventRef,
      event,
      Math.min(count, availableSlots),
    );
  });

  if (!selectedIds.length) {
    throw new HttpsError("failed-precondition", "No entrants are available to sample.");
  }
  return { selectedCount: selectedIds.length, selectedIds };
});

exports.drawReplacement = onCall(async (request) => {
  const eventId = requireString(request.data?.eventId, "eventId");
  const cancelledEntrantId = requireString(request.data?.cancelledEntrantId, "cancelledEntrantId");
  const { db, event, eventRef } = await requireEventManager(request, eventId);

  const selectedIds = await db.runTransaction(async (transaction) => {
    const cancelledRef = eventRef.collection("selected").doc(cancelledEntrantId);
    const cancelledSnap = await transaction.get(cancelledRef);
    if (!cancelledSnap.exists || !canDrawReplacement(cancelledSnap.data())) {
      throw new HttpsError("failed-precondition", "That cancelled entry cannot receive a replacement.");
    }
    const chosen = await selectEntrantsInTransaction(transaction, db, eventRef, event, 1);
    if (!chosen.length) {
      throw new HttpsError("failed-precondition", "No entrants are available to draw as a replacement.");
    }
    transaction.update(cancelledRef, {
      replacementDrawn: true,
      replacementDrawnAt: FieldValue.serverTimestamp(),
      replacementEntrantId: chosen[0],
    });
    return chosen;
  });

  return { replacementUserId: selectedIds[0] };
});

async function runInChunks(items, chunkSize, handler) {
  const results = [];
  for (let start = 0; start < items.length; start += chunkSize) {
    results.push(...await Promise.all(items.slice(start, start + chunkSize).map(handler)));
  }
  return results;
}

async function cancelSelection(db, eventRef, eventId, selectedDocument) {
  return db.runTransaction(async (transaction) => {
    const current = await transaction.get(selectedDocument.ref);
    if (!current.exists || !canCancelSelection(current.data())) return false;

    const userId = current.id;
    transaction.set(current.ref, {
      status: "cancelled",
      cancelledAt: FieldValue.serverTimestamp(),
    }, { merge: true });
    transaction.set(eventRef.collection("cancelled").doc(userId), {
      cancelledAt: FieldValue.serverTimestamp(),
      reason: "organizer_timeout",
    }, { merge: true });
    transaction.set(db.collection("users").doc(userId).collection("eventHistory").doc(eventId), {
      status: "Cancelled",
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
    return true;
  });
}

async function markSelectionNotified(db, selectedDocument) {
  return db.runTransaction(async (transaction) => {
    const current = await transaction.get(selectedDocument.ref);
    if (!current.exists || !canMarkNotified(current.data())) return false;
    transaction.set(current.ref, {
      status: "notified",
      notifiedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
    return true;
  });
}

exports.cancelPendingEntrants = onCall(async (request) => {
  const eventId = requireString(request.data?.eventId, "eventId");
  const { db, eventRef } = await requireEventManager(request, eventId);
  const selectedSnap = await eventRef.collection("selected").get();
  const pendingDocs = selectedSnap.docs.filter((document) =>
    canCancelSelection(document.data()));
  const outcomes = await runInChunks(
    pendingDocs,
    20,
    (document) => cancelSelection(db, eventRef, eventId, document),
  );
  return { cancelledCount: outcomes.filter(Boolean).length };
});

exports.sendChosenNotifications = onCall(async (request) => {
  const eventId = requireString(request.data?.eventId, "eventId");
  const entrantId = request.data?.entrantId ? requireString(request.data.entrantId, "entrantId") : null;
  const { db, event, eventRef, userId } = await requireEventManager(request, eventId);
  const selectedSnap = entrantId
    ? await eventRef.collection("selected").doc(entrantId).get()
    : await eventRef.collection("selected").get();
  const selectedDocs = entrantId
    ? (selectedSnap.exists ? [selectedSnap] : [])
    : selectedSnap.docs;
  const eligibleDocs = selectedDocs.filter((document) =>
    canMarkNotified(document.data()));

  const name = event.name || "your event";
  const date = eventDateText(event);
  const { tokens, skippedCount } = await collectTokens(db, eligibleDocs, "lotteryResults");
  const delivery = await sendToTokens(tokens, {
    notification: {
      title: `You were selected for ${name}`,
      body: date
        ? `Open Ticketjack to confirm your place on ${date}.`
        : "Open Ticketjack to confirm your place.",
    },
    data: {
      eventId,
      eventName: name,
      eventDate: date,
      notificationType: "selection",
    },
  });

  const notificationOutcomes = await runInChunks(
    eligibleDocs,
    20,
    (document) => markSelectionNotified(db, document),
  );

  const result = {
    eventId,
    type: "selection",
    initiatorId: userId,
    recipientCount: eligibleDocs.length,
    notifiedCount: notificationOutcomes.filter(Boolean).length,
    skippedCount,
    ...delivery,
  };
  await writeNotificationLog(db, eventRef, result);
  return result;
});

async function sendAudienceNotification(request, collectionName, type, preferenceKey, fallbackMessage) {
  const eventId = requireString(request.data?.eventId, "eventId");
  const customMessage = sanitizeMessage(request.data?.message);
  const { db, event, eventRef, userId } = await requireEventManager(request, eventId);
  const recipients = await eventRef.collection(collectionName).get();
  const recipientDocs = recipients.docs;
  const name = event.name || "your event";
  const body = customMessage || fallbackMessage(name);
  const { tokens, skippedCount } = await collectTokens(db, recipientDocs, preferenceKey);
  const delivery = await sendToTokens(tokens, {
    notification: { title: `Update about ${name}`, body },
    data: { eventId, eventName: name, notificationType: type },
  });
  const result = {
    eventId,
    type,
    initiatorId: userId,
    message: body,
    recipientCount: recipientDocs.length,
    skippedCount,
    ...delivery,
  };
  await writeNotificationLog(db, eventRef, result);
  return result;
}

exports.sendWaitingListNotifications = onCall((request) =>
  sendAudienceNotification(
    request,
    "waitingList",
    "waitingList",
    "organizerUpdates",
    (name) => `There is an update about the waiting list for ${name}. Open Ticketjack for details.`,
  ));

exports.sendCancelledNotifications = onCall((request) =>
  sendAudienceNotification(
    request,
    "cancelled",
    "cancelled",
    "organizerUpdates",
    (name) => `There is an update about your cancelled entry for ${name}. Open Ticketjack for details.`,
  ));

async function deleteOwnedPoster(event, eventId) {
  const imageUrl = event.posterImageUrl || event.imageUrl || event.bannerUrl || event.thumbnailUrl;
  const objectPath = ownedPosterPath(imageUrl, eventId);
  if (!objectPath) return false;
  await getStorage().bucket().file(objectPath).delete({ ignoreNotFound: true });
  return true;
}

exports.deleteEvent = onCall(async (request) => {
  const eventId = requireString(request.data?.eventId, "eventId");
  const { db, event, eventRef, userId } = await requireEventManager(request, eventId);
  await deleteOwnedPoster(event, eventId);
  await db.recursiveDelete(eventRef);
  await db.collection("eventRemovalLogs").add({
    eventId,
    eventName: event.name || "Untitled event",
    removedByUserId: userId,
    removedAt: FieldValue.serverTimestamp(),
  });
  return { eventId, deleted: true };
});

exports.deactivateOrganizer = onCall(async (request) => {
  const organizerId = requireString(request.data?.organizerId, "organizerId");
  const reason = sanitizeMessage(request.data?.reason) || "Policy violation";
  const { db, userId: adminId } = await requireAdministrator(request);
  const organizerRef = db.collection("users").doc(organizerId);
  const [organizerSnap, organizerEventsSnap, legacyEventsSnap] = await Promise.all([
    organizerRef.get(),
    db.collection("events").where("organizerId", "==", organizerId).get(),
    db.collection("events").where("createdById", "==", organizerId).get(),
  ]);

  if (!organizerSnap.exists || organizerSnap.data()?.role !== "organizer") {
    throw new HttpsError("not-found", "Organizer not found.");
  }

  const eventUpdates = [...new Map(
    [...organizerEventsSnap.docs, ...legacyEventsSnap.docs]
      .map((document) => [document.ref.path, document]),
  ).values()];
  for (let start = 0; start < eventUpdates.length || start === 0; start += 450) {
    const batch = db.batch();
    if (start === 0) {
      batch.set(organizerRef, {
        isActive: false,
        roleActive: false,
        deactivatedAt: FieldValue.serverTimestamp(),
        deactivatedByAdminId: adminId,
        deactivationReason: reason,
      }, { merge: true });
      batch.set(db.collection("organizerDeactivationLogs").doc(), {
        organizerId,
        adminId,
        reason,
        affectedEventCount: eventUpdates.length,
        timestamp: FieldValue.serverTimestamp(),
      });
    }
    eventUpdates.slice(start, start + 450).forEach((document) => batch.set(document.ref, {
      isActive: false,
      deactivatedAt: FieldValue.serverTimestamp(),
      deactivatedByAdminId: adminId,
      deactivationReason: reason,
    }, { merge: true }));
    await batch.commit();
    if (eventUpdates.length === 0) break;
  }

  return { affectedEventCount: eventUpdates.length, organizerId };
});

async function removeUserData(db, userId) {
  const eventsSnap = await db.collection("events").get();
  const writer = db.bulkWriter();
  const participantCollections = [
    "waitingList",
    "selected",
    "confirmedAttendees",
    "cancelled",
  ];
  eventsSnap.docs.forEach((eventDocument) => {
    participantCollections.forEach((collectionName) => {
      writer.delete(eventDocument.ref.collection(collectionName).doc(userId));
    });
  });
  await writer.close();
  await db.recursiveDelete(db.collection("users").doc(userId));
}

async function deleteAuthUser(userId) {
  try {
    await getAuth().deleteUser(userId);
  } catch (error) {
    if (error.code !== "auth/user-not-found") throw error;
  }
}

async function deactivateOwnedEvents(db, organizerId) {
  const [organizerEventsSnap, legacyEventsSnap] = await Promise.all([
    db.collection("events").where("organizerId", "==", organizerId).get(),
    db.collection("events").where("createdById", "==", organizerId).get(),
  ]);
  const events = [...new Map(
    [...organizerEventsSnap.docs, ...legacyEventsSnap.docs]
      .map((document) => [document.ref.path, document]),
  ).values()];
  const writer = db.bulkWriter();
  events.forEach((document) => writer.set(document.ref, {
    isActive: false,
    deactivatedAt: FieldValue.serverTimestamp(),
    deactivationReason: "Organizer account deleted",
  }, { merge: true }));
  await writer.close();
  return events.length;
}

exports.deleteUserProfile = onCall(async (request) => {
  const userId = requireString(request.data?.userId, "userId");
  const { db, userId: adminId } = await requireAdministrator(request);
  if (userId === adminId) {
    throw new HttpsError("failed-precondition", "Administrators cannot remove their own profile here.");
  }
  const targetRef = db.collection("users").doc(userId);
  const targetSnap = await targetRef.get();
  if (!targetSnap.exists) {
    throw new HttpsError("not-found", "User profile not found.");
  }
  if (targetSnap.data()?.role === "organizer") {
    throw new HttpsError(
      "failed-precondition",
      "Deactivate organizer accounts through organizer management.",
    );
  }

  await removeUserData(db, userId);
  await deleteAuthUser(userId);
  await db.collection("adminRemovalLogs").add({
    removedUserId: userId,
    removedByAdminId: adminId,
    timestamp: FieldValue.serverTimestamp(),
  });
  return { deleted: true, userId };
});

exports.deleteOwnAccount = onCall(async (request) => {
  const { db, user, userId } = await requireAuthenticatedUser(request);
  let deactivatedEventCount = 0;
  if (user.role === "organizer") {
    deactivatedEventCount = await deactivateOwnedEvents(db, userId);
  }
  await removeUserData(db, userId);
  await deleteAuthUser(userId);
  return { deactivatedEventCount, deleted: true, userId };
});

exports.removeEventPoster = onCall(async (request) => {
  const eventId = requireString(request.data?.eventId, "eventId");
  const imageUrl = requireString(request.data?.imageUrl, "imageUrl");
  const { db, userId } = await requireAdministrator(request);
  const eventRef = db.collection("events").doc(eventId);
  const eventSnap = await eventRef.get();
  if (!eventSnap.exists) {
    throw new HttpsError("not-found", "Event not found.");
  }

  const objectPath = ownedPosterPath(imageUrl, eventId);
  if (!objectPath) {
    throw new HttpsError(
      "invalid-argument",
      "The image must be a poster owned by the selected event.",
    );
  }

  await getStorage().bucket().file(objectPath).delete({ ignoreNotFound: true });
  await eventRef.set({
    posterImageUrl: null,
    imageUrl: null,
    bannerUrl: null,
    thumbnailUrl: null,
    posterRemovedAt: FieldValue.serverTimestamp(),
    posterRemovedByAdminId: userId,
  }, { merge: true });
  await db.collection("imageRemovalLogs").add({
    eventId,
    imageUrl,
    removedByUserId: userId,
    removedAt: FieldValue.serverTimestamp(),
  });
  return { eventId, removed: true };
});
