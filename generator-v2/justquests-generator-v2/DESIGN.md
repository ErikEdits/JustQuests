# DESIGN — JustQuests generator v2

How the core works, the formulas it uses, the data model, how to tune it, the assumptions made where
the specification left a choice, and the known limitations.

Contents

1. [Architecture](#1-architecture)
2. [Achievability pipeline](#2-achievability-pipeline)
3. [Effort and counts](#3-effort-and-counts)
4. [Rewards](#4-rewards)
5. [Composition, themes and set rules](#5-composition-themes-and-set-rules)
6. [Text generation](#6-text-generation)
7. [Progression](#7-progression)
8. [Claims, rotation, persistence](#8-claims-rotation-persistence)
9. [Statistics and self-calibration](#9-statistics-and-self-calibration)
10. [Data model and file formats](#10-data-model-and-file-formats)
11. [Adding a mod: annotated profile example](#11-adding-a-mod-annotated-profile-example)
12. [Tuning guide](#12-tuning-guide)
13. [Assumptions](#13-assumptions)
14. [Known limitations](#14-known-limitations)
15. [Tools, verification and tests](#15-tools-verification-and-tests)

---

## 1. Architecture

```
com.erikedits.justquests.generator.v2
├─ QuestGeneratorV2            facade: exact §8.3 signatures (+ startWithHolders, holders,
│                              servedRevision, status, selfTest, config); guards every call
├─ api/                        the contract: host interfaces, records, enums, GeneratorConfig
└─ internal/                   free to change
   ├─ Core                     state machine: lifecycle, rotation, claims, reconciliation, migration
   ├─ Generation               one side-effect-free generation run (rotation, reroll, preview, tests)
   ├─ catalog/                 JSON model + CatalogLoader (bundled data + world overrides)
   ├─ gen/                     CandidateResolver (pipeline), SetBuilder (composition),
   │                           RewardBuilder, TextBuilder, Progression, QuestJson, SchemaCheck
   ├─ state/                   GenState, QuestRecord, ClaimRecord, CycleClock, V1Migration
   ├─ stats/                   StatsBook (bounded, anonymous)
   └─ util/                    Json (Gson 2.8.8 only), Rng, NiceNumbers, English, Ids, Medians
```

Runtime model: one instance per world/server, no static mutable state (enforced by
`PurityTest.noStaticMutableState`), single-threaded, no I/O except `StateStore` and the bundled
resources. Every facade method is wrapped: a `RuntimeException` inside the core is logged and a safe
default is returned (`RotationResult.none`, `NOT_SERVED`, empty maps…). Only null arguments throw.

Determinism: all randomness of one generation comes from one `SplittableRandom` seeded with
`mix(worldSeed, cycleId, rerollCounter)` (SplitMix64 finaliser). All iteration is over ordered
collections (catalog order, `LinkedHashMap`, `TreeMap`); host sets are only used for membership
tests. `DeterminismTest` compares the serialised served JSON of two instances byte by byte.

## 2. Achievability pipeline

`CandidateResolver` turns every catalog target into a *candidate* or rejects it, counting the
rejection per step (visible in `stats()` and `explain`):

| Step | Check | Rejection key |
|---|---|---|
| 1 | type is generator-usable and in `HostCapabilities.objectiveTypes()` | `1_type` |
| 2 | profile active: vanilla, or `isModLoaded(any of requiresMod)` and loader/version filters match, and not in `disabledProfiles` | `2_profile` |
| 3 | released on the running version: the target's (or entry's) `since` is not newer than `ContentView.minecraftVersion()`; then the id exists: tag concept first (if the host supports tags for the type and the tag has members), then `id`, then `alt` ids in order | `3_missing_id` |
| 4 | hook compatible: `tame_animal` needs the catalog flag `tamable` and host `isTamableAnimal != NO`; `breed_animal` needs `isBreedableAnimal != NO`; `craft_item` needs `hasCraftingRecipe != NO`; `smelt_item` needs `hasSmeltingRecipe != NO`; `consume_item` needs `isConsumable != NO`; `collect_item` is trusted to the catalog (drops only) | `4_hook` |
| 5a | tier ≤ difficulty cap and target `minDifficulty` ≤ difficulty | `5_tier` |
| 5b | tier unlocked by progression; dimension exists, travel allowed on this difficulty, dimension unlocked; `requires` satisfied | `5_progression` |
| 5c | effort range: the target can land in the difficulty's minute range (min count not above `max × 1.25`, max count not below `min × 0.3`) | `5_effort_range` |
| 6 | not in the 6-day history, target/signature not already used in the set | `6_history` (counted once per run), `6_duplicate` |
| 7 | the finished quest passes `SchemaCheck` (strict §6 check) and the host `QuestValidator` | `7_validator` |

A quest that fails step 7 is discarded, logged once (per signature and run) and a replacement is
built. The "definite NO beats catalog, UNKNOWN keeps catalog" rule of §8.2 is implemented in step 4.

## 3. Effort and counts

Every target carries `effort` = minutes per unit for an average player at that tier (including
finding, travelling inside the dimension, tool use). Modifiers:

```
multiplier   = Π hintMultiplier(h) for h in hints            (balance.hintMultipliers, e.g. desert ×1.3)
             × (1 + toolPenaltyPerLevel × max(0, toolLevel(tool) − expectedToolLevel))
             × calibration(family|type)                       (only with adaptiveBalancing)
expectedToolLevel = min(3, 1 + [gameDay ≥ 2] + [Nether unlocked])   (stone → iron → diamond)
effortPerUnit = effort × multiplier
overhead      = dimensionOverheadMinutes[dimension]            (Nether 3, End 5, modded 5; once per quest)
```

Count selection for a quest whose target time `T` is drawn uniformly from the difficulty range (the
quick slot draws from the lower third):

```
single:  count = niceRound((T − overhead) / effortPerUnit, min, max, stackSize)
multi:   per objective share = (T − overhead − fixed visits) / k × jitter(0.85..1.15)
estimate = (Σ count × effortPerUnit) × multiEffortFactor[k] + overhead
```

`niceRound` picks the value nearest in log space from a friendly scale, restricted to `[min, max]`:
general `1 2 3 4 5 6 8 10 12 16 20 24 32 40 48 64 80 96 128`; stack-64 items the multiples of 8 above
8 (`8 16 24 32 40 48 64 80 96 128`); stack-16 items `1 2 3 4 8 12 16 20 24 32 48 64`; unstackables
`1..5`.

A quest is accepted when `max(min × 0.75, 0.5 × T) ≤ estimate ≤ max × 1.25` (quick slot:
`estimate ≤ min + (max − min) / 3`). Candidates are weighted by
`capacity = min(1, maxMinutes / T)²` so that targets able to reach the drawn time win. The estimate is
stored in the state (`meta.estMinutes`), used for `sort` and shown by `explain`.

## 4. Rewards

```
budget = clamp(estimate × rewardRate(difficulty) × rewardPremium[k], budgetMin, budgetMax)
```

1. Optional extra (Normal/Hard, by chance): a curated loot table (value ≤ 60 % of the budget, tier ≤
   quest tier + 1) **or** a beneficial effect worth ~25 % of the budget (duration rounded to 30 s).
2. Item: `itemBudget = (budget − extra) × itemShare (0.75)`. Eligible items: profile active, id exists
   (or an `alt` id), `tier ≤ questTier + 1`, `minDifficulty` satisfied, family ≠ any quest family, not
   in `avoidFamilies`, not a target id, `value ≤ itemBudget × 1.2`. Weighted by
   `weight × min(1, cap × value / itemBudget)²` (items that can carry the budget are preferred);
   `count = clamp(round(itemBudget / value), 1, min(max, stackSize))`.
3. If the first item leaves more than 40 % of the item budget unused (and there is no extra), a second
   different item is added for the rest.
4. XP from the remainder: `xp = round5((budget − itemsAndExtras) × xpPointsPerValue)`, clamped to
   `[minXp, maxXp]` (5..500 points).
5. Tolerance repair: item counts are reduced/increased (and extras dropped) until
   `|total − budget| ≤ 20 %`; a quest whose rewards cannot be brought into tolerance is rejected.
6. Rarely a flavour `message` reward (value 0) when fewer than three rewards exist.

Never emitted: `justquests:command`. Stack sizes come from the host (`maxStackSize`), else from the
reward table (`stack`), else 64.

Value units ≈ minutes of average play; e.g. iron ingot 0.6, emerald 2.5, diamond 5, golden apple 9,
1 value = 12 XP points.

## 5. Composition, themes and set rules

`SetBuilder.build(n, kept, totalSize, moddedShare)`:

1. **Slot plan.** Objective counts per slot from the difficulty distribution
   (Easy 100/0/0, Normal 75/25/0, Hard 50/35/15 %). Modded plan when ≥ 1 mod profile has candidates:
   `modded = round(totalSize × moddedShare) − keptModded`, at least one slot per active mod when
   `totalSize ≥ 5` (not already covered by kept quests); the rest are vanilla slots. Slots are
   shuffled; the first slot becomes the single-objective **quick** slot unless a kept quest is quick.
2. **Filling a slot** (per relaxation level, up to 40 attempts each):
   - `k ≥ 2`: with probability `themeChance` (0.85) a **theme** is tried: slots are filled with
     matching candidates (type/key/family/profile filters), all objectives in the same dimension and
     within ±1 tier of the first, no duplicate targets inside the quest. If that fails, a
     **same-family combo** (two targets of one family, e.g. mine iron ore + smelt copper) is tried,
     then a single objective.
   - singles: weighted pick by `weight × typeWeight(difficulty) × tierWeight × earlyWorldWeight ×
     0.45^(times this type is already in the set) × capacity`.
   - the quest must pass the range check, its signature must be new (set + history), rewards and a
     unique title must be found, `SchemaCheck` and the host validator must accept it.
3. **Set rules** (all tunable in `balance.json → setRules`):
   - at most one quest per family (a theme may use a family twice inside one quest);
   - no objective type in more than `max(1, ⌊0.4 × N⌋)` quests;
   - at least `min(N, 3)` distinct objective types — repaired at the end by rebuilding the latest
     quests whose types are duplicated, with "new type only";
   - at least one quick quest;
   - modded share and one quest per active mod as planned (the per-mod guarantee takes at most
     `maxPerModShare` of the set; with more mods, a rotating subset is covered and `per_mod` is
     counted);
   - no duplicate targets, signatures or titles (never relaxed).
   - Themes are curated, so inside one themed quest two targets may come from the same entry ("mine
     asurine and crimsite") and their tiers may differ by up to two (lapis and bookshelves); every
     slot still has to come from the same dimension.
4. **Relaxation order** when a slot cannot be filled: `family` → `type_share` → `modded_share` →
   `history`; a quick slot that cannot be quick retries as a normal slot (`quick`); a slot that still
   fails is skipped (`slot_skipped`, the set has fewer quests). Every relaxation is counted in the
   stats and logged as one line. The loop is bounded (≤ 2 passes × 5 levels × 40 attempts per slot).

Measured on the bundled catalog (`RelaxationProbe`, 200 seeds per cell, Nether/End unlocked): with
vanilla only, no set of any difficulty or size needs a relaxation. With all five mod profiles active
(Twilight Forest unlocked), sets of 5 always report `per_mod` (four or five mods, three covered per
cycle), and 4 % of them also relax the family and type-share rules. Sets of 10 relax in 0.5 % (Normal)
and 3.5 % (Hard) of the cases, sets of 20 in 0–1 %. Easy never relaxes anything but `per_mod`. A set
of 20 takes 2–4 ms.

## 6. Text generation

- **Names:** target `name` → `ContentView.englishName` (cleaned: formatting codes, braces and
  control characters removed; untranslated keys such as `item.mod.thing` and names over 40 characters
  are ignored) → prettified id (`raw_iron` → "Raw Iron", namespace dropped). Plurals via a small English helper (mass nouns stay singular: "24 raw iron";
  "wolf" → "wolves", "enderman" → "endermen", "fungus" → "fungi"), overridable per target
  (`plural`).
- **Titles:** theme names → combo titles (`{Family} Order`, family nouns from `templates.familyNames`)
  → type titles (`{Name} Haul`, `Gather {Names}`…) → fallbacks, shuffled by the RNG; the first title
  ≤ 22 characters that is unused in the set wins, else ≤ 32, else a shortened name with a numeral.
- **Wording variants:** a target hint can switch the wording (`cook` → "Cook 16 bacon",
  `drink` → "Drink 4 honey bottles"); defined in `templates.variants`.
- **Descriptions:** objective phrases joined with "and" (`Mine 16 Nether quartz ore and collect 6
  blaze rods`), optionally inside a theme sentence (`The smithy needs help: {list}.`), then hint
  sentences from the catalog in order of usefulness — target `hint`, "Any log counts." for tags, the
  strongest pickaxe requirement only, biome/structure hints, "Found in the Nether." — added while the
  text stays ≤ 140 characters. `SchemaCheck` rejects any leftover `{placeholder}`.

## 7. Progression

A persisted snapshot (`state.progression`): Nether unlocked when ≥ 25 % of online players have
`minecraft:story/enter_the_nether` **or** game day ≥ 10; End when ≥ 25 % have
`minecraft:story/enter_the_end`/`minecraft:end/root` **or** day ≥ 40. Unlocks never go back; with
nobody online the snapshot is used unchanged (day thresholds still apply). Modded dimensions use the
profile's `dimensions` rules (advancement share or day). Content of tier 3/4 needs the Nether/End,
and Easy never leaves the Overworld. Young worlds weight tiers with `progression.earlyWorld`
(day ≤ 2: tier 2 × 0.15; day ≤ 6: tier 2 × 0.6), multiplied with the difficulty's `tierWeights`.
`explain` shows the reason ("tier 3 (unlocked: day 14)").

## 8. Claims, rotation, persistence

- **Claims** follow §10 exactly (`ClaimsTest`). A `ClaimRecord` has `holders` (player → claim time)
  and `completions` (player → completion time). With exclusive claims the state is COMPLETED as soon
  as anyone completed it; without exclusivity completion is per player and the quest stays available
  to others. `tryClaim` checks: generated id → enabled → served → holder (`ALREADY_YOURS`) → completed
  → claimed by other → retained quests are only for their holders (`NOT_SERVED`) → one-active rule.
- **Rotation:** current-cycle quests with holders become *retained*; unclaimed and completed quests
  are removed; retained quests without holders are removed; a full new set of `questsPerCycle` is
  generated (retained quests do not reduce it). This fixes the v1 bug (`RetentionTest`).
- **Cycles:** boundaries on a local wall-clock grid `anchorHour + k × cycleHours` in `config.zone()`
  (00:00 and 12:00 by default; DST-aware because the grid is local time). `cycleId` = boundary in
  epoch seconds. `tick()` compares `now` with a cached next boundary/expiry (O(1)). Downtime over
  several boundaries rotates once (`catch-up`); a clock before the current cycle keeps the set and
  logs once.
- **State file** `generator_v2.json` (format 1): cycle, reroll counter, next index, served quests
  (definition + metadata + claim), retained quests, history (signature → last use, pruned to the
  window on every rotation/reroll), progression snapshot, calibration. Written (compact JSON) after
  every change. Measured worst case: ~100 KB after 90 simulated days of Hard with 20 quests per cycle
  and 60 claims that were never finished (60 retained quests); the stats file stays below ~90 KB.
- **Corruption:** unparseable or wrong-format state → logged, copied to
  `generator_v2.corrupt-<ms>.json`, fresh start (`CorruptStateTest`). Same for the stats file.
- **Reconciliation at start (§10.5):** claims whose holder no longer has the quest are released
  (`releasedClaims`); active quests without a claim are adopted if the definition is known (state or
  v1 file), otherwise reported in `deadQuestIds`; several holders of an exclusive quest are kept with
  a warning (`startWithHolders`).

## 9. Statistics and self-calibration

`generator_v2_stats.json` keeps the last 300 per-quest records (cycle, difficulty, types, tier,
families, profiles, estimate, generated time, claim/complete/abandon/expire counters and last
timestamps — no UUIDs, no names). Older records are folded into aggregate counters per type, tier,
difficulty and profile. Up to 400 claim→complete samples (observed minutes, estimate, `family|type`
for single-objective quests) feed the medians and the calibration. Relaxations and pipeline
rejections are summed.

Self-calibration (off by default): at each rotation, for every `family|type` key with ≥ 5 samples,
`target = clamp(median(observed / estimated), 0.5, 2.0)`; the stored multiplier moves toward it by at
most 10 % per cycle and applies to future effort estimates. `explain` prints the multiplier.
Caveat: claim→complete time is wall-clock time and includes offline time and breaks; use it on
servers with active players or with a median over many samples.

## 10. Data model and file formats

All bundled files live under `src/main/resources/justquests_genv2/`. They were first written with the
authoring scripts in the repository's `tools/` folder (outside this ZIP); the JSON files are the
source of truth and can be edited directly.

### 10.1 Catalog entry (vanilla.json and profiles)

```jsonc
{
  "key": "iron",                      // unique within the profile
  "name": "Iron",                     // optional display name of the resource
  "family": "metals",                 // variety group: at most one quest per family per set
  "tier": 1,                          // 0 surface · 1 stone/iron · 2 deep/diamond · 3 Nether · 4 End
  "dimension": "minecraft:overworld", // where it is obtained
  "tool": "stone",                    // none|wood|stone|iron|diamond|netherite|shears|silk_touch|fishing_rod|knife|shovel
  "hints": ["caves"],                 // biome/structure/rarity keys: effort multiplier + description sentence
  "requires": ["nether"],             // optional: nether | end | dim:<id> | key:<entry key>
  "since": "1.17",                    // first release that has it in survival; newer than the running
                                      // version → rejected (also per target; guards feature-flag leaks)
  "notes": "free text",
  "exclusions": ["collect_item minecraft:iron_ore (drops raw_iron, needs Silk Touch)"],  // shown by explain
  "weight": 1.0,                      // optional selection weight
  "targets": [
    {
      "type": "smelt_item",           // one of the ten generator types (short names)
      "id": "minecraft:iron_ingot",   // block for mine/place, DROP item for collect, OUTPUT for smelt …
      "alt": ["minecraft:chain"],     // optional renamed/versioned ids, tried in order
      "tag": "iron_ingots",           // optional tag concept (tags.json), item types only
      "effort": 0.55,                 // minutes per unit (per visit for visit_dimension)
      "min": 4, "max": 48,            // count bounds (omitted for visit_dimension)
      "tier": 2, "tool": "iron", "dimension": "...", // optional per-target overrides
      "hints": ["deep", "cook"],      // extra hints; "cook"/"drink" also switch the wording
      "name": "Steak", "plural": "Eyes of Ender", // optional display overrides
      "hint": "Smelt raw iron in a furnace.",       // optional sentence for the description
      "stack": 16,                    // fallback stack size if the host cannot tell
      "minDifficulty": "hard",        // optional
      "tamable": true,                // required for tame_animal
      "weight": 1.0, "note": "free text"
    }
  ]
}
```

### 10.2 Other files

| File | Content |
|---|---|
| `catalog/profiles/index.json` | `{"profiles": ["farmersdelight.json", "create.json", "mekanism.json", "twilightforest.json", "botania.json"]}`: the bundled profile list (a jar cannot be listed) |
| `rewards.json` | `items` (`id`, `alt`, `value`, `tier`, `max`, `stack`, `family`, `avoidFamilies`, `weight`, `minDifficulty`), `effects` (`id`, `valuePerMinute`, `minSeconds`, `maxSeconds`, `amplifier`, `minDifficulty`, `weight`), `lootTables` (`id`, `value`, `tier`, `minDifficulty`, `weight`), `messages` |
| `templates.json` | `titles` and `phrases` per type, `variants`, `fallbackTitles`, `comboTitles`, `familyNames`, `tools`, `hints`, `sentences`, `dimensionNames` |
| `themes.json` | `themes[]`: `key`, `names`, `descriptions` (with `{list}`), `slots[]` (`types`, `keys`, `families`, `profiles`, `optional`), `requiresProfiles`, `minDifficulty`, `maxDifficulty`, `weight` |
| `balance.json` | every number of §12 and of this document; see §12 |
| `tags.json` | `concepts`: key → `kind`, `candidates` (tag ids in preference order), `name`, `plural` |

### 10.3 World overrides (no rebuild needed)

- `<world>/justquests/generator_v2/balance.json` — deep-merged over the bundled balance: objects
  merge key by key, any other value (numbers, arrays) replaces the bundled one. Example:
  `{"difficulties": {"EASY": {"targetMinutes": [3, 8]}}, "setRules": {"themeChance": 0.5}}`.
- `<world>/justquests/generator_v2/profiles/*.json` — extra profiles (same schema as bundled
  profiles). A world profile with the id of a bundled profile replaces it. Discovery uses
  `StateStore.list` and, as a fallback, `generator_v2/profiles/index.json`.
- Both are re-read on `updateConfig` (i.e. `/quest reload`); the changes apply from the next rotation.

## 11. Adding a mod: annotated profile example

Adding a mod is a **data-only** task: drop a JSON file into `catalog/profiles/` and list it in
`index.json` (or put it in the world's `generator_v2/profiles/`). Full example for a hypothetical mod:

```jsonc
{
  "format": 1,
  "id": "examplemod",                    // profile id (used by generatorDisabledProfiles, stats, explain)
  "name": "Example Mod",
  "requiresMod": ["examplemod", "examplemod_fabric"],   // any-of: mod ids per loader
  "loaders": ["neoforge", "fabric"],     // optional: only on these loaders
  "minecraft": {"min": "1.20.1", "max": "1.21.10"},   // optional: inclusive version range
  "dimensions": [                        // optional: modded dimensions and how they unlock
    {"id": "examplemod:crystal_caves", "advancement": "examplemod:enter_caves", "share": 0.25, "day": 20, "tier": 3}
  ],
  "entries": [
    {
      "key": "ruby", "family": "ex_gems", "tier": 2, "tool": "iron", "hints": ["deep"],
      "exclusions": ["collect_item examplemod:ruby_ore (drops rubies)"],
      "targets": [
        {"type": "mine_block", "id": "examplemod:ruby_ore", "alt": ["examplemod:ore_ruby"], "effort": 1.5, "min": 2, "max": 16},
        {"type": "collect_item", "id": "examplemod:ruby", "effort": 1.5, "min": 2, "max": 16, "hint": "Ruby ore drops rubies."},
        {"type": "craft_item", "id": "examplemod:ruby_block", "effort": 13.5, "min": 1, "max": 1, "minDifficulty": "hard"}
      ]
    },
    {
      "key": "glow_bats", "family": "ex_mobs", "tier": 3, "dimension": "examplemod:crystal_caves",
      "requires": ["dim:examplemod:crystal_caves"],
      "targets": [{"type": "kill_mob", "id": "examplemod:glow_bat", "effort": 1.2, "min": 4, "max": 16}]
    }
  ],
  "rewards": {
    "items": [{"id": "examplemod:ruby", "value": 3.0, "tier": 2, "max": 8, "family": "ex_gems"}]
  },
  "themes": [
    {"key": "ex_jeweler", "names": ["Jeweler's Order"], "descriptions": ["The jeweler needs stones: {list}."],
     "minDifficulty": "normal",
     "slots": [{"types": ["mine_block"], "keys": ["ruby"]},
               {"types": ["collect_item"], "keys": ["diamond"], "profiles": ["vanilla"]}]}
  ]
}
```

Rules that keep a profile safe:

- Every id is existence-checked at runtime; missing ids are skipped silently (counted as
  `3_missing_id`), so one file serves all mod versions and loaders.
- Only use hooks that fire on all three loaders (§7 of the spec): no `craft_item` for machine,
  stonecutter or smithing outputs; `collect_item` only for real item-entity drops; `tame_animal` only
  for `TamableAnimal` subclasses (`"tamable": true`); `consume_item` for food cooked in modded
  machines.
- Families should be mod-prefixed (`ex_gems`), so the family rule and reward exclusion work.
- Themes are only used when every profile in `requiresProfiles` (the owning profile is added
  automatically) is active.
- Give an English `name` (and `plural` if needed) wherever the prettified id reads badly
  (`ingot_osmium` → "Osmium Ingot"): dedicated servers have no mod language files.
- Put `since` on content that some versions register before it is obtainable (experimental feature
  flags, like pale oak in 1.21.2/1.21.3).
- A modded dimension needs a `dimensions` rule and `requires: ["dim:<id>"]` on its entries, so
  nothing is generated before the world unlocks it.
- Run the tests (`CatalogIntegrityTest`) after adding a bundled profile; add the family nouns to
  `templates.familyNames` for nicer combo titles. `./gradlew catalogReport` shows which targets each
  difficulty can use, and `tools/verify_mods.py` checks ids and recipes against the mod's own data.

## 12. Tuning guide

All numbers live in `balance.json` (or a world override). The most useful knobs:

| Goal | Change |
|---|---|
| Quests too long/short on a difficulty | `difficulties.<D>.targetMinutes` |
| Rewards too stingy/generous | `difficulties.<D>.rewardRate`, `budgetClamp`; per item `value` in `rewards.json` |
| Less XP, more items | `rewards.itemShare` ↑, `rewards.xpPointsPerValue` ↓ |
| More multi-objective quests | `difficulties.<D>.objectiveWeights` |
| Fewer place/tame quests | `typeWeights` (global) or `difficulties.<D>.typeWeights` (per difficulty multiplier) |
| Deeper content earlier | `difficulties.<D>.tierWeights`, `progression.earlyWorld`, `netherUnlockDay` |
| One specific target too slow/fast | its `effort` in the catalog (or enable `adaptiveBalancing` and let the stats do it) |
| Biome-bound content too hard | `hintMultipliers` |
| Themes too frequent | `setRules.themeChance` |
| Quests too far below their target time | `setRules.minTargetFraction` ↑ (more rejected attempts) |
| Claim expiry per difficulty | `difficulties.<D>.claimExpiryHours` (used when `generatorClaimExpiryHours` is 0) |
| Many mods crowd out vanilla in small sets | `setRules.maxPerModShare` (default 0.5) or `generatorModdedShare` |
| A mod's dimension opens too early/late | its profile's `dimensions[].share` / `day` |

Before and after a change, `./gradlew catalogReport` lists every target's minute range and
difficulties, and `./gradlew simulate` shows the effect on 14 simulated days (variety, completion
rates, calibration ratio).

Workflow for the test phase: play with `generatorStats: true`, look at `/quest generator stats`
(completion rate per type/tier, median observed vs. estimated minutes, relaxations, rejections), then
adjust `effort`/`targetMinutes` or turn on `adaptiveBalancing`.

## 13. Assumptions

Decisions taken where the specification left room:

1. **`start(Map<String, UUID>)` cannot carry two holders** of one quest; the added
   `startWithHolders(Map<String, Collection<UUID>>)` does (needed for §10.5 legacy double holders and
   for exclusivity off). `start` delegates to it.
2. **`releaseOnAbandon = false`**: the abandoned current-cycle quest leaves the served set (nobody can
   take it again this cycle). Keeping it "claimed" by someone who abandoned it would block the
   one-active rule.
3. **Retained quests are only for their holders.** With exclusivity off, a player who does not hold a
   retained (older-cycle) quest gets `NOT_SERVED` ("never re-offer stale quests").
4. **`tryClaim` checks "already yours" before "completed"**, so a legacy second holder can still
   finish after the first completed. In normal exclusive operation both cannot be true at once.
5. **Completed quests stay served until the next rotation**, including completed retained quests.
6. **Reroll** keeps claimed *and* completed quests of the current cycle and fills up to
   `questsPerCycle`; if a boundary has passed before the next `tick()`, the reroll starts the new
   cycle instead. Rerolled-away signatures stay in the history (a reroll should bring new content).
7. **History** stores a signature when it is generated (not when claimed); the window is
   `historyDays` (6) measured from the last generation.
8. **Claim expiry**: `generatorClaimExpiryHours > 0` wins; otherwise the difficulty's
   `claimExpiryHours` from `balance.json` (default 0 everywhere). Expiry of a current-cycle quest makes
   it available again; of a retained quest removes it.
9. **Enable/disable** keeps all state and claims; a disabled generator serves nothing, denies claims
   with `DISABLED`, and `tick()` does nothing. Re-enabling rotates at once if the cycle is outdated.
10. **Catch-up** generates only the latest cycle; the skipped cycles never existed (no history for
    them).
11. **Nether/End unlock thresholds**: End by day 40 (spec says "a later day threshold").
12. **Modded share** counts a quest as modded when any objective comes from a mod profile (including
    profile targets with vanilla ids, e.g. baking bread from Farmer's Delight dough). The target is
    `round(N × share)` quests; `moddedShare = 0` means vanilla only.
13. **Quick quest**: estimate ≤ `min + (max − min) / 3` of the difficulty range; on Hard this may be
    below the range minimum (a short errand in an expedition set).
14. **Effort values** are estimates for an average survival player with a basic base (a farm, a few
    animals, iron tools by the mid game). Consume efforts include getting hungry again.
15. **Loot tables** are not verifiable through the host; only vanilla tables that exist in every
    version 1.18.2+ are listed.
16. **Tags on mod content:** the bundled mods use the same namespace on every loader, so tags are only
    used for interchangeable metals (`zinc_ingots`, `osmium_ingots`, `tin_ingots`, `lead_ingots`,
    with `c:` and `forge:` candidates).
    Discovery of extra targets from a mod's tags (spec §9.2 c, a SHOULD) is not implemented — the
    profiles are explicit.
17. **Time zone** defaults to the system zone (`ZoneId.systemDefault()`), as §13 specifies; tests pin
    UTC.
18. **Stats** keep the last 300 detailed records; everything older is aggregated.
19. **Names** in descriptions are lower-cased except proper nouns (Nether, End, TNT).
20. **`servedRevision()`** was added so the mod can re-register lazily after `onAbandon`/
    `releaseAllFor`/`forceRelease` removed a retained quest (these methods return `void`/lists in the
    spec'd signatures).
21. **One quest per active mod, capped.** The spec asks for a `moddedShare` target *and* at least one
    quest per active mod for N ≥ 5, which conflict when more mods are active than a small set can hold
    (five bundled profiles, N = 5 would be all modded). The guarantee takes at most
    `setRules.maxPerModShare` (0.5) of the set, or the modded target if that is larger. When it cannot
    cover every mod, the cycle's RNG picks a rotating subset and the relaxation `per_mod` is counted.
    For N ≥ 2 × active mods the rule holds literally.
22. **Feature-flag leaks** are handled by versions, not by the host alone. Minecraft registers the
    content of the next release behind experimental flags (cherry in 1.19.4, breeze and copper grates
    in 1.20.3/1.20.4, bogged in 1.20.5/1.20.6, pale oak in 1.21.2/1.21.3; the sculk sensor exists in
    1.18.2 but cannot be obtained in survival). The catalog marks such content with `since`, and step 3
    rejects it when the running version is older. An unparsable version string disables only this
    guard. The host should still check `isEnabled(enabledFeatures())` (`INTEGRATION.md` §2.2).
23. **Stored quests are re-validated at start** (schema check and host validator). A state file may
    be edited by hand, or a content mod removed since the last run. Invalid quests are dropped; a player
    who still has one active gets it reported in `StartResult.deadQuestIds()`, and the current set is
    topped up to `questsPerCycle`. When the check itself fails (host exception or a `null` answer), the
    quest is kept. v1-imported quests skip the core's schema check and rely on the host validator.
24. **Host display names are cleaned** before use (see §6). A mod can report formatting codes, an
    untranslated key or a very long name; the id's prettified form is the fallback.
25. **`status()` ends with `Minecraft <version> (<loader>)`**, from `ContentView`, to make porting
    problems visible in one line.
26. **Mod reward tiers**: rewards from a dimension profile use tier 3+ (a reward may be one tier above
    its quest), so the Twilight Forest's items do not appear on the first quests of a world.

## 14. Known limitations

- **Effort numbers are unplayed estimates.** They were derived from game mechanics (drop rates, spawn
  conditions, crafting inputs), not measured. The test phase and the stats exist to tune them.
- **Biome availability is not checked.** A world without deserts can still get a husk quest (the
  description says "Look in deserts."). The core has no biome API; hints and multipliers make such
  quests slower but not impossible to understand.
- **`place_block` can be farmed** (place/break loops); it is weighted low and mostly on Easy.
- **Cheap single crafts are rare.** A loom or a pair of iron boots is shorter than the time range of
  any difficulty, so it fits only the quick slot or a theme (Workshop Setup, Adventurer's Kit), and
  about 3 % of the usable targets did not appear in 18000 test quests.
- **`consume_item` depends on hunger** — the counts are kept small, but a player with a full hunger
  bar must wait. Always-edible items (chorus fruit, milk) are unaffected.
- **Wall-clock claim durations** include offline time; self-calibration therefore stays off by
  default.
- **Recipe checks** depend on the host; with `UNKNOWN` answers the catalog must be right. A data pack
  that removes a vanilla crafting recipe is only noticed if the host answers `NO`.
- **Modded names on dedicated servers** come from the catalog or the prettified id (the server has no
  modded `en_us`); the client shows the real item names in the objective list anyway.
- **Machine outputs** of Create (brass, sheets, sweets), Mekanism (steel, alloys, circuits) and Botania
  (apothecary, runic altar, mana infusion) cannot be `craft_item`/`smelt_item` targets; they appear
  only as `consume_item`, as inputs mentioned in hints, or as rewards.
- **World-generation settings are invisible.** A pack that turns off Mekanism's ores or salt still
  gets those quests; such packs disable the profile or override the entries in a world profile.
- **Mod progression locks are not modelled** beyond dimension unlocks (for example, the Twilight
  Forest's biome effects before its bosses are defeated); boss quests are Hard-only.
- **Only English** (as required).

## 15. Tools, verification and tests

The data files are authored with the Python scripts in `tools/` at the repository root (not part of
the deliverable ZIP's build):

| Script | Purpose |
|---|---|
| `tools/build_data.py` | writes every JSON file under `src/main/resources/justquests_genv2/` from `vanilla_catalog.py`, `vanilla_more.py`, `mod_profiles.py`, `mod_more.py`, `mod_mekanism.py`, `mod_twilightforest.py`, `mod_botania.py` and `other_data.py` |
| `tools/verify_vanilla.py` | checks every vanilla target, reward item, effect, loot table and tag against Minecraft's own registries and recipes (misode/mcmeta data) for 17 versions from 1.18.2 to 1.21.10: 458 targets, 83 reward items, 0 problems; notes for feature-flag leaks, all covered by `since` |
| `tools/verify_mods.py` | checks the five profiles against the mods' language files and recipe data (`MODS.md` §7): 267 targets, 0 problems |

Gradle tasks that write documentation from the real code:

| Task | Output |
|---|---|
| `./gradlew samples` | `samples/{vanilla,modded,mc-1.18.2}/`: 27 sets with `status()` and `explain()` |
| `./gradlew catalogReport` | `samples/catalog-report.md`: every target with count range, minutes, E/N/H |
| `./gradlew simulate` | `samples/simulation/*.md`: 14 simulated days per difficulty with the statistics text |

The tests (28 classes, 111 tests, a little over a minute) cover the 17 groups of the specification plus:

| Test | What it proves |
|---|---|
| `HostFuzzTest` | random TriStates, missing ids, content calls that throw, hostile display names, a broken world/capabilities/validator: nothing escapes the facade, every served quest is valid and uses only ids the host confirmed, and the set fills again once the host recovers |
| `ClaimsPropertyTest` | 12 configurations × 300 random operations (claim, complete, abandon, release, force release, time jumps, reroll, lossy restarts, disable/enable) checked against a model of the mod's view after every step; a coverage check makes sure every claim result was exercised |
| `StateFuzzTest` | 250 mutated or truncated state and stats files, 3000 mutated quest JSONs through the schema check, and a content mod removed between two runs |
| `OldVersionTest` | the since-guard on eleven versions from 1.18.2 to 26.1: flagged content never appears, sets still fill, unparsable versions only disable the guard |
| `TextQualityTest` | 3600 quests with all profiles: spacing, punctuation, articles, repeated words or sentences, counts of one |
| `EnglishTest` | plurals of the names the catalogs use, mass nouns, articles |
| `ModProfilesTest` | each mod profile activates only with its mod, the per-mod guarantee (and its cap), readable modded names, dimension unlocks, Hard-only bosses and machines |
| `ThemeCoverageTest`, `RewardCoverageTest`, `TargetCoverageTest` | over 18000 generated quests with all profiles: every theme is built and every reward handed out; at least 95 % of the usable targets are picked (cheap single crafts fit only the quick slot or a theme and stay rare) |
| `WorldOverrideSamplesTest` | the copy-ready examples in `samples/world-overrides/` load without warnings and take effect |

