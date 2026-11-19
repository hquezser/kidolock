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

    /**
     * Drawable par tier de maturité : tier 0 = nom de base, tier N = « base_tN ».
     * Repli sur la base si la variante n'existe pas encore.
     */
    static int drawableFor(Context c, String base) {
        int tier = Prefs.get(c).maturity();
        if (tier > 0) {
            int id = c.getResources().getIdentifier(base + "_t" + tier, "drawable", c.getPackageName());
            if (id != 0) return id;
        }
        return c.getResources().getIdentifier(base, "drawable", c.getPackageName());
    }

    /** Idem pour res/raw (annonces vocales). */
    static int rawFor(Context c, String name) {
        int tier = Prefs.get(c).maturity();
        if (tier > 0) {
            int id = c.getResources().getIdentifier(name + "_t" + tier, "raw", c.getPackageName());
            if (id != 0) return id;
        }
        return c.getResources().getIdentifier(name, "raw", c.getPackageName());
    }
}
