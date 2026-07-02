# Multi-loader port status

JustQuests started as NeoForge-only. This tracks bringing it to **Fabric** and
**Forge** across MC 1.18–1.21. It's a multi-session effort — this file is the
source of truth for what's done and what's next.

## Status matrix

Legend: ✅ done & building · 🚧 in progress · ⬜ todo · ❌ not possible

| MC line | NeoForge | Fabric | Forge |
|--------|----------|--------|-------|
| 1.21.x | ✅ 1.21, 1.21.1–1.21.10 (not .11) | 🚧 1.21, 1.21.1 done; 1.21.2–1.21.10 todo | ⬜ |
| 1.20.x | ✅ 1.20.4, 1.20.6 only | ⬜ 1.20.1/1.20.4/1.20.6 … | ⬜ 1.20.1 (Forge, not NeoForge) … |
| 1.19.x | ❌ (no NeoForge) | ⬜ 1.19.2, 1.19.4 | ⬜ 1.19.2, 1.19.4 |
| 1.18.x | ❌ (no NeoForge) | ⬜ 1.18.2 | ⬜ 1.18.2 |

Notes:
- NeoForge only exists from 1.20.2 up, so 1.20.1 / 1.19 / 1.18 NeoForge cells are ❌.
- Fabric & Forge exist for all of 1.18–1.21, so those cells are reachable.

## The recipe (per Fabric version)

Loom 1.11.8 works in the Gradle-9 multi-project next to MDG. Because Fabric
uses **Mojmap** (`loom.officialMojangMappings()`), the quest **domain code is
shared** — only the loader layer is rewritten.

1. `cp -r neoforge/<V>/src fabric/<V>/src` — pulls the domain + resources
   **with that version's fixes** (registry `get`/`getValue`, codec vs Gson
   reload listener, NBT optionals, `ClickEvent` records, etc.).
2. Delete NeoForge glue from the copy: `registry/`, `player/PlayerQuests.java`,
   `player/PlayerQuestEvents.java`, `storage/ServerStorageEvents.java`,
   `client/QuestClient.java`, `resources/META-INF/neoforge.mods.toml`.
3. Overlay the Fabric glue (copy from `fabric/1.21.1`): `JustQuestsFabric`,
   `client/JustQuestsFabricClient`, `event/FabricQuestHooks`, `mixin/*`,
   `JustQuests.java` (constants), `fabric.mod.json`, `justquests.mixins.json`.
4. Re-apply the 3 small edits to the copied domain files:
   - `QuestManager`: `implements IdentifiableResourceReloadListener` + import +
     `getFabricId()`.
   - `QuestCommand`: drop the `RegisterCommandsEvent` import + `onRegisterCommands`
     wrapper (keep `register(dispatcher)`).
   - `SelfTest`: `ModList`/`FMLPaths` → `net.fabricmc.loader.api.FabricLoader`.
5. Write `build.gradle` (loom + per-version `mcVersion` / fabric-api / loader).
6. Add the subproject to `settings.gradle`, build, fix what the compiler flags.

## Events on Fabric

Fabric API covers: death (`ServerLivingEntityEvents.AFTER_DEATH`), block break
(`PlayerBlockBreakEvents.AFTER`), dimension change
(`ServerEntityWorldChangeEvents`), login (`ServerPlayConnectionEvents.JOIN`),
server lifecycle/tick. The rest come from **mixins** in `mixin/`:

| Objective | Mixin | Target |
|-----------|-------|--------|
| collect_item | PlayerTakeMixin | `Player.take` |
| place_block | BlockItemPlaceMixin | `BlockItem.place` |
| craft_item | ResultSlotMixin | `ResultSlot.onTake` |
| tame_animal | TamableAnimalMixin | `TamableAnimal.tame` |
| gain_advancement | PlayerAdvancementsMixin | `PlayerAdvancements.award` |
| breed_animal | AnimalBreedMixin | `Animal.spawnChildFromBreeding` |
| consume_item | LivingEntityConsumeMixin | `LivingEntity.completeUsingItem` |
| smelt_item | FurnaceResultSlotMixin | `FurnaceResultSlot.onTake` |

## Known per-version gotchas (to watch when rolling out)

- **InteractionResult** became a sealed interface in **1.21.4+**; `.consumesAction()`
  in `BlockItemPlaceMixin` may need adjusting.
- **KeyMapping** category is a `KeyMapping.Category` (not String) in **1.21.9+**
  → `JustQuestsFabricClient`.
- **NBT** getters return `Optional` from **1.21.5** (already handled in the
  copied domain code from neoforge/<V>).
- Older lines (1.18–1.20) have big API shifts and different mixin targets; the
  1.20.4 NeoForge backport notes (pre-1.20.5) largely apply to Fabric 1.20.4 too.
- **Runtime** (whether each mixin actually fires) is NOT verified by the build —
  needs an in-game test per major line.

## Versioning

`mod_version` is 0.2.1. The multi-loader jars ship in a release once enough
targets are ready (a big enough feature that 0.3.0 would be the honest number).
