package fr.famille.modeenfant;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Liste des applis lançables, avec icônes gardées en mémoire (le chargement est lent sur les vieilles tablettes). */
final class AppCache {
    static final class Entry {
        final String pkg;
        final String label;
        Drawable icon;

        Entry(String pkg, String label) {
            this.pkg = pkg;
            this.label = label;
        }
    }

    private static final Map<String, Drawable> ICONS = new ConcurrentHashMap<>();

    private AppCache() {}

    /** À appeler hors du thread UI. Si {@code only} n'est pas null, ne garde que ces paquets. */
    static List<Entry> launchables(Context c, Set<String> only) {
        PackageManager pm = c.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        Map<String, Entry> byPkg = new HashMap<>();
        for (ResolveInfo ri : pm.queryIntentActivities(main, 0)) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(c.getPackageName()) || byPkg.containsKey(pkg)) continue;
            if (only != null && !only.contains(pkg)) continue;
            byPkg.put(pkg, new Entry(pkg, ri.loadLabel(pm).toString()));
        }
        List<Entry> list = new ArrayList<>(byPkg.values());
        final Collator collator = Collator.getInstance(Locale.FRENCH);
        Collections.sort(list, (a, b) -> collator.compare(a.label, b.label));
        for (Entry e : list) e.icon = icon(pm, e.pkg);
        return list;
    }

    private static Drawable icon(PackageManager pm, String pkg) {
        Drawable d = ICONS.get(pkg);
        if (d == null) {
            try {
                d = pm.getApplicationIcon(pkg);
            } catch (PackageManager.NameNotFoundException e) {
                d = pm.getDefaultActivityIcon();
            }
            ICONS.put(pkg, d);
        }
        return d;
    }
}
