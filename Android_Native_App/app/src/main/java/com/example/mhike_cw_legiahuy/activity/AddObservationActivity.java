package com.example.mhike_cw_legiahuy.activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.example.mhike_cw_legiahuy.R;
import com.example.mhike_cw_legiahuy.db.DbVersion;
import com.example.mhike_cw_legiahuy.db.FirebaseHelper;
import com.example.mhike_cw_legiahuy.model.Hike;
import com.example.mhike_cw_legiahuy.model.Observation;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddObservationActivity extends BaseActivity {

    public static final String EXTRA_HIKE_ID = "hike_id";
    public static final String EXTRA_OBS_ID = "obs_id";

    private static final SimpleDateFormat STORED = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.UK);

    private String hikeId, editingId;
    private EditText edtObsTitle, edtComments;
    private TextView txtWhen, txtKicker;
    private MaterialButtonToggleGroup toggleCondition, toggleMood;
    private ChipGroup wildlifeGroup, vegetationGroup;
    private Slider sliderRating;

    private final Calendar cal = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_observation);

        hikeId = getIntent().getStringExtra(EXTRA_HIKE_ID);
        editingId = getIntent().getStringExtra(EXTRA_OBS_ID);

        edtObsTitle = findViewById(R.id.edtObsTitle);
        edtComments = findViewById(R.id.edtComments);
        txtWhen = findViewById(R.id.txtWhen);
        txtKicker = findViewById(R.id.txtKicker);
        toggleCondition = findViewById(R.id.toggleCondition);
        toggleMood = findViewById(R.id.toggleMood);
        wildlifeGroup = findViewById(R.id.wildlifeGroup);
        vegetationGroup = findViewById(R.id.vegetationGroup);
        sliderRating = findViewById(R.id.sliderRating);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> onSave());
        findViewById(R.id.chipTime).setOnClickListener(v -> pickDateTime());
        findViewById(R.id.rowHike).setOnClickListener(v -> pickHike());

        if (editingId != null) load();
        else prefill();
    }

    private void prefill() {
        cal.setTime(new Date());
        updateTimeLabel();
        showHikeName();
    }

    private void load() {
        FirebaseHelper.observations().child(editingId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Observation o = snapshot.getValue(Observation.class);
                if (o == null) { finish(); return; }
                ((TextView) findViewById(R.id.txtTitle)).setText(R.string.edit_field_note);
                // Without this the save below would write hike_id = null and orphan the note.
                hikeId = o.hikeId;
                edtObsTitle.setText(o.observation);
                edtComments.setText(o.comments);
                check(toggleCondition, o.trailCondition);
                check(toggleMood, o.mood);
                checkChips(wildlifeGroup, o.wildlife);
                checkChips(vegetationGroup, o.vegetation);
                sliderRating.setValue(Math.max(0, Math.min(5, o.rating)));
                try {
                    Date d = STORED.parse(o.obsTime);
                    if (d != null) cal.setTime(d);
                } catch (Exception ignored) {}
                updateTimeLabel();
                showHikeName();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showHikeName() {
        if (hikeId == null) {
            txtKicker.setText(R.string.lbl_pick_one);
            return;
        }
        FirebaseHelper.hikes().child(hikeId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Hike h = snapshot.getValue(Hike.class);
                txtKicker.setText(h == null || TextUtils.isEmpty(h.name) ? getString(R.string.lbl_pick_one) : h.name);
                txtKicker.setTextColor(getColor(h == null ? R.color.mh_text_muted : R.color.mh_text));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void pickHike() {
        FirebaseHelper.hikes().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                final List<Hike> hikes = new ArrayList<>();
                for (DataSnapshot s : snapshot.getChildren()) {
                    Hike h = s.getValue(Hike.class);
                    if (h != null) { h.id = s.getKey(); hikes.add(h); }
                }
                if (hikes.isEmpty()) {
                    Toast.makeText(AddObservationActivity.this, R.string.no_hikes_first, Toast.LENGTH_SHORT).show();
                    return;
                }
                String[] names = new String[hikes.size()];
                for (int i = 0; i < hikes.size(); i++) names[i] = hikes.get(i).name;
                new AlertDialog.Builder(AddObservationActivity.this)
                        .setTitle(R.string.pick_hike)
                        .setItems(names, (d, which) -> {
                            hikeId = hikes.get(which).id;
                            showHikeName();
                        })
                        .show();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateTimeLabel() {
        txtWhen.setText(new SimpleDateFormat("d MMM yyyy · HH:mm", Locale.UK).format(cal.getTime()));
    }

    /** Date then time - the stored value carries HH:mm, so the time has to be editable too. */
    private void pickDateTime() {
        new DatePickerDialog(this, (view, y, m, d) -> {
            cal.set(y, m, d);
            new TimePickerDialog(this, (tv, hh, mm) -> {
                cal.set(Calendar.HOUR_OF_DAY, hh);
                cal.set(Calendar.MINUTE, mm);
                updateTimeLabel();
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void onSave() {
        String title = edtObsTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, R.string.err_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (hikeId == null) {
            Toast.makeText(this, R.string.pick_hike, Toast.LENGTH_SHORT).show();
            return;
        }

        Observation o = new Observation();
        o.hikeId = hikeId;
        o.observation = title;
        o.comments = edtComments.getText().toString().trim();
        o.obsTime = STORED.format(cal.getTime());
        o.trailCondition = checkedLabel(toggleCondition);
        o.mood = checkedLabel(toggleMood);
        o.wildlife = checkedChips(wildlifeGroup);
        o.vegetation = checkedChips(vegetationGroup);
        o.rating = (long) sliderRating.getValue();

        DatabaseReference ref = FirebaseHelper.observations();
        String id = editingId;
        if (id == null) id = ref.push().getKey();
        o.id = id;

        if (id != null) {
            ref.child(id).setValue(o).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DbVersion.bump();
                    Toast.makeText(this, editingId == null ? R.string.note_saved : R.string.note_updated, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Every option carries its stored value in android:tag, not in its label - the labels are
     * translated, so storing them would make a note unreadable after a language switch.
     */
    private static String checkedLabel(MaterialButtonToggleGroup group) {
        int id = group.getCheckedButtonId();
        return id == View.NO_ID ? null : String.valueOf(group.findViewById(id).getTag());
    }

    private static void check(MaterialButtonToggleGroup group, String value) {
        if (value == null) return;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (value.equals(child.getTag())) group.check(child.getId());
        }
    }

    private static String checkedChips(ChipGroup group) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < group.getChildCount(); i++) {
            Chip c = (Chip) group.getChildAt(i);
            if (!c.isChecked()) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(c.getTag());
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static void checkChips(ChipGroup group, String csv) {
        if (TextUtils.isEmpty(csv)) return;
        List<String> picked = Arrays.asList(csv.split("\\s*,\\s*"));
        for (int i = 0; i < group.getChildCount(); i++) {
            Chip c = (Chip) group.getChildAt(i);
            if (picked.contains(String.valueOf(c.getTag()))) c.setChecked(true);
        }
    }
}
