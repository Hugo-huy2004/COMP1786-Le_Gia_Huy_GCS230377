package com.example.mhike_cw_legiahuy.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.example.mhike_cw_legiahuy.R;
import com.example.mhike_cw_legiahuy.db.FirebaseHelper;
import com.example.mhike_cw_legiahuy.model.User;
import com.example.mhike_cw_legiahuy.util.PlanBuilder;
import com.example.mhike_cw_legiahuy.util.Prefs;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TrainingPlanActivity extends BaseActivity {

    private TextView txtBmiValue, txtBmiCategory, txtBmiTarget, txtBmiRange;
    private TextView txtScheduleDetail, txtLoggedSessions, txtWeekLabel;
    private LinearLayout week1Container;
    private final List<Session> sessions = new ArrayList<>();
    private String planStart;
    private User profile;
    private Boolean hasPlan;
    private long storedEta = -1;
    private boolean building;
    private int perWeek = PlanBuilder.PER_WEEK, weeks = PlanBuilder.WEEKS, startMinutes = PlanBuilder.START_MINUTES;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_plan);

        txtBmiValue = findViewById(R.id.txtBmiValue);
        txtBmiCategory = findViewById(R.id.txtBmiCategory);
        txtBmiTarget = findViewById(R.id.txtBmiTarget);
        txtBmiRange = findViewById(R.id.txtBmiRange);
        txtScheduleDetail = findViewById(R.id.txtScheduleDetail);
        txtLoggedSessions = findViewById(R.id.txtLoggedSessions);
        txtWeekLabel = findViewById(R.id.txtWeekLabel);
        week1Container = findViewById(R.id.week1Container);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnEditPlan).setOnClickListener(v -> showEditDialog());
        findViewById(R.id.btnDeletePlan).setOnClickListener(v -> confirmDelete());

        loadData();
    }

    private void loadData() {
        FirebaseHelper.profile().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user == null) return;
                profile = user;
                bindBmi(user);
                autoBuildIfMissing();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        FirebaseHelper.plan().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bindPlanHeader(snapshot);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        FirebaseHelper.planSessions().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bindSessions(snapshot);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void bindPlanHeader(DataSnapshot plan) {
        hasPlan = plan.exists();
        // Edit stays available with no plan - it is the way to create one after a delete.
        findViewById(R.id.btnDeletePlan).setVisibility(hasPlan ? View.VISIBLE : View.GONE);
        if (!hasPlan) {
            txtScheduleDetail.setText(Prefs.planRemoved(this) ? getString(R.string.plan_removed_hint)
                    : getString(R.string.no_plan_yet));
            autoBuildIfMissing();
            return;
        }
        perWeek = (int) num(plan.child("per_week").getValue());
        weeks = (int) num(plan.child("weeks").getValue());
        startMinutes = (int) num(plan.child("start_minutes").getValue());
        if (startMinutes <= 0) startMinutes = PlanBuilder.START_MINUTES;
        planStart = str(plan.child("start").getValue());
        storedEta = num(plan.child("eta_weeks").getValue());

        txtScheduleDetail.setText(getString(R.string.schedule_detail_fmt, perWeek, weeks));
        refreshEta();
    }

    /**
     * The card promises a schedule built from the body profile, so the first visit with a usable
     * profile writes one. Both listeners call in; whichever arrives last does the work. A plan the
     * user deleted stays deleted - see {@link Prefs#planRemoved}.
     */
    private void autoBuildIfMissing() {
        if (building || profile == null || hasPlan == null || hasPlan) return;
        if (Prefs.planRemoved(this)) return;
        if (profile.heightCm <= 0 || profile.weightKg <= 0) {
            txtScheduleDetail.setText(R.string.need_body_profile);
            return;
        }
        building = true;
        writePlan(PlanBuilder.PER_WEEK, PlanBuilder.WEEKS, PlanBuilder.START_MINUTES);
    }

    private void writePlan(int perWeek, int weeks, int startMinutes) {
        Date weekStart = PlanBuilder.weekStart(new Date());
        Map<String, Object> plan = new HashMap<>();
        plan.put("per_week", perWeek);
        plan.put("weeks", weeks);
        plan.put("start_minutes", startMinutes);
        plan.put("start", new SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(weekStart));
        plan.put("eta_weeks", profile == null ? 0 : PlanBuilder.etaWeeks(profile.heightCm, profile.weightKg));

        Prefs.setPlanRemoved(this, false);
        FirebaseHelper.plan().setValue(plan);
        FirebaseHelper.planSessions().setValue(PlanBuilder.sessions(weekStart, perWeek, weeks, startMinutes));
    }

    /** Edit = change the shape and regenerate; the dialog says the logged sessions reset. */
    private void showEditDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        box.setPadding(pad, dp(8), pad, 0);

        EditText inPerWeek = numberField(R.string.lbl_per_week, perWeek);
        EditText inWeeks = numberField(R.string.lbl_weeks, weeks);
        EditText inMinutes = numberField(R.string.lbl_start_minutes, startMinutes);
        box.addView(inPerWeek);
        box.addView(inWeeks);
        box.addView(inMinutes);

        new AlertDialog.Builder(this)
                .setTitle(R.string.action_edit_plan)
                .setMessage(R.string.confirm_rebuild_plan)
                .setView(box)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_save_changes, (d, w) -> writePlan(
                        PlanBuilder.clampPerWeek(number(inPerWeek, PlanBuilder.PER_WEEK)),
                        PlanBuilder.clampWeeks(number(inWeeks, PlanBuilder.WEEKS)),
                        PlanBuilder.clampStartMinutes(number(inMinutes, PlanBuilder.START_MINUTES))))
                .show();
    }

    private EditText numberField(int hintRes, int value) {
        EditText e = new EditText(this);
        e.setHint(hintRes);
        e.setText(String.valueOf(value));
        e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        return e;
    }

    private static int number(EditText e, int fallback) {
        try {
            return Integer.parseInt(e.getText().toString().trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.confirm_delete_plan)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_delete, (d, w) -> {
                    // Remember the choice, otherwise the auto-builder recreates it on the next load.
                    Prefs.setPlanRemoved(this, true);
                    FirebaseHelper.plan().removeValue();
                    FirebaseHelper.planSessions().removeValue();
                })
                .show();
    }

    /**
     * The schedule itself does not depend on weight, so a changed body profile only moves the
     * estimate - rebuilding the sessions would throw away everything already logged.
     */
    private void refreshEta() {
        if (profile == null || profile.heightCm <= 0) return;
        long eta = PlanBuilder.etaWeeks(profile.heightCm, profile.weightKg);
        if (eta != storedEta) FirebaseHelper.plan().child("eta_weeks").setValue(eta);
    }

    private void bindSessions(DataSnapshot snap) {
        sessions.clear();
        int done = 0;
        for (DataSnapshot s : snap.getChildren()) {
            Session x = new Session();
            x.key = s.getKey();
            x.date = str(s.child("date").getValue());
            x.week = (int) num(s.child("week").getValue());
            x.minutes = num(s.child("target_minutes").getValue());
            x.done = truthy(s.child("done").getValue());
            if (x.done) done++;
            sessions.add(x);
        }
        Collections.sort(sessions, (a, b) -> a.date == null ? -1 : a.date.compareTo(b.date));

        txtLoggedSessions.setText(getString(R.string.sessions_logged_fmt, done, sessions.size()));
        renderSessions();
    }

    private void renderSessions() {
        week1Container.removeAllViews();
        if (sessions.isEmpty()) return;

        int current = sessions.get(sessions.size() - 1).week;
        for (Session s : sessions) {
            if (!s.done) { current = s.week; break; }
        }
        if (txtWeekLabel != null) txtWeekLabel.setText(getString(R.string.week_fmt, current));

        SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
        SimpleDateFormat out = new SimpleDateFormat("d MMM yyyy", Locale.UK);
        LayoutInflater inflater = LayoutInflater.from(this);

        int lastWeek = -1;
        for (Session s : sessions) {
            if (s.week != lastWeek) {
                week1Container.addView(weekHeader(s.week, s.week == current));
                lastWeek = s.week;
            } else {
                week1Container.addView(divider());
            }

            View v = inflater.inflate(R.layout.item_training_session, week1Container, false);
            TextView date = v.findViewById(R.id.txtDate);
            TextView duration = v.findViewById(R.id.txtDuration);
            ImageView status = v.findViewById(R.id.imgStatus);

            String label = s.date;
            try {
                Date d = in.parse(s.date);
                if (d != null) label = out.format(d);
            } catch (ParseException ignored) {}
            date.setText(label);
            duration.setText(getString(R.string.minutes_fmt, s.minutes));

            status.setImageResource(s.done ? R.drawable.ic_plus : 0);
            status.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getColor(s.done ? R.color.mh_diff_easy : R.color.mh_fill2)));

            // Tapping a session is the only way progress ever moves in this app.
            v.setOnClickListener(x -> toggleDone(s));

            week1Container.addView(v);
        }
    }

    private void toggleDone(Session s) {
        if (s.key == null) return;
        // The web app writes 0/1 here, so keep the same shape.
        FirebaseHelper.planSessions().child(s.key).child("done").setValue(s.done ? 0 : 1);
        Toast.makeText(this, getString(R.string.session_toggle_fmt, s.date,
                getString(s.done ? R.string.not_done : R.string.done)), Toast.LENGTH_SHORT).show();
    }

    private TextView weekHeader(int week, boolean isCurrent) {
        TextView t = new TextView(this);
        t.setText(getString(R.string.week_fmt, week));
        t.setTextSize(12);
        t.setLetterSpacing(0.08f);
        t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        t.setTextColor(getColor(isCurrent ? R.color.mh_accent : R.color.mh_text_muted));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = week1Container.getChildCount() == 0 ? 0 : dp(18);
        lp.bottomMargin = dp(4);
        t.setLayoutParams(lp);
        return t;
    }

    private View divider() {
        View div = new View(this);
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        lp.setMarginStart(dp(60));
        div.setLayoutParams(lp);
        div.setBackgroundColor(getColor(R.color.mh_divider));
        return div;
    }

    private static class Session {
        String key;
        String date;
        int week;
        long minutes;
        boolean done;
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

    private void bindBmi(User user) {
        txtBmiValue.setText(String.format(Locale.UK, "%.1f", user.bmi()));

        String category;
        int color;
        if (user.bmi() < 18.5) {
            category = "Underweight";
            color = getColor(R.color.mh_deep_sky);
        } else if (user.bmi() < 25) {
            category = "Healthy";
            color = getColor(R.color.mh_diff_easy);
        } else if (user.bmi() < 30) {
            category = "Overweight";
            color = getColor(R.color.mh_deep_amber);
        } else {
            category = "Obese";
            color = getColor(R.color.mh_deep_coral);
        }
        
        txtBmiCategory.setText(category);
        txtBmiValue.setTextColor(color);

        // Both lines below used to be fixed demo text in the layout.
        double m = user.heightCm / 100.0;
        txtBmiRange.setText(getString(R.string.bmi_range_fmt, 18.5 * m * m, 24.9 * m * m));

        long eta = PlanBuilder.etaWeeks(user.heightCm, user.weightKg);
        txtBmiTarget.setText(eta == 0
                ? getString(R.string.bmi_on_track)
                : getString(R.string.bmi_eta_fmt, eta, Math.round(eta / 4.3f)));
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
