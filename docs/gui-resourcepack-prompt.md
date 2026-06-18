# JustQuests — Full GUI Resource Pack Prompt (for Claude Design)

> **Purpose:** a complete, self-contained brief to generate ALL GUI
> textures for the JustQuests v0.2 interface, in advance. Hand this whole
> file to Claude Design.
>
> **Status:** assets only. These textures are produced ahead of time and
> will be wired into the mod in v0.2 — **do NOT change any mod code from
> this prompt.** The deliverable is a set of PNGs + a coordinate map.
>
> This expands the shorter [gui-design-brief.md](gui-design-brief.md) into
> a full per-sprite specification.

---

## 0. What to deliver (summary)

1. A texture atlas `quest_gui.png` (256x256) with all core widgets.
2. A second atlas `quest_icons.png` (256x256) for category/status/
   difficulty icons (kept separate so icons can grow without reflowing
   the main atlas).
3. `TEXTURE_MAP.md` documenting every sprite: name, atlas, `u, v, w, h`,
   and each state's coordinates — ready to drop into `blit()` calls.
4. Assembled mockups (PNG) of every screen and overlay for review.
5. Everything generated programmatically (Python + Pillow), pixel-perfect.

Target in-mod path (for reference, not for you to install):
`assets/justquests/textures/gui/`

---

## 1. Project context

JustQuests is a lightweight quest-book mod (NeoForge now; Fabric/Forge and
a Paper plugin later). v0.1 is command-only. v0.2 adds an in-game GUI.
The GUI must feel like a natural, vanilla-quality Minecraft screen — calm,
readable, "Mojang could have shipped this."

The community will pick the overall layout via a poll, so this prompt
specifies **both** candidate layouts. Produce assets for **both** so we
are ready either way:
- **Layout A — Book style:** a two-page open book (matches the
  "quest book" identity, FTB-Quests-like).
- **Layout B — Single panel:** one modern panel with a scrollable list on
  the left and a detail pane on the right.

Most widgets (buttons, bars, icons, slots) are shared between layouts, so
this is far less than 2x the work.

---

## 2. Hard technical constraints (cross-version: MC 1.14 -> newest)

These are non-negotiable; the same PNGs must work unchanged from MC 1.14
to the latest version, because the mod's code does the drawing.

- **Plain PNG atlases only**, drawn via `blit()` from explicit u/v/w/h.
- **Do NOT** use the 1.20.2+ `textures/gui/sprites/` system or any
  `.mcmeta` nine-slice/animation metadata (it does not exist pre-1.20.2).
  Bake 9-slice regions into the atlas; the mod slices in code.
- **Atlas size 256x256** (power of two). If more space is needed, add
  another 256x256 atlas rather than going to 512.
- **Integer coordinates** for every sprite. No sub-pixel placement.
- **>=2 px transparent gutter** between sprites to avoid texture bleed.
- **No** core shaders, emissive maps, normal maps, or animation.
- **No anti-aliasing.** Hard pixel edges only. No partial-alpha gradients
  except the few soft shadows explicitly called out (and those are simple
  ordered dither or a single 50%-alpha pixel row, never smooth gradients).
- Design at **1x** (GUI pixels = texture pixels). The game scales the
  whole GUI by the user's GUI scale; never pre-scale.
- Color via a fixed palette (Section 3). No off-palette colors.

---

## 3. Art direction & palette

Style: **vanilla-plus**. Base it on the vanilla container/book look, then
add warm parchment + oak accents so it reads as a quest book.

Use ONLY these colors (hex). Each "ramp" is dark -> mid -> light for
shading (top-left light, bottom-right shadow, 1 px bevel).

Vanilla GUI grays (frames, generic panels):
- `#2B2B2B` outline (near-black border)
- `#555555` deep shadow
- `#8B8B8B` slot inset / mid shadow
- `#C6C6C6` base gray
- `#FFFFFF` highlight

Parchment (pages, list rows):
- `#B8A06A` parchment shadow
- `#D6C492` parchment shadow-mid
- `#E8D8A0` parchment base
- `#F4ECC6` parchment highlight

Oak / leather (book cover, frames, headers):
- `#6E4A24` oak shadow
- `#8A5A2B` oak dark
- `#A8743E` oak mid
- `#C49A63` oak light

