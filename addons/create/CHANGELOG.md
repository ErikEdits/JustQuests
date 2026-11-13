# Changelog

All notable changes to the JustQuests Create Addon are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com/).

## [0.1.1] - 2026-11-13

### Fixed
- **Joining a world could fail with "Invalid player data"** in big modpacks with Architectury,
  seen together with Stellaris (#15). The cause is not in the addon: Architectury keeps the packets
  that mods register in maps that are not safe while NeoForge loads mods in parallel, so an entry can
  get lost ("codec is null" in the log). Whether it happens depends on timing, so adding or removing
  any mod can trigger or hide it. The addon now makes these maps safe before any mod registers. This
  only happens when Architectury is installed; the log then says "Made Architectury's packet maps
  safe for parallel mod loading".

## [0.1.0] - 2026-11-01

First version, for NeoForge 1.21.1 with Create 6.0.10 and JustQuests 0.4.6 or newer.

### Added
- **Quest objectives for Create's machines.** Each counts what one kind of machine really makes:
  `press_item`, `compact_item`, `mix_item`, `mill_item`, `crush_item`, `saw_item`, `deploy_item`,
  `fill_item`, `mechanical_craft_item`, `wash_item`, `smoke_item`, `blast_item`, `haunt_item` and
  `assemble_item`, all under `justquests_create:`. (Requested on GitHub, #15.)
- The quest book shows them in words, such as "0/16 Press Iron Sheet", with the item as the icon.
  English, German, Spanish, French and Japanese.
- **Who gets the progress:** the player who placed the machine, wherever they are on the server. A
  machine without a known owner (placed before the addon, built by a schematic) counts for the
  nearest player within 32 blocks. An owner who is offline gets nothing.
