# Contribuer à Mode Enfant

Mode Enfant est un contrôle parental open source sous GPL-3.0. Bugs, idées, traductions et
pull requests sont les bienvenus.

## Monter le projet

Prérequis : JDK 17+, Android SDK, et `JAVA_HOME` pointé vers le JDK
(ex. `export JAVA_HOME=/opt/homebrew/opt/openjdk@17`).

```bash
./build.sh              # assembleDebug → app/build/outputs/apk/debug/
./build.sh install      # build debug + installation sur l'appareil branché
./build.sh release      # bundleRelease → AAB (signé si keystore.properties présent)
```

Gradle wrapper inclus, aucune dépendance à installer à la main.
AGP 8.13.2, compileSdk/targetSdk 36, minSdk 26. Le code source est petit et lisible :
`src/main/java/fr/famille/modeenfant/` pour l'app, `tools/` pour les scripts de génération
d'assets.

## Conventions

- **Java pur, zéro dépendance.** L'APK fait ~250 Ko exprès. Toute nouvelle dépendance doit
  être justifiée dans la PR — la barre est haute, et « on peut le faire en 20 lignes » est
  souvent la bonne réponse.
- **Vues Android natives + XML**, pas de framework UI. Lis les fichiers voisins avant
  d'écrire : le style y est déjà établi.
- **Toute chaîne visible doit exister en FR, EN et ES** (`res/values`, `values-fr`,
  `values-es`, et les voix dans `raw`, `raw-es`). Pas de texte en dur.
- **Pas de tests automatisés** : le projet n'en contient pas. Vérifie à la main sur un
  appareil réel et décris ce que tu as testé dans la PR.
- Les assets (voix, mascottes, icône) sont générés par `tools/`
  (`enregistrer.sh`, `voix.sh`, `assembler_picsart.sh`). Ne committe pas de binaire qui ne
  peut pas être régénéré sans en discuter d'abord.

## Pull requests

- Décris le problème et la solution, pas seulement le diff.
- Une PR = un sujet. Les grosses refactorisations se discutent d'abord dans une issue.
- Ta contribution est publiée sous GPL-3.0 comme le reste du projet.

## Signaler un bug

Ouvre une issue avec le template « bug » : appareil, version d'Android, étapes pour
reproduire, comportement attendu.