Accents (status & feedback):
- `#5ABE50` green (complete / success) + shadow `#3C9637`
- `#E0B33A` gold (available "!" / highlight) + shadow `#B07E1E`
- `#5B8DD6` blue (active / info) + shadow `#3C6196`
- `#C24B4B` red (locked / abandon / error) + shadow `#8E3030`
- `#9B6BD6` purple (AI-generated category) + shadow `#6E47A0`

Text is rendered by the mod (never bake text into textures). Reserve
quiet areas for text. Default body text color the mod will use: `#404040`
on parchment, `#FFFFFF` with shadow on dark headers.

---

## 4. Atlas strategy & file layout

Two atlases, both 256x256, transparent background:

- `quest_gui.png` — backgrounds, panels, buttons, bars, scrollbar, slots,
  frames, search box, toast, HUD tracker.
- `quest_icons.png` — category icons, status icons, difficulty pips, small
  decorative glyphs.

In `TEXTURE_MAP.md`, group entries by section and give every sprite a
stable name (e.g. `button.accept.normal`). Use a consistent grid where
possible (e.g. 20 px button row at a fixed v) so future additions are easy.

---

## 5. Layout A — Book style (two open pages)

Overall screen sprite: **256 x 181** (fits within the 256 atlas width;
the assembled background is one sprite the mod centers on screen).

Anatomy:
- Oak cover border framing the whole book, ~6 px thick, with the oak ramp
  bevel (light top-left, shadow bottom-right).
- A center **spine/binding** strip ~6 px wide down the middle (oak dark +
  a 1 px `#2B2B2B` seam) dividing two parchment pages.
- **Left page (quest list):** parchment field with faint horizontal rule
  lines (`#D6C492`, 1 px every 18 px) suggesting rows.
- **Right page (quest detail):** clean parchment with a header band, a
  description area, an objective area, and a footer for the action button.
- A subtle 1 px inner shadow where pages meet the cover.

Sprites for Layout A (all on `quest_gui.png`):
- `book.background` — 256x181, the assembled empty book (cover + spine +
  two blank parchment pages with rule lines).
- `book.page_highlight` — 100x18, a selected-row frame for the left page
  (parchment highlight `#F4ECC6` fill, 1 px oak-mid border). Provide a
  second tint `book.page_hover` (slightly lighter).
- `book.tab` — 24x24 side tab for category switching, two states
  (`book.tab.active`, `book.tab.inactive`), drawn protruding from the
  book's left edge.
- `book.corner` — 12x12 page-turn corner ornament (decorative, optional).

---

## 6. Layout B — Single panel (modern list + detail)

Overall background sprite: **248 x 166** (vanilla-ish proportions).

Anatomy:
- Vanilla-gray rounded panel (gray ramp, 1 px `#2B2B2B` outline, bevel).
- A **left column** (list, ~96 px wide) with a faint inset (`#8B8B8B`
  groove) separating it from the **right detail pane**.
- A top header band (oak-mid) ~16 px tall spanning the panel for the title
  + category tabs.
- Footer area in the detail pane for the action/claim button.

Sprites for Layout B:
- `panel.background` — 248x166 assembled empty panel.
- `panel.list_row` — 96x16 list row, states: `normal` (transparent),
  `hover` (`#C6C6C6` 30%-ish via 1 px dither), `selected` (blue-tinted
  1 px frame `#5B8DD6`).
- `panel.divider` — 2x120 vertical groove (inset look) between list and
  detail.
- `panel.header_tab` — 28x14 category tab for the header, `active` /
  `inactive`.

---

## 7. Shared widgets (used by both layouts) — `quest_gui.png`

### 7.1 Buttons (20 px tall, 9-slice friendly)
Provide a button sprite designed so the mod can stretch the middle and
keep 3 px caps. Supply at width 80, height 20, for each:
- `button.normal` — gray ramp, raised bevel.
- `button.hover` — slightly lighter + 1 px gold outline.
- `button.disabled` — desaturated, flat, no bevel.
- `button.pressed` — inverted bevel (shadow top-left).

Also provide three colored button variants (same 80x20, 4 states each),
for semantic actions:
- `button.accept.*` — green ramp.
- `button.abandon.*` — red ramp.
- `button.claim.*` — gold ramp.

