# JustQuests v2 - TEXTURE_MAP

Pure vanilla-grey, layered set. Coordinates are pixels within each 256x256 atlas (origin top-left), for classic `blit(x,y,u,v,w,h)`. No AA, >=2px gutters, palette-only.

Atlases: `atlas/quest_gui_v2.png`, `atlas/quest_gui_v2_2.png`, `atlas/quest_gui_v2_3.png`

Also delivered un-packed: `background/<piece>.png`, `interactive/<element>/<state>.png`, `strips/<element>.png`.

In-mod path (reference): `assets/justquests/textures/gui/`

## 1. Background

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `window` | quest_gui_v2 | 2 | 2 | 248 | 184 | main 248x184 panel, 9-slice friendly |
| `titlebar` | quest_gui_v2 | 2 | 188 | 240 | 16 | title strip (game draws 'Quests') |
| `list_pane` | quest_gui_v2_2 | 2 | 2 | 84 | 136 | left list inset |
| `detail_pane` | quest_gui_v2_2 | 88 | 2 | 141 | 158 | right detail inset |

## 2. Interactive / button_back

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `button_back.normal` | quest_gui_v2_3 | 28 | 146 | 12 | 12 | state: normal |
| `button_back.hover` | quest_gui_v2_3 | 42 | 146 | 12 | 12 | state: hover |
| `button_back.pressed` | quest_gui_v2_3 | 56 | 146 | 12 | 12 | state: pressed |
| `button_back.disabled` | quest_gui_v2_3 | 70 | 146 | 12 | 12 | state: disabled |

## 2. Interactive / button_claim

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `button_claim.normal` | quest_gui_v2_3 | 122 | 2 | 72 | 20 | state: normal |
| `button_claim.hover` | quest_gui_v2_3 | 2 | 124 | 72 | 20 | state: hover |
| `button_claim.pressed` | quest_gui_v2_3 | 76 | 124 | 72 | 20 | state: pressed |
| `button_claim.disabled` | quest_gui_v2_3 | 150 | 124 | 72 | 20 | state: disabled |

## 2. Interactive / button_close

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `button_close.normal` | quest_gui_v2_3 | 224 | 124 | 11 | 11 | state: normal |
| `button_close.hover` | quest_gui_v2_3 | 237 | 124 | 11 | 11 | state: hover |
| `button_close.pressed` | quest_gui_v2_3 | 2 | 146 | 11 | 11 | state: pressed |
| `button_close.disabled` | quest_gui_v2_3 | 15 | 146 | 11 | 11 | state: disabled |

## 2. Interactive / glyph_check

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `glyph_check` | quest_gui_v2_3 | 2 | 180 | 9 | 9 | state: glyph |

## 2. Interactive / glyph_lock

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `glyph_lock` | quest_gui_v2_3 | 13 | 180 | 9 | 9 | state: glyph |

## 2. Interactive / glyph_repeat

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `glyph_repeat` | quest_gui_v2_3 | 24 | 180 | 9 | 9 | state: glyph |

## 2. Interactive / icon_frame

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `icon_frame.normal` | quest_gui_v2_3 | 206 | 160 | 18 | 18 | state: normal |
| `icon_frame.selected` | quest_gui_v2_3 | 226 | 160 | 18 | 18 | state: selected |

## 2. Interactive / page_next

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `page_next.normal` | quest_gui_v2_3 | 140 | 146 | 12 | 12 | state: normal |
| `page_next.hover` | quest_gui_v2_3 | 154 | 146 | 12 | 12 | state: hover |
| `page_next.pressed` | quest_gui_v2_3 | 168 | 146 | 12 | 12 | state: pressed |
| `page_next.disabled` | quest_gui_v2_3 | 182 | 146 | 12 | 12 | state: disabled |

## 2. Interactive / page_prev

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `page_prev.normal` | quest_gui_v2_3 | 84 | 146 | 12 | 12 | state: normal |
| `page_prev.hover` | quest_gui_v2_3 | 98 | 146 | 12 | 12 | state: hover |
| `page_prev.pressed` | quest_gui_v2_3 | 112 | 146 | 12 | 12 | state: pressed |
| `page_prev.disabled` | quest_gui_v2_3 | 126 | 146 | 12 | 12 | state: disabled |

## 2. Interactive / progress_bar

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `progress_bar.track` | quest_gui_v2_3 | 2 | 160 | 100 | 6 | state: track |
| `progress_bar.fill` | quest_gui_v2_3 | 104 | 160 | 100 | 6 | state: fill |

## 2. Interactive / quest_row

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `quest_row.available` | quest_gui_v2_2 | 90 | 162 | 80 | 18 | state: available |
| `quest_row.active` | quest_gui_v2_2 | 172 | 162 | 80 | 18 | state: active |
| `quest_row.completed` | quest_gui_v2_2 | 2 | 184 | 80 | 18 | state: completed |
| `quest_row.locked` | quest_gui_v2_2 | 84 | 184 | 80 | 18 | state: locked |
| `quest_row.claimable` | quest_gui_v2_2 | 166 | 184 | 80 | 18 | state: claimable |
| `quest_row.hover` | quest_gui_v2_2 | 2 | 204 | 80 | 18 | state: hover |
| `quest_row.selected` | quest_gui_v2_2 | 84 | 204 | 80 | 18 | state: selected |

## 2. Interactive / scroll_arrow_down

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `scroll_arrow_down.normal` | quest_gui_v2_3 | 82 | 2 | 8 | 8 | state: normal |
| `scroll_arrow_down.hover` | quest_gui_v2_3 | 92 | 2 | 8 | 8 | state: hover |
| `scroll_arrow_down.pressed` | quest_gui_v2_3 | 102 | 2 | 8 | 8 | state: pressed |
| `scroll_arrow_down.disabled` | quest_gui_v2_3 | 112 | 2 | 8 | 8 | state: disabled |

## 2. Interactive / scroll_arrow_up

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `scroll_arrow_up.normal` | quest_gui_v2_3 | 42 | 2 | 8 | 8 | state: normal |
| `scroll_arrow_up.hover` | quest_gui_v2_3 | 52 | 2 | 8 | 8 | state: hover |
| `scroll_arrow_up.pressed` | quest_gui_v2_3 | 62 | 2 | 8 | 8 | state: pressed |
| `scroll_arrow_up.disabled` | quest_gui_v2_3 | 72 | 2 | 8 | 8 | state: disabled |

## 2. Interactive / scroll_handle

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `scroll_handle.normal` | quest_gui_v2_3 | 12 | 2 | 8 | 16 | state: normal |
| `scroll_handle.hover` | quest_gui_v2_3 | 22 | 2 | 8 | 16 | state: hover |
| `scroll_handle.grabbed` | quest_gui_v2_3 | 32 | 2 | 8 | 16 | state: grabbed |

## 2. Interactive / scroll_track

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `scroll_track.normal` | quest_gui_v2_3 | 2 | 2 | 8 | 120 | state: normal |

## 2. Interactive / tab

| name | atlas | u | v | w | h | notes |
|------|-------|---|---|---|---|-------|
| `tab.normal` | quest_gui_v2_2 | 231 | 2 | 20 | 20 | state: normal |
| `tab.hover` | quest_gui_v2_2 | 2 | 162 | 20 | 20 | state: hover |
| `tab.pressed` | quest_gui_v2_2 | 24 | 162 | 20 | 20 | state: pressed |
| `tab.disabled` | quest_gui_v2_2 | 46 | 162 | 20 | 20 | state: disabled |
| `tab.selected` | quest_gui_v2_2 | 68 | 162 | 20 | 20 | state: selected |
