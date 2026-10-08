package com.example.fairchance.ui.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.fairchance.R;
import com.example.fairchance.data.repository.EventRepository;
import com.example.fairchance.models.Event;
import com.google.android.material.textfield.TextInputEditText;
import java.util.HashMap;
import java.util.Map;

public class UpdateEventFragment extends Fragment {

    private static final String ARG_EVENT_ID = "EVENT_ID";

    private String eventId;
    private ImageView ivPoster;
    private TextInputEditText etName, etDesc, etTimeLoc, etRegDates, etRegTimes;
    private CheckBox cbGeo;
    private Button btnSave, btnCancel, btnGenQR;
    private Uri pickedImage;
    private Event loaded;

    private final EventRepository repo = new EventRepository();

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            pickedImage = uri;
                            ivPoster.setImageURI(uri);
                        }
                    });

    public static UpdateEventFragment newInstance(String eventId) {
        UpdateEventFragment f = new UpdateEventFragment();
        Bundle b = new Bundle();
        b.putString(ARG_EVENT_ID, eventId);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.update_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        ivPoster = v.findViewById(R.id.ivEventPoster);
        etName = v.findViewById(R.id.etEventName);
        etDesc = v.findViewById(R.id.etEventDescription);
        etTimeLoc = v.findViewById(R.id.etEventTimeLocation);
        etRegDates = v.findViewById(R.id.etRegistrationDates);
        etRegTimes = v.findViewById(R.id.etRegistrationTimes);
        cbGeo = v.findViewById(R.id.cbGeolocation);
        btnGenQR = v.findViewById(R.id.btnGenerateQRCode);
        btnSave = v.findViewById(R.id.btnSaveChanges);
        btnCancel = v.findViewById(R.id.btnCancelChanges);

        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
        }

        ivPoster.setOnClickListener(v1 -> pickImage.launch("image/*"));

        btnSave.setOnClickListener(v12 -> saveChanges());
        btnCancel.setOnClickListener(v13 -> requireActivity().onBackPressed());
        btnGenQR.setOnClickListener(v14 -> Toast.makeText(getContext(), "QR already generated in details.", Toast.LENGTH_SHORT).show());

        if (!TextUtils.isEmpty(eventId)) {
            repo.getEvent(eventId, new EventRepository.EventCallback() {
                @Override public void onSuccess(Event event) {
                    loaded = event;
                    bind(event);
                }
                @Override public void onError(String message) {
                    Toast.makeText(getContext(), "Load failed: " + message, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void bind(Event e) {
        if (e == null) return;
        Glide.with(requireContext())
                .load(e.getPosterImageUrl())
                .placeholder(R.drawable.fairchance_logo_with_words___transparent)
                .into(ivPoster);

        if (e.getName() != null) etName.setText(e.getName());
        if (e.getDescription() != null) etDesc.setText(e.getDescription());
        if (e.getLocation() != null) etTimeLoc.setText(e.getLocation());
        // Leave dates and times as free text for now
        cbGeo.setChecked(e.isGeolocationRequired());
    }

    private void saveChanges() {
        if (loaded == null) { Toast.makeText(getContext(), "Event not loaded yet.", Toast.LENGTH_SHORT).show(); return; }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", String.valueOf(etName.getText()).trim());
        updates.put("description", String.valueOf(etDesc.getText()).trim());
        updates.put("location", String.valueOf(etTimeLoc.getText()).trim());
        updates.put("geolocationRequired", cbGeo.isChecked());

        repo.updateEventFields(eventId, updates, new EventRepository.EventTaskCallback() {
            @Override public void onSuccess() {

                if (pickedImage != null) {
                    repo.uploadPosterAndUpdate(eventId, pickedImage, new EventRepository.EventTaskCallback() {
                        @Override public void onSuccess() {
                            Toast.makeText(getContext(), "Saved.", Toast.LENGTH_SHORT).show();
                            requireActivity().onBackPressed();
                        }
                        @Override public void onError(String message) {
                            Toast.makeText(getContext(), "Poster upload failed: " + message, Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    Toast.makeText(getContext(), "Saved.", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                }
            }
            @Override public void onError(String message) {
                Toast.makeText(getContext(), "Save failed: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
