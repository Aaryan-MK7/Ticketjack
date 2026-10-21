"use strict";

const assert = require("node:assert/strict");
const test = require("node:test");
const {
  availableEventSlots,
  canCancelSelection,
  canDrawReplacement,
  canMarkNotified,
  canRespondToSelection,
  ownedPosterPath,
} = require("./workflow");

test("selection transitions reject terminal participant states", () => {
  assert.equal(canRespondToSelection({ status: "pending" }), true);
  assert.equal(canRespondToSelection({ status: "notified" }), true);
  assert.equal(canRespondToSelection({ status: "accepted" }), false);
  assert.equal(canCancelSelection({ status: "selected" }), true);
  assert.equal(canCancelSelection({ status: "declined" }), false);
  assert.equal(canMarkNotified({ status: "pending" }), true);
  assert.equal(canMarkNotified({ status: "accepted" }), false);
});

test("a cancelled entry receives at most one replacement draw", () => {
  assert.equal(canDrawReplacement({ status: "cancelled" }), true);
  assert.equal(canDrawReplacement({ status: "cancelled", replacementDrawn: true }), false);
  assert.equal(canDrawReplacement({ status: "accepted" }), false);
});

test("available slots deduplicate confirmed and selected participants", () => {
  const slots = availableEventSlots(
    { capacity: 4 },
    [
      { id: "confirmed-user", status: "accepted" },
      { id: "pending-user", status: "pending" },
      { id: "declined-user", status: "declined" },
    ],
    ["confirmed-user"],
  );
  assert.equal(slots, 2);
  assert.equal(availableEventSlots({ capacity: 0 }, [], []), 0);
});

test("poster paths must belong to the selected event", () => {
  const validUrl = "https://firebasestorage.googleapis.com/v0/b/example/o/" +
    "event_posters%2Fevent-1%2Fposter.jpg?alt=media";
  const otherEventUrl = "https://firebasestorage.googleapis.com/v0/b/example/o/" +
    "event_posters%2Fevent-2%2Fposter.jpg?alt=media";

  assert.equal(ownedPosterPath(validUrl, "event-1"), "event_posters/event-1/poster.jpg");
  assert.equal(ownedPosterPath(otherEventUrl, "event-1"), null);
  assert.equal(ownedPosterPath("not a url", "event-1"), null);
});
