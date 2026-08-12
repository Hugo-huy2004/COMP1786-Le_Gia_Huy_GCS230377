package com.example.mhike_cw_legiahuy.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mhike_cw_legiahuy.R;
import com.example.mhike_cw_legiahuy.db.FirebaseHelper;
import com.example.mhike_cw_legiahuy.model.Hike;
import com.example.mhike_cw_legiahuy.model.Observation;
import com.example.mhike_cw_legiahuy.util.DiffColor;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HikeDetailActivity extends BaseActivity {

    public static final String EXTRA_HIKE_ID = "hike_id";

    private String hikeId;
    private Hike currentHike;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hike_detail);
        hikeId = getIntent().getStringExtra(EXTRA_HIKE_ID);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddNote).setOnClickListener(v ->
                startActivity(new Intent(this, AddObservationActivity.class)
                        .putExtra(AddObservationActivity.EXTRA_HIKE_ID, hikeId)));
        findViewById(R.id.btnEdit).setOnClickListener(v -> {
            Intent i = new Intent(this, CreateHikeActivity.class);
            i.putExtra(CreateHikeActivity.EXTRA_HIKE_ID, hikeId);
            startActivity(i);
        });
        findViewById(R.id.btnDelete).setOnClickListener(v -> confirmDelete());
        findViewById(R.id.btnShare).setOnClickListener(v -> shareHike());

        load();
    }

    private void load() {
        FirebaseHelper.hikes().child(hikeId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Hike h = snapshot.getValue(Hike.class);
                if (h != null) { 
                    h.id = snapshot.getKey(); 
                    currentHike = h;
                    bind(h); 
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        FirebaseHelper.observations().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Observation> obs = new ArrayList<>();
                for (DataSnapshot s : snapshot.getChildren()) {
                    Observation o = s.getValue(Observation.class);
                    if (o != null && hikeId.equals(o.hikeId)) {
                        o.id = s.getKey();
                        obs.add(o);
                    }
                }
                bindObservations(obs);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void bind(Hike h) {
        set(R.id.txtKicker, getString("completed".equalsIgnoreCase(String.valueOf(h.status).trim())
                ? R.string.status_completed : R.string.status_planned));
        set(R.id.txtTitle, h.name);
        set(R.id.txtMeta, meta(h));

        tag(R.id.tagDifficulty, h.difficulty);
        TextView diff = findViewById(R.id.tagDifficulty);
        if (diff != null && h.difficulty != null) {
            diff.setTextColor(getResources().getColor(DiffColor.solid(h.difficulty), null));
            diff.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(DiffColor.tint(h.difficulty), null)));
        }

        set(R.id.statKm, trim(h.lengthKm));
        set(R.id.statDuration, TextUtils.isEmpty(h.duration) ? "\u2014" : h.duration);

        set(R.id.txtWeather, TextUtils.isEmpty(h.weather) ? getString(R.string.not_set) : h.weather);

        set(R.id.txtParking, getString(h.parking ? R.string.parking_yes : R.string.parking_no));
        set(R.id.txtTrailType, TextUtils.isEmpty(h.trailType) ? getString(R.string.not_set) : h.trailType);

        LinearLayout rows = findViewById(R.id.detailRows);
        rows.removeAllViews();
        row(rows, getString(R.string.row_description), h.description);
        row(rows, getString(R.string.lbl_priority), h.priority);
        row(rows, getString(R.string.lbl_kit_list), h.kitList == null || h.kitList.isEmpty()
                ? null : TextUtils.join(", ", h.kitList));
        findViewById(R.id.detailCard).setVisibility(rows.getChildCount() == 0 ? View.GONE : View.VISIBLE);
    }

    /** Same label/value row the note detail screen uses. */
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
        val.setGravity(android.view.Gravity.END);
        val.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f));

        row.addView(l);
        row.addView(val);
        container.addView(row);
    }

    private void bindObservations(List<Observation> obs) {
        set(R.id.txtObsHeader, getString(R.string.observations_fmt, obs.size()));
        LinearLayout container = findViewById(R.id.obsContainer);
        if (container == null) return;
        container.removeAllViews();
        for (Observation o : obs) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            int pad = dp(8);
            row.setPadding(0, pad, 0, pad);
            row.setOnClickListener(v -> startActivity(new Intent(this, ObservationDetailActivity.class)
                    .putExtra(ObservationDetailActivity.EXTRA_OBS_ID, o.id)));
            
            TextView title = new TextView(this);
            title.setText(o.observation);
            title.setTextColor(getColor(R.color.mh_text));
            title.setTextSize(14);
            
            TextView sub = new TextView(this);
            sub.setText(TextUtils.isEmpty(o.comments) ? o.obsTime : o.obsTime + " · " + o.comments);
            sub.setTextColor(getColor(R.color.mh_text_muted));
            sub.setTextSize(12);
            
            row.addView(title);
            row.addView(sub);
            container.addView(row);
        }
    }

    private void shareHike() {
        if (currentHike == null) return;
        String text = getString(R.string.share_hike_fmt, currentHike.name, currentHike.location, 
                currentHike.hikeDate, trim(currentHike.lengthKm));
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(i, getString(R.string.share)));
    }

    private void confirmDelete() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setMessage(R.string.confirm_delete_hike)
                .setPositiveButton(R.string.action_delete, (d, w) -> {
                    FirebaseHelper.hikes().child(hikeId).removeValue();
                    finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private String meta(Hike h) {
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(h.location)) sb.append(h.location);
        if (!TextUtils.isEmpty(h.hikeDate)) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(h.hikeDate);
            if (!TextUtils.isEmpty(h.startTime)) sb.append(" ").append(h.startTime);
        }
        return sb.toString();
    }

    private void set(int id, String text) {
        TextView v = findViewById(id);
        if (v != null) v.setText(text);
    }

    private void tag(int id, String value) {
        TextView t = findViewById(id);
        if (t == null) return;
        if (TextUtils.isEmpty(value)) { t.setVisibility(View.GONE); return; }
        t.setText(value);
        t.setVisibility(View.VISIBLE);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static String trim(double d) {
        if (d == Math.rint(d)) return String.valueOf((long) d);
        return String.format(Locale.UK, "%.1f", d);
    }
}
