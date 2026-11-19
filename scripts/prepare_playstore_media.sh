#!/usr/bin/env bash
# Capture les screenshots Play Store via adb.
# Prérequis : tablette/émulateur connecté, KidoLock installé.
# Usage : ./scripts/prepare_playstore_media.sh
# Le script détecte seul l'appareil ayant KidoLock, active le mode démo,
# lance l'app et **te laisse saisir le code PIN manuellement** (appuie sur
# ENTRÉE dans le terminal une fois déverrouillé). Les taps suivants sont
# résolus par resource-id via uiautomator — pas de coordonnées à régler.
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

# ---------- helpers ----------
dump_ui() { $ADB shell uiautomator dump /sdcard/_ui.xml >/dev/null 2>&1; $ADB shell cat /sdcard/_ui.xml; }
node_xy() { dump_ui | python3 scripts/ui_node.py "$1"; }
node_text() { dump_ui | python3 scripts/ui_node.py "text:$1"; }

tap_id() {
    local xy; xy=$(node_xy "$1")
    [[ -z "$xy" ]] && { echo "  !! vue '$1' introuvable" >&2; return 1; }
    $ADB shell input tap $xy; sleep 0.6
}
hold_id() {
    local xy; xy=$(node_xy "$1")
    [[ -z "$xy" ]] && { echo "  !! vue '$1' introuvable" >&2; return 1; }
    $ADB shell input swipe $xy $xy 1600; sleep 0.6
}
shot() { $ADB exec-out screencap -p > "$OUT/$1.png"; echo "→ $OUT/$1.png"; }

wait_id() {  # attend qu'une vue apparaisse (secondes max en $2, défaut 15)
    local i max=${2:-15}
    for (( i=0; i<max; i++ )); do
        [[ -n "$(node_xy "$1")" ]] && return 0
        sleep 1
    done
    echo "  !! '$1' jamais apparue" >&2; return 1
}

# ---------- 0. état propre ----------
$ADB shell am broadcast -a com.android.systemui.demo -e command exit >/dev/null 2>&1 || true
$ADB shell settings put global sysui_demo 0
$ADB shell am force-stop "$PKG"

# ---------- 1. mode démo : 12:00, batterie 100 %, zéro notification ----------
$ADB shell settings put global sysui_demo 1
$ADB shell am broadcast -a com.android.systemui.demo -e command enter
$ADB shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200
$ADB shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
$ADB shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
$ADB shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
sleep 1

# ---------- 2. écran parent : PIN saisi à la main ----------
$ADB shell am start -n "$PKG/.ParentActivity" >/dev/null
sleep 2
echo
echo "=== Déverrouille la tablette avec ton code PIN,"
echo "=== puis appuie sur ENTRÉE ici pour continuer. ==="
read -r _
wait_id apps_btn 30 || { echo "Toujours verrouillé — déverrouille puis relance."; exit 1; }
sleep 1
shot 01_config_parent

# ---------- 3. sélecteur d'applications ----------
tap_id apps_btn
wait_id grid 5 || true   # AppListActivity a sa propre grille ; au pire on shoote quand même
sleep 1
shot 02_select_apps
$ADB shell input keyevent KEYCODE_BACK
sleep 1

# ---------- 4. durée mini (5 min) pour l'état d'alerte ----------
for (( i=0; i<20; i++ )); do
    dur=$(node_text duration | grep -oE '[0-9]+' | head -1 || true); dur=${dur:-99}
    [[ "$dur" -le 5 ]] && break
    tap_id minus_btn
done

# ---------- 5. session enfant ----------
tap_id start_top
sleep 4
wait_id lock_btn 10 || true
shot 03_session_enfant
shot 04_alerte_fin   # session 5 min : état « Bientôt fini » d'emblée

# ---------- 6. modale parent (appui long cadenas) ----------
hold_id lock_btn
sleep 1.5
shot 05_verrouillage_parent
$ADB shell input keyevent KEYCODE_BACK

# ---------- 7. nettoyage ----------
$ADB shell am broadcast -a com.android.systemui.demo -e command exit
$ADB shell settings put global sysui_demo 0
echo "Terminé — vérifie les PNG dans $OUT/"
