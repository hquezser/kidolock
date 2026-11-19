#!/usr/bin/env python3
"""Prépare le foreground de l'icône adaptative depuis la mascotte robot.

- prend res/drawable-nodpi/mascotte_actif.png (déjà détourée, 512x512 alpha) ;
- rogne au bbox alpha puis recale le robot à ~60 % du canvas : la zone sûre
  d'une adaptive icon (cercle intérieur de 66 dp sur 108 dp) fait ~61 %, on
  reste en dessous pour les masques agressifs (carré arrondi, cercle…) ;
- centre sur un canvas transparent 512x512 ;
- sort res/drawable-nodpi/ic_launcher_foreground.png

Le fond uni et le XML adaptatif se font à la main (voir mipmap-anydpi-v26).
"""
import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "res", "drawable-nodpi", "mascotte_actif.png")
DST = os.path.join(ROOT, "res", "drawable-nodpi", "ic_launcher_foreground.png")

SIZE = 512
# Diamètre du robot sur le canvas : sous les ~61 % de la zone sûre adaptative.
ROBOT_RATIO = 0.60


def main():
    im = Image.open(SRC).convert("RGBA")
    bbox = im.getbbox()
    if bbox is None:
        raise SystemExit("image entièrement transparente, abandon")
    im = im.crop(bbox)

    target = int(SIZE * ROBOT_RATIO)
    scale = target / max(im.size)
    im = im.resize((round(im.width * scale), round(im.height * scale)), Image.LANCZOS)

    out = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    out.paste(im, ((SIZE - im.width) // 2, (SIZE - im.height) // 2), im)
    out.save(DST)
    print(f"{SRC} -> {DST}")
    print(f"  bbox={bbox}, robot ~{target}px sur {SIZE}, canvas transparent centré")


if __name__ == "__main__":
    main()
