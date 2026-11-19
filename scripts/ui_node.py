#!/usr/bin/env python3
"""Lit un dump uiautomator sur stdin et imprime le centre de la vue.
Usage : ui_node.py <id-suffix>   → "x y" du centre
        ui_node.py text:<id>     → texte affiché par la vue"""
import re
import sys

want_text = len(sys.argv) > 1 and sys.argv[1].startswith("text:")
suffix = sys.argv[1].removeprefix("text:")
resource_id = f'resource-id="fr.famille.modeenfant:id/{suffix}"'

for tag in re.findall(r"<node[^>]*>", sys.stdin.read()):
    if resource_id not in tag:
        continue
    if want_text:
        m = re.search(r'text="([^"]*)"', tag)
        if m:
            print(m.group(1))
        break
    m = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', tag)
    if m:
        x1, y1, x2, y2 = map(int, m.groups())
        print(f"{(x1 + x2) // 2} {(y1 + y2) // 2}")
        break
