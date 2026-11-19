#!/usr/bin/env bash
# Assemble les annonces voix_* finales à partir des sources générées par
# Picsart dans tmp_picsart/ : TTS par langue dans tmp_picsart/<lang>/, SFX
# (sans langue) partagés dans tmp_picsart/. Schéma de chaque piste :
# [SFX] + 0,35 s de silence + [TTS], puis loudnorm -23 LUFS, mono 22 kHz,
# AAC 40 kbps — même cible que tools/voix.sh.
#
# LANG : fr | en | es (défaut fr). Sortie : res/raw/ pour en (défaut Android),
# res/raw-<lang>/ pour les autres langues.
#
# Le filtrage vocal (highpass/de-esser) ne s'applique qu'aux voix ; les SFX
# gardent leurs aigus pour la clarté des petits bips.
# NB : loudnorm est fait dans une SECONDE passe ffmpeg — dans le même
# filter_complex qu'un concat il perd ~1,7 s en fin de fichier (bug connu
# du lookahead interne).
set -euo pipefail
cd "$(dirname "$0")"

LANG="${LANG:-fr}"
SRC_SFX="tmp_picsart"
SRC_TTS="tmp_picsart/$LANG"
OUT="../res/raw"
[[ "$LANG" == "en" ]] || OUT="../res/raw-$LANG"
mkdir -p "$OUT"

TMPW="$(mktemp -t voixmix).wav"
trap 'rm -f "$TMPW"' EXIT

# trim : même silenceremove que _audio.sh (début + fin via astuce areverse)
TRIM='silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.05,areverse,silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.15,areverse'
# chaîne vocale : trim + highpass + lowpass 6,5 kHz + de-esser (doux, comme finaliser)
# + atempo : léger ralentissement si la voix paraît trop rapide (1.0 = vitesse source)
ATEMPO="${ATEMPO:-0.9}"
VOIX="${TRIM},highpass=f=110,lowpass=f=6500,deesser,atempo=${ATEMPO}"

# sortie|fichier sfx|fichier tts
PISTES=(
    "voix_10min|sfx_info|tts_10min"
    "voix_5min|sfx_info|tts_5min"
    "voix_2min|sfx_warning|tts_2min"
    "voix_1min|sfx_warning|tts_1min"
    "voix_fin|sfx_end|tts_fin"
)

for p in "${PISTES[@]}"; do
    nom="${p%%|*}"; reste="${p#*|}"
    sfx="${reste%%|*}"; tts="${reste#*|}"
    # passe 1 : mix [SFX] + 0,35 s + [TTS]
    ffmpeg -loglevel error -y \
        -i "$SRC_SFX/$sfx.wav" -i "$SRC_TTS/$tts.wav" \
        -f lavfi -t 0.35 -i "anullsrc=r=44100:cl=mono" \
        -filter_complex "\
[0:a]${TRIM}[s];\
[1:a]${VOIX}[v];\
[2:a]aformat=channel_layouts=mono[sil];\
[s][sil][v]concat=n=3:v=0:a=1,aresample=22050[mix]" \
        -map "[mix]" -c:a pcm_s16le "$TMPW"
    # passe 2 : normalisation -23 LUFS + encodage AAC 40 kbps mono
    ffmpeg -loglevel error -y -i "$TMPW" \
        -af "loudnorm=I=-23:TP=-2:LRA=7,aresample=22050,aformat=channel_layouts=mono" \
        -c:a aac -b:a 40k "$OUT/$nom.m4a"
    d=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$OUT/$nom.m4a")
    echo "$OUT/$nom.m4a  ${d}s"
done
