# JustQuests — GUI texture brief v2 FULL ("everything")

The big expansion of [gui-prompt-v2.md](gui-prompt-v2.md). Same hard style
(vanilla Minecraft pixel art, vanilla grey palette only, clickable parts on
a separate layer) — but now covering **every** element, state, icon set,
animation, and delivery option a complete quest-book GUI could want. Treat
the base brief as the rules; treat this as the full shopping list. Generate
only what's marked **[core]** first if time is short; **[extra]** and
**[opt]** can follow.

---

## 0. The mega-prompt (paste this)

> Produce a COMPLETE Minecraft quest-book GUI texture pack in authentic
> vanilla pixel-art style — indistinguishable from the vanilla
> chest/inventory screen. True 1:1 pixel art: nearest-neighbour, no
> anti-aliasing, no gradients, no blur, no soft shadows, no rounded corners,
> no non-pixel fonts, no baked-in text. Top-left light source. Use ONLY the
> vanilla GUI grey palette (below) — no other colours, except the documented
> tooltip exception. Deliver in named LAYERS: `background` (static frame),
> `interactive` (every clickable control, each with all states), `icons`
> (objective/reward/category icon sets), `overlay` (tooltips, dialogs,
> toasts, HUD tracker), and `anim` (optional animation strips). Provide every
> element as a 9-slice where it can scale, every control with the full state
> set (normal/hover/pressed/disabled/selected/focused), and the full icon
> sets. Export: layered source (.ase/.psd) + individual PNGs + per-element
> strips + a packed atlas with a coordinate map. Native 1× GUI resolution
> (1 px = 1 GUI pixel); optionally also a 2× HD set. Classic `blit()`-style
> atlas (not the 1.20.2 sprite-JSON system) so one set works on MC
> 1.21–1.21.5.

---

## 1. Style rules (unchanged, restated)

- Pixel art, 1 px = 1 GUI pixel, nearest-neighbour only.
- Vanilla container look: 1 px black outer outline, 1 px white top/left
  bevel, 1 px dark bottom/right bevel, flat face.
- Top-left light. Flat. No effects beyond what vanilla itself uses.
- No text baked in; leave flat zones for runtime text + item icons.

## 2. Palette (vanilla greys + two documented exceptions)

| Role | Hex |
|------|-----|
| Outer outline | `#000000` |
| Panel face | `#C6C6C6` |
| Highlight bevel (top/left) | `#FFFFFF` |
| Shadow bevel (bottom/right) | `#555555` |
| Slot / inset face | `#8B8B8B` |
| Inset shadow (top/left) | `#373737` |
| Inset highlight (bottom/right) | `#FFFFFF` |
| Separator / mid | `#373737` |
| Label text (reference) | `#404040` |
| Disabled text (reference) | `#A0A0A0` |

Documented exceptions (only here, optional):
- **Tooltip box** may use the vanilla tooltip look (near-black `#100010`
  fill, `#5000FF`→`#28007F` purple gradient border) — this is vanilla.
  Provide a greyscale alternative too for the "no colours" purists.
- **Focus ring** for keyboard/controller: 1 px `#FFFFFF` dashed/solid
  outline (greyscale, no colour).

State conventions (stay in palette): hover = white outline + face one step
lighter; pressed = bevels inverted (pushed in); disabled = flat `#8B8B8B`,
no bevel; selected = brighter face merged into parent; focused = focus ring.

## 3. Layers

1. `background` **[core]** — static frame, panes, title bar, dividers.
2. `interactive` **[core]** — every clickable control, states stacked
   `normal,hover,pressed,disabled,selected,focused` (only the states that
   apply), named `interactive/<element>/<state>`.
3. `icons` **[extra]** — objective/reward/category/state icon sets.
4. `overlay` **[extra]** — tooltip, confirm dialog, toast/popup, HUD tracker.
5. `anim` **[opt]** — animation strips (claimable pulse, spinner, slides).

## 4. Element catalog

### 4.1 Window & frame  **[core]**
- `window` panel as **explicit 9-slice tiles**: 4 corners, 4 edges, 1
  centre — so any size renders crisp. (Plus a pre-assembled ~248×184 sample.)
- `titlebar` strip; `divider_v` (between panes); `divider_h`.
- `list_pane` and `detail_pane` recessed insets (vanilla slot inset style).
- Variants: a slightly larger `window_wide` (3-pane) and `window_compact`.

