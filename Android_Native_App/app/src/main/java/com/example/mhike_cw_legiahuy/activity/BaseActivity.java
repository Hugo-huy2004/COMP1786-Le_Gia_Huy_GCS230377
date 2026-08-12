package com.example.mhike_cw_legiahuy.activity;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mhike_cw_legiahuy.util.Prefs;

import java.util.Locale;

public abstract class BaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        String lang = Prefs.lang(newBase);
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Resources res = newBase.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        
        super.attachBaseContext(newBase.createConfigurationContext(config));
    }
}
