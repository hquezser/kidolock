#!/usr/bin/env bash
# Enregistre la vidéo de justification Google Play (déclaration Accessibility API).
# 45 s max (limite screenrecord) — montre : divulgation → activation permission
# → lancement session → blocage d'une app non autorisée.
# Usage : SERIAL=192.168.1.15:5555 ./scripts/record_compliance_video.sh [sortie.mp4]
set -euo pipefail
cd "$(dirname "$0")/.."

PKG="fr.famille.modeenfant"
if [[ -z "${SERIAL:-}" ]]; then
    for d in $(adb devices | awk 'NR>1 && $2=="device" {print $1}'); do
        if adb -s "$d" shell pm list packages "$PKG" 2>/dev/null | grep -q "$PKG"; then
            SERIAL="$d"; break
        fi
    done
fi
ADB="adb ${SERIAL:+-s $SERIAL}"
OUT="${1:-store_assets/kidolock_compliance.mp4}"
mkdir -p "$(dirname "$OUT")"

# Résolution réduite : les écrans >1600 px de large font échouer l'encodeur (err=-38).
SIZE=$($ADB shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1)
RW=${SIZE%x*}; RH=${SIZE#*x}
if [[ "$RW" -gt 1600 ]]; then
    RH=$(( RH * 1600 / RW )); RH=$(( RH / 2 * 2 ))  # pair requis
    REC_SIZE="1600x${RH}"
else
    REC_SIZE="$SIZE"
fi
echo "Enregistrement 45 s (${REC_SIZE}) → $OUT"
echo "Scénario à jouer pendant l'enregistrement :"
echo "  1. Ouvrir KidoLock → écran parent → taper « Ouvrir les réglages » (divulgation)"
echo "  2. Activer le service dans Accessibilité → revenir"
echo "  3. Lancer une session 5 min → ouvrir une app non autorisée (rideau)"
echo "  4. Appui long sur le cadenas → code PIN"
echo "Ctrl+C pour arrêter avant la fin."
sleep 3
$ADB shell screenrecord --time-limit 45 --size "$REC_SIZE" --bit-rate 6000000 /sdcard/kidolock_compliance.mp4 &
REC=$!
trap 'kill $REC 2>/dev/null || true' INT
wait $REC || true
$ADB pull /sdcard/kidolock_compliance.mp4 "$OUT"
$ADB shell rm /sdcard/kidolock_compliance.mp4
if [[ ! -s "$OUT" ]]; then
    echo "ERREUR : capture vide — l'encodeur a échoué (err=-38). Essaie une résolution plus petite ou un autre appareil." >&2
    exit 1
fi
echo "→ $OUT ($(du -h "$OUT" | cut -f1))"
