"""Builds the quest book textures added in 0.3.4 and copies them into every build with a quest book.

Source: the v2-full pixel set in docs/assets/gui-2.0.0/JustQuests-GUI-v2-full (vanilla greys only).
  - window.png and quest_row_*.png: the pack originals with the list column 32 px wider
    (one inner pixel column repeated, so every border stays pixel-exact)
  - button_sort_* / button_filter_*: the pack's sort and filter buttons
  - button_stats_* / button_hud_*: the pack's 14x14 button face with a chart / HUD glyph
  - glyph_*: state icons from the pack
  - cat_*: category icons; six from the pack, five drawn here in the same style

Run from the repo root:  python scripts/gui_textures.py
"""
import os
import sys

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PACK = os.path.join(ROOT, "docs", "assets", "gui-2.0.0", "JustQuests-GUI-v2-full")
GUI_REL = os.path.join("src", "main", "resources", "assets", "justquests", "textures", "gui")
SCREEN_REL = os.path.join("src", "main", "java", "com", "erikedits", "justquests", "client", "QuestScreen.java")
EXTRA = 32

BLACK, DARK, SHADOW, INSET, FACE, WHITE = (0, 0, 0), (55, 55, 55), (85, 85, 85), (139, 139, 139), (198, 198, 198), (255, 255, 255)
CMAP = {"k": BLACK, "d": DARK, "s": SHADOW, "m": INSET, "l": FACE, "w": WHITE}


def pack(*parts):
    return Image.open(os.path.join(PACK, *parts)).convert("RGBA")


def widen(im, at, extra):
    """Repeat column `at` `extra` times."""
    out = Image.new("RGBA", (im.width + extra, im.height))
    out.paste(im.crop((0, 0, at, im.height)), (0, 0))
    col = im.crop((at, 0, at + 1, im.height))
    for i in range(extra):
        out.paste(col, (at + i, 0))
    out.paste(im.crop((at, 0, im.width, im.height)), (at + extra, 0))
    return out


def pattern(rows, size=16):
    im = Image.new("RGBA", (size, size))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in CMAP:
                im.putpixel((x, y), (*CMAP[ch], 255))
    return im


def button14(state, glyph_rows):
    """The pack's 14x14 button face (glyph removed) with our own 10x10 glyph."""
    im = pack("interactive", "button_info", state + ".png")
    for y in range(2, 12):
        for x in range(2, 12):
            if im.getpixel((x, y))[:3] == DARK:
                im.putpixel((x, y), (*FACE, 255))
    off = 3 if state == "pressed" else 2   # the pressed face is shifted down-right
    for y, row in enumerate(glyph_rows):
        for x, ch in enumerate(row):
            if ch in CMAP and 0 <= x + off < 12 and 0 <= y + off < 12:
                im.putpixel((x + off, y + off), (*CMAP[ch], 255))
    return im


STATS_GLYPH = [
    "          ",
    "       dd ",
    "       dd ",
    "    dd dd ",
    "    dd dd ",
    " dd dd dd ",
    " dd dd dd ",
    " dd dd dd ",
    " dddddddd ",
    "          ",
]
HUD_GLYPH = [
    "          ",
    "dddddddddd",
    "d        d",
    "d ddd    d",
    "d d d    d",
    "d ddd    d",
    "d        d",
    "d        d",
    "dddddddddd",
    "          ",
]

