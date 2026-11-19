#!/usr/bin/env python3
"""Détourage générique d'une mascotte générée par Picsart (fond blanc uni).

Usage : python3 tools/detourage.py <src.png> <dst.png>

- flood-fill depuis les 4 coins (tolérance couleur) -> fond transparent,
  sans toucher aux zones blanches/intérieures du personnage ;
- rogne les marges transparentes (bbox alpha > 0) + marge de 16 px ;
- redimensionne en 512x512 (ratio préservé, canvas transparent).
"""
import os
import sys
from collections import deque

from PIL import Image

MARGIN = 16
SIZE = 512
# Distance euclidienne RGB au blanc : au-delà, le pixel n'est pas du fond.
TOL = 48.0
# Dé-halo : pixels non remplis mais 8-voisins du fond et encore blanchâtres
# -> alpha proportionnel à leur distance au blanc (0..1 sur 110).
FEATHER = 110.0


def dist_white(px):
    return ((255 - px[0]) ** 2 + (255 - px[1]) ** 2 + (255 - px[2]) ** 2) ** 0.5


def main():
    if len(sys.argv) != 3:
        sys.exit("usage: detourage.py <src.png> <dst.png>")
    src, dst = sys.argv[1], sys.argv[2]
    im = Image.open(src).convert("RGBA")
    w, h = im.size
    px = im.load()

    # Graine = blanc pur (coins vérifiés ~255). On part des 4 coins.
    visited = bytearray(w * h)
    q = deque()
    seeds = [(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)]
    for x, y in seeds:
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

    # Dé-halo limité à l'anneau externe : pixels opaques 8-voisins d'un
    # pixel transparent et encore proches du blanc -> alpha partiel.
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

    # Bbox des pixels visibles
    bbox = im.getbbox()
    if bbox is None:
        sys.exit("image entièrement transparente, abandon")
    left = max(0, bbox[0] - MARGIN)
    top = max(0, bbox[1] - MARGIN)
    right = min(w, bbox[2] + MARGIN)
    bottom = min(h, bbox[3] + MARGIN)
    im = im.crop((left, top, right, bottom))

    # 512x512 en préservant le ratio, centré sur canvas transparent
    im.thumbnail((SIZE, SIZE), Image.LANCZOS)
    out = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    out.paste(im, ((SIZE - im.width) // 2, (SIZE - im.height) // 2), im)

    os.makedirs(os.path.dirname(os.path.abspath(dst)), exist_ok=True)
    out.save(dst)
    print(f"{src} -> {dst}  (source {w}x{h}, sortie {out.size})")


if __name__ == "__main__":
    main()
