# JustQuests v0.2 — GUI Textures

Generated programmatically with Python + Pillow following
`gui-resourcepack-prompt.md` and the `skill-minecraft-textures.md` rules.
**Assets only** — no mod code is included or changed.

## What's in here

```
JustQuests-GUI-Textures/
├─ atlas/            ← the shipping PNGs (256×256, RGBA, no AA)
│   ├─ quest_book.png     Layout A: book background + book-specific sprites
│   ├─ quest_panel.png    Layout B: panel background + panel-specific sprites
│   ├─ quest_gui.png      shared widgets (buttons, bars, slots, hud, toast…)
│   ├─ quest_gui2.png     shared widgets (overflow)
│   └─ quest_icons.png    category / status / difficulty icons
├─ TEXTURE_MAP.md    ← coordinate map: name | atlas | u | v | w | h | notes
├─ preview/          ← 3× nearest-neighbor blow-ups of each atlas (review only)
├─ mockup/           ← assembled screens (review only, NOT shipped)
│   ├─ mockup_book.png    Layout A in use
│   ├─ mockup_panel.png   Layout B in use
│   ├─ mockup_hud.png     HUD tracker over a scene
│   ├─ mockup_toast.png   completion toast
│   └─ mockup_states.png  every button state + all icons/pips
└─ generate_textures.py   the generator (single source of truth)
```

## How to use in the mod

1. Copy **only the `atlas/*.png` files** into
   `assets/justquests/textures/gui/`.
2. Draw regions with `blit(x, y, u, v, w, h)` using the coordinates in
   `TEXTURE_MAP.md`. The map is generated from the same script, so it always
   matches the pixels.
3. `preview/` and `mockup/` are for visual review only — do **not** ship them.

## Notes / deviations from the prompt

- **Multiple atlases instead of one `quest_gui.png`.** The two full
  backgrounds (book 256×181, panel 248×166) plus all shared widgets cannot
  physically fit in a single 256×256 atlas. As the skill doc allows ("add
  another 256×256 atlas rather than going to 512"), the work is split across
  five 256×256 atlases. Every sprite's atlas is recorded in `TEXTURE_MAP.md`.
- **Cross-version safe (MC 1.14 → newest):** plain PNG atlases drawn via
  `blit`, integer coordinates, ≥2 px gutters, no 1.20.2 sprite system, no
  `.mcmeta`, no anti-aliasing. Designed at 1×.
- **Palette-only:** every opaque pixel is from the Section 3 palette (the one
  documented extra is `#373737` for the vanilla slot inner). The few soft
  highlights/shadows and the HUD/tooltip bodies use partial alpha, as the
  prompt permits.
- **No baked text:** all text is left to the mod to render.

## Regenerating / tweaking

```
python generate_textures.py
```

Edit a sprite's draw function in `generate_textures.py` and re-run; the
atlases, previews, mockups, and `TEXTURE_MAP.md` are all rebuilt and
re-verified (bounds, overlaps, palette) in one pass.
