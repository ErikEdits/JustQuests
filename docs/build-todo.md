# JustQuests — Step-by-Step Build TODO

A granular, checkable build plan. Every feature is broken into ordered
sub-steps. Derived from [implementation-order.md](implementation-order.md)
(phase order) + the answered design questions in
[open-questions.md](open-questions.md).

Rules of thumb:
- **Do phases in order.** Phase 1 (storage) unblocks almost everything.
- Check items off as you go; ship small public betas between phases (Q25).
- Near phases are detailed; the AI generator and plugin get their own deep
  breakdown when you actually reach them.

---

## Version ladder (release milestones)

The GUI is the big v0.2.0 jump, and it is designed from **community polls**.
We need more Discord members before those polls are meaningful, so the
0.1.x line keeps shipping useful, command-only releases that also grow the
community until we are ready for the GUI.

| Version | Theme | Status |
|---------|-------|--------|
| 0.1.0 | First release (datapack quests, commands) | ✅ shipped |
| 0.1.1 | Robustness + diagnostics | ✅ shipped |
| 0.1.2 | Content depth (objective/reward types, tags, modes) | ✅ shipped |
| 0.1.3 | Per-world custom quests | ✅ shipped |
| 0.1.4 | Localization (multi-language quest text) | ✅ shipped |
| 0.1.5 | Community / Discord pointer (grow members for the polls) | ✅ shipped |
| 0.1.6 | More objective types (mine, breed, consume, smelt) | ✅ shipped |
| 0.1.7 | Rewards + quest logic (xp/effect/message, prerequisites, repeatable) | ✅ shipped |
| 0.1.8 | Categories & organization (filters, sort, pack-defined) | ✅ shipped |
| 0.1.9 | Server & admin (admin cmds, completion broadcast, settings) | ✅ shipped |
| 0.1.10 | Stats & feedback (sound/toast, leaderboard) | ✅ shipped |
| 0.1.11 | Bug-fix & maintenance (cancelled-event guards, stats cap) | ✅ shipped |
| 0.1.12 | Content & language pack (25 quests, EN/DE/FR/ES, example datapack) | ✅ shipped |
| **0.2.0** | **In-game GUI** (interim, SP) + MC 1.21.6–1.21.10 (11 versions) | ✅ shipped |
| 0.2.1–0.2.2 | Textured GUI everywhere, Fabric + Forge ports (34 builds) | ✅ shipped |
| 0.2.3 | Multiplayer sync (quest book works on servers) | ✅ shipped |
| 0.2.4 | Toggle to hide the built-in main quests (`/quest mainquests`) | ✅ shipped |
| 0.2.5 | Stability (Fabric pickup mixin, lighter sync, cache cleared on disconnect) | ✅ shipped |
| **0.3.0** | **Generator v2** (modded content, claims, difficulty, progression) | ✅ shipped |
| 0.3.1 | Claims shown in the quest book; GUI text fix for 1.21.6+ | ✅ shipped |
| 0.3.2 | `enchant_item`, `use_item`, item filters, `title` reward, v1 generator removed, CurseForge | ✅ shipped |
| 0.3.3 | Block/mob tags, stonecutter counts as crafting, generator uses enchant/use/potions/tags | ✅ shipped |
| 0.3.4 | HUD tracker, quest book grouped by category/status, pixel icons, stats page | ✅ shipped |
| 0.3.5 | MC 1.21.11 (NeoForge + Fabric, 36 builds), book search, pin quests to the HUD, mouse wheel | ✅ done |
| 0.4.0 | Translations (EN, DE, JA, FR, ES), claim button, choice rewards, permissions, more generator profiles, MC 26.x, Spigot/Paper plugin | 🚧 in progress |

Each 0.1.x release is a Modrinth update, which puts the mod back in
"recently updated" and funnels new players to the Discord — so a steady
drip of small, useful releases *is* the growth plan, not a detour from it.

---

## Phase 0 — v0.1 (DONE ✅)

