# JustQuests — Generator v2: Build Specification (hand-off document)

> **Audience:** an autonomous engineering session (AI or human) that will build
> the JustQuests quest generator v2 **without further contact** with the
> maintainer. Everything you need is in this file. Where something is not
> specified, make the most sensible choice, and **write it down** in
> `DESIGN.md → Assumptions`.
>
> **You deliver:** one ZIP (see [§4](#4-deliverable-the-zip)) with a
> version-neutral generator core in plain Java 17 + resources + tests + docs.
> The maintainer's integrator (Claude Code) will then wire it into all 34
> builds of the mod. Your job ends at a clean, tested, documented core with a
> precisely defined API. **Do not modify the mod itself.**
>
> Language of all code, comments, docs and generated quest text: **English.**

---

## Table of contents

1. [Context: what JustQuests is](#1-context-what-justquests-is)
2. [Goal and scope of v2](#2-goal-and-scope-of-v2)
3. [Decisions already made — do not re-decide](#3-decisions-already-made--do-not-re-decide)
4. [Deliverable: the ZIP](#4-deliverable-the-zip)
5. [Hard technical constraints](#5-hard-technical-constraints)
6. [The quest JSON contract (what you must emit)](#6-the-quest-json-contract-what-you-must-emit)
7. [What each objective really counts (hook semantics)](#7-what-each-objective-really-counts-hook-semantics)
8. [Architecture and API — the endpoints](#8-architecture-and-api--the-endpoints)
9. [Intelligence requirements — the core of the work](#9-intelligence-requirements--the-core-of-the-work)
10. [Exclusive claiming and the one-active rule](#10-exclusive-claiming-and-the-one-active-rule)
11. [Rotation, persistence and migration](#11-rotation-persistence-and-migration)
12. [Difficulty levels](#12-difficulty-levels)
13. [Configuration](#13-configuration)
14. [Modded content: pick two big Modrinth mods](#14-modded-content-pick-two-big-modrinth-mods)
15. [Test-phase statistics](#15-test-phase-statistics)
16. [Testing requirements](#16-testing-requirements)
17. [What INTEGRATION.md must contain](#17-what-integrationmd-must-contain)
18. [Per-version API notes for the host adapter](#18-per-version-api-notes-for-the-host-adapter)
19. [Acceptance checklist](#19-acceptance-checklist)
20. [Appendix A — generator v1 as it exists today](#appendix-a--generator-v1-as-it-exists-today)
21. [Appendix B — current mod code map](#appendix-b--current-mod-code-map)
22. [Appendix C — glossary](#appendix-c--glossary)

---

## 1. Context: what JustQuests is

- **JustQuests** is a lightweight, datapack-driven quest mod for Minecraft
  (alternative to FTB Quests / HQM). Repository (public):
  **https://github.com/ErikEdits/JustQuests** — Modrinth slug `justquests`.
  License: **LGPL-3.0-only**. Author: ErikEdits. Personal-scale project.
- Quests are JSON. Three quest **sources** feed one `QuestManager`:
  1. **datapack / built-in quests** (bundled, can be hidden per world via the
     `mainQuests` setting),
  2. **custom quests** from `<world>/justquests/custom-quests.json`,
  3. **generated quests** — produced by the generator. **This is your part.**
- Players accept/abandon quests via `/quest accept|abandon <id>` or the quest
  book GUI (key **J**, which sends those same commands). Progress is tracked
  server-side and synced to the client.
- **34 builds**, each MC version has its **own full source tree**:

  | Loader | MC versions | Java | Quest book GUI |
  |---|---|---|---|
  | NeoForge | 1.20.4, 1.20.6, 1.21, 1.21.1 … 1.21.10 (13) | 17 (1.20.4) / 21 | all |
  | Fabric | 1.18.2, 1.19.2, 1.19.4, 1.20.1, 1.20.4, 1.20.6, 1.21, 1.21.1 … 1.21.10 (17) | 17 (≤1.20.4) / 21 | 1.20.1+ |
  | Forge | 1.18.2, 1.19.2, 1.19.4, 1.20.1 (4) | 17 | 1.20.1 |

  Minecraft classes differ between these versions (registries, tags,
  recipes, `ResourceLocation`, events…). That is exactly why the generator
  must be a **version-neutral core** that never touches Minecraft or loader
  classes: the identical core source is copied into all 34 trees, and each
  tree gets a thin **host adapter** (written by the integrator) that answers
  the core's questions.
- Fabric uses official Mojang mappings (Mojmap), so domain code is shared
  with NeoForge/Forge; only the loader layer differs.
- Current release: **0.2.5**. Generator v2 is expected to ship as **0.3.0**
  (the maintainer decides the final number).

## 2. Goal and scope of v2

Build a **much smarter procedural quest generator** that replaces v1
([Appendix A](#appendix-a--generator-v1-as-it-exists-today)). All four feature
areas are in scope, plus "intelligence":

| # | Feature | Summary |
|---|---|---|
| F1 | **More quest types** | Use every objective type that works fairly in a shared set (see [§7](#7-what-each-objective-really-counts-hook-semantics)), not just v1's four. Includes multi-objective, themed quests. |
| F2 | **Exclusive claiming + one active generated quest per player** | First come, first served; a claimed quest is locked for everyone else; each player may hold at most one unfinished generated quest. Full state machine in [§10](#10-exclusive-claiming-and-the-one-active-rule). |
| F3 | **Difficulty levels Easy / Normal / Hard** | One setting per world/server, set by OP, applies to everyone; scales effort, tiers, objective count and rewards ([§12](#12-difficulty-levels)). |
| F4 | **Modded content** | A data-driven **mod profile** system. Ship profiles for **two big Modrinth mods that you choose** ([§14](#14-modded-content-pick-two-big-modrinth-mods)). Activates only when the mod is installed. |
| F5 | **Intelligence** | Achievability modelling, effort-based counts, value-based rewards, progression awareness, variety balancing, explainability, optional self-calibration ([§9](#9-intelligence-requirements--the-core-of-the-work)). |

**Non-goals (do NOT do these):**
- No AI/LLM, no network access, no telemetry, no downloads, no external
  services. Zero runtime dependencies beyond the JDK and Gson.
- No changes to the mod's 34 source trees, its quest JSON format, its
  objective/reward implementations, commands, GUI or networking. (You may
  *recommend* mod changes in `INTEGRATION.md → Optional mod enhancements`.)
- No new objective or reward **types** (the mod cannot count them).
- No per-player generation — one shared set per world/server.
- No languages other than English.
- Do not bundle, depend on, or copy code/assets from the two mods. Only
  reference their item/block/entity **IDs** (facts), detected at runtime.

## 3. Decisions already made — do not re-decide

These were decided by the maintainer (June 2026 design rounds + this hand-off):

1. **Own procedural code**, weighted random + templates + rules. No AI.
2. **12-hour rotation** based on the **real clock** (works across game
   restarts: downtime is caught up). Details in [§11](#11-rotation-persistence-and-migration).
3. **No repeats within a rolling 6-day window** (by quest signature).
4. Generated quests are **switchable per world** (`generatedQuests` setting).
   Custom quests never rotate.
5. **One shared set** for everyone in the world/server, **loosely synced**.
6. **Exclusive claiming**: once a player accepts a generated quest, nobody
   else can accept it. **One active generated quest per player.**
7. **No reroll for players.** Only OPs can reroll (`/quest reroll`).
8. **Accepted quests survive the rotation** until finished (default).
   Whether a difficulty may expire them is a test-phase decision → implement
   as config, default "never expire".
9. **Difficulty: Easy / Normal / Hard**, one value per world, set by OP,
   same for all players. Concrete effects are tunable via data (test phase).
10. **Rewards are chosen by the generator** inside data-driven limits (value
    model), not from a fixed hand list.
11. Quest **descriptions/titles from templates**, English only.
12. **Amount per cycle** is configurable (default 5); a hard cap protects
    servers (default max 20).
13. **Modded support starts with exactly two big mods** chosen by you from
    Modrinth; the system must make adding more mods a data-only task.
14. **Security**: the mod never contains tokens/webhooks; the generator
    never emits `justquests:command` rewards.

## 4. Deliverable: the ZIP

Deliver **`justquests-generator-v2.zip`** containing one standalone Gradle
project:

```
justquests-generator-v2/
├─ README.md               What it is, how to build/test, file tour
├─ INTEGRATION.md          Exact wiring contract for the mod (see §17)
├─ DESIGN.md               Algorithms, formulas, data model, tuning guide,
│                          "Assumptions" section, known limitations
├─ MODS.md                 The two chosen mods: research + coverage (see §14)
├─ CHANGELOG-snippet.md    Player-facing release notes text
├─ build.gradle(.kts), settings.gradle(.kts), gradlew, gradlew.bat, gradle/
├─ src/main/java/com/erikedits/justquests/generator/v2/...   the core
├─ src/main/resources/justquests_genv2/...                   data files
├─ src/test/java/com/erikedits/justquests/generator/v2/...   JUnit 5 tests
├─ reference-adapter/                                        OPTIONAL (see below)
│  └─ neoforge-1.21.1/…  a reference GeneratorHost implementation
└─ samples/                generated example sets (JSON) — see §16
```

Build requirements:
- `./gradlew build` compiles and runs all tests green on a clean machine.
- Core compiled with `options.release = 17` (must run on Java 17 **and** 21).
- Dependencies: `compileOnly "com.google.code.gson:gson:2.8.8"` (Minecraft
  provides Gson at runtime), `testImplementation` Gson 2.8.8 + JUnit 5.
  **Nothing else** on the main classpath.
- The reference adapter is **optional but recommended**: a NeoForge 1.21.1
  implementation of the host interfaces. If you cannot compile it against
  NeoForge (it needs a Minecraft toolchain), keep it outside the Gradle
  build and mark every file header `// REFERENCE ONLY — NOT COMPILED`.
- Put `samples/` output files next to a short `samples/README.md` that says
  which seed/difficulty/mods produced them.

## 5. Hard technical constraints

**Language level**
- Java **17** source & target. Allowed: records, sealed types, text blocks,
  `instanceof` pattern matching, `switch` *expressions* on enums/strings.
- Forbidden: pattern matching in `switch`, record patterns, sequenced
  collections (`List.reversed()`, `getFirst()`…), virtual threads, string
  templates, any API newer than Java 17.

**Dependencies**
- Only `java.*` and **Gson 2.8.8 API** (`com.google.gson.*`). Do not use
  Gson APIs added after 2.8.8 (e.g. `JsonArray.asList()`,
  `JsonObject.asMap()`). `JsonParser.parseString` is fine.
- No `net.minecraft.*`, `com.mojang.*`, `net.neoforged.*`,
  `net.minecraftforge.*`, `net.fabricmc.*`, no Guava, no SLF4J/log4j, no
  fastutil. A unit test must enforce this ([§16](#16-testing-requirements)).
- No reflection into game classes. No `System.exit`, no threads, no timers.

**Runtime model**
- One `QuestGeneratorV2` **instance per world/server**. No static mutable
  state (server restarts and world switches in singleplayer create a new
  instance).
- **Single-threaded**: every call arrives on the Minecraft server thread.
  The core is not required to be thread-safe; document that.
- **No file/network I/O** except through `StateStore` (world files) and
  reading your own bundled resources via
  `QuestGeneratorV2.class.getResourceAsStream("/justquests_genv2/...")`.
  Keep resources under `/justquests_genv2/` (a directory that is **not** a
  Java package, so module systems on NeoForge/Forge do not encapsulate it).
- **Never throw** out of API methods during normal operation. Bad data,
  missing mods, unknown IDs → skip, log a warning via `GenLog`, continue.
  Only programmer errors (null arguments) may throw `NullPointerException`
  or `IllegalArgumentException`.
- **Deterministic**: for identical inputs (seed, config, catalog, context,
  state) the generated set is byte-identical. All randomness through one
  seeded `java.util.SplittableRandom` or `java.util.Random` per generation.
- **Performance budget**: `tick()` when no rotation is due: O(1), < 0.1 ms.
  Generating a set of 20 quests incl. validation: < 50 ms on a mid-range PC.
  Loading all catalogs: < 200 ms, once at `start()`.
- State files must stay small (< 256 KB for months of play; prune history).

**Package layout (stable surface vs. free internals)**

```
com.erikedits.justquests.generator.v2          → QuestGeneratorV2 (facade)
com.erikedits.justquests.generator.v2.api      → all interfaces/records/enums the mod sees
com.erikedits.justquests.generator.v2.internal → your implementation (free design)
```

Only the facade and `api.*` are the contract. Everything in `internal` may
change freely later.

## 6. The quest JSON contract (what you must emit)

The mod parses every generated quest with the **same codec as datapack
quests**. Emit exactly this format (verified against the mod's codecs).
Unknown keys must **not** be emitted (keep generator metadata in your own
state, keyed by quest id).

### 6.1 Quest object

| Field | Type | Required | Notes for generated quests |
|---|---|---|---|
| `title` | string (or map lang→string) | **yes** | **Plain English string.** Keep ≤ 22 chars ideally, hard max 32 (the GUI list row shows ~12 chars before "…", the detail pane wraps). Most important word first. |
| `description` | string | no | English, ≤ 140 chars. Use it for a helpful hint (where/how). |
| `category` | string | no (default `"datapack"`) | **Always `"generated"`.** |
| `mode` | `"all"` \| `"any"` | no (default `"all"`) | `all` = every objective needed. `any` rarely makes sense for generated quests (progress of alternatives does not add up). |
| `requires` | list of quest ids | no | **Never emit.** |
| `repeatable` | bool | no (default false) | **Never emit / always false.** |
| `cooldown_hours` | int | no | **Never emit.** |
| `sort` | int | no (default 0) | Order inside the category. Use it: quick/easy quests first (e.g. estimated minutes). |
| `objectives` | list | **yes** | 1–3 objectives (see §6.2). Must not be empty. |
| `rewards` | list | **yes** | 1–3 rewards (see §6.3). |

Loader rules the mod applies (a quest failing these is **skipped**): at least
one objective; every objective with a count must have `count > 0`.

### 6.2 Objective types

Common shape: `{ "type": "justquests:<name>", ...fields }`.

| `type` | Fields | Tag (`#ns:path`) allowed? | Use in generator? |
|---|---|---|---|
| `justquests:collect_item` | `item`, `count` | **yes** (item tag) | **yes** |
| `justquests:mine_block` | `block`, `count` | no (single block id) | **yes** |
| `justquests:craft_item` | `item`, `count` | **yes** | **yes** |
| `justquests:smelt_item` | `item`, `count` | **yes** | **yes** (item = the **output**) |
| `justquests:kill_mob` | `entity`, `count` | no | **yes** |
| `justquests:breed_animal` | `entity`, `count` | no | **yes** |
| `justquests:tame_animal` | `entity`, `count` | no | **yes** (restricted, §7) |
| `justquests:consume_item` | `item`, `count` | **yes** | **yes** |
| `justquests:place_block` | `block`, `count` | no | **yes** |
| `justquests:visit_dimension` | `dimension` | no | **yes** (sparingly) |
| `justquests:gain_advancement` | `advancement` | no | **NO** — see §7 |
| `justquests:reach_level` | `level` | no | **NO** — see §7 |
| `justquests:reach_location` | `dimension` (opt), `x`, `y`, `z`, `radius` (opt, default 4) | no | **NO** — see §7 |

IDs are namespaced strings (`minecraft:oak_log`). Tags are written with a
leading `#` (`#minecraft:logs`). Whether an objective type accepts tags is a
**host capability** (`HostCapabilities.supportsTag(type)`, §8) — today only
the four item-based types do; never emit a tag where the host says no.

### 6.3 Reward types

| `type` | Fields | Use in generator? |
|---|---|---|
| `justquests:give_item` | `item` (single id, **no tag**), `count` | **yes** — main reward. Keep `count ≤ maxStackSize` of the item (1 for unstackables). |
| `justquests:xp` | `amount` — **experience points**, not levels | **yes** — typical 10–300. (Level 0→30 ≈ 1395 points.) |
| `justquests:effect` | `effect` (id, e.g. `minecraft:speed`), `seconds` (default 30), `amplifier` (default 0) | optional flavour, beneficial effects only |
| `justquests:loot_table` | `loot_table` (id, e.g. `minecraft:chests/simple_dungeon`) | optional "mystery reward" (Hard), curated list only, verify it exists via host if possible |
| `justquests:message` | `message` (string) | optional flavour line |
| `justquests:command` | `command` | **NEVER** (security) |

### 6.4 Quest ids

- Namespace `justquests`, path under `gen/`:
  **`justquests:gen/<cycleId>_<n>`** where `cycleId` is the cycle start in
  epoch **seconds** and `n` a 0-based index (rerolls continue the index:
  `…_5`, `…_6`). Allowed path characters: `a-z 0-9 _ - . /`.
- Ids must be unique forever within a world (cycle start seconds guarantee
  that across cycles).

### 6.5 Example (single objective)

```json
{
  "title": "Iron Supply",
  "description": "Smelt raw iron in a furnace. Iron ore needs a stone pickaxe or better.",
  "category": "generated",
  "sort": 14,
  "objectives": [
    { "type": "justquests:smelt_item", "item": "minecraft:iron_ingot", "count": 24 }
  ],
  "rewards": [
    { "type": "justquests:give_item", "item": "minecraft:emerald", "count": 5 },
    { "type": "justquests:xp", "amount": 60 }
  ]
}
```

### 6.6 Example (themed, two objectives, Hard)

```json
{
  "title": "Nether Expedition",
  "description": "Bring back quartz and blaze rods. Blazes live in Nether fortresses.",
  "category": "generated",
  "sort": 38,
  "mode": "all",
  "objectives": [
    { "type": "justquests:mine_block", "block": "minecraft:nether_quartz_ore", "count": 16 },
    { "type": "justquests:collect_item", "item": "minecraft:blaze_rod", "count": 6 }
  ],
  "rewards": [
    { "type": "justquests:give_item", "item": "minecraft:diamond", "count": 2 },
    { "type": "justquests:xp", "amount": 200 }
  ]
}
```

## 7. What each objective really counts (hook semantics)

The generator only produces fair quests if it knows **what the mod actually
detects**, on **every** loader. This table is the ground truth (read from the
mod's code, NeoForge events vs. Fabric mixins):

| Objective | Counts when… | NeoForge / Forge hook | Fabric hook | Consequences for the generator |
|---|---|---|---|---|
| `collect_item` | the player **picks up an item entity** from the ground | `ItemEntityPickupEvent.Post` (1.18–1.20.4: `PlayerEvent.ItemPickupEvent`) | mixin `LivingEntity.take` | Only items that **drop as item entities**: mined block drops, mob drops, harvested crops, fishing. **Not**: items crafted into the inventory, taken from chests/furnaces/villager trades. Model **drops**, not blocks: mining `iron_ore` drops `raw_iron` (without Silk Touch). |
| `mine_block` | the player **breaks** the block | `BlockEvent.BreakEvent` | `PlayerBlockBreakEvents.AFTER` | Target is the **block id broken** (not the drop). No tag support → variants are separate blocks (`iron_ore` vs `deepslate_iron_ore`); pick the variant that is common at the relevant depth, or state it in the description. |
| `craft_item` | the player takes the result from a **crafting grid** (crafting table or 2×2) | `PlayerEvent.ItemCraftedEvent` | mixin `ResultSlot.onTake` | Only **crafting-table recipes**. Stonecutter, smithing table, loom, cartography, anvils and **modded machines are not guaranteed** to count → never generate `craft_item` for items obtainable only there. |
| `smelt_item` | the player takes the **output** from a furnace / blast furnace / smoker | `PlayerEvent.ItemSmeltedEvent` | mixin `FurnaceResultSlot.onTake` | Target = the **smelted result** (`iron_ingot`, `cooked_beef`), counted by stack size taken. Modded cooking machines don't count. |
| `kill_mob` | a mob dies and the killer is the player (incl. the player's projectiles) | `LivingDeathEvent` | `ServerLivingEntityEvents.AFTER_DEATH` (1.18.2: mixin) | Model biome/structure/time dependency (husks = deserts, slimes = swamps/slime chunks, blazes = fortresses, witches rare). Never target bosses, passive-only rarities, or mobs that don't exist in all biomes without saying so in the description. |
| `breed_animal` | two animals produce a baby, caused by the player | `BabyEntitySpawnEvent` (parent A type) | mixin `Animal.spawnChildFromBreeding` (self type) | Target = the **parent's** entity type. Only `Animal` subclasses (cow, sheep, pig, chicken, rabbit, wolf, cat, horse, …). **Not villagers** (Fabric doesn't count them). |
| `tame_animal` | the player tames an animal | `AnimalTameEvent` | mixin `TamableAnimal.tame` | **Only `TamableAnimal` subclasses: wolf, cat, parrot** (+ modded `TamableAnimal`s explicitly flagged). **Horses/donkeys/llamas are not counted on Fabric** → forbidden. |
| `consume_item` | the player **finishes using** an item (eating, drinking) | `LivingEntityUseItemEvent.Finish` | mixin `LivingEntity.completeUsingItem` | Foods, potions, milk, honey. Good target for modded meals that can't be counted as crafted (e.g. cooked in a modded pot). |
| `place_block` | the player places a block from a block item | `BlockEvent.EntityPlaceEvent` | mixin `BlockItem.place` | Normal block items only. Cheap filler — use sparingly, mostly Easy. |
| `visit_dimension` | the player changes to the given dimension | `PlayerChangedDimensionEvent` | `ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD` | Repeatable (every entry counts). Only if the dimension exists (`ContentView.dimensionExists`) and is unlocked by progression (§9.7). Low weight. |
| `gain_advancement` | advancement awarded | `AdvancementEvent.AdvancementEarnEvent` | mixin `PlayerAdvancements.award` | **Forbidden**: an advancement can only be earned **once per player** → impossible for anyone who already has it. Unfair in a shared set. |
| `reach_level` | player XP level ≥ N (polled 1×/s) | tick | tick | **Forbidden**: instantly completes for high-level players; meaningless in a shared set. |
| `reach_location` | player within radius of x/y/z | tick | tick | **Forbidden**: the generator has no meaningful coordinates. |

**General rule:** if you are not sure a content entry's acquisition path
triggers the chosen hook on **all three loaders**, do not generate that
combination. Record such exclusions in the catalog (`note` field) and in
`MODS.md`.

## 8. Architecture and API — the endpoints

### 8.1 Big picture

```
            ┌──────────────────── one Minecraft build (×34) ─────────────────────┐
            │                                                                     │
  mod code ─┼─► QuestGeneratorV2 (facade, pure Java)  ◄── your deliverable        │
 (commands, │        │   uses (endpoints IN)                                    │
  events,   │        ▼                                                            │
  storage)  │   GeneratorHost ──► ContentView  (registries, tags, mods, recipes)  │
            │        │        ──► WorldContext (seed, game day, progression)     │
            │        │        ──► StateStore   (world files)                     │
            │        │        ──► QuestValidator (runs the mod's real codec)     │
            │        │        ──► GenLog, clock                                   │
            │   implemented per build by the integrator (thin adapter)           │
            └─────────────────────────────────────────────────────────────────────┘
```

- **Endpoints IN** (`api` interfaces the mod implements): the only way the
  core learns about the game.
- **Endpoints OUT** (`QuestGeneratorV2` methods the mod calls): lifecycle,
  the served quest set, claims, debugging, stats.

You must implement the facade **exactly** with these signatures (you may add
methods; do not remove or change these). Javadoc every public type/method
with its contract (when to call, thread, side effects, return semantics).

### 8.2 Endpoints IN — host interfaces (package `...generator.v2.api`)

```java
public interface GeneratorHost {
    ContentView content();
    WorldContext world();
    StateStore store();
    QuestValidator validator();
    HostCapabilities capabilities();
    GenLog log();
    /** Real wall-clock time in epoch milliseconds (injectable for tests). */
    long currentTimeMillis();
}

public enum TriState { YES, NO, UNKNOWN }
public enum ContentKind { ITEM, BLOCK, ENTITY, DIMENSION }

public interface ContentView {
    /** "neoforge" | "fabric" | "forge" — informational + profile filters. */
    String loaderName();
    /** e.g. "1.21.1" — informational + profile filters. */
    String minecraftVersion();
    /** Loader mod-list check (e.g. "farmersdelight"). */
    boolean isModLoaded(String modId);

    boolean itemExists(String id);          // registry contains key
    boolean blockExists(String id);
    boolean entityTypeExists(String id);
    /** Dimension keys known to the RUNNING server (e.g. "minecraft:the_nether"). */
    boolean dimensionExists(String id);

    /** Resolved members of a tag; empty (never null) if unknown or empty.
     *  Tags bind after datapack load — the core only calls these at/after start(). */
    java.util.Set<String> itemTagMembers(String tagId);   // tagId without '#'
    java.util.Set<String> blockTagMembers(String tagId);
    java.util.Set<String> entityTypeTagMembers(String tagId);

    /** A crafting-grid recipe (table or 2x2) outputs this item. */
    TriState hasCraftingRecipe(String itemId);
    /** A furnace / blast furnace / smoker recipe outputs this item. */
    TriState hasSmeltingRecipe(String outputItemId);
    /** Default max stack size (64, 16, 1); -1 if unknown. */
    int maxStackSize(String itemId);
    /** Entity type is an Animal subclass (breedable by players). */
    TriState isBreedableAnimal(String entityTypeId);
    /** Entity type is a TamableAnimal subclass (wolf/cat/parrot style). */
    TriState isTamableAnimal(String entityTypeId);
    /** Item finishes a "use" action (food, drink, potion, milk). */
    TriState isConsumable(String itemId);
    /** English display name if resolvable on this side; null otherwise.
     *  NOTE: dedicated servers only know vanilla names — modded names are
     *  usually null. Core must fall back to catalog names, then prettify ids. */
    String englishName(ContentKind kind, String id);
}

public interface WorldContext {
    long worldSeed();
    /** Overworld game time / 24000 (in-game days since world creation). */
    long gameDay();
    int onlinePlayerCount();
    /** Players that have JustQuests data in this world (≥ online). */
    int knownPlayerCount();
    /** Share 0..1 of ONLINE players that have the advancement; -1 if no player online or unknown. */
    double onlineShareWithAdvancement(String advancementId);
    /** Server/singleplayer flag (claims still apply in singleplayer). */
    boolean isSingleplayer();
}

public interface StateStore {
    /** Read <world>/justquests/<fileName>; empty if missing/unreadable. */
    java.util.Optional<String> read(String fileName);
    /** Write atomically (temp file + move). Never throws; log on failure. */
    void write(String fileName, String content);
}

public interface QuestValidator {
    /** Runs the mod's real Quest codec + loader rules on the JSON. */
    ValidationResult validate(String questId, com.google.gson.JsonObject questJson);
}
public record ValidationResult(boolean ok, String message) {}

public interface HostCapabilities {
    /** True if the objective type accepts "#tag" targets on this build
     *  (today: collect_item, craft_item, smelt_item, consume_item). */
    boolean supportsTag(String objectiveType);
    /** Objective/reward type ids the running mod knows (e.g. "justquests:smelt_item"). */
    java.util.Set<String> objectiveTypes();
    java.util.Set<String> rewardTypes();
}

public interface GenLog {
    void info(String msg);
    void warn(String msg);
    void error(String msg, Throwable t);   // t may be null
}
```

**Design rule:** the core must produce good results even if **every
`TriState` answer is `UNKNOWN`** and `englishName` always returns `null`
(some versions cannot answer cheaply — see [§18](#18-per-version-api-notes-for-the-host-adapter)).
Host answers refine the catalog; they never replace it. A definite `NO`
from the host overrides a catalog claim (e.g. catalog says craftable, host
says `NO` → drop that craft target).

### 8.3 Endpoints OUT — the facade

```java
package com.erikedits.justquests.generator.v2;

public final class QuestGeneratorV2 {
    public QuestGeneratorV2(GeneratorHost host, GeneratorConfig config);

    /** Call once at server start AFTER datapacks/tags are loaded.
     *  Loads bundled data + world state, migrates v1 state, reconciles
     *  claims with the players' real active quests (see §10.5).
     *  activeGeneratedQuests: questId -> holder UUID for every generated
     *  quest that is currently ACTIVE in any player's saved data.
     *  Returns quest ids the mod must remove from player data because their
     *  definition no longer exists (dead quests). */
    public StartResult start(java.util.Map<String, java.util.UUID> activeGeneratedQuests);

    /** Call periodically (the mod calls it every ~5 minutes). Cheap when
     *  nothing is due. Rotates when a cycle boundary passed; releases expired
     *  claims if claim expiry is configured. */
    public RotationResult tick();

    /** OP reroll: replace all UNCLAIMED quests of the current cycle now.
     *  Claimed and completed quests are kept. */
    public RotationResult reroll();

    /** Apply new settings (after /quest reload, /quest difficulty ...).
     *  Content/difficulty changes take effect at the next rotation or reroll;
     *  enabling/disabling takes effect immediately (servedQuests changes). */
    public void updateConfig(GeneratorConfig config);

    /** Everything the mod must register right now: the current cycle's quests
     *  plus RETAINED quests (claimed, not yet finished, from older cycles).
     *  Map questId ("justquests:gen/...") -> quest JSON (§6). Iteration order
     *  stable (sort order). Empty if disabled. Returns defensive copies. */
    public java.util.Map<String, com.google.gson.JsonObject> servedQuests();

    public boolean isGenerated(String questId);

    /** Call from the accept path AFTER the mod's own eligibility checks and
     *  BEFORE adding the quest to the player. OK / ALREADY_YOURS => proceed. */
    public ClaimResult tryClaim(String questId, java.util.UUID player);

    /** Player abandoned the quest (via /quest abandon or GUI). */
    public void onAbandon(String questId, java.util.UUID player);

    /** Player completed the quest (rewards already granted by the mod). */
    public void onComplete(String questId, java.util.UUID player);

    /** Release every claim held by this player (admin reset of player data). */
    public java.util.List<String> releaseAllFor(java.util.UUID player);

    /** OP tool: force-release one claim; returns the former holder if any. */
    public java.util.Optional<java.util.UUID> forceRelease(String questId);

    /** Claim state for list/GUI/sync. AVAILABLE for unknown ids. */
    public ClaimView claim(String questId);
    public java.util.Map<String, ClaimView> claims();

    /** Preview the NEXT cycle without applying it (OP debug). */
    public java.util.List<com.google.gson.JsonObject> preview(int count);

    /** Human-readable, multi-line explanation of why/how a quest was made. */
    public String explain(String questId);

    /** Test-phase statistics summary (§15). */
    public StatsSummary stats();

    /** Persist state now (the core also saves after every state change). */
    public void save();

    /** Server stopping: final save; instance is unusable afterwards. */
    public void stop();
}
```

Result/value types (`api` package, records/enums — design the exact fields,
but these must exist and carry at least this information):

```java
public enum Difficulty { EASY, NORMAL, HARD }

public enum ClaimState { AVAILABLE, CLAIMED, COMPLETED }

public record ClaimView(ClaimState state, java.util.UUID holder /*nullable*/,
                        long claimedAtMillis, long completedAtMillis) {}

public enum ClaimResult {
    OK,                   // claimed now → mod adds quest to player
    ALREADY_YOURS,        // player already holds it → mod proceeds (idempotent)
    CLAIMED_BY_OTHER,     // deny: "Another player already took this quest."
    HAS_ACTIVE_GENERATED, // deny: "Finish your current generated quest first."
    COMPLETED,            // deny: "This quest was already completed."
    NOT_GENERATED,        // not a generated quest → mod ignores claims
    NOT_SERVED,           // generated id that is no longer offered → deny
    DISABLED              // generator disabled → deny for generated ids
}

public record ExpiredClaim(String questId, java.util.UUID holder) {}

public record RotationResult(boolean changed, String reason /* "cycle", "catch-up", "reroll", "expiry", "none" */,
                             long cycleId,
                             java.util.List<String> added,
                             java.util.List<String> retained,
                             java.util.List<String> removed,
                             java.util.List<ExpiredClaim> expiredClaims) {}

public record StartResult(java.util.List<String> deadQuestIds,
                          java.util.List<ExpiredClaim> releasedClaims,
                          RotationResult initialRotation) {}

public record StatsSummary(/* see §15 */) {}

public record GeneratorConfig(/* see §13 */) {}
```

**What the mod does with results** (describe this in `INTEGRATION.md`):
- After `start()`, `tick()` with `changed == true`, `reroll()`, or an
  enabling `updateConfig()`: parse `servedQuests()` with the mod's codec,
  call `QuestManager.setGeneratedQuests(...)`, then sync all players.
- For every `ExpiredClaim`: remove the quest from that player's active
  quests (as if abandoned) and notify the player if online.
- For every `deadQuestIds` entry: remove it from any player's active quests.

### 8.4 Lifecycle sequence

```
server start ─► (datapacks + tags loaded) ─► new QuestGeneratorV2(host, cfg)
             ─► start(activeGeneratedQuestsFromPlayerData)
             ─► register servedQuests(), sync
every ~5 min ─► tick() ─► if changed: re-register, sync; apply expiredClaims
/quest accept ─► mod eligibility ─► tryClaim ─► OK? add to player : deny msg
/quest abandon ─► onAbandon ─► (quest becomes AVAILABLE if still in the current cycle)
quest completed ─► mod grants rewards ─► onComplete
/quest reroll (OP) ─► reroll() ─► re-register, sync
/quest reload / settings change ─► updateConfig(newCfg)
admin reset player ─► releaseAllFor(uuid)
server stop ─► stop()
```

## 9. Intelligence requirements — the core of the work

"Very intelligent" means, concretely and testably: **every generated quest is
achievable on the running build, costs roughly the intended effort, pays a
fair reward, fits the world's progression, and a set is varied.** v1 fails
several of these (see [Appendix A](#appendix-a--generator-v1-as-it-exists-today)).

### 9.1 Content catalog (data, not code)

Bundled JSON files under `/justquests_genv2/catalog/`:
- `vanilla.json` — the vanilla content model (must be thorough: aim for
  **≥ 150 usable targets** across all generator objective types and tiers).
- `profiles/<modid>.json` — one per supported mod (two in this delivery).

Design the schema yourself, but each **resource entry** must at least model:

| Field | Meaning |
|---|---|
| `key` | stable internal name (`iron`, `oak_wood`, `zombie`) |
| `family` | variety group (`metals`, `wood`, `undead`, `farming`, …) — at most one quest per family per set |
| `tier` | 0 surface · 1 stone/iron age · 2 deep caves/diamond · 3 Nether · 4 End |
| `dimension` | where it is obtained (`minecraft:overworld`, …, or a modded dimension id) |
| `tool` | minimum tool (`none`, `wood`, `stone`, `iron`, `diamond`, `netherite`, `shears`, `silk_touch`, `fishing_rod`, …) |
| `rarity` / `biome` hints | e.g. `desert`, `swamp`, `ocean`, `fortress`, `rare` — used for effort and descriptions |
| `targets` | per objective type: the exact **id(s)** for that objective (block for `mine_block`, **drop item** for `collect_item`, **output** for `smelt_item`, …), optional **alternative ids** tried in order (for renamed/versioned ids), per-unit **effort** (minutes for an average player at that tier), `min`/`max` count bounds |
| `requires` | prerequisite resources/tiers (e.g. blaze rods need Nether access) |
| `notes` | why a combination is excluded / special handling |
| `since` | informational MC version; **runtime existence checks are authoritative** |

Also bundled:
- `/justquests_genv2/rewards.json` — reward table: item id, **value per
  unit**, tier, max count, stackable flag, optional families to avoid.
- `/justquests_genv2/templates.json` — English text templates (titles,
  descriptions, hints, theme names), many variants per objective type.
- `/justquests_genv2/themes.json` — multi-objective archetypes (§9.5).
- `/justquests_genv2/balance.json` — all tunable numbers (§12), so the test
  phase can tune without code changes.
- `/justquests_genv2/tags.json` — tag concept → candidate tag ids per
  convention (§9.2).

Optional (SHOULD): the core also reads **world-level override files** via
`StateStore` if present — `generator_v2/balance.json`, extra
`generator_v2/profiles/*.json` — so server owners can tune or add mods
without rebuilding. Document the merge rules.

### 9.2 Achievability pipeline

For every candidate `(objective type, target id)`, all must pass, in this
order, otherwise the candidate is dropped (and counted in explain/stats):

1. **Type allowed**: generator-usable type (§7) **and** present in
   `HostCapabilities.objectiveTypes()`.
2. **Profile active**: vanilla, or the profile's mod `isModLoaded` **and**
   the profile's loader/version filters match.
3. **Id exists** on this build (`itemExists` / `blockExists` /
   `entityTypeExists` / `dimensionExists`); try alternative ids in order;
   first existing wins.
4. **Hook-compatible** (§7): e.g. `tame_animal` only if catalog flags it
   tamable AND host `isTamableAnimal != NO`; `breed_animal` only if
   `isBreedableAnimal != NO`; `craft_item` only if catalog says
   crafting-grid AND host `hasCraftingRecipe != NO`; `smelt_item` only if
   `hasSmeltingRecipe != NO`; `consume_item` only if `isConsumable != NO`;
   `collect_item` only for real drops.
5. **Progression-unlocked** (§9.7) and **tier ≤ difficulty cap** (§12).
6. **Not in history** (6-day signature window) and not used in this set.
7. After building the full quest JSON: **`QuestValidator.validate` passes.**
   A failing quest is discarded, logged once, and the generator picks a
   replacement.

**Tags across loaders/versions:** tag naming differs by loader and version,
roughly: Forge 1.18–1.20.1 `forge:ingots/iron`; older Fabric `c:iron_ingots`;
newer NeoForge/Fabric `c:ingots/iron`; NeoForge 1.20.4 still `forge:` style.
The exact switch-over versions vary (and mods add their own tags) — which is
precisely why you must never hard-code one convention: `tags.json` maps a concept
to a candidate list, the core uses the first candidate with non-empty
members on this build. Tags are useful for (a) item-tag objectives
("collect 32 of any `#minecraft:logs`"), (b) resolving a mod's ids per
loader, (c) optional discovery inside a profile's namespace (SHOULD, with
strict filters and only if the mod profile allows it).

### 9.3 Effort and count model

- Each target has **effort per unit** (minutes). Quest effort = Σ over
  objectives of `count × effortPerUnit × modifiers`.
- **Modifiers**: rarity/biome (e.g. ×1.5 for desert-only mobs), tool tier
  above the world's current progression, dimension travel overhead (fixed
  minutes for Nether/End quests), multi-objective discount/premium.
- **Count selection**: `count = niceRound(targetMinutes / (effortPerUnit ×
  modifiers))`, clamped to the target's `min`/`max`, where `targetMinutes`
  is drawn from the difficulty's range (§12).
- **Nice numbers**: round to human-friendly values (1, 2, 3, 4, 5, 6, 8, 10,
  12, 16, 20, 24, 32, 40, 48, 64, 80, 96, 128); for stack-64 items prefer
  multiples of 8/16; for stack-16 items multiples of 4; unstackables small
  integers (1–5).
- The **estimated minutes** of every quest are stored (state + stats) and
  shown by `explain`.

### 9.4 Reward valuation

- **Budget** = estimated minutes × `rewardRate(difficulty)` (value units ≈
  minutes of average play). Clamp to a per-quest min/max from `balance.json`.
- Build 1–3 rewards whose **total value ≈ budget** (±20 %): usually one item
  reward + XP; sometimes (Hard) an effect or a curated loot table.
- **Never** reward the objective's target item or anything of the same
  `family` ("collect iron → get iron" is forbidden).
- **Reward tier ≤ quest tier + 1**; no diamonds on Easy tier-0 quests.
- Item counts respect `maxStackSize` (host) or the reward table's stack
  field; unstackables count 1.
- Modded rewards only from an **active** profile and only if the id exists.
- XP amount in **points**; derive from the remaining budget (not random).

### 9.5 Composition, themes and variety

- **Objectives per quest** follows the difficulty distribution (§12).
- **Themes** (`themes.json`): coherent multi-objective archetypes with a
  name pool, e.g. *Blacksmith's Order* (smelt iron ingots + craft an iron
  tool), *Harvest Festival* (collect wheat + collect carrots/potatoes),
  *Monster Patrol* (kill two undead types), *Ranch Hand* (breed cows +
  breed sheep), *Nether Expedition* (quartz + blaze rods), plus themes
  that mix vanilla with an active mod profile. Objectives inside a quest
  must not contradict each other (same dimension/tier band, sensible
  together).
- **Set rules** (for a set of N quests, all tunable):
  - at least `min(N, 3)` different objective types;
  - no objective type in more than 40 % of the set;
  - at most one quest per `family`;
  - at least one "quick" quest (≤ lower third of the difficulty's time range);
  - when ≥ 1 mod profile is active: target `moddedShare` (default 35 %) of
    quests using modded content, and **at least one quest per active mod**
    when N ≥ 5;
  - no duplicate targets across the set.
- **Graceful relaxation**: if the pool is too small, relax rules in a
  documented order (e.g. family rule → type share → modded share → history
  last) and log what was relaxed. Never loop forever, never throw; if
  fewer than N valid quests exist, return fewer.

### 9.6 Text generation (English)

- Titles short and specific (≤ 22 chars ideal, ≤ 32 max), varied via
  template pools; no two titles in a set identical.
- Descriptions give **useful hints** from the catalog: tool needed, where
  (biome/structure/dimension/depth), Silk Touch/shears, smelt vs collect,
  "any log counts" for tags.
- Names: catalog `name` → `ContentView.englishName` → prettified id
  (`raw_iron` → "Raw Iron", mod namespace dropped).
- No placeholders left in output (test enforces).

### 9.7 Progression awareness

Because the set is shared, adapt to the **world**, not one player:
- Unlock tier 3 (Nether) when any of: an online share with advancement
  `minecraft:story/enter_the_nether` ≥ `balance.netherUnlockShare`
  (default 0.25), or `gameDay ≥ balance.netherUnlockDay` (default 10), or
  the value was unlocked in a previous cycle (persist a **progression
  snapshot**; unlocks never go backwards).
- Tier 4 (End) similarly with `minecraft:story/enter_the_end` /
  `minecraft:end/root` and a later day threshold.
- Early world (`gameDay` small) biases toward tier 0–1 even on Hard.
- When no player is online at rotation time, use the persisted snapshot.
- Modded dimensions from profiles follow the same idea via the profile's
  advancement ids (if the mod has them) or day thresholds.

### 9.8 Explainability

`explain(questId)` returns something like:

```
justquests:gen/1727431200_3  "Iron Supply"  (NORMAL, cycle 1727431200, seed 0x5F3A…)
objective smelt_item minecraft:iron_ingot ×24
  effort 0.70 min/unit → 16.8 min (target range 8–20 min)
  tier 1 (unlocked: day 14), family metals, requires stone pickaxe
reward give_item minecraft:emerald ×5 (value 15) + xp 60 (value 2)  → 17 / budget 16.8
rejected alternatives: collect minecraft:iron_ore (drops raw_iron, no Silk Touch)
state: CLAIMED by 3f2a…  at 2026-10-01 14:03
```

### 9.9 Optional self-calibration (implement, default OFF)

From stats (§15) compute per `family` and objective type the ratio
`observed median minutes (claim→complete) / estimated minutes` once ≥ 5
samples exist; with `adaptiveBalancing = true` apply a bounded multiplier
(0.5–2.0, move at most 10 % per cycle) to future effort estimates. Default
**off** (test phase records first). Visible in `explain`.

## 10. Exclusive claiming and the one-active rule

### 10.1 States

```
AVAILABLE ──tryClaim OK──► CLAIMED ──onComplete──► COMPLETED (terminal)
    ▲                         │
    └──── onAbandon ──────────┘   (if releaseOnAbandon and still in current cycle)
          forceRelease / expiry / releaseAllFor
```

### 10.2 Rules

1. `tryClaim(q, p)`:
   - not a generated id → `NOT_GENERATED`; generator disabled → `DISABLED`;
   - id not in `servedQuests()` → `NOT_SERVED`;
   - state COMPLETED → `COMPLETED`;
   - CLAIMED by p → `ALREADY_YOURS`; CLAIMED by other → `CLAIMED_BY_OTHER`
     (only when `exclusiveClaims`; with exclusivity off, multiple players may
     hold the same quest and completion by one does not lock it for others);
   - `oneActivePerPlayer` and p holds another CLAIMED generated quest →
     `HAS_ACTIVE_GENERATED`;
   - else → CLAIMED by p, save, `OK`.
2. `onAbandon(q, p)`: if p holds q → if q is in the **current** cycle and
   `releaseOnAbandon` → AVAILABLE; if q is a **retained** quest from an older
   cycle → remove it entirely (never re-offer stale quests).
3. `onComplete(q, p)`: CLAIMED by p → COMPLETED. Completed quests leave
   `servedQuests()` at the next rotation. (With exclusivity off, track per
   player.)
4. **Rotation**: AVAILABLE quests of the old cycle are removed; **CLAIMED
   quests are retained** (still served, still counting progress) until
   completed or abandoned; COMPLETED quests are dropped. The new cycle adds
   a full `questsPerCycle` set (retained quests do not reduce it).
5. **Expiry** (`claimExpiryHours > 0`, default 0 = never): a CLAIMED quest
   older than that is released at the next `tick()`; returned in
   `expiredClaims` so the mod removes it from the player.
6. `releaseAllFor(p)`: every claim of p → released as in (2); returns ids.
7. Singleplayer: identical logic (one player), no special cases.
8. Offline holders keep their claims (unless expiry is configured).

### 10.3 The v1 bug you must not repeat

In v1 a rotation **replaces** the generated set, so a quest a player already
accepted loses its definition: progress stops forever and the player is
stuck. v2's retained-quest rule (10.2.4) fixes this. Add a test.

### 10.4 Visibility

The core only provides `claim()/claims()`; the mod decides the display
(e.g. "Taken by Steve"). Keep holder UUIDs in state; names are resolved by
the mod.

### 10.5 Reconciliation at start

`start(activeGeneratedQuests)` must reconcile state with reality:
- a claim in state whose holder no longer has the quest active → release;
- a player with an active generated quest but no claim (e.g. v1 data) →
  create the claim **if the definition is known** (state or migrated v1
  file) and retain it; if the definition is unknown → return the id in
  `deadQuestIds` (the mod removes it from the player);
- two players actively holding the same exclusive quest (legacy data) →
  keep both as holders for that quest, log a warning, never crash.

## 11. Rotation, persistence and migration

### 11.1 Cycles

- Cycles are **aligned to fixed local-time boundaries**: default at 00:00
  and 12:00 in `config.zone()` (default system time zone), configurable
  `cycleHours` (default 12, allowed 1–168 for testing) and anchor hour.
- `cycleId` = boundary instant in epoch seconds.
- **Catch-up**: after downtime spanning several boundaries, rotate **once**
  to the latest boundary (reason `"catch-up"`); history timestamps use the
  real time.
- **Clock goes backwards** (system time changed): never rotate backwards;
  keep the current set until real time passes the next boundary; log once.
- Seed of a cycle = mix(worldSeed, cycleId, rerollCounter).

### 11.2 State files (via StateStore, `<world>/justquests/`)

- `generator_v2.json` — versioned (`"format": 1`): current cycle, served
  quests (definitions + metadata: estimated minutes, tier, families,
  signatures), retained quests, claims, 6-day history (signature →
  last-used epoch ms), progression snapshot, reroll counter, calibration
  data. Pruned on every rotation.
- `generator_v2_stats.json` — test-phase stats (§15), bounded size
  (aggregate old entries).
- Atomic writes (the host does temp+move). On a corrupt/unparseable file:
  log, back it up (write `generator_v2.corrupt-<ts>.json` via StateStore),
  start fresh — never crash.

### 11.3 Migration from v1

v1 stores `<world>/justquests/generated.json`:

```json
{
  "lastRefresh": 1727400000000,
  "history": { "<signature>": 1727400000000 },
  "quests":  { "<idPath>": { ...quest JSON... } }
}
```

v1 quest ids are `justquests:gen/<idPath>`, `idPath` = `<timestampMs>_<n>`,
category `"generated"`. On first `start()` with no v2 state but a v1 file:
import history (convert v1 signatures if your signature format differs, or
keep them in a legacy set honoured for 6 days), keep v1 quest definitions
**only** for quests reported active in `activeGeneratedQuests` (as retained
+ claimed), then generate a fresh v2 cycle. Do not delete the v1 file
(the integrator removes v1 later).

### 11.4 Signatures

Signature = normalised, order-independent description of a quest's content
(objective types + resolved targets, **without counts**), e.g.
`collect_item:minecraft:raw_iron|smelt_item:minecraft:iron_ingot`. Two quests
with the same signature are "the same quest" for the 6-day rule.

## 12. Difficulty levels

One value per world (`EASY`/`NORMAL`/`HARD`), set by OP. A change applies
from the **next rotation or reroll** (claimed quests keep their numbers).
All numbers live in `balance.json`; initial defaults (tune later):

| Parameter | Easy | Normal | Hard |
|---|---|---|---|
| Target minutes per quest | 4–10 | 8–20 | 15–35 |
| Max content tier (before progression gating) | 1 | 2 | 4 |
| Objectives per quest (1 / 2 / 3) | 100 / 0 / 0 % | 75 / 25 / 0 % | 50 / 35 / 15 % |
| Reward rate (value per minute) | 1.0 | 1.2 | 1.5 |
| Nether/End quests allowed | no | if unlocked | if unlocked |
| Optional effect / loot-table rewards | no | rare | sometimes |
| Claim expiry (hours, 0 = never) | 0 | 0 | 0 |

Easy must feel welcoming (surface, early tools, short); Hard must feel like
an expedition (deeper tiers, multi-objective, better rewards) but never
grindy or impossible.

## 13. Configuration

`GeneratorConfig` (record, built by the mod from `settings.json`). Provide
`GeneratorConfig.defaults()` and a builder or `with*` methods.

| Field | Type | Default | settings.json key (integrator wires) |
|---|---|---|---|
| `enabled` | boolean | true | `generatedQuests` (exists today) |
| `questsPerCycle` | int | 5 (clamp 1..`maxPerCycle`) | `generatedCount` (exists today) |
| `maxPerCycle` | int | 20 | — (hard cap) |
| `difficulty` | Difficulty | NORMAL | `difficulty` (`"easy"`/`"normal"`/`"hard"`) |
| `exclusiveClaims` | boolean | true | `generatorExclusiveClaims` |
| `oneActivePerPlayer` | boolean | true | `generatorOneActivePerPlayer` |
| `releaseOnAbandon` | boolean | true | `generatorReleaseOnAbandon` |
| `claimExpiryHours` | int | 0 | `generatorClaimExpiryHours` |
| `cycleHours` | int | 12 | `generatorCycleHours` |
| `cycleAnchorHour` | int | 0 | `generatorCycleAnchorHour` |
| `zone` | java.time.ZoneId | system default | — |
| `historyDays` | int | 6 | — |
| `moddedShare` | double | 0.35 | `generatorModdedShare` |
| `disabledProfiles` | Set<String> | empty | `generatorDisabledProfiles` (list) |
| `adaptiveBalancing` | boolean | false | `generatorAdaptiveBalancing` |
| `statsEnabled` | boolean | true | `generatorStats` |

Validate and clamp every value; log adjustments.

## 14. Modded content: pick two big Modrinth mods

### 14.1 Your research task

Choose **two** large, popular mods from **https://modrinth.com** and ship a
profile for each. Selection criteria (document your scoring in `MODS.md`):

1. **Popularity & health**: high download count, actively maintained.
2. **Coverage of our targets**: available for as many of our
   (loader × MC version) builds as possible — ideally NeoForge + Fabric +
   Forge across 1.18.2–1.21.x (the same mod may have an official port with
   another name, e.g. a "Fabric"/"Refabricated" edition). Prefer mods whose
   **namespace/ids are identical across loaders**.
3. **Quest-friendliness**: lots of content that maps to our hooks (§7):
   natural blocks to mine, drops to collect, crafting-table recipes,
   smeltables, foods to consume, breedable/tamable animals, hostile mobs.
4. **Stable ids** across versions (renames are handled by alternative ids,
   but fewer is better).

Examples worth evaluating (not a requirement): Farmer's Delight (and its
Fabric edition), Biomes O' Plenty, Supplementaries, Alex's Mobs, Twilight
Forest, Create (and Create Fabric), Quark, The Aether. Decide from research.

### 14.2 `MODS.md` must contain, per chosen mod

- Name, Modrinth URL, mod id(s) per loader, namespace(s).
- Coverage table: which of our 34 (loader, MC version) builds have a
  release of the mod, and which mod version you studied.
- Id differences between versions/loaders you found (→ alternative ids).
- Which content you included and **why it is achievable with our hooks**;
  which you **excluded** and why (e.g. "cooking-pot meals: output slot is
  not a crafting grid → only usable as `consume_item`"; "mechanical
  crafting: not counted").
- Progression notes (what needs which tools/dimensions), special
  dimensions and their unlock logic.
- Honest limitations and what could not be verified without running the
  game.

### 14.3 Profile requirements

- One JSON per mod: `profiles/<modid>.json`, same entry schema as vanilla
  plus: `requiresMod` (mod id(s), any-of), optional `loaders` /
  `minecraft` filters, theme and reward extensions, optional advancement ids
  for progression.
- **Aim for ≥ 40 usable targets per mod** spread over several objective
  types and tiers, plus ≥ 3 mod themes (some mixing vanilla + mod).
- Profile inactive when the mod is absent → zero effect, zero errors.
- Every id is existence-checked at runtime; missing ids are skipped
  silently (counted in stats/explain), so one profile works across all mod
  versions and loaders.
- Adding a third mod later must require **only a new JSON file** (document
  the schema in `DESIGN.md` with a full annotated example).

## 15. Test-phase statistics

The maintainer will run a test phase to tune balance. Record locally
(`generator_v2_stats.json`, only if `statsEnabled`):

- per generated quest: cycle, difficulty, objective types, tier, families,
  profile(s), estimated minutes, generated/claimed/completed/abandoned/
  expired timestamps (**no player UUIDs or names** — anonymous);
- aggregates: completion rate by objective type/tier/difficulty/profile,
  median claim→complete minutes vs. estimate (calibration ratio), abandon
  rate, how often each relaxation rule fired, candidates rejected per
  pipeline step.

`stats()` returns a `StatsSummary` with those aggregates; provide
`StatsSummary.toText()` for a chat/console printout. No network, ever.

## 16. Testing requirements

JUnit 5, with a **`FakeHost`** (configurable fake ContentView/WorldContext/
StateStore/Validator, fixed clock) in the test sources. Required tests:

1. **Purity**: scan `src/main/java` — fail on any import of `net.minecraft`,
   `com.mojang`, `net.neoforged`, `net.minecraftforge`, `net.fabricmc`,
   `com.google.common`, `org.slf4j`, `org.apache.logging`.
2. **Determinism**: same inputs → identical served JSON (serialise and
   compare) across two instances.
3. **Schema validity**: every generated quest passes a strict validator
   mirroring §6 (required fields, allowed types, counts > 0, no tags where
   unsupported, no forbidden types/rewards, title/description limits, no
   unknown keys, category `"generated"`).
4. **Achievability**: with `FakeHost` reporting certain ids missing / TriState
   `NO`, those targets never appear; with all `UNKNOWN`, generation still
   produces full sets.
5. **Set rules**: 1 000 random seeds × each difficulty × N ∈ {1,5,10,20}:
   type diversity, family uniqueness, no duplicate targets, no duplicate
   titles, relaxation never throws.
6. **History**: no signature repeats within 6 days across simulated cycles;
   may reappear after.
7. **Claims state machine**: every transition and denial in §10, including
   one-active rule, exclusivity on/off, abandon of current vs retained
   quests, expiry, releaseAllFor, forceRelease.
8. **Rotation retention (v1 bug)**: claimed quest survives rotations and
   stays in `servedQuests()` until completed/abandoned.
9. **Reconciliation** (§10.5) and **v1 migration** (§11.3) with sample files.
10. **Clock edge cases**: catch-up after 5 days offline rotates once; clock
    moving backwards never rotates backwards.
11. **Difficulty scaling**: median estimated minutes and reward value are
    monotonic Easy < Normal < Hard; tier caps respected.
12. **Rewards**: never target item/family, value within ±20 % of budget,
    counts ≤ stack size, no command rewards.
13. **Progression**: Nether/End targets only after unlock; snapshot
    persists; unlocks never regress.
14. **Mod profiles**: profile inactive without the mod; active with it;
    `moddedShare` and "≥ 1 per active mod" respected for N ≥ 5.
15. **Corrupt state file** → fresh start, backup written, no exception.
16. **Performance**: generate 20 quests < 50 ms (warm), `tick()` no-op
    < 0.1 ms average over 10 000 calls.
17. **Catalog integrity**: every entry/target references allowed objective
    types, has effort > 0, min ≤ max, valid id syntax; every template
    placeholder resolvable.

`samples/`: for seeds 1, 2, 3 × each difficulty: a set of 10 with vanilla
only, and a set of 10 with both mod profiles active (FakeHost says the mods
are loaded and their ids exist). Include `explain()` output for each quest.

## 17. What INTEGRATION.md must contain

Write it for the integrator who will wire the core into 34 builds. It must
be precise enough that no guessing is needed:

1. **Files to copy**: exact list of core source files and resource paths.
2. **Host adapter contract**: for every method of every host interface —
   what it must return, when it is called, allowed fallbacks (`UNKNOWN`,
   `-1`, `null`), performance expectations (e.g. cache tag lookups per
   rotation).
3. **Call order** (lifecycle §8.4) with exact trigger points in the mod,
   mapped onto the current mod classes ([Appendix B](#appendix-b--current-mod-code-map)):
   - replace `GeneratedQuestStore` usage (server start/tick/reroll/stop),
   - `QuestManager.setGeneratedQuests(...)` from `servedQuests()`,
   - `QuestCommand.accept` → `tryClaim` (after eligibility, before
     `data.accept`), with the exact player-facing deny messages per
     `ClaimResult`,
   - `QuestCommand.abandon` → `onAbandon`,
   - completion in `QuestProgressService` → `onComplete`,
   - admin reset → `releaseAllFor`,
   - handling of `expiredClaims` and `deadQuestIds`,
   - `/quest reload` and settings changes → `updateConfig`.
4. **New settings keys** (§13) with defaults and the updated `_help` text.
5. **Suggested new OP commands** (the integrator implements them):
   `/quest difficulty [easy|normal|hard]`,
   `/quest generator status|explain <id>|preview [n]|stats|release <id>`.
6. **Sync/GUI suggestion**: what claim info to add to the client sync and
   how the list could show "Taken by <name>" / "Yours".
7. **Self-test hooks**: what `/quest test` should check (instance started,
   served set valid, claims consistent).
8. **Optional mod enhancements** (not required for v2): e.g. tag support
   for `mine_block`/`kill_mob`, splitting large `give_item` counts.
9. **Migration notes**: v1 → v2 steps and what to delete later.

## 18. Per-version API notes for the host adapter

Not your code, but design the host interfaces so each can be implemented on
**every** build. Known differences across our 34 builds:

| Topic | Differences |
|---|---|
| Registries | 1.18.2–1.19.2: `net.minecraft.core.Registry.ITEM` etc.; 1.19.3+: `BuiltInRegistries.ITEM`. `get(id)` returns an `Optional` on 1.21.2+ (`getValue`); `containsKey(id)` works everywhere → existence checks are cheap on all versions. |
| `ResourceLocation` | `new ResourceLocation(ns, path)` ≤ 1.20.6; `ResourceLocation.fromNamespaceAndPath/parse` 1.21+. |
| Tags | Bound only after datapack load. 1.18.2 uses `Registry.ITEM_REGISTRY` keys; later `Registries.ITEM`. Naming conventions differ (§9.2). |
| Recipes | Big API churn: `RecipeHolder` wrappers from 1.20.2, results need `RegistryAccess` from 1.19.4, recipe access reworked in 1.21.2+. → `hasCraftingRecipe`/`hasSmeltingRecipe` may be `UNKNOWN` on some builds. The catalog must carry the truth. |
| Stack size | `getDefaultMaxStackSize()` (data components, 1.20.5+) vs `getMaxStackSize()`. |
| Entity class checks | Need an instance or class info; `EntityType.create` signature changes (1.21.2 adds a spawn reason) → `isBreedableAnimal`/`isTamableAnimal` may be `UNKNOWN`. |
| Advancements | `AdvancementHolder` from 1.20.2; earlier `Advancement`. Online-player share is cheap; offline players are not scanned. |
| Mod list | NeoForge/Forge `ModList.get().isLoaded(id)`, Fabric `FabricLoader.getInstance().isModLoaded(id)`. |
| World seed / time | Seed accessor moved (`worldGenSettings()` ≤ 1.19.2 vs `worldGenOptions()` later); game time `overworld().getGameTime()` everywhere. |
| Names | Dedicated servers only have vanilla `en_us` → modded names usually `null`. |
| Java | 1.18.2–1.20.4 run on Java 17 → the core must be Java 17 bytecode. |

## 19. Acceptance checklist

Before you zip, verify every line:

- [ ] `./gradlew build` green on a clean checkout; core compiled `--release 17`.
- [ ] Only JDK + Gson 2.8.8 API used; purity test green.
- [ ] Facade and `api` types match §8 exactly; every public item has Javadoc.
- [ ] No objective types from the forbidden list; no command rewards; no tags
      where unsupported; category `"generated"`; ids `justquests:gen/<cycle>_<n>`.
- [ ] All 17 test groups from §16 implemented and green.
- [ ] Vanilla catalog ≥ 150 usable targets; each mod profile ≥ 40 targets and
      ≥ 3 themes.
- [ ] v1 bug (rotation drops accepted quests) covered by a test.
- [ ] `samples/` present with explain output.
- [ ] `README.md`, `INTEGRATION.md`, `DESIGN.md` (with Assumptions),
      `MODS.md`, `CHANGELOG-snippet.md` complete, in English.
- [ ] No secrets, tokens, webhooks, telemetry, or network code anywhere.
- [ ] No code or assets copied from other mods; license headers compatible
      with LGPL-3.0-only (or no headers).

---

## Appendix A — generator v1 as it exists today

Files: `generator/QuestGenerator.java` (119 lines) and
`generator/GeneratedQuestStore.java` (160 lines), identical logic in every
build (with per-version `ResourceLocation` differences).

**API (to be replaced):**

```java
public record GenQuest(String idPath, String signature, JsonObject json) {}
public static List<GenQuest> generate(int count, Set<String> recentSigs, Random rng, long seedTag);
```

**Behaviour:** 4 templates, each `(objective type, field, fixed pool, min,
max, verb)`; random pool entry, random count in range, random reward item
from a fixed list with `count = max(1, qty/8 + rand(0..2))`; title
`"<Verb> <count> <Name>"`; signature per template+target; 12 h rolling
refresh (`now - lastRefresh ≥ 12h`), checked every 5 min; 6-day history;
`/quest reroll` (OP) regenerates everything; per-world toggle
`generatedQuests` + `generatedCount`.

**v1 pools:**
- `collect_item` (12–48): oak_log, cobblestone, dirt, sand, wheat, coal,
  iron_ore, sugar_cane, kelp, apple
- `mine_block` (16–64): stone, coal_ore, iron_ore, copper_ore, andesite,
  granite, diorite, deepslate, gravel, sandstone
- `craft_item` (4–16): bread, torch, stick, chest, furnace, ladder,
  crafting_table, bowl, bookshelf
- `kill_mob` (4–12): zombie, skeleton, spider, creeper, husk, drowned, slime
- rewards: bread, cooked_beef, iron_ingot, gold_ingot, emerald, coal, apple,
  experience_bottle

**Known v1 problems (v2 must not repeat):**
1. **Rotation drops accepted quests** — the new set replaces the old one, so
   a player's active generated quest loses its definition and is stuck.
2. `collect_item minecraft:iron_ore` — iron ore drops **raw iron** unless
   mined with Silk Touch → effectively a Silk Touch quest.
3. `collect_item minecraft:apple ×12–48` — apples drop from oak/dark oak
   leaves at ~0.5 % → extremely grindy.
4. `craft_item minecraft:bookshelf ×4–16` — each needs 3 books (paper +
   leather) → effort wildly underestimated compared to torches.
5. `kill_mob husk/slime` — biome-bound (deserts / swamps & slime chunks)
   with no hint.
6. Rewards unrelated to effort; reward can equal the objective family.
7. No claims, no one-active rule, no difficulty, only 4 objective types,
   English title template only, no progression awareness.

## Appendix B — current mod code map

Package `com.erikedits.justquests` (same layout in every build):

| File | Role / integration point |
|---|---|
| `generator/QuestGenerator.java`, `generator/GeneratedQuestStore.java` | v1 — to be replaced. Store: `init(server)` at server start, `tickCheck()` every 6000 ticks, `reroll()` from `/quest reroll`, `clear()` on stop; after regenerating → `QuestManager.setGeneratedQuests(map)` → `QuestNetwork.syncAll(server)` → save. |
| `data/QuestManager.java` | 3 sources (datapack, custom, generated). `setGeneratedQuests(Map<ResourceLocation, Quest>)`, `getQuests()`, `get(id)` (hides datapack quests when `mainQuests` is off). |
| `data/Quest.java`, `data/objective/*`, `data/reward/*`, `data/LocalizedText.java` | The codecs behind §6. |
| `commands/QuestCommand.java` | `/quest list/accept/abandon/progress/stats/leaderboard/reload/reroll/mainquests/test/admin …`. `accept(ctx, id)` runs eligibility (already active/completed, repeatable/cooldown, `requires`) then `data.accept(id)`; `abandon(ctx, id)` → `data.abandon(id)`. |
| `progress/QuestProgressService.java` | Advances objectives from events; on completion: `data.complete(questId)`, grants rewards, messages, then syncs progress. |
| `data/PlayerQuestData.java` | Per player: `active` (quest → progress), `pendingClaim`, `completed` (quest → last completion ms). |
| `storage/WorldQuestStore.java` | Per-world player data (`peek(uuid)`), saved periodically. |
| `storage/WorldSettings.java` | `settings.json` (per world): `discordWelcome`, `announceCompletions`, `completionSound`, `completionToast`, `mainQuests`, `generatedQuests`, `generatedCount`; `load(server)` / `save(server)`. |
| `network/QuestNetwork.java`, `network/ClientQuestData.java` | Server→client sync (full list on join/list changes, progress-only otherwise). |
| `client/QuestScreen.java` | Quest book GUI, 248×184, list rows 80 px wide (titles truncated with "…"), detail pane with objectives/rewards, Accept/Abandon. |
| `diagnostics/SelfTest.java` | `/quest test` self-checks. |
| Fabric only: `JustQuestsFabric.java`, `event/FabricQuestHooks.java`, `mixin/*` | Loader glue; mixins listed in §7. |
| NeoForge/Forge: `JustQuests.java`, `player/PlayerQuestEvents.java`, `storage/ServerStorageEvents.java` | Loader glue; events listed in §7. |

Built-in quests live in `src/main/resources/data/justquests/justquests/quests/*.json`
(format §6, with per-language maps). Useful as style reference for titles.

## Appendix C — glossary

- **Build**: one (loader, MC version) combination; 34 in total.
- **Core**: your version-neutral Java 17 generator.
- **Host / adapter**: per-build implementation of the `api` interfaces.
- **Served quests**: what the mod registers now = current cycle + retained.
- **Retained quest**: a claimed, unfinished quest from an older cycle.
- **Cycle**: one 12-hour period between two rotation boundaries.
- **Signature**: count-independent content fingerprint for the 6-day rule.
- **Tier**: coarse progression band 0–4 used for gating and rewards.
- **Profile**: data file enabling a mod's content.
- **Effort**: estimated minutes of average play.
