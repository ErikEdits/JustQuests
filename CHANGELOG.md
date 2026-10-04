# Changelog

All notable changes to JustQuests are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com/).

## [0.4.0] - in progress

Built step by step; test builds go to a local folder until the release.

### Added
- **Minecraft 26.1 – 26.3:** Fabric builds for 26.1, 26.1.1, 26.1.2, 26.2 and 26.3, and NeoForge
  builds for 26.1.2 and 26.2 (NeoForge 26.3 follows once it leaves beta). JustQuests now has 43
  builds. Minecraft 26.x needs Java 25.
- **Claim button:** a finished quest now waits with its rewards until you claim them, with the
  Claim button in the quest book, the `[Claim rewards]` button in chat, or `/quest claim <id>`
  (`/quest claim` alone takes every waiting reward). The quest book shows these quests on top
  under "Rewards ready", the HUD reminds you of them, and `/quest progress` and `/quest list`
  mark them too. On Minecraft 1.18–1.19 (no quest book) the chat button and the command do it.
  Server owners who prefer the old way set `"claimRewards": false` in `settings.json`; then
  rewards arrive the moment a quest is finished, as before.
- **Choice rewards** (`justquests:choice`): a quest can offer several rewards of which the player
  picks one when claiming: in the quest book by clicking an option, in chat with the `[1]` `[2]`
  buttons, or with `/quest claim <id> <number>`. Quests with a choice always wait to be claimed.
  The example datapack has `ex_choice`.
- **HUD position button** in the quest book's title bar: each click moves the tracker to the next
  corner (clockwise). While you point at the button, the book shows the tracker in its corner.
- **Permissions (LuckPerms & co.):** every command has a node, `justquests.command.<name>` for the
  player commands (everyone by default) and `justquests.admin.<name>` for the operator commands
  (operators by default). Quests can name a `permission`; players without it don't see the quest.
  NeoForge and Forge use their permission API, the Fabric builds bundle fabric-permissions-api.
- **Six more mods for the quest generator:** Applied Energistics 2, Immersive Engineering, Biomes O'
  Plenty, Alex's Mobs, Tinkers' Construct and Ars Nouveau. Botania, Mekanism and the Twilight Forest
  got more quests (more flowers and gear, metal blocks and steel machines, more wood, beetles and
  meef). Every new target was checked against the mods' released jars.
- `settings.json` gets new options added by itself, so owners see them without deleting the file.
- **Generated quests in five languages:** quests from the generator now have their title,
  description and reward message in English, German, French, Spanish and Japanese, and each player
  reads their own game language. Item, block and mob names use the official translations of
  Minecraft and of the supported mods. The built-in quests got Japanese as well.

### Changed
- **Every text is translatable:** the quest book, the HUD, goal and reward names, and all chat
  messages of the commands now come from language files (`assets/justquests/lang/`), with
  English, German, French, Spanish and Japanese included. Each player sees their own game
  language, also on Minecraft 1.18–1.20.1. On 1.19.4 and newer, players without the mod see the
  English text.
- The German, French and Spanish texts of the built-in quests have proper accents and use the
  game's own names (for example the "Acquire Hardware" advancement).
- Loot rewards read "Loot: simple dungeon" instead of the full loot table id; tag goals read
  "any logs"; dimension goals name the vanilla dimensions ("Visit the Nether").
- The Discord welcome no longer talks about voting on the v0.2 GUI.

## [0.3.5] - 2026-10-04

### Added
- **Minecraft 1.21.11** for NeoForge and Fabric. JustQuests now has 36 builds.
- **Search in the quest book:** with 15 or more quests a search field appears next to the list
  buttons. It looks at titles, categories and goals, so "diam" finds every quest with diamonds.
- **Pin quests to the HUD:** active quests get a pin button next to "Abandon". With pins the
  tracker shows only the pinned quests, without pins the newest ones as before. Finished or
  abandoned quests are unpinned by themselves.
- **Mouse wheel** over the quest list turns the pages.

### Changed
- What a button does now shows next to the "Quests" title while the mouse is on it.

### Fixed
- The "Abandon" button had an x drawn under its label.

## [0.3.4] - 2026-10-04

