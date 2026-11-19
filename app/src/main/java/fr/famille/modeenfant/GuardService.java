package fr.famille.modeenfant;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Le gardien de session. Hors session il ne fait rien (retour immédiat sur chaque événement),
 * donc aucune gêne pour les adultes. Pendant une session :
 * - décompte du temps, en pause quand l'écran est éteint ;
 * - avertissements progressifs (bandeau + voix) à 10, 5, 2 et 1 min, puis compte à rebours ;
 * - toute appli non autorisée est masquée par un rideau, et l'enfant revient sur son espace.
 */
public class GuardService extends AccessibilityService {
    static GuardService instance;

    private static final long MIN = 60_000L;
    private static final long[] WARN_AT = {10 * MIN, 5 * MIN, 2 * MIN, MIN};
    /** Fenêtres système qu'une appli autorisée peut légitimement ouvrir (dialogues, permissions). */
    private static final Set<String> HELPERS = new HashSet<>(Arrays.asList(
            "android", "com.android.permissioncontroller", "com.google.android.permissioncontroller"));
    private static final String SYSTEM_UI = "com.android.systemui";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Prefs prefs;
    private WindowManager wm;
    private MediaPlayer voice;

    private boolean active, expired, ticking, ownInFront;
    private long remaining, total, lastTick, lastSave, lastLaunch;
    private int warned, launchRetries;
    private Set<String> allowed = Collections.emptySet();
    private String imePkg = "";

