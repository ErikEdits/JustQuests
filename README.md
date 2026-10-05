# JustQuests

**A lightweight quest book for NeoForge, Fabric and Forge - and a plugin for Spigot, Paper and Purpur.** No GUI bloat, no heavy
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
- **Ready to play** — 25 built-in quests in English, German, French, Spanish and Japanese.
  Server owners can hide them with `/quest mainquests off`.
- **Generated quests** — a fresh board every 12 hours (00:00 and 12:00 by default), sized to a
  sensible amount of play time and paid with fitting rewards: gather, mine, craft, hunt, enchant,
  throw, brew and more. Supports content from
  **Farmer's Delight, Create, Mekanism, The Twilight Forest, Botania, Applied Energistics 2,
  Immersive Engineering, Biomes O' Plenty, Alex's Mobs, Tinkers' Construct and Ars Nouveau** when
  installed, three
  difficulty levels, and Nether/End quests once players have been there.
- **First come, first served** — on servers a generated quest someone accepted is taken; the book
  shows *Taken by Steve* or *Reserved for you*.
- **Write your own quests** — in a datapack or in a per-world `custom-quests.json` that reloads
  by itself while you edit it. 15 objective types, 8 reward types, item tags and item filters.
- **Speaks the player's language** — the mod, the built-in quests and the generated quests come
  in English, German, French, Spanish and Japanese; each player reads their own game language.
  Your own quests can use a per-language map too, and item, block and mob names are translated
  by the game.
- **Quest logic** — prerequisites (`requires`), repeatable quests with cooldowns, "all" or "any"
  objectives, categories and sort order.
- **For server owners** — completion broadcast, stats and leaderboard, admin commands, a
  self-test (`/quest test`) and per-world `settings.json`.

## Supported versions

