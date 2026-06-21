# JustQuests — example datapack

A ready-to-use datapack with **one example quest for every objective and
reward type**, plus item tags, `mode: any`, a prerequisite chain, a
repeatable quest, and a multi-language quest. Use it as a copy-paste
reference for writing your own quests.

## How to use

1. Copy this whole `example-datapack` folder into your world's
   `datapacks/` folder (so you have `…/<world>/datapacks/example-datapack/pack.mcmeta`).
2. In game, run `/reload`.
3. Run `/quest list examples` to see them (they're all in the `examples`
   category). Run `/quest accept justquests:ex_collect_item`, etc.

> You can also just open the `.json` files and copy bits into your own
> `<world>/justquests/custom-quests.json` — same format.

## What's inside (`data/justquests/justquests/quests/`)

Objectives (one each):
`ex_collect_item`, `ex_mine_block`, `ex_craft_item`, `ex_smelt_item`,
`ex_consume_item`, `ex_place_block`, `ex_kill_mob`, `ex_tame_animal`,
`ex_breed_animal`, `ex_gain_advancement`, `ex_visit_dimension`,
`ex_reach_level`, `ex_reach_location`.

Features:
- `ex_item_tag` — item field as a `#tag`.
- `ex_mode_any` — finish on **any** objective (`"mode": "any"`).
- `ex_chain_1` + `ex_chain_2` — `requires` (a locked chain).
- `ex_repeatable` — `repeatable` + `cooldown_hours`.
- `ex_multilang` — `title`/`description` as a per-language map.

Reward types shown across the examples: `give_item`, `xp`, `effect`,
`message`, `loot_table`, and `command` (`ex_reach_location` runs
`give {player} minecraft:cookie 1`).

## Notes
- `pack.mcmeta` declares a pack format range covering MC 1.21–1.21.5. If
  your version warns about the format, bump `pack_format` to match.
- `ex_reach_location` uses example coordinates (0, 64, 0) in the overworld —
  change them to somewhere meaningful in your world.
- Full field reference: https://github.com/ErikEdits/JustQuests
