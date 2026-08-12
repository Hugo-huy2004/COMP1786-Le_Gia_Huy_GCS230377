package com.example.mhike_cw_legiahuy.util;

import com.example.mhike_cw_legiahuy.R;

/** Difficulty → v3 semantic colour (Easy=green, Moderate=orange, Hard=red). Shared by list, detail, map. */
public final class DiffColor {
    private DiffColor() {}

    /** Solid value/text/pin colour res id. */
    public static int solid(String difficulty) {
        if ("Hard".equalsIgnoreCase(difficulty)) return R.color.mh_diff_hard;
        if ("Moderate".equalsIgnoreCase(difficulty)) return R.color.mh_diff_moderate;
        return R.color.mh_diff_easy; // Easy / null default
    }

    /** Light icon-tile / tag-background tint res id. */
    public static int tint(String difficulty) {
        if ("Hard".equalsIgnoreCase(difficulty)) return R.color.mh_tint_coral;
        if ("Moderate".equalsIgnoreCase(difficulty)) return R.color.mh_tint_amber;
        return R.color.mh_tint_mint;
    }
}
