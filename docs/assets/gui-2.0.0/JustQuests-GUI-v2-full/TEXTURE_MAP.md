# JustQuests v2 FULL - TEXTURE_MAP

Pure vanilla-grey, layered. Classic `blit(x,y,u,v,w,h)`; coords are pixels within each 256x256 atlas. No AA, >=2px gutters, palette-only (+ documented tooltip exception). Also delivered un-packed under `background/ interactive/<el>/<state> icons/<set> overlay/ hud/ anim/`.

Atlases: `atlas/quest_full.png`, `atlas/quest_full_2.png`, `atlas/quest_full_3.png`, `atlas/quest_full_4.png`, `atlas/quest_full_5.png`, `atlas/quest_full_6.png`, `atlas/quest_full_7.png`

Themes: `themes/{standard,dark,highcontrast}/`  |  HD: `2x/atlas/`

In-mod path (reference): `assets/justquests/textures/gui/`

## 1. Background (sample 248x184)

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `window` | quest_full | 2 | 2 | 248 | 184 |

## 1. Background

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `detail_pane` | quest_full_2 | 2 | 2 | 141 | 158 |
| `list_pane` | quest_full_4 | 2 | 2 | 84 | 136 |
| `divider_v` | quest_full_4 | 98 | 2 | 2 | 120 |
| `titlebar` | quest_full_6 | 2 | 62 | 240 | 16 |
| `window.tl` | quest_full_7 | 202 | 76 | 8 | 8 |
| `window.tr` | quest_full_7 | 212 | 76 | 8 | 8 |
| `window.bl` | quest_full_7 | 222 | 76 | 8 | 8 |
| `window.br` | quest_full_7 | 232 | 76 | 8 | 8 |
| `window.center` | quest_full_7 | 242 | 76 | 8 | 8 |
| `window.left` | quest_full_7 | 172 | 88 | 3 | 8 |
| `window.right` | quest_full_7 | 177 | 88 | 3 | 8 |
| `window.top` | quest_full_7 | 228 | 114 | 8 | 3 |
| `window.bottom` | quest_full_7 | 238 | 114 | 8 | 3 |
| `divider_h` | quest_full_7 | 2 | 122 | 120 | 2 |

## 1. Background (compact)

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `window_compact` | quest_full_3 | 2 | 2 | 200 | 150 |

## 4.2 Navigation - tab

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `tab.normal` | quest_full_5 | 224 | 128 | 20 | 20 |
| `tab.hover` | quest_full_5 | 2 | 150 | 20 | 20 |
| `tab.pressed` | quest_full_5 | 24 | 150 | 20 | 20 |
| `tab.disabled` | quest_full_5 | 46 | 150 | 20 | 20 |
| `tab.selected` | quest_full_5 | 68 | 150 | 20 | 20 |
| `tab.focused` | quest_full_5 | 90 | 150 | 20 | 20 |

## 4.2 Navigation - tab (side)

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `tab_side.normal` | quest_full_5 | 112 | 150 | 20 | 20 |
| `tab_side.selected` | quest_full_5 | 134 | 150 | 20 | 20 |

## 4.2 Navigation - sort

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `sort_button.normal` | quest_full_6 | 83 | 116 | 16 | 16 |
| `sort_button.hover` | quest_full_6 | 101 | 116 | 16 | 16 |
| `sort_button.pressed` | quest_full_6 | 119 | 116 | 16 | 16 |
| `sort_button.disabled` | quest_full_6 | 137 | 116 | 16 | 16 |
| `sort_dir.asc` | quest_full_7 | 228 | 64 | 10 | 10 |
| `sort_dir.desc` | quest_full_7 | 240 | 64 | 10 | 10 |

## 4.2 Navigation - filter

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `filter_button.normal` | quest_full_6 | 155 | 116 | 16 | 16 |
| `filter_button.hover` | quest_full_6 | 173 | 116 | 16 | 16 |
| `filter_button.pressed` | quest_full_6 | 191 | 116 | 16 | 16 |
| `filter_button.disabled` | quest_full_6 | 209 | 116 | 16 | 16 |

## 4.2 Navigation - tab overflow

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `tab_overflow.prev` | quest_full_6 | 128 | 206 | 12 | 16 |
| `tab_overflow.next` | quest_full_6 | 142 | 206 | 12 | 16 |

