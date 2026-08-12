package com.example.mhike_cw_legiahuy.activity;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mhike_cw_legiahuy.R;
import com.example.mhike_cw_legiahuy.db.FirebaseHelper;
import com.example.mhike_cw_legiahuy.model.Hike;
import com.example.mhike_cw_legiahuy.model.Observation;
import com.example.mhike_cw_legiahuy.ui.BottomNav;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class StatsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);
        BottomNav.wire(this, 3);
        listen();
    }

    private void listen() {
        FirebaseHelper.userRoot().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Hike> hikes = new ArrayList<>();
                for (DataSnapshot s : snapshot.child("hikes").getChildren()) {
                    Hike h = s.getValue(Hike.class);
                    if (h != null) hikes.add(h);
                }
                
                List<Observation> notes = new ArrayList<>();
                for (DataSnapshot s : snapshot.child("observations").getChildren()) {
                    Observation o = s.getValue(Observation.class);
                    if (o != null) notes.add(o);
                }
                
                bind(hikes, notes);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("MHike", "stats listener cancelled: " + error.getMessage());
            }
        });
    }

    private void bind(List<Hike> hikes, List<Observation> notes) {
        ((TextView) findViewById(R.id.txtStatSummary))
                .setText(getString(R.string.notes_count_fmt, notes.size(), hikes.size()));

        double totalKm = 0;
        int easy = 0, mod = 0, hard = 0;
        for (Hike h : hikes) {
            totalKm += h.lengthKm;
            if ("Easy".equals(h.difficulty)) easy++;
            else if ("Moderate".equals(h.difficulty)) mod++;
            else if ("Hard".equals(h.difficulty)) hard++;
        }

        // Completion comes from the stored status, not a demo constant.
        int completed = 0;
        for (Hike h : hikes) if (isCompleted(h)) completed++;

        ProgressBar circle = findViewById(R.id.progressCircle);
        TextView txtF = findViewById(R.id.txtCompletionFraction);
        circle.setProgress(hikes.isEmpty() ? 0 : Math.round(completed * 100f / hikes.size()));
        txtF.setText(getString(R.string.completion_fmt, completed, hikes.size()));

        ((TextView) findViewById(R.id.txtTotalKmPlanned))
                .setText(getString(R.string.km_planned_total_fmt, fmt(totalKm)));

        updateDifficultyBar(R.id.barEasy, R.id.txtCountEasy, easy, hikes.size());
        updateDifficultyBar(R.id.barModerate, R.id.txtCountModerate, mod, hikes.size());
        updateDifficultyBar(R.id.barHard, R.id.txtCountHard, hard, hikes.size());
        buildMonthlyChart(hikes);
    }

    private static boolean isCompleted(Hike h) {
        return h.status != null && "completed".equalsIgnoreCase(h.status.trim());
    }

    /** Kilometres per month over the trailing six months, newest on the right. */
    private void buildMonthlyChart(List<Hike> hikes) {
        LinearLayout chart = findViewById(R.id.chartContainer);
        if (chart == null) return;
        chart.removeAllViews();

        Calendar cal = Calendar.getInstance();
        String[] keys = new String[6];
        String[] labels = new String[6];
        double[] km = new double[6];
        SimpleDateFormat key = new SimpleDateFormat("yyyy-MM", Locale.UK);
        SimpleDateFormat label = new SimpleDateFormat("MMM", Locale.UK);

        cal.add(Calendar.MONTH, -5);
        for (int i = 0; i < 6; i++) {
            keys[i] = key.format(cal.getTime());
            labels[i] = label.format(cal.getTime());
            cal.add(Calendar.MONTH, 1);
        }

        for (Hike h : hikes) {
            if (h.hikeDate == null || h.hikeDate.length() < 7) continue;
            String month = h.hikeDate.substring(0, 7);
            for (int i = 0; i < 6; i++) {
                if (keys[i].equals(month)) { km[i] += h.lengthKm; break; }
            }
        }

        double max = 0;
        for (double v : km) max = Math.max(max, v);

        for (int i = 0; i < 6; i++) {
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.BOTTOM);
            col.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1f));

            TextView value = new TextView(this);
            value.setText(km[i] == 0 ? "" : fmt(km[i]));
            value.setTextSize(11);
            value.setTextColor(getColor(R.color.mh_text_muted));

            View bar = new View(this);
            // Zero months keep a 2dp stub so the baseline stays readable.
            int h = max == 0 ? dp(2) : Math.max(dp(2), (int) (dp(130) * (km[i] / max)));
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(26), h);
            blp.topMargin = dp(4);
            bar.setLayoutParams(blp);
            bar.setBackgroundColor(getColor(km[i] > 0 ? R.color.mh_accent : R.color.mh_fill2));

            TextView name = new TextView(this);
            name.setText(labels[i]);
            name.setTextSize(11);
            name.setTextColor(getColor(R.color.mh_text_muted));
            name.setPadding(0, dp(6), 0, 0);

            col.addView(value);
            col.addView(bar);
            col.addView(name);
            chart.addView(col);
        }
    }

    private void updateDifficultyBar(int barId, int txtId, int count, int total) {
        ViewGroup.LayoutParams lp = findViewById(barId).getLayoutParams();
        if (total > 0) {
            lp.height = (int) (dp(120) * ((float) count / total));
        } else {
            lp.height = 0;
        }
        findViewById(barId).setLayoutParams(lp);
        ((TextView) findViewById(txtId)).setText(String.valueOf(count));
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private String fmt(double d) {
        if (d == Math.rint(d)) return String.valueOf((long) d);
        return String.format(Locale.UK, "%.1f", d);
    }
}
