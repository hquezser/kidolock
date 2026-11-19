#!/usr/bin/env bash
# Vidéo de justification Google Play — mode « rafale de screencap ».
# Capture ~2 img/s via adb pendant que tu joues le scénario, puis assemble
# en mp4 avec ffmpeg. Fiable même quand l'encodeur hardware de l'appareil
# est HS (screenrecord → err=-38 sur Nexus 10 / LineageOS 17.1).
#
# Usage : ./scripts/record_compliance_video.sh [sortie.mp4]
#   Pendant l'enregistrement, joue le scénario affiché puis appuie sur
#   ENTRÉE (ou une touche) pour arrêter — ou attends la limite de ~3 min.
set -euo pipefail
cd "$(dirname "$0")/.."

PKG="fr.famille.modeenfant"
OUT="${1:-store_assets/kidolock_compliance.mp4}"
mkdir -p "$(dirname "$OUT")"

command -v ffmpeg >/dev/null || { echo "ERREUR : ffmpeg requis (brew install ffmpeg)." >&2; exit 1; }

# Cible : SERIAL=... ou détection auto de l'appareil ayant KidoLock installé.
if [[ -z "${SERIAL:-}" ]]; then
    for d in $(adb devices | awk 'NR>1 && $2=="device" {print $1}'); do
        if adb -s "$d" shell pm list packages "$PKG" 2>/dev/null | grep -q "$PKG"; then
            SERIAL="$d"; break
        fi
    done
fi
ADB="adb ${SERIAL:+-s $SERIAL}"

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

echo "Capture en cours → $OUT"
echo "Scénario à jouer pendant la capture :"
echo "  1. Ouvrir KidoLock → écran parent → taper « Ouvrir les réglages » (divulgation)"
echo "  2. Activer le service dans Accessibilité → revenir"
echo "  3. Lancer une session 5 min → ouvrir une app non autorisée (rideau)"
echo "  4. Appui long sur le cadenas → code PIN"
echo
echo "Joue le scénario, puis appuie sur ENTRÉE pour arrêter."

START=$(date +%s); i=0
while true; do
    $ADB exec-out screencap -p > "$TMP/f$(printf '%05d' "$i").png" 2>/dev/null || break
    i=$((i+1)); [[ $i -ge 600 ]] && break
    # touche = stop, sans bloquer plus de 0,2 s par image
    if read -t 0.2 -n 1 -r _ 2>/dev/null; then break; fi
done
END=$(date +%s); DUR=$((END - START )); DUR=$(( DUR > 0 ? DUR : 1 ))

N=$i
[[ "$N" -lt 5 ]] && { echo "ERREUR : capture trop courte ($N images)." >&2; exit 1; }

FPS=$(awk "BEGIN { printf \"%.2f\", $N / $DUR }")
echo "→ $N images en ${DUR}s (~${FPS} img/s), assemblage…"

ffmpeg -y -loglevel error \
    -framerate "$FPS" -i "$TMP/f%05d.png" \
    -vf "scale='min(1600,iw)':-2" \
    -c:v libx264 -preset fast -crf 23 -pix_fmt yuv420p -movflags +faststart \
    "$OUT"

if [[ ! -s "$OUT" ]]; then
    echo "ERREUR : vidéo vide." >&2; exit 1
fi
echo "→ $OUT ($(du -h "$OUT" | cut -f1))"