### Added
- **Quest tracker (HUD):** your active quests and their goals in a small panel in the top-left
  corner, newest first, up to three. Toggle it with **H** (rebindable in Controls) or the new
  button in the quest book. Hidden with F1, F3 and while a menu is open.
- **Sorted quest book:** the list is grouped under headers, either by category ("Combat 1/3")
  or by status (Active, Available, Locked, Completed). A button switches between the two, another
  hides completed quests. Page number between the arrows.
- **Pixel icons:** every quest shows an item icon, in the list and next to its title. It is taken
  from the first goal (the item or block, a spawn egg for mobs, a compass for places and so on),
  or set with the new optional quest field `"icon": "minecraft:diamond_sword"`. Categories and
  states got their own pixel icons in the style of the book.
- **Stats page:** a button in the title bar shows your progress in the book: completed and
  active quests, every category with its count, your first and last completion and, on servers,
  your place on the leaderboard.

### Changed
- Locked quests are now marked in the book: if a quest needs another one first it shows a lock,
  "Needs: <quest>" and a disabled button. Repeatable quests on cooldown show a clock and when
  they are available again.
- The quest book window is 32 pixels wider so titles fit next to the icons.
- Goal lines read "5/8 Mine Iron Ore" instead of "5/8 Mine 8x Iron Ore"; tags read "any logs".
- Book and HUD options are saved in `config/justquests-client.json`.
- A completed quest shows its goals as done and "Rewards received" (rewards are still paid out
  the moment a quest completes; a claim button is planned for 0.4.0). It used to show 0/x.

## [0.3.3] - 2026-10-04

### Added
- **Tags for block and mob goals:** `mine_block`, `place_block`, `kill_mob`, `tame_animal` and
  `breed_animal` accept a tag as well as an id, e.g. `"block": "#minecraft:logs"` or
  `"entity": "#minecraft:skeletons"`.
- **The stonecutter counts as crafting:** `craft_item` now also counts items taken out of a
  stonecutter.
- **More variety in generated quests:**
  - enchanting: "Enchant 3 books", "Enchant an iron sword"
  - using items: throw snowballs, eggs or ender pearls, use bone meal, launch fireworks
  - brewing (Hard, after the Nether): drink a Potion of Swiftness, Night Vision, Fire Resistance,
    Strength or Water Breathing
  - any kind counts: "Mine 32 logs" (any log), "Defeat 10 skeletons" (strays, wither skeletons
    and bogged too)
  - stonecutter crafts: stone brick stairs, chiseled stone bricks, cut copper, and with Create the
    cut granite, diorite, andesite, tuff, limestone and asurine

## [0.3.2] - 2026-10-04

### Added
- **New goal `enchant_item`:** enchant items at an enchanting table, optionally a specific item
  (`"item": "#minecraft:swords"`). An anvil does not count.
- **New goal `use_item`:** use an item a number of times: throw snowballs or ender pearls, apply
  bone meal, look through a spyglass, light a fire, place blocks, mine with a tool. Only uses
  that actually do something count, so spamming right-click does nothing.
- **Item filters:** item goals can ask for more than the item. Write `item` as an object with
  `enchantments` (minimum levels), `potion` or `name` (an anvil name), for example
  `{"id": "minecraft:potion", "potion": "minecraft:swiftness"}`. The same quest file works on
  every Minecraft version.
- **New reward `title`:** a big on-screen title with an optional subtitle and timing
  (`fade_in`, `stay`, `fade_out`), translatable like the other quest texts.
- Example quests for all of it in the example datapack.

### Changed
- The two new goals count Minecraft's own statistics ("Items Enchanted", "Times Used"), so
  they behave the same on NeoForge, Fabric and Forge.
- Removed the old (v1) quest generator code, unused since 0.3.0. Worlds keep their old
  generated-quest file, which the new generator still reads once.

## [0.3.1] - 2026-10-03

### Added
- **See who took a generated quest.** In the quest book, a generated quest another player already
  accepted is greyed out with a lock and says *Taken by Steve* (or *Completed by Steve*); its
  button shows *Taken* instead of *Accept*. Your own one says *Reserved for you*. The book updates
  for everyone the moment someone accepts, abandons or finishes a quest.
- `/quest list` marks generated quests with `[yours]`, `[taken by Steve]` or `[completed by Steve]`,
  and trying to accept a taken quest now names the player who has it.

