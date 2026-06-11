# License Guide for JustQuests

Plain-language overview of the licenses that make sense for this project
(mod + future plugin edition), and why the current choice is what it is.

> **Current status:** JustQuests is **MIT-licensed** (see [LICENSE](../LICENSE)).

## What a license decides (in mod terms)

- May pack makers **put the mod into modpacks** and redistribute it?
- May others **read, modify and fork** the code?
- Must forks **stay open source** (copyleft) or may they go closed?
- Must users **credit** the original author?
- Nobody can sue the author if the mod breaks something (every license
  below disclaims warranty)

A license does **not** protect the name/branding, and it cannot
practically stop illegal reuploads — taking stolen reuploads down is a
platform/DMCA matter, regardless of license.

## The realistic options

### MIT — current choice
- **Anyone may do anything** (use, modify, redistribute, modpacks,
  closed-source forks), only the copyright notice must stay
- Simplest license that exists; pack-maker friendliest option
- Risk: someone could fork JustQuests closed-source — in practice rare
  and rarely harmful for mods
- Used by: huge parts of the Fabric ecosystem, countless mods

### LGPL-3.0 — the "protect my code" middle ground
- Modpacks and use: free, like MIT
- **Forks/modified versions must stay LGPL** (open source) — nobody can
  take the code closed
- Other mods may depend on/interact with it without restrictions
- NeoForge itself is LGPL-2.1, so the ecosystem knows this license well
- Choose this if closed-source forks would genuinely bother you

### GPL-3.0 — strong copyleft
- Everything that builds on the code must become GPL too
- In modding this creates friction (addons must be GPL) — usually more
  enforcement than a quest mod needs

### Apache-2.0 — MIT plus lawyers
- Like MIT, plus an explicit patent grant and "state your changes" rule
- Sensible for corporate projects; overkill for a Minecraft mod

### MPL-2.0 — file-level copyleft
- Changed **files** must stay open, new files around them may be closed
- Reasonable middle ground, but uncommon in modding — pack makers know
  MIT and LGPL much better

### ARR (All Rights Reserved) — maximum control
- Nobody may redistribute without asking — **breaks modpack inclusion**
  unless you grant permission case by case
- Actively hostile to the stated JustQuests goal ("use it in any pack")
- Not recommended

## Special note for the future plugin edition

Paper/Spigot/Bukkit APIs are **GPL-3.0**. A plugin linking against them
should use a GPL-compatible license. **MIT is GPL-compatible**, so the
plugin edition can simply stay MIT — no change needed.

## Comparison

| | Modpacks OK | Forks may go closed | Ecosystem familiarity | Complexity |
|---|---|---|---|---|
| **MIT** | ✅ | ✅ | very high | minimal |
| **LGPL-3.0** | ✅ | ❌ (stay open) | high | low |
| **GPL-3.0** | ✅ | ❌ (everything GPL) | medium | medium |
| **Apache-2.0** | ✅ | ✅ | medium | medium |
| **MPL-2.0** | ✅ | partially | low | medium |
| **ARR** | ❌ (ask first) | — | high (CurseForge) | none |

## Recommendation

**Stay with MIT.** It matches the project goals exactly: maximum
pack-maker friendliness, zero friction for adoption, simplest possible
terms, GPL-compatible for the plugin edition. The only reason to switch
would be if closed-source forks become a real concern — then **LGPL-3.0**
is the natural upgrade (modpacks stay unaffected; switching is possible
for future versions at any time, since there are no outside contributors
yet whose consent would be needed).
