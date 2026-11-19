package fr.famille.modeenfant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** L'espace enfant : ses applis en grand, le disque de temps, et l'écran « C'est fini ». */
public class KidHomeActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<AppCache.Entry> apps = new ArrayList<>();
    private BaseAdapter adapter;
    private TimerView timer;
    private TextView timeText, empty;
    private View expiredPanel;
    private Set<String> shownApps;

    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            update();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Locales.wrap(base));
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_kid_home);
        timer = findViewById(R.id.timer);
        timeText = findViewById(R.id.time_text);
        empty = findViewById(R.id.empty);
        expiredPanel = findViewById(R.id.expired_panel);
        ((ImageView) findViewById(R.id.expired_mascot))
                .setImageResource(Ui.drawableFor(this, "mascotte_fin"));

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
                if (v == null) v = getLayoutInflater().inflate(R.layout.item_app_tile, parent, false);
                AppCache.Entry e = apps.get(i);
                ((ImageView) v.findViewById(R.id.icon)).setImageDrawable(e.icon);
                ((TextView) v.findViewById(R.id.label)).setText(e.label);
                return v;
            }
        };
        GridView grid = findViewById(R.id.grid);
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((p, v, pos, id) -> launch(apps.get(pos).pkg));

        // Appui long volontaire : un tap d'enfant n'ouvre rien.
        for (int id : new int[]{R.id.parent_btn, R.id.parent_btn2}) {
            View b = findViewById(id);
            b.setOnClickListener(v -> Toast.makeText(this, R.string.parent_hint, Toast.LENGTH_SHORT).show());
            b.setOnLongClickListener(v -> {
                openParentMenu();
                return true;
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBars();
        GuardService s = GuardService.instance;
        if (s != null) s.onKidHomeVisible();
        loadApps();
        handler.post(refresh);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refresh);
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }

    @Override
    public void onBackPressed() {
        // L'enfant reste sur son espace.
    }

    private void update() {
        GuardService s = GuardService.instance;
        if (s == null) {
            timeText.setText(R.string.service_off);
            return;
        }
        if (!s.isActive()) {
            finish();
            return;
        }
        long rem = s.remainingMs();
        timer.setFraction(rem / (float) Math.max(1, s.totalMs()));
        timeText.setText(Ui.formatRemaining(rem));
        expiredPanel.setVisibility(s.isExpired() ? View.VISIBLE : View.GONE);
    }

    private void loadApps() {
        Set<String> want = Prefs.get(this).allowedApps();
        if (want.equals(shownApps)) return;
        shownApps = want;
        new Thread(() -> {
            List<AppCache.Entry> list = AppCache.launchables(this, want);
            runOnUiThread(() -> {
                apps.clear();
                apps.addAll(list);
                adapter.notifyDataSetChanged();
                empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            });
        }).start();
    }

    private void launch(String pkg) {
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) return;
        try {
            startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException ignored) {
        }
    }

    private void openParentMenu() {
        PinPad.askParent(this, () -> {
            String[] items = {getString(R.string.add_5), getString(R.string.add_15),
                    getString(R.string.choose_apps), getString(R.string.end_session)};
            new AlertDialog.Builder(this).setTitle(R.string.parents).setItems(items, (d, which) -> {
                GuardService s = GuardService.instance;
                switch (which) {
                    case 0:
                        if (s != null) s.addTime(5 * 60_000L);
                        break;
                    case 1:
                        if (s != null) s.addTime(15 * 60_000L);
                        break;
                    case 2:
                        startActivity(new Intent(this, AppListActivity.class));
                        break;
                    case 3:
                        if (s != null) s.endSession();
                        else Prefs.get(this).saveSession(false, false, 0, 0, 0);
                        startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        finish();
                        return;
                }
                update();
            }).show();
        }, null);
    }

    @SuppressWarnings("deprecation")
    private void hideSystemBars() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }
}