### Fixed
- **Quest book text on 1.21.6 - 1.21.10.** Minecraft 1.21.6 stopped drawing text without an
  opacity value, which made the titles, descriptions and buttons in the quest book invisible.
- The admin commands `/quest admin reset` and `/quest admin complete` now refresh the player's open
  quest book right away.

## [0.3.0] - 2026-09-27

Generated quests 2.0 — the quest generator was rebuilt from scratch.

### Added
- **Smarter quests.** Every generated quest is checked to be doable on your game version and mod
  set, sized to a sensible amount of play time, and paid with a reward that matches the effort. No
  more "collect 40 apples" or "collect iron ore" (which drops raw iron).
- **More variety.** Ten kinds of objectives — collect, mine, craft, smelt, hunt, breed, tame, eat,
  place and travel — plus themed multi-step quests like *Blacksmith's Order*, *Harvest Festival* or
  *Nether Expedition*. Each description tells you where to look and what tool you need.
- **First come, first served.** A generated quest someone accepted is taken; everyone can hold one
  generated quest at a time. (Both rules can be switched off in `settings.json`.)
- **Difficulty for the whole world:** `easy`, `normal` or `hard` (OP: `/quest difficulty`). Hard
  means longer expeditions, deeper tiers and better rewards.
- **Grows with your world.** Nether and End quests appear once players have been there, and quests
  in the Twilight Forest once players have found a portal.
- **Mod support:** with **Farmer's Delight**, **Create**, **Mekanism**, **The Twilight Forest** or
  **Botania** installed, the board also offers quests for their crops, meals, ores, machines, mobs
  and flowers. Every installed mod gets its turn on the board.
- **Right for your version:** content that your Minecraft version only has behind an experimental
  toggle (for example cherry wood on 1.19.4) never shows up.
- **For admins:** `/quest generator status | explain <id> | preview [n] | stats | release <id>`, new
  `settings.json` keys (`difficulty`, `generatorExclusiveClaims`, `generatorOneActivePerPlayer`,
  `generatorReleaseOnAbandon`, `generatorClaimExpiryHours`, `generatorCycleHours`,
  `generatorCycleAnchorHour`, `generatorModdedShare`, `generatorDisabledProfiles`,
  `generatorAdaptiveBalancing`, `generatorStats`) and optional world overrides in
  `justquests/generator_v2/`.

### Fixed
- **Accepted generated quests are safe.** A quest you accepted stays until you finish or abandon it,
  even when the board rotates or the server restarts (the old generator dropped it on rotation).

### Changed
- Rotation now happens at fixed local times (00:00 and 12:00 by default) instead of 12 hours after
  the last refresh. Existing worlds are migrated automatically; quests you had active from the old
  generator are kept.

[0.3.0]: https://github.com/ErikEdits/JustQuests/releases/tag/v0.3.0

## [0.2.5] - 2026-09-27

Stability update — mainly a Fabric fix, plus lighter multiplayer sync.

### Fixed
- **Fabric: item-pickup mixin targeted a method that doesn't exist** — the
  `collect_item` mixin injected into `Player.take(...)`, but `Player` never
  declares `take()` on any supported version (it is only inherited from
  `LivingEntity`). With the mixin config requiring every injection to find its
  target, this could fail when the class loads. The earlier issue #1 fix only
  made the target unambiguous; it didn't fix this. The mixin now targets
  `LivingEntity.take(...)` (and still only counts server players picking up
  items), on all 17 Fabric versions.
- **Fabric 1.18.2–1.20.4: mixin compatibility level** — these versions run on
  Java 17 but the mixin config asked for `JAVA_21`; it now uses `JAVA_17`.
- **Quest book showed the previous server's quests** — the client cache is now
  cleared on disconnect, so switching servers/worlds never shows stale data.

### Changed
- **Lighter multiplayer sync** — picking up, mining, crafting etc. used to resend
  the *whole* quest list to the player on every bit of progress. Progress,
  accept and abandon now send only the player's progress; the full list is sent
  on join and when the list itself changes (reload, reroll, rotation,
  `/quest mainquests`).

[0.2.5]: https://github.com/ErikEdits/JustQuests/releases/tag/v0.2.5

## [0.2.4] - 2026-09-25

Server owners can now turn the built-in quests off (a community request).