## 4.2 Navigation - search

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `search_field.normal` | quest_full_6 | 2 | 224 | 100 | 14 |
| `search_field.focused` | quest_full_6 | 104 | 224 | 100 | 14 |
| `search_field.placeholder` | quest_full_6 | 2 | 240 | 100 | 14 |
| `search_field.clear_x` | quest_full_7 | 216 | 64 | 10 | 10 |

## 4.2 Navigation - filter dropdown

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `filter_dropdown.collapsed` | quest_full_6 | 104 | 240 | 80 | 14 |
| `filter_dropdown.expanded` | quest_full_7 | 2 | 2 | 80 | 14 |
| `filter_dropdown.row` | quest_full_7 | 84 | 2 | 80 | 14 |
| `filter_dropdown.row_hover` | quest_full_7 | 166 | 2 | 80 | 14 |
| `filter_dropdown.check_on` | quest_full_7 | 164 | 34 | 12 | 12 |
| `filter_dropdown.check_off` | quest_full_7 | 178 | 34 | 12 | 12 |

## 4.2 Navigation - page

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `page_prev.normal` | quest_full_7 | 192 | 34 | 12 | 12 |
| `page_next.normal` | quest_full_7 | 206 | 34 | 12 | 12 |
| `page_prev.hover` | quest_full_7 | 220 | 34 | 12 | 12 |
| `page_next.hover` | quest_full_7 | 234 | 34 | 12 | 12 |
| `page_prev.pressed` | quest_full_7 | 2 | 50 | 12 | 12 |
| `page_next.pressed` | quest_full_7 | 16 | 50 | 12 | 12 |
| `page_prev.disabled` | quest_full_7 | 30 | 50 | 12 | 12 |
| `page_next.disabled` | quest_full_7 | 44 | 50 | 12 | 12 |

## 4.2 Navigation - page dots

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `page_dots.active` | quest_full_7 | 150 | 114 | 6 | 6 |
| `page_dots.inactive` | quest_full_7 | 158 | 114 | 6 | 6 |

## 4.3 Quest list - row

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `quest_row.available` | quest_full_5 | 46 | 172 | 80 | 18 |
| `quest_row.hover` | quest_full_5 | 128 | 172 | 80 | 18 |
| `quest_row.selected` | quest_full_5 | 2 | 194 | 80 | 18 |
| `quest_row.active` | quest_full_5 | 84 | 194 | 80 | 18 |
| `quest_row.completed` | quest_full_5 | 166 | 194 | 80 | 18 |
| `quest_row.locked` | quest_full_5 | 2 | 214 | 80 | 18 |
| `quest_row.claimable` | quest_full_5 | 84 | 214 | 80 | 18 |
| `quest_row.new` | quest_full_5 | 166 | 214 | 80 | 18 |
| `quest_row.favorited` | quest_full_5 | 2 | 234 | 80 | 18 |
| `quest_row.pinned` | quest_full_5 | 84 | 234 | 80 | 18 |
| `quest_row.expired` | quest_full_5 | 166 | 234 | 80 | 18 |
| `quest_row.in_progress` | quest_full_6 | 2 | 2 | 80 | 18 |

## 4.3 Quest list - group header

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `group_header.normal` | quest_full_7 | 82 | 34 | 80 | 12 |

## 4.3 Quest list - inline progress

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `row_progress_inline.normal` | quest_full_7 | 124 | 122 | 80 | 2 |

## 4.3 Quest list - divider

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `row_divider.normal` | quest_full_7 | 2 | 126 | 80 | 1 |

