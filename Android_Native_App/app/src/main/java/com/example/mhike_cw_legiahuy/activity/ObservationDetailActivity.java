package com.example.mhike_cw_legiahuy.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mhike_cw_legiahuy.R;
import com.example.mhike_cw_legiahuy.db.FirebaseHelper;
import com.example.mhike_cw_legiahuy.model.Hike;
import com.example.mhike_cw_legiahuy.model.Observation;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class ObservationDetailActivity extends BaseActivity {

    public static final String EXTRA_OBS_ID = "observation_id";

    private String obsId;
    private String watchedHikeId;
    private DatabaseReference hikeRef;
    private ValueEventListener hikeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_observation_detail);
        obsId = getIntent().getStringExtra(EXTRA_OBS_ID);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnEdit).setOnClickListener(v ->
                startActivity(new Intent(this, AddObservationActivity.class)
                        .putExtra(AddObservationActivity.EXTRA_OBS_ID, obsId)));
        findViewById(R.id.btnDelete).setOnClickListener(v -> confirmDelete());

        load();
    }

    private void load() {
        FirebaseHelper.observations().child(obsId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Observation o = snapshot.getValue(Observation.class);
                if (o == null) { finish(); return; }
                o.id = snapshot.getKey();
                bind(o);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /**
     * Keeps the hike name live. bind() runs on every observation change, so the previous
     * listener is detached first - otherwise each update would stack another one.
     */
    private void watchHike(String hikeId) {
        if (hikeId != null && hikeId.equals(watchedHikeId)) return;

        if (hikeRef != null && hikeListener != null) hikeRef.removeEventListener(hikeListener);
        hikeRef = null;
        hikeListener = null;
        watchedHikeId = hikeId;

        if (hikeId == null) {
            set(R.id.txtOnHike, getString(R.string.on_hike_fmt, ""));
            return;
        }

        hikeRef = FirebaseHelper.hikes().child(hikeId);
        hikeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Hike h = snapshot.getValue(Hike.class);
                set(R.id.txtOnHike, getString(R.string.on_hike_fmt, h == null ? "" : h.name));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("MHike", "hike name cancelled: " + error.getMessage());
            }
        };
        hikeRef.addValueEventListener(hikeListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hikeRef != null && hikeListener != null) hikeRef.removeEventListener(hikeListener);
    }

    private void bind(Observation o) {
        set(R.id.txtKicker, getString(R.string.field_note_kicker_fmt, o.obsTime == null ? "" : o.obsTime));
        set(R.id.txtTitle, o.observation);

        watchHike(o.hikeId);

        LinearLayout rows = findViewById(R.id.detailRows);
        if (rows == null) return;
        rows.removeAllViews();
        row(rows, getString(R.string.lbl_detail), o.comments);
        row(rows, getString(R.string.lbl_trail_condition), o.trailCondition);
        row(rows, getString(R.string.lbl_wildlife), o.wildlife);
        row(rows, getString(R.string.lbl_vegetation), o.vegetation);
        row(rows, getString(R.string.lbl_mood_short), o.mood);
        if (o.rating > 0) row(rows, getString(R.string.lbl_rating_short), getString(R.string.rating_fmt, (int) o.rating));
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.confirm_delete_note)
                .setPositiveButton(R.string.action_delete, (d, w) -> {
                    FirebaseHelper.observations().child(obsId).removeValue();
                    finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void row(LinearLayout container, String label, String value) {
        if (TextUtils.isEmpty(value)) return;
        if (container.getChildCount() > 0) {
            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
            divider.setBackgroundColor(getColor(R.color.mh_divider));
            container.addView(divider);
        }
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int pad = dp(11);
        row.setPadding(0, pad, 0, pad);

        TextView l = new TextView(this);
        l.setText(label);
        l.setTextColor(getColor(R.color.mh_text_muted));
        l.setTextSize(13);
        l.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView val = new TextView(this);
        val.setText(value);
        val.setTextColor(getColor(R.color.mh_text));
        val.setTextSize(13);
        val.setGravity(Gravity.END);
        val.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f));

        row.addView(l);
        row.addView(val);
        container.addView(row);
    }

    private void set(int id, String text) {
        TextView v = findViewById(id);
        if (v != null) v.setText(text);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
