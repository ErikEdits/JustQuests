# Changelog

All notable changes to JustQuests are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com/).

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
