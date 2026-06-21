#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JustQuests GUI texture set - v2 (pure vanilla grey, layered).

Follows gui-prompt-v2.md:
  * Looks exactly like a vanilla container GUI (chest / inventory).
  * True 1x pixel art, nearest-neighbour, NO anti-aliasing, NO gradients,
    NO rounded corners, NO baked text.
  * ONLY the vanilla GUI grey palette (Section 2). Every opaque pixel is one
    of six greys; verified at the end.
  * Top-left light source on every bevel.

Delivery (layers as a named folder tree, per the chosen option):
  background/<piece>.png                  static window pieces
  background/background_layer.png         the whole static layer, one image
  interactive/<element>/<state>.png       each clickable control + state
  interactive/interactive_layer.png       all controls composited in place
  strips/<element>.png                    states stacked vertically (sheet)
  atlas/quest_gui_v2.png (+overflow)      packed atlas for classic blit()
  TEXTURE_MAP.md                          name | atlas | u | v | w | h | notes
  preview/*_3x.png , mockup/*.png         review only (not shipped)
"""

import os
from PIL import Image, ImageDraw, ImageFont

# --------------------------------------------------------------------------
OUT         = r"C:\Users\mukse\Desktop\JustQuests-GUI-v2"
ATLAS_DIR   = os.path.join(OUT, "atlas")
PREVIEW_DIR = os.path.join(OUT, "preview")
MOCKUP_DIR  = os.path.join(OUT, "mockup")
BG_DIR      = os.path.join(OUT, "background")
INT_DIR     = os.path.join(OUT, "interactive")
STRIP_DIR   = os.path.join(OUT, "strips")
for d in (OUT, ATLAS_DIR, PREVIEW_DIR, MOCKUP_DIR, BG_DIR, INT_DIR, STRIP_DIR):
    os.makedirs(d, exist_ok=True)
# wipe previously generated PNGs so stale files never linger
for root, _dirs, files in os.walk(OUT):
    for fn in files:
        if fn.lower().endswith(".png"):
            os.remove(os.path.join(root, fn))

GUTTER, ATLAS = 2, 256

# --------------------------------------------------------------------------
# Palette - vanilla GUI greys ONLY (Section 2)
# --------------------------------------------------------------------------
BLACK  = (0, 0, 0)         # outer outline
FACE   = (198, 198, 198)   # #C6C6C6 panel face
WHITE  = (255, 255, 255)   # highlight bevel (top/left)
SHADOW = (85, 85, 85)      # #555555 shadow bevel (bottom/right)
INSET  = (139, 139, 139)   # #8B8B8B slot / inset face
DARK   = (55, 55, 55)      # #373737 inset shadow / separator / glyphs
TEXTCOL = (64, 64, 64)     # #404040 - reference only, mockup labels

ALLOWED_RGB = {BLACK, FACE, WHITE, SHADOW, INSET, DARK}


def tile(w, h):
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def A(c):                       # opaque
    return (c[0], c[1], c[2], 255)


def bevel(d, x, y, w, h, hi, lo):
    """1px bevel inside the rect (x,y,w,h). hi = top/left, lo = bottom/right."""
    d.line([(x + 1, y + 1), (x + w - 2, y + 1)], fill=A(hi))
    d.line([(x + 1, y + 1), (x + 1, y + h - 2)], fill=A(hi))
    d.line([(x + 1, y + h - 2), (x + w - 2, y + h - 2)], fill=A(lo))
    d.line([(x + w - 2, y + 1), (x + w - 2, y + h - 2)], fill=A(lo))


def slot(w, h, outline=None):
    """Vanilla inset slot: dark top/left, white bottom/right, INSET face."""
    img, d = tile(w, h)
    d.rectangle([0, 0, w - 1, h - 1], fill=A(INSET))
    d.line([(0, 0), (w - 1, 0)], fill=A(DARK))
    d.line([(0, 0), (0, h - 1)], fill=A(DARK))
    d.line([(0, h - 1), (w - 1, h - 1)], fill=A(WHITE))
    d.line([(w - 1, 0), (w - 1, h - 1)], fill=A(WHITE))
    if outline:
        d.rectangle([0, 0, w - 1, h - 1], outline=A(outline))
    return img, d


# --------------------------------------------------------------------------
# Core control face (vanilla button look) shared by every button-like control
# --------------------------------------------------------------------------
def face_box(w, h, state, face=FACE):
    img, d = tile(w, h)
    if state == "disabled":
        d.rectangle([0, 0, w - 1, h - 1], fill=A(INSET), outline=A(BLACK))
        return img, d
    d.rectangle([0, 0, w - 1, h - 1], fill=A(face), outline=A(BLACK))
    if state == "pressed":
        bevel(d, 0, 0, w, h, SHADOW, WHITE)             # inverted (pushed in)
    else:
        bevel(d, 0, 0, w, h, WHITE, SHADOW)             # raised
        if state == "hover":                            # white outline + lift
            d.rectangle([0, 0, w - 1, h - 1], outline=A(WHITE))
            d.line([(1, 1), (w - 2, 1)], fill=A(WHITE))
    return img, d


def glyph_col(state):
    return SHADOW if state == "disabled" else DARK


def shift(state):
    return 1 if state == "pressed" else 0               # pushed-in glyph nudge


# ---- glyph painters (monochrome, DARK/SHADOW only) ----
def paint_check(d, ox, oy, col):
    d.line([(ox + 3, oy + 6), (ox + 7, oy + 2)], fill=A(col), width=2)
    d.line([(ox + 1, oy + 4), (ox + 3, oy + 6)], fill=A(col), width=2)


def paint_lock(d, ox, oy, col):
    d.arc([ox + 2, oy + 0, ox + 6, oy + 6], 180, 360, fill=A(SHADOW))
    d.arc([ox + 2, oy + 1, ox + 6, oy + 6], 180, 360, fill=A(SHADOW))
    d.rectangle([ox + 1, oy + 4, ox + 7, oy + 8], fill=A(col))
    d.point((ox + 4, oy + 6), fill=A(SHADOW))           # keyhole hint


def paint_repeat(d, ox, oy, col):
    d.line([(ox + 1, oy + 2), (ox + 6, oy + 2)], fill=A(col))
    d.polygon([(ox + 5, oy + 0), (ox + 5, oy + 4), (ox + 8, oy + 2)], fill=A(col))
    d.line([(ox + 2, oy + 6), (ox + 7, oy + 6)], fill=A(col))
    d.polygon([(ox + 3, oy + 4), (ox + 3, oy + 8), (ox + 0, oy + 6)], fill=A(col))


def paint_tri(d, cx, cy, direction, col, r=2):
    if direction == "up":
        d.polygon([(cx - r, cy + r), (cx + r, cy + r), (cx, cy - r)], fill=A(col))
    elif direction == "down":
        d.polygon([(cx - r, cy - r), (cx + r, cy - r), (cx, cy + r)], fill=A(col))
    elif direction == "left":
        d.polygon([(cx + r, cy - r), (cx + r, cy + r), (cx - r, cy)], fill=A(col))
    else:  # right
        d.polygon([(cx - r, cy - r), (cx - r, cy + r), (cx + r, cy)], fill=A(col))


# ==========================================================================
# BACKGROUND pieces  -  window geometry
# ==========================================================================
WIN_W, WIN_H = 248, 184
PANE_TOP = 20            # content begins below the title bar
TAB_BAND = 22           # height of the category-tab strip atop the left pane
LIST_X, LIST_W = 5, 84
LIST_Y = PANE_TOP + TAB_BAND
LIST_H = (WIN_H - 6) - LIST_Y
SCROLL_X = LIST_X + LIST_W + 1          # 90  (8px scrollbar channel)
DIV_X = SCROLL_X + 8 + 1                # 99  divider groove
DET_X = DIV_X + 3                       # 102 detail pane
DET_W = WIN_W - DET_X - 5
DET_H = (WIN_H - 6) - PANE_TOP


def bg_window():
    """248x184 vanilla two-pane panel, 9-slice friendly (flat repeatable
    centre, 3px caps). Left = tab strip + recessed list; right = recessed
    detail pane; an 8px scrollbar channel sits between list and divider."""
    img, d = tile(WIN_W, WIN_H)
    d.rectangle([0, 0, WIN_W - 1, WIN_H - 1], fill=A(FACE), outline=A(BLACK))
    bevel(d, 0, 0, WIN_W, WIN_H, WHITE, SHADOW)
    # title bar separator
    d.line([(4, PANE_TOP - 3), (WIN_W - 5, PANE_TOP - 3)], fill=A(DARK))
    d.line([(4, PANE_TOP - 2), (WIN_W - 5, PANE_TOP - 2)], fill=A(WHITE))
    # left list pane inset (below the tab band)
    li, _ = slot(LIST_W, LIST_H)
    img.alpha_composite(li, (LIST_X, LIST_Y))
    # detail pane inset
    de, _ = slot(DET_W, DET_H)
    img.alpha_composite(de, (DET_X, PANE_TOP))
    # divider groove between panes
    d.line([(DIV_X, PANE_TOP), (DIV_X, WIN_H - 7)], fill=A(DARK))
    d.line([(DIV_X + 1, PANE_TOP), (DIV_X + 1, WIN_H - 7)], fill=A(WHITE))
    # faint separator under the tab band
    d.line([(LIST_X, LIST_Y - 2), (LIST_X + LIST_W - 1, LIST_Y - 2)], fill=A(DARK))
    return img


def bg_titlebar():
    img, d = tile(240, 16)
    d.rectangle([0, 0, 239, 15], fill=A(FACE))
    d.line([(0, 15), (239, 15)], fill=A(DARK))
    d.line([(0, 14), (239, 14)], fill=A(SHADOW))
    return img


def bg_list_pane():
    img, _ = slot(LIST_W, LIST_H)
    return img


def bg_detail_pane():
    img, _ = slot(DET_W, DET_H)
    return img


# ==========================================================================
# INTERACTIVE controls   fn(state) -> Image
# ==========================================================================
def el_tab(state):
    w, h = 20, 20
    if state == "disabled":
        img, d = tile(w, h)
        d.rectangle([0, 0, w - 1, h - 1], fill=A(INSET), outline=A(BLACK))
        return img
    if state == "selected":                      # pulled forward, bottom merged
        img, d = tile(w, h)
        d.rectangle([0, 0, w - 1, h - 1], fill=A(FACE))
        d.line([(0, 0), (w - 1, 0)], fill=A(BLACK))      # top/left/right outline
        d.line([(0, 0), (0, h - 1)], fill=A(BLACK))
        d.line([(w - 1, 0), (w - 1, h - 1)], fill=A(BLACK))
        d.line([(1, 1), (w - 2, 1)], fill=A(WHITE))
        d.line([(1, 1), (1, h - 2)], fill=A(WHITE))
        d.line([(w - 2, 1), (w - 2, h - 1)], fill=A(SHADOW))
        return img                               # no bottom edge => merged
    img, _ = face_box(w, h, state)               # normal / hover / pressed
    return img                                    # inner 16x16 stays flat (icon)


def el_quest_row(state):
    w, h = 80, 18                                    # fits the 84px list pane
    img, d = tile(w, h)
    if state in ("hover", "selected", "claimable"):
        if state != "claimable":
            d.rectangle([0, 0, w - 1, h - 2], fill=A(FACE))
        d.rectangle([0, 0, w - 1, h - 2], outline=A(WHITE))      # white outline
        if state == "selected":
            d.line([(1, 1), (w - 2, 1)], fill=A(WHITE))
        if state != "selected":
            d.line([(0, h - 1), (w - 1, h - 1)], fill=A(DARK))
        return img
    # status variants
    d.line([(0, h - 1), (w - 1, h - 1)], fill=A(DARK))           # row separator
    if state == "active":
        d.rectangle([0, 0, 2, h - 2], fill=A(WHITE))             # active marker
    elif state == "completed":
        sx = w - 15                                              # right check box
        sl, _ = slot(13, 13)
        img.alpha_composite(sl, (sx, 3))
        paint_check(d, sx + 2, 3, DARK)
    elif state == "locked":
        d.rectangle([0, 0, w - 1, h - 2], fill=A(INSET))         # greyed wash
        d.line([(0, h - 1), (w - 1, h - 1)], fill=A(DARK))
    # 'available' = just the separator
    return img


def el_scroll_track(state):
    w, h = 8, 120
    img, d = tile(w, h)
    d.rectangle([0, 0, w - 1, h - 1], fill=A(INSET), outline=A(BLACK))
    d.line([(1, 1), (w - 2, 1)], fill=A(DARK))
    d.line([(1, 1), (1, h - 2)], fill=A(DARK))
    return img


def el_scroll_handle(state):
    st = "pressed" if state == "grabbed" else state
    img, d = face_box(8, 16, st)
    g = glyph_col("normal")
    for y in (6, 8, 10):
        d.line([(2, y), (5, y)], fill=A(g))
    return img


def _arrow_btn(size, direction, state):
    img, d = face_box(size, size, state)
    c = size // 2 + shift(state)
    paint_tri(d, c, c, direction, glyph_col(state), r=2)
    return img


def el_scroll_arrow_up(state):   return _arrow_btn(8, "up", state)
def el_scroll_arrow_down(state): return _arrow_btn(8, "down", state)
def el_page_prev(state):         return _arrow_btn(12, "left", state)
def el_page_next(state):         return _arrow_btn(12, "right", state)


def el_button_back(state):
    img, d = face_box(12, 12, state)
    o = shift(state)
    col = glyph_col(state)
    paint_tri(d, 4 + o, 6 + o, "left", col, r=2)
    d.line([(4 + o, 6 + o), (9 + o, 6 + o)], fill=A(col))        # tail => "back"
    return img


def el_button_close(state):
    img, d = face_box(11, 11, state)
    o = shift(state)
    col = glyph_col(state)
    d.line([(3 + o, 3 + o), (7 + o, 7 + o)], fill=A(col))
    d.line([(7 + o, 3 + o), (3 + o, 7 + o)], fill=A(col))
    return img


def el_button_claim(state):
    img, d = face_box(72, 20, state)
    return img                                   # game draws "Claim" text


def el_icon_frame(state):
    if state == "selected":
        img, _ = slot(18, 18)
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, 17, 17], outline=A(WHITE))
        return img
    img, _ = slot(18, 18)
    return img


def el_progress_bar(state):
    w, h = 100, 6
    img, d = tile(w, h)
    if state == "fill":                          # light-grey fill, NOT a colour
        d.rectangle([0, 0, w - 1, h - 1], fill=A(WHITE))
        d.line([(0, h - 1), (w - 1, h - 1)], fill=A(SHADOW))
        d.line([(0, 0), (w - 1, 0)], fill=A(WHITE))
        return img
    # track (inset groove)
    d.rectangle([0, 0, w - 1, h - 1], fill=A(INSET), outline=A(BLACK))
    d.line([(1, 1), (w - 2, 1)], fill=A(DARK))
    return img


def el_glyph_check(state):
    img, d = tile(9, 9); paint_check(d, 0, 0, DARK); return img


def el_glyph_lock(state):
    img, d = tile(9, 9); paint_lock(d, 0, 0, DARK); return img


def el_glyph_repeat(state):
    img, d = tile(9, 9); paint_repeat(d, 0, 0, DARK); return img


# --------------------------------------------------------------------------
# Registry: element -> (states, draw fn)
# --------------------------------------------------------------------------
BUTTON_STATES = ["normal", "hover", "pressed", "disabled"]
INTERACTIVE = [
    ("tab",            ["normal", "hover", "pressed", "disabled", "selected"], el_tab),
    ("quest_row",      ["available", "active", "completed", "locked",
                        "claimable", "hover", "selected"], el_quest_row),
    ("scroll_track",   ["normal"], el_scroll_track),
    ("scroll_handle",  ["normal", "hover", "grabbed"], el_scroll_handle),
    ("scroll_arrow_up",   BUTTON_STATES, el_scroll_arrow_up),
    ("scroll_arrow_down", BUTTON_STATES, el_scroll_arrow_down),
    ("button_claim",   BUTTON_STATES, el_button_claim),
    ("button_close",   BUTTON_STATES, el_button_close),
    ("button_back",    BUTTON_STATES, el_button_back),
    ("page_prev",      BUTTON_STATES, el_page_prev),
    ("page_next",      BUTTON_STATES, el_page_next),
    ("progress_bar",   ["track", "fill"], el_progress_bar),
    ("icon_frame",     ["normal", "selected"], el_icon_frame),
    ("glyph_check",    ["glyph"], el_glyph_check),
    ("glyph_lock",     ["glyph"], el_glyph_lock),
    ("glyph_repeat",   ["glyph"], el_glyph_repeat),
]
BACKGROUND = [
    ("window",      bg_window(),      "main 248x184 panel, 9-slice friendly"),
    ("titlebar",    bg_titlebar(),    "title strip (game draws 'Quests')"),
    ("list_pane",   bg_list_pane(),   "left list inset"),
    ("detail_pane", bg_detail_pane(), "right detail inset"),
]


# ==========================================================================
# Shelf packer (paste tiles into 256x256 atlases, record coords)
# ==========================================================================
class Packer:
    def __init__(self, base):
        self.base = base; self.atlases = []; self.records = []
        self._new()

    def _new(self):
        i = len(self.atlases)
        name = self.base if i == 0 else f"{self.base}_{i + 1}"
        img = Image.new("RGBA", (ATLAS, ATLAS), (0, 0, 0, 0))
        self.atlases.append((name, img))
        self.cur, self.img = name, img
        self.x = self.y = GUTTER; self.sh = 0

    def place(self, name, t, section, notes=""):
        w, h = t.size
        if w > ATLAS - 2 * GUTTER:                      # full-width row
            if self.x > GUTTER or self.sh > 0:
                self.y += self.sh + GUTTER
            self.img.paste(t, (0, self.y))
            self.records.append((name, self.cur, 0, self.y, w, h, section, notes))
            self.y += h + GUTTER; self.x = GUTTER; self.sh = 0
            return
        if self.x + w + GUTTER > ATLAS:
            self.y += self.sh + GUTTER; self.x = GUTTER; self.sh = 0
        if self.y + h + GUTTER > ATLAS:
            self._new()
        self.img.paste(t, (self.x, self.y))
        self.records.append((name, self.cur, self.x, self.y, w, h, section, notes))
        self.x += w + GUTTER; self.sh = max(self.sh, h)


# --------------------------------------------------------------------------
# Build: write per-element PNGs, strips, then pack the atlas
# --------------------------------------------------------------------------
SPRITES = {}     # full name -> Image (for atlas + mockups)
packer = Packer("quest_gui_v2")

# background
packer.place("window", BACKGROUND[0][1], "1. Background", BACKGROUND[0][2])
for nm, img, note in BACKGROUND:
    img.save(os.path.join(BG_DIR, f"{nm}.png"))
    SPRITES[nm] = img
    if nm != "window":
        packer.place(nm, img, "1. Background", note)

# interactive: individual PNGs + vertical strips + atlas entries
for elem, states, fn in INTERACTIVE:
    folder = os.path.join(INT_DIR, elem)
    os.makedirs(folder, exist_ok=True)
    frames = []
    for st in states:
        img = fn(st)
        full = elem if st == "glyph" else f"{elem}.{st}"
        SPRITES[full] = img
        img.save(os.path.join(folder, f"{st}.png"))
        packer.place(full, img, f"2. Interactive / {elem}",
                     f"state: {st}")
        frames.append(img)
    # vertical state strip (cells = max frame size, stacked in 'states' order)
    if len(frames) > 1:
        cw = max(f.width for f in frames)
        ch = max(f.height for f in frames)
        strip = Image.new("RGBA", (cw, ch * len(frames)), (0, 0, 0, 0))
        for i, f in enumerate(frames):
            strip.alpha_composite(f, (0, i * ch))
        strip.save(os.path.join(STRIP_DIR, f"{elem}.png"))

ATLASES = dict(packer.atlases)
for name, img in ATLASES.items():
    img.save(os.path.join(ATLAS_DIR, f"{name}.png"))
    img.resize((ATLAS * 3, ATLAS * 3), Image.NEAREST).save(
        os.path.join(PREVIEW_DIR, f"{name}_3x.png"))
MAP = packer.records


# ==========================================================================
# Two composite "layers"
# ==========================================================================
def s(name):
    return SPRITES[name]


def build_layers():
    W, H = WIN_W, WIN_H
    # background layer = the window image itself
    s("window").save(os.path.join(BG_DIR, "background_layer.png"))
    # interactive layer = controls placed where they live, transparent elsewhere
    lay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    lay.alpha_composite(s("button_close.normal"), (W - 16, 4))
    # category tabs: horizontal strip atop the left pane (icon area left blank)
    for i in range(4):
        st = "selected" if i == 0 else "normal"
        lay.alpha_composite(s(f"tab.{st}"), (LIST_X + i * 21, PANE_TOP))
    # quest rows in the recessed list
    row_states = ["selected", "active", "completed", "locked", "available"]
    for i, rs in enumerate(row_states):
        lay.alpha_composite(s(f"quest_row.{rs}"), (LIST_X + 2, LIST_Y + 2 + i * 19))
    # scrollbar in its own channel
    lay.alpha_composite(s("scroll_arrow_up.normal"), (SCROLL_X, LIST_Y))
    lay.alpha_composite(s("scroll_track.normal"), (SCROLL_X, LIST_Y + 10))
    lay.alpha_composite(s("scroll_handle.normal"), (SCROLL_X, LIST_Y + 12))
    lay.alpha_composite(s("scroll_arrow_down.normal"), (SCROLL_X, WIN_H - 14))
    # detail pane controls
    lay.alpha_composite(s("button_back.normal"), (DET_X + 4, PANE_TOP + 4))
    lay.alpha_composite(s("page_prev.normal"), (W - 6 - 27, PANE_TOP + 4))
    lay.alpha_composite(s("page_next.normal"), (W - 6 - 13, PANE_TOP + 4))
    lay.alpha_composite(s("icon_frame.selected"), (DET_X + 4, PANE_TOP + 20))
    lay.alpha_composite(s("progress_bar.track"), (DET_X + 4, PANE_TOP + 46))
    lay.alpha_composite(s("button_claim.normal"), (DET_X + 4, H - 28))
    lay.save(os.path.join(INT_DIR, "interactive_layer.png"))
    return lay


interactive_layer = build_layers()


# ==========================================================================
# Verification
# ==========================================================================
def verify():
    problems = []
    by_atlas = {}
    for rec in MAP:
        by_atlas.setdefault(rec[1], []).append(rec)
    for atlas, recs in by_atlas.items():
        for (n, a, u, v, w, h, sec, nt) in recs:
            if u < 0 or v < 0 or u + w > ATLAS or v + h > ATLAS:
                problems.append(f"OUT OF BOUNDS: {n}")
        for i in range(len(recs)):
            for j in range(i + 1, len(recs)):
                _, _, u1, v1, w1, h1, _, _ = recs[i]
                _, _, u2, v2, w2, h2, _, _ = recs[j]
                if u1 < u2 + w2 and u1 + w1 > u2 and v1 < v2 + h2 and v1 + h1 > v2:
                    problems.append(f"OVERLAP: {recs[i][0]} vs {recs[j][0]}")
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
# TEXTURE_MAP.md
# ==========================================================================
def write_map():
    by_sec = {}
    for rec in MAP:
        by_sec.setdefault(rec[6], []).append(rec)
    L = ["# JustQuests v2 - TEXTURE_MAP",
         "",
         "Pure vanilla-grey, layered set. Coordinates are pixels within each "
         "256x256 atlas (origin top-left), for classic `blit(x,y,u,v,w,h)`. "
         "No AA, >=2px gutters, palette-only.",
         "",
         "Atlases: " + ", ".join(f"`atlas/{n}.png`" for n in sorted(ATLASES)),
         "",
         "Also delivered un-packed: `background/<piece>.png`, "
         "`interactive/<element>/<state>.png`, `strips/<element>.png`.",
         "",
         "In-mod path (reference): `assets/justquests/textures/gui/`",
         ""]

    def key(sname):
        return (0 if sname.startswith("1") else 1, sname)
    for sec in sorted(by_sec, key=key):
        L += [f"## {sec}", "",
              "| name | atlas | u | v | w | h | notes |",
              "|------|-------|---|---|---|---|-------|"]
        for (n, a, u, v, w, h, s_, nt) in by_sec[sec]:
            L.append(f"| `{n}` | {a} | {u} | {v} | {w} | {h} | {nt} |")
        L.append("")
    with open(os.path.join(OUT, "TEXTURE_MAP.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(L))


write_map()


# ==========================================================================
# Mockups (assembled from sprites; review only)
# ==========================================================================
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
    sp = s(name)
    return sp.crop((0, 0, max(1, int(sp.width * pct)), sp.height))


BACKDROP = (52, 52, 60, 255)


def mockup_window():
    W, H = WIN_W, WIN_H
    O = 4
    base = Image.new("RGBA", (W + 8, H + 8), BACKDROP)
    base.alpha_composite(s("window"), (O, O))
    base.alpha_composite(interactive_layer, (O, O))
    # progress fill at 60% over the track
    base.alpha_composite(crop_fill("progress_bar.fill", 0.6),
                         (O + DET_X + 4, O + PANE_TOP + 46))
    big = scale(base, 3)
    d = ImageDraw.Draw(big)
    f = get_font(15); fs = get_font(11)
    d.text(((O + 8) * 3, (O + 4) * 3), "Quests", font=f, fill=(255, 255, 255))
    titles = ["Diamond Hunt", "Mine Stone", "Gather Wood", "??? Locked", "Cook Food"]
    for i, ti in enumerate(titles):
        d.text(((O + LIST_X + 5) * 3, (O + LIST_Y + 2 + i * 19 + 5) * 3),
               ti, font=fs, fill=TEXTCOL)
    d.text(((O + DET_X + 22) * 3, (O + PANE_TOP + 6) * 3), "Diamond Hunt",
           font=f, fill=TEXTCOL)
    d.text(((O + DET_X + 44) * 3, (O + PANE_TOP + 45) * 3), "6/10",
           font=fs, fill=TEXTCOL)
    d.text(((O + DET_X + 26) * 3, (O + H - 26) * 3), "Claim", font=f,
           fill=(40, 40, 40))
    big.save(os.path.join(MOCKUP_DIR, "mockup_window.png"))


def mockup_states():
    pad, gap, lblw, cellw, cap = 6, 10, 110, 86, 40
    rows = list(INTERACTIVE)

    def disp(full):
        sp = s(full)
        if sp.height > cap:                       # crop tall, uniform sprites
            sp = sp.crop((0, 0, sp.width, cap))
        return sp

    # pre-compute per-row heights
    row_h = []
    for elem, states, _ in rows:
        hh = 16
        for st in states:
            full = elem if st == "glyph" else f"{elem}.{st}"
            hh = max(hh, disp(full).height)
        row_h.append(hh + gap)

    W = lblw + max(len(st) for _, st, _ in rows) * cellw + pad * 2
    Hh = pad * 2 + sum(row_h)
    sheet = Image.new("RGBA", (W, Hh), (228, 228, 228, 255))
    y = pad
    for (elem, states, _), rh in zip(rows, row_h):
        x = lblw
        for st in states:
            full = elem if st == "glyph" else f"{elem}.{st}"
            sp = disp(full)
            sheet.alpha_composite(sp, (x + 2, y + (rh - gap - sp.height) // 2 + gap // 2))
            x += cellw
        y += rh

    big = scale(sheet, 3)
    d = ImageDraw.Draw(big)
    f = get_font(13); fs = get_font(11)
    y = pad
    for (elem, states, _), rh in zip(rows, row_h):
        d.text((pad * 3, (y + rh // 2) * 3), elem, font=f, fill=(30, 30, 30))
        x = lblw
        for st in states:
            d.text((x * 3 + 4, y * 3 + 2), st, font=fs, fill=(110, 110, 110))
            x += cellw
        y += rh
    big.save(os.path.join(MOCKUP_DIR, "mockup_states.png"))


mockup_window()
mockup_states()


# ==========================================================================
# Report
# ==========================================================================
print("=" * 60)
print("JustQuests GUI v2 (pure vanilla grey, layered)")
print("=" * 60)
n_states = sum(len(st) for _, st, _ in INTERACTIVE)
print(f"Background pieces : {len(BACKGROUND)}")
print(f"Interactive elems : {len(INTERACTIVE)}  ({n_states} state sprites)")
print(f"Atlases           : {len(ATLASES)}")
for name, img in ATLASES.items():
    used = sum(1 for r in MAP if r[1] == name)
    print(f"  atlas/{name}.png  {img.size[0]}x{img.size[1]}  {used} sprites")
print(f"Total mapped      : {len(MAP)}")
print("-" * 60)
print("VERIFICATION:", "OK (bounds, no overlaps, 6-grey palette only)"
      if not issues else "PROBLEMS")
for p in issues:
    print("  !", p)
print("=" * 60)
