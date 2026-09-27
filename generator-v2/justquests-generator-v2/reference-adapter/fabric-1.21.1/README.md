# Reference host adapter — Fabric 1.21.1

**REFERENCE ONLY — NOT COMPILED.** Same code as [`../neoforge-1.21.1/`](../neoforge-1.21.1/) (the
Fabric trees of JustQuests use Mojang mappings too). Only these parts differ:

- `GenV2Host.loaderName()` returns `"fabric"`; `isModLoaded` uses
  `FabricLoader.getInstance().isModLoaded(id)`.
- The start runs on **`ServerLifecycleEvents.SERVER_STARTED`**. The tree runs its own loaders in
  `SERVER_STARTING`, which fires before the worlds are loaded (no overworld, no seed, tags not bound).

Copy `GenV2Host.java` and `GenV2.java` into `fabric/1.21.1/src/main/java/com/erikedits/justquests/generator/`.
Fits Fabric 1.21 as well; for 1.20.5/1.20.6 replace `ResourceLocation.parse(s)` with `new ResourceLocation(s)`.

## Wiring in `JustQuestsFabric.onInitialize()`

```java
ServerLifecycleEvents.SERVER_STARTING.register(server -> {
    WorldQuestStore.load(server);
    WorldSettings.load(server);
    CustomQuestLoader.init(server);
    // GeneratedQuestStore.init(server);      // v1: removed
    CommunityHints.init(server);
    JustQuests.LOG.info("JustQuests loaded (Fabric)");
});
ServerLifecycleEvents.SERVER_STARTED.register(GenV2::start);   // v2: worlds and tags are ready here
ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
    GenV2.stop();                             // first: saves the generator state
    WorldQuestStore.unload();
    CustomQuestLoader.clear();
    // GeneratedQuestStore.clear();           // v1: removed
    CommunityHints.clear();
    WorldSettings.reset();
});
ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
    if (success) GenV2.reloadConfig(server);  // recipe caches and settings after /reload
});
```

In the `END_SERVER_TICK` handler replace the v1 call:

```java
if (++genCounter >= GEN_INTERVAL_TICKS) {
    genCounter = 0;
    GenV2.tick(server);                       // was GeneratedQuestStore.tickCheck()
}
```

The command and progress hooks are the same as in every tree: `INTEGRATION.md` §3.3 – §3.9
(`ctx.getSource().sendFailure(Component.literal("§c" + claim.denyMessage()))` for a denied claim).
