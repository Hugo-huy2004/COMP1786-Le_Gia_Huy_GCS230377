package com.example.mhike_cw_legiahuy.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mhike_cw_legiahuy.R;
import com.example.mhike_cw_legiahuy.db.DbVersion;
import com.example.mhike_cw_legiahuy.db.FirebaseHelper;
import com.example.mhike_cw_legiahuy.model.Hike;
import com.google.firebase.database.DatabaseReference;

import java.util.Locale;

public class ConfirmActivity extends BaseActivity {

    private Hike hike;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);
        hike = (Hike) getIntent().getSerializableExtra(CreateHikeActivity.EXTRA_HIKE);
        if (hike == null) { finish(); return; }

        ((TextView) findViewById(R.id.txtName)).setText(hike.name);
        TextView tag = findViewById(R.id.txtDifficultyTag);
        if (hike.difficulty != null) {
            tag.setText(hike.difficulty);
            tag.setVisibility(View.VISIBLE);
        } else tag.setVisibility(View.GONE);

        renderRows();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnEdit).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnDiscard).setOnClickListener(v -> discard());
    }

    private void renderRows() {
        LinearLayout c = findViewById(R.id.rowsContainer);
        c.removeAllViews();
        row(c, getString(R.string.row_location), hike.location);
        row(c, getString(R.string.row_datetime),
                hike.startTime == null ? hike.hikeDate : hike.hikeDate + " · " + hike.startTime);
        row(c, getString(R.string.row_distance), trim(hike.lengthKm) + " km");
        row(c, getString(R.string.row_trail), hike.trailType);
        row(c, getString(R.string.row_parking), hike.parking ? "Yes" : "No");
        row(c, getString(R.string.row_weather), hike.weather);
        row(c, getString(R.string.row_description), hike.description);
    }

    private void row(LinearLayout container, String label, String val) {
        if (val == null || val.trim().isEmpty()) return;
        View v = getLayoutInflater().inflate(R.layout.item_confirm_row, container, false);
        ((TextView) v.findViewById(R.id.txtLabel)).setText(label);
        ((TextView) v.findViewById(R.id.txtValue)).setText(val);
        container.addView(v);
    }

    private void save() {
        DatabaseReference ref = FirebaseHelper.hikes();
        String id = hike.id;
        boolean editing = id != null;
        if (id == null) {
            id = ref.push().getKey();
            hike.id = id;
        }
        
        if (id != null) {
            ref.child(id).setValue(hike).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DbVersion.bump();
                    Toast.makeText(this, editing ? R.string.updated_toast : R.string.saved_toast,
                            Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void discard() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.action_discard)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    setResult(CreateHikeActivity.RESULT_DISCARDED);
                    finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static String trim(double d) {
        if (d == Math.rint(d)) return String.valueOf((long) d);
        return String.format(Locale.UK, "%.1f", d);
    }
}
