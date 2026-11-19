#!/usr/bin/env bash
# Extrait l'aperçu vocal « Bip-bip ! Coucou ! » depuis tts_10min.wav pour
# chaque langue/tier → res/raw*/apercu{,_t1}.m4a (utilisé par le sélecteur
# de style dans l'écran parent).
#
# LANG : fr | en | es (défaut fr). TIER : vide (jeune) ou t1 (grand).
# Source : tmp_picsart[_t1]/<lang>/tts_10min.wav. Sortie : res/raw/ pour en
# (défaut Android), res/raw-<lang>/ sinon ; suffixe _t1 si TIER=t1.
#
# La coupe se fait au 2ᵉ silence ≥0,20 s : le 1ᵉr sépare « Bip-bip » et
# « Coucou », le 2ᵉ marque la fin du « Coucou » avant la suite de la phrase.
set -euo pipefail
cd "$(dirname "$0")"

LANG="${LANG:-fr}"
TIER="${TIER:-}"
SRC_TTS="tmp_picsart${TIER:+_$TIER}/$LANG"
OUT="../res/raw"
[[ "$LANG" == "en" ]] || OUT="../res/raw-$LANG"
mkdir -p "$OUT"
SUF="${TIER:+_$TIER}"
SRC="$SRC_TTS/tts_10min.wav"

TRIM='silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.05,areverse,silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.15,areverse'
ATEMPO="${ATEMPO:-0.9}"
VOIX="${TRIM},highpass=f=110,lowpass=f=6500,deesser,atempo=${ATEMPO}"

# 2ᵉ silence_start = fin du « Coucou » ; +0,12 s de queue pour la syllabe
cut=$(ffmpeg -i "$SRC" -af silencedetect=n=-45dB:d=0.20 -f null - 2>&1 \
    | awk '/silence_start/{n++; if(n==2){print $NF; exit}}')
[ -z "$cut" ] && cut=1.8
cut=$(awk -v c="$cut" 'BEGIN{printf "%.2f", c+0.12}')

TMPW="$(mktemp -t apercu).wav"
trap 'rm -f "$TMPW"' EXIT

# passe 1 : segment [0, cut] + chaîne vocale complète
ffmpeg -loglevel error -y -i "$SRC" \
    -af "atrim=0:${cut},asetpts=PTS-STARTPTS,${VOIX},aresample=22050,aformat=channel_layouts=mono" \
    -c:a pcm_s16le "$TMPW"
# passe 2 : loudnorm -23 LUFS + AAC 40 kbps mono
ffmpeg -loglevel error -y -i "$TMPW" \
    -af "loudnorm=I=-23:TP=-2:LRA=7,aresample=22050,aformat=channel_layouts=mono" \
    -c:a aac -b:a 40k "$OUT/apercu${SUF}.m4a"
d=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$OUT/apercu${SUF}.m4a")
echo "$OUT/apercu${SUF}.m4a  ${d}s  (coupe à ${cut}s)"
