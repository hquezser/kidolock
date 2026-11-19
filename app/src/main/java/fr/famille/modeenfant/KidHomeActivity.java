package fr.famille.modeenfant;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** L'espace enfant : ses applis en grand, l'anneau de temps, la bulle mascotte et l'écran « C'est fini ». */
public class KidHomeActivity extends Activity {
    private static final long HOLD_MS = 1500;
    private static final int[] TILE_TONES = {
            R.color.success_soft, R.color.primary_soft, R.color.secondary_grey, R.color.accent_soft};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<AppCache.Entry> apps = new ArrayList<>();
    private BaseAdapter adapter;
    private TimerView timer;
    private TextView timeText, empty, bubbleText;
    private ImageView timerIcon, bubbleMascot;
    private View bubble, expiredPanel, lockBtn;
    private HoldRingView holdRing;
    private Set<String> shownApps;
    private ValueAnimator holdAnim;
    private boolean holdFired;

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
        timerIcon = findViewById(R.id.timer_icon);
        timeText = findViewById(R.id.time_text);
        bubble = findViewById(R.id.bubble);
        bubbleMascot = findViewById(R.id.bubble_mascot);
        bubbleText = findViewById(R.id.bubble_text);
        empty = findViewById(R.id.empty);
        expiredPanel = findViewById(R.id.expired_panel);
        lockBtn = findViewById(R.id.lock_btn);
        holdRing = findViewById(R.id.hold_ring);
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
                v.findViewById(R.id.tile_bg).setBackground(
                        Ui.rounded(KidHomeActivity.this,
                                KidHomeActivity.this.getColor(TILE_TONES[i % TILE_TONES.length]), 28));
                return v;
            }
        };
        GridView grid = findViewById(R.id.grid);
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((p, v, pos, id) -> launch(apps.get(pos).pkg));

        lockBtn.setOnTouchListener(this::onLockTouch);
        View parentBtn2 = findViewById(R.id.parent_btn2);
        parentBtn2.setOnClickListener(v -> Toast.makeText(this, R.string.parent_hint, Toast.LENGTH_SHORT).show());
        parentBtn2.setOnLongClickListener(v -> {
            openParentMenu();
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBars();
        GuardService s = GuardService.instance;
        if (s != null) s.onKidHomeVisible();
        loadApps();
        update();
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

        // États du design : vert > 5 min, orange ≤ 5 min, rouge ≤ 1 min, fin.
        int stateColor, bubbleBg, moodRes;
        String mascotState, mood;
        if (rem <= 0) {
            stateColor = R.color.destructive;
            bubbleBg = R.color.destructive_soft;
            moodRes = R.string.kid_mood_end;
            mascotState = "mascotte_fin";
        } else if (rem <= 60_000) {
            stateColor = R.color.destructive;
            bubbleBg = R.color.destructive_soft;
            moodRes = R.string.kid_mood_last;
            mascotState = "mascotte_fatigue";
        } else if (rem <= 300_000) {
            stateColor = R.color.accent;
            bubbleBg = R.color.accent_soft;
            moodRes = R.string.kid_mood_soon;
            mascotState = "mascotte_actif";
        } else {
            stateColor = R.color.success;
            bubbleBg = R.color.success_soft;
            moodRes = R.string.kid_mood_ok;
            mascotState = "mascotte_actif";
        }
        timer.setColorRes(stateColor);
        timerIcon.setColorFilter(getColor(stateColor));
        timeText.setTextColor(getColor(stateColor));
        mood = getString(moodRes);
        bubble.setBackground(Ui.rounded(this, getColor(bubbleBg), 24));
        bubbleText.setText(mood);
        bubbleText.setTextColor(getColor(stateColor));
        bubbleMascot.setImageResource(Ui.drawableFor(this, mascotState));

        expiredPanel.setVisibility(s.isExpired() ? View.VISIBLE : View.GONE);
        lockBtn.setVisibility(s.isExpired() ? View.GONE : View.VISIBLE);
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

    /** Appui long ~1,5 s sur le cadenas : l'anneau se remplit, puis le menu parent s'ouvre. */
    private boolean onLockTouch(View v, MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                holdFired = false;
                holdAnim = ValueAnimator.ofFloat(0f, 1f);
                holdAnim.setDuration(HOLD_MS);
                holdAnim.addUpdateListener(a -> holdRing.setProgress((float) a.getAnimatedValue()));
                holdAnim.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        if (!holdFired) {
                            holdFired = true;
                            openParentMenu();
                        }
                    }
                });
                holdAnim.start();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (holdAnim != null && holdAnim.isRunning()) {
                    holdAnim.cancel();
                    if (!holdFired) {
                        Toast.makeText(this, R.string.parent_hint, Toast.LENGTH_SHORT).show();
                    }
                }
                holdRing.setProgress(0f);
                return true;
            default:
                return true;
        }
    }

    private void openParentMenu() {
        PinPad.askParent(this, this::showParentActions, null);
    }

    /** Modale « Accès Parent » du design : +5 min / +15 min / arrêter. */
    private void showParentActions() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        int pad = Ui.dp(this, 24);
        box.setPadding(pad, pad, pad, pad / 2);

        TextView title = new TextView(this);
        title.setText(R.string.parents);
        title.setTextSize(24);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        title.setTextColor(getColor(R.color.text_main));
        title.setPadding(0, 0, 0, Ui.dp(this, 16));
        box.addView(title);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        String[] labels = {getString(R.string.add_5min), getString(R.string.add_15min)};
        long[] deltas = {5 * 60_000L, 15 * 60_000L};
        for (int i = 0; i < 2; i++) {
            TextView b = actionButton(labels[i], R.color.primary_soft, R.color.primary);
            final long d = deltas[i];
            b.setOnClickListener(v -> {
                GuardService s = GuardService.instance;
                if (s != null) s.addTime(d);
                update();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(i == 0 ? 0 : Ui.dp(this, 8), 0, i == 0 ? Ui.dp(this, 8) : 0, 0);
            row.addView(b, lp);
        }
        box.addView(row);

        TextView stop = actionButton(getString(R.string.end_session),
                R.color.destructive_soft, R.color.destructive);
        stop.setTextColor(getColor(R.color.destructive));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.topMargin = Ui.dp(this, 12);
        stop.setOnClickListener(v -> {
            GuardService s = GuardService.instance;
            if (s != null) s.endSession();
            else Prefs.get(this).saveSession(false, false, 0, 0, 0);
            startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        });
        box.addView(stop, slp);

        new AlertDialog.Builder(this).setView(box).show();
    }

    /** Bouton pill du dialogue parent : fond doux, texte coloré, coins arrondis. */
    private TextView actionButton(String label, int fillRes, int textRes) {
        TextView b = new TextView(this);
        b.setText(label);
        b.setTextSize(20);
        b.setTypeface(b.getTypeface(), android.graphics.Typeface.BOLD);
        b.setTextColor(getColor(textRes));
        b.setGravity(android.view.Gravity.CENTER);
        b.setBackground(Ui.rounded(this, getColor(fillRes), 16));
        b.setPadding(0, Ui.dp(this, 18), 0, Ui.dp(this, 18));
        b.setMinHeight(Ui.dp(this, 56));
        return b;
    }

    @SuppressWarnings("deprecation")
    private void hideSystemBars() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }
}
