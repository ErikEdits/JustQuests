# Skill: Making Minecraft Textures & Resource Packs (with Claude + Pillow)

> **What this is:** a reusable playbook for generating Minecraft textures
> — GUI atlases, item/block textures, icons, animated textures — as real
> PNG files, programmatically with Python + Pillow, in a way that works
> across Minecraft versions. Hand a *spec* (like
> [gui-resourcepack-prompt.md](gui-resourcepack-prompt.md)) to Claude and
> it produces the files following the rules below.
>
> **When to use:** any time you need mod/resource-pack art — a GUI, an
> item icon, a mod logo, block textures, a HUD overlay.

---

## 0. The short version (TL;DR)

1. Write a **spec**: every sprite's name, size, states, and the palette.
2. Claude writes one **Pillow generator script** per atlas/texture set.
3. The script draws on an **integer pixel grid**, saves **32-bit RGBA**
   PNGs, and emits a **coordinate map** (for GUI atlases).
4. Claude renders **scaled-up mockups** (nearest-neighbor) for review.
5. **Verify**: palette-only, no overlaps, readable at 1x/2x/3x.
6. Drop the PNGs into `assets/<modid>/textures/...` and reference them.

---

## 1. Core concepts

### Pixel art rules (always)
- **No anti-aliasing.** Hard edges. Every pixel is a deliberate block.
- **Integer grid.** No sub-pixel placement; sizes/positions are whole px.
- **Fixed palette.** Pick a small palette and use only those colors
  (plus full transparency). Off-palette colors look "off" instantly.
- **Shade with a ramp** (dark -> mid -> light): light from the top-left,
  shadow to the bottom-right, usually a 1 px bevel on edges.
- **Design at 1x.** Texture pixels = in-game pixels. The game scales the
  whole GUI by the user's GUI-scale setting — never pre-scale.

### Standard sizes
- **Items & blocks:** 16x16 (vanilla base). HD packs use 32/64/… but keep
  it a power of two and consistent.
- **GUI atlases:** power-of-two canvas, **256x256** is the safe default.
  Add another 256x256 atlas instead of going to 512 when you run out.
- **Mod logo (mods.toml `logoFile`):** square, 128 or 256.
- **Modrinth/CurseForge project icon:** 512x512 recommended.

### Where textures live
Mod assets and resource packs use the same layout:
```
assets/<namespace>/textures/
  item/<name>.png        # item textures (16x16)
  block/<name>.png       # block textures (16x16)
  gui/<name>.png         # GUI atlases
  entity/...             # entity textures
```
- In a **mod**, `<namespace>` = your mod id (e.g. `justquests`).
- In a **resource pack**, you also override vanilla under
  `assets/minecraft/textures/...`.
- A resource pack needs a `pack.mcmeta` (see Section 7); a mod's bundled
  assets do not (the mod jar is the "pack").

---

## 2. Texture categories & how each works

### 2.1 Item / block textures
- 16x16 PNG. The item also needs a **model JSON** (for items, usually
  `models/item/<name>.json` with `"parent": "item/generated"` and
  `"layer0": "<namespace>:item/<name>"`). Blocks need blockstate + model
  JSONs. (Model JSON is code/data, not part of the texture task, but the
  texture must match the expected path.)

### 2.2 GUI textures (the important, version-sensitive one)
Two eras:
- **Classic (works everywhere, 1.14 -> newest):** one PNG **atlas**; the
  mod's code draws regions with `blit(x, y, u, v, w, h)`. You bake
  everything — backgrounds, buttons, every state — into the atlas and
  document each region's `u,v,w,h`.
- **New sprite system (1.20.2+ only):** `textures/gui/sprites/` with
  per-sprite files + `.mcmeta` for 9-slice/animation. **Do NOT use this if
  you want cross-version assets** — it doesn't exist before 1.20.2.

**Rule for portable GUIs:** classic atlas + `blit`, no `.mcmeta` 9-slice.
Bake 9-slice regions into the atlas and let code slice them. The same PNG
then works from 1.14 to the latest version; only the drawing code differs
per MC version (`GuiGraphics.blit` in 1.20+, `Screen.blit`/`AbstractGui`
earlier).

### 2.3 Animated textures (.mcmeta animation)
A texture animates if you add `<name>.png.mcmeta` next to it:
```json
{ "animation": { "frametime": 4 } }
```
The PNG is a vertical strip of frames (e.g. 16x96 = six 16x16 frames).
This `.mcmeta` (animation) is fine cross-version; it's only the GUI
*sprite* system that's 1.20.2+.

### 2.4 Entity / armor (brief)
Fixed UV layouts (entity model defines the unwrap). Only do these from a
provided template; freehand rarely lines up.

---

## 3. The Pillow generation workflow

### Setup
```
python -m pip install pillow
```

### Generator script pattern
One readable script per atlas/texture set, so coordinates are
reproducible and the map is generated from the same source of truth:

