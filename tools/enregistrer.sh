#!/usr/bin/env bash
# Enregistre les annonces avec TA voix, via le micro du Mac.
#   ./tools/enregistrer.sh            les 5 phrases
#   ./tools/enregistrer.sh voix_fin   une seule phrase à refaire
# Pour chaque phrase : Entrée pour commencer, parle, Entrée pour arrêter.
# Ensuite on peut réécouter et recommencer. Les silences et le volume sont corrigés automatiquement.
# Au premier lancement, macOS demande l'accès au micro pour le Terminal.
# MICRO=":1" pour choisir un autre micro (liste : ffmpeg -f avfoundation -list_devices true -i "").
set -euo pipefail
cd "$(dirname "$0")"
source ./_audio.sh
MICRO="${MICRO:-:0}"   # :0 = micro du MacBook
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

for p in "${PHRASES[@]}"; do
    nom="${p%%|*}"
    texte="${p#*|}"
    [[ $# -gt 0 && " $* " != *" $nom "* ]] && continue
    while true; do
        echo
        echo "  « $texte »"
        read -r -p "  Entrée pour enregistrer… " _
        ffmpeg -nostdin -loglevel error -y -f avfoundation -i "$MICRO" -ac 1 "$TMP/brut.wav" &
        pid=$!
        read -r -p "  ● Enregistrement… Entrée pour arrêter " _
        kill -INT "$pid" 2>/dev/null || true
        wait "$pid" 2>/dev/null || true
        finaliser "$TMP/brut.wav" "$TMP/$nom.m4a"
        afplay "$TMP/$nom.m4a"
        read -r -p "  On garde ? [O/n] " ok
        if [[ "${ok:-o}" =~ ^[OoYy]$ ]]; then
            cp "$TMP/$nom.m4a" "../res/raw/$nom.m4a"
            echo "  ✓ $nom"
            break
        fi
    done
done
echo
echo "Terminé. Lance ./build.sh install pour mettre la tablette à jour."