## 4.4 Detail - buttons

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `button_claim.normal` | quest_full_5 | 170 | 52 | 72 | 20 |
| `button_claim.hover` | quest_full_5 | 2 | 84 | 72 | 20 |
| `button_claim.pressed` | quest_full_5 | 76 | 84 | 72 | 20 |
| `button_claim.disabled` | quest_full_5 | 150 | 84 | 72 | 20 |
| `button_claim.focused` | quest_full_5 | 2 | 106 | 72 | 20 |
| `button_abandon.normal` | quest_full_5 | 76 | 106 | 72 | 20 |
| `button_abandon.hover` | quest_full_5 | 150 | 106 | 72 | 20 |
| `button_abandon.pressed` | quest_full_5 | 2 | 128 | 72 | 20 |
| `button_abandon.disabled` | quest_full_5 | 76 | 128 | 72 | 20 |
| `button_abandon.focused` | quest_full_5 | 150 | 128 | 72 | 20 |
| `button_track.normal` | quest_full_6 | 2 | 98 | 60 | 16 |
| `button_track.hover` | quest_full_6 | 64 | 98 | 60 | 16 |
| `button_track.pressed` | quest_full_6 | 126 | 98 | 60 | 16 |
| `button_track.disabled` | quest_full_6 | 188 | 98 | 60 | 16 |
| `button_track.focused` | quest_full_6 | 2 | 116 | 60 | 16 |
| `button_back.normal` | quest_full_7 | 2 | 18 | 14 | 14 |
| `button_back.hover` | quest_full_7 | 18 | 18 | 14 | 14 |
| `button_back.pressed` | quest_full_7 | 34 | 18 | 14 | 14 |
| `button_back.disabled` | quest_full_7 | 50 | 18 | 14 | 14 |
| `button_back.focused` | quest_full_7 | 66 | 18 | 14 | 14 |
| `button_settings.normal` | quest_full_7 | 82 | 18 | 14 | 14 |
| `button_settings.hover` | quest_full_7 | 98 | 18 | 14 | 14 |
| `button_settings.pressed` | quest_full_7 | 114 | 18 | 14 | 14 |
| `button_settings.disabled` | quest_full_7 | 130 | 18 | 14 | 14 |
| `button_settings.focused` | quest_full_7 | 146 | 18 | 14 | 14 |
| `button_help.normal` | quest_full_7 | 162 | 18 | 14 | 14 |
| `button_help.hover` | quest_full_7 | 178 | 18 | 14 | 14 |
| `button_help.pressed` | quest_full_7 | 194 | 18 | 14 | 14 |
| `button_help.disabled` | quest_full_7 | 210 | 18 | 14 | 14 |
| `button_help.focused` | quest_full_7 | 226 | 18 | 14 | 14 |
| `button_info.normal` | quest_full_7 | 2 | 34 | 14 | 14 |
| `button_info.hover` | quest_full_7 | 18 | 34 | 14 | 14 |
| `button_info.pressed` | quest_full_7 | 34 | 34 | 14 | 14 |
| `button_info.disabled` | quest_full_7 | 50 | 34 | 14 | 14 |
| `button_info.focused` | quest_full_7 | 66 | 34 | 14 | 14 |
| `button_close.normal` | quest_full_7 | 152 | 50 | 11 | 11 |
| `button_close.hover` | quest_full_7 | 165 | 50 | 11 | 11 |
| `button_close.pressed` | quest_full_7 | 178 | 50 | 11 | 11 |
| `button_close.disabled` | quest_full_7 | 191 | 50 | 11 | 11 |
| `button_close.focused` | quest_full_7 | 204 | 50 | 11 | 11 |

## 4.4 Detail - reward slot

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `reward_slot.normal` | quest_full_5 | 156 | 150 | 20 | 20 |
| `reward_slot.hover` | quest_full_5 | 178 | 150 | 20 | 20 |
| `reward_slot.claimed` | quest_full_5 | 200 | 150 | 20 | 20 |
| `reward_slot.choice` | quest_full_5 | 222 | 150 | 20 | 20 |
| `reward_slot.locked` | quest_full_5 | 2 | 172 | 20 | 20 |
| `reward_slot.focused` | quest_full_5 | 24 | 172 | 20 | 20 |

## 4.4 Detail - icon frame

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `icon_frame.normal` | quest_full_6 | 54 | 42 | 18 | 18 |
| `icon_frame.selected` | quest_full_6 | 74 | 42 | 18 | 18 |
| `icon_frame.empty` | quest_full_6 | 94 | 42 | 18 | 18 |

## 4.4 Detail - objective row

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `objective_row.incomplete` | quest_full_6 | 2 | 80 | 120 | 16 |
| `objective_row.complete` | quest_full_6 | 124 | 80 | 120 | 16 |

