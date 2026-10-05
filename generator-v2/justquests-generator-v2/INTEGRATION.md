# INTEGRATION — wiring generator v2 into the 34 builds

This is the contract between the version-neutral core (this project) and the mod. Everything the
mod needs is listed here; nothing in `internal` is part of the contract. Code snippets use the
NeoForge 1.21.1 class layout (`com.erikedits.justquests.*`, Mojmap); the same layout exists in every
build, only the Minecraft API calls differ (see §2 and `reference-adapter/`).

Contents

1. [Files to copy](#1-files-to-copy)
2. [Host adapter contract](#2-host-adapter-contract)
3. [Call order and trigger points](#3-call-order-and-trigger-points)
4. [Settings keys](#4-settings-keys)
5. [Suggested OP commands](#5-suggested-op-commands)
6. [Sync / GUI suggestion](#6-sync--gui-suggestion)
7. [Self-test hooks](#7-self-test-hooks)
8. [Optional mod enhancements](#8-optional-mod-enhancements)
9. [Migration notes (v1 → v2)](#9-migration-notes-v1--v2)
10. [Checklist per build](#10-checklist-per-build)

---

## 1. Files to copy

Copy these **unchanged** into every build (identical in all 34 trees):

| From this project | To each build |
|---|---|
| `src/main/java/com/erikedits/justquests/generator/v2/**` (all `.java` files, including `api/` and `internal/`) | `src/main/java/com/erikedits/justquests/generator/v2/**` |
| `src/main/resources/justquests_genv2/**` (all `.json` files) | `src/main/resources/justquests_genv2/**` |

Exact resource list:

```
justquests_genv2/balance.json
justquests_genv2/rewards.json
justquests_genv2/tags.json
justquests_genv2/templates.json
justquests_genv2/themes.json
justquests_genv2/catalog/vanilla.json
justquests_genv2/catalog/profiles/index.json
justquests_genv2/catalog/profiles/farmersdelight.json
justquests_genv2/catalog/profiles/create.json
justquests_genv2/catalog/profiles/mekanism.json
justquests_genv2/catalog/profiles/twilightforest.json
justquests_genv2/catalog/profiles/botania.json
```

Notes

- The core needs only the JDK and Gson (Minecraft ships Gson). Java 17 bytecode, so it also runs
  on the Java 17 builds (1.18.2–1.20.4).
- `justquests_genv2/` is deliberately **not** a Java package, so the NeoForge/Forge module system
  does not encapsulate it; the core reads it with
  `QuestGeneratorV2.class.getResourceAsStream("/justquests_genv2/...")`.
- Keep the core sources in sync across trees with a script (a straight copy); never patch them per
  build. Everything build-specific lives in the adapter.
- Delete `generator/QuestGenerator.java` and `generator/GeneratedQuestStore.java` only after the
  migration period (§9).

## 2. Host adapter contract

Write one adapter per build (the "host"), e.g. `generator/GenV2Host.java`, implementing
`GeneratorHost` and its six views. A complete NeoForge 1.21.1 reference implementation is in
`reference-adapter/neoforge-1.21.1/` (not compiled here; see its README).

General rules for every method:

- Called **only on the server thread**, from inside a facade call.
- **Never throw.** Return the documented fallback instead (`false`, empty set, `UNKNOWN`, `-1`,
  `null`). The core also catches exceptions from host calls, logs them and continues, but a
  throwing adapter floods the log.
- Ids are full strings (`minecraft:oak_log`). Tag ids come **without** `#`. Malformed ids → answer
  as "does not exist".

### 2.1 `GeneratorHost`

| Method | Return | Notes |
|---|---|---|
| `content()` | `ContentView` | Same instance for the whole server lifetime. |
| `world()` | `WorldContext` | Same instance. |
| `store()` | `StateStore` | Same instance. |
| `validator()` | `QuestValidator` | Same instance. |
| `capabilities()` | `HostCapabilities` | Same instance. |
| `log()` | `GenLog` | Must never be null (the facade constructor checks it). |
| `currentTimeMillis()` | `long` | `System.currentTimeMillis()`. Tests inject a fake clock. |

### 2.2 `ContentView` — registries, tags, recipes

| Method | Must return | Fallback | When called / cost |
|---|---|---|---|
| `loaderName()` | `"neoforge"`, `"fabric"` or `"forge"` (lower case) | — | Per rotation. Constant. |
| `minecraftVersion()` | e.g. `"1.21.1"` (`SharedConstants.getCurrentVersion().getName()` or a build constant) | `""` | Per rotation. Constant. |
| `isModLoaded(modId)` | NeoForge/Forge `ModList.get().isLoaded(id)`, Fabric `FabricLoader.getInstance().isModLoaded(id)` | `false` | Per rotation, a few calls. |
| `itemExists(id)` | registered **and enabled for this world**: `BuiltInRegistries.ITEM.containsKey(rl) && ITEM.get(rl).isEnabled(server.getWorldData().enabledFeatures())` (1.18.2–1.19.2: `Registry.ITEM.containsKey`, no feature flags before 1.19.3) | `false` | ~500 calls per rotation; must be cheap. |
| `blockExists(id)` | `BLOCK.containsKey` + `block.isEnabled(flags)` | `false` | same |
| `entityTypeExists(id)` | `ENTITY_TYPE.containsKey` + `type.isEnabled(flags)` | `false` | same |
| `dimensionExists(id)` | `server.levelKeys()` contains the key (dimensions of the **running** server) | `false` | Per rotation. |
| `itemTagMembers(tagId)` | ids of all items in the tag (resolved after datapack load) | empty set | ≤ 1 call per tag per rotation; cache per rotation if expensive. |
| `blockTagMembers(tagId)` | same for blocks | empty set | rarely |
| `entityTypeTagMembers(tagId)` | same for entity types | empty set | rarely |
| `hasCraftingRecipe(itemId)` | `YES` if any `RecipeType.CRAFTING` recipe outputs the item, else `NO` | `UNKNOWN` | Per candidate per rotation. **Cache** the set of crafting outputs per rotation. If the recipe API is awkward on a build (1.19.4 `RegistryAccess`, 1.21.2+ recipe rework), just return `UNKNOWN` — the catalog carries the truth. |
| `hasSmeltingRecipe(outputItemId)` | `YES`/`NO` for SMELTING, BLASTING and SMOKING outputs | `UNKNOWN` | same |
| `maxStackSize(itemId)` | default max stack size (`getDefaultMaxStackSize()` on 1.20.5+, `getMaxStackSize()` before) | `-1` | Per reward per rotation. |
| `isBreedableAnimal(id)` | `YES` if the entity class is an `Animal` subclass | `UNKNOWN` | Needs a class check; if you cannot get the class cheaply, return `UNKNOWN`. Never create entities. |
| `isTamableAnimal(id)` | `YES` if `TamableAnimal` subclass | `UNKNOWN` | same |
| `isConsumable(itemId)` | `YES` for food / drinkable (`UseAnim.EAT`/`DRINK`, food component) | `UNKNOWN` | same |
| `englishName(kind, id)` | English display name if the server knows it (vanilla `en_us` on dedicated servers) | `null` | Optional. Returning `null` everywhere is fine: the prettified id is used, and the catalogs name every target whose id reads badly. Formatting codes are stripped; untranslated keys and names over 40 characters are ignored. |

**Feature flags matter.** From 1.19.3 on, content of experimental data packs is *registered* even when
the world has the experiment off (cherry wood on 1.19.4, the breeze on 1.20.4, the bogged on 1.20.6,
pale oak on 1.21.2/1.21.3). Check `isEnabled(enabledFeatures)` as shown. The core adds a second guard:
a catalog target whose `since` version is newer than `minecraftVersion()` is rejected — so return the
real version string from `minecraftVersion()`.

A definite `NO` removes a catalog target; `UNKNOWN` keeps it. The core is designed to work with
every TriState `UNKNOWN` and every name `null` (this is tested).

### 2.3 `WorldContext`

| Method | Must return | Fallback |
|---|---|---|
| `worldSeed()` | overworld seed (`server.getWorldData().worldGenOptions().seed()`; ≤ 1.19.2 `worldGenSettings().seed()`) | `0` |
| `gameDay()` | `server.overworld().getGameTime() / 24000` | `0` |
| `onlinePlayerCount()` | `server.getPlayerList().getPlayerCount()` | `0` |
| `knownPlayerCount()` | `WorldQuestStore.get().allPlayers().size()` (≥ online) | online count |
| `onlineShareWithAdvancement(advId)` | share 0..1 of **online** players with the advancement done; `-1` if nobody is online or the advancement does not exist | `-1` |
| `isSingleplayer()` | `server.isSingleplayer()` | `false` |

Advancement lookup: `server.getAdvancements().get(rl)` (1.20.2+ returns `AdvancementHolder`), then
`player.getAdvancements().getOrStartProgress(adv).isDone()`. Called at most 4 times per rotation.

### 2.4 `StateStore`

Root: `<world>/justquests/` (`server.getWorldPath(LevelResource.ROOT).resolve("justquests")`).

| Method | Behaviour |
|---|---|
| `read(name)` | UTF-8 content of `<root>/<name>`, `Optional.empty()` if missing or unreadable. |
| `write(name, content)` | Create parent directories, write `<name>.tmp`, then `Files.move(tmp, target, REPLACE_EXISTING, ATOMIC_MOVE)` (fall back to a non-atomic move if the file system refuses). Never throw; log failures. |
| `list(dir)` *(default method)* | Regular file names directly inside `<root>/<dir>`, sorted; empty if missing. Used for `generator_v2/profiles/`. |

Reject names that contain `..` or start with `/`. Files written by the core:
`generator_v2.json`, `generator_v2_stats.json`, and on corruption `generator_v2.corrupt-<ms>.json` /
`generator_v2_stats.corrupt-<ms>.json`. Files read: those two, `generated.json` (v1, read only) and
the optional overrides `generator_v2/balance.json`, `generator_v2/profiles/*.json`,
`generator_v2/profiles/index.json`.

### 2.5 `QuestValidator`

```java
ValidationResult validate(String questId, JsonObject questJson) {
    var result = Quest.CODEC.parse(JsonOps.INSTANCE, questJson);
    Optional<Quest> q = result.result();
    if (q.isEmpty()) return ValidationResult.fail(result.error().map(Object::toString).orElse("parse error"));
    if (q.get().objectives().isEmpty()) return ValidationResult.fail("no objectives");
    if (q.get().objectives().stream().anyMatch(o -> o.requiredCount() <= 0)) return ValidationResult.fail("count <= 0");
    return ValidationResult.OK;
}
```

Use the same codec the mod uses for datapack quests. Do not keep `questJson`. Called once per
generated quest (≤ 25 per rotation).

### 2.6 `HostCapabilities`

| Method | Today's answer |
|---|---|
| `supportsTag(type)` | `true` for every item, block and entity objective (`collect_item`, `craft_item`, `smelt_item`, `consume_item`, `enchant_item`, `use_item`, `mine_block`, `place_block`, `kill_mob`, `tame_animal`, `breed_animal`; mod 0.3.3+); `false` for `visit_dimension`. |
| `objectiveTypes()` | the 15 ids of `QuestObjective.codecForType` (the core ignores the forbidden three). |
| `rewardTypes()` | the 6 reward ids (`command` is listed by the mod but never emitted by the core). |

Block and entity tags (`#minecraft:logs`, `#minecraft:skeletons`) need mod 0.3.3+; an older build
reports only the item types and the core falls back to the plain ids.

### 2.7 `GenLog`

Forward to `JustQuests.LOG` (`info`, `warn`, `error(msg, t)`). The core prefixes its messages with
`[GenV2]`. Expect a handful of lines per rotation.

## 3. Call order and trigger points

One `QuestGeneratorV2` per server (singleplayer world switches create a new server → new instance).
Keep it in a holder, e.g. `generator/GenV2.java`:

```java
public final class GenV2 {
    private static QuestGeneratorV2 gen;           // the only static: cleared on stop
    public static QuestGeneratorV2 get() { return gen; }
    ...
}
```

### 3.1 Server start — replaces `GeneratedQuestStore.init(server)`

In `ServerStorageEvents.onServerStarting` (Fabric: `ServerLifecycleEvents.SERVER_STARTED`, **not**
`SERVER_STARTING`, which fires before the worlds are loaded), **after** `WorldQuestStore.load`,
`WorldSettings.load` and `CustomQuestLoader.init` (tags and recipes are bound at this point). The
reference adapters show the wiring for every loader (`reference-adapter/README.md`):

```java
gen = new QuestGeneratorV2(new GenV2Host(server), GenV2Config.fromSettings());
Map<String, Set<UUID>> active = new HashMap<>();
Map<String, Set<UUID>> waiting = new HashMap<>();
WorldQuestStore.get().allPlayers().forEach((uuid, data) -> {
    data.active.keySet().forEach(id -> {
        if (gen.isGenerated(id.toString())) active.computeIfAbsent(id.toString(), k -> new HashSet<>()).add(uuid);
    });
    data.pendingClaim.keySet().forEach(id -> {   // finished, rewards not claimed yet (see 3.5)
        if (gen.isGenerated(id.toString())) waiting.computeIfAbsent(id.toString(), k -> new HashSet<>()).add(uuid);
    });
});
StartResult r = gen.startWithHolders(active, waiting);   // or start(Map<String, UUID>) if you prefer the spec signature
for (String dead : r.deadQuestIds()) {            // definitions that no longer exist
    ResourceLocation rl = ResourceLocation.parse(dead);
    WorldQuestStore.get().allPlayers().values().forEach(d -> d.abandon(rl));
}
WorldQuestStore.get().markDirty();
registerServed();                                  // always after start, even if nothing rotated
```

`deadQuestIds` lists every quest a player has active that the generator cannot serve any more:
unknown ids, and stored quests that failed re-validation at start (a content mod was removed, or the
state file was edited). The generator drops those and tops the current set up by itself, so
`registerServed` after `start` shows a full set.

`registerServed()`:

```java
static void registerServed(MinecraftServer server) {
    Map<ResourceLocation, Quest> map = new LinkedHashMap<>();
    gen.servedQuests().forEach((id, json) -> Quest.CODEC.parse(JsonOps.INSTANCE, json).result()
        .ifPresent(q -> map.put(ResourceLocation.parse(id), q)));
    QuestManager.INSTANCE.setGeneratedQuests(map);
    QuestNetwork.syncAll(server);
}
```

`startWithHolders` is an addition to the spec'd `start(Map<String, UUID>)`: a plain map can carry only
one holder per quest, but legacy data (or exclusive claims switched off) can have two. Both methods
exist; prefer `startWithHolders`. The two-map form also passes whose rewards wait to be claimed, so a
finished quest is kept only while that is still true (without it, the stored list is kept as is).

### 3.2 Periodic tick — replaces `GeneratedQuestStore.tickCheck()`

Keep the existing 6000-tick counter in `ServerStorageEvents.onServerTick`:

```java
RotationResult r = gen.tick();
applyExpired(server, r.expiredClaims());
if (r.changed()) registerServed(server);
```

`tick()` is O(1) when nothing is due. It rotates at most once, even after days of downtime
(`reason = "catch-up"`), and releases expired claims when claim expiry is configured.

`applyExpired`: for every `ExpiredClaim(questId, holder)` remove the quest from that player's active
quests (`data.abandon(id)`), `markDirty()`, sync the player if online and tell them:
`"§eYour generated quest expired and was released: " + title`.

### 3.3 `/quest accept` — `QuestCommand.accept`

Insert the claim **after** all existing eligibility checks (already active, prerequisites,
completed/cooldown) and **before** `data.accept(id)`:

```java
ClaimResult claim = GenV2.get() == null ? ClaimResult.NOT_GENERATED : GenV2.get().tryClaim(id.toString(), player.getUUID());
if (!claim.proceed()) {
    ctx.getSource().sendFailure(Component.literal("§c" + claim.denyMessage()));
    return 0;
}
data.accept(id);
```

Exact deny messages (`ClaimResult.denyMessage()`):

| Result | Player message |
|---|---|
| `OK`, `ALREADY_YOURS`, `NOT_GENERATED` | — (proceed) |
| `CLAIMED_BY_OTHER` | Another player already took this quest. |
| `HAS_ACTIVE_GENERATED` | Finish your current generated quest first. |
| `COMPLETED` | This quest was already completed. |
| `NOT_SERVED` | This generated quest is no longer available. |
| `DISABLED` | Generated quests are disabled in this world. |

The GUI sends the same command, so nothing else changes. Optional nicer text for
`CLAIMED_BY_OTHER`: `"Taken by " + name` using `gen.claim(id).holder()`.

### 3.4 `/quest abandon` — `QuestCommand.abandon`

After `data.abandon(id)`:

```java
long before = gen.servedRevision();
gen.onAbandon(id.toString(), player.getUUID());
if (gen.servedRevision() != before) registerServed(server);   // a retained quest disappeared
```

### 3.5 Completion — `QuestProgressService`

In the completion loop, right after `data.complete(questId)`, telling whether the rewards wait for
the player's Claim button (`pendingClaim`) or were granted at once:

```java
if (GenV2.get() != null && GenV2.get().isGenerated(questId.toString())) {
    GenV2.get().onComplete(questId.toString(), player.getUUID(), rewardsWait);
}
```

When the player claims them (and when an admin reset drops them), right after the rewards are paid:

```java
long before = gen.servedRevision();
gen.onRewardsClaimed(questId.toString(), player.getUUID());
if (gen.servedRevision() != before) registerServed(server);   // a finished quest from an older cycle left
```

Do the same in `QuestCommand.adminComplete`. Completed generated quests stay registered until the
next rotation, and quests whose rewards still wait stay past it (retained, shown as completed) until
they are claimed - a rotation must not take the rewards away. The player's `completed` map keeps its
entry; it is harmless if the definition later disappears.

### 3.6 `/quest reroll` — `QuestCommand.reroll`

```java
RotationResult r = gen.reroll();
if (!gen.config().enabled()) { /* existing "disabled" message */ }
registerServed(server);
"§aRerolled generated quests. §7Now " + gen.servedQuests().size() + " in category §fgenerated§7."
```

Claimed and completed quests of the current cycle are kept; the rest is replaced. The current
difficulty applies.

### 3.7 Admin reset — `QuestCommand.adminResetAll` / `adminResetOne`

- `adminResetAll(player)`: before clearing `data.active`, call `gen.releaseAllFor(uuid)` (it also
  drops the rewards the player waited for), then `registerServed(server)` if `servedRevision()` changed.
- `adminResetOne(player, id)`: if `gen.isGenerated(id)` and the player holds it, call
  `gen.onAbandon(id, uuid)` (same as a player abandon); if its rewards waited, call
  `gen.onRewardsClaimed(id, uuid)`.

### 3.8 `/quest reload` and settings changes

After `WorldSettings.load(server)` (and after any command that changes a generator setting):

```java
boolean wasEnabled = gen.config().enabled();
gen.updateConfig(GenV2Config.fromSettings());
if (wasEnabled != gen.config().enabled()) registerServed(server);   // enable/disable is immediate
```

`updateConfig` also re-reads the world override files (`generator_v2/balance.json`, extra profiles).
Difficulty, count, profiles and balance take effect at the next rotation or reroll.

### 3.9 Server stop — replaces `GeneratedQuestStore.clear()`

```java
if (gen != null) { gen.stop(); gen = null; }
```

### 3.10 Summary

```
server start ─► WorldQuestStore/Settings/Custom loaded ─► new QuestGeneratorV2(host, cfg)
             ─► startWithHolders(active, waiting) ─► remove deadQuestIds ─► registerServed + syncAll
every 6000 t ─► tick() ─► apply expiredClaims ─► if changed: registerServed + syncAll
/quest accept ─► mod checks ─► tryClaim ─► proceed? data.accept : deny message
/quest abandon ─► data.abandon ─► onAbandon ─► re-register if servedRevision changed
completion ─► data.complete (+ rewards or pendingClaim) ─► onComplete(id, player, rewardsWait)
/quest claim ─► rewards ─► onRewardsClaimed ─► re-register if servedRevision changed
/quest reroll ─► reroll() ─► registerServed + syncAll
/quest reload, settings ─► updateConfig(cfg) ─► re-register if enabled flag flipped
admin reset ─► releaseAllFor / onAbandon
server stop ─► stop()
```

## 4. Settings keys

Build `GeneratorConfig` from `WorldSettings` (`GenV2Config.fromSettings()`):

```java
GeneratorConfig.builder()
    .enabled(WorldSettings.generatedQuests())
    .questsPerCycle(WorldSettings.generatedCount())
    .difficulty(Difficulty.parse(WorldSettings.difficulty()).orElse(Difficulty.NORMAL))
    .exclusiveClaims(...).oneActivePerPlayer(...).releaseOnAbandon(...).claimExpiryHours(...)
    .cycleHours(...).cycleAnchorHour(...).moddedShare(...).disabledProfiles(...)
    .adaptiveBalancing(...).statsEnabled(...)
    .build();
```

The core clamps every value and logs the adjustment (`[GenV2] config adjusted: ...`).

| settings.json key | Type | Default | Maps to |
|---|---|---|---|
| `generatedQuests` *(exists)* | bool | `true` | `enabled` |
| `generatedCount` *(exists)* | int | `5` | `questsPerCycle` (1..20) |
| `difficulty` | `"easy"`/`"normal"`/`"hard"` | `"normal"` | `difficulty` |
| `generatorExclusiveClaims` | bool | `true` | `exclusiveClaims` |
| `generatorOneActivePerPlayer` | bool | `true` | `oneActivePerPlayer` |
| `generatorReleaseOnAbandon` | bool | `true` | `releaseOnAbandon` |
| `generatorClaimExpiryHours` | int | `0` | `claimExpiryHours` (0 = never) |
| `generatorCycleHours` | int | `12` | `cycleHours` (1..168) |
| `generatorCycleAnchorHour` | int | `0` | `cycleAnchorHour` (0..23) |
| `generatorModdedShare` | number | `0.35` | `moddedShare` (0..1) |
| `generatorDisabledProfiles` | list of strings | `[]` | `disabledProfiles` (e.g. `["create"]`) |
| `generatorAdaptiveBalancing` | bool | `false` | `adaptiveBalancing` |
| `generatorStats` | bool | `true` | `statsEnabled` |

`maxPerCycle` (20), `zone` (system default) and `historyDays` (6) have no settings key.

Updated `_help` text (replace the last two sentences of `WorldSettings.HELP`):

```
generatedQuests: auto-generate a rotating set of quests (category 'generated'); set false to disable.
generatedCount: how many generated quests per cycle (1-20).
difficulty: easy, normal or hard - scales effort, content tiers, objectives and rewards of generated quests (OP: /quest difficulty).
generatorExclusiveClaims: a generated quest accepted by one player is locked for everyone else.
generatorOneActivePerPlayer: each player may hold at most one unfinished generated quest.
generatorReleaseOnAbandon: an abandoned generated quest becomes available again (false: it disappears).
generatorClaimExpiryHours: release accepted generated quests after this many hours (0 = never).
generatorCycleHours / generatorCycleAnchorHour: rotation period and the local hour of the first rotation of the day.
generatorModdedShare: share (0-1) of generated quests that use content from supported mods.
generatorDisabledProfiles: mod profiles to ignore, e.g. ["create"].
generatorAdaptiveBalancing: let the generator tune its time estimates from how long quests really take.
generatorStats: record anonymous statistics in justquests/generator_v2_stats.json.
```

## 5. Suggested OP commands

Permission level 2, under `/quest`:

| Command | Implementation |
|---|---|
| `/quest difficulty` | show `gen.config().difficulty().settingsValue()` |
| `/quest difficulty easy\|normal\|hard` | write `WorldSettings`, save, `gen.updateConfig(...)`; reply "Difficulty set to hard — applies from the next rotation or /quest reroll." |
| `/quest generator status` | `gen.status()` |
| `/quest generator explain <id>` | `gen.explain(id)`, one chat line per `\n` |
| `/quest generator preview [n]` | `gen.preview(n)` → list `title` + objectives (does not change anything) |
| `/quest generator stats` | `gen.stats().toText()` |
| `/quest generator release <id>` | `gen.forceRelease(id)` → remove the quest from the former holder's active quests (`data.abandon`), notify them, `registerServed` |

Keep `/quest reroll` as it is (now backed by `gen.reroll()`).

## 6. Sync / GUI suggestion

Claims are visible through `gen.claim(id)` / `gen.claims()` (state, first holder, times) and
`gen.holders(id)`. Suggested additions (optional, no core change needed):

- **Sync:** add `claimState` (byte: 0 available, 1 claimed, 2 completed) and `holderName`
  (string, resolved server side from `holder` via the profile cache; may be empty) to each generated
  quest entry of the full list sync. Resend the list when a claim changes (after `tryClaim OK`,
  `onAbandon`, `onComplete`, expiry, `forceRelease`).
- **Quest book:** in the list row, show a small suffix: `Yours` (green) if the holder is the viewer,
  `Taken` (gray) otherwise, `Done` for completed. In the detail pane: "Taken by Steve", and disable
  the Accept button with the tooltip "Another player already took this quest.".
- `/quest list`: append `§8[taken by Steve]` / `§a[yours]`.

## 7. Self-test hooks

Replace the v1 generator check in `SelfTest.run` with:

1. **Instance started:** `GenV2.get() != null && GenV2.get().servedQuests() != null`.
2. **Served set valid:** `gen.selfTest()` returns an empty list (runs the host validator on every
   served quest and checks the claim rules).
3. **Registered == served:** every id in `gen.servedQuests()` is present in `QuestManager` and vice
   versa for the `generated` category.
4. **Claims consistent:** for every claimed quest, the holder has it in `data.active`
   (`gen.claims()` × `WorldQuestStore`).
5. Report `gen.status()` as info.

## 8. Optional mod enhancements

Not required for v2; each unlocks more generator content with a data-only change:

- **Tag support for `mine_block` and `kill_mob`** (`#minecraft:logs`, `#minecraft:skeletons`):
  report it in `HostCapabilities.supportsTag`; the catalog can then use tags there too.
- **Split large `give_item` counts** above the stack size into several stacks — the core would then
  allow counts above `maxStackSize` for rewards (currently capped).
- ~~**Stonecutter hook**~~ — done in mod 0.3.3: `craft_item` counts stonecutter results, so Create's cut
  stone and many decoration blocks craft targets.
- **`claimState` in the sync packet** (§6).
- **Per-objective progress for `mode: any`** (not used by the generator today).

## 9. Migration notes (v1 → v2)

- On the first `start()` without `generator_v2.json`, the core reads `generated.json` (v1):
  - v1 history signatures (`justquests:collect_item|minecraft:oak_log`) are converted to v2
    (`collect_item:minecraft:oak_log`) and honoured for the rest of their 6-day window;
  - v1 quest definitions are kept **only** if a player has them active (passed to `start`) — they
    become *retained, claimed* quests and keep their ids `justquests:gen/<ms>_<n>`;
  - all other v1 quests disappear; a fresh v2 cycle is generated.
- `generated.json` is **never modified or deleted** by the core. Delete it (and the v1 classes
  `QuestGenerator`, `GeneratedQuestStore`, the v1 check in `SelfTest`) in a later release, once all
  worlds have started v2 at least once — e.g. in 0.4.0.
- v1 ids use epoch **milliseconds** (13 digits), v2 ids epoch **seconds** (10 digits) — they can
  never collide.

## 9a. Server extras (used by the server plugin, off in the mod)

- `setRewardOptions(new RewardOptions(choice, scale))`: `choice` turns the first item reward into a
  `justquests:choice` of up to three options of about its value (needs the host to list
  `justquests:choice` in its reward types); `scale` multiplies every reward budget and the reward
  caps. `RewardOptions.STANDARD` (the default) changes nothing.
- `generateSet(SetRequest)`: a set outside the board for hosts with boards of their own (personal,
  weekly, server-goal quests). Nothing is stored, claimed or counted. A request can scale the play
  time (`minutesScale`; counts and caps grow along) and the rewards (`budgetScale`), ask for one
  objective, leave objective types out, pass the history to avoid, and pass its own `WorldContext`
  with `dayUnlocks = false` so only that context's advancements unlock the Nether and the End.
- `rewardValue(questId)`: value of a board quest's rewards (for money rewards).

## 10. Checklist per build

- [ ] core + resources copied (identical to this project)
- [ ] `GenV2Host` implemented (registries, tags, recipes-or-UNKNOWN, advancements, store, validator);
      start from the matching `reference-adapter/` folder
- [ ] existence checks include `isEnabled(enabledFeatures())` on 1.19.3+; `minecraftVersion()` returns
      the real version (`/quest generator status` ends with `Minecraft <version> (<loader>)`)
- [ ] start / tick / accept / abandon / complete / reroll / reload / admin reset / stop wired (§3);
      on Fabric the start runs on `SERVER_STARTED`
- [ ] `deadQuestIds` and `expiredClaims` handled
- [ ] settings keys + `_help` updated
- [ ] `/quest test` updated
- [ ] manual smoke test: accept a generated quest, `/quest generator explain`, reroll, restart the
      server, quest still active and counting
