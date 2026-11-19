package fr.famille.modeenfant;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;

final class Ui {
    private Ui() {}

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static GradientDrawable rounded(Context c, int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    /** « 12 min » au-delà d'une minute, puis « 45 s ». */
    static String formatRemaining(long ms) {
        if (ms >= 60_000) return ((ms + 59_999) / 60_000) + " min";
        return ((ms + 999) / 1000) + " s";
    }
}
