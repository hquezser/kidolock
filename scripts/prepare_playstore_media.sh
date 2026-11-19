#!/usr/bin/env bash
# Capture les screenshots Play Store via adb.
# Prérequis : tablette/émulateur connecté, KidoLock installé, PIN parent connu.
# Usage : PIN=1234 SERIAL=192.168.1.15:5555 ./scripts/prepare_playstore_media.sh
# Les coordonnées de tap sont des pourcentages de la résolution détectée —
# à ajuster une fois sur l'écran réel si besoin (variables TAP_* en tête).
set -euo pipefail
cd "$(dirname "$0")/.."

ADB="adb ${SERIAL:+-s $SERIAL}"
OUT="store_assets/screenshots"
mkdir -p "$OUT"
PKG="fr.famille.modeenfant"
PIN="${PIN:-}"

SIZE=$($ADB shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1)
W=${SIZE%x*}; H=${SIZE#*x}
echo "Résolution : ${W}x${H}"

# Pourcentages (x%, y%) — adapter après un premier essai.
T_PARENT_PIN="${T_PARENT_PIN:-50,18}"     # pavé PIN (premier chiffre approximatif)
T_PARENT_DOT="${T_PARENT_DOT:-50,30}"     # suite du PIN — même point si saisie auto
T_UNLOCK="${T_UNLOCK:-50,78}"             # déverrouillage éventuel
T_APPS="${T_APPS:-50,42}"                 # bouton « Choisir les applis »
T_BACK="${T_BACK:-6,6}"                   # retour
T_LAUNCH="${T_LAUNCH:-50,5}"              # « Lancer » dans la barre de titre
T_LOCK="${T_LOCK:-93,90}"                 # cadenas appui long
T_PIN_OK="${T_PIN_OK:-50,85}"             # valider PIN modale

tap() { local xy=(${1//,/ }); $ADB shell input tap $(( W * xy[0] / 100 )) $(( H * xy[1] / 100 )); }
swipe_hold() { local xy=(${1//,/ }); $ADB shell input swipe $(( W * xy[0] / 100 )) $(( H * xy[1] / 100 )) $(( W * xy[0] / 100 )) $(( H * xy[1] / 100 )) 1600; }
shot() { $ADB exec-out screencap -p > "$OUT/$1.png"; echo "→ $OUT/$1.png"; }

if [[ -z "$PIN" ]]; then
    echo "ERREUR : définis PIN=<code parent> pour saisir le pavé." >&2
    exit 1
fi

# 1. Mode démo : heure 12:00, batterie 100 %, zéro notification.
$ADB shell settings put global sysui_demo 1
$ADB shell am broadcast -a com.android.systemui.demo -e command enter
$ADB shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200
$ADB shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
$ADB shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
$ADB shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
sleep 1

# 2. Écran parent : lance l'app, saisit le PIN.
$ADB shell am start -n "$PKG/.ParentActivity"
sleep 2
for (( i=0; i<${#PIN}; i++ )); do
    # Position des chiffres sur le pavé : grille 3x4 — à calibrer selon PinPad.
    # Valeur par défaut : appui au centre du pavé + attente (saisie manuelle conseillée).
    tap "$T_PARENT_PIN"
    sleep 0.4
done
tap "$T_UNLOCK"
sleep 1.5
shot 01_config_parent

# 3. Sélecteur d'applications.
tap "$T_APPS"
sleep 1.5
shot 02_select_apps
tap "$T_BACK"
sleep 1

# 4. Session enfant : 5 min → état « Bientôt fini » immédiat (orange).
tap "$T_LAUNCH"
sleep 3
shot 03_session_enfant
sleep 2
shot 04_alerte_fin

# 5. Modale parent (appui long sur le cadenas + PIN).
swipe_hold "$T_LOCK"
sleep 1.5
shot 05_verrouillage_parent

# 6. Restaure la barre d'état et quitte la session si encore active.
$ADB shell input keyevent KEYCODE_BACK
$ADB shell am broadcast -a com.android.systemui.demo -e command exit
$ADB shell settings put global sysui_demo 0
echo "Terminé — vérifie les PNG dans $OUT/ puis ajuste les TAP_* si besoin."