### Added
- **Disable the built-in "main" quests** — a new per-world setting `mainQuests`
  (default `true`) in `settings.json`. When off, the quests bundled with the mod
  are hidden from `/quest list` and the quest book; your `custom-quests.json`
  quests and the generated quests stay. Toggle it in-game with
  **`/quest mainquests on|off`** (OP), which saves to `settings.json` and updates
  everyone live; `/quest mainquests` shows the current state.
- **`/quest reload` re-reads `settings.json`** too, so hand-edited settings apply
  without a restart.

### Fixed
- **Open quest book now refreshes live** — the book cached its quest list when
  opened, so a `/quest reload`, reroll, generator rotation or `mainquests` toggle
  didn't show until you reopened it. It now rebuilds the list when a new sync
  arrives.

[0.2.4]: https://github.com/ErikEdits/JustQuests/releases/tag/v0.2.4

## [0.2.3] - 2026-08-03

Multiplayer sync — the quest book now works on dedicated servers, not just in
singleplayer.

### Fixed
- **Quest book empty on servers** — the book read quest data directly from the
  server-only stores, which only worked through the integrated (singleplayer)
  server. On a dedicated server the client had no data, so the book showed
  "No quests (singleplayer only for now)."

### Added
- **Server → client quest sync** — the full quest list plus the player's own
  progress are serialized (reusing the existing codecs) and sent to the client
  on join, whenever that player's progress changes, and on quest reload/reroll
  and generator rotation. The quest book reads this synced copy, so it works on
  dedicated servers and still in singleplayer (the sync also fires over the
  loopback connection). Rolled out to every GUI version (28 builds:
  NeoForge 1.20.4–1.21.10, Forge 1.20.1, Fabric 1.20.1–1.21.10). The 1.18/1.19
  builds stay command-only.
- **Upload script sets the Modrinth environment** — the local uploader now
  patches the project to Client + Server "required" (both sides) on upload, so
  the environment no longer has to be set by hand.

[0.2.3]: https://github.com/ErikEdits/JustQuests/releases/tag/v0.2.3

## [0.2.2] - 2026-08-03

Crash fix, the quest generator on every version, and a textured GUI.

### Fixed
- **Fabric startup crash** (issue #1): the `PlayerTakeMixin` (collect_item
  pickup) targeted `take` by name only, which is ambiguous on `Player`
  (several `take` overloads) — the mixin refmap ended up without an entry, so
  Fabric couldn't find the target at runtime and the game crashed on launch.
  Now the injection uses the full descriptor
  `take(Lnet/minecraft/world/entity/Entity;I)V`, so it resolves and remaps
  correctly on every Fabric version.

### Added
- **Procedural quest generator on all versions** — a rotating set of
  auto-generated quests (category `generated`), 12h real-clock rotation,
  6-day no-repeat, per-world toggle (`generatedQuests`/`generatedCount` in
  `settings.json`), and `/quest reroll` (OP). Now present on all 34 builds.
- **Textured quest book** — the quest book (key **J**) now renders from the
  JustQuests v2-full texture set instead of vanilla buttons (NeoForge 1.21.1
  first; rolling out to the other GUI versions).

[0.2.2]: https://github.com/ErikEdits/JustQuests/releases/tag/v0.2.2

## [0.2.1] - 2026-07-02

Older-version reach: JustQuests now runs on two 1.20.x versions.

### Added
- **Minecraft 1.20.4 and 1.20.6** (NeoForge). JustQuests now ships for
  **1.20.4, 1.20.6 and 1.21–1.21.10** (13 versions). Same features and the
  interim quest-book GUI (key **J**) as on 1.21.

### Notes
- **1.20.1, 1.19.x and 1.18.x are not supported** — NeoForge only exists from
  1.20.2 upward. Those versions would require a separate Forge (or Fabric)
  port, which isn't planned right now.
- **1.20.2, 1.20.3 and 1.20.5 are not included** — their NeoForge artifacts
  can't be consumed by the current build toolchain (1.20.2 predates the
  required metadata; 1.20.3 and 1.20.5 only have beta builds without it).
- 1.20.4 was backported to the older pre-1.20.5 APIs (DataFixerUpper,
  tick/pickup events, potion-effect registry, loot tables). It builds on
  **Java 17**; 1.20.6 and all 1.21.x build on Java 21.

