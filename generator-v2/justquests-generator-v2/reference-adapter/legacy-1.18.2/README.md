# Reference host adapter — Forge and Fabric 1.18.2

**REFERENCE ONLY — NOT COMPILED.** One pair of files for both loaders of 1.18.2. The loader name and
the "is mod loaded" check are passed in by the entry point:

```java
// Forge (ServerStorageEvents.onServerStarting, after CustomQuestLoader.init):
GenV2.start(event.getServer(), "forge", id -> net.minecraftforge.fml.ModList.get().isLoaded(id));

// Fabric (JustQuestsFabric.onInitialize):
ServerLifecycleEvents.SERVER_STARTED.register(server ->
    GenV2.start(server, "fabric", id -> net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(id)));
```

Copy `GenV2Host.java` and `GenV2.java` into `forge/1.18.2/...` or `fabric/1.18.2/src/main/java/com/erikedits/justquests/generator/`.
Tick, stop and reload wiring are as in [`../forge-1.20.1/`](../forge-1.20.1/) and
[`../fabric-1.21.1/`](../fabric-1.21.1/).

## What differs from 1.19.4+

| Place | 1.19.4+ | 1.18.2 |
|---|---|---|
| Registries | `BuiltInRegistries.ITEM` | `Registry.ITEM`, `Registry.BLOCK`, `Registry.ENTITY_TYPE` |
| Tag keys | `TagKey.create(Registries.ITEM, rl)` | `TagKey.create(Registry.ITEM_REGISTRY, rl)` (also `BLOCK_REGISTRY`, `ENTITY_TYPE_REGISTRY`) |
| Existence | registered **and** enabled by feature flags | registered (there are no feature flags) |
| Recipe result | `getResultItem(RegistryAccess)` | `getResultItem()` |
| Chat | `sendSystemMessage(Component.literal(..))` | `sendMessage(new TextComponent(..), Util.NIL_UUID)` |
| Client sync | `QuestNetwork.syncAll/syncProgress` | none: the 1.18.2 trees have no `network` package |

A denied claim in `QuestCommand.accept` (`INTEGRATION.md` §3.3) reads on 1.18.2:

```java
if (!claim.proceed()) {
    ctx.getSource().sendFailure(new TextComponent("§c" + claim.denyMessage()));
    return 0;
}
```

1.18.2 registers the sculk sensor, which cannot be obtained in survival before 1.19. There is no
feature flag to detect that. The catalog marks such content with `"since": "1.19"`, and the core
compares it with `ContentView.minecraftVersion()`. So `minecraftVersion()` must return the real
version (`SharedConstants.getCurrentVersion().getName()`).

Minecraft 1.18.2 ships Gson 2.8.8 and runs on Java 17, which are exactly the core's compile targets.
The core JAR can be shaded or jar-in-jar'd unchanged.

## 1.19.2

The 1.19.2 trees keep `Registry.ITEM`, `getResultItem()` and `new ResourceLocation`, but chat is
`sendSystemMessage(Component.literal(..))` as in 1.19.4+. Take this adapter and replace the one chat
line in `GenV2.applyExpired`. The 1.19.2 trees have no `network` package either.
