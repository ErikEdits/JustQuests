# JustQuests GUI Texture Design Brief (v0.2)

This is a ready-to-use prompt for designing the JustQuests GUI textures.
Paste the block below into a Claude session when starting v0.2 GUI work.

---

You are designing the GUI textures for **JustQuests**, a lightweight,
datapack-driven quest book mod (https://github.com/ErikEdits/JustQuests,
NeoForge 1.21.x, MIT). The GUI ships in v0.2. The textures must be classic
Minecraft pixel art that works **unchanged in every game version from 1.14
to the newest** — the mod code draws them directly, so the assets must not
depend on any version-specific resource pack feature.

## Deliverables

1. `quest_book.png` — 256x256 texture atlas with the main screen background
   and all widgets
2. `icons.png` — 256x256 atlas for status icons (only if `quest_book.png`
   gets crowded; prefer a single atlas)
3. `TEXTURE_MAP.md` — documents every sprite's pixel coordinates
   (u, v, width, height) for use in `blit()` calls
4. A rendered mockup image of the assembled screen for review

Generate the PNGs programmatically (Python + Pillow). Pixel-perfect, no
anti-aliasing, no partial transparency except where specified.

## Screen layout to support

> **Note (2026-06-12):** the overall GUI style — book-style two-page
> layout vs. a modern single-panel list — is being decided by a Discord
> community poll (Q37). The layout below describes the book-style option;
> adapt if the poll picks the single-panel direction.

A book-style screen, 240x180 px base size, two panels:
- **Left panel (quest list):** scrollable rows, 18 px tall each, showing
  status icon + quest title; selected row gets a highlight frame
- **Right panel (quest details):** title area, description area, objective
  list with progress bars, one action button at the bottom
  (Accept / Abandon depending on state)

## Required sprites

| Sprite | Size | States |
|---|---|---|
| Screen background (book/panel) | 240x180 | 1 |
| List row highlight frame | 100x18 | selected, hovered |
| Action button (9-slice friendly) | 60x20 | normal, hover, disabled |
| Progress bar track | 100x5 | 1 |
| Progress bar fill | 100x5 | 1 (code crops width by progress) |
| Scrollbar track | 8x120 | 1 |
| Scrollbar handle | 8x15 | active, inactive |
| Status icon: available | 9x9 | 1 |
| Status icon: active | 9x9 | 1 |
| Status icon: completed (checkmark) | 9x9 | 1 |
| Status icon: locked (future chains) | 9x9 | 1 |

## Hard technical constraints (cross-version 1.14 → newest)

- Plain PNG atlas only, drawn via `blit()` — do **not** use the 1.20.2+
  `textures/gui/sprites/` system and no `.mcmeta` nine-slice scaling
  (that metadata does not exist before 1.20.2); bake 9-slice regions into
  the atlas and let code do the slicing
- Atlas size 256x256 (power of two), all sprites at integer coordinates
- Leave at least 2 px gap between sprites to avoid texture bleed
- No core shaders, no emissive textures, no animations
- Target path in the mod: `assets/justquests/textures/gui/`

## Style

- **Vanilla-plus:** it should look like Mojang could have shipped it
- Base the palette on the vanilla GUI tones so the screen feels native:
  base gray `#C6C6C6`, shadow `#555555`, highlight `#FFFFFF`,
  inset `#8B8B8B`; quest-book accents in parchment (`#E8D8A0` family) and
  oak tones (`#B8945F` family) are welcome
- Text is rendered by code (default label color `#404040`) — do not bake
  text into textures
- Simple and calm; readable at GUI scale 2 on a 1080p screen
- No gradients, no modern flat design, no drop shadows beyond the 1 px
  vanilla-style bevel

## Verification (do this before delivering)

1. Render a mockup assembling all sprites into the full screen
2. Cross-check every sprite boundary against `TEXTURE_MAP.md`
3. Confirm no sprites overlap and no off-by-one bleed at the edges
4. View the mockup downscaled 50% and upscaled 200% — it must stay readable

---

## Notes for the implementer (not part of the prompt)

- The textures are version-portable; only the Screen **code** differs per
  MC version (`GuiGraphics.blit` in 1.20+, `Screen.blit` earlier)
- `pack_format` reference if a standalone resource pack is ever published:
  1.14 = 4, 1.16.2 = 6, 1.18 = 8, 1.20.1 = 15, 1.21.1 = 34; a mismatch
  only produces a warning for simple texture packs
- Vanilla metric conventions used above: 18x18 slots, 20 px button height,
  176x166 container, 192x192 book screen
