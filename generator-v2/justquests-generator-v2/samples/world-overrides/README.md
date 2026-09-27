# World overrides (examples)

Copy the `generator_v2/` folder into `<world>/justquests/` to try them. Both kinds of file are read at
server start and on `/quest reload`, and apply from the next rotation. `WorldOverrideSamplesTest`
loads exactly these files, so they stay valid.

| File | What it does |
|---|---|
| `generator_v2/balance.json` | deep-merged over the bundled `balance.json`: shorter Easy quests (3–8 minutes), 48-hour claim expiry on Hard, fewer themed quests, at most 40 % of a set for the one-quest-per-mod guarantee, Nether content from day 14 |
| `generator_v2/profiles/examplemod.json` | a complete profile for a hypothetical mod `examplemod`: an ore with its drop and storage block, a mob in the mod's own dimension (unlocked like the Nether), a reward item and a theme that mixes the mod with vanilla diamonds. It is inactive unless a mod with the id `examplemod` is installed |

Rules for real profiles: `DESIGN.md` §11. Check the result with `/quest generator status` (the
profile appears in the list once its mod is loaded) and `/quest generator preview 20`.
