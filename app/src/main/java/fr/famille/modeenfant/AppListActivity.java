package fr.famille.modeenfant;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Choix des applis autorisées : un tap = coché/décoché, enregistré tout de suite. */
public class AppListActivity extends Activity {
    private static final String SETTINGS_PKG = "com.android.settings";

    private final List<AppCache.Entry> apps = new ArrayList<>();
    private Prefs prefs;
    private Set<String> allowed;
    private BaseAdapter adapter;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Locales.wrap(base));
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_app_list);
        prefs = Prefs.get(this);
        allowed = prefs.allowedApps();

        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return apps.size();
            }

            @Override
            public Object getItem(int i) {
                return apps.get(i);
            }

            @Override
            public long getItemId(int i) {
                return i;
            }

            @Override
            public View getView(int i, View v, ViewGroup parent) {
                if (v == null) v = getLayoutInflater().inflate(R.layout.item_app_row, parent, false);
                AppCache.Entry e = apps.get(i);
                ((ImageView) v.findViewById(R.id.icon)).setImageDrawable(e.icon);
                ((TextView) v.findViewById(R.id.label)).setText(e.label);
                ((CheckBox) v.findViewById(R.id.check)).setChecked(allowed.contains(e.pkg));
                v.findViewById(R.id.note).setVisibility(SETTINGS_PKG.equals(e.pkg) ? View.VISIBLE : View.GONE);
                return v;
            }
        };
        ListView list = findViewById(R.id.list);
        list.setAdapter(adapter);
        findViewById(R.id.done_btn).setOnClickListener(v -> finish());
        list.setOnItemClickListener((p, v, pos, id) -> {
            String pkg = apps.get(pos).pkg;
            if (!allowed.remove(pkg)) allowed.add(pkg);
            prefs.setAllowedApps(allowed);
            adapter.notifyDataSetChanged();
        });

        View progress = findViewById(R.id.progress);
        new Thread(() -> {
            List<AppCache.Entry> all = AppCache.launchables(this, null);
            // Applis déjà autorisées en tête, puis ordre alphabétique (tri stable).
            Collections.sort(all, (a, b) -> Boolean.compare(!allowed.contains(a.pkg), !allowed.contains(b.pkg)));
            runOnUiThread(() -> {
                Set<String> valid = new HashSet<>();
                for (AppCache.Entry e : all) valid.add(e.pkg);
                allowed.retainAll(valid);
                prefs.setAllowedApps(allowed);
                apps.addAll(all);
                adapter.notifyDataSetChanged();
                progress.setVisibility(View.GONE);
            });
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        GuardService s = GuardService.instance;
        if (s != null) s.reloadAllowed();
    }
}
