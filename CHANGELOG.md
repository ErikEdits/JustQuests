# Changelog

All notable changes to JustQuests are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com/).

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
