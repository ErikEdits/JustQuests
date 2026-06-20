# JustQuests — GUI texture brief (v2)

A prompt for a pixel artist **or** an image/texture AI. Goal: produce the
texture set for the JustQuests quest-book GUI (the v0.2 headline feature).
It must look **exactly like a vanilla Minecraft container GUI** (inventory /
chest), be true pixel art, use **only the vanilla GUI colours**, and ship
the **clickable parts on a separate layer**.

---

## 0. The one-paragraph prompt (paste this)

> Create a Minecraft quest-book GUI texture set in authentic vanilla pixel-art
> style — indistinguishable from the vanilla chest/inventory screen. True
> 1:1 pixel art (nearest-neighbour, no anti-aliasing, no gradients, no blur,
> no drop shadows, no rounded corners). Use **only the vanilla GUI grey
> palette** listed below — no other colours. Light source is top-left
> (highlights on top/left edges, shadows on bottom/right), exactly like
> vanilla bevels. Deliver TWO separate layers: (1) a static **background
> panel**, and (2) an **interactive layer** holding every clickable element
> as its own sprite with all of its states (normal / hover / pressed /
> disabled / selected). Output transparent PNG at native GUI resolution
> (1 px = 1 GUI pixel), plus the layered source file with named layers.

Everything below is the detail that prompt expands to.

---

## 1. Hard style rules (do / don't)

**DO**
- Pixel art only, 1 px = 1 GUI pixel, nearest-neighbour scaling.
- Match the vanilla container look pixel-for-pixel: 1 px black outer
  outline, 1 px white top/left bevel, 1 px dark grey bottom/right bevel,
  flat light-grey face.
- Top-left light source for every bevel and inset.
- Keep it flat and clean — it should look like it shipped with the game.

**DON'T**
- No colours outside the palette in §2. No blues, greens, browns, etc.
- No anti-aliasing, gradients, soft shadows, glow, blur, or transparency
  fades. No rounded corners. No modern/flat-UI look. No non-pixel fonts.
- Don't bake text into the textures (the game draws text at runtime).

## 2. Palette — vanilla GUI greys ONLY

| Role | Hex |
|------|-----|
| Outer outline | `#000000` |
| Panel face (background) | `#C6C6C6` |
| Panel highlight bevel (top/left) | `#FFFFFF` |
| Panel shadow bevel (bottom/right) | `#555555` |
| Slot / inset face | `#8B8B8B` |
| Slot inset shadow (top/left) | `#373737` |
| Slot inset highlight (bottom/right) | `#FFFFFF` |
| Separator / mid line | `#373737` |
| Label text colour (reference only) | `#404040` |

State changes stay **inside** this palette:
- **hover** = 1 px `#FFFFFF` outline around the element + face lightened one
  step (toward `#FFFFFF`).
- **pressed** = bevels inverted (shadow on top/left) so it looks pushed in.
- **disabled** = face desaturated/darker (`#8B8B8B`) with no bevel, text
  greyed.
- **selected** (tabs, current quest) = pulled "forward": brighter face
  (`#C6C6C6`) and the connecting edge merged into the panel, like vanilla
  creative-inventory tabs.

## 3. Layers (this is the key requirement)

Deliver as **two separate layers / file groups**:

1. **`background`** — the static, non-interactive window: the outer frame,
   title bar area, the divider between the list and the detail pane, and any
   decorative insets. One image. Nothing clickable here.

2. **`interactive`** — every clickable control as its **own** sprite, each
   with its full state set stacked vertically in this order:
   `normal, hover, pressed, disabled` (add `selected` where noted). Name them
   `interactive/<element>/<state>`. This lets the dev map click regions and
   swap states without touching the background.

In the layered source file (Aseprite `.ase` or `.psd`), use exactly those
layer/group names so the export is unambiguous.

## 4. Elements to produce

