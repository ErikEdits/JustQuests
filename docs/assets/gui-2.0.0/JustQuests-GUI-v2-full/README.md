# JustQuests — GUI Textures v2 FULL ("everything")

Generated with Python + Pillow following `gui-prompt-v2-full.md`. **Assets
only** — no mod code. Strict-vanilla direction: indistinguishable from the
vanilla chest/inventory screen. **332 PNGs, 251 sprites**, all verified
(bounds / no overlaps / palette-only).

This is the full expansion of the smaller v2 set (`JustQuests-GUI-v2`). It
covers the entire §13 master checklist of the brief.

## Folder tree (layers + delivery)

```
JustQuests-GUI-v2-full/
├─ background/        9-slice window tiles (window.tl/tr/bl/br/top/bottom/
│                     left/right/center) + assembled window/_wide/_compact,
│                     titlebar, divider_v/_h, list_pane, detail_pane
├─ interactive/<element>/<state>.png   42 controls, full state sets:
│     tab · tab_side · tab_overflow · search_field · sort_button · sort_dir ·
│     filter_button · filter_dropdown · page_prev/next · page_dots ·
│     quest_row(12 states) · group_header · row_divider · row_progress_inline ·
│     icon_frame · objective_row · progress_bar(6) · reward_slot(6) ·
│     difficulty_pips · badge(4) · button_claim/abandon/track/back/close/
│     settings/help/info · scroll_track(+_h) · scroll_handle(+_h) ·
│     scroll_arrow_up/down/left/right · checkbox · radio · slider ·
│     focus_ring · corner_grip
├─ icons/<set>/<name>.png   objective×13 (+__done/__locked) · reward×6 ·
│                           category×6 · glyph×8
├─ overlay/          dialog (+ yes/no buttons), tooltip_vanilla, tooltip_grey,
│                    toast, empty_state
├─ hud/              hud_panel, hud_row, hud_pin, hud_unpin
├─ anim/             claimable_pulse, loading_spinner, toast_slide,
│                    progress_fill_shimmer, tab_switch  (horizontal strips)
├─ strips/           per-element vertical state sheets
├─ atlas/            quest_full.png … quest_full_7.png  +  atlas_map.txt
├─ themes/           standard/  dark/  highcontrast/   (palette-swapped atlases)
├─ 2x/atlas/         HD pixel-double atlases (@2x)
├─ preview/          3× nearest blow-ups of each atlas (review)
├─ mockup/           mockup_window · mockup_icons · mockup_states · mockup_overlays
├─ TEXTURE_MAP.md    name | atlas | u | v | w | h  (grouped by section)
├─ README.md
└─ generate_v2_full.py
```

## Style (verified)

- **Vanilla grey palette only:** `#000000 #C6C6C6 #FFFFFF #555555 #8B8B8B
  #373737`. Every opaque pixel is asserted to be one of these — **except** the
  one documented vanilla exception: `tooltip_vanilla` uses the near-black
  `#100010` fill + `#5000FF`→`#28007F` purple border. A greyscale
  `tooltip_grey` is provided for the "no colours" purists.
- 1 px = 1 GUI pixel, top-left light, hard bevels, flat faces. **No** AA,
  gradients (beyond that tooltip border), soft shadows, rounded corners, or
  baked text.
- **States stay in palette:** hover = white outline + lift · pressed =
  inverted bevel · disabled = flat `#8B8B8B` · selected = brighter face merged
  into parent · focused = 1 px dashed white focus ring.
- Progress fill is **light grey, not coloured**. Glyphs/icons are monochrome
  greyscale.

## 9-slice

`background/window.*` are explicit corner/edge/centre tiles (8 px caps, 1 px
black outline + bevel) so the window renders crisp at any size. The same
9-slice approach applies to `tooltip_*`, `dialog`, `toast`, `button_*`,
`progress_bar`, `quest_row` — keep ~3 px caps and tile the centre. Assembled
samples (`window`, `window_wide`, `window_compact`, `dialog`, `toast`) are
included for reference.

## How to wire it up

- **Atlas route (recommended):** `atlas/quest_full*.png` + `atlas_map.txt`
  (or the grouped `TEXTURE_MAP.md`) with classic `blit(x,y,u,v,w,h)`.
  Version-proof across MC 1.21–1.21.5 (no 1.20.2+ GUI-sprite/JSON system).
- **File route:** blit the individual `interactive/<element>/<state>.png`
  directly; swap a control's state by swapping the file. The background never
  changes.
- **Themes:** point your texture loader at `themes/dark/` or
  `themes/highcontrast/` instead of the default atlases (same coordinates).
- **HD:** `2x/atlas/*@2x.png` — a clean pixel-double (double the map coords).

## Notes / deviations

- **Layered source as a folder tree** instead of `.ase`/`.psd` (Pillow can't
  author those). The named tree imports directly as layers and is more
  practical for code.
- **2× is a clean nearest-neighbour pixel-double**, not hand-redrawn detail —
  which is exactly the "same shapes, double pixels" the brief describes for
  flat vanilla pixel art (no sub-pixel detail to add).
- Oversized assembled samples (`window_wide`) and the animation strips live as
  standalone files, not in the packed atlas (they exceed 256 px); they're
  listed in `TEXTURE_MAP.md` under "Standalone".

## Regenerate

```
python generate_v2_full.py
```

Rebuilds every PNG, strips, 7 atlases, 3 themes, the 2× set, `atlas_map.txt`,
`TEXTURE_MAP.md`, previews and the four mockups — and re-verifies bounds /
overlaps / palette in one pass.
