package com.example.mhike_cw_legiahuy.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mhike_cw_legiahuy.R;
import com.example.mhike_cw_legiahuy.db.FirebaseHelper;
import com.example.mhike_cw_legiahuy.model.Hike;
import com.example.mhike_cw_legiahuy.model.User;
import com.example.mhike_cw_legiahuy.ui.BottomNav;
import com.example.mhike_cw_legiahuy.util.Prefs;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends BaseActivity {

    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        BottomNav.wire(this, 4);

        setupOption(R.id.optExplore, R.drawable.ic_mountain, R.string.opt_explore, v -> 
                startActivity(new Intent(this, MapActivity.class)));
        setupOption(R.id.optTraining, R.drawable.ic_calendar, R.string.opt_training, v -> 
                startActivity(new Intent(this, TrainingPlanActivity.class)));
        setupOption(R.id.optNotes, R.drawable.ic_pencil, R.string.opt_all_notes, v ->
                startActivity(new Intent(this, ObservationsActivity.class)));
        setupOption(R.id.optSettings, R.drawable.ic_book, R.string.opt_settings, v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        
        findViewById(R.id.btnSignOut).setOnClickListener(v -> signOut());
        findViewById(R.id.btnEditBody).setOnClickListener(v -> showEditBodyDialog());

        setupStat(R.id.statHikes, "0", "hikes", R.color.mh_diff_easy);
        setupStat(R.id.statKm, "0", "km", R.color.mh_accent);
        setupStat(R.id.statNotes, "0", "notes", R.color.mh_deep_coral);
        setupStat(R.id.statBmi, "0.0", "BMI", R.color.mh_deep_lavender);

        listenForData();
    }

    private void showEditBodyDialog() {
        if (currentUser == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Body Profile");
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 40);
        
        final EditText inputH = new EditText(this);
        inputH.setHint("Height (cm)");
        inputH.setText(String.valueOf(currentUser.heightCm));
        inputH.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(inputH);
        
        final EditText inputW = new EditText(this);
        inputW.setHint("Weight (kg)");
        inputW.setText(String.valueOf(currentUser.weightKg));
        inputW.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(inputW);
        
        final EditText inputA = new EditText(this);
        inputA.setHint("Age");
        inputA.setText(String.valueOf(currentUser.age));
        inputA.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(inputA);
        
        builder.setView(layout);
        builder.setPositiveButton("Update", (dialog, which) -> {
            try {
                double h = Double.parseDouble(inputH.getText().toString());
                double w = Double.parseDouble(inputW.getText().toString());
                int a = Integer.parseInt(inputA.getText().toString());
                
                currentUser.heightCm = h;
                currentUser.weightKg = w;
                currentUser.age = a;

                // Patch only these three: a setValue would drop gender/lastLoginAt,
                // which the web app owns.
                Map<String, Object> patch = new HashMap<>();
                patch.put("height_cm", h);
                patch.put("weight_kg", w);
                patch.put("age", a);
                FirebaseHelper.profile().updateChildren(patch);
                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignInClient client = GoogleSignIn.getClient(this, gso);
        client.signOut().addOnCompleteListener(task -> {
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finishAffinity();
        });
    }

    private void listenForData() {
        FirebaseHelper.userRoot().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int hikeCount = (int) snapshot.child("hikes").getChildrenCount();
                int noteCount = (int) snapshot.child("observations").getChildrenCount();
                double totalKm = 0;
                for (DataSnapshot s : snapshot.child("hikes").getChildren()) {
                    Hike h = s.getValue(Hike.class);
                    if (h != null) totalKm += h.lengthKm;
                }
                updateSummary(hikeCount, (int) totalKm, noteCount);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
        
        FirebaseHelper.profile().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    currentUser = user;
                    bindProfile(user);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("MHike", "profile listener cancelled: " + error.getMessage());
            }
        });
    }

    /** Every line in the body-profile card is derived from the stored height/weight. */
    private void bindProfile(User user) {
        double bmi = user.bmi();
        setText(R.id.txtUserName, user.name);
        setText(R.id.txtUserEmail, user.email);
        updateStatValue(R.id.statBmi, String.format(Locale.UK, "%.1f", bmi));
        ((TextView) findViewById(R.id.profileInitials)).setText(
                user.name != null && !user.name.isEmpty() ? user.name.substring(0, 1).toUpperCase() : "U");
        setText(R.id.txtBodyStats, String.format(Locale.UK, "%.0f cm · %.1f kg · %d yr",
                user.heightCm, user.weightKg, user.age));

        setText(R.id.txtBmiLine, String.format(Locale.UK, "BMI %.1f (%s)", bmi, categoryOf(bmi)));

        // Healthy weight band for this height, from the standard 18.5-24.9 BMI range.
        double m = user.heightCm / 100.0;
        double low = 18.5 * m * m, high = 24.9 * m * m;
        setText(R.id.txtBmiRange, String.format(Locale.UK,
                "Healthy range: %.1f–%.1f kg · General guidance only — not a substitute for medical advice.",
                low, high));

        TextView advice = findViewById(R.id.txtBmiAdvice);
        if (advice == null) return;
        if (user.weightKg > high) {
            advice.setText(String.format(Locale.UK,
                    "About %.1f kg above a healthy range for your height — ease in gradually and "
                            + "consider a chat with a health pro. Every step counts.", user.weightKg - high));
            advice.setVisibility(View.VISIBLE);
        } else if (user.weightKg < low) {
            advice.setText(String.format(Locale.UK,
                    "About %.1f kg below a healthy range for your height — build up gently.", low - user.weightKg));
            advice.setVisibility(View.VISIBLE);
        } else {
            advice.setVisibility(View.GONE);
        }
    }

    private static String categoryOf(double bmi) {
        if (bmi < 18.5) return "underweight";
        if (bmi < 25) return "healthy";
        if (bmi < 30) return "overweight";
        return "obese";
    }

    private void setText(int id, String value) {
        TextView v = findViewById(id);
        if (v != null) v.setText(value == null ? "" : value);
    }

    private void updateSummary(int hikes, int km, int notes) {
        updateStatValue(R.id.statHikes, String.valueOf(hikes));
        updateStatValue(R.id.statKm, String.valueOf(km));
        updateStatValue(R.id.statNotes, String.valueOf(notes));
    }

    private void setupStat(int viewId, String val, String label, int colorRes) {
        View v = findViewById(viewId);
        if (v == null) return;
        TextView txtVal = v.findViewById(R.id.txtStatValue);
        TextView txtLabel = v.findViewById(R.id.txtStatLabel);
        txtVal.setText(val);
        txtVal.setTextColor(getColor(colorRes));
        txtLabel.setText(label);
    }

    private void updateStatValue(int viewId, String val) {
        View v = findViewById(viewId);
        if (v != null) {
            TextView txtVal = v.findViewById(R.id.txtStatValue);
            if (txtVal != null) txtVal.setText(val);
        }
    }

    private void setupOption(int viewId, int iconRes, int labelRes, View.OnClickListener listener) {
        View v = findViewById(viewId);
        if (v == null) return;
        ImageView icon = v.findViewById(R.id.imgIcon);
        TextView label = v.findViewById(R.id.txtLabel);
        
        if (icon != null) icon.setImageResource(iconRes);
        if (label != null) label.setText(labelRes);
        v.setOnClickListener(listener);
    }
}
