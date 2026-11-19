package fr.famille.modeenfant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Écran parent : 3 étapes visibles d'un coup (service, applis, durée) et un gros bouton « Lancer ». */
public class ParentActivity extends Activity {
    private static final int[] PRESETS = {15, 30, 45, 60, 90};
    /** Après 2 min en arrière-plan, on redemande le code (sinon un enfant trouverait l'écran ouvert). */
    private static final long RELOCK_MS = 2 * 60_000L;

    private Prefs prefs;
    private View content;
    private TextView serviceStatus, appsCount, durationText;
    private Button serviceBtn, startBtn, styleYoung, styleMature;
    private boolean unlocked;
    private long stoppedAt;
    private int minutes;

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

        styleYoung = findViewById(R.id.style_young);
        styleMature = findViewById(R.id.style_mature);
        styleYoung.setOnClickListener(v -> setMaturity(0));
        styleMature.setOnClickListener(v -> setMaturity(1));

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
        styleYoung.setAlpha(prefs.maturity() == 0 ? 1f : 0.5f);
        styleMature.setAlpha(prefs.maturity() == 1 ? 1f : 0.5f);
    }

    private void setMaturity(int tier) {
        prefs.setMaturity(tier);
        render();
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
