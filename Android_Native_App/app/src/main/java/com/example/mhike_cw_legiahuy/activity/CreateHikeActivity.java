package com.example.mhike_cw_legiahuy.activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mhike_cw_legiahuy.R;
import com.example.mhike_cw_legiahuy.db.FirebaseHelper;
import com.example.mhike_cw_legiahuy.model.Hike;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CreateHikeActivity extends BaseActivity {

    public static final String EXTRA_HIKE_ID = "hike_id";
    static final String EXTRA_HIKE = "hike_draft";
    static final int RESULT_DISCARDED = RESULT_FIRST_USER;
    private static final int REQ_CONFIRM = 1;

    private EditText edtName, edtLocation, edtDistance, edtDuration, edtTrailType, edtWeather, edtDescription;
    private TextView txtDate, txtTime, errDifficulty, errParking, txtTitle, txtKicker;
    private MaterialButtonToggleGroup toggleDifficulty, toggleParking, togglePriority;
    private MaterialButton btnReview;
    private ChipGroup kitGroup;

    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 0, currentLng = 0;

    private final Calendar dateCal = Calendar.getInstance();
    private boolean dateSet = false;
    private String startTime = null;
    private String editingId = null;
    private Hike original = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_hike);
        bind();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
        // The whole row is the tap target, not just the label - the icon is half of it.
        findViewById(R.id.rowDate).setOnClickListener(v -> pickDate());
        txtDate.setOnClickListener(v -> pickDate());
        findViewById(R.id.rowTime).setOnClickListener(v -> pickTime());
        txtTime.setOnClickListener(v -> pickTime());
        btnReview.setOnClickListener(v -> onReview());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        findViewById(R.id.btnUseLocation).setOnClickListener(v -> useMyLocation());

        toggleDifficulty.addOnButtonCheckedListener((g, id, checked) -> errDifficulty.setVisibility(View.GONE));
        toggleParking.addOnButtonCheckedListener((g, id, checked) -> errParking.setVisibility(View.GONE));

        editingId = getIntent().getStringExtra(EXTRA_HIKE_ID);
        if (editingId != null) enterEditMode(editingId);
        else prefillNow();
    }

    private void prefillNow() {
        Calendar now = Calendar.getInstance();
        dateCal.setTime(now.getTime());
        dateSet = true;
        txtDate.setText(String.format(Locale.UK, "%04d-%02d-%02d",
                now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH)));
    }

    private void bind() {
        edtName = findViewById(R.id.edtName);
        edtLocation = findViewById(R.id.edtLocation);
        txtDate = findViewById(R.id.edtDate);
        txtTime = findViewById(R.id.txtTime);
        edtDistance = findViewById(R.id.edtDistance);
        edtDuration = findViewById(R.id.edtDuration);
        edtTrailType = findViewById(R.id.edtTrailType);
        edtWeather = findViewById(R.id.edtWeather);
        edtDescription = findViewById(R.id.edtDescription);
        toggleDifficulty = findViewById(R.id.toggleDifficulty);
        toggleParking = findViewById(R.id.toggleParking);
        togglePriority = findViewById(R.id.togglePriority);
        errDifficulty = findViewById(R.id.errDifficulty);
        errParking = findViewById(R.id.errParking);
        txtTitle = findViewById(R.id.txtTitle);
        txtKicker = findViewById(R.id.txtKicker);
        btnReview = findViewById(R.id.btnReview);
        kitGroup = findViewById(R.id.kitGroup);
    }

    private void pickDate() {
        new DatePickerDialog(this, (view, y, m, d) -> {
            dateCal.set(y, m, d);
            dateSet = true;
            txtDate.setText(String.format(Locale.UK, "%04d-%02d-%02d", y, m + 1, d));
        }, dateCal.get(Calendar.YEAR), dateCal.get(Calendar.MONTH),
                dateCal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pickTime() {
        Calendar now = Calendar.getInstance();
        int h = now.get(Calendar.HOUR_OF_DAY), min = now.get(Calendar.MINUTE);
        // The web app may store "9:30" as well as "09:30", so split rather than slice.
        if (startTime != null && startTime.contains(":")) {
            String[] p = startTime.split(":");
            try {
                h = Integer.parseInt(p[0].trim());
                min = Integer.parseInt(p[1].trim());
            } catch (NumberFormatException ignored) {}
        }
        new TimePickerDialog(this, (view, hh, mm) -> setStartTime(String.format(Locale.UK, "%02d:%02d", hh, mm)),
                h, min, true).show();
    }

    private void setStartTime(String hhmm) {
        startTime = hhmm;
        txtTime.setText(hhmm);
        txtTime.setTextColor(getColor(R.color.mh_text));
    }

    private void useMyLocation() {
        if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();
                edtLocation.setText(String.format(Locale.UK, "%.4f, %.4f", currentLat, currentLng));
            }
        });
    }

    private void onReview() {
        boolean ok = true;

        if (TextUtils.isEmpty(text(edtName))) ok = false;
        if (TextUtils.isEmpty(text(edtLocation))) ok = false;
        if (!dateSet || TextUtils.isEmpty(txtDate.getText())) ok = false;

        Double distance = parseNumber(text(edtDistance));
        if (distance == null) ok = false;

        if (toggleDifficulty.getCheckedButtonId() == View.NO_ID) {
            errDifficulty.setText(R.string.err_pick_difficulty);
            errDifficulty.setVisibility(View.VISIBLE);
            ok = false;
        }
        if (toggleParking.getCheckedButtonId() == View.NO_ID) {
            errParking.setText(R.string.err_pick_parking);
            errParking.setVisibility(View.VISIBLE);
            ok = false;
        }

        if (!ok) return;

        Hike h = collect(distance);
        Intent i = new Intent(this, ConfirmActivity.class);
        i.putExtra(EXTRA_HIKE, h);
        startActivityForResult(i, REQ_CONFIRM);
    }

    private Hike collect(double distance) {
        Hike h = new Hike();
        h.id = editingId;
        h.name = text(edtName);
        h.location = text(edtLocation);
        h.hikeDate = txtDate.getText().toString().trim();
        h.parking = toggleParking.getCheckedButtonId() == R.id.btnParkingYes;
        h.lengthKm = distance;
        h.difficulty = difficultyLabel();
        h.description = emptyToNull(text(edtDescription));
        h.trailType = emptyToNull(text(edtTrailType));
        h.weather = emptyToNull(text(edtWeather));
        h.duration = text(edtDuration);
        h.startTime = startTime;
        h.latitude = currentLat;
        h.longitude = currentLng;
        h.priority = priorityLabel();
        h.kitList = getSelectedKit();
        // The save is a whole-node setValue, so carry over the fields this form never edits.
        if (original != null) {
            h.status = original.status;
            h.favourite = original.favourite;
            h.photos = original.photos;
            h.visibility = original.visibility;
            h.emergencyContact = original.emergencyContact;
        }
        return h;
    }

    private List<String> getSelectedKit() {
        List<String> kit = new ArrayList<>();
        if (kitGroup == null) return kit;
        for (int i = 0; i < kitGroup.getChildCount(); i++) {
            Chip chip = (Chip) kitGroup.getChildAt(i);
            if (chip.isChecked()) kit.add(chip.getText().toString());
        }
        return kit;
    }

    private String difficultyLabel() {
        int id = toggleDifficulty.getCheckedButtonId();
        if (id == R.id.btnEasy) return "Easy";
        if (id == R.id.btnModerate) return "Moderate";
        if (id == R.id.btnHard) return "Hard";
        return null;
    }

    private String priorityLabel() {
        int id = togglePriority.getCheckedButtonId();
        if (id == R.id.btnMust) return "Must do";
        if (id == R.id.btnSoon) return "Soon";
        if (id == R.id.btnSomeday) return "Someday";
        return null;
    }

    private void enterEditMode(String id) {
        FirebaseHelper.hikes().child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Hike h = snapshot.getValue(Hike.class);
                if (h != null) {
                    h.id = snapshot.getKey();
                    original = h;
                    txtTitle.setText(R.string.edit_hike);
                    txtKicker.setText(R.string.kicker_edit);
                    btnReview.setText(R.string.action_save_changes);
                    populate(h);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void populate(Hike h) {
        edtName.setText(h.name);
        edtLocation.setText(h.location);
        if (!TextUtils.isEmpty(h.hikeDate)) {
            txtDate.setText(h.hikeDate);
            dateSet = true;
            String[] p = h.hikeDate.split("-");
            if (p.length == 3) dateCal.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]));
        }
        if (!TextUtils.isEmpty(h.startTime)) setStartTime(h.startTime);
        edtDistance.setText(fmt(h.lengthKm));
        edtDuration.setText(h.duration);
        edtWeather.setText(h.weather);
        edtDescription.setText(h.description);
        edtTrailType.setText(h.trailType);
        toggleParking.check(h.parking ? R.id.btnParkingYes : R.id.btnParkingNo);
        checkDifficulty(h.difficulty);
        checkPriority(h.priority);
        checkKit(h.kitList);
        currentLat = h.latitude;
        currentLng = h.longitude;
    }

    private void checkDifficulty(String d) {
        if ("Easy".equals(d)) toggleDifficulty.check(R.id.btnEasy);
        else if ("Moderate".equals(d)) toggleDifficulty.check(R.id.btnModerate);
        else if ("Hard".equals(d)) toggleDifficulty.check(R.id.btnHard);
    }

    private void checkPriority(String p) {
        if ("Must do".equals(p)) togglePriority.check(R.id.btnMust);
        else if ("Soon".equals(p)) togglePriority.check(R.id.btnSoon);
        else if ("Someday".equals(p)) togglePriority.check(R.id.btnSomeday);
    }

    private void checkKit(List<String> kit) {
        if (kit == null || kitGroup == null) return;
        for (int i = 0; i < kitGroup.getChildCount(); i++) {
            Chip chip = (Chip) kitGroup.getChildAt(i);
            if (kit.contains(chip.getText().toString())) chip.setChecked(true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CONFIRM) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK);
                finish();
            } else if (resultCode == RESULT_DISCARDED) {
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }

    private String text(EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private static String emptyToNull(String s) {
        return TextUtils.isEmpty(s) ? null : s;
    }

    private static Double parseNumber(String s) {
        if (TextUtils.isEmpty(s)) return null;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return null; }
    }

    private static String fmt(double d) {
        if (d == Math.rint(d)) return String.valueOf((long) d);
        return String.valueOf(d);
    }
}
