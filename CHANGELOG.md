# Changelog

All notable changes to JustQuests are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com/).

## [0.1.9] - 2026-06-20

Server & admin tools. Still command-only; the GUI comes in v0.2.

### Added
- **Admin commands** (OP, permission level 2):
  - `/quest admin view <player>` — see a player's active and completed
    quests.
  - `/quest admin reset <player>` — clear all of a player's progress.
  - `/quest admin reset <player> <id>` — reset a single quest.
  - `/quest admin complete <player> <id>` — force-complete a quest and
    grant its rewards.
- **Completion broadcast.** When a player finishes a quest, a message is
  announced to everyone on the server. On by default; turn it off with
  `"announceCompletions": false` in the per-world settings file.
- **Per-world settings** are now centralized in
  `<world>/justquests/settings.json` (`discordWelcome`,
  `announceCompletions`), written with defaults on first run.

### Notes
- Difficulty levels and per-quest permission nodes are still planned for a
  later 0.1.x release.

[0.1.9]: https://github.com/ErikEdits/JustQuests/releases/tag/v0.1.9

## [0.1.8] - 2026-06-20

Categories & organization. The quest list is now sorted and filterable.
Still command-only; the GUI comes in v0.2.

### Added
- **`/quest list <category>`** — filter the list to one category (with tab
  completion for the categories that exist).
- **`/quest categories`** — list all categories with how many quests each
  has.
- **Sorted quest list.** Quests are now grouped by category (with a header
  per category) and ordered by an optional per-quest `sort` weight, then by
  id — a stable, predictable order instead of a random one.
- **`sort` field** on quests (integer, default 0) to control the order
  within a category.

### Changed
- All 18 bundled quests now have proper categories (gathering, farming,
  combat, survival, daily) and a sort order forming a sensible progression.

[0.1.8]: https://github.com/ErikEdits/JustQuests/releases/tag/v0.1.8

## [0.1.7] - 2026-06-20

Rewards & quest logic. Quests can now chain, repeat, and hand out XP,
effects and messages. Still command-only; the GUI comes in v0.2.

### Added
- **Quest prerequisites / chains.** A quest can list `requires` (quest ids
  that must be completed first). Locked quests show a teaser in
  `/quest list` and can't be accepted until their requirements are met.
- **Repeatable quests.** Set `repeatable: true` to let a quest be taken
  again after completion, with an optional `cooldown_hours` wait between
  runs (e.g. daily quests).
- **Three new reward types:**
  - `xp` — give experience points.
  - `effect` — apply a potion effect (`seconds`, `amplifier`).
  - `message` — send the player a message (supports the per-language map).
- Bundled examples: `seasoned_miner` (requires `master_miner`, gives XP +
  Haste) and `daily_bread` (repeatable, 24h cooldown).
- `/quest test` parses a sample of every new reward type.

### Notes
- This release also ships for **Minecraft 1.21 and 1.21.2** in addition to
  1.21.1 (NeoForge). Pick the jar for your version.

[0.1.7]: https://github.com/ErikEdits/JustQuests/releases/tag/v0.1.7

## [0.1.6] - 2026-06-19

More objective types. Still command-only; the GUI comes in v0.2.

### Added
- **Four new objective types:**
  - `mine_block` — break blocks of a type (counts the break itself, unlike
    `collect_item` which counts pickups).
  - `breed_animal` — breed animals of a type.
  - `consume_item` — eat or drink an item (or any item in a tag).
  - `smelt_item` — take a smelted result out of a furnace (id or tag).
- Bundled example quests for each: `master_miner`, `cattle_rancher`,
  `hearty_meal`, `the_smelter` (English + German).
- `/quest test` now also parses a sample of every new objective type.

### Notes
- `enchant_item` and per-objective component/NBT matching are still
  planned — they need a mixin / a richer matcher and will land in a later
  0.1.x release.

[0.1.6]: https://github.com/ErikEdits/JustQuests/releases/tag/v0.1.6

## [0.1.5] - 2026-06-19

Community update. The in-game GUI (v0.2) will be designed from community
polls, so this release helps players find the Discord. Still command-only.

### Added
- **One-time Discord welcome.** The first time a player joins a world,
  they get a single, clickable invite to the community Discord (vote on
  the upcoming GUI, get support, see sneak peeks). It never repeats —
  seen players are remembered in `<world>/justquests/seen-players.json`.
- **`/quest discord`** — shows the clickable invite anytime.
- **Per-world settings file** `<world>/justquests/settings.json`. Set
  `"discordWelcome": false` to turn the welcome off (the file is created
  with defaults on first run).
- `/quest test` gained a community-hint check.

### Notes
- The Discord invite is a public link; no tokens or webhooks are bundled.

[0.1.5]: https://github.com/ErikEdits/JustQuests/releases/tag/v0.1.5

## [0.1.4] - 2026-06-19

Localization update (Phase 7). Quests can speak the player's language —
still command-only; the GUI comes in v0.2.

### Added
- **Multi-language quest text.** A quest's `title` and `description` can
  now be either a plain string (as before) or a per-language map, e.g.
  `"title": { "en_us": "Mining Trip", "de_de": "Bergbau-Ausflug" }`.
  Each player sees their own client language, falling back to English,
  then to any provided language. Existing string-only quests keep working.
- **Free content translation.** Item, mob and block names shown in the
  goal/reward lines are sent as translatable text, so they appear in each
  player's own language automatically — no translation files needed.
- All 12 bundled quests now ship with English **and** German text.
- The custom-quest template documents the per-language map form and
  includes a localized example quest.
- `/quest test` gained a localization check (map resolution + English
  fallback).