- [x] Datapack quest loading (`collect_item` + `give_item`)
- [x] Per-player NBT progress, commands, tab completion, 10 bundled quests
- [x] Icon, LGPL-3.0, public GitHub release

---

## Phase 1 — Storage foundation ⚠️ (do first) — DONE

**1.1 Data model** ✅
- [x] `PlayerQuestData` (active map, pendingClaim [reserved, Q48],
      completed map with timestamps, teamId [reserved, Q14])
- [x] Optional **team/group id** field reserved (Q14)
- [x] **Timestamped** completed map for the 6-day window/repeatable (Q26)
- [x] Codecs (`QuestProgress.CODEC`, `PlayerQuestData.CODEC`) serialize
      to JSON via JsonOps; NBT-capable via the same codecs (Q15)

**1.2 World-folder file** ✅
- [x] `WorldQuestStore` reads/writes `<world>/justquests/progress.json`
      (Q31)
- [x] **Loose sync** — dirty flag, periodic save every 30s + on stop
      (`ServerStorageEvents`) (Q47)
- [x] One-time **migration** from the v0.1 per-player NBT attachment on
      login; skips players already in the store, never overwrites

**1.3 Verify** — ✅ DONE (live, 2026-06-18)
- [x] Build compiles; dev server boots clean; store loads with no errors
- [x] Live play test in the instance PASSED: accept/complete worked
      (Green Thumb, Ender Seeker), progress.json written with correct
      structure, migration confirmed (hot_stuff completed=0 from v0.1),
      persistence confirmed ("Loaded quest progress for 1 player(s)" on
      restart), `/reload` works.
- [x] Added `/quest test` diagnostics command (OP) writing a full
      self-test + Minecraft environment report to
      `justquests-diagnostics.log` in the instance folder.

**PHASE 1 COMPLETE.**

---

## Phase 2 — Content depth (v0.2 core, still command-only)

**2.1 Objective types (priority order, Q13/Q41)**
- [x] Refactored objective tracking into `QuestProgressService` so any
      event (not just item pickup) can advance objectives.
- [x] `kill_mob` (LivingDeathEvent) + bundled `slayer` example quest
- [x] `place_block` (BlockEvent.EntityPlaceEvent)
- [x] `craft_item` (PlayerEvent.ItemCraftedEvent — lifts pickup-only limit)
- [x] `reach_location`, `reach_level` (polled via PlayerTickEvent, portable)
- [x] `tame_animal` (AnimalTameEvent)
- [x] `gain_advancement` (AdvancementEvent.AdvancementEarnEvent)
- [x] `visit_dimension` (PlayerChangedDimensionEvent; matches modded dims by id)
- [x] `enchant_item` (0.3.2) — counts the vanilla "Items Enchanted" statistic
      (enchanting table only), no mixin; optional `item` filter
- Cross-loader strategy documented in
  [cross-loader-events.md](cross-loader-events.md)
- [x] Tag support in item fields — `item` accepts a single id, a list,
      or a `#tag` (collect_item + craft_item), via ITEM_OR_TAG codec (Q38)
- [x] Item filters (Q39, 0.3.2) — `item` may be `{"id", "enchantments", "potion", "name"}`;
      one JSON for every version (NBT before 1.20.5, components after)
- [x] `mode: all | any` flag for multi-objective (Q40)

**2.2 Reward types**
- [x] `loot_table` reward (random items from a loot table, Q29)
- [x] `command` reward ({player} substitution, runs as @s level 4 — Q52)
- [x] `xp` reward (0.1.7)
- [x] Choice rewards (Q49, 0.4.0: `justquests:choice`)

**2.3 Quest categories (Q3)**
- [x] `category` field in the data model (default "datapack"), shown in
      `/quest list`
- [x] Pack-definable free-form category (Q77, decided in 0.1.8)

---

## Phase 3 — GUI (v0.2 headline)

**Discord-vote gating DROPPED (2026-07-03)** — only ~5 members, not enough to
decide. So we adopt the **v2-full texture set directly** as the GUI (no vote).

