#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JustQuests GUI texture set - v2 FULL ("everything").

Implements gui-prompt-v2-full.md on top of the strict-vanilla v2 rules:
  * Looks exactly like a vanilla container GUI. True 1x pixel art, NO AA,
    NO gradients (except the documented vanilla tooltip border), NO rounded
    corners, NO baked text. Top-left light source.
  * Vanilla grey palette only (+ documented tooltip exception colours).
  * Layers delivered as a named folder tree:
        background/  interactive/<el>/<state>/  icons/<set>/  overlay/
        hud/  anim/  strips/  atlas/(+atlas_map.txt)  themes/  2x/
  * 9-slice window tiles, full control catalog, full icon sets, overlays,
    animation strips, HUD, three greyscale themes, and a 2x pixel-double set.

Everything is generated programmatically and verified (bounds / overlap /
palette) before delivery.
"""

import os
from PIL import Image, ImageDraw, ImageFont

# --------------------------------------------------------------------------
OUT = r"C:\Users\mukse\Desktop\JustQuests-GUI-v2-full"
DIRS = {k: os.path.join(OUT, k) for k in
        ("background", "interactive", "icons", "overlay", "hud", "anim",
         "strips", "atlas", "themes", "2x", "preview", "mockup")}
for d in [OUT] + list(DIRS.values()):
    os.makedirs(d, exist_ok=True)
for root, _dirs, files in os.walk(OUT):          # wipe old PNGs
    for fn in files:
        if fn.lower().endswith((".png", ".txt")):
            os.remove(os.path.join(root, fn))

GUTTER, ATLAS = 2, 256

# --------------------------------------------------------------------------
# Palette - vanilla GUI greys (+ documented tooltip exception)
# --------------------------------------------------------------------------
BLACK  = (0, 0, 0)
FACE   = (198, 198, 198)
WHITE  = (255, 255, 255)
SHADOW = (85, 85, 85)
INSET  = (139, 139, 139)
DARK   = (55, 55, 55)
TEXTCOL = (64, 64, 64)
GREYS = [BLACK, DARK, SHADOW, INSET, FACE, WHITE]
# tooltip exception (vanilla): near-black fill + 2-tone purple border
TIP_FILL = (16, 0, 16)
TIP_B1   = (80, 0, 255)
TIP_B2   = (40, 0, 127)
ALLOWED_RGB = set(GREYS) | {TIP_FILL, TIP_B1, TIP_B2}


def tile(w, h):
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def A(c):
    return (c[0], c[1], c[2], 255)


def bevel(d, x, y, w, h, hi, lo):
    d.line([(x + 1, y + 1), (x + w - 2, y + 1)], fill=A(hi))
    d.line([(x + 1, y + 1), (x + 1, y + h - 2)], fill=A(hi))
    d.line([(x + 1, y + h - 2), (x + w - 2, y + h - 2)], fill=A(lo))
    d.line([(x + w - 2, y + 1), (x + w - 2, y + h - 2)], fill=A(lo))


def slot(w, h, outline=None):
    img, d = tile(w, h)
    d.rectangle([0, 0, w - 1, h - 1], fill=A(INSET))
    d.line([(0, 0), (w - 1, 0)], fill=A(DARK))
    d.line([(0, 0), (0, h - 1)], fill=A(DARK))
    d.line([(0, h - 1), (w - 1, h - 1)], fill=A(WHITE))
    d.line([(w - 1, 0), (w - 1, h - 1)], fill=A(WHITE))
    if outline:
        d.rectangle([0, 0, w - 1, h - 1], outline=A(outline))
    return img, d


def face_box(w, h, state, face=FACE):
    img, d = tile(w, h)
    if state == "disabled":
        d.rectangle([0, 0, w - 1, h - 1], fill=A(INSET), outline=A(BLACK))
        return img, d
    d.rectangle([0, 0, w - 1, h - 1], fill=A(face), outline=A(BLACK))
    if state == "pressed":
        bevel(d, 0, 0, w, h, SHADOW, WHITE)
    else:
        bevel(d, 0, 0, w, h, WHITE, SHADOW)
        if state in ("hover", "selected"):
            d.rectangle([0, 0, w - 1, h - 1], outline=A(WHITE))
            d.line([(1, 1), (w - 2, 1)], fill=A(WHITE))
    if state == "focused":
        focus_ring(d, 0, 0, w, h)
    return img, d


def focus_ring(d, x, y, w, h):
    """1px dashed white focus outline (greyscale)."""
    for i in range(x, x + w, 2):
        d.point((i, y), fill=A(WHITE)); d.point((i, y + h - 1), fill=A(WHITE))
    for j in range(y, y + h, 2):
        d.point((x, j), fill=A(WHITE)); d.point((x + w - 1, j), fill=A(WHITE))


def gcol(state):
    return SHADOW if state == "disabled" else DARK


def sh(state):
    return 1 if state == "pressed" else 0


def recolor(img, mapping):
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a and (r, g, b) in mapping:
                nr, ng, nb = mapping[(r, g, b)]
                px[x, y] = (nr, ng, nb, a)
    return out


# ---- small glyph painters (monochrome) ----
def paint_check(d, ox, oy, col=DARK):
    d.line([(ox + 3, oy + 6), (ox + 7, oy + 2)], fill=A(col), width=2)
    d.line([(ox + 1, oy + 4), (ox + 3, oy + 6)], fill=A(col), width=2)


def paint_lock(d, ox, oy, col=DARK):
    d.arc([ox + 2, oy + 0, ox + 6, oy + 6], 180, 360, fill=A(SHADOW))
    d.arc([ox + 2, oy + 1, ox + 6, oy + 6], 180, 360, fill=A(SHADOW))
    d.rectangle([ox + 1, oy + 4, ox + 7, oy + 8], fill=A(col))
    d.point((ox + 4, oy + 6), fill=A(SHADOW))


def paint_repeat(d, ox, oy, col=DARK):
    d.line([(ox + 1, oy + 2), (ox + 6, oy + 2)], fill=A(col))
    d.polygon([(ox + 5, oy), (ox + 5, oy + 4), (ox + 8, oy + 2)], fill=A(col))
    d.line([(ox + 2, oy + 6), (ox + 7, oy + 6)], fill=A(col))
    d.polygon([(ox + 3, oy + 4), (ox + 3, oy + 8), (ox, oy + 6)], fill=A(col))


def paint_star(d, ox, oy, col=DARK):
    pts = [(4, 0), (5, 3), (8, 3), (6, 5), (7, 8), (4, 6), (1, 8), (2, 5),
           (0, 3), (3, 3)]
    d.polygon([(ox + a, oy + b) for a, b in pts], fill=A(col))


def paint_tri(d, cx, cy, direction, col, r=2):
    if direction == "up":
        d.polygon([(cx - r, cy + r), (cx + r, cy + r), (cx, cy - r)], fill=A(col))
    elif direction == "down":
        d.polygon([(cx - r, cy - r), (cx + r, cy - r), (cx, cy + r)], fill=A(col))
    elif direction == "left":
        d.polygon([(cx + r, cy - r), (cx + r, cy + r), (cx - r, cy)], fill=A(col))
    else:
        d.polygon([(cx - r, cy - r), (cx - r, cy + r), (cx + r, cy)], fill=A(col))


# --------------------------------------------------------------------------
# Icon system: 16x16 ASCII patterns -> tile
#   k=black d=#373737 s=#555 m=#8B8B8B l=#C6C6C6 w=white  (space = clear)
# --------------------------------------------------------------------------
CMAP = {' ': None, 'k': BLACK, 'd': DARK, 's': SHADOW, 'm': INSET,
        'l': FACE, 'w': WHITE}


def icon(pat):
    h = len(pat)
    w = max(len(r) for r in pat)
    img, _ = tile(w, h)
    px = img.load()
    for y, row in enumerate(pat):
        for x, ch in enumerate(row):
            c = CMAP.get(ch)
            if c:
                px[x, y] = A(c)
    return img


# 16x16 objective / reward / category icons
P = {
 "crate": [
  "                ", "   kkkkkkkkkk   ", "  k ddddddd k  ", "  kd lllll dk  ",
  "  kd l   l dk  ", "  kdddddddddk  ", "  kd l   l dk  ", "  kd l   l dk  ",
  "  kd l   l dk  ", "  kdddddddddk  ", "  kd l   l dk  ", "  kd lllll dk  ",
  "  k dddddddk   ", "   kkkkkkkkk   ", "                ", "                "],
 "pickaxe": [
  "                ", "      kkkkk     ", "    kkdddddkk   ", "  kkddlll lddk  ",
  " kdll s    dk   ", " kd s  kkk  k   ", "  kkk k.k kk    ", "     kk.kk      ",
  "      k.k       ", "      k.k       ", "      k.k       ", "      k.k       ",
  "      k.k       ", "      k.k       ", "      kkk       ", "                "],
 "craft": [
  "                ", " kkkkkkkkkkkkkk ", " kdddddddddddk ", " kd l l l l ddk",
  " kdddddddddddk ", " kd l l l l ddk", " kdddddddddddk ", " kd l l l l ddk",
  " kdddddddddddk ", " kkkkkkkkkkkkk ", "  k d     d k  ", "  k d     d k  ",
  "  k d     d k  ", "  kkk     kkk  ", "                ", "                "],
 "furnace": [
  "                ", " kkkkkkkkkkkk   ", " kssssssssssk   ", " ks kkkkkk sk   ",
  " ks k    k sk   ", " ks k ll k sk   ", " ks k ll k sk   ", " ks kkkkkk sk   ",
  " kssssssssssk   ", " ks  kkkk  sk   ", " ks k mm k sk   ", " ks k mm k sk   ",
  " ks kkkkkk sk   ", " kkkkkkkkkkkk   ", "                ", "                "],
 "apple": [
  "        k       ", "       k        ", "      ddd       ", "    kk d kk     ",
  "  kkdddddddkk   ", " kdllllllllldk  ", " kdlllllllllldk ", " kdlllllllllldk ",
  " kdllllllllllk  ", " kdllllllllldk  ", "  kdlllllllldk  ", "  kdllllllldk   ",
  "   kdllllldk    ", "    kkdddkk     ", "      kkk       ", "                "],
 "cube": [
  "      kkkk      ", "    kk lll kk   ", "  kk lll lll kk ", " k lll lll lll k",
  " kk lll lll kkk ", " kmkk lll kk lk ", " kmmmkkkkkkmmlk ", " kmmmmk  kmmmlk ",
  " kmmmmk  kmmmlk ", " kmmmmk  kmmmlk ", " kmmmsk  kssmlk ", "  kmmsk  ksslk  ",
  "   kmsk  kslk   ", "    kskkkksk    ", "     kkkkkk     ", "                "],
 "sword": [
  "             kk ", "            kwk ", "           klk  ", "          klk   ",
  "         klk    ", "        klk     ", "       klk      ", "      klk       ",
  "     klk        ", "  k klk         ", "  kklk          ", "   kkk          ",
  "  klkkk         ", "  kk  k         ", "                ", "                "],
 "bone": [
  "                ", "  kk        kk  ", " kllk      kllk ", " klllk    klllk ",
  " kllllkkkkkllllk", "  klllllllllllk ", "   kllllllllllk ", "   kllllllllk   ",
  " kllllkkkkkllllk", " klllk    klllk ", " kllk      kllk ", "  kk        kk  ",
  "                ", "                ", "                ", "                "],
 "trophy": [
  "                ", "  kkkkkkkkkk    ", "  kllllllllk    ", " kk llllll kk   ",
  " ll kllllk ll   ", " ll kllllk ll   ", " kk kllllk kk   ", "    kllllk      ",
  "     klllk      ", "     klllk      ", "    kklllkk     ", "    klllllk     ",
  "   kkkkkkkkk    ", "   klllllllk    ", "   kkkkkkkkk    ", "                "],
 "portal": [
  "     kkkkkk     ", "   kkmmmmmmkk   ", "  kmm dddd mmk  ", " kmm dssssd mmk ",
  " km dsslllsd mk ", " km dsl  lsd mk ", " km dsl  lsd mk ", " km dsl  lsd mk ",
  " km dsl  lsd mk ", " km dsl  lsd mk ", " km dsslllsd mk ", " kmm dssssd mmk ",
  "  kmm dddd mmk  ", "   kkmmmmmmkk   ", "     kkkkkk     ", "                "],
 "chevrons": [
  "                ", "       kk       ", "      kllk      ", "     kllllk     ",
  "    kll  llk    ", "   kll    llk   ", "  kk        kk  ", "                ",
  "       kk       ", "      kllk      ", "     kllllk     ", "    kll  llk    ",
  "   kll    llk   ", "  kk        kk  ", "                ", "                "],
 "pin": [
  "      kkkk      ", "    kkllllkk    ", "   klllllllk    ", "  kll kk lllk   ",
  "  kl kddk llk   ", "  kl kddk llk   ", "  kll kk lllk   ", "   klllllllk    ",
  "    klllllk     ", "     klllk      ", "      klk       ", "      klk       ",
  "       k        ", "                ", "                ", "                "],
 "gift": [
  "                ", "     k    k     ", "    kdk  kdk    ", "   kkdddddkk    ",
  "  kllllllllllk  ", "  kl ll ll llk  ", "  kkkkkkkkkkkk  ", "  kllll llllk   ",
  "  kl ll ll llk  ", "  kl ll ll llk  ", "  kl ll ll llk  ", "  kl ll ll llk  ",
  "  kllll llllk   ", "  kkkkkkkkkkkk  ", "                ", "                "],
 "potion": [
  "      kkk       ", "      kmk       ", "      kmk       ", "     kdddk      ",
  "     kd dk      ", "    kd   dk     ", "   kdlllllldk   ", "   kdl ssl dk   ",
  "   kdlsssss dk  ", "   kdsssssss k  ", "   kdsssssssk   ", "   kdlsssss dk  ",
  "    kdlllll dk  ", "    kkdddddkk   ", "      kkkk      ", "                "],
 "speech": [
  "                ", " kkkkkkkkkkkkk  ", " klllllllllll k ", " kl ddddddd l k ",
  " kl lllllll l k ", " kl ddddddd l k ", " kl lllllll l k ", " kl ddddddd l k ",
  " kllllllllll k  ", " kkkkkkkkkkk k  ", "    klk kk       ", "    klk         ",
  "    kk          ", "                ", "                ", "                "],
 "command": [
  "                ", " kkkkkkkkkkkkk  ", " kdddddddddddk  ", " kd l       dk  ",
  " kd ll      dk  ", " kd  ll     dk  ", " kd l ll    dk  ", " kd l  ll   dk  ",
  " kd l ll    dk  ", " kd ll      dk  ", " kd l   lll dk  ", " kd     lll dk  ",
  " kddddddddddddk ", " kkkkkkkkkkkkk  ", "                ", "                "],
 "sapling": [
  "        k       ", "       klk      ", "    kk klk kk   ", "   kllkklkkllk  ",
  "   kllllllllk   ", "    kllklllk    ", "      klk       ", "      klk       ",
  "      klk       ", "      klk       ", "     kklkk      ", "    kmmlmmk     ",
  "   kmmmmmmmk    ", "   kkkkkkkkk    ", "                ", "                "],
 "wheat": [
  "      klk       ", "     k klk      ", "    kl klkl     ", "    kklklkk     ",
  "   kl kklk lk   ", "   klkklklkkl   ", "    kklklklk    ", "   kl kklk lk   ",
  "   klkklklkkl   ", "    kklklklk    ", "      klk       ", "      klk       ",
  "      klk       ", "     kkkkk      ", "                ", "                "],
 "swords": [
  "  k        kk   ", "  klk     kwk   ", "   klk   klk    ", "    klk klk     ",
  "     klklk      ", "      kkk       ", "     klklk      ", "    klk klk     ",
  "   klk   klk    ", "  kklk   klkk   ", "  kk k   k kk   ", "                ",
  "                ", "                ", "                ", "                "],
 "gear": [
  "      kkkk      ", "    k kllk k    ", "   klkkllkklk   ", "   kllllllllk   ",
  "  kkllk  kllkk  ", " klll k  k lllk ", " kll k    k llk ", " kll k    k llk ",
  " klll k  k lllk ", "  kkllk  kllkk  ", "   kllllllllk   ", "   klkkllkklk   ",
  "    k kllk k    ", "      kkkk      ", "                ", "                "],
}


def proc_icon(kind):
    img, d = tile(16, 16)
    if kind == "heart":
        for (x0, x1, y) in [(3, 6, 3), (9, 12, 3), (2, 13, 4), (2, 13, 5),
                            (2, 13, 6), (2, 13, 7), (3, 12, 8), (4, 11, 9),
                            (5, 10, 10), (6, 9, 11), (7, 8, 12)]:
            d.line([(x0, y), (x1, y)], fill=A(FACE))
        d.line([(2, 3), (6, 3)], fill=A(BLACK)); d.line([(9, 3), (13, 3)], fill=A(BLACK))
        for (x, y) in [(1, 4), (1, 5), (1, 6), (14, 4), (14, 5), (14, 6),
                       (2, 7), (13, 7), (3, 9), (12, 9), (7, 13), (8, 13)]:
            d.point((x, y), fill=A(BLACK))
        d.line([(3, 4), (4, 5)], fill=A(WHITE))
    elif kind == "shield":
        d.polygon([(3, 2), (12, 2), (12, 9), (8, 14), (3, 9)], fill=A(FACE),
                  outline=A(BLACK))
        d.line([(8, 3), (8, 12)], fill=A(SHADOW))
        d.line([(4, 6), (11, 6)], fill=A(SHADOW))
    elif kind == "sun":
        d.ellipse([5, 5, 10, 10], fill=A(FACE), outline=A(BLACK))
        for (x, y) in [(7, 1), (8, 1), (7, 14), (8, 14), (1, 7), (1, 8),
                       (14, 7), (14, 8), (3, 3), (12, 12), (12, 3), (3, 12)]:
            d.point((x, y), fill=A(DARK))
    elif kind == "new_dot":
        d.ellipse([4, 4, 11, 11], fill=A(WHITE), outline=A(BLACK))
        d.ellipse([6, 6, 9, 9], fill=A(DARK))
    elif kind == "clock":
        d.ellipse([2, 2, 13, 13], fill=A(FACE), outline=A(BLACK))
        d.line([(8, 8), (8, 4)], fill=A(DARK))
        d.line([(8, 8), (11, 9)], fill=A(DARK))
    elif kind == "xp":
        d.ellipse([3, 3, 12, 12], fill=A(FACE), outline=A(BLACK))
        d.line([(5, 5), (10, 10)], fill=A(SHADOW))
        d.line([(10, 5), (5, 10)], fill=A(SHADOW))
        d.point((6, 4), fill=A(WHITE))
    elif kind == "exclamation":
        d.rectangle([7, 2, 8, 9], fill=A(DARK))
        d.rectangle([7, 11, 8, 12], fill=A(DARK))
    elif kind == "check":
        paint_check(d, 3, 3)
    elif kind == "lock":
        paint_lock(d, 3, 3)
    elif kind == "repeat":
        paint_repeat(d, 3, 3)
    elif kind == "star":
        paint_star(d, 4, 4)
    return img


# ==========================================================================
# Registry
# ==========================================================================
REG = []   # list of dicts: name, img, kind, element, state, section, folder
SPRITES = {}


def reg(name, img, kind, element, state, section, folder):
    REG.append(dict(name=name, img=img, kind=kind, element=element,
                    state=state, section=section, folder=folder))
    SPRITES[name] = img


def add_bg(name, img, section="1. Background"):
    reg(name, img, "background", name, "", section, DIRS["background"])


def add_int(element, state, img, section):
    reg(f"{element}.{state}", img, "interactive", element, state, section,
        os.path.join(DIRS["interactive"], element))


def add_icon(iset, name, img, section="5. Icons"):
    reg(f"{iset}.{name}", img, "icon", iset, name, section,
        os.path.join(DIRS["icons"], iset))


def add_overlay(name, img, section="6. Overlay"):
    reg(name, img, "overlay", name, "", section, DIRS["overlay"])


def add_hud(name, img, section="12. HUD"):
    reg(name, img, "hud", name, "", section, DIRS["hud"])


def add_anim(name, img, section="7. Animation"):
    reg(name, img, "anim", name, "", section, DIRS["anim"])


# ==========================================================================
# 1. BACKGROUND  (window 9-slice, panes, dividers, variants)
# ==========================================================================
WIN_W, WIN_H = 248, 184
PANE_TOP, TAB_BAND = 20, 22
LIST_X, LIST_W = 5, 84
LIST_Y = PANE_TOP + TAB_BAND
LIST_H = (WIN_H - 6) - LIST_Y
SCROLL_X = LIST_X + LIST_W + 1
DIV_X = SCROLL_X + 8 + 1
DET_X = DIV_X + 3
DET_W = WIN_W - DET_X - 5
DET_H = (WIN_H - 6) - PANE_TOP


def panel_tiles(prefix, c=8):
    """Explicit 9-slice tiles for a raised panel: 4 corners, 4 edges, centre.
    Each border is 3px (outline + white + face); centre is flat face."""
    def corner(cx, cy):
        img, d = tile(c, c)
        d.rectangle([0, 0, c - 1, c - 1], fill=A(FACE))
        if cy == 0:
            d.line([(0, 0), (c - 1, 0)], fill=A(BLACK)); d.line([(1, 1), (c - 1, 1)], fill=A(WHITE))
        else:
            d.line([(0, c - 1), (c - 1, c - 1)], fill=A(BLACK)); d.line([(1, c - 2), (c - 1, c - 2)], fill=A(SHADOW))
        if cx == 0:
            d.line([(0, 0), (0, c - 1)], fill=A(BLACK)); d.line([(1, 1), (1, c - 1)], fill=A(WHITE))
        else:
            d.line([(c - 1, 0), (c - 1, c - 1)], fill=A(BLACK)); d.line([(c - 2, 1), (c - 2, c - 1)], fill=A(SHADOW))
        return img
    add_bg(f"{prefix}.tl", corner(0, 0)); add_bg(f"{prefix}.tr", corner(1, 0))
    add_bg(f"{prefix}.bl", corner(0, 1)); add_bg(f"{prefix}.br", corner(1, 1))
    # edges (repeatable, c long)
    et, dt = tile(c, 3); dt.rectangle([0, 0, c - 1, 2], fill=A(FACE)); dt.line([(0, 0), (c - 1, 0)], fill=A(BLACK)); dt.line([(0, 1), (c - 1, 1)], fill=A(WHITE))
    add_bg(f"{prefix}.top", et)
    eb, db = tile(c, 3); db.rectangle([0, 0, c - 1, 2], fill=A(FACE)); db.line([(0, 2), (c - 1, 2)], fill=A(BLACK)); db.line([(0, 1), (c - 1, 1)], fill=A(SHADOW))
    add_bg(f"{prefix}.bottom", eb)
    el, dl = tile(3, c); dl.rectangle([0, 0, 2, c - 1], fill=A(FACE)); dl.line([(0, 0), (0, c - 1)], fill=A(BLACK)); dl.line([(1, 0), (1, c - 1)], fill=A(WHITE))
    add_bg(f"{prefix}.left", el)
    er, dr = tile(3, c); dr.rectangle([0, 0, 2, c - 1], fill=A(FACE)); dr.line([(2, 0), (2, c - 1)], fill=A(BLACK)); dr.line([(1, 0), (1, c - 1)], fill=A(SHADOW))
    add_bg(f"{prefix}.right", er)
    ce, dc = tile(c, c); dc.rectangle([0, 0, c - 1, c - 1], fill=A(FACE))
    add_bg(f"{prefix}.center", ce)


def assemble_window(w, h):
    img, d = tile(w, h)
    d.rectangle([0, 0, w - 1, h - 1], fill=A(FACE), outline=A(BLACK))
    bevel(d, 0, 0, w, h, WHITE, SHADOW)
    d.line([(4, PANE_TOP - 3), (w - 5, PANE_TOP - 3)], fill=A(DARK))
    d.line([(4, PANE_TOP - 2), (w - 5, PANE_TOP - 2)], fill=A(WHITE))
    li, _ = slot(LIST_W, h - 6 - LIST_Y); img.alpha_composite(li, (LIST_X, LIST_Y))
    de, _ = slot(w - DET_X - 5, h - 6 - PANE_TOP); img.alpha_composite(de, (DET_X, PANE_TOP))
    d.line([(DIV_X, PANE_TOP), (DIV_X, h - 7)], fill=A(DARK))
    d.line([(DIV_X + 1, PANE_TOP), (DIV_X + 1, h - 7)], fill=A(WHITE))
    d.line([(LIST_X, LIST_Y - 2), (LIST_X + LIST_W - 1, LIST_Y - 2)], fill=A(DARK))
    return img


def simple_panel(w, h):
    """Plain raised vanilla panel (no panes) - for dialogs, toasts, popups."""
    img, d = tile(w, h)
    d.rectangle([0, 0, w - 1, h - 1], fill=A(FACE), outline=A(BLACK))
    bevel(d, 0, 0, w, h, WHITE, SHADOW)
    return img


def build_background():
    panel_tiles("window")
    add_bg("window", assemble_window(WIN_W, WIN_H), "1. Background (sample 248x184)")
    add_bg("window_wide", assemble_window(280, 184), "1. Background (3-pane sample)")
    add_bg("window_compact", assemble_window(200, 150), "1. Background (compact)")
    tb, dtb = tile(240, 16); dtb.rectangle([0, 0, 239, 15], fill=A(FACE)); dtb.line([(0, 15), (239, 15)], fill=A(DARK)); dtb.line([(0, 14), (239, 14)], fill=A(SHADOW))
    add_bg("titlebar", tb)
    dv, ddv = tile(2, 120); ddv.line([(0, 0), (0, 119)], fill=A(DARK)); ddv.line([(1, 0), (1, 119)], fill=A(WHITE))
    add_bg("divider_v", dv)
    dh, ddh = tile(120, 2); ddh.line([(0, 0), (119, 0)], fill=A(DARK)); ddh.line([(0, 1), (119, 1)], fill=A(WHITE))
    add_bg("divider_h", dh)
    add_bg("list_pane", slot(LIST_W, LIST_H)[0])
    add_bg("detail_pane", slot(DET_W, DET_H)[0])


# ==========================================================================
# 2. INTERACTIVE controls
# ==========================================================================
def build_interactive():
    # ---- tabs (top + side) ----
    def tab(state, side=False):
        w, h = 20, 20
        if state == "selected":
            img, d = tile(w, h)
            d.rectangle([0, 0, w - 1, h - 1], fill=A(FACE))
            d.line([(0, 0), (w - 1, 0)], fill=A(BLACK)); d.line([(0, 0), (0, h - 1)], fill=A(BLACK)); d.line([(w - 1, 0), (w - 1, h - 1)], fill=A(BLACK))
            d.line([(1, 1), (w - 2, 1)], fill=A(WHITE)); d.line([(1, 1), (1, h - 2)], fill=A(WHITE)); d.line([(w - 2, 1), (w - 2, h - 1)], fill=A(SHADOW))
            return img
        return face_box(w, h, state)[0]
    for st in ("normal", "hover", "pressed", "disabled", "selected", "focused"):
        add_int("tab", st, tab(st), "4.2 Navigation - tab")
    for st in ("normal", "selected"):
        add_int("tab_side", st, tab(st, True), "4.2 Navigation - tab (side)")
    # tab_overflow « »
    for st in ("prev", "next"):
        img, d = face_box(12, 16, "normal")
        paint_tri(d, 6, 8, "left" if st == "prev" else "right", DARK, 2)
        paint_tri(d, 3 if st == "prev" else 9, 8, "left" if st == "prev" else "right", DARK, 2)
        add_int("tab_overflow", st, img, "4.2 Navigation - tab overflow")

    # ---- search field ----
    def search(state):
        w, h = 100, 14
        img, d = slot(w, h, outline=BLACK)
        if state == "focused":
            d.rectangle([0, 0, w - 1, h - 1], outline=A(WHITE))
            d.line([(4, 3), (4, h - 4)], fill=A(WHITE))   # caret
        if state == "placeholder":
            d.line([(4, h // 2), (40, h // 2)], fill=A(SHADOW))  # ghost text line
        return img
    for st in ("normal", "focused", "placeholder"):
        add_int("search_field", st, search(st), "4.2 Navigation - search")
    img, d = face_box(10, 10, "normal"); d.line([(3, 3), (6, 6)], fill=A(DARK)); d.line([(6, 3), (3, 6)], fill=A(DARK))
    add_int("search_field", "clear_x", img, "4.2 Navigation - search")

    # ---- sort / filter ----
    for st in BTN4:
        img, d = face_box(16, 16, st); paint_tri(d, 8, 6 + sh(st), "up", gcol(st), 2); paint_tri(d, 8, 11 + sh(st), "down", gcol(st), 2)
        add_int("sort_button", st, img, "4.2 Navigation - sort")
    for st in ("asc", "desc"):
        img, d = face_box(10, 10, "normal"); paint_tri(d, 5, 5, "up" if st == "asc" else "down", DARK, 3)
        add_int("sort_dir", st, img, "4.2 Navigation - sort")
    for st in BTN4:
        img, d = face_box(16, 16, st)
        d.polygon([(3, 4 + sh(st)), (12, 4 + sh(st)), (9, 8 + sh(st)), (9, 12 + sh(st)), (6, 12 + sh(st)), (6, 8 + sh(st))], fill=A(gcol(st)))  # funnel
        add_int("filter_button", st, img, "4.2 Navigation - filter")
    for st in ("collapsed", "expanded", "row", "row_hover", "check_on", "check_off"):
        if st in ("collapsed", "expanded"):
            img, d = face_box(80, 14, "normal"); paint_tri(d, 72, 7, "down" if st == "collapsed" else "up", DARK, 2)
        elif st in ("row", "row_hover"):
            img, d = tile(80, 14)
            if st == "row_hover":
                d.rectangle([0, 0, 79, 13], fill=A(FACE), outline=A(WHITE))
            d.line([(0, 13), (79, 13)], fill=A(DARK))
        else:
            img, d = slot(12, 12, outline=BLACK)
            if st == "check_on":
                paint_check(d, 1, 1)
        add_int("filter_dropdown", st, img, "4.2 Navigation - filter dropdown")

    # ---- page nav ----
    for st in BTN4:
        add_int("page_prev", st, arrow_btn(12, "left", st), "4.2 Navigation - page")
        add_int("page_next", st, arrow_btn(12, "right", st), "4.2 Navigation - page")
    for st in ("active", "inactive"):
        img, d = tile(6, 6)
        if st == "active":
            d.ellipse([0, 0, 5, 5], fill=A(DARK))
        else:
            d.ellipse([0, 0, 5, 5], outline=A(SHADOW))
        add_int("page_dots", st, img, "4.2 Navigation - page dots")

    # ---- quest_row (full state set) ----
    for st in ("available", "hover", "selected", "active", "completed",
               "locked", "claimable", "new", "favorited", "pinned",
               "expired", "in_progress"):
        add_int("quest_row", st, quest_row(st), "4.3 Quest list - row")
    # group header
    img, d = tile(80, 12); d.rectangle([0, 0, 79, 11], fill=A(INSET)); d.line([(0, 0), (79, 0)], fill=A(DARK)); d.line([(0, 11), (79, 11)], fill=A(WHITE))
    add_int("group_header", "normal", img, "4.3 Quest list - group header")
    rd, drd = tile(80, 1); drd.line([(0, 0), (79, 0)], fill=A(DARK))
    add_int("row_divider", "normal", rd, "4.3 Quest list - divider")
    rp, drp = tile(80, 2); drp.rectangle([0, 0, 47, 1], fill=A(WHITE)); drp.rectangle([48, 0, 79, 1], fill=A(SHADOW))
    add_int("row_progress_inline", "normal", rp, "4.3 Quest list - inline progress")

    # ---- icon_frame ----
    for st in ("normal", "selected", "empty"):
        img, _ = slot(18, 18)
        if st == "selected":
            ImageDraw.Draw(img).rectangle([0, 0, 17, 17], outline=A(WHITE))
        if st == "empty":
            img = recolor(img, {FACE: INSET})  # subtle; mostly identical
        add_int("icon_frame", st, img, "4.4 Detail - icon frame")

    # ---- objective_row ----
    for st in ("incomplete", "complete"):
        img, d = tile(120, 16)
        sl, _ = slot(14, 14); img.alpha_composite(sl, (1, 1))      # type-icon slot
        tr = progress_bar("track"); img.alpha_composite(tr, (38, 9))
        if st == "complete":
            fl = progress_bar("fill"); img.alpha_composite(fl, (38, 9))
            ch, dch = tile(10, 10); paint_check(dch, 0, 0); img.alpha_composite(ch, (108, 3))
        add_int("objective_row", st, img, "4.4 Detail - objective row")

    # ---- progress_bar variants ----
    for st in ("track", "fill", "segmented", "large", "mini", "ready"):
        add_int("progress_bar", st, progress_bar(st), "4.4 Detail - progress bar")

    # ---- reward_slot ----
    for st in ("normal", "hover", "claimed", "choice", "locked", "focused"):
        img, _ = slot(20, 20, outline=BLACK)
        d = ImageDraw.Draw(img)
        if st == "hover":
            d.rectangle([0, 0, 19, 19], outline=A(WHITE))
        if st == "claimed":
            img = recolor(img, {INSET: SHADOW})
            d = ImageDraw.Draw(img); paint_check(d, 6, 6)
        if st == "choice":
            d.rectangle([0, 0, 19, 19], outline=A(WHITE)); d.rectangle([1, 1, 18, 18], outline=A(WHITE))
        if st == "locked":
            img = recolor(img, {INSET: SHADOW, FACE: SHADOW}); d = ImageDraw.Draw(img); paint_lock(d, 6, 6)
        if st == "focused":
            focus_ring(d, 0, 0, 20, 20)
        add_int("reward_slot", st, img, "4.4 Detail - reward slot")

    # difficulty pips
    for n, name in ((1, "easy"), (2, "normal"), (3, "hard")):
        img, d = tile(20, 6)
        for i in range(3):
            cx = 1 + i * 6
            if i < n:
                d.ellipse([cx, 1, cx + 3, 4], fill=A(DARK), outline=A(BLACK))
            else:
                d.ellipse([cx, 1, cx + 3, 4], outline=A(SHADOW))
        add_int("difficulty_pips", name, img, "4.4 Detail - difficulty")

    # badges
    def b_repeatable(d):
        paint_repeat(d, 3, 3)

    def b_cooldown(d):
        d.ellipse([3, 3, 12, 12], outline=A(BLACK))
        d.line([(8, 8), (8, 4)], fill=A(DARK)); d.line([(8, 8), (10, 9)], fill=A(DARK))

    def b_daily(d):
        d.ellipse([4, 4, 11, 11], fill=A(FACE), outline=A(BLACK))
        for x, y in [(7, 1), (7, 14), (1, 7), (14, 7)]:
            d.point((x, y), fill=A(DARK))

    def b_new(d):
        paint_star(d, 3, 3)

    for name, painter in (("repeatable", b_repeatable), ("cooldown", b_cooldown),
                          ("daily", b_daily), ("new", b_new)):
        img, d = tile(16, 16)
        d.rectangle([0, 0, 15, 15], fill=A(FACE), outline=A(BLACK))
        bevel(d, 0, 0, 16, 16, WHITE, SHADOW)
        painter(d)
        add_int("badge", name, img, "4.4 Detail - badges")

    # buttons
    for el, glyph in (("button_claim", None), ("button_abandon", "x"),
                      ("button_track", "pin"), ("button_back", "back"),
                      ("button_settings", "gear"), ("button_help", "?"),
                      ("button_info", "i")):
        for st in BTN4 + ["focused"]:
            if el in ("button_claim", "button_abandon"):
                img, d = face_box(72, 20, st)
            elif el == "button_track":
                img, d = face_box(60, 16, st)
            else:
                img, d = face_box(14, 14, st)
            o = sh(st); col = gcol(st)
            if glyph == "x":
                d.line([(30 + o, 6 + o), (42 + o, 14 + o)], fill=A(col)); d.line([(42 + o, 6 + o), (30 + o, 14 + o)], fill=A(col))
            elif glyph == "pin":
                paint_tri(d, 30 + o, 8 + o, "left", col, 2); d.line([(30 + o, 8 + o), (40 + o, 8 + o)], fill=A(col))
            elif glyph == "back":
                paint_tri(d, 5 + o, 7 + o, "left", col, 2); d.line([(5 + o, 7 + o), (9 + o, 7 + o)], fill=A(col))
            elif glyph == "gear":
                d.ellipse([4 + o, 4 + o, 9 + o, 9 + o], outline=A(col)); d.point((6 + o, 6 + o), fill=A(col))
            elif glyph == "?":
                d.arc([4 + o, 3 + o, 9 + o, 8 + o], 0, 270, fill=A(col)); d.point((7 + o, 11 + o), fill=A(col))
            elif glyph == "i":
                d.point((7 + o, 3 + o), fill=A(col)); d.line([(7 + o, 5 + o), (7 + o, 10 + o)], fill=A(col))
            add_int(el, st, img, "4.4 Detail - buttons")
    # close button (small X), used by the window corner
    for st in BTN4 + ["focused"]:
        img, d = face_box(11, 11, st); o = sh(st); col = gcol(st)
        d.line([(3 + o, 3 + o), (7 + o, 7 + o)], fill=A(col))
        d.line([(7 + o, 3 + o), (3 + o, 7 + o)], fill=A(col))
        add_int("button_close", st, img, "4.4 Detail - buttons")

    # ---- scroll (V + H) ----
    def sc_track(vert=True):
        w, h = (8, 120) if vert else (120, 8)
        img, d = tile(w, h); d.rectangle([0, 0, w - 1, h - 1], fill=A(INSET), outline=A(BLACK))
        d.line([(1, 1), (w - 2, 1)], fill=A(DARK)); d.line([(1, 1), (1, h - 2)], fill=A(DARK))
        return img
    add_int("scroll_track", "normal", sc_track(True), "4.5 Scroll V")
    add_int("scroll_track_h", "normal", sc_track(False), "4.5 Scroll H")
    for st in ("normal", "hover", "grabbed"):
        s2 = "pressed" if st == "grabbed" else st
        img, d = face_box(8, 16, s2)
        for y in (6, 8, 10):
            d.line([(2, y), (5, y)], fill=A(DARK))
        add_int("scroll_handle", st, img, "4.5 Scroll V")
        img2, d2 = face_box(16, 8, s2)
        for x in (6, 8, 10):
            d2.line([(x, 2), (x, 5)], fill=A(DARK))
        add_int("scroll_handle_h", st, img2, "4.5 Scroll H")
    for st in BTN4:
        add_int("scroll_arrow_up", st, arrow_btn(8, "up", st), "4.5 Scroll V")
        add_int("scroll_arrow_down", st, arrow_btn(8, "down", st), "4.5 Scroll V")
        add_int("scroll_arrow_left", st, arrow_btn(8, "left", st), "4.5 Scroll H")
        add_int("scroll_arrow_right", st, arrow_btn(8, "right", st), "4.5 Scroll H")

    # ---- misc controls ----
    for st in ("unchecked", "checked", "hover", "disabled"):
        img, _ = slot(12, 12, outline=BLACK)
        d = ImageDraw.Draw(img)
        if st == "checked":
            paint_check(d, 1, 1)
        if st == "hover":
            d.rectangle([0, 0, 11, 11], outline=A(WHITE))
        if st == "disabled":
            img = recolor(img, {INSET: SHADOW})
        add_int("checkbox", st, img, "4.7 Misc - checkbox")
    for st in ("off", "on"):
        img, d = tile(12, 12); d.ellipse([0, 0, 11, 11], fill=A(INSET), outline=A(BLACK))
        if st == "on":
            d.ellipse([3, 3, 8, 8], fill=A(DARK))
        add_int("radio", st, img, "4.7 Misc - radio")
    st_track, dst = tile(80, 6); dst.rectangle([0, 0, 79, 5], fill=A(INSET), outline=A(BLACK)); dst.line([(1, 1), (78, 1)], fill=A(DARK))
    add_int("slider", "track", st_track, "4.7 Misc - slider")
    add_int("slider", "handle", face_box(8, 12, "normal")[0], "4.7 Misc - slider")
    fr, dfr = tile(24, 24); focus_ring(dfr, 0, 0, 24, 24)
    add_int("focus_ring", "normal", fr, "4.7 Misc - focus ring")
    cg, dcg = tile(8, 8)
    for i in range(0, 8, 2):
        dcg.line([(i, 7), (7, i)], fill=A(SHADOW))
    add_int("corner_grip", "normal", cg, "4.7 Misc - corner grip")


# ---- shared control builders ----
BTN4 = ["normal", "hover", "pressed", "disabled"]


def arrow_btn(size, direction, state):
    img, d = face_box(size, size, state)
    c = size // 2 + sh(state)
    paint_tri(d, c, c, direction, gcol(state), r=2)
    return img


def progress_bar(state):
    if state == "large":
        w, h = 100, 10
    elif state == "mini":
        w, h = 60, 4
    else:
        w, h = 100, 6
    img, d = tile(w, h)
    if state == "fill":
        d.rectangle([0, 0, w - 1, h - 1], fill=A(WHITE)); d.line([(0, h - 1), (w - 1, h - 1)], fill=A(SHADOW)); d.line([(0, 0), (w - 1, 0)], fill=A(WHITE))
        return img
    if state == "ready":                                  # claimable highlight
        d.rectangle([0, 0, w - 1, h - 1], fill=A(WHITE), outline=A(WHITE))
        d.line([(0, h - 1), (w - 1, h - 1)], fill=A(SHADOW))
        return img
    if state == "segmented":
        d.rectangle([0, 0, w - 1, h - 1], fill=A(INSET), outline=A(BLACK))
        for x in range(10, w, 10):
            d.line([(x, 1), (x, h - 2)], fill=A(DARK))
        return img
    d.rectangle([0, 0, w - 1, h - 1], fill=A(INSET), outline=A(BLACK)); d.line([(1, 1), (w - 2, 1)], fill=A(DARK))
    return img


def quest_row(state):
    w, h = 80, 18
    img, d = tile(w, h)
    outlined = state in ("hover", "selected", "claimable", "favorited", "pinned", "new")
    if state in ("hover", "selected"):
        d.rectangle([0, 0, w - 1, h - 2], fill=A(FACE))
    d.line([(0, h - 1), (w - 1, h - 1)], fill=A(DARK))
    if state == "selected":
        d.rectangle([0, 0, w - 1, h - 2], outline=A(WHITE)); d.line([(1, 1), (w - 2, 1)], fill=A(WHITE)); d.line([(0, h - 1), (w - 1, h - 1)], fill=(0, 0, 0, 0))
    elif state == "hover":
        d.rectangle([0, 0, w - 1, h - 2], outline=A(WHITE))
    elif state == "claimable":
        d.rectangle([0, 0, w - 1, h - 2], outline=A(WHITE))
    elif state == "active":
        d.rectangle([0, 0, 2, h - 2], fill=A(WHITE))
    elif state == "in_progress":
        d.rectangle([0, h - 3, int(w * 0.5), h - 2], fill=A(WHITE))
    elif state == "completed":
        sl, _ = slot(13, 13); img.alpha_composite(sl, (w - 15, 3)); paint_check(d, w - 13, 3)
    elif state == "locked":
        d.rectangle([0, 0, w - 1, h - 2], fill=A(INSET)); d.line([(0, h - 1), (w - 1, h - 1)], fill=A(DARK)); paint_lock(d, w - 12, 4)
    elif state == "new":
        d.ellipse([w - 8, 4, w - 4, 8], fill=A(WHITE), outline=A(BLACK))
    elif state == "favorited":
        paint_star(d, w - 11, 4)
    elif state == "pinned":
        paint_tri(d, w - 8, 8, "left", DARK, 2); d.line([(w - 8, 8), (w - 4, 8)], fill=A(DARK))
    elif state == "expired":
        img = recolor(img, {}); d = ImageDraw.Draw(img)
        for x in range(0, w, 3):
            d.point((x, h // 2), fill=A(SHADOW))
    return img


# ==========================================================================
# 5. ICONS
# ==========================================================================
OBJECTIVE = {
    "collect_item": "crate", "mine_block": "pickaxe", "craft_item": "craft",
    "smelt_item": "furnace", "consume_item": "apple", "place_block": "cube",
    "kill_mob": "sword", "tame_animal": "bone", "breed_animal": "heart",
    "gain_advancement": "trophy", "visit_dimension": "portal",
    "reach_level": "chevrons", "reach_location": "pin",
}
REWARD = {"give_item": "gift", "loot_table": "crate", "xp": "xp",
          "effect": "potion", "message": "speech", "command": "command"}
CATEGORY = {"gathering": "sapling", "farming": "wheat", "combat": "swords",
            "survival": "shield", "daily": "sun", "custom": "gear"}
GLYPHS = ["check", "lock", "repeat", "star", "new_dot", "clock", "xp",
          "exclamation"]


def make_icon(shape):
    return icon(P[shape]) if shape in P else proc_icon(shape)


def derive_done(base):
    img = base.copy(); d = ImageDraw.Draw(img)
    d.rectangle([9, 9, 15, 15], fill=(0, 0, 0, 0))
    d.rectangle([9, 9, 15, 15], outline=A(BLACK)); d.rectangle([10, 10, 14, 14], fill=A(FACE))
    paint_check(d, 9, 9)
    return img


def derive_locked(base):
    return recolor(base, {FACE: INSET, WHITE: INSET, SHADOW: INSET,
                          DARK: SHADOW, BLACK: SHADOW})


def build_icons():
    for name, shape in OBJECTIVE.items():
        base = make_icon(shape)
        add_icon("objective", name, base, "5. Icons - objective")
        add_icon("objective", f"{name}__done", derive_done(base), "5. Icons - objective")
        add_icon("objective", f"{name}__locked", derive_locked(base), "5. Icons - objective")
    for name, shape in REWARD.items():
        add_icon("reward", name, make_icon(shape), "5. Icons - reward")
    for name, shape in CATEGORY.items():
        add_icon("category", name, make_icon(shape), "5. Icons - category")
    for g in GLYPHS:
        add_icon("glyph", g, proc_icon(g), "5. Icons - glyphs")


# ==========================================================================
# 6. OVERLAY  (dialog, tooltip x2, toast, empty state)
# ==========================================================================
def build_overlay():
    # dialog modal
    dlg = simple_panel(160, 80)
    dd0 = ImageDraw.Draw(dlg); dd0.line([(8, 16), (151, 16)], fill=A(DARK)); dd0.line([(8, 17), (151, 17)], fill=A(WHITE))
    add_overlay("dialog", dlg, "6. Overlay - dialog")
    for st in BTN4:
        add_overlay(f"dialog_button_yes.{st}", face_box(50, 18, st)[0], "6. Overlay - dialog")
        add_overlay(f"dialog_button_no.{st}", face_box(50, 18, st)[0], "6. Overlay - dialog")
    # tooltip vanilla (purple border, documented exception)
    tv, d = tile(16, 16)
    d.rectangle([1, 1, 14, 14], fill=(*TIP_FILL, 240))
    d.rectangle([0, 0, 15, 15], outline=(*TIP_FILL, 240))
    d.rectangle([1, 1, 14, 14], outline=(*TIP_B1, 255)); d.rectangle([2, 2, 13, 13], outline=(*TIP_B2, 255))
    add_overlay("tooltip_vanilla", tv, "6. Overlay - tooltip")
    # tooltip greyscale
    tg, dg = tile(16, 16)
    dg.rectangle([1, 1, 14, 14], fill=(*DARK, 240)); dg.rectangle([0, 0, 15, 15], outline=A(BLACK)); dg.rectangle([1, 1, 14, 14], outline=A(INSET))
    add_overlay("tooltip_grey", tg, "6. Overlay - tooltip")
    # toast / popup
    toast = simple_panel(140, 30)
    dd = ImageDraw.Draw(toast); dd.rectangle([3, 3, 9, 26], fill=A(INSET), outline=A(DARK))  # icon zone
    add_overlay("toast", toast, "6. Overlay - toast")
    # empty state (closed book, greyscale)
    es, de = tile(48, 48)
    de.rectangle([8, 10, 40, 40], fill=A(INSET), outline=A(BLACK)); bevel(de, 8, 10, 33, 31, FACE, SHADOW)
    de.rectangle([12, 14, 38, 37], fill=A(FACE), outline=A(SHADOW))
    for y in range(18, 36, 4):
        de.line([(15, y), (34, y)], fill=A(INSET))
    de.rectangle([8, 10, 12, 40], fill=A(SHADOW))
    add_overlay("empty_state", es, "6. Overlay - empty state")


# ==========================================================================
# 7. ANIMATION strips  (horizontal frame strips)
# ==========================================================================
def strip(frames, note_unused=None):
    w = max(f.width for f in frames); h = max(f.height for f in frames)
    img = Image.new("RGBA", (w * len(frames), h), (0, 0, 0, 0))
    for i, f in enumerate(frames):
        img.alpha_composite(f, (i * w, 0))
    return img


def build_anim():
    # claimable pulse (3 frames: outline breathing on a row)
    frames = []
    for k in (0, 1, 2):
        img, d = tile(80, 18); d.line([(0, 17), (79, 17)], fill=A(DARK))
        if k >= 1:
            d.rectangle([0, 0, 79, 16], outline=A(WHITE))
        if k == 2:
            d.rectangle([1, 1, 78, 15], outline=A(WHITE))
        frames.append(img)
    add_anim("claimable_pulse", strip(frames), "7. Animation (3f, ~250ms)")
    # loading spinner (8 frames)
    frames = []
    import math
    for k in range(8):
        img, d = tile(16, 16)
        for j in range(8):
            ang = math.radians((k * 45 + j * 45) % 360)
            x = 8 + int(5 * math.cos(ang)); y = 8 + int(5 * math.sin(ang))
            shade = WHITE if j == 0 else (FACE if j == 1 else (INSET if j < 4 else DARK))
            d.point((x, y), fill=A(shade)); d.point((x + 1, y), fill=A(shade))
        frames.append(img)
    add_anim("loading_spinner", strip(frames), "7. Animation (8f, ~80ms)")
    # toast slide (4 frames moving in from right)
    base = SPRITES.get("toast") or simple_panel(140, 30)
    frames = []
    for k in range(4):
        img = Image.new("RGBA", (140, 30), (0, 0, 0, 0))
        off = (3 - k) * 30
        img.alpha_composite(base, (off, 0))
        frames.append(img.crop((0, 0, 140, 30)))
    add_anim("toast_slide", strip(frames), "7. Animation (4f, ~60ms)")
    # progress shimmer (2 frames - highlight sweeps)
    frames = []
    for k in (0, 1):
        f = progress_bar("fill").copy(); d = ImageDraw.Draw(f)
        x = 20 + k * 40
        d.line([(x, 1), (x, f.height - 2)], fill=A(WHITE))
        frames.append(f)
    add_anim("progress_fill_shimmer", strip(frames), "7. Animation (2f, ~200ms)")
    # tab switch (2 frames)
    frames = [SPRITES["tab.normal"], SPRITES["tab.selected"]]
    add_anim("tab_switch", strip(frames), "7. Animation (2f)")


# ==========================================================================
# 12. HUD tracker (off-book overlay)
# ==========================================================================
def build_hud():
    panel, d = tile(120, 48)
    d.rectangle([1, 1, 118, 46], fill=(0, 0, 0, 170))
    for cx, cy in ((1, 1), (118, 1), (1, 46), (118, 46)):
        panel.putpixel((cx, cy), (0, 0, 0, 0))
    d.rectangle([0, 0, 119, 47], outline=A(BLACK))
    for cx, cy in ((0, 0), (119, 0), (0, 47), (119, 47)):
        panel.putpixel((cx, cy), (0, 0, 0, 0))
    d.line([(2, 1), (117, 1)], fill=(*WHITE, 120))
    add_hud("hud_panel", panel, "12. HUD")
    hr, dr = tile(110, 10)
    tr = progress_bar("mini"); hr.alpha_composite(tr, (4, 4))
    add_hud("hud_row", hr, "12. HUD")
    for st in ("pin", "unpin"):
        img, d = face_box(10, 10, "normal")
        if st == "pin":
            paint_tri(d, 5, 5, "down", DARK, 2)
        else:
            d.ellipse([3, 3, 7, 7], outline=A(DARK))
        add_hud(f"hud_{st}", img, "12. HUD")


# ==========================================================================
# Build everything
# ==========================================================================
build_background()
build_interactive()
build_icons()
build_overlay()
build_anim()
build_hud()


# ==========================================================================
# Save the PNG tree + per-element strips
# ==========================================================================
def save_tree():
    for r in REG:
        folder = r["folder"]
        os.makedirs(folder, exist_ok=True)
        if r["kind"] == "interactive":
            fn = f"{r['state']}.png"
        elif r["kind"] == "icon":
            fn = f"{r['state']}.png"
        else:
            fn = f"{r['name']}.png"
        r["img"].save(os.path.join(folder, fn))
    # strips per interactive element
    by_el = {}
    for r in REG:
        if r["kind"] == "interactive":
            by_el.setdefault(r["element"], []).append(r)
    for el, items in by_el.items():
        if len(items) < 2:
            continue
        cw = max(i["img"].width for i in items)
        ch = max(i["img"].height for i in items)
        st = Image.new("RGBA", (cw, ch * len(items)), (0, 0, 0, 0))
        for i, it in enumerate(items):
            st.alpha_composite(it["img"], (0, i * ch))
        st.save(os.path.join(DIRS["strips"], f"{el}.png"))


save_tree()


# ==========================================================================
# Pack atlas(es)
# ==========================================================================
class Packer:
    def __init__(self, base):
        self.base = base; self.atlases = []; self.records = []; self._new()

    def _new(self):
        i = len(self.atlases)
        name = self.base if i == 0 else f"{self.base}_{i + 1}"
        img = Image.new("RGBA", (ATLAS, ATLAS), (0, 0, 0, 0))
        self.atlases.append((name, img)); self.cur = name; self.img = img
        self.x = self.y = GUTTER; self.shh = 0

    def place(self, name, t, section, notes=""):
        w, h = t.size
        if w > ATLAS - 2 * GUTTER or h > ATLAS - 2 * GUTTER:
            t = t.crop((0, 0, min(w, ATLAS), min(h, ATLAS))); w, h = t.size
        if self.x + w + GUTTER > ATLAS:
            self.y += self.shh + GUTTER; self.x = GUTTER; self.shh = 0
        if self.y + h + GUTTER > ATLAS:
            self._new()
        self.img.paste(t, (self.x, self.y))
        self.records.append((name, self.cur, self.x, self.y, w, h, section, notes))
        self.x += w + GUTTER; self.shh = max(self.shh, h)


packer = Packer("quest_full")
# pack in height-sorted order for density; animation strips and any sprite too
# big for a 256 atlas (e.g. window_wide) are delivered as standalone files.
order = sorted(REG, key=lambda r: (-r["img"].size[1], -r["img"].size[0]))
UNPACKED = []
for r in order:
    im = r["img"]
    if r["kind"] == "anim" or im.width > ATLAS - 2 * GUTTER or im.height > ATLAS - 2 * GUTTER:
        UNPACKED.append(r)
        continue
    packer.place(r["name"], im, r["section"], (r["kind"] + " " + r["state"]).strip())
ATLASES = dict(packer.atlases)
MAP = packer.records
for name, img in ATLASES.items():
    img.save(os.path.join(DIRS["atlas"], f"{name}.png"))
    img.resize((ATLAS * 3, ATLAS * 3), Image.NEAREST).save(
        os.path.join(DIRS["preview"], f"{name}_3x.png"))


# ==========================================================================
# Themes (palette swap) + 2x
# ==========================================================================
THEMES = {
    "standard": {},
    "dark": {WHITE: (170, 170, 170), FACE: (120, 120, 120), INSET: (85, 85, 85),
             SHADOW: (55, 55, 55), DARK: (35, 35, 35)},
    "highcontrast": {WHITE: (255, 255, 255), FACE: (225, 225, 225),
                     INSET: (150, 150, 150), SHADOW: (60, 60, 60),
                     DARK: (0, 0, 0)},
}
for tname, mp in THEMES.items():
    tdir = os.path.join(DIRS["themes"], tname); os.makedirs(tdir, exist_ok=True)
    for aname, img in ATLASES.items():
        (recolor(img, mp) if mp else img).save(os.path.join(tdir, f"{aname}.png"))

x2dir = os.path.join(DIRS["2x"], "atlas"); os.makedirs(x2dir, exist_ok=True)
for aname, img in ATLASES.items():
    img.resize((img.width * 2, img.height * 2), Image.NEAREST).save(
        os.path.join(x2dir, f"{aname}@2x.png"))


# ==========================================================================
# Verify
# ==========================================================================
def verify():
    problems = []
    by_atlas = {}
    for rec in MAP:
        by_atlas.setdefault(rec[1], []).append(rec)
    for atlas, recs in by_atlas.items():
        for i in range(len(recs)):
            n, a, u, v, w, h, s, nt = recs[i]
            if u < 0 or v < 0 or u + w > ATLAS or v + h > ATLAS:
                problems.append(f"OOB {n}")
            for j in range(i + 1, len(recs)):
                _, _, u2, v2, w2, h2, _, _ = recs[j]
                if u < u2 + w2 and u + w > u2 and v < v2 + h2 and v + h > v2:
                    problems.append(f"OVERLAP {n} vs {recs[j][0]}")
    off = 0
    for name, img in ATLASES.items():
        px = img.load()
        for y in range(img.height):
            for x in range(img.width):
                r, g, b, a = px[x, y]
                if a == 255 and (r, g, b) not in ALLOWED_RGB:
                    off += 1
    if off:
        problems.append(f"OFF-PALETTE opaque pixels: {off}")
    return problems


issues = verify()


# ==========================================================================
# TEXTURE_MAP.md + atlas_map.txt
# ==========================================================================
def write_maps():
    with open(os.path.join(DIRS["atlas"], "atlas_map.txt"), "w", encoding="utf-8") as f:
        f.write("# name  atlas  x  y  w  h\n")
        for (n, a, u, v, w, h, s, nt) in MAP:
            f.write(f"{n}\t{a}\t{u}\t{v}\t{w}\t{h}\n")
    by_sec = {}
    for rec in MAP:
        by_sec.setdefault(rec[6], []).append(rec)
    L = ["# JustQuests v2 FULL - TEXTURE_MAP", "",
         "Pure vanilla-grey, layered. Classic `blit(x,y,u,v,w,h)`; coords are "
         "pixels within each 256x256 atlas. No AA, >=2px gutters, palette-only "
         "(+ documented tooltip exception). Also delivered un-packed under "
         "`background/ interactive/<el>/<state> icons/<set> overlay/ hud/ anim/`.",
         "", "Atlases: " + ", ".join(f"`atlas/{n}.png`" for n in sorted(ATLASES)),
         "", "Themes: `themes/{standard,dark,highcontrast}/`  |  HD: `2x/atlas/`",
         "", "In-mod path (reference): `assets/justquests/textures/gui/`", ""]

    def sec_key(s):
        tok = s.split(" ")[0].rstrip(".")
        try:
            return tuple(int(p) for p in tok.split("."))
        except ValueError:
            return (999,)
    for sec in sorted(by_sec, key=sec_key):
        L += [f"## {sec}", "", "| name | atlas | u | v | w | h |",
              "|------|-------|---|---|---|---|"]
        for (n, a, u, v, w, h, s2, nt) in by_sec[sec]:
            L.append(f"| `{n}` | {a} | {u} | {v} | {w} | {h} |")
        L.append("")
    if UNPACKED:
        L += ["## Standalone (not in atlas - use the file directly)", "",
              "| name | file | w | h |", "|------|------|---|---|"]
        for r in UNPACKED:
            sub = ("anim" if r["kind"] == "anim" else "background")
            L.append(f"| `{r['name']}` | {sub}/{r['name']}.png | "
                     f"{r['img'].width} | {r['img'].height} |")
        L.append("")
    with open(os.path.join(OUT, "TEXTURE_MAP.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(L))


write_maps()


# ==========================================================================
# Mockups
# ==========================================================================
def font(sz):
    for fn in ("arialbd.ttf", "arial.ttf", "segoeui.ttf"):
        try:
            return ImageFont.truetype(fn, sz)
        except OSError:
            continue
    return ImageFont.load_default()


def scale(img, f):
    return img.resize((img.width * f, img.height * f), Image.NEAREST)


def nine_slice(src, w, h, m=3):
    """Render a 9-slice sprite to w×h with crisp pixels (corners fixed,
    edges/centre tiled by nearest-neighbour). Mockup helper only."""
    sw, sh = src.size
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    def pc(b): return src.crop(b)
    out.alpha_composite(pc((0, 0, m, m)), (0, 0))
    out.alpha_composite(pc((sw - m, 0, sw, m)), (w - m, 0))
    out.alpha_composite(pc((0, sh - m, m, sh)), (0, h - m))
    out.alpha_composite(pc((sw - m, sh - m, sw, sh)), (w - m, h - m))
    out.alpha_composite(pc((m, 0, sw - m, m)).resize((w - 2 * m, m), Image.NEAREST), (m, 0))
    out.alpha_composite(pc((m, sh - m, sw - m, sh)).resize((w - 2 * m, m), Image.NEAREST), (m, h - m))
    out.alpha_composite(pc((0, m, m, sh - m)).resize((m, h - 2 * m), Image.NEAREST), (0, m))
    out.alpha_composite(pc((sw - m, m, sw, sh - m)).resize((m, h - 2 * m), Image.NEAREST), (w - m, m))
    out.alpha_composite(pc((m, m, sw - m, sh - m)).resize((w - 2 * m, h - 2 * m), Image.NEAREST), (m, m))
    return out


def sp(name):
    return SPRITES[name]


BACKDROP = (52, 52, 60, 255)


def mockup_window():
    W, H = WIN_W, WIN_H
    O = 4
    base = Image.new("RGBA", (W + 8, H + 8), BACKDROP)
    base.alpha_composite(sp("window"), (O, O))
    base.alpha_composite(sp("button_close.normal") if "button_close.normal" in SPRITES else sp("button_info.normal"), (O + W - 16, O + 4))
    for i in range(4):
        base.alpha_composite(sp("tab.selected" if i == 0 else "tab.normal"), (O + LIST_X + i * 21, O + PANE_TOP))
        if i < 4:
            cat = list(CATEGORY)[i]
            base.alpha_composite(sp(f"category.{cat}").resize((16, 16)), (O + LIST_X + i * 21 + 2, O + PANE_TOP + 2))
    rows = ["selected", "active", "completed", "locked", "available"]
    for i, rs in enumerate(rows):
        base.alpha_composite(sp(f"quest_row.{rs}"), (O + LIST_X + 2, O + LIST_Y + 2 + i * 19))
    base.alpha_composite(sp("scroll_arrow_up.normal"), (O + SCROLL_X, O + LIST_Y))
    base.alpha_composite(sp("scroll_track.normal"), (O + SCROLL_X, O + LIST_Y + 10))
    base.alpha_composite(sp("scroll_handle.normal"), (O + SCROLL_X, O + LIST_Y + 12))
    base.alpha_composite(sp("scroll_arrow_down.normal"), (O + SCROLL_X, O + WIN_H - 14))
    base.alpha_composite(sp("button_back.normal"), (O + DET_X + 4, O + PANE_TOP + 4))
    base.alpha_composite(sp("page_prev.normal"), (O + W - 33, O + PANE_TOP + 4))
    base.alpha_composite(sp("page_next.normal"), (O + W - 19, O + PANE_TOP + 4))
    base.alpha_composite(sp("icon_frame.selected"), (O + DET_X + 4, O + PANE_TOP + 20))
    base.alpha_composite(sp("objective.mine_block").resize((16, 16)), (O + DET_X + 5, O + PANE_TOP + 21))
    base.alpha_composite(sp("objective_row.incomplete"), (O + DET_X + 4, O + PANE_TOP + 44))
    base.alpha_composite(progress_bar("fill").crop((0, 0, 60, 6)), (O + DET_X + 42, O + PANE_TOP + 53))
    base.alpha_composite(sp("difficulty_pips.hard"), (O + DET_X + 4, O + PANE_TOP + 64))
    base.alpha_composite(sp("reward_slot.normal"), (O + DET_X + 4, O + H - 52))
    base.alpha_composite(sp("reward_slot.choice"), (O + DET_X + 26, O + H - 52))
    base.alpha_composite(sp("button_claim.normal"), (O + DET_X + 50, O + H - 28))
    big = scale(base, 3)
    d = ImageDraw.Draw(big); f = font(15); fs = font(11)
    d.text(((O + 8) * 3, (O + 4) * 3), "Quests", font=f, fill=(255, 255, 255))
    titles = ["Diamond Hunt", "Mine Stone", "Gather Wood", "??? Locked", "Cook Food"]
    for i, ti in enumerate(titles):
        d.text(((O + LIST_X + 5) * 3, (O + LIST_Y + 2 + i * 19 + 5) * 3), ti, font=fs, fill=TEXTCOL)
    d.text(((O + DET_X + 22) * 3, (O + PANE_TOP + 6) * 3), "Diamond Hunt", font=f, fill=TEXTCOL)
    d.text(((O + DET_X + 24) * 3, (O + PANE_TOP + 46) * 3), "Mine 6/10", font=fs, fill=TEXTCOL)
    d.text(((O + DET_X + 56) * 3, (O + H - 24) * 3), "Claim", font=f, fill=(40, 40, 40))
    big.save(os.path.join(DIRS["mockup"], "mockup_window.png"))


def sheet(items, cols, title, cell=22, label=True, fname="sheet.png"):
    pad = 8
    rows = (len(items) + cols - 1) // cols
    W = pad * 2 + cols * cell
    H = pad * 2 + rows * (cell + (14 if label else 2)) + 16
    sh_img = Image.new("RGBA", (W, H), (228, 228, 228, 255))
    for idx, (nm, im) in enumerate(items):
        r, c = divmod(idx, cols)
        x = pad + c * cell; y = pad + 16 + r * (cell + (14 if label else 2))
        im2 = im if max(im.size) <= cell else im.crop((0, 0, min(im.width, cell), min(im.height, cell)))
        sh_img.alpha_composite(im2, (x + (cell - im2.width) // 2, y + (cell - im2.height) // 2))
    big = scale(sh_img, 3)
    d = ImageDraw.Draw(big); f = font(14); fs = font(9)
    d.text((pad * 3, pad), title, font=f, fill=(20, 20, 20))
    if label:
        for idx, (nm, im) in enumerate(items):
            r, c = divmod(idx, cols)
            x = pad + c * cell; y = pad + 16 + r * (cell + 14) + cell
            d.text((x * 3, y * 3), nm.split(".")[-1][:12], font=fs, fill=(90, 90, 90))
    big.save(os.path.join(DIRS["mockup"], fname))


def mockup_icons():
    items = []
    for name in OBJECTIVE:
        items.append((name, sp(f"objective.{name}")))
    items += [(f"{n}.done", sp(f"objective.{n}__done")) for n in list(OBJECTIVE)[:6]]
    items += [(n, sp(f"reward.{n}")) for n in REWARD]
    items += [(n, sp(f"category.{n}")) for n in CATEGORY]
    items += [(g, sp(f"glyph.{g}")) for g in GLYPHS]
    sheet(items, 10, "Icons - objective / reward / category / glyphs", cell=18,
          fname="mockup_icons.png")


def mockup_states():
    els = ["tab", "quest_row", "progress_bar", "reward_slot", "scroll_handle",
           "scroll_arrow_up", "button_claim", "button_back", "button_settings",
           "page_prev", "checkbox", "radio", "difficulty_pips",
           "objective_row", "filter_dropdown"]
    pad, lblw, cellw, cap, gap = 8, 130, 84, 40, 10
    rows = []
    for el in els:
        sts = [r for r in REG if r["kind"] == "interactive" and r["element"] == el]
        rows.append((el, sts))
    row_h = [max([16] + [min(cap, r["img"].height) for r in sts]) + gap for el, sts in rows]
    W = lblw + max(len(s) for _, s in rows) * cellw + pad * 2
    H = pad * 2 + sum(row_h)
    img = Image.new("RGBA", (W, H), (228, 228, 228, 255))
    y = pad
    for (el, sts), rh in zip(rows, row_h):
        x = lblw
        for r in sts:
            im = r["img"]
            if im.height > cap:
                im = im.crop((0, 0, im.width, cap))
            img.alpha_composite(im, (x + 2, y + (rh - gap - im.height) // 2 + gap // 2))
            x += cellw
        y += rh
    big = scale(img, 3); d = ImageDraw.Draw(big); f = font(12); fs = font(9)
    y = pad
    for (el, sts), rh in zip(rows, row_h):
        d.text((pad * 3, (y + rh // 2) * 3), el, font=f, fill=(30, 30, 30))
        x = lblw
        for r in sts:
            d.text((x * 3 + 4, y * 3 + 2), r["state"][:11], font=fs, fill=(110, 110, 110))
            x += cellw
        y += rh
    big.save(os.path.join(DIRS["mockup"], "mockup_states.png"))


def mockup_overlays():
    base = Image.new("RGBA", (360, 200), BACKDROP)
    base.alpha_composite(sp("dialog"), (10, 10))
    base.alpha_composite(sp("dialog_button_yes.normal"), (24, 60))
    base.alpha_composite(sp("dialog_button_no.normal"), (90, 60))
    base.alpha_composite(sp("toast"), (190, 14))
    base.alpha_composite(sp("glyph.check").resize((16, 16)), (196, 20))
    base.alpha_composite(nine_slice(sp("tooltip_vanilla"), 60, 36), (190, 62))
    base.alpha_composite(nine_slice(sp("tooltip_grey"), 60, 36), (260, 62))
    base.alpha_composite(sp("empty_state"), (40, 110))
    base.alpha_composite(sp("hud_panel"), (200, 110))
    base.alpha_composite(sp("hud_row"), (205, 140))
    base.alpha_composite(sp("loading_spinner").crop((0, 0, 16, 16)), (150, 120))
    big = scale(base, 3); d = ImageDraw.Draw(big); f = font(13); fs = font(11)
    d.text((24 * 3, 24 * 3), "Abandon quest?", font=fs, fill=(40, 40, 40))
    d.text((30 * 3, 63 * 3), "Yes", font=fs, fill=(40, 40, 40))
    d.text((98 * 3, 63 * 3), "No", font=fs, fill=(40, 40, 40))
    d.text((206 * 3, 18 * 3), "Quest Complete!", font=fs, fill=(60, 40, 16))
    d.text((205 * 3, 116 * 3), "Mine Stone 5/16", font=fs, fill=(230, 230, 230))
    big.save(os.path.join(DIRS["mockup"], "mockup_overlays.png"))


mockup_window()
mockup_icons()
mockup_states()
mockup_overlays()


# ==========================================================================
# Report
# ==========================================================================
print("=" * 64)
print("JustQuests GUI v2 FULL")
print("=" * 64)
kinds = {}
for r in REG:
    kinds[r["kind"]] = kinds.get(r["kind"], 0) + 1
for k, v in kinds.items():
    print(f"  {k:12s}: {v}")
print(f"  TOTAL sprites: {len(REG)}")
print(f"  Atlases      : {len(ATLASES)}  ({', '.join(ATLASES)})")
print(f"  Themes       : {', '.join(THEMES)}   + 2x HD set")
print("-" * 64)
print("VERIFICATION:", "OK (bounds, no overlaps, palette-only)" if not issues else "PROBLEMS:")
for p in issues[:20]:
    print("  !", p)
print("=" * 64)
