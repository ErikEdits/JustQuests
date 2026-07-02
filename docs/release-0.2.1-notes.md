# JustQuests 0.2.1 — full breakdown

The multi-loader release. 0.2.1 started as "add a couple more NeoForge
versions" and grew into bringing JustQuests to **three loaders** across
Minecraft **1.18 – 1.21**.

## 1. What ships in 0.2.1 — 34 jars

| Loader | Count | Versions |
|--------|-------|----------|
| **NeoForge** | 13 | 1.20.4, 1.20.6, 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10 |
| **Fabric** | 17 | 1.18.2, 1.19.2, 1.19.4, 1.20.1, 1.20.4, 1.20.6, 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10 |
| **Forge** | 4 | 1.18.2, 1.19.2, 1.19.4, 1.20.1 |

Coverage matrix (✅ done · ❌ not possible):

| MC | NeoForge | Fabric | Forge |
|----|----------|--------|-------|
| 1.21.x | ✅ 1.21–1.21.10 | ✅ 1.21–1.21.10 | ❌ redundant/infeasible |
| 1.20.x | ✅ 1.20.4, 1.20.6 | ✅ 1.20.1, 1.20.4, 1.20.6 | ✅ 1.20.1 (❌ 1.20.2+) |
| 1.19.x | ❌ no NeoForge | ✅ 1.19.2, 1.19.4 | ✅ 1.19.2, 1.19.4 |
| 1.18.x | ❌ no NeoForge | ✅ 1.18.2 | ✅ 1.18.2 |

## 2. What we did (feature-wise)

- **NeoForge 1.20.4 + 1.20.6** added (1.20.4 is a real pre-1.20.5 backport;
  1.20.6 is close to 1.21.1).
- **Fabric port from scratch** for all of 1.18–1.21 (17 versions). Uses Mojmap
  (`loom.officialMojangMappings()`) so the quest domain code is shared with the
  NeoForge build; only the loader layer is Fabric-specific:
  - `ModInitializer` + `ClientModInitializer`, `CommandRegistrationCallback`,
    reload listener, server lifecycle/tick, and Fabric API events (death,
    block-break, dimension, login).
  - **8 mixins** for events Fabric API lacks: item pickup, block place, craft,
    tame, advancement, breed, consume, smelt.
- **Forge port** for the versions where NeoForge doesn't exist (1.18–1.20.1),
  built with ModDevGradle's `legacyforge` plugin. Forge 1.20.1 forked from
  NeoForge at 1.20.1, so the glue mirrors the NeoForge build with
  `net.minecraftforge` packages.
- **Build tooling:** Java 17 + 21 toolchains (pre-1.20.5 needs 17); CI installs
  both; `./gradlew exportJars` now sorts jars into
  `Desktop/Justquests/{neoforge,fabric,forge}/`.

## 3. Bugs found & fixed

- **Fabric `minecraft` version over-match (fixed).** Every `fabric.mod.json`
  used `"minecraft": "~1.X.Y"`. In Fabric SemVer that means `>=1.X.Y <1.(X+1)`,
  i.e. the 1.21.1 jar declared itself compatible with **all** of 1.21.x. A user
  could force-load the 1.21.1 jar onto 1.21.5, where the intermediary mappings
  differ → crash. Fixed to pin each jar to its **exact** MC version. (Forge's
  `mods.toml` already used bounded ranges like `[1.20.1,1.20.2)`, so it was fine.)
- **Dozens of per-version API shims applied during porting** (these were the
  bulk of the work — each is a real incompatibility that would otherwise break
  a version). Highlights:
  - Pre-1.20: `LootParams`→`LootContext.Builder`; `entity.level()`→`getLevel()`;
    `sendSuccess(Supplier)`→`sendSuccess(Component)`; `renderBackground` 1-arg.
  - Pre-1.19.3: `BuiltInRegistries`→`Registry`; `Registries.ITEM`→
    `Registry.ITEM_REGISTRY`; `UUIDUtil.STRING_CODEC`→inline codec.
  - 1.18.2: `Component.literal/translatable`→`new TextComponent/…`;
    `sendSystemMessage`→`sendMessage(c, NIL_UUID)`; `broadcastSystemMessage`→
    `broadcastMessage(c, ChatType.SYSTEM, …)`; `performPrefixedCommand`→
    `performCommand`; `CommandSourceStack.getPlayer()`→null-safe cast; Fabric
    command `v2`→`v1`; kill_mob via a `LivingEntity.die` mixin (no
    `ServerLivingEntityEvents` yet).
  - NeoForge 1.20.4 backport: DataFixerUpper `getOrThrow()`→`result().orElseThrow()`,
    non-Holder `MobEffect`, `LootDataManager` loot tables, `TickEvent` phases,
    `Mod.EventBusSubscriber`.

## 4. What does NOT work — and why

- **NeoForge for 1.20.1 / 1.19.x / 1.18.x** — impossible: **NeoForge only exists
  from 1.20.2 up.** Those are covered by Fabric and Forge instead.
- **NeoForge 1.20.2 / 1.20.3 / 1.20.5** — the build toolchain (MDG 2.x) can't
  consume their NeoForge artifacts: 1.20.2 predates the required `moddev-bundle`
  metadata; 1.20.3 and 1.20.5 only have `-beta` builds without it. Folders kept,
  excluded in `settings.gradle`.
- **NeoForge 1.21.11** — excluded: its upstream NeoForm package is broken
  (duplicate `mcp/client/Start.class` → empty merged jar). Will re-add when fixed.
- **Forge for 1.20.2+ / 1.21.x** — not built, on purpose:
  1. MDG `legacyforge` only supports the pre-NeoForge Forge era (≤1.20.1); its
     NeoForm step fails on 1.20.4 (verified). The alternative, ForgeGradle,
     doesn't support this repo's Gradle 9.2.1.
  2. It's **redundant** — after the 1.20.1 Forge→NeoForge split, NeoForge already
     covers 1.20.2, 1.20.4, 1.20.6 and 1.21–1.21.10. Forge there would be
     duplicate, lower-audience maintenance.
- **The in-game GUI (key J) on 1.18/1.19** — dropped: `GuiGraphics` doesn't
  exist before 1.20, and the current GUI is the throwaway interim one (being
  replaced by the community-voted design). 1.18/1.19 ship **command-only** (full
  `/quest` set + all objective tracking still work). 1.20.1 and 1.21.x keep the GUI.
- **Per-player language on 1.20.1 and older** — quest text falls back to
  **English**: `ServerPlayer.clientInformation()` doesn't exist before 1.20.2,
  and the private language field couldn't be read reliably. Newer versions still
  localize per player.

## 5. Not yet verified

- **Runtime behaviour is not tested.** Everything compiles and the mixin
  annotation processor validates every injection target against the game at
  build time (a wrong target fails the build), so confidence is high — but
  whether each event/mixin actually fires **in-game** has not been checked
  (the build environment is headless). Please smoke-test at least one Fabric
  and one Forge build before publishing (open the quest book, complete a quest,
  check pickup/craft/kill tracking).

## 6. Versioning / release

- `mod_version` stays **0.2.1** (your call). The NeoForge v0.2.1 GitHub Release
  is already published; the Fabric/Forge jars are on
  `Desktop/Justquests/{fabric,forge}/` ready to upload.
- **Modrinth upload:** the local `upload-modrinth.ps1` needs to set the **loader
  tag per jar** (neoforge / fabric / forge) from the filename before these can go
  up — a small script change, not done yet.

See `docs/multiloader-status.md` for the living status + the exact porting recipe.