### Notes
- The mod's own connective words (e.g. "Collect", "Reward", command
  feedback) remain English for now (Q22); the data-driven text and the
  vanilla content names are what localize.

[0.1.4]: https://github.com/ErikEdits/JustQuests/releases/tag/v0.1.4

## [0.1.3] - 2026-06-19

Custom quests update (Phase 4). Server owners and players can now write
their own quests per world — still command-only; the GUI comes in v0.2.

### Added
- **Per-world custom quests.** A `<world>/justquests/custom-quests.json`
  file lets you add quests without a datapack. Each key is the quest id
  (a bare name becomes `justquests:<name>`, or use a full
  `namespace:path`); keys starting with `_` are ignored.
- **Fill-in-the-blanks template** is written automatically on first run,
  with an explained example and blank slots. Slots with no objectives are
  skipped silently, so you can leave blanks.
- **Automatic live reload** — the file is watched and reloaded within a
  few seconds of saving it; `/quest reload` also reloads it on demand and
  now reports the total quest count.
- **Source precedence:** a custom quest overrides a datapack quest that
  shares the same id.

[0.1.3]: https://github.com/ErikEdits/JustQuests/releases/tag/v0.1.3

## [0.1.2] - 2026-06-19

Content update (Phase 2). Many new objective and reward types — still
command-only; the GUI comes in v0.2.

### Added
- **New objective types:** `kill_mob`, `place_block`, `craft_item`,
  `tame_animal`, `gain_advancement`, `visit_dimension`, `reach_level`,
  `reach_location` (alongside `collect_item`). `visit_dimension` matches
  modded dimensions by id.
- **Item tags:** `collect_item` / `craft_item` accept a single id **or** a
  tag (`#minecraft:logs`).
- **Quest mode** `all | any` — finish a quest when *all* objectives are
  done, or *any* one of them.
- **Quest categories** (`category` field, shown in `/quest list`).
- **New reward types:** `command` (runs a command as the player, `{player}`
  substitution — enables economy/effects with no dependency) and
  `loot_table` (random items from a loot table).
- **Expanded `/quest test`:** now also round-trips every loaded quest and
  parses a sample of every objective + reward type, and reports the
  objective/reward types in use.
- Bundled example quests: `slayer` (kill 10 zombies) and `lumberjack`
  (collect 32 of any log via tag).

### Changed
- Objective tracking refactored into a loader-agnostic core so any event
  can advance objectives (item pickup, mob kill, block place, craft, tame,
  advancement, dimension change; reach_level/location are polled).

### Fixed
- **Item tag parsing.** The first tag implementation used a codec that
  needed RegistryOps, which broke parsing of a plain single item id under
  the datapack's JsonOps (only 1 of the bundled quests loaded). Replaced
  with a runtime matcher (`ItemStack.is`) so a single id, a list, and a
  `#tag` all parse correctly. Verified all bundled quests load.
- Unknown/typo'd reward types are now logged and skipped per quest
  (same hardening as objectives) instead of being able to abort loading.

### Notes
- `enchant_item` is not yet included (no clean cross-version event;
  planned via poll/mixin). Cross-loader strategy: see
  [docs/cross-loader-events.md](docs/cross-loader-events.md).

[0.1.2]: https://github.com/ErikEdits/JustQuests/releases/tag/v0.1.2

## [0.1.1] - 2026-06-18

Robustness + diagnostics update. Still command-only (GUI comes in v0.2).

### Changed
- **Storage moved to a per-world JSON file**
  (`<world>/justquests/progress.json`) — the single source of truth and
  the future bridge to the planned plugin edition. Replaces the v0.1
  per-player NBT attachment; existing v0.1 progress is **migrated
  automatically** on first login.
- Saves are now **atomic** (temp file + move), so a crash mid-write can
  never corrupt or lose player progress. Saving is throttled (loose).

### Added
- `/quest test` (OP) — runs a self-test battery (quest loading, quest
  validity, codec round-trip, storage writability, store status) and
  writes a full report including the Minecraft environment (version,
  loaded mods, memory, OS, worlds, player data) to
  `justquests-diagnostics.log` in the game folder.

### Fixed
- A single broken quest (unknown objective/reward type, no objectives, or
  a non-positive count) no longer aborts loading of all quests — it is
  logged and skipped.
- Large item rewards are split into proper max-size stacks instead of one
  oversized stack.
- No more empty `{}` entries written for players without quests; saves
  only happen when progress actually changes; null-safety hardening.

[0.1.1]: https://github.com/ErikEdits/JustQuests/releases/tag/v0.1.1

## [0.1.0] - 2026-06-12

First public release. A lightweight, datapack-driven quest book for
NeoForge 1.21.1 — command-only, no GUI yet.

### Added
- Datapack-driven quests loaded from
  `data/<namespace>/justquests/quests/<id>.json`
- `collect_item` objective type (counts items picked up from the ground)
- `give_item` reward type (drops at the player's feet if the inventory
  is full)
- Per-player progress stored via a NeoForge data attachment, persists
  across logout and survives death
- Commands: `/quest list` (shows each quest's description, goal and
  reward), `/quest progress`, `/quest accept <id>`,
  `/quest abandon <id>`, `/quest reload`
- Quest ids use tab completion (`accept` suggests all quests, `abandon`
  suggests your active ones)
- 10 bundled starter quests (first_steps → ender_seeker)
- Mod icon

### Notes
- Objectives track items **picked up from the ground** (mining, crops,
  mob drops). Crafted items go straight to the inventory and do not
  count — design quests around gathering.
- NeoForge 1.21.1 only. More loaders and MC versions are planned.

[0.1.0]: https://github.com/ErikEdits/JustQuests/releases/tag/v0.1.0
