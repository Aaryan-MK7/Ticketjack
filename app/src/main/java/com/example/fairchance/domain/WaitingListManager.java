package com.example.fairchance.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple manager for handling an event's waiting list in memory.
 * Supports adding/removing users and selecting a replacement.
 */
public class WaitingListManager {

    private final List<String> waitingList;

    /**
     * Creates an empty waiting list.
     */
    public WaitingListManager() {
        this.waitingList = new ArrayList<>();
    }

    /**
     * Creates a waiting list initialized with existing user IDs.
     */
    public WaitingListManager(List<String> initialList) {
        this.waitingList = initialList == null
                ? new ArrayList<>()
                : new ArrayList<>(initialList);
    }

    /**
     * Returns the current waiting list.
     */
    public List<String> getWaitingList() {
        return Collections.unmodifiableList(new ArrayList<>(waitingList));
    }

    /**
     * Adds a user if they are not already on the waiting list.
     */
    public void join(String userId) {
        if (userId != null && !waitingList.contains(userId)) {
            waitingList.add(userId);
        }
    }

    /**
     * Removes a user from the waiting list.
     */
    public void leave(String userId) {
        waitingList.remove(userId);
    }

    /**
     * Returns how many users are currently on the waiting list.
     */
    public int count() {
        return waitingList.size();
    }

    /**
     * Returns the first user in the waiting list who has not already been selected,
     * or null if no such replacement exists.
     */
    public String drawReplacement(List<String> alreadySelected) {
        List<String> selected = alreadySelected == null
                ? Collections.emptyList()
                : alreadySelected;
        for (String id : waitingList) {
            if (!selected.contains(id)) {
                return id;
            }
        }
        return null;
    }
}
