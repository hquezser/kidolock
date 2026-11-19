package fr.famille.modeenfant;

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Tout l'état persistant de l'app : code parent, réglages, et session en cours (survit au redémarrage). */
final class Prefs {
    private final SharedPreferences sp;

    private Prefs(Context c) {
        sp = c.getSharedPreferences("mode_enfant", Context.MODE_PRIVATE);
    }

    static Prefs get(Context c) {
        return new Prefs(c.getApplicationContext());
    }

    // --- Code parent (stocké salé + haché, jamais en clair) ---

    boolean hasPin() {
        return sp.contains("pin_hash");
    }

    void setPin(String pin) {
        byte[] salt = new byte[8];
        new SecureRandom().nextBytes(salt);
        String s = hex(salt);
        sp.edit().putString("pin_salt", s).putString("pin_hash", sha256(s + pin)).apply();
    }

    boolean checkPin(String pin) {
        return sha256(sp.getString("pin_salt", "") + pin).equals(sp.getString("pin_hash", null));
    }

    // --- Réglages ---

    Set<String> allowedApps() {
        return new HashSet<>(sp.getStringSet("allowed", Collections.<String>emptySet()));
    }

    void setAllowedApps(Set<String> apps) {
        sp.edit().putStringSet("allowed", new HashSet<>(apps)).apply();
    }

    int durationMin() {
        return sp.getInt("duration", 30);
    }

    void setDurationMin(int min) {
        sp.edit().putInt("duration", min).apply();
    }

    boolean voiceEnabled() {
        return sp.getBoolean("voice", true);
    }

    void setVoiceEnabled(boolean on) {
        sp.edit().putBoolean("voice", on).apply();
    }

    // --- Session en cours ---

    boolean sessionActive() {
        return sp.getBoolean("s_active", false);
    }

    boolean sessionExpired() {
        return sp.getBoolean("s_expired", false);
    }

    long remainingMs() {
        return sp.getLong("s_remaining", 0);
    }

    long totalMs() {
        return sp.getLong("s_total", 0);
    }

    int warnedMask() {
        return sp.getInt("s_warned", 0);
    }

    void saveSession(boolean active, boolean expired, long remaining, long total, int warned) {
        sp.edit()
                .putBoolean("s_active", active)
                .putBoolean("s_expired", expired)
                .putLong("s_remaining", remaining)
                .putLong("s_total", total)
                .putInt("s_warned", warned)
                .apply();
    }

    private static String sha256(String s) {
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
