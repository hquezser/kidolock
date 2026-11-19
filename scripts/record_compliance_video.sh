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

# Capture en 1280x800 à densité identique : UI strictement identique,
# PNG ~4x plus légers → screencap ~3x plus rapide. Restauré à la sortie.
ORIG_SIZE=$($ADB shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1)
ORIG_DENS=$($ADB shell wm density | grep -oE '[0-9]+' | tail -1)
cleanup() {
    $ADB shell wm size "$ORIG_SIZE" >/dev/null 2>&1 || true
    [[ -n "$ORIG_DENS" ]] && $ADB shell wm density "$ORIG_DENS" >/dev/null 2>&1 || true
    rm -rf "$TMP"
}
trap cleanup EXIT

RW=${ORIG_SIZE%x*}
if [[ "$RW" -gt 1280 ]]; then
    # Même densité réduite proportionnellement → même taille logique (dp), UI identique.
    NEW_DENS=""
    [[ -n "$ORIG_DENS" && "$ORIG_DENS" -gt 0 ]] && NEW_DENS=$(( ORIG_DENS * 1280 / RW ))
    echo "→ capture en 1280x800 (UI identique, plus rapide)"
    $ADB shell wm size 1280x800
    [[ -n "$NEW_DENS" ]] && $ADB shell wm density "$NEW_DENS"
    sleep 1   # laisse les activities se recréer à la nouvelle taille
fi

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

# fps en rationnel exact — pas de souci de virgule/décimale locale.
echo "→ $N images en ${DUR}s, assemblage…"

ffmpeg -y -loglevel error \
    -framerate "$N/$DUR" -i "$TMP/f%05d.png" \
    -vf "scale='min(1600,iw)':-2" \
    -c:v libx264 -preset fast -crf 23 -pix_fmt yuv420p -movflags +faststart \
    "$OUT"

if [[ ! -s "$OUT" ]]; then
    echo "ERREUR : vidéo vide." >&2; exit 1
fi
echo "→ $OUT ($(du -h "$OUT" | cut -f1))"
