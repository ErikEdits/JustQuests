# Discord Poll Questions (JustQuests)

Ready-to-run community polls for the JustQuests Discord. These are the
design decisions deliberately deferred to the community
(from [open-questions.md](open-questions.md): Q10, Q11, Q12, Q36, Q37).

Run each one with the bot's poll cog (reaction poll → results DM'd to the
owner). The `/poll create` line under each question is ready to paste.

> Tip: don't post all 5 at once — space them out so people actually vote
> on each. One per day works well.

---

## Poll 1 — How should the quest GUI open? (Q10)

**Question:** How do you want to open the JustQuests menu?
1. Command (`/quest`)
2. A keybind (press a key)
3. A quest book item in the inventory
4. A button on the inventory screen

```
/poll create frage:How do you want to open the JustQuests menu? optionen:Command (/quest), A keybind, A quest book item, A button in the inventory screen dauer_stunden:24
```

---

## Poll 2 — Quest book item? (Q11)

**Question:** Should there be a quest book item?
1. Yes — a book that appears automatically in the inventory (can't be dropped)
2. No — command / inventory button is enough

```
/poll create frage:Should there be a quest book item? optionen:Yes - auto-given book (undroppable), No - command/button is enough dauer_stunden:24
```

---

## Poll 3 — Notifications on progress & completion? (Q12)

**Question:** How should JustQuests notify you about quest progress and completion?
1. Chat messages only
2. Chat + toast pop-ups (top-right, like advancements)
3. Chat + sounds
4. Everything (chat + toasts + sounds)

```
/poll create frage:How should JustQuests notify you about quest progress and completion? optionen:Chat only, Chat + toast pop-ups, Chat + sounds, Everything (chat+toasts+sounds) dauer_stunden:24
```

---

## Poll 4 — GUI style direction? (Q37)

**Question:** Which look should the JustQuests GUI have?
1. Book style — two open pages (classic quest book)
2. Modern single panel — one clean scrollable list

```
/poll create frage:Which look should the JustQuests GUI have? optionen:Book style (two pages), Modern single panel list dauer_stunden:24
```

---

## Poll 5 — Update notice? (Q36)

**Question:** Should JustQuests tell OPs at login when a newer version is out?
1. Yes — notify on login (can be turned off)
2. No — no version checking at all

```
/poll create frage:Should JustQuests tell OPs at login when a newer version is out? optionen:Yes - notify on login (toggleable), No - no version check dauer_stunden:24
```

---

## Poll 6 — HUD quest tracker (Q43)

**Question:** Want a small on-screen tracker showing your active quest and progress?
1. Yes — show it on the HUD (toggleable)
2. Only in the menu/GUI, not on screen
3. Don't care

```
/poll create frage:Want a small on-screen tracker showing your active quest and progress? optionen:Yes - HUD tracker (toggleable), Only in the menu, Don't care dauer_stunden:24
```

---

## After the polls

Record each result back in [open-questions.md](open-questions.md) (the
matching Q gets its final answer), then move it into the relevant brief.