NEW_CATEGORIES = {
    "building": [  # bricks
        "                ", "                ", " kkkkkkkkkkkkkk ", " kllllkwllllkwk ",
        " kmmmmkmmmmmkmk ", " kkkkkkkkkkkkkk ", " kwkllllkwllllk ", " kmkmmmmkmmmmmk ",
        " kkkkkkkkkkkkkk ", " kllllkwllllkwk ", " kmmmmkmmmmmkmk ", " kkkkkkkkkkkkkk ",
        "                ", "                ", "                ", "                "],
    "crafting": [  # hammer
        "                ", "   kkkkkkk      ", "  kwllllllk     ", "  klllllllkk    ",
        "  kmmmmmmmk     ", "   kkkkkkk      ", "      kdk       ", "      kdk       ",
        "      kdk       ", "      kdk       ", "      kdk       ", "      kdk       ",
        "      kdk       ", "      kkk       ", "                ", "                "],
    "challenges": [  # flag
        "                ", "  kk            ", "  kwkkkkkkkkk   ", "  kwklllllllk   ",
        "  kwklllllllk   ", "  kwkllllllk    ", "  kwklllllllk   ", "  kwkkkkkkkkk   ",
        "  kwk           ", "  kwk           ", "  kwk           ", "  kwk           ",
        "  kwk           ", " kkkkk          ", " kmmmk          ", " kkkkk          "],
    "generated": [  # die
        "                ", "  kkkkkkkkkkk   ", " kwwwwwwwwwwlk  ", " kwkkwwwwwkklk  ",
        " kwkkwwwwwkklk  ", " kwwwwwwwwwwlk  ", " kwwwwkkwwwwlk  ", " kwwwwkkwwwwlk  ",
        " kwwwwwwwwwwlk  ", " kwkkwwwwwkklk  ", " kwkkwwwwwkklk  ", " klllllllllllk  ",
        "  kkkkkkkkkkk   ", "                ", "                ", "                "],
}


def compass():
    im = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(im)
    d.ellipse([2, 2, 13, 13], fill=(*FACE, 255), outline=(*BLACK, 255))
    for p in ((5, 4), (4, 5), (6, 3)):
        d.point(p, fill=(*WHITE, 255))
    for p in ((8, 7), (9, 6), (10, 5)):
        d.point(p, fill=(*BLACK, 255))
    for p in ((7, 8), (6, 9), (5, 10)):
        d.point(p, fill=(*SHADOW, 255))
    d.point((7, 7), fill=(*DARK, 255))
    d.point((8, 8), fill=(*DARK, 255))
    return im


def build():
    out = {"window": widen(pack("background", "window.png"), 50, EXTRA)}
    for state in ("available", "hover", "selected", "active", "completed", "claimable", "locked"):
        out["quest_row_" + state] = widen(pack("interactive", "quest_row", state + ".png"), 40, EXTRA)
    out["button_sort_normal"] = pack("interactive", "sort_button", "normal.png")
    out["button_sort_hover"] = pack("interactive", "sort_button", "hover.png")
    out["button_filter_normal"] = pack("interactive", "filter_button", "normal.png")
    out["button_filter_hover"] = pack("interactive", "filter_button", "hover.png")
    out["button_filter_on"] = pack("interactive", "filter_button", "pressed.png")
    for name, rows in (("stats", STATS_GLYPH), ("hud", HUD_GLYPH)):
        out[f"button_{name}_normal"] = button14("normal", rows)
        out[f"button_{name}_hover"] = button14("hover", rows)
        out[f"button_{name}_on"] = button14("pressed", rows)
    for g in ("check", "lock", "clock", "repeat", "star", "exclamation"):
        out["glyph_" + g] = pack("icons", "glyph", g + ".png")
    for c in ("gathering", "farming", "combat", "survival", "daily", "custom"):
        out["cat_" + c] = pack("icons", "category", c + ".png")
    for c, rows in NEW_CATEGORIES.items():
        out["cat_" + c] = pattern(rows)
    out["cat_exploration"] = compass()
    return out


def gui_trees():
    sys.path.insert(0, os.path.join(ROOT, "scripts"))
    from sync_generator_v2 import version_trees
    for loader, v in version_trees():
        tree = os.path.join(ROOT, loader, v)
        if os.path.exists(os.path.join(tree, SCREEN_REL)):
            yield tree


def main():
    textures = build()
    trees = list(gui_trees())
    for tree in trees:
        folder = os.path.join(tree, GUI_REL)
        for name, im in textures.items():
            im.save(os.path.join(folder, name + ".png"))
    print(f"{len(textures)} textures written into {len(trees)} builds")


if __name__ == "__main__":
    main()
