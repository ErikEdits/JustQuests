## Generated quests 2.0

The quest generator was rebuilt from scratch.

- **Smarter quests.** Every generated quest is checked to be doable on your game version and mod set,
  sized to a sensible amount of play time, and paid with a reward that matches the effort. No more
  "collect 40 apples" or "collect iron ore" (which drops raw iron).
- **More variety.** Ten kinds of objectives — collect, mine, craft, smelt, hunt, breed, tame, eat,
  place and travel — plus themed multi-step quests like *Blacksmith's Order*, *Harvest Festival* or
  *Nether Expedition*. Each description tells you where to look and what tool you need.
- **Accepted quests are safe.** A quest you accepted stays until you finish or abandon it, even when
  the board rotates or the server restarts.
- **First come, first served.** A generated quest someone accepted is taken; everyone can hold one
  generated quest at a time. (Both rules can be switched off in `settings.json`.)
- **Difficulty for the whole world:** `easy`, `normal` or `hard` (OP: `/quest difficulty`). Hard means
  longer expeditions, deeper tiers and better rewards.
- **Grows with your world.** Nether and End quests appear once players have been there, and quests
  in the Twilight Forest once players have found a portal.
- **Mod support:** with **Farmer's Delight**, **Create**, **Mekanism**, **The Twilight Forest** or
  **Botania** installed, the board also offers quests for their crops, meals, ores, machines, mobs and
  flowers. Every installed mod gets its turn on the board.
- **Right for your version:** content that your Minecraft version only has behind an experimental
  toggle (for example cherry wood on 1.19.4) never shows up.
- **For admins:** `/quest generator status | explain <id> | preview | stats | release <id>`, new
  `settings.json` keys (`difficulty`, `generatorExclusiveClaims`, `generatorOneActivePerPlayer`, …),
  optional world overrides in `justquests/generator_v2/`. Existing worlds are migrated automatically;
  quests you had active from the old generator are kept.
