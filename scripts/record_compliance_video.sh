#!/usr/bin/env bash
# Enregistre la vidéo de justification Google Play (déclaration Accessibility API).
# 45 s max (limite screenrecord) — montre : divulgation → activation permission
# → lancement session → blocage d'une app non autorisée.
# Usage : SERIAL=192.168.1.15:5555 ./scripts/record_compliance_video.sh [sortie.mp4]
set -euo pipefail
cd "$(dirname "$0")/.."

ADB="adb ${SERIAL:+-s $SERIAL}"
OUT="${1:-store_assets/kidolock_compliance.mp4}"
mkdir -p "$(dirname "$OUT")"

echo "Enregistrement 45 s → $OUT"
echo "Scénario à jouer pendant l'enregistrement :"
echo "  1. Ouvrir KidoLock → écran parent → taper « Ouvrir les réglages » (divulgation)"
echo "  2. Activer le service dans Accessibilité → revenir"
echo "  3. Lancer une session 5 min → ouvrir une app non autorisée (rideau)"
echo "  4. Appui long sur le cadenas → code PIN"
echo "Ctrl+C pour arrêter avant la fin."
sleep 3
$ADB shell screenrecord --time-limit 45 --bit-rate 8000000 /sdcard/kidolock_compliance.mp4 &
REC=$!
trap 'kill $REC 2>/dev/null || true' INT
wait $REC || true
$ADB pull /sdcard/kidolock_compliance.mp4 "$OUT"
$ADB shell rm /sdcard/kidolock_compliance.mp4
echo "→ $OUT"
