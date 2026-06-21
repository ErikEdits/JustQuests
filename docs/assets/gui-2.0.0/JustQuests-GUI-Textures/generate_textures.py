#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JustQuests v0.2 GUI texture generator.

Builds every sprite from the spec (gui-resourcepack-prompt.md) as its own
RGBA tile, packs the tiles into 256x256 atlases with >=2px gutters and zero
overlaps, emits TEXTURE_MAP.md (the single source of truth for blit coords),
verifies palette/bounds/overlap, and assembles review mockups straight from
the saved atlases.

Cross-version safe: plain PNG atlases, integer coords, no .mcmeta sprite
system, no anti-aliasing, palette-only opaque pixels. Design is 1x.

NOTE on atlas count: the two full backgrounds (book 256x181, panel 248x166)
plus all shared widgets cannot fit in a single 256x256 atlas. Per the skill
doc ("add another 256x256 atlas rather than going to 512") the deliverable is
split into several 256x256 atlases; every sprite's atlas is recorded in
TEXTURE_MAP.md.
"""

import os
from PIL import Image, ImageDraw, ImageFont

# --------------------------------------------------------------------------
# Output locations
# --------------------------------------------------------------------------
OUT        = r"C:\Users\mukse\Desktop\JustQuests-GUI-Textures"
ATLAS_DIR  = os.path.join(OUT, "atlas")
PREVIEW_DIR= os.path.join(OUT, "preview")
MOCKUP_DIR = os.path.join(OUT, "mockup")
for d in (OUT, ATLAS_DIR, PREVIEW_DIR, MOCKUP_DIR):
    os.makedirs(d, exist_ok=True)
# wipe previously generated PNGs so renamed/removed atlases never linger
for d in (ATLAS_DIR, PREVIEW_DIR, MOCKUP_DIR):
    for fn in os.listdir(d):
        if fn.lower().endswith(".png"):
            os.remove(os.path.join(d, fn))

GUTTER = 2
ATLAS  = 256

# --------------------------------------------------------------------------
# Palette (Section 3 of the spec). RGB only; alpha added per draw.
# --------------------------------------------------------------------------
PAL = {
    # vanilla grays
    "outline": (43, 43, 43),    "deep": (85, 85, 85),
    "inset":   (139, 139, 139), "gray": (198, 198, 198),
    "white":   (255, 255, 255),
    # parchment
    "parch_sh": (184, 160, 106), "parch_shm": (214, 196, 146),
    "parch":    (232, 216, 160), "parch_hi":  (244, 236, 198),
    # oak / leather
    "oak_sh": (110, 74, 36),  "oak_dk": (138, 90, 43),
    "oak_md": (168, 116, 62), "oak_lt": (196, 154, 99),
    # accents + shadows
    "green": (90, 190, 80),   "green_sh": (60, 150, 55),
    "gold":  (224, 179, 58),  "gold_sh":  (176, 126, 30),
    "blue":  (91, 141, 214),  "blue_sh":  (60, 97, 150),
    "red":   (194, 75, 75),   "red_sh":   (142, 48, 48),
    "purple":(155, 107, 214), "purple_sh":(110, 71, 160),
    # documented extra: vanilla slot dark inner (spec 7.5)
    "dark37": (55, 55, 55),
}
# Documented non-palette tones that only ever appear with partial alpha:
TIP_FILL = (16, 0, 16)   # vanilla tooltip dark fill
HUD_FILL = (0, 0, 0)     # hud panel dark body

ALLOWED_RGB = set(PAL.values())   # opaque pixels must be in here


def rgb(name):
    r, g, b = PAL[name]
    return (r, g, b, 255)


def rgba(name, a):
    r, g, b = PAL[name]
    return (r, g, b, a)


# --------------------------------------------------------------------------
# Tile helpers
# --------------------------------------------------------------------------
def tile(w, h):
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img), img.load()


def raised(w, h, base, hi, lo, outline="outline"):
    """Vanilla raised bevel box filling the whole tile."""
    img, d, _ = tile(w, h)
    d.rectangle([0, 0, w - 1, h - 1], fill=rgb(base), outline=rgb(outline))
    d.line([(1, 1), (w - 2, 1)], fill=rgb(hi))          # top
    d.line([(1, 1), (1, h - 2)], fill=rgb(hi))          # left
    d.line([(1, h - 2), (w - 2, h - 2)], fill=rgb(lo))  # bottom
    d.line([(w - 2, 1), (w - 2, h - 2)], fill=rgb(lo))  # right
    return img, d


def pressed(w, h, base, hi, lo, outline="outline"):
    """Inverted bevel (shadow top-left, light bottom-right)."""
    return raised(w, h, base, lo, hi, outline)


def flat(w, h, base, outline="deep"):
    img, d, _ = tile(w, h)
    d.rectangle([0, 0, w - 1, h - 1], fill=rgb(base), outline=rgb(outline))
    return img, d


def inset(w, h, fill, dark, light, outline="outline"):
    """Recessed groove: dark top-left, light bottom-right."""
    img, d, _ = tile(w, h)
    d.rectangle([0, 0, w - 1, h - 1], fill=rgb(fill), outline=rgb(outline))
    d.line([(1, 1), (w - 2, 1)], fill=rgb(dark))
    d.line([(1, 1), (1, h - 2)], fill=rgb(dark))
    d.line([(1, h - 2), (w - 2, h - 2)], fill=rgb(light))
    d.line([(w - 2, 1), (w - 2, h - 2)], fill=rgb(light))
    return img, d


# ==========================================================================
# SHARED WIDGETS  (Section 7, 11, 12, 13, 14)
# ==========================================================================
def button(face, hi, lo, state):
    """80x20 button, 9-slice friendly (3px caps). state in normal/hover/
    disabled/pressed."""
    w, h = 80, 20
    if state == "disabled":
        img, d = flat(w, h, "inset", "deep")
        return img
    if state == "pressed":
        img, d = pressed(w, h, face, hi, lo)
        return img
    img, d = raised(w, h, face, hi, lo)
    if state == "hover":
        # 1px gold glow ring just inside the outline + brighter top
        d.rectangle([1, 1, w - 2, h - 2], outline=rgb("gold"))
        d.line([(2, 2), (w - 3, 2)], fill=rgb(hi))
    return img


def btn_set(prefix, face, lo):
    out = []
    for st in ("normal", "hover", "disabled", "pressed"):
        out.append((f"{prefix}.{st}", button(face, "white", lo, st)))
    return out


def bar_track():
    img, d = inset(100, 6, "inset", "deep", "inset")
    d.line([(1, 1), (98, 1)], fill=rgb("deep"))   # inner top shadow
    return img


def bar_fill(color, sh):
    img, d, _ = tile(100, 6)
    d.rectangle([0, 0, 99, 5], fill=rgb(color))
    d.line([(0, 5), (99, 5)], fill=rgb(sh))        # bottom shade
    d.line([(0, 0), (99, 0)], fill=rgba("white", 110))  # 50%-ish hi row
    return img


def bar_cap():
    img, d, _ = tile(2, 6)
    d.rectangle([0, 1, 1, 4], fill=rgba("white", 110))
    return img


def scroll_track():
    img, d = inset(8, 120, "inset", "deep", "inset")
    return img


def scroll_handle(state):
    if state == "disabled":
        img, d = flat(8, 16, "inset", "deep")
        return img
    base, hi, lo = "gray", "white", "deep"
    img, d = raised(8, 16, base, hi, lo)
    grip = "deep" if state == "normal" else "inset"
    for y in (6, 8, 10):
        d.line([(2, y), (5, y)], fill=rgb(grip))
    if state == "hover":
        d.line([(1, 1), (6, 1)], fill=rgb("white"))
    return img


def search_box():
    img, d = inset(100, 14, "dark37", "deep", "inset")
    return img


def search_icon():
    img, d, _ = tile(8, 8)
    d.ellipse([0, 0, 5, 5], outline=rgb("outline"))
    d.line([(5, 5), (7, 7)], fill=rgb("outline"), width=1)
    return img


def search_clear():
    img, d, _ = tile(8, 8)
    d.line([(1, 1), (6, 6)], fill=rgb("red"), width=1)
    d.line([(6, 1), (1, 6)], fill=rgb("red"), width=1)
    return img


def slot():
    """18x18 vanilla item slot: inset, dark top-left, white bottom-right."""
    img, d, _ = tile(18, 18)
    d.rectangle([0, 0, 17, 17], fill=rgb("inset"))
    d.line([(0, 0), (17, 0)], fill=rgb("dark37"))
    d.line([(0, 0), (0, 17)], fill=rgb("dark37"))
    d.line([(0, 17), (17, 17)], fill=rgb("white"))
    d.line([(17, 0), (17, 17)], fill=rgb("white"))
    return img


def icon_frame(selected=False):
    img, d, _ = tile(20, 20)
    border = "gold" if selected else "oak_dk"
    d.rectangle([0, 0, 19, 19], fill=rgb("inset"), outline=rgb(border))
    d.line([(1, 1), (18, 1)], fill=rgb("oak_lt" if not selected else "gold"))
    d.line([(1, 1), (1, 18)], fill=rgb("oak_lt" if not selected else "gold"))
    d.line([(1, 18), (18, 18)], fill=rgb("oak_sh"))
    d.line([(18, 1), (18, 18)], fill=rgb("oak_sh"))
    # inner inset for the item
    d.rectangle([3, 3, 16, 16], fill=rgb("dark37"))
    if selected:
        d.rectangle([2, 2, 17, 17], outline=rgba("gold", 120))  # glow
    return img


def tooltip_bg():
    """16x16 nine-slice vanilla-style tooltip (optional). Semi-transparent."""
    img, d, _ = tile(16, 16)
    d.rectangle([1, 1, 14, 14], fill=(*TIP_FILL, 240))
    d.rectangle([0, 0, 15, 15], outline=(*TIP_FILL, 240))
    d.rectangle([1, 1, 14, 14], outline=rgba("purple", 200))
    return img


def reward_tray():
    img, d = inset(80, 24, "parch", "parch_sh", "parch_hi", "oak_dk")
    return img


def choice_frame():
    img, d = raised(60, 40, "parch", "parch_hi", "parch_sh", "oak_dk")
    return img


def choice_option(selected=False):
    if selected:
        img, d, _ = tile(24, 24)
        d.rectangle([0, 0, 23, 23], fill=rgb("inset"), outline=rgb("gold"))
        d.line([(0, 0), (23, 0)], fill=rgb("dark37"))
        d.line([(0, 0), (0, 23)], fill=rgb("dark37"))
        d.line([(0, 23), (23, 23)], fill=rgb("white"))
        d.line([(23, 0), (23, 23)], fill=rgb("white"))
        d.rectangle([1, 1, 22, 22], outline=rgba("gold", 110))  # glow row
        return img
    img, d, _ = tile(24, 24)
    d.rectangle([0, 0, 23, 23], fill=rgb("inset"))
    d.line([(0, 0), (23, 0)], fill=rgb("dark37"))
    d.line([(0, 0), (0, 23)], fill=rgb("dark37"))
    d.line([(0, 23), (23, 23)], fill=rgb("white"))
    d.line([(23, 0), (23, 23)], fill=rgb("white"))
    return img


def hud_bg():
    """120x40 rounded dark panel, ~70% opacity, oak top edge."""
    img, d, _ = tile(120, 40)
    d.rectangle([1, 1, 118, 38], fill=(*HUD_FILL, 180))
    # rounded corners: clear the 4 corner pixels
    for cx, cy in ((1, 1), (118, 1), (1, 38), (118, 38)):
        img.putpixel((cx, cy), (0, 0, 0, 0))
    d.rectangle([0, 0, 119, 39], outline=rgb("outline"))
    for cx, cy in ((0, 0), (119, 0), (0, 39), (119, 39)):
        img.putpixel((cx, cy), (0, 0, 0, 0))
    d.line([(2, 1), (117, 1)], fill=rgba("oak_md", 200))  # subtle oak top
    return img


def hud_bar_track():
    img, d = inset(100, 4, "inset", "deep", "inset")
    return img


def hud_bar_fill():
    img, d, _ = tile(100, 4)
    d.rectangle([0, 0, 99, 3], fill=rgb("green"))
    d.line([(0, 0), (99, 0)], fill=rgba("white", 110))
    return img


def toast_bg():
    img, d = raised(160, 32, "parch", "parch_hi", "parch_sh", "oak_dk")
    d.rectangle([1, 1, 158, 30], outline=rgb("oak_md"))
    return img


def toast_accent(color):
    img, d, _ = tile(4, 32)
    d.rectangle([0, 0, 3, 31], fill=rgb(color))
    d.line([(0, 0), (0, 31)], fill=rgba("white", 90))
    return img


def empty_art():
    """48x48 friendly closed book, calm parchment/oak tones."""
    img, d, _ = tile(48, 48)
    # back cover
    d.rectangle([8, 10, 40, 40], fill=rgb("oak_dk"), outline=rgb("oak_sh"))
    d.line([(9, 11), (39, 11)], fill=rgb("oak_lt"))
    d.line([(9, 11), (9, 39)], fill=rgb("oak_lt"))
    # page block
    d.rectangle([12, 14, 38, 37], fill=rgb("parch"), outline=rgb("parch_sh"))
    for y in range(17, 36, 4):
        d.line([(15, y), (34, y)], fill=rgb("parch_shm"))
    # spine
    d.rectangle([8, 10, 12, 40], fill=rgb("oak_sh"))
    # bookmark
    d.rectangle([30, 10, 33, 22], fill=rgb("red"))
    d.polygon([(30, 22), (33, 22), (31, 25)], fill=rgb("red_sh"))
    return img


# ==========================================================================
# LAYOUT A  -  BOOK  (Section 5)
# ==========================================================================
def book_background():
    W, H = 256, 181
    img, d, _ = tile(W, H)
    # oak cover (outer bevel + near-black outline)
    d.rectangle([0, 0, W - 1, H - 1], fill=rgb("oak_md"), outline=rgb("outline"))
    d.line([(1, 1), (W - 2, 1)], fill=rgb("oak_lt"))
    d.line([(1, 1), (1, H - 2)], fill=rgb("oak_lt"))
    d.line([(1, H - 2), (W - 2, H - 2)], fill=rgb("oak_sh"))
    d.line([(W - 2, 1), (W - 2, H - 2)], fill=rgb("oak_sh"))
    d.rectangle([2, 2, W - 3, H - 3], outline=rgb("oak_dk"))
    # page area inside 6px cover
    L, T, R, B = 6, 6, W - 7, H - 7
    # parchment pages
    d.rectangle([L, T, R, B], fill=rgb("parch"))
    # inner shadow where pages meet cover
    d.rectangle([L, T, R, B], outline=rgb("parch_sh"))
    # spine / binding down the middle (~6px)
    cx = W // 2
    d.rectangle([cx - 3, T, cx + 2, B], fill=rgb("oak_dk"))
    d.line([(cx - 1, T), (cx - 1, B)], fill=rgb("outline"))     # seam
    d.line([(cx - 4, T), (cx - 4, B)], fill=rgb("parch_sh"))    # left page shadow
    d.line([(cx + 3, T), (cx + 3, B)], fill=rgb("parch_sh"))    # right page shadow
    # left page rule lines every 18px
    for y in range(T + 14, B - 4, 18):
        d.line([(L + 4, y), (cx - 6, y)], fill=rgb("parch_shm"))
    # right page: header band
    d.rectangle([cx + 5, T + 2, R - 2, T + 14], fill=rgb("oak_md"),
                outline=rgb("oak_dk"))
    d.line([(cx + 6, T + 3), (R - 3, T + 3)], fill=rgb("oak_lt"))
    # right page footer divider for the action button
    d.line([(cx + 6, B - 24), (R - 3, B - 24)], fill=rgb("parch_shm"))
    return img


def book_row(highlight=True):
    img, d, _ = tile(100, 18)
    fill = "parch_hi" if highlight else "parch_hi"
    border = "oak_md" if highlight else "parch_shm"
    d.rectangle([0, 0, 99, 17], fill=rgb(fill), outline=rgb(border))
    return img


def book_tab(active=True):
    img, d, _ = tile(24, 24)
    base = "oak_md" if active else "oak_dk"
    hi   = "oak_lt" if active else "oak_md"
    d.rectangle([0, 2, 21, 21], fill=rgb(base), outline=rgb("outline"))
    # rounded protruding right edge
    d.line([(1, 3), (20, 3)], fill=rgb(hi))
    d.line([(1, 3), (1, 20)], fill=rgb(hi))
    d.line([(1, 20), (20, 20)], fill=rgb("oak_sh"))
    img.putpixel((0, 2), (0, 0, 0, 0))
    img.putpixel((0, 21), (0, 0, 0, 0))
    if active:
        d.rectangle([20, 2, 23, 21], fill=rgb("oak_md"))  # connects to page
    return img


def book_corner():
    img, d, _ = tile(12, 12)
    d.polygon([(11, 0), (11, 11), (0, 11)], fill=rgb("parch_shm"),
              outline=rgb("parch_sh"))
    d.line([(11, 0), (0, 11)], fill=rgb("oak_md"))
    return img


# ==========================================================================
# LAYOUT B  -  PANEL  (Section 6)
# ==========================================================================
def panel_background():
    W, H = 248, 166
    img, d = raised(W, H, "gray", "white", "deep")
    # header band (oak) ~16px
    d.rectangle([2, 2, W - 3, 17], fill=rgb("oak_md"))
    d.line([(2, 2), (W - 3, 2)], fill=rgb("oak_lt"))
    d.line([(2, 17), (W - 3, 17)], fill=rgb("oak_sh"))
    # left list column ~96px wide, inset groove divider
    lx = 2 + 96
    d.line([(lx, 18), (lx, H - 3)], fill=rgb("deep"))
    d.line([(lx + 1, 18), (lx + 1, H - 3)], fill=rgb("white"))
    # left column faint inset background
    d.rectangle([3, 19, lx - 1, H - 3], fill=rgb("gray"))
    d.line([(3, 19), (lx - 1, 19)], fill=rgb("deep"))
    # footer divider in detail pane
    d.line([(lx + 4, H - 26), (W - 4, H - 26)], fill=rgb("deep"))
    return img


def panel_row(state):
    img, d, _ = tile(96, 16)
    if state == "normal":
        return img  # transparent
    if state == "hover":
        for y in range(0, 16, 2):       # 1px dither ~ light wash
            for x in range((y // 2) % 2, 96, 2):
                img.putpixel((x, y), rgba("white", 60))
        return img
    # selected: blue 1px frame
    d.rectangle([0, 0, 95, 15], outline=rgb("blue"))
    d.rectangle([1, 1, 94, 14], outline=rgba("blue", 60))
    return img


def panel_divider():
    img, d, _ = tile(2, 120)
    d.line([(0, 0), (0, 119)], fill=rgb("deep"))
    d.line([(1, 0), (1, 119)], fill=rgb("white"))
    return img


def panel_header_tab(active=True):
    img, d, _ = tile(28, 14)
    if active:
        img2, d = raised(28, 14, "oak_lt", "parch_hi", "oak_md")
        return img2
    img2, d = flat(28, 14, "oak_dk", "oak_sh")
    return img2


# ==========================================================================
# ICONS  (Section 8 categories, 9 status, 10 difficulty)
# ==========================================================================
def glyph_check():
    img, d, _ = tile(9, 9)
    d.line([(3, 7), (8, 2)], fill=rgb("green_sh"), width=2)
    d.line([(1, 5), (3, 7)], fill=rgb("green_sh"), width=2)
    d.line([(3, 6), (8, 1)], fill=rgb("green"), width=2)
    d.line([(1, 4), (3, 6)], fill=rgb("green"), width=2)
    return img


def glyph_excl():
    img, d, _ = tile(9, 9)
    d.rectangle([3, 1, 5, 5], fill=rgb("gold"))
    d.rectangle([3, 7, 5, 8], fill=rgb("gold"))
    d.line([(5, 1), (5, 5)], fill=rgb("gold_sh"))
    d.line([(5, 7), (5, 8)], fill=rgb("gold_sh"))
    return img


def glyph_active():
    img, d, _ = tile(9, 9)
    d.pieslice([1, 1, 7, 7], 0, 180, fill=rgb("blue"))     # bottom half
    d.ellipse([1, 1, 7, 7], outline=rgb("blue_sh"))
    return img


def glyph_lock():
    img, d, _ = tile(9, 9)
    d.arc([2, 0, 6, 6], 180, 360, fill=rgb("deep"))        # shackle
    d.arc([2, 1, 6, 7], 180, 360, fill=rgb("deep"))
    d.rectangle([1, 4, 7, 8], fill=rgb("inset"), outline=rgb("deep"))
    img.putpixel((4, 6), rgb("outline"))                   # keyhole
    return img


def glyph_claim():
    img, d, _ = tile(9, 9)
    d.rectangle([3, 0, 5, 8], fill=rgb("gold"))
    d.rectangle([0, 3, 8, 5], fill=rgb("gold"))
    for x, y in ((1, 1), (7, 1), (1, 7), (7, 7)):
        img.putpixel((x, y), rgb("gold_sh"))
    return img


def glyph_book():
    img, d, _ = tile(9, 9)
    d.rectangle([1, 1, 7, 7], fill=rgb("oak_md"), outline=rgb("oak_sh"))
    d.rectangle([5, 2, 7, 6], fill=rgb("parch"))
    d.line([(4, 1), (4, 7)], fill=rgb("oak_dk"))
    return img


def glyph_pencil():
    img, d, _ = tile(9, 9)
    d.line([(1, 7), (6, 2)], fill=rgb("oak_md"), width=2)
    d.line([(6, 2), (8, 0)], fill=rgb("parch"), width=2)
    img.putpixel((1, 7), rgb("outline"))
    return img


def glyph_spark():
    img, d, _ = tile(9, 9)
    d.rectangle([4, 0, 4, 8], fill=rgb("purple"))
    d.rectangle([0, 4, 8, 4], fill=rgb("purple"))
    d.line([(2, 2), (6, 6)], fill=rgb("purple_sh"))
    d.line([(6, 2), (2, 6)], fill=rgb("purple_sh"))
    return img


def glyph_shield():
    img, d, _ = tile(9, 9)
    d.polygon([(1, 1), (7, 1), (7, 4), (4, 8), (1, 4)],
              fill=rgb("blue"), outline=rgb("blue_sh"))
    return img


def badge(glyph_img):
    img, d, _ = tile(12, 12)
    d.rectangle([0, 0, 11, 11], fill=rgb("parch"), outline=rgb("oak_dk"))
    d.line([(1, 1), (10, 1)], fill=rgb("oak_lt"))
    d.line([(1, 1), (1, 10)], fill=rgb("oak_lt"))
    d.line([(1, 10), (10, 10)], fill=rgb("oak_sh"))
    d.line([(10, 1), (10, 10)], fill=rgb("oak_sh"))
    img.alpha_composite(glyph_img, (1, 1))
    return img


def diff_pips(filled, color):
    img, d, _ = tile(16, 6)
    for i in range(3):
        cx = 2 + i * 5
        if i < filled:
            d.ellipse([cx, 1, cx + 3, 4], fill=rgb(color), outline=rgb("outline"))
        else:
            d.ellipse([cx, 1, cx + 3, 4], outline=rgb("inset"))
    return img


# ==========================================================================
# Shelf packer  (paste tiles into 256x256 atlases, record coords)
# ==========================================================================
class Packer:
    def __init__(self, base):
        self.base = base
        self.atlases = []      # list of (name, Image)
        self._new_atlas()
        self.records = []      # (name, atlas, u, v, w, h, section, notes)

    def _new_atlas(self):
        idx = len(self.atlases)
        name = self.base if idx == 0 else f"{self.base}{idx + 1}"
        img = Image.new("RGBA", (ATLAS, ATLAS), (0, 0, 0, 0))
        self.atlases.append((name, img))
        self.cur = name
        self.img = img
        self.x = GUTTER
        self.y = GUTTER
        self.shelf_h = 0

    def place(self, name, t, section, notes=""):
        w, h = t.size
        full_width = w > ATLAS - 2 * GUTTER
        if full_width:
            # dedicate a full row at x=0 (atlas edge, no neighbours)
            if self.x > GUTTER or self.shelf_h > 0:
                self.y += self.shelf_h + GUTTER
            ux, uy = 0, self.y
            self.img.paste(t, (ux, uy))
            self._record(name, ux, uy, w, h, section, notes)
            self.y += h + GUTTER
            self.x = GUTTER
            self.shelf_h = 0
            return
        if self.x + w + GUTTER > ATLAS:                 # wrap to new shelf
            self.y += self.shelf_h + GUTTER
            self.x = GUTTER
            self.shelf_h = 0
        if self.y + h + GUTTER > ATLAS:                 # overflow -> new atlas
            self._new_atlas()
        self.img.paste(t, (self.x, self.y))
        self._record(name, self.x, self.y, w, h, section, notes)
        self.x += w + GUTTER
        self.shelf_h = max(self.shelf_h, h)

    def _record(self, name, u, v, w, h, section, notes):
        self.records.append((name, self.cur, u, v, w, h, section, notes))


# --------------------------------------------------------------------------
# Build all sprites, grouped by section, packed into atlases.
# --------------------------------------------------------------------------
ATLASES = {}        # name -> Image  (for mockup assembly)
MAP = []            # combined records


def commit(packer):
    for name, img in packer.atlases:
        ATLASES[name] = img
    MAP.extend(packer.records)


# ---- Backgrounds: book ----
bg_book = Packer("quest_book")
bg_book.place("book.background", book_background(), "5. Layout A - Book",
              "centered full-screen book; cover+spine+two parchment pages")
bg_book.place("book.tab.active", book_tab(True), "5. Layout A - Book",
              "side category tab, protruding")
bg_book.place("book.tab.inactive", book_tab(False), "5. Layout A - Book",
              "side category tab, recessed")
bg_book.place("book.page_highlight", book_row(True), "5. Layout A - Book",
              "selected left-page row frame")
bg_book.place("book.page_hover", book_row(False), "5. Layout A - Book",
              "hovered left-page row frame (subtler)")
bg_book.place("book.corner", book_corner(), "5. Layout A - Book",
              "decorative page-turn corner (optional)")
commit(bg_book)

# ---- Backgrounds: panel ----
bg_panel = Packer("quest_panel")
bg_panel.place("panel.background", panel_background(), "6. Layout B - Panel",
               "assembled empty panel; header band + list column + detail")
bg_panel.place("panel.header_tab.active", panel_header_tab(True),
               "6. Layout B - Panel", "header category tab, active")
bg_panel.place("panel.header_tab.inactive", panel_header_tab(False),
               "6. Layout B - Panel", "header category tab, inactive")
bg_panel.place("panel.list_row.normal", panel_row("normal"),
               "6. Layout B - Panel", "list row, transparent")
bg_panel.place("panel.list_row.hover", panel_row("hover"),
               "6. Layout B - Panel", "list row, 1px dither wash")
bg_panel.place("panel.list_row.selected", panel_row("selected"),
               "6. Layout B - Panel", "list row, blue 1px frame")
commit(bg_panel)
# panel.divider (2x120) is too tall to fit under the panel background, so it
# rides the widget atlas next to the equally-tall scrollbar track.

# ---- Shared widgets (sorted by height for tight packing) ----
shared = []
shared += btn_set("button", "gray", "deep")          # generic gray
shared += btn_set("button.accept", "green", "green_sh")
shared += btn_set("button.abandon", "red", "red_sh")
shared += btn_set("button.claim", "gold", "gold_sh")
shared += [
    ("bar.track", bar_track()),
    ("bar.fill.green", bar_fill("green", "green_sh")),
    ("bar.fill.blue", bar_fill("blue", "blue_sh")),
    ("bar.cap", bar_cap()),
    ("scroll.track", scroll_track()),
    ("panel.divider", panel_divider()),
    ("scroll.handle.normal", scroll_handle("normal")),
    ("scroll.handle.hover", scroll_handle("hover")),
    ("scroll.handle.disabled", scroll_handle("disabled")),
    ("search.box", search_box()),
    ("search.icon", search_icon()),
    ("search.clear", search_clear()),
    ("slot", slot()),
    ("icon_frame", icon_frame(False)),
    ("icon_frame.selected", icon_frame(True)),
    ("tooltip.bg", tooltip_bg()),
    ("reward.tray", reward_tray()),
    ("choice.frame", choice_frame()),
    ("choice.option.normal", choice_option(False)),
    ("choice.option.selected", choice_option(True)),
    ("hud.bg", hud_bg()),
    ("hud.bar.track", hud_bar_track()),
    ("hud.bar.fill", hud_bar_fill()),
    ("toast.bg", toast_bg()),
    ("toast.accent.complete", toast_accent("green")),
    ("toast.accent.available", toast_accent("gold")),
    ("empty.art", empty_art()),
]


def section_of(name):
    if name.startswith("panel"):   return "6. Layout B - Panel"
    if name.startswith("button"):  return "7.1 Buttons"
    if name.startswith("bar"):     return "7.2 Progress bar"
    if name.startswith("scroll"):  return "7.3 Scrollbar"
    if name.startswith("search"):  return "7.4 Search box"
    if name in ("slot",) or name.startswith("icon_frame"): return "7.5 Slot / icon frame"
    if name.startswith("tooltip"): return "7.6 Tooltip"
    if name.startswith("reward") or name.startswith("choice"): return "11. Reward & claim"
    if name.startswith("hud"):     return "12. HUD tracker"
    if name.startswith("toast"):   return "13. Toast"
    if name.startswith("empty"):   return "14. Empty state"
    return "7. Shared widgets"


gui = Packer("quest_gui")
for name, t in sorted(shared, key=lambda kv: (-kv[1].size[1], -kv[1].size[0])):
    gui.place(name, t, section_of(name))
commit(gui)

# ---- Icons ----
icons = [
    ("cat.datapack.small", glyph_book(), "8. Category icons"),
    ("cat.custom.small",   glyph_pencil(), "8. Category icons"),
    ("cat.ai.small",       glyph_spark(), "8. Category icons"),
    ("cat.team.small",     glyph_shield(), "8. Category icons"),
    ("cat.datapack.badge", badge(glyph_book()), "8. Category icons"),
    ("cat.custom.badge",   badge(glyph_pencil()), "8. Category icons"),
    ("cat.ai.badge",       badge(glyph_spark()), "8. Category icons"),
    ("cat.team.badge",     badge(glyph_shield()), "8. Category icons"),
    ("status.available", glyph_excl(),   "9. Status icons"),
    ("status.active",    glyph_active(),  "9. Status icons"),
    ("status.completed", glyph_check(),   "9. Status icons"),
    ("status.locked",    glyph_lock(),    "9. Status icons"),
    ("status.claim",     glyph_claim(),   "9. Status icons"),
    ("diff.easy",   diff_pips(1, "green"), "10. Difficulty pips"),
    ("diff.normal", diff_pips(2, "gold"),  "10. Difficulty pips"),
    ("diff.hard",   diff_pips(3, "red"),   "10. Difficulty pips"),
]
icp = Packer("quest_icons")
for name, t, sec in icons:
    icp.place(name, t, sec)
commit(icp)


# ==========================================================================
# Save atlases + previews
# ==========================================================================
def save_png(img, path):
    img.save(path, "PNG", optimize=False)


for name, img in ATLASES.items():
    save_png(img, os.path.join(ATLAS_DIR, f"{name}.png"))
    img.resize((ATLAS * 3, ATLAS * 3), Image.NEAREST).save(
        os.path.join(PREVIEW_DIR, f"{name}_3x.png"))


# ==========================================================================
# Verification
# ==========================================================================
def verify():
    problems = []
    # 1. bounds + 2. overlap per atlas
    by_atlas = {}
    for rec in MAP:
        by_atlas.setdefault(rec[1], []).append(rec)
    for atlas, recs in by_atlas.items():
        for (n, a, u, v, w, h, s, nt) in recs:
            if u < 0 or v < 0 or u + w > ATLAS or v + h > ATLAS:
                problems.append(f"OUT OF BOUNDS: {n} ({u},{v},{w},{h})")
        for i in range(len(recs)):
            for j in range(i + 1, len(recs)):
                _, _, u1, v1, w1, h1, _, _ = recs[i]
                _, _, u2, v2, w2, h2, _, _ = recs[j]
                if (u1 < u2 + w2 and u1 + w1 > u2 and
                        v1 < v2 + h2 and v1 + h1 > v2):
                    problems.append(f"OVERLAP: {recs[i][0]} vs {recs[j][0]}")
    # 3. palette: every opaque pixel in ALLOWED_RGB
    offpix = 0
    for name, img in ATLASES.items():
        px = img.load()
        W, H = img.size
        for y in range(H):
            for x in range(W):
                r, g, b, a = px[x, y]
                if a == 255 and (r, g, b) not in ALLOWED_RGB:
                    offpix += 1
    if offpix:
        problems.append(f"OFF-PALETTE opaque pixels: {offpix}")
    return problems


issues = verify()


# ==========================================================================
# TEXTURE_MAP.md
# ==========================================================================
def write_map():
    by_sec = {}
    for rec in MAP:
        by_sec.setdefault(rec[6], []).append(rec)
    lines = []
    lines.append("# JustQuests v0.2 - TEXTURE_MAP")
    lines.append("")
    lines.append("Single source of truth for `blit(x, y, u, v, w, h)`. "
                 "Coordinates are pixels within each atlas (origin top-left). "
                 "All atlases are 256x256, RGBA, no AA, >=2px gutters.")
    lines.append("")
    lines.append("Atlases:")
    for name in sorted(ATLASES):
        lines.append(f"- `atlas/{name}.png`")
    lines.append("")
    lines.append("In-mod path (reference only): "
                 "`assets/justquests/textures/gui/`")
    lines.append("")

    def sec_key(s):
        tok = s.split(" ", 1)[0].rstrip(".")
        try:
            return tuple(int(p) for p in tok.split("."))
        except ValueError:
            return (999,)

    for sec in sorted(by_sec, key=sec_key):
        lines.append(f"## {sec}")
        lines.append("")
        lines.append("| name | atlas | u | v | w | h | notes |")
        lines.append("|------|-------|---|---|---|---|-------|")
        for (n, a, u, v, w, h, s, nt) in by_sec[sec]:
            lines.append(f"| `{n}` | {a} | {u} | {v} | {w} | {h} | {nt} |")
        lines.append("")
    with open(os.path.join(OUT, "TEXTURE_MAP.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(lines))


write_map()


# ==========================================================================
# Mockups  (assembled ONLY from the saved atlases via recorded coords)
# ==========================================================================
COORD = {rec[0]: rec for rec in MAP}   # name -> record


def sprite(name):
    _, atlas, u, v, w, h, _, _ = COORD[name]
    return ATLASES[atlas].crop((u, v, u + w, v + h))


def get_font(sz):
    for fn in ("arialbd.ttf", "arial.ttf", "segoeui.ttf"):
        try:
            return ImageFont.truetype(fn, sz)
        except OSError:
            continue
    return ImageFont.load_default()


def scale(img, f):
    return img.resize((img.width * f, img.height * f), Image.NEAREST)


def crop_fill(name, pct):
    s = sprite(name)
    w = max(1, int(round(s.width * pct)))
    return s.crop((0, 0, w, s.height))


BACKDROP = (52, 52, 60, 255)   # dimmed "game screen" behind the GUI


# ---- mockup_book ----
def mockup_book():
    book = sprite("book.background").convert("RGBA")
    M = 22                                       # left margin for protruding tabs
    base = Image.new("RGBA", (book.width + M, book.height), BACKDROP)
    # side tabs protrude into the left margin
    base.alpha_composite(sprite("book.tab.active"), (M - 21, 28))
    base.alpha_composite(sprite("book.tab.inactive"), (M - 21, 54))
    base.alpha_composite(book, (M, 0))
    cx = M + book.width // 2
    L, T = M + 6, 6
    base.alpha_composite(sprite("book.page_highlight"), (L + 3, T + 30))
    rows_y = [T + 12, T + 30, T + 48, T + 66, T + 84]
    cats = ["cat.datapack.small", "cat.custom.small", "cat.ai.small",
            "cat.team.small", "cat.datapack.small"]
    stat = ["status.completed", "status.active", "status.available",
            "status.locked", "status.available"]
    for y, c, st in zip(rows_y, cats, stat):
        base.alpha_composite(sprite(c), (L + 4, y + 4))
        base.alpha_composite(sprite(st), (cx - 16, y + 4))
    rx = cx + 8
    base.alpha_composite(sprite("icon_frame"), (rx + 2, T + 20))
    base.alpha_composite(sprite("bar.track"), (rx + 2, T + 50))
    base.alpha_composite(crop_fill("bar.fill.green", 0.62), (rx + 2, T + 50))
    base.alpha_composite(sprite("diff.normal"), (rx + 2, T + 62))
    base.alpha_composite(sprite("button.accept.normal"), (rx + 2, base.height - 28))

    big = scale(base, 3)
    d = ImageDraw.Draw(big)
    f = get_font(16); fs = get_font(13)
    titles = ["Gather Wood", "Mine Stone", "Find Diamonds", "??? Locked",
              "Build a House"]
    for y, ti in zip(rows_y, titles):
        d.text(((L + 16) * 3, (y + 4) * 3), ti, font=fs, fill=(64, 64, 64))
    d.text(((cx + 8) * 3, (T + 3) * 3), "Quest Details", font=f,
           fill=(255, 255, 255))
    d.text(((rx + 30) * 3, (T + 22) * 3), "Oak Logs", font=fs, fill=(64, 64, 64))
    d.text(((cx + 26) * 3, (T + 52) * 3), "62%", font=fs, fill=(64, 64, 64))
    d.text(((cx + 22) * 3, (base.height - 24) * 3), "Accept", font=f,
           fill=(255, 255, 255))
    big.save(os.path.join(MOCKUP_DIR, "mockup_book.png"))


# ---- mockup_panel ----
def mockup_panel():
    panel = sprite("panel.background").convert("RGBA")
    base = Image.new("RGBA", (panel.width + 8, panel.height + 8), BACKDROP)
    base.alpha_composite(panel, (4, 4))
    O = 4                                        # panel offset on the backdrop
    base.alpha_composite(sprite("panel.header_tab.active"), (O + 6, O + 3))
    base.alpha_composite(sprite("panel.header_tab.inactive"), (O + 38, O + 3))
    base.alpha_composite(sprite("panel.header_tab.inactive"), (O + 70, O + 3))
    rows_y = [O + 22, O + 40, O + 58, O + 76, O + 94]
    base.alpha_composite(sprite("panel.list_row.selected"), (O + 3, O + 40))
    base.alpha_composite(sprite("panel.list_row.hover"), (O + 3, O + 58))
    cats = ["cat.datapack.small", "cat.custom.small", "cat.ai.small",
            "cat.team.small", "cat.custom.small"]
    stat = ["status.completed", "status.active", "status.available",
            "status.locked", "status.available"]
    for y, c, st in zip(rows_y, cats, stat):
        base.alpha_composite(sprite(c), (O + 6, y + 3))
        base.alpha_composite(sprite(st), (O + 82, y + 3))
    # scrollbar at the right edge of the list column (just left of divider)
    base.alpha_composite(sprite("scroll.track"), (O + 88, O + 20))
    base.alpha_composite(sprite("scroll.handle.normal"), (O + 88, O + 24))
    # detail pane
    dx = O + 106
    base.alpha_composite(sprite("icon_frame.selected"), (dx, O + 22))
    base.alpha_composite(sprite("slot"), (dx + 26, O + 24))
    base.alpha_composite(sprite("bar.track"), (dx, O + 58))
    base.alpha_composite(crop_fill("bar.fill.blue", 0.45), (dx, O + 58))
    base.alpha_composite(sprite("diff.hard"), (dx, O + 70))
    base.alpha_composite(sprite("button.claim.normal"), (dx, base.height - 24))

    big = scale(base, 3)
    d = ImageDraw.Draw(big)
    f = get_font(16); fs = get_font(13)
    d.text(((O + 10) * 3, (O + 5) * 3), "All", font=fs, fill=(255, 255, 255))
    titles = ["Gather Wood", "Mine Stone", "Diamonds", "??? Locked", "Cook Food"]
    for y, ti in zip(rows_y, titles):
        d.text(((O + 20) * 3, (y + 3) * 3), ti, font=fs, fill=(48, 48, 48))
    d.text(((dx + 26) * 3, (O + 44) * 3), "Diamond Hunt", font=f,
           fill=(48, 48, 48))
    d.text(((dx + 26) * 3, (O + 60) * 3), "45%", font=fs, fill=(48, 48, 48))
    d.text(((dx + 22) * 3, (base.height - 22) * 3), "Claim", font=f,
           fill=(255, 255, 255))
    big.save(os.path.join(MOCKUP_DIR, "mockup_panel.png"))


# ---- mockup_hud ----
def mockup_hud():
    W, H = 220, 130
    bg = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    d = ImageDraw.Draw(bg)
    for y in range(H):                     # fake sky -> ground gradient
        if y < 80:
            t = y / 80
            d.line([(0, y), (W, y)], fill=(int(120 + 60 * t), int(170 + 40 * t),
                                           int(230 - 40 * t)))
        else:
            t = (y - 80) / (H - 80)
            d.line([(0, y), (W, y)], fill=(int(90 - 30 * t), int(140 - 60 * t),
                                           int(60 - 20 * t)))
    hud = sprite("hud.bg")
    bg.alpha_composite(hud, (W - hud.width - 6, 6))
    ox, oy = W - hud.width - 6, 6
    bg.alpha_composite(sprite("status.active"), (ox + 6, oy + 5))
    bg.alpha_composite(sprite("hud.bar.track"), (ox + 8, oy + 28))
    track = sprite("hud.bar.fill")
    bg.alpha_composite(track.crop((0, 0, int(track.width * 0.31), track.height)),
                       (ox + 8, oy + 28))
    big = scale(bg, 3)
    dd = ImageDraw.Draw(big)
    f = get_font(15); fs = get_font(12)
    dd.text(((ox + 18) * 3, (oy + 4) * 3), "Mine Stone", font=f,
            fill=(255, 255, 255))
    dd.text(((ox + 8) * 3, (oy + 16) * 3), "5 / 16", font=fs,
            fill=(220, 220, 220))
    big.save(os.path.join(MOCKUP_DIR, "mockup_hud.png"))


# ---- mockup_toast ----
def mockup_toast():
    bg = Image.new("RGBA", (180, 48), (60, 60, 70, 255))
    toast = sprite("toast.bg")
    bg.alpha_composite(toast, (10, 8))
    bg.alpha_composite(sprite("toast.accent.complete"), (11, 8))
    bg.alpha_composite(sprite("status.completed"), (20, 16))
    big = scale(bg, 3)
    d = ImageDraw.Draw(big)
    f = get_font(15); fs = get_font(12)
    d.text((36 * 3, 14 * 3), "Quest Complete!", font=f, fill=(64, 48, 24))
    d.text((36 * 3, 26 * 3), "Gather Wood", font=fs, fill=(90, 70, 40))
    big.save(os.path.join(MOCKUP_DIR, "mockup_toast.png"))


# ---- mockup_states ----
def mockup_states():
    rows = [
        ("Generic", ["button.normal", "button.hover", "button.disabled",
                     "button.pressed"]),
        ("Accept",  ["button.accept.normal", "button.accept.hover",
                     "button.accept.disabled", "button.accept.pressed"]),
        ("Abandon", ["button.abandon.normal", "button.abandon.hover",
                     "button.abandon.disabled", "button.abandon.pressed"]),
        ("Claim",   ["button.claim.normal", "button.claim.hover",
                     "button.claim.disabled", "button.claim.pressed"]),
    ]
    icons_row = ["status.available", "status.active", "status.completed",
                 "status.locked", "status.claim", "cat.datapack.small",
                 "cat.custom.small", "cat.ai.small", "cat.team.small"]
    badges_row = ["cat.datapack.badge", "cat.custom.badge", "cat.ai.badge",
                  "cat.team.badge"]
    diffs = ["diff.easy", "diff.normal", "diff.hard"]

    pad, lblw, cellw, cellh = 6, 70, 84, 24
    W = lblw + 4 * cellw + pad * 2
    H = pad * 2 + len(rows) * cellh + 90
    sheet = Image.new("RGBA", (W, H), (238, 238, 238, 255))

    y = pad
    for label, names in rows:
        x = lblw
        for n in names:
            sheet.alpha_composite(sprite(n), (x, y))
            x += cellw
        y += cellh
    # icons
    yi = y + 6
    x = lblw
    for n in icons_row:
        sheet.alpha_composite(sprite(n), (x, yi))
        x += 18
    # badges
    x = lblw
    yb = yi + 18
    for n in badges_row:
        sheet.alpha_composite(sprite(n), (x, yb))
        x += 18
    # diff pips
    x = lblw + 90
    for n in diffs:
        sheet.alpha_composite(sprite(n), (x, yb + 2))
        x += 24

    big = scale(sheet, 3)
    d = ImageDraw.Draw(big)
    f = get_font(15); fs = get_font(12)
    y = pad
    for label, names in rows:
        d.text((pad * 3, (y + 6) * 3), label, font=f, fill=(40, 40, 40))
        # column captions on first row
        if label == "Generic":
            for i, cap in enumerate(["normal", "hover", "disabled", "pressed"]):
                d.text(((lblw + i * cellw) * 3, (pad - 0) * 3 - 16), cap,
                       font=fs, fill=(90, 90, 90))
        y += cellh
    d.text((pad * 3, (yi) * 3), "icons", font=fs, fill=(40, 40, 40))
    d.text((pad * 3, (yb) * 3), "badges /", font=fs, fill=(40, 40, 40))
    d.text((pad * 3, (yb + 9) * 3), "pips", font=fs, fill=(40, 40, 40))
    big.save(os.path.join(MOCKUP_DIR, "mockup_states.png"))


mockup_book()
mockup_panel()
mockup_hud()
mockup_toast()
mockup_states()


# ==========================================================================
# Report
# ==========================================================================
print("=" * 60)
print("JustQuests GUI texture generation")
print("=" * 60)
print(f"Atlases ({len(ATLASES)}):")
for name, img in ATLASES.items():
    used = sum(1 for r in MAP if r[1] == name)
    print(f"  atlas/{name}.png   {img.size[0]}x{img.size[1]}   {used} sprites")
print(f"Total sprites mapped: {len(MAP)}")
print("Mockups: mockup_book, mockup_panel, mockup_hud, mockup_toast, "
      "mockup_states")
print("-" * 60)
if issues:
    print("VERIFICATION PROBLEMS:")
    for p in issues:
        print("  !", p)
else:
    print("VERIFICATION: OK  (in-bounds, no overlaps, palette-only opaque)")
print("=" * 60)
