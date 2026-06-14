# Implementation Order — where to start

A recommended build order for everything decided in the design docs.
Ordered by **dependencies first** (things other features need), then by
**value vs. effort**. Keep shipping small public betas along the way (Q25).

> Status: v0.1 is shipped — command-only, `collect_item` + `give_item`,
> per-player NBT, 10 bundled quests, NeoForge 1.21.1.

---

## Phase 1 — Storage foundation ⚠️ do this first

Everything below depends on it, so don't build features on the v0.1
NBT-attachment storage.

- Move progress to a **per-world JSON file** in the world folder, via one
  Codec that also serializes to NBT where needed ([[Q15]] / [[Q31]]).
- Bake in from day one: **timestamps** (for the 6-day no-repeat history,
  [[Q26]]) and an optional **team/group id field** ([[Q14]]).
- This single file is also the **mod↔plugin bridge** and the
  singleplayer→server migration path.

**Why first:** retrofitting timestamps + team id after players have save
files is painful. The AI generator, custom files, plugin and migration
all read this format.

---

## Phase 2 — Content depth (v0.2 core, still command-only)

Ships fast, no UI work, immediately useful.

1. New objective types in priority order ([[Q13]]):
   `kill_mob` → `place_block` → `reach_location` → `craft_item`
2. **Loot-table reward** type ([[Q29]]).
3. **Quest categories** in the data model (AI / custom / datapack /
   team) with ids — needed before the GUI and the AI generator ([[Q3]]).

---

## Phase 3 — GUI (the v0.2 headline)

- Wait for the **Discord poll** to pick the style (book vs. single-panel,
  [[Q37]]) before building.
- Use `docs/gui-design-brief.md`; add category icons on top of the
  state icons ([[Q3]]).
- Opening method + book/button + notifications also come from the poll
  ([[Q10]] / [[Q11]] / [[Q12]]).

---

## Phase 4 — Custom quests

- Per-world custom quest file in the world folder ([[Q31]]).
- Fill-in-the-blanks template (~400 lines of empty slots; blanks skipped
  silently).
- **Automatic live reload** — no command needed ([[Q32]]).

---

## Phase 5 — Server & QoL

- Admin commands: reset / view other player / list claimed ([[Q33]]).
- Statistics + server leaderboard ([[Q34]]).
- Difficulty system Easy/Normal/Hard, OP-set, per world ([[Q8]] / [[Q9]]).
- Update notice (poll for details, [[Q36]]).
- Self-managed JSON config for all settings ([[Q35]]).

---

## Phase 6 — AI quest generator (big — budget 2+ months)

Built on the Phase 1 storage. Procedural generator (no AI model needed),
templates + weighted random + baked-in rules.

- 12-hour real-clock rotation, per world.
- 6-day no-repeat window (uses the timestamp history from Phase 1).
- One shared, synced quest set; **exclusive claiming**.
- Amount: server-configurable / singleplayer cap.
- Descriptions are template-based too; optional AI-model layer much later.
- Run a **test phase** to tune balancing/limits before shipping.

---

## Phase 7 — Localization (can overlap once the data model is stable)

- Per-user language display via Minecraft's built-in translation keys
  (objectives are free); multi-language title/description fields with
  English fallback ([[Q20]] / [[Q21]]). Mod's own text ships English-only
  ([[Q22]]).

---

## Phase 8 — Reach: more loaders & versions

- MC versions: **1.20.1 first**, then onward step by step toward
  1.12.x ([[Q17]]).
- Loader ports: Fabric, Forge (~3 weeks after a feature matures).
- **Paper/Bukkit plugin edition** — viable once Phase 1's shared JSON
  file is the bridge ([[idea-paper-plugin]]).

---

## The one rule

Do **Phase 1 before anything else**. After that, the phases are roughly
independent — pick whatever you feel like shipping next, keep releases
small and public, and let test-phase data + Discord feedback steer the
details.
