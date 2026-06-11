# JustQuests

A lightweight, datapack-driven quest book for NeoForge. No GUI bloat, no complex dependencies — just JSON quests and four commands.

[![Modrinth](https://img.shields.io/badge/Modrinth-JustQuests-00AF5C?logo=modrinth)](https://modrinth.com/mod/justquests)
[![License: LGPL-3.0](https://img.shields.io/badge/License-LGPL--3.0-blue.svg)](LICENSE)

## What it does

JustQuests lets pack makers define quests in plain JSON files inside any datapack. Players accept quests, gather the required items, and receive rewards — all through simple chat commands. Built as a focused alternative to FTB Quests and HQM for packs that want questing without the weight.

## Features (v0.1)

- **Datapack-driven** — Define quests in `data/<namespace>/justquests/quests/<id>.json`. Reloads with `/reload`.
- **Item collection objectives** — Track how many of an item the player has gathered.
- **Item rewards** — Hand out stacks on completion. Drops at player's feet if inventory is full.
- **Per-player progress** — Stored persistently, survives logout and server restart.
- **Server-side friendly** — Works fine on dedicated servers; clients don't need the mod for basic play.
- **No GUI dependency** — Everything runs through commands, so no UI conflicts with other mods.

## Commands

| Command | Description |
|---------|-------------|
| `/quest list` | Show all available quests |
| `/quest accept <id>` | Start a quest |
| `/quest progress` | Show your active quests and progress |
| `/quest abandon <id>` | Drop an active quest (no reward) |
| `/quest reload` | OP-only: reload quest definitions |

## Example quest

```json
{
  "title": "First Steps",
  "description": "Gather some basic materials to get started.",
  "objectives": [
    {
      "type": "justquests:collect_item",
      "item": "minecraft:oak_log",
      "count": 16
    }
  ],
  "rewards": [
    {
      "type": "justquests:give_item",
      "item": "minecraft:bread",
      "count": 4
    }
  ]
}
```

Drop this in `data/yourpack/justquests/quests/first_steps.json` and it's live after `/reload`.

> **Design note:** Progress counts items *picked up from the ground* — mined blocks, crop drops, mob loot. Crafted items go straight into the inventory and do not count. Design your quests around gathering, not crafting.

## Bundled starter quests

The mod ships with a 10-quest starter line you can play out of the box (or override via datapack):

| Quest ID | Goal | Reward |
|----------|------|--------|
| `justquests:first_steps` | 16 oak logs | 4 bread |
| `justquests:stone_age` | 32 cobblestone | 16 torches, stone pickaxe |
| `justquests:fuel_for_the_fire` | 8 coal | 32 torches |
| `justquests:iron_harvest` | 5 raw iron | iron pickaxe, 8 bread |
| `justquests:green_thumb` | 32 wheat | 4 golden carrots, 16 bone meal |
| `justquests:monster_hunter` | 8 rotten flesh + 8 bones + 8 string | bow, 32 arrows |
| `justquests:shine_bright` | 3 diamonds | 8 XP bottles, golden apple |
| `justquests:deep_down` | 32 cobbled deepslate | 8 lanterns, golden apple |
| `justquests:hot_stuff` | 4 blaze rods | 8 ender pearls, 4 magma cream |
| `justquests:ender_seeker` | 8 ender pearls | 12 obsidian, 2 diamonds |

## Building from source

Requirements: JDK 21, Git.

```bash
git clone https://github.com/ErikEdits/JustQuests.git
cd JustQuests
./gradlew build
```

The compiled `.jar` ends up in `build/libs/`.

## Running a dev client

```bash
./gradlew runClient
```

Test quests can be placed in `run/saves/<world>/datapacks/<your-pack>/data/<namespace>/justquests/quests/`.

## Project structure

```
src/main/
├── java/com/erikedits/justquests/
│   ├── JustQuests.java              # Mod entry point
│   ├── data/                        # Quest definitions + codec
│   ├── player/                      # Per-player progress (AttachmentType)
│   └── commands/                    # /quest command tree
└── resources/
    └── META-INF/neoforge.mods.toml
```

## What it does *not* do (yet)

JustQuests is intentionally minimal in v0.1. The following are planned but not included:

- No in-game GUI (commands only)
- No quest chains or prerequisites
- No mob-kill, block-place, or advancement-based objectives
- No XP, command, or loot-table rewards
- No Fabric support

If you need any of those today, use [FTB Quests](https://modrinth.com/mod/ftb-quests) instead.

## Roadmap

- **v0.2** — GUI, more objective types in this order: kill mob, place block, reach location, craft item; XP/command rewards
- **v0.3** — Quest chains and prerequisites
- **v1.0** — Fabric port

## Compatibility

- Minecraft 1.21.x
- NeoForge only (for now)
- Works on dedicated servers and singleplayer

## Contributing

Bug reports and pull requests welcome. Please keep PRs scoped — see the roadmap for what fits the current direction.

## License

[LGPL-3.0](LICENSE) — free to use in any modpack, no permission needed.
Modified versions and forks must stay open source under the LGPL.
