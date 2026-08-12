package com.example.mhike_cw_legiahuy.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.mhike_cw_legiahuy.model.Hike;

import org.junit.Test;

public class HikeFilterTest {

    private static Hike hike(String name, String location, double km, String date) {
        Hike h = new Hike();
        h.name = name;
        h.location = location;
        h.lengthKm = km;
        h.hikeDate = date;
        return h;
    }

    @Test
    public void emptyFilterAcceptsEverything() {
        assertTrue(new HikeFilter().accepts(hike("Ben Nevis", "Fort William", 17, "2026-08-20")));
        assertTrue(new HikeFilter().accepts(hike(null, null, 0, null)));
    }

    @Test
    public void matchesTextCaseInsensitively() {
        HikeFilter f = new HikeFilter();
        f.name = "ben";
        assertTrue(f.accepts(hike("Ben Nevis", "Fort William", 17, "2026-08-20")));
        assertFalse(f.accepts(hike("Snowdon", "Fort William", 17, "2026-08-20")));
    }

    @Test
    public void distanceBoundsAreInclusive() {
        HikeFilter f = new HikeFilter();
        f.minKm = 5.0;
        f.maxKm = 10.0;
        assertTrue(f.accepts(hike("a", "b", 5, "2026-08-20")));
        assertTrue(f.accepts(hike("a", "b", 10, "2026-08-20")));
        assertFalse(f.accepts(hike("a", "b", 4.9, "2026-08-20")));
        assertFalse(f.accepts(hike("a", "b", 10.1, "2026-08-20")));
    }

    @Test
    public void dateRangeIsInclusiveAndRejectsUndated() {
        HikeFilter f = new HikeFilter();
        f.from = "2026-08-01";
        f.to = "2026-08-31";
        assertTrue(f.accepts(hike("a", "b", 1, "2026-08-01")));
        assertTrue(f.accepts(hike("a", "b", 1, "2026-08-31")));
        assertFalse(f.accepts(hike("a", "b", 1, "2026-07-31")));
        assertFalse(f.accepts(hike("a", "b", 1, "2026-09-01")));
        assertFalse(f.accepts(hike("a", "b", 1, null)));
    }
}
