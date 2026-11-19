package fr.famille.modeenfant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Écran parent : 3 étapes visibles d'un coup (service, applis, durée) et un gros bouton « Lancer ». */
public class ParentActivity extends Activity {
    private static final int[] PRESETS = {15, 30, 45, 60, 90};
    /** Après 2 min en arrière-plan, on redemande le code (sinon un enfant trouverait l'écran ouvert). */
    private static final long RELOCK_MS = 2 * 60_000L;
    private static final int[] LANG_BTNS = {R.id.lang_auto, R.id.lang_fr, R.id.lang_en, R.id.lang_es};
    private static final String[] LANG_CODES = {Locales.SYSTEM, "fr", "en", "es"};

    private Prefs prefs;
    private View content;
    private TextView serviceStatus, appsCount, durationText;
    private Button serviceBtn, startBtn;
    private View styleYoungRow, styleMatureRow;
    private MediaPlayer preview;
    private boolean unlocked;
    private long stoppedAt;
    private int minutes;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Locales.wrap(base));
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_parent);
        prefs = Prefs.get(this);
        minutes = prefs.durationMin();

        content = findViewById(R.id.content);
        serviceStatus = findViewById(R.id.service_status);
        appsCount = findViewById(R.id.apps_count);
        durationText = findViewById(R.id.duration);
        serviceBtn = findViewById(R.id.service_btn);
        startBtn = findViewById(R.id.start_btn);

        serviceBtn.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        findViewById(R.id.apps_btn).setOnClickListener(v -> startActivity(new Intent(this, AppListActivity.class)));
        findViewById(R.id.minus_btn).setOnClickListener(v -> setMinutes(minutes - 5));
        findViewById(R.id.plus_btn).setOnClickListener(v -> setMinutes(minutes + 5));
        startBtn.setOnClickListener(v -> start());
        findViewById(R.id.pin_btn).setOnClickListener(v -> PinPad.createPin(this, true,
                () -> Toast.makeText(this, R.string.pin_changed, Toast.LENGTH_SHORT).show()));

        CheckBox voice = findViewById(R.id.voice);
        voice.setChecked(prefs.voiceEnabled());
        voice.setOnCheckedChangeListener((b, on) -> prefs.setVoiceEnabled(on));

        styleYoungRow = findViewById(R.id.style_young_row);
        styleMatureRow = findViewById(R.id.style_mature_row);
        ((ImageView) findViewById(R.id.style_young_img))
                .setImageResource(Ui.drawableForTier(this, "mascotte_actif", 0));
        ((ImageView) findViewById(R.id.style_mature_img))
                .setImageResource(Ui.drawableForTier(this, "mascotte_actif", 1));
        styleYoungRow.setOnClickListener(v -> setMaturity(0));
        styleMatureRow.setOnClickListener(v -> setMaturity(1));
        findViewById(R.id.preview_young).setOnClickListener(v -> playPreview(0));
        findViewById(R.id.preview_mature).setOnClickListener(v -> playPreview(1));

        for (int i = 0; i < LANG_BTNS.length; i++) {
            final String code = LANG_CODES[i];
            findViewById(LANG_BTNS[i]).setOnClickListener(v -> setLanguage(code));
        }

        LinearLayout presets = findViewById(R.id.presets);
        for (int m : PRESETS) {
            Button b = new Button(this);
            b.setText(m + " min");
            b.setOnClickListener(v -> setMinutes(m));
            presets.addView(b);
        }

        lock();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (unlocked && stoppedAt > 0 && SystemClock.elapsedRealtime() - stoppedAt > RELOCK_MS) {
            unlocked = false;
            content.setVisibility(View.INVISIBLE);
            lock();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (unlocked) render();
    }

    @Override
    protected void onStop() {
        stoppedAt = SystemClock.elapsedRealtime();
        super.onStop();
    }

    private void lock() {
        if (!prefs.hasPin()) PinPad.createPin(this, false, this::unlock);
        else PinPad.askParent(this, this::unlock, this::finish);
    }

    private void unlock() {
        unlocked = true;
        content.setVisibility(View.VISIBLE);
        render();
    }

    private void render() {
        boolean on = GuardService.instance != null;
        serviceStatus.setText(on ? R.string.service_on : R.string.service_needed);
        serviceStatus.setTextColor(getColor(on ? R.color.ok : R.color.warn));
        serviceBtn.setVisibility(on ? View.GONE : View.VISIBLE);

        int n = prefs.allowedApps().size();
        appsCount.setText(n == 0 ? getString(R.string.apps_none) : getString(R.string.apps_count, n));
        durationText.setText(minutes + " min");
        startBtn.setEnabled(on);
        startBtn.setAlpha(on ? 1f : 0.5f);

        int tier = prefs.maturity();
        styleYoungRow.setBackground(Ui.rounded(this,
                tier == 0 ? 0x33FB8C00 : 0xFFF5F6FA, 12));
        styleMatureRow.setBackground(Ui.rounded(this,
                tier == 1 ? 0x33FB8C00 : 0xFFF5F6FA, 12));

        String lang = prefs.language();
        for (int i = 0; i < LANG_BTNS.length; i++) {
            ((Button) findViewById(LANG_BTNS[i])).setAlpha(LANG_CODES[i].equals(lang) ? 1f : 0.5f);
        }
    }

    private void setMaturity(int tier) {
        prefs.setMaturity(tier);
        render();
    }

    private void setLanguage(String code) {
        if (code.equals(prefs.language())) return;
        prefs.setLanguage(code);
        recreate();
    }

    /** « Bip-bip ! Coucou ! » du tier choisi, dans la langue choisie. */
    private void playPreview(int tier) {
        stopPreview();
        int res = Ui.rawForTier(this, "apercu", tier);
        if (res == 0) return;
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();
        preview = MediaPlayer.create(this, res, attrs, 0);
        if (preview == null) return;
        preview.setOnCompletionListener(mp -> stopPreview());
        preview.start();
    }

    private void stopPreview() {
        if (preview != null) {
            preview.release();
            preview = null;
        }
    }

    @Override
    protected void onDestroy() {
        stopPreview();
        super.onDestroy();
    }

    private void setMinutes(int m) {
        minutes = Math.max(5, Math.min(240, m));
        prefs.setDurationMin(minutes);
        render();
    }

    private void start() {
        if (GuardService.instance == null) {
            Toast.makeText(this, R.string.service_starting, Toast.LENGTH_SHORT).show();
            render();
            return;
        }
        if (prefs.allowedApps().isEmpty()) {
            new AlertDialog.Builder(this).setMessage(R.string.no_apps_confirm)
                    .setPositiveButton(android.R.string.ok, (d, w) -> doStart())
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {
            doStart();
        }
    }

    private void doStart() {
        GuardService s = GuardService.instance;
        if (s == null) return;
        s.startSession(minutes * 60_000L);
        finish();
    }
}