[0.2.1]: https://github.com/ErikEdits/JustQuests/releases/tag/v0.2.1

## [0.2.0] - 2026-06-21

The GUI update — plus a big reach expansion.

### Added
- **In-game quest book (GUI).** Press **J** to open a quest book: browse
  quests (paged), see a quest's description, objectives with live progress,
  and rewards, and **accept/abandon** with a click. Vanilla-grey styling.
  - This is an **interim** GUI for singleplayer (it reads quest data
    directly). The final design will be chosen by a community Discord poll,
    and multiplayer sync lands with it.
- **Five more Minecraft versions:** 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10
  (NeoForge) — JustQuests now ships for **1.21 through 1.21.10** (11
  versions).

### Changed
- Build toolchain bumped (ModDevGradle 2.0.78 → 2.0.141) to support the
  newer Minecraft versions.

### Notes
- 1.21.11 is not included yet: its upstream NeoForge package is currently
  broken (produces an empty jar). It'll be added once that's fixed.
- The GUI keybind is unbound-safe and rebindable under Controls → "Open
  Quests".

[0.2.0]: https://github.com/ErikEdits/JustQuests/releases/tag/v0.2.0

## [0.1.12] - 2026-06-21

Content & language pack. Still command-only; the GUI comes in v0.2.

### Added
- **7 new bundled quests** (now 25 total), covering the previously
  unshowcased objective types: `home_builder` (place_block), `craftsman`
  (craft_item), `animal_friend` (tame_animal), `into_the_nether`
  (visit_dimension), `level_up` (reach_level), `prospector` (mine_block +
  a `loot_table` reward), `achiever` (gain_advancement). New categories:
  building, crafting, exploration, challenges.
- **Two more languages.** Every bundled quest now ships in **English,
  German, French and Spanish** (was English + German).
- **Example datapack** in the repo
  (`docs/example-datapack/`) — a ready-to-use datapack with one quest per
  objective & reward type, plus item tags, `mode: any`, a prerequisite
  chain, a repeatable quest and a multi-language quest, with a README.

[0.1.12]: https://github.com/ErikEdits/JustQuests/releases/tag/v0.1.12

## [0.1.11] - 2026-06-21

Bug-fix & maintenance pass. Still command-only; the GUI comes in v0.2.

### Fixed
- **Cancelled actions no longer advance quests.** `place_block`,
  `mine_block`, `kill_mob`, `tame_animal` and `breed_animal` now ignore
  events that get cancelled by claim/protection mods, anti-cheat, totems,
  etc. (they run at lowest priority and skip cancelled events), so quests
  only count actions that actually happened.
- **`/quest stats` could show over 100%** when a player had completed
  quests that are no longer loaded — the completion percentage is now
  capped at 100%.
- **Hardened quest completion** against a quest disappearing mid-tick (e.g.
  a custom-quest reload): it's skipped instead of risking a crash.

### Removed
- The in-game **update notice** added in 0.1.10. Modrinth uploads are one
  version per jar (so players can pick any exact version), which the update
  checker can't compare — it would never fire correctly. Update via the
  Modrinth app as usual.

[0.1.11]: https://github.com/ErikEdits/JustQuests/releases/tag/v0.1.11

## [0.1.10] - 2026-06-20

Stats & feedback. Still command-only; the GUI comes in v0.2.

### Added
- **`/quest stats`** — your personal stats: completed/total (+%), active,
  a per-category breakdown, and your first/last completion date.
- **`/quest leaderboard`** — the server's top 10 players by quests
  completed (names resolved even for offline players).
- **Completion feedback** — a sound and an action-bar toast ("✓ <quest>")
  when you finish a quest. Both toggle in the per-world settings file
  (`completionSound`, `completionToast`).
- **Update notice** — OPs and the singleplayer host get a chat message when
  a newer version is on Modrinth (via NeoForge's built-in version checker
  reading Modrinth's update feed). Toggle: `updateNotice`. Public feed only,
  no tokens or data sent.

### Notes
- Planned for the future server/plugin edition: optional automatic updates
  on server start/restart (default on), with an OP on/off notice.

[0.1.10]: https://github.com/ErikEdits/JustQuests/releases/tag/v0.1.10

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
