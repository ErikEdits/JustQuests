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

## Notes for later

- The mod is already designed server-side friendly (clients don't need it
  for command play), which makes a plugin port realistic
- Timing: after the mod editions are established; no date set
- Open: whether the AI quest generator (see
  [idea-ai-quest-generator.md](idea-ai-quest-generator.md)) also comes to
  the plugin edition, running on the server host
