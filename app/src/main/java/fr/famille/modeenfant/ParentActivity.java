package fr.famille.modeenfant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Écran parent : service, applis, durée, options (voix, robot, langue) — design « tableau de bord ». */
public class ParentActivity extends Activity {
    private static final int[] PRESETS = {15, 30, 45, 60, 90};
    /** Après 2 min en arrière-plan, on redemande le code (sinon un enfant trouverait l'écran ouvert). */
    private static final long RELOCK_MS = 2 * 60_000L;
    private static final int[] LANG_BTNS = {R.id.lang_auto, R.id.lang_fr, R.id.lang_en, R.id.lang_es};
    private static final String[] LANG_CODES = {Locales.SYSTEM, "fr", "en", "es"};

    private Prefs prefs;
    private View content;
    private TextView serviceStatus, appsCount, durationText;
    private Button serviceBtn, startTopBtn;
    private View serviceRow;
    private MenuItem startItem;
    private View styleCardYoung, styleCardMature, checkYoung, checkMature, personaBox;
    private Switch voice;
    private final List<TextView> presetBtns = new ArrayList<>();
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
        serviceRow = findViewById(R.id.service_row);
        personaBox = findViewById(R.id.persona_box);
        startTopBtn = findViewById(R.id.start_top);
        startTopBtn.setOnClickListener(v -> start());

        serviceBtn.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.disclosure_title)
                .setMessage(R.string.disclosure_text)
                .setPositiveButton(R.string.disclosure_accept, (d, w) ->
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
                .setNegativeButton(R.string.disclosure_later, null)
                .show());
        findViewById(R.id.apps_btn).setOnClickListener(v -> startActivity(new Intent(this, AppListActivity.class)));
        findViewById(R.id.minus_btn).setOnClickListener(v -> setMinutes(minutes - 5));
        findViewById(R.id.plus_btn).setOnClickListener(v -> setMinutes(minutes + 5));
        findViewById(R.id.pin_btn).setOnClickListener(v -> PinPad.createPin(this, true,
                () -> Toast.makeText(this, R.string.pin_changed, Toast.LENGTH_SHORT).show()));
        findViewById(R.id.privacy_btn).setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse(getString(R.string.privacy_url)))));

        voice = findViewById(R.id.voice);
        voice.setChecked(prefs.voiceEnabled());
        voice.setOnCheckedChangeListener((b, on) -> {
            prefs.setVoiceEnabled(on);
            render();
        });

        styleCardYoung = findViewById(R.id.style_card_young);
        styleCardMature = findViewById(R.id.style_card_mature);
        checkYoung = findViewById(R.id.style_check_young);
        checkMature = findViewById(R.id.style_check_mature);
        ((ImageView) findViewById(R.id.style_young_img))
                .setImageResource(Ui.drawableForTier(this, "mascotte_actif", 0));
        ((ImageView) findViewById(R.id.style_mature_img))
                .setImageResource(Ui.drawableForTier(this, "mascotte_actif", 1));
        styleCardYoung.setOnClickListener(v -> setMaturity(0));
        styleCardMature.setOnClickListener(v -> setMaturity(1));
        findViewById(R.id.preview_young).setOnClickListener(v -> playPreview(0));
        findViewById(R.id.preview_mature).setOnClickListener(v -> playPreview(1));

        // « Auto (FR) » : la langue système résolue, indicative.
        String sys = Locale.getDefault().getLanguage();
        sys = sys.isEmpty() ? "?" : sys.substring(0, 1).toUpperCase(Locale.ROOT) + sys.substring(1);
        ((Button) findViewById(R.id.lang_auto)).setText(getString(R.string.lang_auto, sys.toUpperCase(Locale.ROOT)));
        for (int i = 0; i < LANG_BTNS.length; i++) {
            final String code = LANG_CODES[i];
            findViewById(LANG_BTNS[i]).setOnClickListener(v -> setLanguage(code));
        }

        LinearLayout presets = findViewById(R.id.presets);
        for (int m : PRESETS) {
            TextView b = new TextView(this);
            b.setTextSize(15);
            b.setTypeface(b.getTypeface(), android.graphics.Typeface.BOLD);
            b.setGravity(android.view.Gravity.CENTER);
            b.setPadding(Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12));
            b.setMinHeight(Ui.dp(this, 48));
            b.setOnClickListener(v -> setMinutes(m));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            int mg = Ui.dp(this, 4);
            lp.setMargins(mg, 0, mg, 0);
            presets.addView(b, lp);
            presetBtns.add(b);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.parent_menu, menu);
        startItem = menu.findItem(R.id.action_start);
        startItem.setEnabled(unlocked);
        if (unlocked) render();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_start) {
            start();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        serviceStatus.setText(on ? R.string.svc_active : R.string.svc_inactive);
        serviceStatus.setTextColor(getColor(on ? R.color.success : R.color.warn));
        serviceStatus.setBackground(Ui.rounded(this,
                getColor(on ? R.color.success_soft : R.color.destructive_soft), 16));
        serviceRow.setVisibility(on ? View.GONE : View.VISIBLE);

        int n = 0;
        for (String pkg : prefs.allowedApps()) {
            if (getPackageManager().getLaunchIntentForPackage(pkg) != null) n++;
        }
        appsCount.setText(n == 0 ? getString(R.string.apps_none) : getString(R.string.apps_count, n));
        durationText.setText(minutes + " min");
        startTopBtn.setEnabled(on);
        startTopBtn.setAlpha(on ? 1f : 0.5f);
        if (startItem != null) startItem.setEnabled(on && unlocked);

        // Cartes robot : sélection = fond bleu clair + bordure bleue + coche.
        int tier = prefs.maturity();
        styleCardYoung.setBackground(Ui.roundedStroke(this,
                getColor(tier == 0 ? R.color.primary_soft : android.R.color.white),
                getColor(tier == 0 ? R.color.primary : R.color.card_stroke), 16, tier == 0 ? 2 : 1));
        styleCardMature.setBackground(Ui.roundedStroke(this,
                getColor(tier == 1 ? R.color.primary_soft : android.R.color.white),
                getColor(tier == 1 ? R.color.primary : R.color.card_stroke), 16, tier == 1 ? 2 : 1));
        checkYoung.setVisibility(tier == 0 ? View.VISIBLE : View.GONE);
        checkMature.setVisibility(tier == 1 ? View.VISIBLE : View.GONE);

        // Section persona grisée quand les annonces vocales sont coupées (design).
        personaBox.setAlpha(voice.isChecked() ? 1f : 0.4f);

        // Langue : segment sélectionné = pastille blanche, sinon texte discret.
        String lang = prefs.language();
        for (int i = 0; i < LANG_BTNS.length; i++) {
            boolean sel = LANG_CODES[i].equals(lang);
            Button b = (Button) findViewById(LANG_BTNS[i]);
            b.setBackground(Ui.rounded(this,
                    sel ? getColor(android.R.color.white) : 0x00000000, 12));
            b.setTextColor(getColor(sel ? R.color.primary : R.color.text_soft));
        }

        // Presets : sélection = pastille bleue.
        for (int i = 0; i < PRESETS.length; i++) {
            boolean sel = minutes == PRESETS[i];
            TextView b = presetBtns.get(i);
            b.setText(PRESETS[i] + " min");
            b.setBackground(Ui.rounded(this,
                    getColor(sel ? R.color.primary : R.color.secondary_grey), 24));
            b.setTextColor(getColor(sel ? android.R.color.white : R.color.text_main));
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
