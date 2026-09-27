# Reference host adapter — NeoForge 1.21.1

**REFERENCE ONLY — NOT COMPILED.** These files are not part of the Gradle build of the core (the core
must not see Minecraft classes, and this environment has no Minecraft toolchain). They show a
complete `GeneratorHost` for the NeoForge 1.21.1 tree of JustQuests and the wiring glue, written
against that tree's classes (`JustQuests.LOG`, `QuestManager`, `Quest.CODEC`, `QuestNetwork`,
`WorldQuestStore`, `WorldSettings`). Copy them into `neoforge/1.21.1/src/main/java/com/erikedits/justquests/generator/`,
compile, and adapt per build.

| File | Role |
|---|---|
| `GenV2Host.java` | `GeneratorHost` + all six views (registries, tags, recipes, advancements, world files, codec, capabilities, log) |
| `GenV2.java` | Holder of the one instance per server + the glue from `INTEGRATION.md` §3 (start, tick, register, expired claims, config from settings) |

Other API eras have their own folders next to this one; [`../README.md`](../README.md) has the
version matrix. NeoForge 1.21 uses the same files. For 1.20.5/1.20.6 replace `ResourceLocation.parse(s)`
with `new ResourceLocation(s)`.

## Wiring in `ServerStorageEvents`

```java
@SubscribeEvent
public void onServerStarting(ServerStartingEvent event) {
    WorldQuestStore.load(event.getServer());
    WorldSettings.load(event.getServer());
    CustomQuestLoader.init(event.getServer());
    GenV2.start(event.getServer());           // was GeneratedQuestStore.init(server)
    CommunityHints.init(event.getServer());
}

@SubscribeEvent
public void onServerStopping(ServerStoppingEvent event) {
    GenV2.stop();                             // was GeneratedQuestStore.clear(); first, so it saves
    WorldQuestStore.unload();
    CustomQuestLoader.clear();
    CommunityHints.clear();
    WorldSettings.reset();
}
```

In `onServerTick` the existing 6000-tick counter calls `GenV2.tick(server)` instead of
`GeneratedQuestStore.tickCheck()`. Command and progress hooks: `INTEGRATION.md` §3.3 – §3.9.
