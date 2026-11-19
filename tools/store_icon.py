#!/usr/bin/env python3
"""Génère store_assets/icon_512x512.png : mascotte sur fond crème, sans alpha (exigence Play)."""
from PIL import Image
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "app/src/main/res/drawable-nodpi/mascotte_actif.png")
OUT = os.path.join(ROOT, "store_assets/icon_512x512.png")
BG = (0xFF, 0xF4, 0xD6)  # ic_launcher_background

img = Image.open(SRC).convert("RGBA")
bbox = img.split()[-1].getbbox()
robot = img.crop(bbox)
side = int(512 * 0.62)
robot.thumbnail((side, side), Image.LANCZOS)

canvas = Image.new("RGB", (512, 512), BG)
canvas.paste(robot, ((512 - robot.width) // 2, (512 - robot.height) // 2), robot)
os.makedirs(os.path.dirname(OUT), exist_ok=True)
canvas.save(OUT, "PNG")
print(OUT, canvas.size, canvas.mode)
