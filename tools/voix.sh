#!/usr/bin/env bash
# Régénère les annonces vocales (res/raw/*.m4a) avec une voix de synthèse macOS, réglée pour être douce.
#   ./tools/voix.sh                       voix par défaut
#   VOIX="Amélie" HAUTEUR=4 ./tools/voix.sh   autre voix (liste : say -v '?' | grep fr_)
#   VITESSE=140 HAUTEUR=4 CARILLON=0 ./tools/voix.sh
set -euo pipefail
cd "$(dirname "$0")"
source ./_audio.sh
VOIX="${VOIX:-Audrey (Premium)}"
VITESSE="${VITESSE:-155}"   # mots/minute : plus lent = plus posé
HAUTEUR="${HAUTEUR:-0}"     # décalage de hauteur, utile pour adoucir les voix robotiques (ex. 4)
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

for p in "${PHRASES[@]}"; do
    nom="${p%%|*}"
    texte="${p#*|}"
    [[ "$HAUTEUR" != "0" ]] && texte="[[pbas +$HAUTEUR]] $texte"
    say -v "$VOIX" -r "$VITESSE" -o "$TMP/$nom.aiff" "$texte"
    finaliser "$TMP/$nom.aiff" "../res/raw/$nom.m4a"
done
ls -lh ../res/raw
