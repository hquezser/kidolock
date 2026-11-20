# Mode Enfant

Contrôle parental minimaliste façon Kids Place : gratuit, hors ligne, sans pub ni compte.
APK d'environ 250 Ko (dont 200 Ko de voix) en Java pur, sans aucune dépendance. Validé sur : Nexus 10 / LineageOS 17.1 (Android 10),
compatible Android 8 et plus.

## Ce que ça fait

- **Session enfant à la demande** : le parent choisit la durée et lance la session. En dehors
  d'une session la tablette est **100 % normale** : pas de launcher imposé, rien n'est bloqué.
- **Applis autorisées** : pendant la session, l'enfant voit un écran avec ses applis en grand.
  Toute autre appli (launcher, réglages, store…) est masquée immédiatement et l'enfant revient
  sur son espace. Le volet de notifications est refermé.
- **Avertissements progressifs** à 10, 5, 2 et 1 min : un bandeau non bloquant s'affiche par-dessus
  le jeu, avec une phrase dite à voix haute (« Il te reste 5 minutes de tablette »). Les annonces
  sont pré-enregistrées dans l'APK et passent par le volume média : elles fonctionnent sans moteur
  de synthèse vocale et même en mode silencieux. Deux façons de les changer :
  `./tools/enregistrer.sh` pour enregistrer **ta propre voix** au micro du Mac, ou
  `VOIX="Jacques" ./tools/voix.sh` pour une voix de synthèse macOS. Dans les deux cas, les silences
  et le volume sont corrigés et un petit carillon est ajouté devant (`CARILLON=0` pour le retirer).
  Pendant la dernière minute, un petit compte à rebours apparaît en coin.
- **Disque de temps** (façon Time Timer) qui fond du vert au rouge, compréhensible sans savoir lire.
- **Fin douce** : l'écran « C'est fini ! 🌙 » s'affiche. Le parent peut ajouter 5 ou 15 min, ou arrêter.
- **Le temps est en pause quand l'écran est éteint.** Une session en cours reprend après un redémarrage.
- **Code parent à 4 chiffres** (pavé géant, blocage de 30 s après 5 erreurs). En cas d'oubli, il
  suffit de résoudre une multiplication comme 47 × 8, hors de portée d'un jeune enfant.

## Installer

```bash
./build.sh install      # build debug + adb install
./build.sh              # assembleDebug → app/build/outputs/apk/debug/
./build.sh release      # bundleRelease → AAB (signé si keystore.properties présent)
```

Le projet utilise **Gradle** (wrapper `./gradlew`, AGP 8.13.2, compileSdk/targetSdk 36) et le JDK 17.
Pour signer la release : copier `keystore.properties.example` en `keystore.properties` et générer
le keystore (voir l'en-tête du fichier). Sans lui, le bundle est produit non signé.
`JAVA_HOME` doit pointer vers un JDK 17+ (`export JAVA_HOME=/opt/homebrew/opt/openjdk@17`).

## Première utilisation (2 minutes)

1. Ouvre **Mode Enfant** et choisis un code parent.
2. **Étape 1** : touche « Ouvrir les réglages d'accessibilité », puis Mode Enfant → Activer.
   Il faut le faire une seule fois.
   (Android 13 et plus : si l'option est grisée, va dans Infos de l'appli → ⋮ → « Autoriser les
   paramètres restreints ».)
3. **Étape 2** : coche les applis de l'enfant.
4. **Étape 3** : choisis la durée, puis **▶ Lancer le mode enfant**.

Pendant la session, un **appui long sur le cadenas** (en haut à droite) demande le code puis
propose : +5 min, +15 min, choisir les applis, terminer.

## Comment c'est fait

| Fichier | Rôle |
|---|---|
| `GuardService.java` | Service d'accessibilité : minuteur, avertissements, rideau + retour vers l'espace enfant. Il ne lit **que** le nom de l'appli au premier plan, et rien hors session. |
| `KidHomeActivity.java` | Espace enfant : grille d'applis, disque de temps, écran de fin. |
| `ParentActivity.java` | Réglages en 3 étapes, sur un seul écran. |
| `AppListActivity.java` | Choix des applis autorisées. |
| `PinPad.java` | Pavé du code parent et question de secours. |

Pourquoi ce n'est pas lent :
- Le service ne fait rien en boucle. Il réagit seulement aux changements de fenêtre et sort
  immédiatement s'il n'y a pas de session.
- L'interface utilise les vues Android natives, sans bibliothèque.
- Les icônes sont chargées en arrière-plan puis gardées en mémoire.

Le **rideau** : quand une appli interdite passe au premier plan, un écran plein « On retourne à
ton espace… » s'affiche instantanément par-dessus. Android peut en effet retarder de quelques
secondes le retour d'une appli après un appui sur « Accueil », et le rideau évite que l'enfant
profite de ce délai.

## Limites connues (volontaires, pour rester simple)

- Ce n'est pas un coffre-fort. Un enfant très motivé peut contourner l'app (mode sans échec,
  adb). L'objectif est un cadre bienveillant, pas une prison.
- Un seul profil enfant et pas de quota journalier (le temps est fixé à chaque session).
- Une appli autorisée qui ouvre une autre appli (navigateur, store) renvoie l'enfant sur son
  espace. C'est le comportement voulu.

## Licence

[GPL-3.0](LICENSE) — voir le fichier LICENSE. Le code source est la documentation :
`src/` pour l'app, `tools/` pour tout ce qui génère les assets (voix, mascottes, icône).