### 7.2 Progress bar
- `bar.track` — 100x6, empty bar (inset groove, `#8B8B8B` + `#555555`).
- `bar.fill.green` — 100x6, full green fill (mod crops width by %).
- `bar.fill.blue` — 100x6, full blue fill (alt color for active).
- `bar.cap` — 2x6, optional rounded end cap.
Make the fill a flat color with a 1 px top highlight so cropping any width
still looks correct.

### 7.3 Scrollbar
- `scroll.track` — 8x120, vertical groove.
- `scroll.handle.normal` — 8x16.
- `scroll.handle.hover` — 8x16 (lighter).
- `scroll.handle.disabled` — 8x16 (flat).

### 7.4 Search box (appears only at high quest counts)
- `search.box` — 100x14, inset text field (1 px `#2B2B2B` border,
  `#8B8B8B` inset), leave interior dark/parchment for code-drawn text.
- `search.icon` — 8x8 magnifier glyph (place left inside the box).
- `search.clear` — 8x8 small "x" glyph.

### 7.5 Item/reward slot & quest-icon frame
- `slot` — 18x18 vanilla item slot (the classic `#8B8B8B` inset square
  with `#373737`-style dark inner). Used for reward/objective item icons.
- `icon_frame` — 20x20 framed slot for the per-quest icon (Q44), with a
  1 px oak border so the quest's item icon sits in it nicely.
- `icon_frame.selected` — 20x20 highlighted version.

### 7.6 Tooltip background (optional)
- `tooltip.bg` — 16x16 nine-slice tooltip background matching vanilla
  (dark fill `#100010`-ish with the purple-ish border). Only needed if the
  mod draws custom tooltips; otherwise vanilla tooltips are used. Provide
  it but mark optional.

---

## 8. Category icons — `quest_icons.png` (Q3)

9x9 each, two variants per category: `small` (9x9, for list rows) and
`badge` (12x12, framed, for the detail header). Categories:
- `cat.datapack` — a small book/page glyph, oak tones.
- `cat.custom` — a pencil/quill glyph, parchment + oak.
- `cat.ai` — a spark/star-burst glyph, purple accent (`#9B6BD6`).
- `cat.team` — two-figures / shield glyph, blue accent (`#5B8DD6`).

Each must be distinguishable by **shape** alone (not just color) for
colorblind accessibility.

---

## 9. Status icons — `quest_icons.png` (Q28)

9x9 each:
- `status.available` — gold "!" (`#E0B33A`), the classic RPG quest mark.
- `status.active` — blue half-filled circle / arrow (`#5B8DD6`).
- `status.completed` — green checkmark (`#5ABE50`).
- `status.locked` — gray padlock (`#8B8B8B`) for the "???" teaser.
- `status.claim` — gold gift/star pulse to mark a claimable reward.

Again: distinct shapes, not color-only.

---

## 10. Difficulty pips — `quest_icons.png` (Q9)

Small indicators for Easy/Normal/Hard (server difficulty + per-quest
hint):
- `diff.easy` — 1 filled pip, green.
- `diff.normal` — 2 pips, gold.
- `diff.hard` — 3 pips, red.
Provide each as a single 16x6 sprite showing the pip row.

---

## 11. Reward & claim UI

- `reward.tray` — 80x24 strip that holds 1–4 reward slots, parchment inset.
- Reuse `slot` (7.5) for each reward item.
- `claim.button` — covered by `button.claim.*` (7.1).
- **Choice reward picker (Q49):** `choice.frame` — 60x40 framed mini-panel
  showing 2–3 selectable reward slots; states `choice.option.normal` and
  `choice.option.selected` (24x24 each, selected has a gold 1 px frame +
  subtle glow row).

---

## 12. HUD tracker overlay (Q43) — `quest_gui.png`

A small, semi-transparent on-screen tracker (top-right by default,
toggleable). It must read well over any game background.
- `hud.bg` — 120x40 rounded dark panel at ~70% opacity (use a flat
  `#000000` at reduced alpha baked as a single tone, or a 1 px dither for
  the body — keep it simple). 1 px `#2B2B2B` border, subtle oak top edge.
- `hud.bar.track` / `hud.bar.fill` — 100x4 mini progress bar (can reuse a
  thinner version of 7.2).
