package com.example.fairchance;

/**
 * Defines listener callbacks for tests when repository code
 * does not expose OnCompleteListener publicly.
 */
public interface TestListeners {

    interface OnCompleteListener {
        void onSuccess();
        void onFailure(Exception e);
    }
}
