package com.example.mhike_cw_legiahuy.db;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public final class FirebaseHelper {
    private static final String DB_URL = "https://mhike-legiahu-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private static FirebaseDatabase database;
    private static volatile String userKey;

    public static synchronized FirebaseDatabase getDb() {
        if (database == null) {
            database = FirebaseDatabase.getInstance(DB_URL);
            database.setPersistenceEnabled(true);
        }
        return database;
    }

    public static void setUserKey(String googleId) {
        userKey = googleId;
        if (googleId != null) {
            DatabaseReference user = getDb().getReference("users").child(googleId);
            user.keepSynced(true);
            user.child("hikes").keepSynced(true);
            user.child("observations").keepSynced(true);
        }
    }

    public static String userKey() {
        return userKey;
    }

    public static DatabaseReference userRoot() {
        if (userKey == null) {
            throw new IllegalStateException("User session not initialized");
        }
        return getDb().getReference("users").child(userKey);
    }

    public static DatabaseReference hikes() {
        return userRoot().child("hikes");
    }

    public static DatabaseReference observations() {
        return userRoot().child("observations");
    }

    public static DatabaseReference profile() {
        return userRoot().child("profile");
    }

    public static DatabaseReference plan() {
        return userRoot().child("plan");
    }

    public static DatabaseReference planSessions() {
        return userRoot().child("planSessions");
    }
}