- Leave the interior clear for code-drawn quest title + `5/16` text.

---

## 13. Toast / notification (Q12)

Vanilla-style toast for quest events (poll may enable this):
- `toast.bg` — 160x32 toast background (vanilla toast proportions, oak +
  parchment trim).
- `toast.accent.complete` — 4x32 left accent strip, green.
- `toast.accent.available` — 4x32 left accent strip, gold.
- Reserve right area for a code-drawn icon (reuse status icons) + two
  lines of text.

---

## 14. Empty state (Q70)

- `empty.art` — 48x48 friendly illustration (a closed book or a dotted
  quest mark) shown when no quests are available, parchment/oak tones,
  calm. Centered in the list area with code-drawn caption below.

---

## 15. Deliverables in detail

Produce, in this order:
1. `quest_gui.png` (256x256) — all Section 5/6/7/11/12/13/14 sprites laid
   out with >=2 px gutters; document every coordinate.
2. `quest_icons.png` (256x256) — all Section 8/9/10 sprites.
3. `TEXTURE_MAP.md` — for every sprite: `name | atlas | u | v | w | h |
   notes/states`. Group by section. This is what the mod will code against
   — accuracy here matters most.
4. Mockups (separate PNGs, may be any size):
   - `mockup_book.png` — Layout A fully assembled with example quests,
     icons, a progress bar mid-fill, a selected row, an accept button.
   - `mockup_panel.png` — Layout B fully assembled, same content.
   - `mockup_hud.png` — the HUD tracker composited over a screenshot-like
     background.
   - `mockup_toast.png` — a completion toast.
   - `mockup_states.png` — a sheet showing every button/icon state.

---

## 16. Generation method (Python + Pillow)

- Build each sprite on an integer pixel grid; scale up only for the review
  mockups (nearest-neighbor), never for the shipped atlas.
- Write a small, readable generator script per atlas so coordinates are
  reproducible. Emit `TEXTURE_MAP.md` from the same script (single source
  of truth for coordinates).
- Save PNGs as 32-bit RGBA, no interlacing, no color profile.
- Verify with the script that no two sprites' bounding boxes overlap and
  every sprite has a >=2 px gutter.

---

## 17. Verification checklist (do before delivering)

1. Every coordinate in `TEXTURE_MAP.md` matches the actual atlas (assert
   in code).
2. No sprite overlaps; all within 0..255.
3. Only palette colors used (assert: every opaque pixel is in the palette,
   allowing the few documented dither/alpha rows).
4. Each icon is distinguishable in grayscale (colorblind check).
5. Mockups assembled purely from the atlases via the documented
   coordinates (proves the map is correct and usable).
6. View mockups at 1x, 2x, 3x — readable and crisp at all.

---

## 18. Naming & paths (for the eventual mod wiring — reference only)

Final in-mod locations (the mod author will place them; you just produce
the files with these names):
- `assets/justquests/textures/gui/quest_gui.png`
- `assets/justquests/textures/gui/quest_icons.png`
- Keep `TEXTURE_MAP.md` and the generator scripts alongside as dev assets.

Sprite naming: `category.element.state` (lowercase, dots), e.g.
`button.accept.hover`, `status.completed`, `cat.ai.badge`.

---

## 19. What NOT to do

- Do not use the 1.20.2+ sprite/mcmeta system or animations.
- Do not bake any text into textures.
- Do not use off-palette colors, smooth gradients, or anti-aliasing.
- Do not exceed 256x256 per atlas (add another atlas instead).
- Do not change or assume any mod code — this is an assets-only task.
- Do not rely on color alone to convey meaning (shapes must differ).

---

## 20. Priority if time-boxed

If everything can't be done at once, deliver in this order (v0.2 needs the
top group first):
1. `book.background` OR `panel.background` (whichever the poll picks; if
   unknown, do book first), `slot`, `icon_frame`, `button.*` (normal/hover/
   disabled/pressed + accept/abandon/claim), `bar.*`, `scroll.*`.
2. `status.*` and `cat.*` icons.
3. `search.*`, `reward.tray`, `choice.*`.
4. `hud.*`, `toast.*`, `empty.art`, `diff.*`.
5. Mockups.
