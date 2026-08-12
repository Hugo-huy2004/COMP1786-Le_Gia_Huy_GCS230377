package com.example.mhike_cw_legiahuy.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class Prefs {
    private static final String FILE = "mhike";
    private static final String K_NIGHT = "night_mode";
    private static final String K_LANG = "app_lang";
    private static final String K_GOOGLE_ID = "google_id";
    private static final String K_NO_PLAN = "plan_removed";

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static int nightMode(Context c) {
        return sp(c).getInt(K_NIGHT, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static String lang(Context c) {
        return sp(c).getString(K_LANG, "en");
    }

    public static void setLang(Context c, String lang) {
        sp(c).edit().putString(K_LANG, lang).apply();
    }

    /**
     * Google account id ("sub"), the key the web app stores everything under in
     * /users/&lt;id&gt;. Not the Firebase Auth uid - the two differ for the same person.
     */
    public static String googleId(Context c) {
        return sp(c).getString(K_GOOGLE_ID, null);
    }

    public static void setGoogleId(Context c, String id) {
        sp(c).edit().putString(K_GOOGLE_ID, id).apply();
    }

    /** Set when the user deletes their plan, so the auto-builder does not put it straight back. */
    public static boolean planRemoved(Context c) {
        return sp(c).getBoolean(K_NO_PLAN, false);
    }

    public static void setPlanRemoved(Context c, boolean removed) {
        sp(c).edit().putBoolean(K_NO_PLAN, removed).apply();
    }

    public static void setNightMode(Context c, int mode) {
        if (mode == nightMode(c)) return;
        sp(c).edit().putInt(K_NIGHT, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }
}
