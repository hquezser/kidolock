#!/usr/bin/env bash
# Capture interactive des screenshots Play Store via adb.
# Le script détecte l'appareil ayant KidoLock, active le mode démo (12:00,
# batterie 100 %, zéro notification), puis te guide : tu navigues dans
# l'app et appuies sur ENTRÉE dans le terminal pour chaque capture.
# Usage : ./scripts/prepare_playstore_media.sh
set -euo pipefail
cd "$(dirname "$0")/.."

PKG="fr.famille.modeenfant"
OUT="store_assets/screenshots"
mkdir -p "$OUT"

# Cible : SERIAL=... ou détection auto de l'appareil ayant KidoLock installé.
if [[ -z "${SERIAL:-}" ]]; then
    for d in $(adb devices | awk 'NR>1 && $2=="device" {print $1}'); do
        if adb -s "$d" shell pm list packages "$PKG" 2>/dev/null | grep -q "$PKG"; then
            SERIAL="$d"; break
        fi
    done
fi
if [[ -z "${SERIAL:-}" ]]; then
    echo "ERREUR : aucun appareil avec $PKG trouvé. Connecte la tablette ou passe SERIAL=ip:port." >&2
    adb devices >&2
    exit 1
fi
ADB="adb -s $SERIAL"
echo "Appareil : $SERIAL"

shot() { $ADB exec-out screencap -p > "$OUT/$1.png"; echo "  ✓ $OUT/$1.png"; }
pause() { echo; echo "── $1"; echo "   Prépare l'écran puis appuie sur ENTRÉE…"; read -r _; }

# ---------- état propre + mode démo ----------
$ADB shell am broadcast -a com.android.systemui.demo -e command exit >/dev/null 2>&1 || true
$ADB shell settings put global sysui_demo 0
$ADB shell am force-stop "$PKG"
sleep 1

$ADB shell settings put global sysui_demo 1
$ADB shell am broadcast -a com.android.systemui.demo -e command enter
$ADB shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200
$ADB shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
$ADB shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
$ADB shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false

$ADB shell am start -n "$PKG/.ParentActivity" >/dev/null
sleep 2

echo "=== Captures Play Store — navigue sur la tablette, ENTRÉE pour chaque shot ==="

pause "1/5 — Déverrouille avec le code PIN → écran de config (service actif, applis, durée)"
shot 01_config_parent

pause "2/5 — Ouvre « Choisir les applis » (écran de sélection)"
shot 02_select_apps
$ADB shell input keyevent KEYCODE_BACK
sleep 1

pause "3/5 — Reviens, lance une session (durée ≥ 15 min pour le vert « Amuse-toi bien ») → écran enfant"
shot 03_session_enfant

pause "4/5 — Pour l'état d'alerte : termine la session (cadenas → Arrêter), relance avec 5 min → « Bientôt fini » orange"
shot 04_alerte_fin

pause "5/5 — Appui long sur le cadenas en bas à droite → modale parent (code PIN / calcul)"
shot 05_verrouillage_parent
$ADB shell input keyevent KEYCODE_BACK

# ---------- nettoyage ----------
$ADB shell am broadcast -a com.android.systemui.demo -e command exit
$ADB shell settings put global sysui_demo 0
echo
echo "Terminé — vérifie les PNG dans $OUT/"