**Textured GUI in baseline 1.21.1 (2026-07-03):** `client/QuestScreen` rewritten
to render from `assets/justquests/textures/gui/` (v2-full): fixed 248x184 window,
`quest_row` state textures, textured close/page/accept(claim)/abandon buttons,
progress bars. No vanilla widgets — manual `blit` + `mouseClicked` hit-testing.
Textures copied from `docs/assets/gui-2.0.0/JustQuests-GUI-v2-full`.
Since rolled out to every GUI version (1.20.1+) and checked in-game. NOTE: `blit`
signature differs 1.21.2+ / 1.21.4+ — the roll-out needs a per-version blit helper.
Client classes: `client/QuestClient` (keybind; `KeyMapping` category is a String
pre-1.21.9, a `Category` object in 1.21.9+; `@EventBusSubscriber bus=` gone in 1.21.6+).

- [x] ~~Wait for Discord poll~~ — dropped, v2-full textures adopted directly
- [x] Textured screen rolled out to every GUI build (1.20.1+, 0.2.1)
- [x] **Multiplayer sync** (server -> client payload, 0.2.3; lighter in 0.2.5)
- [x] Generated-quest claims in the book ("Taken by X", 0.3.1)
- [x] Quest list screen: grouping by category or status, hide completed (Q45, 0.3.4)
- [x] Per-quest icon with fallback (Q44, 0.3.4: `icon` field, else from the first objective); detail view (Q78 open)
- [x] Search box that auto-appears at high quest counts (Q46, 0.3.5: from 15 quests; title, category, goals)
- [x] **Claim button** + completed-pending state (Q48, 0.4.0: default on, `claimRewards` in settings.json, `/quest claim [id]`)
- [x] **Choice reward** picker (Q49, 0.4.0: `justquests:choice`, picked in the book or with `/quest claim <id> <n>`)
- [x] Category + state icons (Q3, 0.3.4)
- [x] Optional HUD tracker overlay, toggleable (Q43, 0.3.4: key H, `config/justquests-client.json`)

---

## Phase 4 — Custom quests — DONE (2026-06-19)

- [x] Per-world custom quest file `<world>/justquests/custom-quests.json`
      (Q31), loaded by CustomQuestLoader on server start
- [x] Fill-in-the-blanks template auto-written on first run (help +
      example + blank slots; blank/no-objective slots skipped silently)
- [x] **Automatic live reload** — file polled every ~3s, reloads on change
      (Q32); `/quest reload` also reloads it manually
- [x] Source precedence: custom > datapack (Q54) via QuestManager merge
- Verified: template created, example loaded ("Loaded 1 custom quest(s)"),
  blank slots skipped, 12 datapack + 1 custom.
- (generated source comes with the Phase 6 AI generator)

---

## Phase 5 — Server & QoL

- [x] Admin commands: reset / view other (Q33, 0.1.9); claimed generated
      quests via `/quest generator status|release` (0.3.0)
- [x] Statistics + server leaderboard (Q34, 0.1.10); in-game view (Q58, stats page in the book, 0.3.4)
- [x] Difficulty Easy/Normal/Hard, OP-set per world (Q8/Q9, 0.3.0)
- [x] Permission gating via OP + LuckPerms/perm plugins (Q83); per-quest
      permission (Q55) — 0.4.0: `justquests.command.*`, `justquests.admin.*`, quest `permission`
- [x] Self-managed JSON config — per-world `settings.json` (Q35)
- [~] Update notice (Q36) — tried in 0.1.10, removed in 0.1.11 (see 9f)
- [x] Locked-quest teaser, command-enabled (Q28, 0.1.7)
- [x] Announce-flagged completion broadcast, default on (Q53, 0.1.9)

---

## Phase 6 — quest generator (v1 0.2.x → **v2 in 0.3.0** — DONE)