## 4.4 Detail - badges

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `badge.repeatable` | quest_full_6 | 227 | 116 | 16 | 16 |
| `badge.cooldown` | quest_full_6 | 2 | 134 | 16 | 16 |
| `badge.daily` | quest_full_6 | 20 | 134 | 16 | 16 |
| `badge.new` | quest_full_6 | 38 | 134 | 16 | 16 |

## 4.4 Detail - progress bar

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `progress_bar.large` | quest_full_7 | 114 | 64 | 100 | 10 |
| `progress_bar.track` | quest_full_7 | 2 | 98 | 100 | 6 |
| `progress_bar.fill` | quest_full_7 | 104 | 98 | 100 | 6 |
| `progress_bar.segmented` | quest_full_7 | 2 | 106 | 100 | 6 |
| `progress_bar.ready` | quest_full_7 | 104 | 106 | 100 | 6 |
| `progress_bar.mini` | quest_full_7 | 166 | 114 | 60 | 4 |

## 4.4 Detail - difficulty

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `difficulty_pips.easy` | quest_full_7 | 84 | 114 | 20 | 6 |
| `difficulty_pips.normal` | quest_full_7 | 106 | 114 | 20 | 6 |
| `difficulty_pips.hard` | quest_full_7 | 128 | 114 | 20 | 6 |

## 4.5 Scroll V

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `scroll_track.normal` | quest_full_4 | 88 | 2 | 8 | 120 |
| `scroll_handle.normal` | quest_full_6 | 156 | 206 | 8 | 16 |
| `scroll_handle.hover` | quest_full_6 | 166 | 206 | 8 | 16 |
| `scroll_handle.grabbed` | quest_full_6 | 176 | 206 | 8 | 16 |
| `scroll_arrow_up.normal` | quest_full_7 | 2 | 88 | 8 | 8 |
| `scroll_arrow_down.normal` | quest_full_7 | 12 | 88 | 8 | 8 |
| `scroll_arrow_up.hover` | quest_full_7 | 42 | 88 | 8 | 8 |
| `scroll_arrow_down.hover` | quest_full_7 | 52 | 88 | 8 | 8 |
| `scroll_arrow_up.pressed` | quest_full_7 | 82 | 88 | 8 | 8 |
| `scroll_arrow_down.pressed` | quest_full_7 | 92 | 88 | 8 | 8 |
| `scroll_arrow_up.disabled` | quest_full_7 | 122 | 88 | 8 | 8 |
| `scroll_arrow_down.disabled` | quest_full_7 | 132 | 88 | 8 | 8 |

## 4.5 Scroll H

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `scroll_track_h.normal` | quest_full_7 | 26 | 76 | 120 | 8 |
| `scroll_handle_h.normal` | quest_full_7 | 148 | 76 | 16 | 8 |
| `scroll_handle_h.hover` | quest_full_7 | 166 | 76 | 16 | 8 |
| `scroll_handle_h.grabbed` | quest_full_7 | 184 | 76 | 16 | 8 |
| `scroll_arrow_left.normal` | quest_full_7 | 22 | 88 | 8 | 8 |
| `scroll_arrow_right.normal` | quest_full_7 | 32 | 88 | 8 | 8 |
| `scroll_arrow_left.hover` | quest_full_7 | 62 | 88 | 8 | 8 |
| `scroll_arrow_right.hover` | quest_full_7 | 72 | 88 | 8 | 8 |
| `scroll_arrow_left.pressed` | quest_full_7 | 102 | 88 | 8 | 8 |
| `scroll_arrow_right.pressed` | quest_full_7 | 112 | 88 | 8 | 8 |
| `scroll_arrow_left.disabled` | quest_full_7 | 142 | 88 | 8 | 8 |
| `scroll_arrow_right.disabled` | quest_full_7 | 152 | 88 | 8 | 8 |

## 4.7 Misc - focus ring

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `focus_ring.normal` | quest_full_5 | 144 | 52 | 24 | 24 |

## 4.7 Misc - checkbox

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `checkbox.unchecked` | quest_full_7 | 58 | 50 | 12 | 12 |
| `checkbox.checked` | quest_full_7 | 72 | 50 | 12 | 12 |
| `checkbox.hover` | quest_full_7 | 86 | 50 | 12 | 12 |
| `checkbox.disabled` | quest_full_7 | 100 | 50 | 12 | 12 |

