package com.example.mhike_cw_legiahuy.util;

import com.example.mhike_cw_legiahuy.model.Hike;

import java.util.Locale;

/** Advanced-search criteria. Any null field means "don't care". */
public class HikeFilter {

    public String name, location, from, to;
    public Double minKm, maxKm;

    public boolean accepts(Hike h) {
        if (name != null && !contains(h.name, name)) return false;
        if (location != null && !contains(h.location, location)) return false;
        if (minKm != null && h.lengthKm < minKm) return false;
        if (maxKm != null && h.lengthKm > maxKm) return false;
        // Dates are stored as yyyy-MM-dd, so a string compare is already chronological.
        if (from != null && (h.hikeDate == null || h.hikeDate.compareTo(from) < 0)) return false;
        if (to != null && (h.hikeDate == null || h.hikeDate.compareTo(to) > 0)) return false;
        return true;
    }

    public static boolean contains(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase(Locale.UK).contains(needle.toLowerCase(Locale.UK));
    }
}
