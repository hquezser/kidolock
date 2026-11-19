#!/usr/bin/env python3
"""Découpe la planche mascottes_actif/fatigué et prépare les PNG Android.

- split vertical en deux moitiés (gauche = actif, droite = fatigué) ;
- par moitié : flood-fill depuis les 4 coins (tolérance couleur) -> fond
  transparent, sans toucher aux zones blanches/intérieures du robot ;
- rogne les marges transparentes (bbox alpha > 0) + marge de 16 px ;
- redimensionne en 512x512 (ratio préservé, canvas transparent) ;
- sort res/drawable-nodpi/mascotte_actif.png et mascotte_fatigue.png
"""
import os
import sys
from collections import deque

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "tools", "tmp_picsart", "mascottes_alertes_raw.png")
OUT_DIR = os.path.join(ROOT, "res", "drawable-nodpi")

MARGIN = 16
SIZE = 512
TOL = 48.0      # distance euclidienne RGB au blanc : au-delà, pas du fond
FEATHER = 110.0  # dé-halo : anneau de bord proche-blanc -> alpha partiel


def dist_white(px):
    return ((255 - px[0]) ** 2 + (255 - px[1]) ** 2 + (255 - px[2]) ** 2) ** 0.5


def cutout(im):
    """RGBA -> RGBA détouré depuis les coins, rogné aux marges vides + MARGIN."""
    w, h = im.size
    px = im.load()

    visited = bytearray(w * h)
    q = deque()
    for x, y in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)):
        if not visited[y * w + x]:
            visited[y * w + x] = 1
            q.append((x, y))

    while q:
        x, y = q.popleft()
        p = px[x, y]
        if dist_white(p) > TOL:
            continue
        px[x, y] = (p[0], p[1], p[2], 0)
        for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
            if 0 <= nx < w and 0 <= ny < h and not visited[ny * w + nx]:
                visited[ny * w + nx] = 1
                q.append((nx, ny))

    # Dé-halo : pixels opaques 8-voisins d'un transparent et encore
    # blanchâtres -> alpha proportionnel à la distance au blanc.
    transparent = bytearray(w * h)
    for y in range(h):
        for x in range(w):
            if px[x, y][3] == 0:
                transparent[y * w + x] = 1
    for y in range(h):
        for x in range(w):
            if transparent[y * w + x]:
                continue
            near_bg = False
            for dy in (-1, 0, 1):
                for dx in (-1, 0, 1):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h and transparent[ny * w + nx]:
                        near_bg = True
                        break
                if near_bg:
                    break
            if not near_bg:
                continue
            p = px[x, y]
            d = dist_white(p)
            if d < FEATHER:
                px[x, y] = (p[0], p[1], p[2], int(255 * d / FEATHER))

    bbox = im.getbbox()
    if bbox is None:
        sys.exit("moitié entièrement transparente, abandon")
    left = max(0, bbox[0] - MARGIN)
    top = max(0, bbox[1] - MARGIN)
    right = min(w, bbox[2] + MARGIN)
    bottom = min(h, bbox[3] + MARGIN)
    return im.crop((left, top, right, bottom)), bbox


def to_square(im):
    im.thumbnail((SIZE, SIZE), Image.LANCZOS)
    out = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    out.paste(im, ((SIZE - im.width) // 2, (SIZE - im.height) // 2), im)
    return out


def main():
    sheet = Image.open(SRC).convert("RGBA")
    w, h = sheet.size
    mid = w // 2
    halves = [
        ("mascotte_actif", sheet.crop((0, 0, mid, h))),
        ("mascotte_fatigue", sheet.crop((mid, 0, w, h))),
    ]
    os.makedirs(OUT_DIR, exist_ok=True)
    for name, half in halves:
        cut, bbox = cutout(half.copy())
        out = to_square(cut)
        dst = os.path.join(OUT_DIR, name + ".png")
        out.save(dst)
        print(f"{name}: half {half.size}, bbox={bbox} -> {dst} {out.size}")


if __name__ == "__main__":
    main()
