package com.example.mhike_cw_legiahuy.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mhike_cw_legiahuy.R;
import com.example.mhike_cw_legiahuy.db.FirebaseHelper;
import com.example.mhike_cw_legiahuy.model.Hike;
import com.example.mhike_cw_legiahuy.model.Observation;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class ObservationsActivity extends BaseActivity {

    public static final String EXTRA_HIKE_ID = "hike_id";

    private static final SimpleDateFormat DAY = new SimpleDateFormat("EEEE d MMMM", Locale.UK);

    private String hikeId;
    private final List<Observation> master = new ArrayList<>();
    private LinearLayout timeline;
    private TextView txtEmpty;
    private String query = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_observations);
        hikeId = getIntent().getStringExtra(EXTRA_HIKE_ID);

        timeline = findViewById(R.id.timeline);
        txtEmpty = findViewById(R.id.txtEmpty);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fab).setOnClickListener(v -> addNote());
        
        ChipGroup categoryChips = findViewById(R.id.categoryChips);
        if (categoryChips != null) {
            categoryChips.setOnCheckedStateChangeListener((g, ids) -> render());
        }

        ((TextView) findViewById(R.id.edtSearch)).addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                query = s.toString().trim().toLowerCase(Locale.UK);
                render();
            }
            public void afterTextChanged(Editable s) {}
        });

        listen();
    }

    private void listen() {
        FirebaseHelper.observations().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                master.clear();
                for (DataSnapshot s : snapshot.getChildren()) {
                    Observation o = s.getValue(Observation.class);
                    if (o != null) {
                        o.id = s.getKey();
                        if (hikeId == null || hikeId.equals(o.hikeId)) {
                            master.add(o);
                        }
                    }
                }
                HashSet<String> uniqueHikes = new HashSet<>();
                for (Observation o : master) if (o.hikeId != null) uniqueHikes.add(o.hikeId);
                
                TextView txtCounts = findViewById(R.id.txtCounts);
                if (txtCounts != null) {
                    txtCounts.setText(getString(R.string.notes_count_fmt, master.size(), uniqueHikes.size()));
                }
                render();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void render() {
        timeline.removeAllViews();
        ChipGroup chips = findViewById(R.id.categoryChips);
        int category = chips == null ? R.id.chipAll : chips.getCheckedChipId();
        List<Observation> filtered = new ArrayList<>();
        for (Observation o : master) {
            if (!query.isEmpty() && !matches(o, query)) continue;
            if (!inCategory(o, category)) continue;
            filtered.add(o);
        }
        txtEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);

        String currentDay = null;
        for (Observation o : filtered) {
            String day = o.obsTime == null ? "" : (o.obsTime.length() >= 10 ? o.obsTime.substring(0, 10) : o.obsTime);
            if (!day.equals(currentDay)) {
                timeline.addView(dayHeader(day));
                currentDay = day;
            }
            timeline.addView(noteRow(o));
        }
    }

    /** A note belongs to a category when it actually recorded something for it. */
    private static boolean inCategory(Observation o, int chipId) {
        if (chipId == R.id.chipWildlife) return !TextUtils.isEmpty(o.wildlife);
        if (chipId == R.id.chipVegetation) return !TextUtils.isEmpty(o.vegetation);
        if (chipId == R.id.chipTrail) return !TextUtils.isEmpty(o.trailCondition);
        return true;
    }

    private static boolean matches(Observation o, String q) {
        return contains(o.observation, q) || contains(o.comments, q);
    }

    private static boolean contains(String s, String q) {
        return s != null && s.toLowerCase(Locale.UK).contains(q);
    }

    private TextView dayHeader(String isoDay) {
        TextView h = new TextView(this);
        h.setText(prettyDay(isoDay));
        h.setTextColor(getColor(R.color.mh_text_muted));
        h.setTextSize(11);
        h.setAllCaps(true);
        h.setLetterSpacing(0.12f);
        h.setPadding(0, dp(14), 0, dp(8));
        return h;
    }

    private String prettyDay(String isoDay) {
        try { return DAY.format(new SimpleDateFormat("yyyy-MM-dd", Locale.UK).parse(isoDay)); }
        catch (Exception e) { return isoDay; }
    }

    private View noteRow(final Observation o) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(12));
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(outValue());
        row.setClickable(true);
        row.setOnClickListener(v -> {
            Intent i = new Intent(this, ObservationDetailActivity.class);
            i.putExtra(ObservationDetailActivity.EXTRA_OBS_ID, o.id);
            startActivity(i);
        });

        TextView time = new TextView(this);
        time.setText(o.obsTime != null && o.obsTime.length() >= 16 ? o.obsTime.substring(11, 16) : "");
        time.setTextColor(getColor(R.color.mh_text_muted));
        time.setTextSize(11);
        time.setWidth(dp(46));
        row.addView(time);

        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams midLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        midLp.setMarginStart(dp(10));
        mid.setLayoutParams(midLp);

        TextView title = new TextView(this);
        title.setText(o.observation);
        title.setTextColor(getColor(R.color.mh_text));
        title.setTextSize(15);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        mid.addView(title);

        if (!TextUtils.isEmpty(o.comments)) {
            TextView sub = new TextView(this);
            sub.setText(o.comments);
            sub.setTextColor(getColor(R.color.mh_text_muted));
            sub.setTextSize(12);
            sub.setMaxLines(2);
            sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
            mid.addView(sub);
        }

        row.addView(mid);
        return row;
    }

    private void addNote() {
        if (hikeId != null) {
            startActivity(new Intent(this, AddObservationActivity.class)
                    .putExtra(AddObservationActivity.EXTRA_HIKE_ID, hikeId));
            return;
        }
        
        FirebaseHelper.hikes().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                final List<Hike> hikes = new ArrayList<>();
                for (DataSnapshot s : snapshot.getChildren()) {
                    Hike h = s.getValue(Hike.class);
                    if (h != null) { h.id = s.getKey(); hikes.add(h); }
                }
                
                if (hikes.isEmpty()) {
                    Toast.makeText(ObservationsActivity.this, R.string.no_hikes_first, Toast.LENGTH_SHORT).show();
                    return;
                }
                
                String[] names = new String[hikes.size()];
                for (int i = 0; i < hikes.size(); i++) names[i] = hikes.get(i).name;
                new AlertDialog.Builder(ObservationsActivity.this)
                        .setTitle(R.string.pick_hike)
                        .setItems(names, (d, which) -> startActivity(new Intent(ObservationsActivity.this, AddObservationActivity.class)
                                .putExtra(AddObservationActivity.EXTRA_HIKE_ID, hikes.get(which).id)))
                        .show();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private int outValue() {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        return tv.resourceId;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