**Background layer**
- `window` — main panel, sized for a two-pane quest book (suggest **248×184**
  px; vanilla bevels + outline). Designed to be **9-slice friendly** (a
  repeatable centre so it can scale), since it renders via classic `blit()`.
- `list_pane` / `detail_pane` insets (the two recessed areas), vanilla slot-
  style inset (dark `#373737` top/left, white bottom/right).
- `titlebar` strip.

**Interactive layer** (each: normal/hover/pressed/disabled; + selected where ✔)
- `tab` ✔ — category tab (gathering / farming / combat / survival / daily /
  custom). Leave a blank inner area for a 16×16 icon. Provide one neutral
  tab; the icon sits on top at runtime.
- `quest_row` ✔ — a quest entry in the list (full width of the list pane,
  ~16–18 px tall). Variants: `available`, `active`, `completed` (small inset
  check mark area on the right), `locked` (greyed), `claimable` (subtle
  white pulse outline using the hover style).
- `scroll_track` + `scroll_handle` (handle: normal/hover/grabbed).
- `scroll_arrow_up` / `scroll_arrow_down` (normal/hover/pressed/disabled).
- `button_claim` — primary action button, vanilla button look, ~20 px tall.
- `button_close` — small `X` button for the corner (~9–11 px square), drawn
  as a pixel X in `#373737`.
- `button_back` / `page_prev` / `page_next` — small arrow buttons.
- `progress_bar` — empty track + a fill piece (fill drawn left-to-right at
  runtime). Vanilla inset-bar look; fill is a lighter grey (`#FFFFFF` over
  `#8B8B8B`), NOT a colour.
- `icon_frame` — a 18×18 slot frame to hold each quest's item icon (exactly
  like an inventory slot).

**Tiny state glyphs** (monochrome, pixel, on the interactive layer)
- `glyph_check` (completed), `glyph_lock` (locked), `glyph_repeat` (a small
  two-arrow loop for repeatable). Drawn in `#373737` / `#555555` only.

## 5. Layout (described, for the artist)

Two-pane quest book inside one vanilla panel:
- **Top:** title bar (game draws "Quests" text) + `button_close` top-right.
- **Left pane:** a vertical column of `tab`s (categories) and below/around
  it the scrollable list of `quest_row`s with `scroll_track`/arrows.
- **Right pane:** the selected quest's detail — an `icon_frame`, room for the
  title + description text, a few `progress_bar`s for objectives, a rewards
  strip of `icon_frame`s, and the `button_claim` at the bottom.

Keep generous flat margins like vanilla; don't crowd the bevels.

## 6. Technical / delivery

- Format: **PNG, 32-bit RGBA**, transparent outside the panel shapes.
- Authored at **1× native GUI resolution** (1 px = 1 GUI pixel). No HD/2×
  for now — the source is 1× so it stays crisp with `blit()`.
- Nearest-neighbour everywhere; never resample with smoothing.
- Note each element's **pixel dimensions** next to it.
- Deliver:
  1. the **layered source** (`.ase` or `.psd`) with the `background` and
     `interactive/<element>/<state>` layers named as in §3, **and**
  2. **individual exported PNGs** per element + state, **and**
  3. optionally a **packed atlas** PNG + a small text map of x/y/w/h per
     sprite (handy for `blit()`).
- Must work with the **classic `blit()` atlas** approach (not the 1.20.2+
  GUI sprite/JSON system) — the mod ships one texture set across MC
  1.21–1.21.5, so a plain atlas keeps it version-proof.

## 7. JustQuests context (so the set is complete)

The GUI must be able to show: quest **categories** (gathering, farming,
combat, survival, daily, custom), quest **states** (available, active,
locked behind prerequisites, completed, claimable), per-objective **progress
counts**, **rewards**, and a **repeatable** marker. Every one of those needs
a matching texture/state above. Text and item icons are drawn by the game at
runtime — leave clean flat areas for them.
