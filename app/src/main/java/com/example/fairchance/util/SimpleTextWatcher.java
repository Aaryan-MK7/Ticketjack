package com.example.fairchance.util;

import android.text.Editable;
import android.text.TextWatcher;

/**
 * TextWatcher that triggers a callback only during onTextChanged().
 */
public class SimpleTextWatcher implements TextWatcher {

    public interface OnChanged {
        void onChanged();
    }

    private final OnChanged onChanged;

    public SimpleTextWatcher(OnChanged onChanged) {
        this.onChanged = onChanged;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (onChanged != null) onChanged.onChanged();
    }

    @Override
    public void afterTextChanged(Editable s) { }
}
