# JustQuests — YouTube Short script & how-to (v0.2.0 showcase)

A complete, beginner-friendly guide to making one ~60-second vertical Short
that shows off JustQuests. Read top to bottom: it tells you **what to film**,
**how to get the AI voice**, and **how to put it together**.

> Two text blocks live here:
> - **Section 2** = the *full* voiceover (with `[pause]` markers — the pauses
>   are only for editing, you do NOT read them out loud).
> - **Section 3** = a *shortened* voiceover that fits the 500-character limit
>   of free AI-voice sites like tiktokvoice.net.

---

## 1. How to make this video — step by step

1. **Record the gameplay first** (no audio needed yet). Use the test
   instance (Minecraft 1.21.1, singleplayer) so the quest book (key **J**)
   works. Capture everything in **Section 5 (B-roll checklist)**.
   - Recorder: OBS Studio (free) or the Windows Game Bar (`Win+G`).
   - Record at normal 16:9 — you'll crop to vertical in step 4.
2. **Make the AI voiceover** (Section 4). Paste the Section 3 text into
   tiktokvoice.net, pick an English voice, download the MP3.
3. **Open a video editor** (free options: CapCut, DaVinci Resolve, or
   Shotcut). Create a **vertical 9:16 project, 1080×1920**.
4. **Drop in the voiceover MP3 first**, then lay your gameplay clips on top,
   matching each line to the matching footage (use the table in Section 6).
   Crop/zoom each clip so the important part (the quest book) is centered in
   the vertical frame.
5. **Add the on-screen text overlays** from Section 7 (big, centered).
6. **Add quiet background music** (YouTube Audio Library has free tracks).
   Keep it low so the voice is clear.
7. **Export** as MP4, 1080×1920, 30 fps. Upload as a **Short** (vertical +
   under 60 s = YouTube treats it as a Short automatically). Put the links
   from Section 8 in the description.

That's it. Total time once you have footage: ~30–45 min.

---

## 2. Full voiceover (the script — `[pause]` = a beat for editing, don't say it)

> Tired of heavy quest mods? [pause]
> This is **JustQuests** — lightweight quests for Minecraft.
> Press **J** — and there's your quest book. [pause]
> Pick a quest, see your goals, watch the progress fill in, grab the reward.
> [pause]
> Mine, craft, breed, tame, slay, explore — over a dozen quest types. [pause]
> Want your own? One JSON file. Save it, and it loads live — no restart.
> [pause]
> It runs on Minecraft 1.21 all the way to 1.21.10, in four languages. [pause]
> And the final look of this GUI? **You** decide — three designs, voted on
> our Discord. [pause]
> JustQuests. Free on Modrinth. Link in the description — come build yours.

---

## 3. Shortened voiceover (paste THIS into tiktokvoice.net — 428 chars, fits 500)

```
Tired of heavy quest mods? This is JustQuests, lightweight quests for Minecraft. Press J for your quest book. Pick a quest, watch the progress fill in, grab the reward. Mine, craft, breed, tame, explore, over a dozen quest types. Want your own? One file, and it loads live, no restart. It runs on Minecraft 1.21 up to 1.21.10. And this GUI's final look? You decide, vote on our Discord. JustQuests, free on Modrinth, link below.
```

Commas (not dashes) are used on purpose so the AI voice pauses cleanly. No
`[pause]` markers — you add those gaps in the editor.

---

## 4. Getting the AI voice (tiktokvoice.net) — exact steps
1. Go to **tiktokvoice.net**.
2. Paste the **Section 3** text into the box.
3. **Language: English. Voice:** pick a male narrator — e.g. **"Joey"** or
   **"Narrator"** (these sound like the typical TikTok/Shorts voice).
4. Click generate, then **download the MP3**.
5. If a word sounds wrong (e.g. "JSON" or "1.21"), tell me and I'll respell
   that part phonetically (e.g. "one twenty-one") so it reads correctly.
- Alternatives if you want higher quality: **ElevenLabs** (voice "Brian"/
  "Adam", free tier) or the TikTok app's own text-to-speech.

---

## 5. B-roll / footage checklist (film these first)
Tick each off — you'll need one short clip (2–5 s) for every line:
- [ ] Press **J**, quest book opens (clean shot).
- [ ] Click through 2–3 quests; pause on the detail pane (objectives + rewards).
- [ ] Complete a quest on camera so the **✓ action-bar toast + level-up sound** show.
- [ ] 3–4 quick gameplay clips matching the quest types you mention
      (mining, crafting, a mob kill, walking through a Nether portal).
- [ ] Editing `custom-quests.json` in a text editor, saving, then it showing
      up via `/quest list` (proves "loads live").
- [ ] A language switch (Options → Language, EN → DE) so quest text changes.
- [ ] Your Discord server screen (and a poll mockup image if you have one).
- [ ] The Modrinth page (justquests) for the end card.

---

## 6. Shot-by-shot timeline (match voice line → footage → overlay)

| Time | Voice line (from Section 3) | Show this footage | Overlay text |
|------|------|------|------|
| 0:00–0:03 | "Tired of heavy quest mods?" | quick zoom on the world | **JustQuests** (logo) |
| 0:03–0:07 | "This is JustQuests, lightweight quests for Minecraft." | press J, book opens | — |
| 0:07–0:16 | "Press J for your quest book. Pick a quest, watch the progress fill in, grab the reward." | click a quest, show `6/10`, complete one → ✓ toast | Track progress |
| 0:16–0:24 | "Mine, craft, breed, tame, explore, over a dozen quest types." | fast montage of those actions | 13 objective types |
| 0:24–0:34 | "Want your own? One file, and it loads live, no restart." | edit `custom-quests.json` → save → `/quest list` | Make your own |
| 0:34–0:42 | "It runs on Minecraft 1.21 up to 1.21.10." | Modrinth versions / language switch | 1.21–1.21.10 · EN/DE/FR/ES |
| 0:42–0:52 | "And this GUI's final look? You decide, vote on our Discord." | Discord server / poll mockup | Vote on Discord |
| 0:52–0:60 | "JustQuests, free on Modrinth, link below." | Modrinth page + logo end card | Free on Modrinth ⬇ |

Tip: if you run over 60 s, cut the version/language shot (0:34–0:42) first.

---

## 7. On-screen text overlays (big, centered, keep inside the middle ~80%)
`JustQuests` · `Track progress` · `13 objective types` · `Make your own` ·
`1.21–1.21.10 · EN/DE/FR/ES` · `Vote on Discord` · `Free on Modrinth ⬇`

Also burn in **subtitles** of the spoken line — most Shorts are watched muted.

---

## 8. Upload details (copy into the YouTube description)
- **Title:** `I made a lightweight Minecraft quest mod (you vote on the GUI!)`
- **Discord:** https://discord.gg/cMTGE9QCja
- **Modrinth:** https://modrinth.com/mod/justquests
- **Hashtags:** #minecraft #minecraftmods #neoforge #modrinth #shorts

---

## 9. Good to know
- The GUI you're filming is the **interim** one — that's the whole point of
  the "you vote" line. It doesn't need to look final; the vote does the rest.
- Record in **singleplayer** — that's where this GUI reads its data.
- Want the voiceover script in **German** instead? Ask and I'll rewrite
  Sections 2 + 3 in German (still under 500 chars).
