# Idea: Localization System

> **Status: design decided, not scheduled for a specific release.**
> Captured from Q20/Q21 in [open-questions.md](open-questions.md).

## The idea in one sentence

Every player sees quests in **their own Minecraft language** — two players
on the same server, two languages, same quest.

## How it works

**Per-user language detection (Q20, 2026-06-11).**
The mod reads each player's Minecraft language setting and displays all
quest UI and texts in that language. English, German, Japanese, … —
whatever the player selected.

**Reuse Minecraft's built-in translations (Q21, 2026-06-12).**
Minecraft ships translations for 100+ languages for every item, block,
mob and biome via translation keys (`item.minecraft.oak_log` →
"Eichenstamm" / "オークの原木"). Other mods bring their own lang files
the same way. JustQuests builds objective texts from these keys:

- Template skeleton (`"Collect %d x %s"`) → translated **once per
  language by us**, one line in the mod's lang files
- Item/block/mob name → translated **automatically by Minecraft**, in
  the player's language, zero work for quest authors

This makes **all objectives fully multilingual for free** — including
generated quests, since they are built purely from such blocks.

**Author-written texts: multi-language fields with English fallback
(Q21, 2026-06-12).**
Only free-form quest **titles** and **descriptions** cannot come from
Minecraft. For those:

```json
"title": {
  "en": "First Steps",
  "de": "Erste Schritte"
}
```

- Authors fill in the languages they know — no language is mandatory
  except a sensible default
- **Fallback is English**: a player whose language is missing sees the
  English text
- A plain string (`"title": "First Steps"`) stays valid and counts as
  the English/fallback text — existing quests keep working

## Open

- Q22 (launch languages for the mod's own texts, community
  translations) — see [open-questions.md](open-questions.md)
