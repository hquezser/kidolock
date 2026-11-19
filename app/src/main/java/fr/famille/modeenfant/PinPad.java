package fr.famille.modeenfant;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Random;

/**
 * Gros pavé numérique pour le code parent (pas de clavier système).
 * « Code oublié ? » bascule sur une multiplication, hors de portée d'un jeune enfant.
 */
final class PinPad {
    private interface OnValue {
        /** @return true pour fermer le pavé */
        boolean accept(String value);
    }

    private static int failures;
    private static long lockedUntil;

    private final AlertDialog dialog;
    private final TextView title, display, message;
    private final Button forgot;
    private final StringBuilder input = new StringBuilder();
    private int length = 4;
    private boolean masked = true;
    private OnValue onValue;

    private PinPad(Activity a, boolean cancelable, Runnable onCancel) {
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = Ui.dp(a, 20);
        root.setPadding(pad, pad, pad, pad / 2);

        title = label(a, 22);
        display = label(a, 40);
        message = label(a, 16);
        message.setTextColor(a.getColor(R.color.warn));
        root.addView(title);
        root.addView(display);
        root.addView(message);

        GridLayout grid = new GridLayout(a);
        grid.setColumnCount(3);
        String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "OK"};
        for (String k : keys) {
            Button b = new Button(a);
            b.setText(k);
            b.setTextSize(26);
            b.setOnClickListener(v -> press(k));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = Ui.dp(a, 92);
            lp.height = Ui.dp(a, 72);
            int m = Ui.dp(a, 4);
            lp.setMargins(m, m, m, m);
            grid.addView(b, lp);
        }
        root.addView(grid, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        forgot = new Button(a, null, android.R.attr.borderlessButtonStyle);
        forgot.setText(R.string.pin_forgot);
        forgot.setVisibility(View.GONE);
        root.addView(forgot);

        dialog = new AlertDialog.Builder(a).setView(root).setCancelable(cancelable).create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnCancelListener(d -> {
            if (onCancel != null) onCancel.run();
        });
    }

    /** Demande le code parent ; {@code onOk} est appelé si le code (ou la question de secours) est bon. */
    static void askParent(Activity a, Runnable onOk, Runnable onCancel) {
        Prefs prefs = Prefs.get(a);
        PinPad p = new PinPad(a, true, onCancel);
        p.setup(a.getString(R.string.pin_title), 4, true, v -> {
            long now = SystemClock.elapsedRealtime();
            if (now < lockedUntil) {
                p.message.setText(a.getString(R.string.pin_locked, (lockedUntil - now) / 1000 + 1));
                return false;
            }
            if (prefs.checkPin(v)) {
                failures = 0;
                onOk.run();
                return true;
            }
            if (++failures >= 5) {
                failures = 0;
                lockedUntil = now + 30_000;
                p.message.setText(a.getString(R.string.pin_locked, 30));
            } else {
                p.message.setText(R.string.pin_wrong);
            }
            return false;
        });
        p.forgot.setVisibility(View.VISIBLE);
        p.forgot.setOnClickListener(x -> p.askMath(a, onOk));
        p.dialog.show();
    }

    /** Création (ou changement) du code parent, saisi deux fois. */
    static void createPin(Activity a, boolean cancelable, Runnable onDone) {
        PinPad p = new PinPad(a, cancelable, null);
        String[] first = {null};
        p.setup(a.getString(R.string.pin_create), 4, true, v -> {
            if (first[0] == null) {
                first[0] = v;
                p.title.setText(R.string.pin_confirm);
                return false;
            }
            if (first[0].equals(v)) {
                Prefs.get(a).setPin(v);
                onDone.run();
                return true;
            }
            first[0] = null;
            p.title.setText(R.string.pin_create);
            p.message.setText(R.string.pin_mismatch);
            return false;
        });
        p.dialog.show();
    }

    private void askMath(Activity a, Runnable onOk) {
        Random r = new Random();
        int x = 12 + r.nextInt(80), y = 6 + r.nextInt(4);
        forgot.setVisibility(View.GONE);
        setup(a.getString(R.string.math_question, x, y), 4, false, v -> {
            if (v.equals(String.valueOf(x * y))) {
                onOk.run();
                return true;
            }
            message.setText(R.string.math_wrong);
            return false;
        });
    }

    private void setup(String t, int len, boolean mask, OnValue handler) {
        title.setText(t);
        length = len;
        masked = mask;
        onValue = handler;
        input.setLength(0);
        message.setText("");
        refresh();
    }

    private void press(String k) {
        message.setText("");
        if (k.equals("OK")) {
            submit();
            return;
        }
        if (k.equals("⌫")) {
            if (input.length() > 0) input.deleteCharAt(input.length() - 1);
        } else if (input.length() < length) {
            input.append(k);
        }
        refresh();
        if (masked && input.length() == length) submit(); // code complet : validation auto
    }

    private void submit() {
        if (input.length() == 0) return;
        String v = input.toString();
        input.setLength(0);
        refresh();
        if (onValue.accept(v)) dialog.dismiss();
    }

    private void refresh() {
        if (masked) {
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < length; i++) s.append(i < input.length() ? "● " : "○ ");
            display.setText(s.toString().trim());
        } else {
            display.setText(input.length() == 0 ? "_" : input);
        }
    }

    private static TextView label(Activity a, float sp) {
        TextView t = new TextView(a);
        t.setTextSize(sp);
        t.setGravity(Gravity.CENTER);
        t.setTextColor(a.getColor(R.color.text_main));
        t.setPadding(0, Ui.dp(a, 4), 0, Ui.dp(a, 4));
        return t;
    }
}