### 4.2 Navigation  **[core]/[extra]**
- `tab` (top **and** side variants), states inc. `selected`; blank 16×16
  icon area inside. **[core]**
- `tab_overflow` (« » when too many tabs). **[extra]**
- `search_field`: track, `focused` (with caret position marker),
  `placeholder` (empty), `clear_x` button. **[extra]**
- `sort_button` (cycles: A–Z / progress / category) + `sort_dir` arrow. **[extra]**
- `filter_button` + `filter_dropdown` (collapsed/expanded, row, row-hover,
  checkbox checked/unchecked). **[extra]**
- `page_prev` / `page_next` **[core]**; `page_dots` (•◦ indicators). **[extra]**

### 4.3 Quest list  **[core]/[extra]**
- `quest_row` states: `available, hover, selected, active, completed,
  locked, claimable` **[core]** + `new` (unread dot), `favorited` (star),
  `pinned`, `expired`, `in_progress` (inline mini-bar). **[extra]**
- `group_header` (category section header inside the list). **[extra]**
- `row_divider`; `row_progress_inline` (thin bar at the row's bottom edge).

### 4.4 Detail pane  **[core]/[extra]**
- `icon_frame` (18×18 slot) states `normal/selected/empty`. **[core]**
- `objective_row`: label zone + a `progress_bar`; states `incomplete`,
  `complete` (check), with an objective-type icon slot on the left. **[core]**
- `progress_bar` variants: `track`+`fill` **[core]**; `segmented` (per unit),
  `large`, `mini`, `ready` (claimable highlight/pulse). **[extra]**
- `reward_slot`: item slot with quantity zone; states `normal`, `hover`,
  `claimed` (greyed + check overlay), `choice` (pick-one highlight ring),
  `locked`. **[extra]**
- `difficulty_pips` (1–3 filled/empty pips). **[extra]**
- Badges: `badge_repeatable`, `badge_cooldown` (with a clock), `badge_daily`,
  `badge_new`. **[extra]**
- Buttons: `button_claim` **[core]**, `button_abandon`, `button_track`
  (pin to HUD), `button_back` **[core]**. **[extra]**

### 4.5 Scroll  **[core]/[extra]**
- Vertical: `scroll_track`, `scroll_handle` (normal/hover/grabbed),
  `scroll_arrow_up/down` (normal/hover/pressed/disabled). **[core]**
- Horizontal variants of the above. **[extra]**

### 4.6 Dialogs & feedback (overlay)  **[extra]**
- `dialog` modal panel (smaller window) + `dialog_button_yes` /
  `dialog_button_no` (e.g. "Abandon this quest?").
- `tooltip` box 9-slice (vanilla + greyscale variant).
- `toast` / `popup` (quest complete / reward claimed) 9-slice, + slide-in
  anim frames in `anim`.
- `empty_state` (a small "no quests here" pixel illustration, greyscale).
- `loading_spinner` (4–8 frame pixel spinner) in `anim`.

### 4.7 Misc controls  **[extra]/[opt]**
- `button_settings` (gear), `button_help` (?), `button_info` (i).
- `checkbox` (checked/unchecked/hover/disabled), `radio` (on/off).
- `slider` (track + handle) for any numeric setting.
- `focus_ring` overlay (keyboard/controller selection).
- `corner_grip` (decorative only; vanilla doesn't resize windows).

## 5. Icon sets (16×16 each, greyscale pixel)  **[extra]**

- **Objective icons (13)** — one per type:
  `collect_item, mine_block, craft_item, smelt_item, consume_item,
  place_block, kill_mob, tame_animal, breed_animal, gain_advancement,
  visit_dimension, reach_level, reach_location`. Each: `normal` + `done`
  (with check overlay) + `locked` (greyed).
- **Reward icons (6)** — `give_item, loot_table, xp, effect, message,
  command`.
- **Category icons (6)** — `gathering, farming, combat, survival, daily,
  custom` (sit inside the `tab` icon area).
- **State glyphs** — `check, lock, repeat, star, new_dot, clock(cooldown),
  xp, exclamation`.

All icons readable at 16×16, recognisably Minecraft-ish (e.g. a pixel
pickaxe for mine, a sword for kill), greyscale only.

## 6. State matrix (apply where meaningful)

`normal · hover · pressed · disabled · selected · focused · active ·
completed · locked · claimable · new · favorited · error · success`

For each interactive element, deliver the subset that applies, stacked
vertically in that order, labelled.

## 7. Animations (optional strips, `anim/`)  **[opt]**

- `claimable_pulse` (2–3 frames; white outline breathing).
- `loading_spinner` (4–8 frames).
- `toast_slide` (3–4 frames slide-in).
- `tab_switch` (2 frames).
- `progress_fill_shimmer` (2 frames).
Each as a horizontal frame strip + a note of frame count / suggested ms.

## 8. 9-slice spec  **[core]**

For every scalable panel (`window`, `tooltip`, `dialog`, `toast`,
`quest_row`, `button_*`, `progress_bar`): mark the 9-slice insets (left,
right, top, bottom border widths) so the centre tiles and borders stay 1 px
crisp at any size. Deliver the explicit corner/edge/centre tiles **and** a
sample assembled image.

## 9. Resolution  **[core]/[opt]**

- `1x/` **[core]** — native, 1 px = 1 GUI pixel (the real source).
- `2x/` **[opt]** — hand-cleaned 2× (not naive upscale) for HD resource
  packs; same palette, same shapes, double pixels.

## 10. Delivery

- `source/` — layered `.ase` (preferred) or `.psd`, layers named exactly as
  in §3 (`background`, `interactive/<el>/<state>`, `icons/<set>/<name>`,
  `overlay/<el>`, `anim/<el>`).
- `png/` — individual PNG per element+state (the file tree you already have).
- `strips/` — per-element state strips (you already have these).
- `atlas/` — packed atlas PNG(s) **+** `atlas_map.txt` (name → x,y,w,h) for
  `blit()`.
- `mockup/` — assembled window + a full states sheet (you already have these).
- `README.md` — palette, sizes, 9-slice insets, naming, atlas coords.

## 11. Themes & accessibility  **[opt]**

- `theme_standard` (the vanilla grey above) — default.
- `theme_dark` (optional): a darker vanilla-like grey set, same shapes.
- `theme_highcontrast` (optional): stronger black/white edges for
  readability — still greyscale.
- Provide all three from the SAME shapes if generated (palette swap only).

## 12. HUD tracker (off-book overlay)  **[extra]**

A small on-screen quest tracker (not part of the book window):
- `hud_panel` (semi-transparent 9-slice, top-right by default),
- `hud_row` (objective line with a mini `progress_bar`),
- `hud_pin`/`hud_unpin` toggle. Keep it subtle and vanilla.

## 13. Master checklist

```
[ ] window 9-slice tiles (+ sample)   [ ] titlebar / dividers / panes
[ ] tabs (top+side, all states, icon area)
[ ] search field (+focused/clear)     [ ] sort + filter (+dropdown)
[ ] page prev/next + dots
[ ] quest_row all states (avail/hover/selected/active/completed/locked/
    claimable/new/favorited/pinned/expired)
[ ] group header / dividers
[ ] icon_frame states                 [ ] objective_row + type icon slot
[ ] progress_bar variants (track/fill/segmented/large/mini/ready)
[ ] reward_slot states (normal/hover/claimed/choice/locked)
[ ] difficulty pips                    [ ] badges (repeatable/cooldown/daily/new)
[ ] buttons (claim/abandon/track/back/settings/help/info)
[ ] scroll V (+H) full states
[ ] dialog + yes/no                    [ ] tooltip (vanilla+grey)
[ ] toast/popup                        [ ] empty state  [ ] loading spinner
[ ] checkbox / radio / slider          [ ] focus ring
[ ] objective icons x13 (+done/locked) [ ] reward icons x6
[ ] category icons x6                  [ ] state glyphs x8
[ ] animations (pulse/spinner/toast/tab/shimmer)
[ ] 1x set  [ ] (opt) 2x set
[ ] source + png + strips + atlas+map + mockups + README
[ ] (opt) dark + high-contrast themes  [ ] (opt) HUD tracker set
```

## 14. JustQuests data → texture mapping (don't miss a state)

The GUI must visually cover every real game state:
- **Categories:** gathering, farming, combat, survival, daily, custom → tab + category icon each.
- **Objective types (13):** each needs a type icon + progress display.
- **Reward types (6):** give_item, loot_table, xp, effect, message, command → reward slot/icon.
- **Quest states:** available, active (in progress), locked (prereq), completed, claimable, repeatable (+ on cooldown).
- **Per-quest:** title, description, difficulty, requires-chain hint, repeatable/cooldown badge.
Every one of those maps to a texture/state in the checklist above — if a
game state has no texture, it's missing.
