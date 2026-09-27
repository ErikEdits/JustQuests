# Reference host adapter — Forge 1.20.1

**REFERENCE ONLY — NOT COMPILED.** Derived from [`../neoforge-1.21.1/`](../neoforge-1.21.1/) for the
1.19.4 – 1.20.1 API era. Differences:

| Place | 1.21.1 adapter | This adapter |
|---|---|---|
| Mod list | `net.neoforged.fml.ModList` | `net.minecraftforge.fml.ModList` |
| Recipes | `RecipeHolder<?>` → `holder.value().getResultItem(registryAccess)` | `Recipe<?>` → `recipe.getResultItem(registryAccess)` |
| Max stack size | `item.getDefaultMaxStackSize()` | `item.getMaxStackSize()` |
| Advancements | `server.getAdvancements().get(rl)` → `AdvancementHolder` | `server.getAdvancements().getAdvancement(rl)` → `Advancement` |
| Resource ids (glue) | `ResourceLocation.parse(s)` | `new ResourceLocation(s)` |
| Quest title in the expiry message | `q.title().get(p.clientInformation().language())` | `q.title().get(LocalizedText.DEFAULT_LANG)`, as the tree does |

Registries (`BuiltInRegistries`, `Registries`), tags and the feature-flag check are the same as in
1.21.1. Copy both files into `forge/1.20.1/src/main/java/com/erikedits/justquests/generator/`.

## Other trees of this era

- **Forge / Fabric 1.19.4**: same code, but the trees have no `network` package. Drop the two
  `QuestNetwork` calls in `GenV2` (players see the new set through `/quest list`).
- **Fabric 1.20.1**: `FabricLoader.getInstance().isModLoaded(id)`, loader name `"fabric"`, and start on
  `SERVER_STARTED` (see [`../fabric-1.21.1/README.md`](../fabric-1.21.1/README.md)).
- **NeoForge / Fabric 1.20.4** (and 1.20.2/1.20.3 once they have the v1 plumbing): recipes are
  `RecipeHolder<?>` and advancements `AdvancementHolder` from 1.20.2 (take those two methods from the
  1.21.1 adapter); `ServerPlayer.clientInformation().language()` exists; NeoForge's `ModList` is
  `net.neoforged.fml.ModList`.

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

// in onServerTick, the existing 6000-tick counter:
if (++genCounter >= GEN_INTERVAL_TICKS) {
    genCounter = 0;
    GenV2.tick(ServerLifecycleHooks.getCurrentServer());   // was GeneratedQuestStore.tickCheck()
}
```

`ServerLifecycleHooks` is `net.minecraftforge.server.ServerLifecycleHooks`. Alternatively keep the
server from `onServerStarting` in a field. After `/reload` call `GenV2.reloadConfig(server)` from the
tree's reload path (`AddReloadListenerEvent` or the `/quest reload` command).

Command and progress hooks: `INTEGRATION.md` §3.3 – §3.9, unchanged
(`ctx.getSource().sendFailure(Component.literal("§c" + claim.denyMessage()))`).
