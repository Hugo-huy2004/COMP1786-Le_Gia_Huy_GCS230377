package com.example.mhike_cw_legiahuy.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class PlanBuilderTest {

    private static final SimpleDateFormat ISO = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);

    private static Date date(String iso) throws Exception {
        return ISO.parse(iso);
    }

    @Test
    public void weekStartIsTheMondayOfThatWeek() throws Exception {
        // 2026-08-12 is a Wednesday; 2026-08-16 is the Sunday that closes the same week.
        assertEquals("2026-08-10", ISO.format(PlanBuilder.weekStart(date("2026-08-12"))));
        assertEquals("2026-08-10", ISO.format(PlanBuilder.weekStart(date("2026-08-10"))));
        assertEquals("2026-08-10", ISO.format(PlanBuilder.weekStart(date("2026-08-16"))));
    }

    @Test
    public void minutesGrowEverySecondWeekAndStayCapped() {
        assertEquals(25, PlanBuilder.minutesForWeek(1));
        assertEquals(25, PlanBuilder.minutesForWeek(2));
        assertEquals(30, PlanBuilder.minutesForWeek(3));
        assertEquals(50, PlanBuilder.minutesForWeek(12));
        assertEquals(PlanBuilder.MAX_MINUTES, PlanBuilder.minutesForWeek(99));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void sessionsLandOnMonWedFriOfEachWeek() throws Exception {
        Map<String, Object> s = PlanBuilder.sessions(date("2026-08-10"));

        assertEquals(PlanBuilder.WEEKS * PlanBuilder.PER_WEEK, s.size());
        assertEquals("2026-08-10", ((Map<String, Object>) s.get("w01s1")).get("date"));
        assertEquals("2026-08-12", ((Map<String, Object>) s.get("w01s2")).get("date"));
        assertEquals("2026-08-14", ((Map<String, Object>) s.get("w01s3")).get("date"));
        // Week 12 starts 11 weeks (77 days) after week 1.
        assertEquals("2026-10-26", ((Map<String, Object>) s.get("w12s1")).get("date"));
        assertEquals(50, ((Map<String, Object>) s.get("w12s1")).get("target_minutes"));
        assertEquals(0, ((Map<String, Object>) s.get("w12s1")).get("done"));
    }

    @Test
    public void everySessionDayIsMondayWednesdayOrFriday() throws Exception {
        Calendar c = Calendar.getInstance();
        for (Object v : PlanBuilder.sessions(date("2026-08-12")).values()) {
            @SuppressWarnings("unchecked")
            String d = (String) ((Map<String, Object>) v).get("date");
            c.setTime(date(d));
            int day = c.get(Calendar.DAY_OF_WEEK);
            assertEquals("unexpected weekday for " + d, true,
                    day == Calendar.MONDAY || day == Calendar.WEDNESDAY || day == Calendar.FRIDAY);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void customShapeSpreadsSessionsAcrossTheWeek() throws Exception {
        Map<String, Object> s = PlanBuilder.sessions(date("2026-08-10"), 2, 4, 40);

        assertEquals(8, s.size());
        assertEquals("2026-08-10", ((Map<String, Object>) s.get("w01s1")).get("date")); // Monday
        assertEquals("2026-08-13", ((Map<String, Object>) s.get("w01s2")).get("date")); // Thursday
        assertEquals(40, ((Map<String, Object>) s.get("w01s1")).get("target_minutes"));
        assertEquals(45, ((Map<String, Object>) s.get("w03s1")).get("target_minutes"));
    }

    @Test
    public void clampsKeepAnEditedPlanSane() {
        assertEquals(1, PlanBuilder.clampPerWeek(0));
        assertEquals(7, PlanBuilder.clampPerWeek(99));
        assertEquals(52, PlanBuilder.clampWeeks(400));
        assertEquals(5, PlanBuilder.clampStartMinutes(-3));
        assertEquals(PlanBuilder.MAX_MINUTES, PlanBuilder.clampStartMinutes(999));
    }

    @Test
    public void etaIsZeroWhenAlreadyHealthy() {
        assertEquals(0, PlanBuilder.etaWeeks(180, 70));
        assertEquals(0, PlanBuilder.etaWeeks(0, 90));
        // 1.80 m tops out at 80.68 kg healthy; 100 kg is ~19.3 kg over, ~39 weeks at 0.5 kg/week.
        assertEquals(39, PlanBuilder.etaWeeks(180, 100));
    }
}
