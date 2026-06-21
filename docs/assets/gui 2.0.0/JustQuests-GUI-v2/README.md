# JustQuests — GUI Textures v2 (pure vanilla grey, layered)

Generated with Python + Pillow following `gui-prompt-v2.md`. **Assets only** —
no mod code. This is the **strict-vanilla** direction: the screen is meant to
look indistinguishable from the vanilla chest/inventory GUI.

This is a **separate** set from the parchment/oak "vanilla-plus" v1 set
(`Desktop\JustQuests-GUI-Textures`). Keep whichever the poll picks.

## Layers as a folder tree (the key deliverable)

The brief asked for a layered source (`.ase`/`.psd`). Pillow can't author
those binary formats, so the layers are delivered as a **named folder tree**
that drops straight into Aseprite/GIMP/Photoshop as layers — and is actually
easier for a dev to wire up:

```
JustQuests-GUI-v2/
├─ background/                    ← the static, non-interactive layer
│   ├─ background_layer.png         the whole window as one image
│   ├─ window.png                   main 248×184 panel
│   ├─ titlebar.png   list_pane.png   detail_pane.png
├─ interactive/                   ← every clickable control, its own sprite
│   ├─ interactive_layer.png        all controls composited in place
│   ├─ <element>/<state>.png        e.g. interactive/tab/selected.png
│   │   tab/            normal hover pressed disabled selected
│   │   quest_row/      available active completed locked claimable hover selected
│   │   scroll_track/   normal
│   │   scroll_handle/  normal hover grabbed
│   │   scroll_arrow_up/ scroll_arrow_down/   normal hover pressed disabled
│   │   button_claim/ button_close/ button_back/ page_prev/ page_next/
│   │                  normal hover pressed disabled
│   │   progress_bar/  track fill
│   │   icon_frame/    normal selected
│   │   glyph_check/ glyph_lock/ glyph_repeat/   glyph
├─ strips/                        ← each element's states stacked vertically
│   └─ <element>.png                (1 sheet per element; cell = state in order)
├─ atlas/                         ← packed atlases for classic blit()
│   ├─ quest_gui_v2.png  quest_gui_v2_2.png  quest_gui_v2_3.png
├─ TEXTURE_MAP.md                 ← name | atlas | u | v | w | h | notes
├─ preview/                       ← 3× nearest-neighbour atlas blow-ups (review)
├─ mockup/                        ← assembled screens (review only)
│   ├─ mockup_window.png   the full GUI in use
│   └─ mockup_states.png   every element × every state, labelled
└─ generate_v2.py                 ← the generator (single source of truth)
```

## Style — exactly vanilla (verified)

- **Only six greys:** `#000000` outline · `#C6C6C6` face · `#FFFFFF` highlight ·
  `#555555` shadow · `#8B8B8B` inset · `#373737` inset-shadow/separator/glyphs.
  Every opaque pixel is asserted to be one of these.
- Top-left light source, 1 px black outline + 1 px white/grey bevels, flat
  faces. **No** AA, gradients, soft shadows, glow, rounded corners, or color.
- **State changes stay in palette:** hover = white outline + lifted highlight;
  pressed = inverted bevel (pushed in); disabled = flat `#8B8B8B`, greyed
  glyph; selected = brighter face, edge merged into the panel (vanilla tab).
- Progress fill is **light grey (`#FFFFFF` over the `#8B8B8B` track), not a
  colour**. Glyphs are monochrome `#373737`/`#555555`.
- **No baked text** — the game draws "Quests", quest titles, counts, etc.

## How to wire it up

- **Easiest:** use the packed `atlas/*.png` + `TEXTURE_MAP.md` with classic
  `blit(x, y, u, v, w, h)` — version-proof across MC 1.21–1.21.5 (no 1.20.2+
  GUI-sprite/JSON system).
- **Or** blit the individual `interactive/<element>/<state>.png` files
  directly. Swap a control's state by swapping the file — the background never
  changes.
- The `strips/<element>.png` sheets let you blit by state index
  (`v = state_index × cell_height`) if you prefer one texture per control.

Tabs leave a blank 16×16 inner area; quest item icons go in `icon_frame`; both
are drawn by the game at runtime.

## Regenerate

```
python generate_v2.py
```

Rebuilds every PNG, the strips, the atlases, `TEXTURE_MAP.md`, previews and
mockups, and re-verifies bounds / overlaps / palette in one pass.
