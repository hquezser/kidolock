package fr.famille.modeenfant;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

/**
 * Langue applicative choisie par le parent (« system » = locale système,
 * sinon fr/en/es forcé). wrap() reconfigure le contexte pour que les
 * ressources localisées (texts + raw) se résolvent dans la langue choisie.
 */
final class Locales {
    static final String SYSTEM = "system";

    private Locales() {}

    /** Contexte reconfiguré sur la langue choisie ; inchangé si « system ». */
    static Context wrap(Context base) {
        String lang = Prefs.get(base).language();
        if (lang == null || lang.isEmpty() || lang.equals(SYSTEM)) return base;
        Locale locale = toLocale(lang);
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(locale);
        if (Build.VERSION.SDK_INT >= 24) {
            config.setLocales(new LocaleList(locale));
        }
        return base.createConfigurationContext(config);
    }

    static Locale toLocale(String lang) {
        switch (lang) {
            case "fr": return Locale.FRENCH;
            case "es": return new Locale("es");
            default: return Locale.ENGLISH;
        }
    }
}