```python
from PIL import Image

S, SCALE = 32, 16          # design grid, preview scale
img = Image.new("RGBA", (S, S), (0,0,0,0))
px = img.load()

PAL = {                    # the fixed palette
    "outline": (43,43,43,255),
    "base":    (198,198,198,255),
    "light":   (255,255,255,255),
    "shadow":  (85,85,85,255),
}

def rect(x0,y0,x1,y1,c):
    for y in range(y0,y1+1):
        for x in range(x0,x1+1):
            if 0<=x<S and 0<=y<S: px[x,y]=c

# ...draw...
img.save("out.png")                                   # shipped: 1x
img.resize((S*SCALE, S*SCALE), Image.NEAREST).save("out_preview.png")
```

Key points:
- Draw at **1x**; only the *preview* is scaled (NEAREST = crisp pixels).
- Save **RGBA**, no interlacing, no color profile.
- For atlases, keep a **>=2 px transparent gutter** between sprites so
  rendering never bleeds neighboring pixels.

### Coordinate map (for atlases)
Emit a `TEXTURE_MAP.md` from the script — one row per sprite:
`name | u | v | w | h | states/notes`. This is what the mod codes against;
generating it from the same script keeps it exact.

### Mockups for review
Assemble the finished sprites (from the atlas, via the documented
coordinates) into full-screen mockups, scaled up, so a human can judge the
result and confirm the map is correct.

---

## 4. Color & style (vanilla-aligned)

A safe vanilla GUI palette to start from:
- `#2B2B2B` outline · `#555555` deep shadow · `#8B8B8B` inset ·
  `#C6C6C6` base · `#FFFFFF` highlight.
Add themed accents as needed (e.g. parchment `#E8D8A0`, oak `#A8743E`,
green `#5ABE50`, gold `#E0B33A`, blue `#5B8DD6`, red `#C24B4B`).

Style guidance:
- Match vanilla's **1 px bevel** look (light top-left edge, dark
  bottom-right).
- Convey meaning by **shape**, not color alone (colorblind-safe icons).
- **Never bake text** into textures — the game renders text; leave quiet
  areas for it. Keeps everything translatable.

---

## 5. Cross-version compatibility checklist (1.14 -> newest)

- [ ] Classic atlas + `blit`, **no** 1.20.2 sprite system, **no** `.mcmeta`
      9-slice.
- [ ] Power-of-two atlas (256x256), integer coordinates, >=2 px gutters.
- [ ] No shaders / emissive / normal maps.
- [ ] No anti-aliasing; palette-only colors.
- [ ] Animations (if any) via the classic `<name>.png.mcmeta` strip.
- [ ] `pack_format` set correctly **only if** shipping a standalone
      resource pack (see Section 7).

---

## 6. Common pitfalls

- **Using the 1.20.2 sprite system** → breaks on older versions.
- **Smooth gradients / AA** → looks blurry and non-vanilla in-game.
- **Texture bleeding** → forgot the gutter; neighboring sprite pixels show
  at edges. Add >=2 px gaps.
- **Wrong `pack_format`** → pack shows a yellow "incompatible" warning;
  simple textures still load, but set it right.
- **Baking text** → can't translate; also looks bad at other GUI scales.
- **Pre-scaling** (drawing at 2x) → mismatched with the game's GUI scale.
- **Non-square / non-power-of-two atlas** → can render wrong on some
  versions/drivers.

---

## 7. `pack_format` reference (standalone resource packs)

Set in `pack.mcmeta`:
```json
{ "pack": { "description": "My pack", "pack_format": 34 } }
```
| MC version | pack_format |
|---|---|
| 1.14–1.16.1 | 4 |
| 1.16.2–1.16.5 | 6 |
| 1.17 | 7 |
| 1.18 | 8 |
| 1.19 | 9 |
| 1.20 / 1.20.1 | 15 |
| 1.20.2 | 18 |
| 1.20.4 | 22 |
| 1.21 / 1.21.1 | 34 |
(For newer versions, check the wiki; for **mod-bundled** assets the format
barely matters — a mismatch only warns for simple texture packs.)

---

## 8. Driving this with Claude (the actual workflow)

1. Write or open a **spec** (sprite list + sizes + states + palette). For
   GUIs, the JustQuests example is
   [gui-resourcepack-prompt.md](gui-resourcepack-prompt.md).
2. Tell Claude: *"Generate these as PNGs with Python/Pillow following the
   spec; also emit TEXTURE_MAP.md and scaled mockups."*
3. Review the mockups Claude shows. Ask for tweaks ("make the checkmark
   greener", "thicken the border") — Claude edits the script and
   regenerates.
4. When happy, place the PNGs at `assets/<modid>/textures/gui/...` and
   wire them into the screen code (separate coding task).

Claude can run Pillow directly, so it produces real downloadable files,
iterates on feedback, and keeps the coordinate map in sync with the art.

---

## 9. Quick recipe (copy/paste mental model)

> "16x16 for items/blocks. 256x256 atlas + blit for GUIs (no 1.20.2
> sprite system if cross-version). Fixed palette, no AA, 1x design, 2 px
> gutters. Generate with Pillow, emit a coordinate map + mockups, verify
> palette/overlap/scale, then drop into assets/<modid>/textures."
