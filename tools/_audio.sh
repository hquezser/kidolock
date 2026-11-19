# Fonctions communes aux scripts de voix (à sourcer, pas à lancer).
# Les phrases des annonces : nom du fichier res/raw → texte.
# Pas de points d'exclamation : la synthèse vocale les dit plus fort et plus haut, ce qui peut
# faire peur à un enfant. Formulations courtes et câlines.
PHRASES=(
    "voix_10min|Coucou. Encore dix minutes pour jouer."
    "voix_5min|Encore cinq petites minutes."
    "voix_2min|Encore deux minutes. Tu peux commencer à finir ton jeu."
    "voix_1min|Plus qu'une minute. Termine tranquillement ce que tu fais."
    "voix_fin|C'est l'heure de faire une pause. Bravo, et à bientôt."
)

# finaliser <entrée> <sortie.m4a>
# Coupe les silences au début et à la fin, adoucit le son (moins de graves sourds, moins d'aigus
# et de « s » sifflants), baisse et harmonise le volume, passe en mono 22 kHz,
# puis ajoute un carillon doux devant (CARILLON=0 pour le retirer).
finaliser() {
    local voix="silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.05,areverse,silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.15,areverse,highpass=f=110,lowpass=f=6500,deesser,loudnorm=I=-23:TP=-4,afade=t=in:d=0.08,aresample=22050"
    if [[ "${CARILLON:-1}" == "1" ]]; then
        # Deux notes graves et rondes (sol puis mi) qui s'éteignent lentement, puis un court silence.
        local ding="aevalsrc='0.12*sin(2*PI*784*t)*exp(-3*t)+gte(t,0.4)*0.12*sin(2*PI*659.3*(t-0.4))*exp(-3*(t-0.4))':s=22050:d=1.6"
        ffmpeg -loglevel error -y -f lavfi -i "$ding" -i "$1" \
            -filter_complex "[1:a]${voix}[v];[0:a][v]concat=n=2:v=0:a=1,aformat=channel_layouts=mono[out]" \
            -map "[out]" -c:a aac -b:a 40k "$2"
    else
        ffmpeg -loglevel error -y -i "$1" -af "${voix},aformat=channel_layouts=mono" -c:a aac -b:a 40k "$2"
    fi
}
