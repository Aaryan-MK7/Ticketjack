"use strict";

const SELECTABLE_STATUSES = new Set(["pending", "selected", "notified"]);
const OCCUPIED_STATUSES = new Set(["pending", "selected", "notified", "accepted"]);

function canRespondToSelection(selection) {
  return Boolean(selection && SELECTABLE_STATUSES.has(selection.status));
}

function canCancelSelection(selection) {
  return Boolean(selection && SELECTABLE_STATUSES.has(selection.status));
}

function canMarkNotified(selection) {
  return Boolean(selection && ["pending", "selected"].includes(selection.status));
}

function canDrawReplacement(selection) {
  return Boolean(
    selection
    && selection.status === "cancelled"
    && selection.replacementDrawn !== true
  );
}

function availableEventSlots(event, selectedEntries = [], confirmedIds = []) {
  const capacity = Number(event?.capacity);
  if (!Number.isInteger(capacity) || capacity < 1) return 0;

  const occupied = new Set(confirmedIds.filter(Boolean));
  selectedEntries.forEach((entry) => {
    if (entry?.id && OCCUPIED_STATUSES.has(entry.status)) occupied.add(entry.id);
  });
  return Math.max(0, capacity - occupied.size);
}

function ownedPosterPath(imageUrl, eventId) {
  if (typeof imageUrl !== "string" || !imageUrl.trim() || !eventId) return null;
  try {
    const decodedPath = decodeURIComponent(new URL(imageUrl).pathname);
    const marker = "/o/";
    const markerIndex = decodedPath.indexOf(marker);
    if (markerIndex < 0) return null;
    const objectPath = decodedPath.substring(markerIndex + marker.length);
    const prefix = `event_posters/${eventId}/`;
    return objectPath.startsWith(prefix) && objectPath.length > prefix.length
      ? objectPath
      : null;
  } catch {
    return null;
  }
}

module.exports = {
  availableEventSlots,
  canCancelSelection,
  canDrawReplacement,
  canMarkNotified,
  canRespondToSelection,
  ownedPosterPath,
};