| Loader | Minecraft | Quest book |
|---|---|---|
| NeoForge | 1.20.4, 1.20.6, 1.21 – 1.21.11, 26.1.2, 26.2 | yes |
| Fabric (needs [Fabric API](https://modrinth.com/mod/fabric-api)) | 1.18.2, 1.19.2, 1.19.4, 1.20.1, 1.20.4, 1.20.6, 1.21 – 1.21.11, 26.1 – 26.3 | 1.20.1 and newer |
| Forge | 1.18.2, 1.19.2, 1.19.4, 1.20.1 | 1.20.1 |
| Spigot / Paper / Purpur (server plugin) | 1.19 – 1.21.11, 26.1 – 26.3 | chest menu |

On 1.18 and 1.19 JustQuests is command-only; quests, tracking and rewards work the same.
Install the mod on the server **and** the clients (both are required). The plugin is
server-only - see [Server plugin](#server-plugin-spigot-paper-purpur).

## Installation

1. Install NeoForge, Fabric (+ Fabric API) or Forge for your Minecraft version.
2. Download the jar for **your loader and exact version** from
   [Modrinth](https://modrinth.com/mod/justquests) or the
   [GitHub releases](https://github.com/ErikEdits/JustQuests/releases) — the file name says which,
   e.g. `JustQuests-fabric-1.21.1-0.3.5.jar` — and put it into `mods/`.
3. Start the game, press **J** (or run `/quest list`).

## Server plugin (Spigot, Paper, Purpur)

JustQuests also comes as a plugin for Spigot, Paper and Purpur. Players join with a plain vanilla
client - nothing to install on their side. There is one jar per range of versions:

| Server version | Jar | Java |
|---|---|---|
| 1.19 – 1.20.6 | `JustQuests-plugin-1.19-<version>.jar` | 17 or newer |
| 1.21 – 1.21.11 | `JustQuests-plugin-1.21-<version>.jar` | 21 or newer |
| 26.1 – 26.3 | `JustQuests-plugin-26.1-<version>.jar` | 25 |

1. Put the jar for your server version into the `plugins/` folder and start the server (a jar for
   another range says so in the console).
2. New players get a **quest book**; right-click it (or type `/quest`, `/quests` or `/jq`) to open
   the quests. `/quest book` gives a new one.

What is the same as the mod: the 25 built-in quests, the quest format (datapack quests in the
world's `datapacks/` folder work unchanged), all 15 objective and 8 reward types, claiming and
reward choices, prerequisites, repeatable quests, the commands, the permission nodes, and the five
languages - each player reads their own game language, and item, block and mob names are
translated by their game.

What is different without a client mod:

- The **quest book is a chest menu**: the start page has your stats, the quests by status
  (rewards ready, active, available, locked, completed) and by category; a click opens a quest with
  its goals, rewards and the Accept / Abandon / Claim button. A reward choice is picked by clicking
  the option. Shift-click in a list accepts a quest right away.
- The **quest tracker is a boss bar** with the pinned quest (or the first active one) and its
  progress. Pin a quest in its page or with `/quest track <id>`; `/quest bossbar` or the button on
  the start page hides it for you.
- Progress shows above the hotbar ("Mine 16x Stone (12/16)").

**The server generator.** The plugin has the quest generator too, built out for servers:

- **The board** (first come, first served) grows with the server: `baseQuests` plus one quest per
  `perPlayers` players who played in the last 7 days, up to `maxQuests`.
- **Personal quests:** every player gets their own quests each day (3 by default), which only they
  see and nobody can take from them. They follow the player's own progress - Nether quests only
  once *they* have been to the Nether.
- **Weekly quests:** harder, longer quests (about one to three hours) for everyone, new each week;
  each player can do each one once. Valuable loot on top (enchanted books on hard).
- **Server goal:** one goal per week that everyone works on together, for example "mine 1,000 copper
  ore", sized by the players of the last 7 days. Every matching action counts by itself; 25, 50, 75
  and 100 % are announced in chat, and everyone who helped gets the same reward (also when they
  were offline at the time). `/quest goal` shows how far it is.
- **Better rewards:** generated quests offer a choice of up to three rewards of about the same value;
  a lucky bonus now and then (a list in `config.yml`); a personal streak pays up to 50 % more for
  days in a row with a personal quest done; a reward multiplier; and money through an economy plugin
  when you set its command (`generator.rewards.money.command`, e.g. `eco give {player} {amount}`).
  Your own quests can pay money too: `{"type": "justquests:money", "amount": 25}` (plugin only).

The quest book's start page has buttons for the personal quests, the weekly quests and the server
goal. Operators have `/quest reroll`, `/quest difficulty` and `/quest generator status|stats|preview|explain|release`.
Everything is set in the `generator:` section of `config.yml`; the state lives in
`plugins/JustQuests/generator/`.

**Announcements.** Everyone online hears about a new board, new weekly quests and a new server goal
(a click on the message opens the quest book), and when the server goal is reached, fireworks go up
around every player - they hurt nobody. Each is a switch in the `announcements:` section.

**Quest givers.** Look at a villager, any mob or an armor stand and type `/quest npc set [name]`
(`&` colour codes work in the name): from then on a right-click on it opens the quest book instead
of trading. It stands still, stays silent, can't be hurt and never despawns. `/quest npc remove`
makes it ordinary again. Citizens NPCs: give them the command `quest` with Citizens' own
`/npc command add`, run as the player.

**PlaceholderAPI.** With [PlaceholderAPI](https://placeholderapi.com/) on the server, scoreboards,
tab lists and chat plugins can show:

| Placeholder | Shows |
|---|---|
| `%justquests_completed%` | quests the player has completed (as on the leaderboard) |
| `%justquests_active%` / `%justquests_claimable%` / `%justquests_available%` | active quests / rewards waiting / quests to take now |
| `%justquests_rank%` | the player's place on the leaderboard |
| `%justquests_tracked%` / `%justquests_tracked_progress%` | the quest in the boss bar and its progress ("12/32") |
| `%justquests_streak%` | days in a row with a personal quest done |
| `%justquests_goal%` / `%justquests_goal_progress%` / `%justquests_goal_percent%` | the server goal, "640/1000", "64" |
| `%justquests_goal_left%` / `%justquests_goal_yours%` | time left ("3d 4h"), the player's part |

Files in `plugins/JustQuests/`:

| File | What it is |
|---|---|
| `config.yml` | the settings (the same names as the mod's `settings.json`), plus the quest book and boss bar options |
| `custom-quests.json` | your own quests in the mod's format; reloads by itself when saved |
| `quests/` | your own quests, one per file (`quests/my_quest.json` becomes `justquests:my_quest`) |
| `players/<uuid>.json` | each player's progress |
| `generator/` | the generator's state: board, personal and weekly quests, server goal |

**Moving a world from the mod to a plugin server:** on its first start the plugin takes over the
progress (`<world>/justquests/progress.json`), the custom quests and the generator's board of the
main world, so a world played in singleplayer with the mod keeps its quests on the server.

## Commands

| Command | What it does |
|---|---|
| `/quest list [category]` | All quests, grouped by category (locked ones show what they need) |
| `/quest categories` | Categories and how many quests each has |
| `/quest accept <id>` / `/quest abandon <id>` | Start or drop a quest (tab completion) |
| `/quest progress` | Your active quests and how far along you are |
| `/quest claim [id]` | Take the rewards of a finished quest (without an id: all waiting rewards) |
| `/quest stats` / `/quest leaderboard` | Your stats / the server's top 10 |
| `/quest team [create <name>\|invite <player>\|accept\|leave\|kick <name>]` | Make a team for team quests; without more, shows your team |
| `/quest discord` | The community Discord invite |

Operator commands (permission level 2, or the node from [Permissions](#permissions)):

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
`permission` (a permission node; only players with it see and take the quest, see below),
`team` (`true`: a team quest, see below), `objectives`, `rewards`. `title` and `description` can
be a string or a per-language map (`{"en_us": "...", "de_de": "..."}`); players see their own
language with English as fallback.

**Team quests.** A quest with `"team": true` (category `team` unless it names one) belongs to a
whole team: any member takes it, everyone's actions count towards it, the quest book and tracker
show the shared progress, and when it is done every member claims the rewards for themselves -
also members who were offline. A player's team is the one made with `/quest team` (create,
invite, accept, leave, kick, info; up to 8 players), else their scoreboard team (`/team`). The
teams live with the world in `justquests/teams.json` and `team-progress.json`.

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
| `justquests:choice` | `options`: a list of rewards; the player picks one when claiming (one choice per quest) |

A complete [example datapack](docs/example-datapack) has one quest for every objective and reward
type, plus tags, filters, chains, repeatable and multi-language quests.

## Settings

Each world has `<world>/justquests/settings.json` (with a `_help` text inside). Change it and run
`/quest reload`.

Each player's own book and tracker options live in `config/justquests-client.json`: `hud`
(tracker on/off), `hudCorner` (`top_left`, `top_right`, `bottom_left`, `bottom_right`), `hudMax`
(quests shown, 1–5), `byStatus`, `hideCompleted` and `pinned` (also set by the book's buttons;
the corner button in the title bar moves the tracker).

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

## Permissions

With LuckPerms (or another permission mod) every command has a node:

| Node | Default without a permission mod |
|---|---|
| `justquests.command.<name>` — `list`, `categories`, `stats`, `leaderboard`, `progress`, `accept`, `abandon`, `claim`, `team`, `discord` | everyone |
| `justquests.admin.<name>` — `reload`, `reroll`, `mainquests`, `difficulty`, `generator`, `test`, `admin` | operators (level 2) |
| the `permission` of a quest, e.g. `myserver.vip` | operators (level 2) |

The plugin adds `open`, `track`, `bossbar`, `book` and `goal` to the player nodes; its operator
commands are `reload`, `reroll`, `mainquests`, `difficulty`, `generator`, `npc`, `test` and `admin`.
`justquests.admin.*` gives all operator commands. A quest with a `permission` is hidden from
`/quest list`, the quest book and tab completion for players without the node. NeoForge and Forge
learn the quest nodes at server start, so a node added to a quest while the server runs works
after the next restart (Fabric needs no restart). The Fabric builds bundle
[fabric-permissions-api](https://github.com/lucko/fabric-permissions-api).

## Building from source

Needs JDK 17, JDK 21 and JDK 25 (Gradle picks the right one per Minecraft version; 26.x builds on 25).

```bash
git clone https://github.com/ErikEdits/JustQuests.git
cd JustQuests
./gradlew :neoforge-1_21_1:build      # one version
./gradlew :plugin-1_21:build         # the server plugin for 1.21.x (also plugin-1_19, plugin-26_1)
./gradlew build                       # all 43 mod builds and the 3 plugin jars (slow the first time)
```

Jars end up in `<loader>/<version>/build/libs/` (the plugin in `plugin/<version>/build/libs/`). GitHub
Actions builds everything on every push and
publishes a release with every jar for each `v*` tag.

### Project layout

```
neoforge/<version>/   16 NeoForge builds   ─┐
fabric/<version>/     23 Fabric builds      ├─ one source tree per Minecraft version
forge/<version>/       4 Forge builds      ─┘
plugin/               the Spigot/Paper/Purpur plugin (plain Spigot API): shared code in src/,
                      plugin/<version>/ adds one jar per version range (1.19, 1.21, 26.1)
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
