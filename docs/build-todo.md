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
| 0.1.9 | Server & admin QoL (admin cmds, broadcast, perms, difficulty) | ⏭️ planned |
| 0.1.10 | Stats, update notice & feedback (sound/toast, leaderboard) | ⏭️ planned |
| 0.1.11 | Content & language pack (more quests + more languages) | ⏭️ planned |
| **0.2.0** | **In-game GUI** (once the polls have enough voters) | ⏭️ gated on Discord |
| 0.3.0+ | Generator, more loaders/versions, Paper/Bukkit plugin | ⏭️ later |

Each 0.1.x release is a Modrinth update, which puts the mod back in
"recently updated" and funnels new players to the Discord — so a steady
drip of small, useful releases *is* the growth plan, not a detour from it.

---

## Phase 0 — v0.1 (DONE ✅)

- [x] Datapack quest loading (`collect_item` + `give_item`)
- [x] Per-player NBT progress, commands, tab completion, 10 bundled quests
- [x] Icon, LGPL-3.0, public GitHub release

---

## Phase 1 — Storage foundation ⚠️ (do first) — IN PROGRESS

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
- [ ] `enchant_item` — pending (no clean cross-version event; poll/mixin
      later, see cross-loader-events.md)
- Cross-loader strategy documented in
  [cross-loader-events.md](cross-loader-events.md)
- [x] Tag support in item fields — `item` accepts a single id, a list,
      or a `#tag` (collect_item + craft_item), via ITEM_OR_TAG codec (Q38)
- [ ] Optional `components`/NBT match per objective (Q39)
- [x] `mode: all | any` flag for multi-objective (Q40)

**2.2 Reward types**
- [x] `loot_table` reward (random items from a loot table, Q29)
- [x] `command` reward ({player} substitution, runs as @s level 4 — Q52)
- [ ] (later) `xp`, choice rewards (Q49 — needs GUI)

**2.3 Quest categories (Q3)**
- [x] `category` field in the data model (default "datapack"), shown in
      `/quest list`
- [ ] Decide fixed vs. pack-definable (Q77 — still open)

---

## Phase 3 — GUI (v0.2 headline)

- [ ] Wait for Discord poll: style (Q37), opening method (Q10), book/
      button (Q11), notifications (Q12), HUD tracker (Q43)
- [ ] Build textures from [gui-design-brief.md](gui-design-brief.md)
- [ ] Quest list screen: grouping by category/status/custom order (Q45)
- [ ] Per-quest icon with fallback (Q44); detail view (Q78 open)
- [ ] Search box that auto-appears at high quest counts (Q46)
- [ ] **Claim button** + completed-pending state (Q48)
- [ ] **Choice reward** picker (Q49)
- [ ] Category + state icons (Q3)
- [ ] Optional HUD tracker overlay, toggleable (Q43)

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

- [ ] Admin commands: reset / view other / list claimed (Q33)
- [ ] Statistics + server leaderboard (Q34); in-game view (Q58 open)
- [ ] Difficulty Easy/Normal/Hard, OP-set per world (Q8/Q9)
- [ ] Permission gating via OP + LuckPerms/perm plugins (Q83); per-quest
      permission (Q55 open)
- [ ] Self-managed JSON config (Q35)
- [ ] Update notice; optional plugin auto-update w/ safeguards (Q36)
- [ ] Locked-quest teaser, command-enabled (Q28)
- [ ] Announce-flagged completion broadcast, default on (Q53)

---

## Phase 6 — AI quest generator (big; own deep breakdown when reached)

- [ ] Procedural generator (templates + weighted random + baked rules)
- [ ] Read loaded registries for completable, modded-aware content (Q41)
- [ ] 12h rotation per world; 6-day no-repeat via history (Q26)
- [ ] Shared, loosely-synced set; one active quest/player; exclusive
      claiming (Q7/Q47)
- [ ] Amount: server-configurable / singleplayer cap (Q2)
- [ ] Template-based descriptions; optional AI-model layer later (Q6)
- [ ] Test phase to tune balancing/limits/expiry (Q5/Q9)

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
- [ ] Later MC versions (1.21.3/1.21.4/…) — add folders as desired
- [ ] Loader ports: Fabric, Forge (~3 weeks each after a feature matures)
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
- [ ] `enchant_item` objective — deferred (needs a mixin; no clean event)
- [ ] `use_item` (raw right-click) — deferred (noisy; consume covers food/drink)
- [ ] Optional `components`/NBT match per item objective (Q39) — deferred
      to a later release (needs a richer ItemMatcher)

### Phase 9c — More rewards + quest logic (v0.1.7) — DONE (2026-06-20)
- [x] `xp` reward, `effect` (potion) reward, `message` reward
      (message supports the per-language map)
- [x] **Repeatable quests** with optional `cooldown_hours` (reuses the
      timestamped completed-map, Q26)
- [x] **Prerequisites / chains** — `requires` (list of quest ids) enforced
      on accept
- [x] Locked-quest teaser in `/quest list` (Q28)
- [x] Bundled examples: `seasoned_miner` (chain), `daily_bread` (repeatable)
- [ ] `title` reward (actionbar/title) — deferred; `message` covers chat

### Phase 9d — Categories & organization (v0.1.8) — DONE (2026-06-20)
- [x] Category stays **pack-definable** (free-form string field, Q77)
- [x] `/quest list <category>` filter (tab-completed) + `/quest categories`
- [x] Stable sort: category -> per-quest `sort` weight -> id, with a
      category header per group in `/quest list`
- [x] All 18 bundled quests categorized (gathering/farming/combat/
      survival/daily) with a progression sort order
- [ ] Per-category icon id (reserved for the GUI, v0.2)

### Phase 9e — Server & admin QoL (v0.1.9) — planned
- [ ] Admin commands: reset / view other player / list claimed (Q33)
- [ ] Announce-flagged completion broadcast, default on (Q53)
- [ ] Permission gating via OP + perms plugins (Q83); per-quest perm (Q55)
- [ ] Difficulty Easy/Normal/Hard, OP-set per world (Q8/Q9)

### Phase 9f — Stats, notices & feedback (v0.1.10) — planned
- [ ] `/quest stats` (personal) + server leaderboard (Q34)
- [ ] Completion **sound** + chat/actionbar toast (non-GUI part of Q12)
- [ ] Update notice on login for OPs (Q36)

### Phase 9g — Content & language pack (v0.1.11) — planned
- [ ] A longer bundled progression (more starter quests)
- [ ] More bundled languages for the built-in quests (e.g. fr/es)
- [ ] An example datapack showing every objective/reward type

---

## Cross-cutting (apply throughout)

- [ ] Everything server-side-safe; no webhooks/tokens in the mod jar
- [ ] CI stays green; docs-only commits skip builds
- [ ] Modrinth + CurseForge publish per release (auto-publish later)
- [ ] Back up to USB after each work session
