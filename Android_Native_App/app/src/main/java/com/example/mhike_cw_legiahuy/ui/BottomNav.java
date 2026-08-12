package com.example.mhike_cw_legiahuy.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.mhike_cw_legiahuy.R;
import com.example.mhike_cw_legiahuy.activity.HikeListActivity;
import com.example.mhike_cw_legiahuy.activity.HomeActivity;
import com.example.mhike_cw_legiahuy.activity.SearchActivity;
import com.example.mhike_cw_legiahuy.activity.StatsActivity;
import com.example.mhike_cw_legiahuy.activity.ProfileActivity;

public final class BottomNav {
    private BottomNav() {}

    private static final int[] TAB_IDS   = { R.id.navHome, R.id.navHikes, R.id.navSearch, R.id.navStats, R.id.navProfile };
    private static final int[] ICON_IDS  = { R.id.navHomeIcon, R.id.navHikesIcon, R.id.navSearchIcon, R.id.navStatsIcon, R.id.navProfileIcon };
    private static final int[] LABEL_IDS = { R.id.navHomeLabel, R.id.navHikesLabel, R.id.navSearchLabel, R.id.navStatsLabel, R.id.navProfileLabel };
    private static final Class<?>[] TARGETS = { HomeActivity.class, HikeListActivity.class, SearchActivity.class, StatsActivity.class, ProfileActivity.class };

    public static void wire(Activity a, int activeIndex) {
        View firstTab = a.findViewById(R.id.navHome);
        if (firstTab == null) return;
        ViewGroup tabsRow = (ViewGroup) firstTab.getParent();
        if (tabsRow == null) return;
        final ViewGroup bar = (ViewGroup) tabsRow.getParent();
        if (bar == null) return;

        View old = a.findViewById(R.id.tabIndicator);
        if (old != null) {
            ViewGroup oldParent = (ViewGroup) old.getParent();
            if (oldParent != null) oldParent.removeView(old);
        }

        View indicator = new View(a);
        indicator.setId(R.id.tabIndicator);
        indicator.setBackgroundResource(R.drawable.mh_round_button);
        indicator.getBackground().setColorFilter(ContextCompat.getColor(a, R.color.mh_accent_tint), PorterDuff.Mode.SRC_IN);
        indicator.setVisibility(View.INVISIBLE);

        FrameLayout.LayoutParams indicatorLp = new FrameLayout.LayoutParams(0, 0);
        bar.addView(indicator, 0, indicatorLp);

        for (int i = 0; i < TAB_IDS.length; i++) {
            final int idx = i;
            boolean active = (i == activeIndex);

            View tab = a.findViewById(TAB_IDS[i]);
            if (tab == null) continue;
            ImageView icon = a.findViewById(ICON_IDS[i]);
            TextView label = a.findViewById(LABEL_IDS[i]);

            int tint = ContextCompat.getColor(a, active ? R.color.mh_accent : R.color.mh_text_muted);
            if (icon != null) icon.setColorFilter(tint, PorterDuff.Mode.SRC_IN);
            if (label != null) label.setTextColor(tint);

            tab.setOnClickListener(v -> {
                if (!active) go(a, TARGETS[idx]);
            });
        }

        final int fi = activeIndex;
        indicator.post(() -> positionIndicator(a, indicator, fi));
    }

    private static void positionIndicator(Activity a, View indicator, int activeIndex) {
        final View tab = a.findViewById(TAB_IDS[activeIndex]);
        if (tab == null) return;
        if (tab.getWidth() == 0 || tab.getHeight() == 0) {
            tab.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int l, int t, int r, int b,
                                           int ol, int ot, int or2, int ob) {
                    tab.removeOnLayoutChangeListener(this);
                    positionIndicator(a, indicator, activeIndex);
                }
            });
            return;
        }
        ViewGroup tabsRow = (ViewGroup) tab.getParent();

        int insetX = dp(a, 4), insetY = dp(a, 4);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) indicator.getLayoutParams();
        lp.width = tab.getWidth() - insetX * 2;
        lp.height = tab.getHeight() - insetY * 2;
        lp.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
        lp.topMargin = tabsRow.getTop() + tab.getTop() + insetY;
        indicator.setLayoutParams(lp);
        indicator.setTranslationX(tabsRow.getLeft() + tab.getLeft() + insetX);
        indicator.setAlpha(1f);
        indicator.setVisibility(View.VISIBLE);
    }

    private static int dp(Activity a, int v) {
        return Math.round(v * a.getResources().getDisplayMetrics().density);
    }

    private static void go(Activity a, Class<?> target) {
        Intent i = new Intent(a, target);
        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        a.startActivity(i);
        a.overridePendingTransition(0, 0);
    }
}