## 4.7 Misc - radio

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `radio.off` | quest_full_7 | 114 | 50 | 12 | 12 |
| `radio.on` | quest_full_7 | 128 | 50 | 12 | 12 |

## 4.7 Misc - slider

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `slider.handle` | quest_full_7 | 142 | 50 | 8 | 12 |
| `slider.track` | quest_full_7 | 2 | 114 | 80 | 6 |

## 4.7 Misc - corner grip

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `corner_grip.normal` | quest_full_7 | 162 | 88 | 8 | 8 |

## 5. Icons - reward

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `reward.message` | quest_full_6 | 64 | 116 | 17 | 16 |
| `reward.give_item` | quest_full_6 | 2 | 188 | 16 | 16 |
| `reward.loot_table` | quest_full_6 | 20 | 188 | 16 | 16 |
| `reward.xp` | quest_full_6 | 38 | 188 | 16 | 16 |
| `reward.effect` | quest_full_6 | 56 | 188 | 16 | 16 |
| `reward.command` | quest_full_6 | 74 | 188 | 16 | 16 |

## 5. Icons - objective

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `objective.collect_item` | quest_full_6 | 56 | 134 | 16 | 16 |
| `objective.collect_item__done` | quest_full_6 | 74 | 134 | 16 | 16 |
| `objective.collect_item__locked` | quest_full_6 | 92 | 134 | 16 | 16 |
| `objective.mine_block` | quest_full_6 | 110 | 134 | 16 | 16 |
| `objective.mine_block__done` | quest_full_6 | 128 | 134 | 16 | 16 |
| `objective.mine_block__locked` | quest_full_6 | 146 | 134 | 16 | 16 |
| `objective.craft_item` | quest_full_6 | 164 | 134 | 16 | 16 |
| `objective.craft_item__done` | quest_full_6 | 182 | 134 | 16 | 16 |
| `objective.craft_item__locked` | quest_full_6 | 200 | 134 | 16 | 16 |
| `objective.smelt_item` | quest_full_6 | 218 | 134 | 16 | 16 |
| `objective.smelt_item__done` | quest_full_6 | 236 | 134 | 16 | 16 |
| `objective.smelt_item__locked` | quest_full_6 | 2 | 152 | 16 | 16 |
| `objective.consume_item` | quest_full_6 | 20 | 152 | 16 | 16 |
| `objective.consume_item__done` | quest_full_6 | 38 | 152 | 16 | 16 |
| `objective.consume_item__locked` | quest_full_6 | 56 | 152 | 16 | 16 |
| `objective.place_block` | quest_full_6 | 74 | 152 | 16 | 16 |
| `objective.place_block__done` | quest_full_6 | 92 | 152 | 16 | 16 |
| `objective.place_block__locked` | quest_full_6 | 110 | 152 | 16 | 16 |
| `objective.kill_mob` | quest_full_6 | 128 | 152 | 16 | 16 |
| `objective.kill_mob__done` | quest_full_6 | 146 | 152 | 16 | 16 |
| `objective.kill_mob__locked` | quest_full_6 | 164 | 152 | 16 | 16 |
| `objective.tame_animal` | quest_full_6 | 182 | 152 | 16 | 16 |
| `objective.tame_animal__done` | quest_full_6 | 200 | 152 | 16 | 16 |
| `objective.tame_animal__locked` | quest_full_6 | 218 | 152 | 16 | 16 |
| `objective.breed_animal` | quest_full_6 | 236 | 152 | 16 | 16 |
| `objective.breed_animal__done` | quest_full_6 | 2 | 170 | 16 | 16 |
| `objective.breed_animal__locked` | quest_full_6 | 20 | 170 | 16 | 16 |
| `objective.gain_advancement` | quest_full_6 | 38 | 170 | 16 | 16 |
| `objective.gain_advancement__done` | quest_full_6 | 56 | 170 | 16 | 16 |
| `objective.gain_advancement__locked` | quest_full_6 | 74 | 170 | 16 | 16 |
| `objective.visit_dimension` | quest_full_6 | 92 | 170 | 16 | 16 |
| `objective.visit_dimension__done` | quest_full_6 | 110 | 170 | 16 | 16 |
| `objective.visit_dimension__locked` | quest_full_6 | 128 | 170 | 16 | 16 |
| `objective.reach_level` | quest_full_6 | 146 | 170 | 16 | 16 |
| `objective.reach_level__done` | quest_full_6 | 164 | 170 | 16 | 16 |
| `objective.reach_level__locked` | quest_full_6 | 182 | 170 | 16 | 16 |
| `objective.reach_location` | quest_full_6 | 200 | 170 | 16 | 16 |
| `objective.reach_location__done` | quest_full_6 | 218 | 170 | 16 | 16 |
| `objective.reach_location__locked` | quest_full_6 | 236 | 170 | 16 | 16 |

