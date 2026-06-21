# JustQuests v0.2 - TEXTURE_MAP

Single source of truth for `blit(x, y, u, v, w, h)`. Coordinates are pixels within each atlas (origin top-left). All atlases are 256x256, RGBA, no AA, >=2px gutters.

Atlases:
- `atlas/quest_book.png`
- `atlas/quest_gui.png`
- `atlas/quest_gui2.png`
- `atlas/quest_icons.png`
- `atlas/quest_panel.png`

In-mod path (reference only): `assets/justquests/textures/gui/`

## 5. Layout A - Book

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `book.background` | quest_book | 0 | 2 | 256 | 181 | centered full-screen book; cover+spine+two parchment pages |
| `book.tab.active` | quest_book | 2 | 185 | 24 | 24 | side category tab, protruding |
| `book.tab.inactive` | quest_book | 28 | 185 | 24 | 24 | side category tab, recessed |
| `book.page_highlight` | quest_book | 54 | 185 | 100 | 18 | selected left-page row frame |
| `book.page_hover` | quest_book | 2 | 211 | 100 | 18 | hovered left-page row frame (subtler) |
| `book.corner` | quest_book | 104 | 211 | 12 | 12 | decorative page-turn corner (optional) |

## 6. Layout B - Panel

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `panel.background` | quest_panel | 2 | 2 | 248 | 166 | assembled empty panel; header band + list column + detail |
| `panel.header_tab.active` | quest_panel | 2 | 170 | 28 | 14 | header category tab, active |
| `panel.header_tab.inactive` | quest_panel | 32 | 170 | 28 | 14 | header category tab, inactive |
| `panel.list_row.normal` | quest_panel | 62 | 170 | 96 | 16 | list row, transparent |
| `panel.list_row.hover` | quest_panel | 2 | 188 | 96 | 16 | list row, 1px dither wash |
| `panel.list_row.selected` | quest_panel | 100 | 188 | 96 | 16 | list row, blue 1px frame |
| `panel.divider` | quest_gui | 12 | 2 | 2 | 120 |  |

## 7.1 Buttons

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `button.normal` | quest_gui | 136 | 158 | 80 | 20 |  |
| `button.hover` | quest_gui | 2 | 184 | 80 | 20 |  |
| `button.disabled` | quest_gui | 84 | 184 | 80 | 20 |  |
| `button.pressed` | quest_gui | 166 | 184 | 80 | 20 |  |
| `button.accept.normal` | quest_gui | 2 | 206 | 80 | 20 |  |
| `button.accept.hover` | quest_gui | 84 | 206 | 80 | 20 |  |
| `button.accept.disabled` | quest_gui | 166 | 206 | 80 | 20 |  |
| `button.accept.pressed` | quest_gui | 2 | 228 | 80 | 20 |  |
| `button.abandon.normal` | quest_gui | 84 | 228 | 80 | 20 |  |
| `button.abandon.hover` | quest_gui | 166 | 228 | 80 | 20 |  |
| `button.abandon.disabled` | quest_gui2 | 2 | 2 | 80 | 20 |  |
| `button.abandon.pressed` | quest_gui2 | 84 | 2 | 80 | 20 |  |
| `button.claim.normal` | quest_gui2 | 166 | 2 | 80 | 20 |  |
| `button.claim.hover` | quest_gui2 | 2 | 24 | 80 | 20 |  |
| `button.claim.disabled` | quest_gui2 | 84 | 24 | 80 | 20 |  |
| `button.claim.pressed` | quest_gui2 | 166 | 24 | 80 | 20 |  |

## 7.2 Progress bar

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `bar.track` | quest_gui2 | 2 | 68 | 100 | 6 |  |
| `bar.fill.green` | quest_gui2 | 104 | 68 | 100 | 6 |  |
| `bar.fill.blue` | quest_gui2 | 2 | 76 | 100 | 6 |  |
| `bar.cap` | quest_gui2 | 104 | 76 | 2 | 6 |  |

## 7.3 Scrollbar

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `scroll.track` | quest_gui | 2 | 2 | 8 | 120 |  |
| `scroll.handle.normal` | quest_gui2 | 84 | 46 | 8 | 16 |  |
| `scroll.handle.hover` | quest_gui2 | 94 | 46 | 8 | 16 |  |
| `scroll.handle.disabled` | quest_gui2 | 104 | 46 | 8 | 16 |  |

## 7.4 Search box

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `search.box` | quest_gui2 | 114 | 46 | 100 | 14 |  |
| `search.icon` | quest_gui2 | 216 | 46 | 8 | 8 |  |
| `search.clear` | quest_gui2 | 226 | 46 | 8 | 8 |  |

## 7.5 Slot / icon frame

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `icon_frame` | quest_gui2 | 2 | 46 | 20 | 20 |  |
| `icon_frame.selected` | quest_gui2 | 24 | 46 | 20 | 20 |  |
| `slot` | quest_gui2 | 46 | 46 | 18 | 18 |  |

## 7.6 Tooltip

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `tooltip.bg` | quest_gui2 | 66 | 46 | 16 | 16 |  |

## 8. Category icons

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `cat.datapack.small` | quest_icons | 2 | 2 | 9 | 9 |  |
| `cat.custom.small` | quest_icons | 13 | 2 | 9 | 9 |  |
| `cat.ai.small` | quest_icons | 24 | 2 | 9 | 9 |  |
| `cat.team.small` | quest_icons | 35 | 2 | 9 | 9 |  |
| `cat.datapack.badge` | quest_icons | 46 | 2 | 12 | 12 |  |
| `cat.custom.badge` | quest_icons | 60 | 2 | 12 | 12 |  |
| `cat.ai.badge` | quest_icons | 74 | 2 | 12 | 12 |  |
| `cat.team.badge` | quest_icons | 88 | 2 | 12 | 12 |  |

## 9. Status icons

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `status.available` | quest_icons | 102 | 2 | 9 | 9 |  |
| `status.active` | quest_icons | 113 | 2 | 9 | 9 |  |
| `status.completed` | quest_icons | 124 | 2 | 9 | 9 |  |
| `status.locked` | quest_icons | 135 | 2 | 9 | 9 |  |
| `status.claim` | quest_icons | 146 | 2 | 9 | 9 |  |

## 10. Difficulty pips

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `diff.easy` | quest_icons | 157 | 2 | 16 | 6 |  |
| `diff.normal` | quest_icons | 175 | 2 | 16 | 6 |  |
| `diff.hard` | quest_icons | 193 | 2 | 16 | 6 |  |

## 11. Reward & claim

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `choice.frame` | quest_gui | 188 | 2 | 60 | 40 |  |
| `reward.tray` | quest_gui | 2 | 158 | 80 | 24 |  |
| `choice.option.normal` | quest_gui | 84 | 158 | 24 | 24 |  |
| `choice.option.selected` | quest_gui | 110 | 158 | 24 | 24 |  |

## 12. HUD tracker

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `hud.bg` | quest_gui | 66 | 2 | 120 | 40 |  |
| `hud.bar.track` | quest_gui2 | 108 | 76 | 100 | 4 |  |
| `hud.bar.fill` | quest_gui2 | 2 | 84 | 100 | 4 |  |

## 13. Toast

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `toast.bg` | quest_gui | 2 | 124 | 160 | 32 |  |
| `toast.accent.complete` | quest_gui | 164 | 124 | 4 | 32 |  |
| `toast.accent.available` | quest_gui | 170 | 124 | 4 | 32 |  |

## 14. Empty state

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `empty.art` | quest_gui | 16 | 2 | 48 | 48 |  |
