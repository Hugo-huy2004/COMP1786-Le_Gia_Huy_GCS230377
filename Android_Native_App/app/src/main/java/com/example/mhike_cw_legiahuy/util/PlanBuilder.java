package com.example.mhike_cw_legiahuy.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the walking-plan sessions the training screen has always described:
 * PER_WEEK sessions a week for WEEKS weeks, starting at START_MINUTES and growing
 * STEP_MINUTES every second week, capped at MAX_MINUTES.
 */
public final class PlanBuilder {

    public static final int PER_WEEK = 3, WEEKS = 12, START_MINUTES = 25, STEP_MINUTES = 5, MAX_MINUTES = 60;

    private PlanBuilder() {}

    /** Monday of the week containing {@code from}. */
    public static Date weekStart(Date from) {
        Calendar c = Calendar.getInstance();
        c.setTime(from);
        c.setFirstDayOfWeek(Calendar.MONDAY);
        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        return c.getTime();
    }

    public static int minutesForWeek(int week) {
        return minutesForWeek(week, START_MINUTES);
    }

    public static int minutesForWeek(int week, int startMinutes) {
        return Math.min(MAX_MINUTES, startMinutes + ((week - 1) / 2) * STEP_MINUTES);
    }

    public static Map<String, Object> sessions(Date weekStart) {
        return sessions(weekStart, PER_WEEK, WEEKS, START_MINUTES);
    }

    /**
     * Session nodes keyed w01s1 … wNNsM, ready to hand to Firebase. Sessions are spread evenly
     * across the week from Monday, so the default 3/week lands on Mon/Wed/Fri.
     */
    public static Map<String, Object> sessions(Date from, int perWeek, int weeks, int startMinutes) {
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
        Calendar c = Calendar.getInstance();
        c.setFirstDayOfWeek(Calendar.MONDAY);
        // Day offsets below count from Monday, so snap first - callers may pass any day.
        Date monday = weekStart(from);

        Map<String, Object> out = new LinkedHashMap<>();
        for (int week = 1; week <= weeks; week++) {
            int minutes = minutesForWeek(week, startMinutes);
            for (int i = 0; i < perWeek; i++) {
                c.setTime(monday);
                c.add(Calendar.DAY_OF_YEAR, (week - 1) * 7 + (i * 7) / perWeek);

                Map<String, Object> s = new HashMap<>();
                s.put("date", iso.format(c.getTime()));
                s.put("week", week);
                s.put("target_minutes", minutes);
                s.put("done", 0);
                out.put(String.format(Locale.UK, "w%02ds%d", week, i + 1), s);
            }
        }
        return out;
    }

    public static int clampPerWeek(int v) {
        return Math.max(1, Math.min(7, v));
    }

    public static int clampWeeks(int v) {
        return Math.max(1, Math.min(52, v));
    }

    public static int clampStartMinutes(int v) {
        return Math.max(5, Math.min(MAX_MINUTES, v));
    }

    /** Weeks to reach the top of a healthy BMI at a safe 0.5 kg/week. 0 when already there. */
    public static long etaWeeks(double heightCm, double weightKg) {
        double m = heightCm / 100.0;
        double healthyMax = 24.9 * m * m;
        if (m <= 0 || weightKg <= healthyMax) return 0;
        return Math.round((weightKg - healthyMax) / 0.5);
    }
}
