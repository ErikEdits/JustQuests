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
| 0.1.6 | More content & QoL, no GUI needed (see Phase 9b) | ⏭️ next |
| 0.1.7 | Polish + admin/permission groundwork | ⏭️ planned |
| **0.2.0** | **In-game GUI** (once the polls have enough voters) | ⏭️ gated on Discord |
| 0.3.0+ | Stats/leaderboard, generator, more loaders/versions, plugin | ⏭️ later |

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

- [ ] MC versions: 1.20.1 first, then onward toward 1.12.x (Q17)
- [ ] Loader ports: Fabric, Forge (~3 weeks each after a feature matures)
- [ ] **Paper/Bukkit plugin** edition (shared JSON file is the bridge;
      poll cog already specced) — own deep breakdown when reached

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

## Phase 9b — More content & QoL toward 0.2.0 (v0.1.6+) — planned

Command-only work that keeps shipping value while the Discord grows. No
GUI required. Pulls forward the non-GUI items from Phases 2 and 5:

- [ ] `enchant_item` objective (poll/mixin; see cross-loader-events.md)
- [ ] Optional `components`/NBT match per objective (Q39)
- [ ] Announce-flagged completion broadcast, default on (Q53)
- [ ] `/quest stats` (personal) + groundwork for the leaderboard (Q34)
- [ ] Admin commands: reset / view other player / list claimed (Q33)
- [ ] Permission gating via OP + perms plugins (Q83); per-quest perm (Q55)
- [ ] Locked-quest teaser, command-enabled (Q28)
- [ ] Decide category: fixed vs. pack-definable (Q77)

---

## Cross-cutting (apply throughout)

- [ ] Everything server-side-safe; no webhooks/tokens in the mod jar
- [ ] CI stays green; docs-only commits skip builds
- [ ] Modrinth + CurseForge publish per release (auto-publish later)
- [ ] Back up to USB after each work session
