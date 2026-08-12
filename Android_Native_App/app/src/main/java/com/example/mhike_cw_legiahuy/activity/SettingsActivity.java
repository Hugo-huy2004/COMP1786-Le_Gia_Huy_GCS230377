package com.example.mhike_cw_legiahuy.activity;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mhike_cw_legiahuy.BuildConfig;
import com.example.mhike_cw_legiahuy.MHikeApp;
import com.example.mhike_cw_legiahuy.R;
import com.example.mhike_cw_legiahuy.db.FirebaseHelper;
import com.example.mhike_cw_legiahuy.util.Prefs;

import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.button.MaterialButtonToggleGroup;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        MaterialButtonToggleGroup toggleTheme = findViewById(R.id.toggleTheme);
        if (toggleTheme != null) {
            toggleTheme.setSaveEnabled(false);
            toggleTheme.check(idForMode(Prefs.nightMode(this)));
            toggleTheme.addOnButtonCheckedListener((g, id, checked) -> {
                if (!checked) return;
                int mode = modeForId(id);
                if (mode != Prefs.nightMode(this)) Prefs.setNightMode(this, mode);
            });
        }

        MaterialButtonToggleGroup toggleLang = findViewById(R.id.toggleLang);
        if (toggleLang != null) {
            toggleLang.setSaveEnabled(false);
            toggleLang.check("vi".equals(Prefs.lang(this)) ? R.id.btnVi : R.id.btnEn);
            toggleLang.addOnButtonCheckedListener((g, id, checked) -> {
                if (!checked) return;
                String lang = id == R.id.btnVi ? "vi" : "en";
                if (!lang.equals(Prefs.lang(this))) {
                    Prefs.setLang(this, lang);
                    MHikeApp.applyLocale(this);
                    recreate();
                }
            });
        }

        findViewById(R.id.rowDeleteAll).setOnClickListener(v -> confirmDeleteAll());

        ((TextView) findViewById(R.id.txtVersion)).setText(BuildConfig.APP_VERSION);
        ((TextView) findViewById(R.id.txtAuthor)).setText(BuildConfig.AUTHOR);
    }

    private int idForMode(int mode) {
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) return R.id.btnLight;
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) return R.id.btnDark;
        return R.id.btnAuto;
    }

    private int modeForId(int id) {
        if (id == R.id.btnLight) return AppCompatDelegate.MODE_NIGHT_NO;
        if (id == R.id.btnDark) return AppCompatDelegate.MODE_NIGHT_YES;
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    private void confirmDeleteAll() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.confirm_delete_data_1)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete_all_data, (d, w) -> new AlertDialog.Builder(this)
                        .setMessage(R.string.confirm_delete_data_2)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.delete_all_data, (d2, w2) -> {
                            FirebaseHelper.userRoot().removeValue();
                            Toast.makeText(this, R.string.all_data_deleted, Toast.LENGTH_SHORT).show();
                        })
                        .show())
                .show();
    }
}
