# Idea: Paper/Bukkit Plugin Edition

> **Status: idea only — NOT scheduled, NOT in any release plan.**
> Do not implement this. It lives here so the idea is not lost.

## The idea in one sentence

JustQuests will also be released as a **server plugin for Paper/Bukkit**,
so servers can offer quests without switching to a mod loader at all.

## Why

Many servers run Paper or Bukkit/Spigot and will never move to NeoForge,
Fabric or Forge. Server owners should not have to change their server
software just to get JustQuests. They keep Paper/Bukkit, drop the plugin
in, and it runs entirely on the server.

## How it should work

- The plugin runs **fully server-side** — players joining the server need
  to install nothing; quests, progress and rewards work for everyone
- Same idea, same commands (`/quest list`, `accept`, `progress`, ...) as
  the mod editions
- The quest JSON format should stay identical across mod and plugin, so a
  quest pack written once works everywhere (storage backend will differ —
  plugins use their own data folder instead of datapacks/attachments)

## Design decisions (answered 2026-06-10)

**The AI quest generator comes to the plugin too.**
Somewhat later than in the mod, but not much later — by then the test
phases of the mod will already have produced balancing data, so the
plugin version starts from proven rules instead of from scratch. On a
server the AI runs on the server host.
(See [idea-ai-quest-generator.md](idea-ai-quest-generator.md).)

**Quest progress lives in the world folder and migrates with the world.**
- The progress file is stored **inside the world folder**
- Singleplayer world (played with the mod) moved onto a Paper server →
  the progress file travels with the world, the plugin picks it up and
  players continue where they left off
- Plugin installed fresh on a server whose world never saw the mod →
  there is simply no progress to import; the plugin creates a fresh
  progress file in the world folder

**Team systems: integrate, don't build (answered 2026-06-11, Q14).**
JustQuests ships no own team system. If the server runs one (a team mod
or plugin), JustQuests works together with it. Consequence for the
shared progress file: reserve an optional **team/group id field** from
the first version of the format, so external team systems can plug in
without a format migration.

**Team quests are their own category (added 2026-06-11).**
Likely a dedicated **team quest category**, designed specifically for
teams — instead of turning normal quests shareable. Normal quests stay
personal (with their exclusive claiming); team quests are where group
play happens. Fits the existing category system (each category has its
own symbol, see the AI brief).

**Implication for storage architecture (note, not a work order):**
v0.1 currently stores progress in per-player NBT attachments
(`playerdata`), not as a standalone file in the world folder. Before the
plugin edition happens, the mod's storage needs to move to (or mirror
into) a defined world-folder file format that both editions read and
write — that shared file IS the compatibility layer between mod and
plugin.

## Notes for later

- The mod is already designed server-side friendly (clients don't need it
  for command play), which makes a plugin port realistic
- Timing: after the mod editions are established; no date set
