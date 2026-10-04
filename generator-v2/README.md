# Generator v2 — upstream project

The source of truth for the JustQuests quest generator v2 (shipped in 0.3.0). It is a standalone,
version-neutral Java 17 core (JDK + Gson only) with its own tests. The mod does **not** build this
folder: the core sources and data files are **copied** into all 34 version trees.

| Path | What |
|---|---|
| `justquests-generator-v2/` | Gradle project: core (`src/main/java/.../generator/v2`), data (`src/main/resources/justquests_genv2`), 111 JUnit tests, samples, reference adapters |
| `justquests-generator-v2/INTEGRATION.md` | The contract between the core and the mod (host adapter, call order, settings) |
| `justquests-generator-v2/DESIGN.md` | Algorithms, effort/reward models, data schemas, tuning guide, assumptions |
| `justquests-generator-v2/MODS.md` | The eleven supported mods (Farmer's Delight, Create, Mekanism, Twilight Forest, Botania; since 0.4.0 also AE2, Immersive Engineering, Biomes O' Plenty, Alex's Mobs, Tinkers' Construct, Ars Nouveau) |
| `tools/` | Python scripts that **generate** the JSON catalogs (`build_data.py`) and check them |

Built from the hand-off spec [`docs/generator-v2-spec.md`](../docs/generator-v2-spec.md).

## Changing the generator

1. Edit the core or, for catalog content, the Python sources in `tools/` and run
   `python tools/build_data.py` (writes the JSON files into the project).
2. Test: `cd justquests-generator-v2 && ./gradlew test` (Java 17+).
3. Copy the result into all 34 trees: `python scripts/sync_generator_v2.py` from the repo root.
4. Build the mod (CI builds all 34 on push).

Never patch the copied core inside a version tree — everything build-specific lives in the per-build
adapter (`generator/GenV2Host.java`, `generator/GenV2.java`).

## Known limitations

- The mod ids in the five profiles were researched without access to Modrinth; every id is
  existence-checked at runtime, so a wrong id only means fewer quests for that mod.
- The in-game behaviour (claims, rotation, progression unlocks) is covered by the core's tests and
  simulation, and was play-tested on every loader and version (October 2026).
