package com.example.fairchance.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.fairchance.R;

/**
 * Fragment that displays the comprehensive rules and guidelines for the FairChance
 * lottery system. Can also display specific guidelines for a particular event.
 * Fulfills US 01.05.05.
 */
public class GuidelinesFragment extends Fragment {

    private static final String ARG_GUIDELINES = "arg_guidelines";
    private String specificGuidelines;

    public GuidelinesFragment() {
        // Required empty public constructor
    }

    /**
     * Creates a new instance of GuidelinesFragment with optional specific guidelines.
     * @param guidelines The specific guidelines text from the event (can be null).
     * @return A new instance of fragment GuidelinesFragment.
     */
    public static GuidelinesFragment newInstance(String guidelines) {
        GuidelinesFragment fragment = new GuidelinesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_GUIDELINES, guidelines);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            specificGuidelines = getArguments().getString(ARG_GUIDELINES);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_guidelines, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button backButton = view.findViewById(R.id.button2);

        backButton.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });
    }
}