    private View curtain, banner, bubble;
    private TextView bubbleText;
    private TimerView bubbleTimer;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!ticking) return;
            onTick();
            if (ticking) handler.postDelayed(this, 1000);
        }
    };
    private final Runnable hideBanner = this::removeBanner;
    /** Si l'espace enfant n'est pas revenu au premier plan, on réessaie ; on abandonne le rideau au bout de ~15 s. */
    private final Runnable retryLaunch = () -> {
        if (curtain == null) return;
        if (++launchRetries > 5) {
            removeCurtain();
            return;
        }
        lastLaunch = 0;
        launchKidHome();
    };

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            if (Intent.ACTION_SCREEN_OFF.equals(i.getAction())) pause();
            else resume();
        }
    };

    // ------------------------------------------------------------------ cycle de vie

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Locales.wrap(base));
    }

    @Override
    protected void onServiceConnected() {
        instance = this;
        prefs = Prefs.get(this);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        IntentFilter f = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        f.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screenReceiver, f);

        // Reprise d'une session après redémarrage de la tablette.
        active = prefs.sessionActive();
        expired = prefs.sessionExpired();
        remaining = prefs.remainingMs();
        total = prefs.totalMs();
        warned = prefs.warnedMask();
        allowed = prefs.allowedApps();
        imePkg = currentIme();
        if (active) {
            if (isScreenOn()) resume();
            launchKidHome();
        }
    }

    @Override
    public void onDestroy() {
        pause();
        if (prefs != null) save();
        try {
            unregisterReceiver(screenReceiver);
        } catch (IllegalArgumentException ignored) {
        }
        stopVoice();
        removeCurtain();
        removeBanner();
        removeBubble();
        instance = null;
        super.onDestroy();
    }

    @Override
    public void onInterrupt() {}

    // ------------------------------------------------------------------ blocage

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (!active || e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || e.getPackageName() == null) return;
        String pkg = e.getPackageName().toString();

        if (pkg.equals(getPackageName())) {
            // Nos propres overlays émettent aussi des événements : seuls nos écrans comptent.
            CharSequence cls = e.getClassName();
            if (cls != null && cls.toString().startsWith(pkg + ".")) onKidHomeVisible();
            return;
        }
        if (pkg.equals(SYSTEM_UI)) {
            closeShade(); // volet de notifications / réglages rapides
            return;
        }
        if (HELPERS.contains(pkg) || pkg.equals(imePkg)) return;

        ownInFront = false;
        if (expired || !allowed.contains(pkg)) {
            showCurtain();
            launchKidHome();
        }
    }

    @SuppressWarnings("deprecation")
    private void closeShade() {
        if (Build.VERSION.SDK_INT >= 31) performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE);
        else sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }

    private void launchKidHome() {
        long now = SystemClock.uptimeMillis();
        if (now - lastLaunch < 500) return;
        lastLaunch = now;
        startActivity(new Intent(this, KidHomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        handler.removeCallbacks(retryLaunch);
        handler.postDelayed(retryLaunch, 2500);
    }

    /**
     * Rideau plein écran affiché instantanément par-dessus l'appli interdite : l'enfant ne peut
     * pas l'utiliser pendant que l'espace enfant revient (Android peut retarder ce retour de
     * quelques secondes après un appui sur « Accueil »).
     */
    private void showCurtain() {
        if (curtain != null) return;
        launchRetries = 0;
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setGravity(Gravity.CENTER);
        v.setBackgroundColor(getColor(R.color.kid_bg));
        v.addView(text("🏠", 96, getColor(R.color.kid_text)));
        v.addView(text(Locales.wrap(this).getString(R.string.curtain_text), 28, getColor(R.color.kid_text)));
        v.setOnClickListener(x -> {
            lastLaunch = 0;
            launchKidHome();
        });
        curtain = v;
        addOverlay(v, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER, 0, 0, true);
    }

    private void removeCurtain() {
        handler.removeCallbacks(retryLaunch);
        if (curtain != null) {
            removeOverlay(curtain);
            curtain = null;
        }
    }

    // ------------------------------------------------------------------ temps

    private void resume() {
        if (!active || expired || ticking) return;
        ticking = true;
        lastTick = SystemClock.elapsedRealtime();
        handler.postDelayed(tick, 1000);
    }

    private void pause() {
        if (!ticking) return;
        consume();
        ticking = false;
        handler.removeCallbacks(tick);
        save();
    }

    private void consume() {
        long now = SystemClock.elapsedRealtime();
        remaining = Math.max(0, remaining - (now - lastTick));
        lastTick = now;
    }

    private void onTick() {
        consume();
        for (int i = 0; i < WARN_AT.length; i++) {
            long at = WARN_AT[i];
            if ((warned & (1 << i)) == 0 && remaining <= at) {
                warned |= 1 << i;
                // Pas d'« encore 10 minutes » sur une session de 10 minutes.
                if (total >= at + MIN) warn(at);
            }
        }
        if (remaining <= MIN) showBubble();
        if (remaining <= 0) {
            expire();
            return;
        }
        if (SystemClock.elapsedRealtime() - lastSave > 15_000) save();
    }

    private void warn(long at) {
        int min = (int) (at / MIN);
        // Robot content pour les rappels, fatigué quand ça devient urgent.
        String state = min <= 2 ? "mascotte_fatigue" : "mascotte_actif";
        Context l10n = Locales.wrap(this);
        showBanner(Ui.drawableFor(this, state),
                min == 1 ? l10n.getString(R.string.warn_last_minute) : l10n.getString(R.string.warn_minutes, min));
        speak(Ui.rawFor(this, min == 10 ? "voix_10min" : min == 5 ? "voix_5min" : min == 2 ? "voix_2min" : "voix_1min"));
    }

    private void expire() {
        expired = true;
        ticking = false;
        remaining = 0;
        handler.removeCallbacks(tick);
        removeBubble();
        removeBanner();
        save();
        speak(Ui.rawFor(this, "voix_fin"));
        if (!ownInFront) {
            showCurtain();
            launchKidHome();
        }
    }

    private void save() {
        prefs.saveSession(active, expired, remaining, total, warned);
        lastSave = SystemClock.elapsedRealtime();
    }

    // ------------------------------------------------------------------ API pour les écrans

    boolean isActive() {
        return active;
    }

    boolean isExpired() {
        return expired;
    }

    long remainingMs() {
        if (ticking) consume();
        return remaining;
    }

    long totalMs() {
        return total;
    }

    void startSession(long ms) {
        active = true;
        expired = false;
        total = remaining = ms;
        warned = 0;
        allowed = prefs.allowedApps();
        imePkg = currentIme();
        save();
        if (isScreenOn()) resume();
        lastLaunch = 0;
        launchKidHome();
    }

    void addTime(long ms) {
        if (!active) return;
        if (ticking) consume();
        remaining += ms;
        total = remaining; // le disque repart plein
        expired = false;
        for (int i = 0; i < WARN_AT.length; i++) {
            if (WARN_AT[i] < remaining) warned &= ~(1 << i);
        }
        if (remaining > MIN) removeBubble();
        save();
        if (isScreenOn()) resume();
    }

    void endSession() {
        pause();
        active = false;
        expired = false;
        removeCurtain();
        removeBanner();
        removeBubble();
        save();
    }

    void reloadAllowed() {
        allowed = prefs.allowedApps();
    }

    void onKidHomeVisible() {
        ownInFront = true;
        removeCurtain();
    }

    // ------------------------------------------------------------------ avertissements

    private void showBanner(int mascotRes, String message) {
        removeBanner();
        LinearLayout v = new LinearLayout(this);
        v.setGravity(Gravity.CENTER_VERTICAL);
        int p = Ui.dp(this, 18);
        v.setPadding(p, p / 2, p + p / 2, p / 2);
        v.setBackground(Ui.rounded(this, 0xF2FFFFFF, 40));
        v.setElevation(Ui.dp(this, 8));
        ImageView mascot = new ImageView(this);
        mascot.setImageResource(mascotRes);
        int ms = Ui.dp(this, 52);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(ms, ms);
        mlp.setMargins(0, 0, Ui.dp(this, 10), 0);
        v.addView(mascot, mlp);
        TimerView t = new TimerView(this);
        t.setFraction(total > 0 ? remaining / (float) total : 0f);
        int s = Ui.dp(this, 56);
        v.addView(t, new LinearLayout.LayoutParams(s, s));
        TextView tv = text(message, 28, getColor(R.color.kid_text));
        tv.setPadding(Ui.dp(this, 16), 0, 0, 0);
        v.addView(tv);
        banner = v;
        addOverlay(v, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, Ui.dp(this, 36), false);
        handler.postDelayed(hideBanner, 8000);
    }

    private void removeBanner() {
        handler.removeCallbacks(hideBanner);
        if (banner != null) {
            removeOverlay(banner);
            banner = null;
        }
    }

    /** Petit compte à rebours discret en haut à droite pendant la dernière minute. */
    private void showBubble() {
        if (bubble == null) {
            LinearLayout v = new LinearLayout(this);
            v.setGravity(Gravity.CENTER_VERTICAL);
            int p = Ui.dp(this, 10);
            v.setPadding(p, p, p + p, p);
            v.setBackground(Ui.rounded(this, 0xE6FFFFFF, 32));
            v.setElevation(Ui.dp(this, 6));
            bubbleTimer = new TimerView(this);
            int s = Ui.dp(this, 40);
            v.addView(bubbleTimer, new LinearLayout.LayoutParams(s, s));
            bubbleText = text("", 24, getColor(R.color.kid_text));
            bubbleText.setPadding(p, 0, 0, 0);
            v.addView(bubbleText);
            bubble = v;
            addOverlay(v, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.END, Ui.dp(this, 16), Ui.dp(this, 16), false);
        }
        bubbleTimer.setFraction(remaining / (float) MIN);
        bubbleText.setText(Ui.formatRemaining(remaining));
    }

    private void removeBubble() {
        if (bubble != null) {
            removeOverlay(bubble);
            bubble = null;
        }
    }

    /**
     * Annonces pré-enregistrées (res/raw, voir tools/voix.sh) : pas besoin de moteur de synthèse
     * vocale sur la tablette. Volume média, donc audible même en mode silencieux.
     */
    private void speak(int rawRes) {
        if (!prefs.voiceEnabled()) return;
        stopVoice();
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();
        voice = MediaPlayer.create(Locales.wrap(this), rawRes, attrs, 0);
        if (voice == null) return;
        voice.setOnCompletionListener(mp -> stopVoice());
        voice.start();
    }

    private void stopVoice() {
        if (voice != null) {
            voice.release();
            voice = null;
        }
    }

    // ------------------------------------------------------------------ utilitaires

    private TextView text(String s, float sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private void addOverlay(View v, int w, int h, int gravity, int x, int y, boolean touchable) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(w, h,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | (touchable ? 0 : WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE),
                PixelFormat.TRANSLUCENT);
        lp.gravity = gravity;
        lp.x = x;
        lp.y = y;
        try {
            wm.addView(v, lp);
        } catch (RuntimeException ignored) {
        }
    }

    private void removeOverlay(View v) {
        try {
            wm.removeView(v);
        } catch (RuntimeException ignored) {
        }
    }

    private boolean isScreenOn() {
        return ((PowerManager) getSystemService(POWER_SERVICE)).isInteractive();
    }

    private String currentIme() {
        String ime = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        if (ime == null) return "";
        int slash = ime.indexOf('/');
        return slash > 0 ? ime.substring(0, slash) : ime;
    }
}
