package com.example.mhike_cw_legiahuy;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.mhike_cw_legiahuy.db.FirebaseHelper;
import com.example.mhike_cw_legiahuy.util.Prefs;
import com.google.firebase.FirebaseApp;

import java.util.Locale;

public class MHikeApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        FirebaseHelper.setUserKey(Prefs.googleId(this));
        applyLocale(this);
        AppCompatDelegate.setDefaultNightMode(Prefs.nightMode(this));
    }

    public static void applyLocale(Context context) {
        String lang = Prefs.lang(context);
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }
}
