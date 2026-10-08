package com.example.fairchance.domain;

/**
 * Manages the invitation state for a single entrant.
 * Encapsulates transitions between PENDING → ACCEPTED/DECLINED.
 */
public class InvitationManager {

    /**
     * Represents the possible states of an invitation.
     */
    public enum Status {
        /** Invitation has been sent but no response has been made. */
        PENDING,
        /** Invitation was accepted by the user. */
        ACCEPTED,
        /** Invitation was declined by the user. */
        DECLINED
    }

    private Status status = Status.PENDING;

    /**
     * Returns the current invitation status.
     *
     * @return the current {@link Status}
     */
    public Status getStatus() {
        return status;
    }

    /**
     * @return {@code true} if the invitation is still pending
     */
    public boolean isPending() {
        return status == Status.PENDING;
    }

    /**
     * @return {@code true} if the invitation has been accepted
     */
    public boolean isAccepted() {
        return status == Status.ACCEPTED;
    }

    /**
     * @return {@code true} if the invitation has been declined
     */
    public boolean isDeclined() {
        return status == Status.DECLINED;
    }

    /**
     * Accepts the invitation.
     * Only valid if the current status is {@code PENDING}.
     */
    public void accept() {
        if (status == Status.PENDING) {
            status = Status.ACCEPTED;
        }
    }

    /**
     * Declines the invitation.
     * Only valid if the current status is {@code PENDING}.
     */
    public void decline() {
        if (status == Status.PENDING) {
            status = Status.DECLINED;
        }
    }
}