**v1 built in the NeoForge 1.21.1 baseline (2026-07-03).** Procedural, no AI/LLM,
zero dependencies. `generator/QuestGenerator` emits the same quest JSON the
datapack loader understands (reuses Codec validation); `generator/GeneratedQuestStore`
holds the per-world set + rotates it. Wired via `ServerStorageEvents`, exposed as
category `generated`, toggled by `generatedQuests`/`generatedCount` in
`settings.json`, rerollable with `/quest reroll` (OP).

- [x] Procedural generator (curated pools + weighted random + baked count/reward rules)
- [x] 12h rotation per world; 6-day no-repeat via timestamped history (Q26)
- [x] Amount: singleplayer cap via `generatedCount` (server-configurable = same setting)
- [x] Template-based English descriptions (Q6); optional AI-model layer later
- [x] Per-world toggle; own category (Q1/Q3)
- [x] **Propagated to all 34 version folders** (NeoForge/Fabric/Forge; pre-1.21 uses
      the `new ResourceLocation(...)` shim; per-loader wiring + per-version /quest reroll feedback)
- [x] **Generator v2 (0.3.0)** — version-neutral core in `generator-v2/`, copied into all
      34 builds; spec: [generator-v2-spec.md](generator-v2-spec.md)
- [x] Read loaded registries/tags for modded-aware content (Q41) — five mod profiles
      (Farmer's Delight, Create, Mekanism, Twilight Forest, Botania)
- [x] Shared set; one active quest/player; exclusive claiming (Q7/Q47), shown in the book (0.3.1)
- [ ] Test phase to tune balancing/limits/expiry + difficulty (Q5/Q9) — anonymous stats
      are collected; adaptive balancing is off by default
- [x] Runtime-tested in-game on every loader (2026-10)
- [x] v1 classes (`QuestGenerator`, `GeneratedQuestStore`) removed in 0.3.2; worlds keep
      their old `generated.json`, which v2 migrates on first start
- [x] Tags for `mine_block`/`kill_mob` targets, stonecutter craft targets, enchant/use/potion
      quests (0.3.3)
- [ ] Next: thicker Botania / Mekanism / Twilight Forest profiles, more mod profiles

---

## Phase 7 — Localization — DONE (2026-06-19)

- [x] Multi-language `title`/`description` — accept a plain string OR a
      per-language map `{"en_us": "...", "de_de": "..."}` (new
      `LocalizedText` type), resolved from the player's client language
      with an English fallback (Q21). Backward compatible: plain strings
      still parse.
- [x] Per-user language via vanilla translation keys — objective/reward
      content (item/mob/block names) is sent as translatable components,
      so it localizes to each client for free (objectives free).
- [x] Mod's own connective words stay English (Q22) — only data-driven
      text and vanilla content names translate.
- [x] All 12 bundled quests shipped with English + German text.
- [x] Custom-quest template documents the map form + a localized example.
- [x] `/quest test` gains a LocalizedText check (map resolve + fallback).

---

## Phase 8 — Reach

**Multi-version build (IN PROGRESS, 2026-06-20).** The repo is now a Gradle
multi-project: each MC version is its own subproject with its own source
under `neoforge/<mc-version>/`, all built by one `./gradlew build`.
`./gradlew exportJars` also copies the finished jars to
`Desktop/Justquests/neoforge/`. Adding a version = new folder + one line in
`settings.gradle`. Future loaders get sibling trees (`fabric/<ver>`, …).

- [x] NeoForge **1.21** (21.0) — code compiles unchanged from 1.21.1
- [x] NeoForge **1.21.1** (21.1) — the original target
- [x] NeoForge **1.21.2** (21.2.1-beta) — needed per-version fixes:
      `BuiltInRegistries.ITEM.getValue(...)` (registry `get` rename) and the
      codec-based `SimpleJsonResourceReloadListener` in `QuestManager`
- [x] NeoForge **1.21.3** (21.3.96) — same source as 1.21.2
- [x] NeoForge **1.21.4** (21.4.157) — extra fixes: reload event renamed to
      `AddServerReloadListenersEvent` (keyed `addListener`), and the reload
      listener constructor now takes `FileToIdConverter`
- [x] NeoForge **1.21.5** (21.5.97) — extra fixes: NBT getters return
      `Optional`/`*Or` + `getAllKeys()`→`keySet()` (legacy migration code),
      and `ClickEvent`/`HoverEvent` are sealed (use `ClickEvent.OpenUrl` /
      `HoverEvent.ShowText`)
- [x] NeoForge **1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10** (toward 0.2.0).
      Needed: **MDG bumped 2.0.78 → 2.0.141** (older NFRT couldn't build
      1.21.9/1.21.10). 1.21.6 dropped `INBTSerializable` + `serverLevel()`,
      so the dead v0.1 NBT migration was removed from 1.21.6+ and loot uses
      `(ServerLevel) player.level()`. 1.21.10 removed `getGameProfile()`,
      `Player.getServer()`, `getProfileCache()` → use `getName().getString()`,
      `player.level().getServer()`, drop the profile-cache lookup (all
      cross-version safe).
- [x] **1.21.11** (0.3.5): the NeoForm package was fixed upstream; NeoForge 21.11.45 builds with MDG
      2.0.141, Fabric needs Loom 1.14.10 — so every Fabric build moved to 1.14.10 (two Loom versions in
      one build break remapJar). Loom 1.14 remaps mixin targets inside the classes instead of a refmap. Mojang renamed
      `ResourceLocation` to `Identifier`, `ResourceKey.location()` to `identifier()`, commands use
      `PermissionSet`s and `playNotifySound` is gone. (Excluded until then: the upstream NeoForm
      package was broken, a duplicate `mcp/client/Start.class` gave an empty merged jar.)
- 12 → **11 NeoForge versions** building (1.21–1.21.10).
- [x] Loader ports: Fabric 1.18.2–1.21.11 (18), Forge 1.18.2–1.20.1 (4) — 36 builds
      with NeoForge 1.20.4–1.21.11 (14)
- [ ] Minecraft 26.1+ (year-numbered releases after 1.21.11) — not started
- [ ] **CurseForge** listing (0.3.2: local create sheet + upload script)
- [ ] **Paper/Bukkit plugin** edition (shared JSON file is the bridge;
      poll cog already specced) — own deep breakdown when reached

> Maintenance note: each version has its own copy of the source (your
> chosen layout), so a feature/bugfix must be applied to each version
> folder. For shared logic that doesn't differ, copy 1.21.1 → others and
> only patch the spots the API changed (as done for 1.21.2).

---

## Phase 9 — Community & growth (v0.1.5) — DONE (2026-06-19)

Grow the Discord so the v0.2 GUI polls have enough voters. Discord:
https://discord.gg/cMTGE9QCja

- [x] One-time, clickable Discord welcome on a player's first join,
      persisted in `<world>/justquests/seen-players.json` so it never
      repeats. Hook: vote on the GUI + support + sneak peeks.
- [x] `/quest discord` command (clickable invite anytime).
- [x] Per-world `settings.json` with `discordWelcome` toggle (opt-out for
      server owners) — first step of the Phase 5 self-managed JSON config.
- [x] Modrinth description points to the Discord with a reason.
- [x] `/quest test` community-hint check.
- (only a public invite link is bundled — no tokens/webhooks)

---

## The 0.1.x runway toward 0.2.0 (all command-only, no GUI)

Each phase below = one Modrinth release. They keep value shipping (and the
mod visible) while the Discord grows toward enough voters for the GUI
polls. Order is a suggestion; any can be reordered or merged. They pull
forward the non-GUI items from Phases 2, 5, 6 and 7.

### Phase 9b — More objective types (v0.1.6) — DONE (2026-06-19)
- [x] `mine_block` objective (BlockEvent.BreakEvent — distinct from collect)
- [x] `breed_animal` objective (BabyEntitySpawnEvent, matched on parent type)
- [x] `consume_item` objective (LivingEntityUseItemEvent.Finish; id or tag)
- [x] `smelt_item` objective (PlayerEvent.ItemSmeltedEvent; id or tag)
- [x] 4 bundled examples + `/quest test` samples for each
- [x] `enchant_item` objective — done in 0.3.2 via vanilla statistics (no mixin)
- [x] `use_item` — done in 0.3.2: the vanilla "Times Used" statistic, so only
      uses that did something count (not every right-click)
- [x] Item filters per item objective (Q39) — done in 0.3.2

### Phase 9c — More rewards + quest logic (v0.1.7) — DONE (2026-06-20)
- [x] `xp` reward, `effect` (potion) reward, `message` reward
      (message supports the per-language map)
- [x] **Repeatable quests** with optional `cooldown_hours` (reuses the
      timestamped completed-map, Q26)
- [x] **Prerequisites / chains** — `requires` (list of quest ids) enforced
      on accept
- [x] Locked-quest teaser in `/quest list` (Q28)
- [x] Bundled examples: `seasoned_miner` (chain), `daily_bread` (repeatable)
- [x] `title` reward — done in 0.3.2 (title + optional subtitle and timings)

### Phase 9d — Categories & organization (v0.1.8) — DONE (2026-06-20)
- [x] Category stays **pack-definable** (free-form string field, Q77)
- [x] `/quest list <category>` filter (tab-completed) + `/quest categories`
- [x] Stable sort: category -> per-quest `sort` weight -> id, with a
      category header per group in `/quest list`
- [x] All 18 bundled quests categorized (gathering/farming/combat/
      survival/daily) with a progression sort order
- [x] Category icons in the book (0.3.4; known categories have a pixel icon, others a gear)

### Phase 9e — Server & admin QoL (v0.1.9) — DONE (2026-06-20)
- [x] Admin commands (OP): `/quest admin view|reset|complete <player> [id]` (Q33)
- [x] Completion broadcast to the server, default on, toggle in settings (Q53)
- [x] Per-world `settings.json` centralized in `WorldSettings`
      (discordWelcome, announceCompletions) — Phase 5 self-config groundwork
- [x] Permission gating via perms plugins / per-quest perm (Q83/Q55) — 0.4.0
- [x] Difficulty Easy/Normal/Hard (Q8/Q9) — done in 0.3.0 (generator)

### Phase 9f — Stats, notices & feedback (v0.1.10) — DONE (2026-06-20)
- [x] `/quest stats` (personal: %, per-category, first/last) (Q34)
- [x] `/quest leaderboard` (server top 10, offline-name resolution) (Q34)
- [x] Completion **sound** + action-bar toast, toggles in settings (Q12)
- [~] Update notice (Q36) — shipped in 0.1.10, then **removed** (0.1.11):
      it needs a bare mod version to compare, but Modrinth uploads are
      **one version per jar** by design (`neoforge-<mc>-<modver>`, so users
      can pick any exact version), which the checker can't compare. Dropped
      the in-game notice; players update via the Modrinth app as usual.
- [ ] FUTURE (plugin edition): auto-update on server start/restart, default
      on, with an OP on/off notice + opt-out — see Phase 8 plugin

> Modrinth layout (decided 2026-06-21): **one version per jar**, each tagged
> with only its own MC version, so users can freely switch versions. The
> local `upload-modrinth.ps1` does this; old versions stay up.

### Phase 9g — Content & language pack (v0.1.12) — DONE (2026-06-21)
- [x] Longer bundled progression: 25 quests (was 18), new categories
      building/crafting/exploration/challenges; covers every objective type
- [x] More bundled languages: every quest in EN/DE/FR/ES (was EN/DE)
- [x] Example datapack (`docs/example-datapack/`) with one quest per
      objective & reward type + tag/any/chain/repeatable/multilang + README

---

## Cross-cutting (apply throughout)

- [x] Everything server-side-safe; no webhooks/tokens in the mod jar
- [x] CI builds all 36 jars on every push; a `v*` tag publishes the GitHub release
- [x] Modrinth publish per release (local `upload-modrinth.ps1`, one version per jar)
- [ ] CurseForge publish per release (local `upload-curseforge.ps1`, from 0.3.2)
- [ ] Back up to USB after each work session
