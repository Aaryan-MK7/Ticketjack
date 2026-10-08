package com.example.fairchance.domain;

import java.util.ArrayList;
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
        this.waitingList = new ArrayList<>(initialList);
    }

    /**
     * Returns the current waiting list.
     */
    public List<String> getWaitingList() {
        return waitingList;
    }

    /**
     * Adds a user if they are not already on the waiting list.
     */
    public void join(String userId) {
        if (!waitingList.contains(userId)) {
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
        for (String id : waitingList) {
            if (!alreadySelected.contains(id)) {
                return id;
            }
        }
        return null;
    }
}