## 5. Icons - category

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `category.gathering` | quest_full_6 | 92 | 188 | 16 | 16 |
| `category.farming` | quest_full_6 | 110 | 188 | 16 | 16 |
| `category.combat` | quest_full_6 | 128 | 188 | 16 | 16 |
| `category.survival` | quest_full_6 | 146 | 188 | 16 | 16 |
| `category.daily` | quest_full_6 | 164 | 188 | 16 | 16 |
| `category.custom` | quest_full_6 | 182 | 188 | 16 | 16 |

## 5. Icons - glyphs

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `glyph.check` | quest_full_6 | 200 | 188 | 16 | 16 |
| `glyph.lock` | quest_full_6 | 218 | 188 | 16 | 16 |
| `glyph.repeat` | quest_full_6 | 236 | 188 | 16 | 16 |
| `glyph.star` | quest_full_6 | 2 | 206 | 16 | 16 |
| `glyph.new_dot` | quest_full_6 | 20 | 206 | 16 | 16 |
| `glyph.clock` | quest_full_6 | 38 | 206 | 16 | 16 |
| `glyph.xp` | quest_full_6 | 56 | 206 | 16 | 16 |
| `glyph.exclamation` | quest_full_6 | 74 | 206 | 16 | 16 |

## 6. Overlay - dialog

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `dialog` | quest_full_4 | 2 | 140 | 160 | 80 |
| `dialog_button_yes.normal` | quest_full_6 | 84 | 2 | 50 | 18 |
| `dialog_button_no.normal` | quest_full_6 | 136 | 2 | 50 | 18 |
| `dialog_button_yes.hover` | quest_full_6 | 188 | 2 | 50 | 18 |
| `dialog_button_no.hover` | quest_full_6 | 2 | 22 | 50 | 18 |
| `dialog_button_yes.pressed` | quest_full_6 | 54 | 22 | 50 | 18 |
| `dialog_button_no.pressed` | quest_full_6 | 106 | 22 | 50 | 18 |
| `dialog_button_yes.disabled` | quest_full_6 | 158 | 22 | 50 | 18 |
| `dialog_button_no.disabled` | quest_full_6 | 2 | 42 | 50 | 18 |

## 6. Overlay - empty state

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `empty_state` | quest_full_5 | 124 | 2 | 48 | 48 |

## 6. Overlay - toast

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `toast` | quest_full_5 | 2 | 52 | 140 | 30 |

## 6. Overlay - tooltip

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `tooltip_vanilla` | quest_full_6 | 92 | 206 | 16 | 16 |
| `tooltip_grey` | quest_full_6 | 110 | 206 | 16 | 16 |

## 12. HUD

| name | atlas | u | v | w | h |
|------|-------|---|---|---|---|
| `hud_panel` | quest_full_5 | 2 | 2 | 120 | 48 |
| `hud_row` | quest_full_7 | 2 | 64 | 110 | 10 |
| `hud_pin` | quest_full_7 | 2 | 76 | 10 | 10 |
| `hud_unpin` | quest_full_7 | 14 | 76 | 10 | 10 |

## Standalone (not in atlas - use the file directly)

| name | file | w | h |
|------|------|---|---|
| `window_wide` | background/window_wide.png | 280 | 184 |
| `toast_slide` | anim/toast_slide.png | 560 | 30 |
| `tab_switch` | anim/tab_switch.png | 40 | 20 |
| `claimable_pulse` | anim/claimable_pulse.png | 240 | 18 |
| `loading_spinner` | anim/loading_spinner.png | 128 | 16 |
| `progress_fill_shimmer` | anim/progress_fill_shimmer.png | 200 | 6 |
