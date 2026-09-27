# Reference host adapters

**REFERENCE ONLY — NOT COMPILED.** Nothing in this folder is part of the Gradle build. The core must
not see Minecraft classes, and the build environment has no Minecraft toolchain. Each folder holds a
complete `GeneratorHost` and the glue class for one API era of the JustQuests source trees. They are
written against the real classes of those trees (`JustQuests.LOG`, `Quest.CODEC`, `QuestManager`,
`WorldQuestStore`, `PlayerQuestData`, `WorldSettings`, `LocalizedText`, `QuestNetwork`). Copy the two
files into `<tree>/src/main/java/com/erikedits/justquests/generator/`, then compile and adjust.

| Folder | Trees it fits | Notes |
|---|---|---|
| [`neoforge-1.21.1/`](neoforge-1.21.1/) | NeoForge 1.21 – 1.21.1 (1.20.5/1.20.6: use `new ResourceLocation`) | the first adapter; `RecipeHolder`, `AdvancementHolder`, feature flags |
| [`fabric-1.21.1/`](fabric-1.21.1/) | Fabric 1.21 – 1.21.1 (1.20.5/1.20.6 as above) | same Mojmap code; `FabricLoader`; start on `SERVER_STARTED` |
| [`forge-1.20.1/`](forge-1.20.1/) | Forge/Fabric 1.19.4 – 1.20.1; NeoForge/Fabric 1.20.4 with the notes in its README | no `RecipeHolder`/`AdvancementHolder`; `new ResourceLocation` |
| [`legacy-1.18.2/`](legacy-1.18.2/) | Forge and Fabric 1.18.2 (1.19.2 with the notes in its README) | `Registry.ITEM`, `TextComponent`, no feature flags, no client sync |
| [`neoforge-1.21.4-plus/`](neoforge-1.21.4-plus/) | NeoForge/Fabric 1.21.2 – 1.21.11 | notes only: what changes against the 1.21.1 adapter |

The NeoForge 1.20.2 and 1.20.3 trees have no generated-quest support at all (no `generator` package,
no `QuestManager.setGeneratedQuests`, no `QuestNetwork`). They need the v1 plumbing ported first; after
that the `forge-1.20.1` adapter applies with the 1.20.2 changes below.

## API differences at a glance

| Topic | 1.18.2 | 1.19.2 | 1.19.4 – 1.20.1 | 1.20.2 – 1.20.4 | 1.20.5 – 1.20.6 | 1.21 – 1.21.1 | 1.21.2+ |
|---|---|---|---|---|---|---|---|
| Registries | `Registry.ITEM` | `Registry.ITEM` | `BuiltInRegistries.ITEM` | same | same | same | `getValue(rl)` returns the element, `get(rl)` an `Optional` holder |
| Tag lookup | `Registry.ITEM.getTag(TagKey.create(Registry.ITEM_REGISTRY, rl))` | same | `BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, rl))` | same | same | same | `BuiltInRegistries.ITEM.get(TagKey)` (renamed) |
| Feature flags (experimental content) | none | none | `FeatureElement.isEnabled(server.getWorldData().enabledFeatures())` | same | same | same | same |
| Recipe list | `getAllRecipesFor(type)` → `Recipe` | same | same | → `RecipeHolder` | same | same | reworked (`recipeMap()`); answer `UNKNOWN` |
| Recipe result | `getResultItem()` | same | `getResultItem(RegistryAccess)` | same | same | same | no generic result; answer `UNKNOWN` |
| Max stack size | `item.getMaxStackSize()` | same | same | same | `item.getDefaultMaxStackSize()` | same | same |
| Use animation | `UseAnim` | same | same | same | same | same | `ItemUseAnimation` (renamed) |
| Advancement lookup | `getAdvancement(rl)` → `Advancement` | same | same | `get(rl)` → `AdvancementHolder` | same | same | same |
| Resource ids | `new ResourceLocation(s)`, `tryParse` | same | same | same | same | `ResourceLocation.parse(s)`, `tryParse` | same |
| Chat to a player | `sendMessage(new TextComponent(..), Util.NIL_UUID)` | `sendSystemMessage(Component.literal(..))` | same | same | same | same | same |
| Player language on the server | not available; use `LocalizedText.DEFAULT_LANG` | same | same | `clientInformation().language()` | same | same | same |
| Client sync (`QuestNetwork`) in the JustQuests trees | none | none | Forge/Fabric 1.20.1 only (none in 1.19.4) | NeoForge/Fabric 1.20.4 only | present | present | present |

"same" means unchanged from the column to its left. The recipe and animation answers are optional:
`TriState.UNKNOWN` is always a valid answer, because the catalog carries what the core needs. Only the
existence checks (`itemExists` and friends) must be exact. On 1.19.3+ they must include the
feature-flag check, because experimental content is registered even when the datapack is off (see
`INTEGRATION.md` §2.2). The catalog's `since` versions are a second line of defence for hosts that
cannot check flags.

## Loader wiring summary

| Loader | Server start | Tick | Stop |
|---|---|---|---|
| NeoForge / Forge | `ServerStartingEvent` (worlds are loaded) in `ServerStorageEvents.onServerStarting`, after `CustomQuestLoader.init` | `ServerStorageEvents.onServerTick`, the existing 6000-tick counter | `ServerStoppingEvent` |
| Fabric | `ServerLifecycleEvents.SERVER_STARTED`; **not** `SERVER_STARTING`, which fires before the worlds load (no seed, no overworld) | `ServerTickEvents.END_SERVER_TICK`, the existing `genCounter` | `ServerLifecycleEvents.SERVER_STOPPING` |

The command and progress hooks (`/quest accept`, `/quest abandon`, completion, reroll, admin reset)
live in shared domain code that is the same in every tree apart from the chat API. They are described
once in `INTEGRATION.md` §3.3 – §3.9. Each adapter's README shows the one line that differs.
