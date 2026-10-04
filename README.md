# JustQuests

**A lightweight quest book for NeoForge, Fabric and Forge.** No GUI bloat, no heavy
dependencies — datapack-driven quests, an in-game quest book, a rotating board of generated
quests and a handful of commands. A focused, server-friendly alternative to FTB Quests and HQM.

[![Modrinth](https://img.shields.io/badge/Modrinth-JustQuests-00AF5C?logo=modrinth)](https://modrinth.com/mod/justquests)
[![Discord](https://img.shields.io/badge/Discord-community-5865F2?logo=discord&logoColor=white)](https://discord.gg/cMTGE9QCja)
[![License: LGPL-3.0](https://img.shields.io/badge/License-LGPL--3.0-blue.svg)](LICENSE)

Current version: **0.3.5** — see the [changelog](CHANGELOG.md).

## Features

- **Quest book** — press **J** to browse quests, follow progress bars and accept or abandon with a
  click (Minecraft 1.20.1 and newer). Grouped by category or by status, a pixel icon for every
  quest, a search field, and a stats page. Works in singleplayer and on servers.
- **Quest tracker** — your pinned (or newest) active quests and their goals in a corner of the
  screen; **H** turns it on or off.
- **Ready to play** — 25 built-in quests in English, German, French and Spanish. Server owners can
  hide them with `/quest mainquests off`.
- **Generated quests** — a fresh board every 12 hours (00:00 and 12:00 by default), sized to a
  sensible amount of play time and paid with fitting rewards: gather, mine, craft, hunt, enchant,
  throw, brew and more. Supports content from
  **Farmer's Delight, Create, Mekanism, The Twilight Forest and Botania** when installed, three
  difficulty levels, and Nether/End quests once players have been there.
- **First come, first served** — on servers a generated quest someone accepted is taken; the book
  shows *Taken by Steve* or *Reserved for you*.
- **Write your own quests** — in a datapack or in a per-world `custom-quests.json` that reloads
  by itself while you edit it. 15 objective types, 7 reward types, item tags and item filters.
- **Speaks the player's language** — quest text can be a per-language map; item, block and mob
  names are translated by the game.
- **Quest logic** — prerequisites (`requires`), repeatable quests with cooldowns, "all" or "any"
  objectives, categories and sort order.
- **For server owners** — completion broadcast, stats and leaderboard, admin commands, a
  self-test (`/quest test`) and per-world `settings.json`.

## Supported versions

| Loader | Minecraft | Quest book |
|---|---|---|
| NeoForge | 1.20.4, 1.20.6, 1.21 – 1.21.11 | yes |
| Fabric (needs [Fabric API](https://modrinth.com/mod/fabric-api)) | 1.18.2, 1.19.2, 1.19.4, 1.20.1, 1.20.4, 1.20.6, 1.21 – 1.21.11 | 1.20.1 and newer |
| Forge | 1.18.2, 1.19.2, 1.19.4, 1.20.1 | 1.20.1 |

On 1.18 and 1.19 JustQuests is command-only; quests, tracking and rewards work the same.
Install the mod on the server **and** the clients (both are required).

## Installation

1. Install NeoForge, Fabric (+ Fabric API) or Forge for your Minecraft version.
2. Download the jar for **your loader and exact version** from
   [Modrinth](https://modrinth.com/mod/justquests) or the
   [GitHub releases](https://github.com/ErikEdits/JustQuests/releases) — the file name says which,
   e.g. `JustQuests-fabric-1.21.1-0.3.5.jar` — and put it into `mods/`.
3. Start the game, press **J** (or run `/quest list`).

## Commands

| Command | What it does |
|---|---|
| `/quest list [category]` | All quests, grouped by category (locked ones show what they need) |
| `/quest categories` | Categories and how many quests each has |
| `/quest accept <id>` / `/quest abandon <id>` | Start or drop a quest (tab completion) |
| `/quest progress` | Your active quests and how far along you are |
| `/quest claim [id]` | Take the rewards of a finished quest (without an id: all waiting rewards) |
| `/quest stats` / `/quest leaderboard` | Your stats / the server's top 10 |
| `/quest discord` | The community Discord invite |

Operator commands (permission level 2):

| Command | What it does |
|---|---|
| `/quest reload` | Reload custom quests and `settings.json` |
| `/quest mainquests on\|off` | Show or hide the built-in quests |
| `/quest difficulty [easy\|normal\|hard]` | Difficulty of generated quests |
| `/quest reroll` | New set of generated quests now |
| `/quest generator status\|stats\|preview [n]\|explain <id>\|release <id>` | Inspect the generator, preview the next board, free a taken quest |
| `/quest admin view\|reset\|complete <player> [id]` | Manage a player's progress |
| `/quest test` | Self-test, writes `justquests-diagnostics.log` |

## Writing quests

Put a quest file into a datapack at `data/<namespace>/justquests/quests/<id>.json` (then
`/reload`), or add it to `<world>/justquests/custom-quests.json`, which is created on first start
with an explained example and reloads automatically. A custom quest overrides a datapack quest
with the same id.

```json
{
  "title": { "en_us": "Sharp Start", "de_de": "Scharfer Start" },
  "description": "Mine some iron, then enchant a sword.",
  "category": "combat",
  "objectives": [
    { "type": "justquests:mine_block", "block": "minecraft:iron_ore", "count": 8 },
    { "type": "justquests:enchant_item", "item": "#minecraft:swords", "count": 1 }
  ],
  "rewards": [
    { "type": "justquests:give_item", "item": "minecraft:diamond", "count": 2 },
    { "type": "justquests:title", "title": "Well done!", "subtitle": "Sharp Start complete" }
  ]
}
```

**Quest fields:** `title` (required), `description`, `category` (default `datapack`), `sort`,
`mode` (`all` or `any`), `requires` (list of quest ids), `repeatable`, `cooldown_hours`,
`icon` (an item id for the quest book; taken from the first objective when left out),
`objectives`, `rewards`. `title` and `description` can be a string or a per-language map
(`{"en_us": "...", "de_de": "..."}`); players see their own language with English as fallback.

### Objectives

| Type | Fields | Counts when the player… |
|---|---|---|
| `justquests:collect_item` | `item`, `count` | picks the item up (mining, harvest, mob drops) |
| `justquests:craft_item` | `item`, `count` | crafts it (crafting table, inventory or stonecutter) |
| `justquests:smelt_item` | `item`, `count` | takes it out of a furnace |
| `justquests:consume_item` | `item`, `count` | eats or drinks it |
| `justquests:enchant_item` | `item` (optional), `count` | enchants an item at an enchanting table |
| `justquests:use_item` | `item`, `count` | uses it and it does something (throw, place, apply, mine…) |
| `justquests:mine_block` | `block`, `count` | breaks the block |
| `justquests:place_block` | `block`, `count` | places the block |
| `justquests:kill_mob` | `entity`, `count` | kills the mob |
| `justquests:tame_animal` | `entity`, `count` | tames the animal |
| `justquests:breed_animal` | `entity`, `count` | breeds the animal |
| `justquests:reach_level` | `level` | reaches the XP level |
| `justquests:reach_location` | `x`, `y`, `z`, `radius` (4), `dimension` (optional) | stands at the spot |
| `justquests:visit_dimension` | `dimension` | enters the dimension (vanilla or modded) |
| `justquests:gain_advancement` | `advancement` | earns the advancement |

`item` is an id (`minecraft:oak_log`), a tag (`#minecraft:logs`) or an object with filters:

```json
"item": { "id": "minecraft:potion", "potion": "minecraft:swiftness" }
"item": { "id": "#minecraft:swords", "enchantments": { "minecraft:sharpness": 2 }, "name": "Excalibur" }
```

`block` and `entity` take an id or a tag as well (`#minecraft:logs`, `#minecraft:skeletons`).

`enchantments` are minimum levels, `name` is an anvil name. The same JSON works on every version.
Filters are checked on the item the event is about, so they are most useful for `consume_item`,
`enchant_item` and `craft_item`; `use_item` counts per item type and ignores them.

### Rewards

| Type | Fields |
|---|---|
| `justquests:give_item` | `item`, `count` (dropped at the player's feet if the inventory is full) |
| `justquests:loot_table` | `loot_table` |
| `justquests:xp` | `amount` |
| `justquests:effect` | `effect`, `seconds` (30), `amplifier` (0) |
| `justquests:message` | `message` (string or per-language map) |
| `justquests:title` | `title`, `subtitle` (optional), `fade_in` (10), `stay` (70), `fade_out` (20) — ticks |
| `justquests:command` | `command`, run for the player; `{player}` is replaced by the name |

A complete [example datapack](docs/example-datapack) has one quest for every objective and reward
type, plus tags, filters, chains, repeatable and multi-language quests.

## Settings

Each world has `<world>/justquests/settings.json` (with a `_help` text inside). Change it and run
`/quest reload`.

Each player's own book and tracker options live in `config/justquests-client.json`: `hud`
(tracker on/off), `hudCorner` (`top_left`, `top_right`, `bottom_left`, `bottom_right`), `hudMax`
(quests shown, 1–5), `byStatus`, `hideCompleted` and `pinned` (also set by the book's buttons).

| Key | Default | Meaning |
|---|---|---|
| `mainQuests` | `true` | Show the built-in quests |
| `announceCompletions` | `true` | Tell everyone when a player finishes a quest |
| `completionSound` / `completionToast` | `true` | Sound and action-bar message on completion |
| `claimRewards` | `true` | Finished quests wait until the player claims the rewards; `false` pays them out at once |
| `discordWelcome` | `true` | One-time Discord invite on a player's first join |
| `generatedQuests` / `generatedCount` | `true` / `5` | Generated quests on/off and how many per board (1–20) |
| `difficulty` | `normal` | `easy`, `normal` or `hard` for generated quests |
| `generatorExclusiveClaims` | `true` | An accepted generated quest is locked for everyone else |
| `generatorOneActivePerPlayer` | `true` | At most one unfinished generated quest per player |
| `generatorReleaseOnAbandon` | `true` | An abandoned generated quest becomes available again |
| `generatorClaimExpiryHours` | `0` | Release accepted generated quests after this many hours (0 = never) |
| `generatorCycleHours` / `generatorCycleAnchorHour` | `12` / `0` | Rotation period and the hour of the first rotation |
| `generatorModdedShare` | `0.35` | Share of generated quests that use content from supported mods |
| `generatorDisabledProfiles` | `[]` | Mod profiles to ignore, e.g. `["create"]` |
| `generatorAdaptiveBalancing` | `false` | Tune time estimates from how long quests really take |
| `generatorStats` | `true` | Anonymous statistics in `justquests/generator_v2_stats.json` |

## Building from source

Needs JDK 17 and JDK 21 (Gradle picks the right one per Minecraft version).

```bash
git clone https://github.com/ErikEdits/JustQuests.git
cd JustQuests
./gradlew :neoforge-1_21_1:build      # one version
./gradlew build                       # all 36 builds (slow the first time)
```

Jars end up in `<loader>/<version>/build/libs/`. GitHub Actions builds all 36 on every push and
publishes a release with every jar for each `v*` tag.

### Project layout

```
neoforge/<version>/   14 NeoForge builds   ─┐
fabric/<version>/     18 Fabric builds      ├─ one source tree per Minecraft version
forge/<version>/       4 Forge builds      ─┘
generator-v2/         the quest generator core (Java 17, own tests), copied into every build
                      by scripts/sync_generator_v2.py
docs/                 example datapack, design notes, roadmap (build-todo.md)
```

## Roadmap

What's done and what's next: [docs/build-todo.md](docs/build-todo.md). Ideas and questions from
the community: [docs/.requests/questions.md](docs/.requests/questions.md).

## Community and support

- Discord: https://discord.gg/cMTGE9QCja
- Bugs and suggestions: [issue tracker](https://github.com/ErikEdits/JustQuests/issues)

## License

[GNU LGPL v3](LICENSE). Free to use in any modpack, no permission needed. Modified versions must
stay open source under the same license.
