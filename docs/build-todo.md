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
- [ ] Tag support in item fields (`#minecraft:logs`) (Q38)
- [ ] Optional `components`/NBT match per objective (Q39)
- [ ] `mode: all | any` flag for multi-objective (Q40)

**2.2 Reward types**
- [ ] `loot_table` reward (priority, Q29)
- [ ] `command` reward (enables economy + anything, Q52)
- [ ] (later) `xp`, choice rewards (Q49 — needs GUI)

**2.3 Quest categories (Q3)**
- [ ] Category field in data model (AI / custom / datapack / team)
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

## Phase 4 — Custom quests

- [ ] Per-world custom quest file in world folder (Q31)
- [ ] Fill-in-the-blanks template (~400 lines, blanks skipped silently)
- [ ] **Automatic live reload** via file watcher (Q32)
- [ ] Source precedence: custom > datapack > generated (Q54)

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

## Phase 7 — Localization

- [ ] Per-user language via vanilla translation keys (objectives free)
- [ ] Multi-language title/description fields, English fallback (Q21)
- [ ] Mod's own strings English-only at launch (Q22)

---

## Phase 8 — Reach

- [ ] MC versions: 1.20.1 first, then onward toward 1.12.x (Q17)
- [ ] Loader ports: Fabric, Forge (~3 weeks each after a feature matures)
- [ ] **Paper/Bukkit plugin** edition (shared JSON file is the bridge;
      poll cog already specced) — own deep breakdown when reached

---

## Cross-cutting (apply throughout)

- [ ] Everything server-side-safe; no webhooks/tokens in the mod jar
- [ ] CI stays green; docs-only commits skip builds
- [ ] Modrinth + CurseForge publish per release (auto-publish later)
- [ ] Back up to USB after each work session
