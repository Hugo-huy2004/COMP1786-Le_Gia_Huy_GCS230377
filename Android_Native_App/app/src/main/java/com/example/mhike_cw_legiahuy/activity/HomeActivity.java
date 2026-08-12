package com.example.mhike_cw_legiahuy.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.mhike_cw_legiahuy.R;
import com.example.mhike_cw_legiahuy.db.FirebaseHelper;
import com.example.mhike_cw_legiahuy.db.LocalDatabase;
import com.example.mhike_cw_legiahuy.model.Hike;
import com.example.mhike_cw_legiahuy.model.Observation;
import com.example.mhike_cw_legiahuy.ui.BottomNav;
import com.example.mhike_cw_legiahuy.util.Prefs;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends BaseActivity {

    private Hike nextHike;
    private LocalDatabase localDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        localDb = new LocalDatabase(this);

        ((TextView) findViewById(R.id.txtDate))
                .setText(new SimpleDateFormat("EEEE d MMMM", new Locale(Prefs.lang(this))).format(new Date()).toUpperCase());

        ((TextView) findViewById(R.id.profileInitials)).setText(initialsOfSignedInUser());

        findViewById(R.id.qaNew).setOnClickListener(v ->
                startActivity(new Intent(this, CreateHikeActivity.class)));
        findViewById(R.id.qaField).setOnClickListener(v ->
                startActivity(new Intent(this, ObservationsActivity.class)));
        findViewById(R.id.qaExplore).setOnClickListener(v ->
                startActivity(new Intent(this, MapActivity.class)));
        findViewById(R.id.qaPlan).setOnClickListener(v ->
                startActivity(new Intent(this, TrainingPlanActivity.class)));
        findViewById(R.id.qaShare).setOnClickListener(v -> {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out my hike progress on MHike Android!");
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, "Share with"));
        });
        
        findViewById(R.id.hikePlanCard).setOnClickListener(v ->
                startActivity(new Intent(this, TrainingPlanActivity.class)));

        findViewById(R.id.btnStartSession).setOnClickListener(v -> 
                startActivity(new Intent(this, TrainingPlanActivity.class)));

        BottomNav.wire(this, 0);

        // Instant Load from SQLite
        List<Hike> cached = localDb.getAllHikes();
        if (!cached.isEmpty()) {
            bindNextHike(cached);
            bindStats(cached);
        }

        listenForData();
    }

    private void listenForData() {
        FirebaseHelper.hikes().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Hike> all = new ArrayList<>();
                for (DataSnapshot s : snapshot.getChildren()) {
                    Hike h = s.getValue(Hike.class);
                    if (h != null) { h.id = s.getKey(); all.add(h); }
                }
                localDb.saveHikes(all);
                bindNextHike(all);
                bindStats(all);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        FirebaseHelper.planSessions().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bindPlan(snapshot);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        FirebaseHelper.observations().limitToLast(3).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Observation> notes = new ArrayList<>();
                for (DataSnapshot s : snapshot.getChildren()) {
                    Observation o = s.getValue(Observation.class);
                    if (o != null) { o.id = s.getKey(); notes.add(o); }
                }
                Collections.reverse(notes);
                bindRecentNotes(notes);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void bindNextHike(List<Hike> all) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(new Date());
        nextHike = null;
        for (Hike h : all) {
            if (h.hikeDate != null && h.hikeDate.compareTo(today) >= 0) {
                if (nextHike == null || h.hikeDate.compareTo(nextHike.hikeDate) < 0) nextHike = h;
            }
        }
        findViewById(R.id.heroEmpty).setVisibility(all.isEmpty() ? View.VISIBLE : View.GONE);

        View card = findViewById(R.id.nextHikeCard);
        TextView title = findViewById(R.id.txtNextHike);
        TextView meta = findViewById(R.id.txtNextHikeMeta);
        if (nextHike == null) {
            card.setVisibility(View.GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);
        title.setText(nextHike.name);
        meta.setText(nextHikeMeta(nextHike));
        card.setOnClickListener(v -> startActivity(new Intent(this, HikeDetailActivity.class)
                .putExtra(HikeDetailActivity.EXTRA_HIKE_ID, nextHike.id)));
    }

    private String nextHikeMeta(Hike h) {
        StringBuilder sb = new StringBuilder(h.hikeDate);
        if (h.startTime != null && !h.startTime.isEmpty()) sb.append(" ").append(h.startTime);
        if (h.location != null && !h.location.isEmpty()) sb.append(" · ").append(h.location);
        if (h.weather != null && !h.weather.isEmpty()) sb.append(" · ").append(h.weather);
        return sb.toString();
    }

    private void bindPlan(DataSnapshot sessions) {
        int total = 0, done = 0;
        String nextDate = null;
        long nextMinutes = 0;

        for (DataSnapshot s : sessions.getChildren()) {
            total++;
            boolean isDone = truthy(s.child("done").getValue());
            if (isDone) {
                done++;
                continue;
            }
            String date = str(s.child("date").getValue());
            if (date != null && (nextDate == null || date.compareTo(nextDate) < 0)) {
                nextDate = date;
                nextMinutes = num(s.child("target_minutes").getValue());
            }
        }

        View card = findViewById(R.id.hikePlanCard);
        if (total == 0) {
            card.setVisibility(View.GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);

        TextView title = findViewById(R.id.txtPlanTitle);
        TextView meta = findViewById(R.id.txtPlanMeta);
        LinearProgressIndicator bar = findViewById(R.id.planProgress);

        title.setText(nextDate == null ? getString(R.string.plan_all_done) : prettyDate(nextDate));
        meta.setText(nextDate == null
                ? getString(R.string.plan_progress_done_fmt, done, total)
                : getString(R.string.plan_progress_fmt, nextMinutes, done, total));
        bar.setProgress(Math.round(done * 100f / total));

        findViewById(R.id.btnStartSession).setEnabled(nextDate != null);
    }

    private static String prettyDate(String iso) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.UK).parse(iso);
            return d == null ? iso : new SimpleDateFormat("EEE d MMM", Locale.UK).format(d);
        } catch (ParseException e) {
            return iso;
        }
    }

    private static boolean truthy(Object v) {
        if (v instanceof Number) return ((Number) v).intValue() != 0;
        if (v instanceof Boolean) return (Boolean) v;
        return "1".equals(v) || "true".equalsIgnoreCase(String.valueOf(v));
    }

    private static long num(Object v) {
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    /** The card is headed "This week", so only this week's hikes count towards it. */
    private void bindStats(List<Hike> all) {
        Calendar c = Calendar.getInstance();
        c.setFirstDayOfWeek(Calendar.MONDAY);
        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
        String monday = iso.format(c.getTime());
        c.add(Calendar.DAY_OF_YEAR, 6);
        String sunday = iso.format(c.getTime());

        double km = 0;
        int count = 0;
        for (Hike h : all) {
            if (h.hikeDate == null) continue;
            if (h.hikeDate.compareTo(monday) < 0 || h.hikeDate.compareTo(sunday) > 0) continue;
            km += h.lengthKm;
            count++;
        }
        ((TextView) findViewById(R.id.txtStatKm)).setText(trim(km));
        ((TextView) findViewById(R.id.txtStatCount)).setText(String.valueOf(count));
    }

    private void bindRecentNotes(List<Observation> notes) {
        LinearLayout container = findViewById(R.id.notesContainer);
        container.removeAllViews();
        if (notes.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.no_notes);
            empty.setTextColor(getColor(R.color.mh_text_muted));
            empty.setTextSize(13);
            container.addView(empty);
            return;
        }
        for (Observation o : notes) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            int pad = dp(10);
            row.setPadding(0, pad, 0, pad);
            row.setOnClickListener(v -> startActivity(new Intent(this, ObservationDetailActivity.class)
                    .putExtra(ObservationDetailActivity.EXTRA_OBS_ID, o.id)));
            TextView title = new TextView(this);
            title.setText(o.observation);
            title.setTextColor(getColor(R.color.mh_text));
            title.setTextSize(14);
            TextView sub = new TextView(this);
            sub.setText(o.obsTime);
            sub.setTextColor(getColor(R.color.mh_text_muted));
            sub.setTextSize(12);
            row.addView(title);
            row.addView(sub);
            container.addView(row);
        }
    }

    private static String initialsOfSignedInUser() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return "";
        String name = u.getDisplayName();
        if (name == null || name.trim().isEmpty()) name = u.getEmail();
        if (name == null || name.trim().isEmpty()) return "";

        StringBuilder out = new StringBuilder();
        for (String part : name.trim().split("[\\s._@-]+")) {
            if (part.isEmpty()) continue;
            out.append(Character.toUpperCase(part.charAt(0)));
            if (out.length() == 2) break;
        }
        return out.toString();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static String trim(double d) {
        if (d == Math.rint(d)) return String.valueOf((long) d);
        return String.format(Locale.UK, "%.1f", d);
    }
}
