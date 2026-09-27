# JustQuests — Generator v2 (core)

A version-neutral, procedural quest generator for the JustQuests mod: plain Java 17 + Gson 2.8.8,
no Minecraft or loader classes. The identical core is copied into all 34 builds of the mod; each
build adds a thin host adapter (see `INTEGRATION.md` and `reference-adapter/`).

What it does

- Generates a shared set of quests every 12 hours (real clock, catch-up after downtime), with no
  repeats within 6 days.
- **Achievable by construction:** every target passes a 7-step pipeline (type supported, profile
  active, id exists on this build and version, hook-compatible on all loaders, tier/progression,
  history, the mod's real codec). Content that a version registers only behind an experimental
  feature flag (cherry in 1.19.4, breeze in 1.20.4, pale oak in 1.21.2 …) is kept out by the
  catalog's `since` versions.
- **Effort-based counts and value-based rewards:** 725 targets (458 vanilla, 267 modded) with minutes
  per unit, biome/tool/dimension modifiers, nice round counts, rewards worth the time spent
  (139 reward items, 13 effects, 9 loot tables).
- **Varied sets:** ten objective types, 76 themes (44 vanilla + 32 from mod profiles), at most one
  quest per family, type share cap, a quick quest in every set, graceful rule relaxation, text
  checked over thousands of sets (articles, plurals, punctuation).
- **Exclusive claiming** (first come, first served), **one active generated quest per player**,
  claimed quests survive rotations until finished (the v1 bug is fixed), optional claim expiry.
- **Difficulty** Easy / Normal / Hard, **progression awareness** (Nether, End and modded dimensions
  unlock per world), **explainability** (`explain`, `status`), anonymous **test-phase statistics**
  and optional **self-calibration**.
- **Five mod profiles**, each active only when its mod is installed, all verified against the mods'
  own data for 1.18.2 – 1.21.1: Farmer's Delight (121 targets, 12 themes), Create (70, 10),
  Mekanism (31, 4), The Twilight Forest (28, 4, with its own dimension unlock) and Botania (17, 2).
  More mods are a data-only addition (`DESIGN.md` §11).
- **Robust:** never throws to the mod; survives throwing or lying hosts, corrupt or hand-edited state
  files, removed mods (stored quests are re-validated at start), clock jumps and v1 data.

## Build and test

Requirements: JDK 17 or newer (the build emits Java 17 bytecode via `options.release = 17`).

```
./gradlew build          # compile, run all 111 tests (~75 s), build the jar and sources jar
./gradlew test           # tests only
./gradlew samples        # regenerate samples/ (vanilla, five mods, Minecraft 1.18.2; with explain output)
./gradlew catalogReport  # samples/catalog-report.md: every target, count range, minutes, difficulties
./gradlew simulate       # samples/simulation/: 14 simulated days per difficulty with statistics
./gradlew javadoc        # API documentation into build/docs/javadoc
./gradlew deliverableZip # build/dist/justquests-generator-v2.zip
```

Dependencies: `compileOnly com.google.code.gson:gson:2.8.8` (Minecraft provides Gson at runtime);
tests use Gson 2.8.8 and JUnit 5. Nothing else is on the main classpath.

## File tour

```
README.md                this file
INTEGRATION.md           exact wiring contract for the mod (host adapter, call order, settings, commands)
DESIGN.md                algorithms, formulas, data model, tuning guide, assumptions, limitations
MODS.md                  the five mod profiles: selection, licences, verification, included/excluded content
CHANGELOG-snippet.md     player-facing release notes
build.gradle, settings.gradle, gradlew, gradlew.bat, gradle/wrapper/

src/main/java/com/erikedits/justquests/generator/v2/
  QuestGeneratorV2.java        the facade (the only class the mod calls)
  api/                         host interfaces (GeneratorHost, ContentView, WorldContext, StateStore,
                               QuestValidator, HostCapabilities, GenLog) and result types
  internal/                    implementation (free to change): Core, Generation, catalog/, gen/,
                               state/, stats/, util/

src/main/resources/justquests_genv2/
  catalog/vanilla.json         vanilla content model (195 entries, 458 targets)
  catalog/profiles/            farmersdelight, create, mekanism, twilightforest, botania + index.json
  rewards.json                 reward items/effects/loot tables with values
  templates.json               English titles, phrases, hints, family and dimension names
  themes.json                  multi-objective archetypes
  balance.json                 every tunable number
  tags.json                    tag concepts with per-loader candidates

src/test/java/com/erikedits/justquests/generator/v2/
  FakeHost.java                configurable fake host with a fixed clock
  *Test.java                   28 test classes: the 17 groups of the specification, plus fuzzing
                               (host, state files), a claims property test, old versions, text quality,
                               English plurals, coverage of every theme, reward and target, and the
                               world-override examples
  SampleWriter, CatalogReport, Simulation   the three generators behind samples/

reference-adapter/           REFERENCE ONLY — NOT COMPILED: host adapters for NeoForge 1.21.1,
                             Fabric 1.21.1, Forge 1.20.1 and Forge/Fabric 1.18.2, notes for 1.21.2+
samples/                     generated example sets, catalog report, simulations and copy-ready world
                             overrides (see samples/README.md)
```

The data files are written by the Python scripts in `tools/` at the repository root
(`tools/build_data.py`), and checked against Minecraft's and the mods' own data by
`tools/verify_vanilla.py` and `tools/verify_mods.py`. The JSON files are the source of truth that
ships; the scripts are not needed to build.

## Using it (short version)

```java
QuestGeneratorV2 gen = new QuestGeneratorV2(host, GeneratorConfig.builder()
        .difficulty(Difficulty.NORMAL).questsPerCycle(5).build());
StartResult start = gen.startWithHolders(activeGeneratedQuestsByPlayer);  // or start(Map<String, UUID>)
register(gen.servedQuests());                                              // questId -> quest JSON

RotationResult r = gen.tick();                  // every ~5 minutes; re-register when r.changed()
ClaimResult c = gen.tryClaim(questId, player);   // in /quest accept, after the mod's own checks
gen.onAbandon(questId, player);                  // /quest abandon
gen.onComplete(questId, player);                 // after rewards were granted
String why = gen.explain(questId);               // OP debugging
gen.stop();                                      // server stopping
```

Everything is single-threaded (server thread), deterministic for identical inputs, and never throws
during normal operation. Details: `INTEGRATION.md`.

## Licence

Written for JustQuests (LGPL-3.0-only). The mod profiles contain only facts about the referenced
mods (ids, English names, which items have grid or furnace recipes); no code or assets of other mods
are included. Credits and licences of those mods: `MODS.md`.
