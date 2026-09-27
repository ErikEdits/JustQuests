# Samples

Everything in this folder except `world-overrides/` is generated and deterministic: running the three
Gradle tasks again reproduces the files byte for byte.

| Path | Task | Content |
|---|---|---|
| `vanilla/`, `modded/`, `mc-1.18.2/` | `./gradlew samples` | example sets with `explain()` output |
| `catalog-report.md` | `./gradlew catalogReport` | every catalog target: type, count range, minutes, which difficulties can use it |
| `simulation/{easy,normal,hard}.md` | `./gradlew simulate` | 14 simulated days on a busy server, with variety figures and `stats().toText()` |
| `world-overrides/` | hand-written, checked by `WorldOverrideSamplesTest` | copy-ready examples of a world `balance.json` override and a world profile for another mod (see its README) |

## Example sets (`SampleWriter.java`)

| Setting | Value |
|---|---|
| World seeds | 1, 2, 3 |
| Difficulties | easy, normal, hard |
| Quests per set | 10 |
| Clock | 2026-09-27 12:00 UTC (cycle 1790510400), zone UTC |
| World | game day 30, 2 players online, 100 % have entered the Nether, 50 % the End (so Hard may use Nether/End content) |
| Host answers | every TriState `UNKNOWN`, English names `null`, stack sizes unknown (the hardest case for the core, and what a dedicated server without mod lang files looks like) |
| `vanilla/` | no mods loaded, Minecraft 1.21.1 |
| `modded/` | Farmer's Delight, Create, Mekanism, the Twilight Forest (dimension present) and Botania loaded; default modded share 0.35, so each set has one quest per mod and the rest vanilla |
| `mc-1.18.2/` | no mods, Minecraft 1.18.2 on Forge: nothing released after 1.18.2 appears (mangrove, cherry, armadillo, …) |

Files per set:

- `seed<N>-<difficulty>.json` — exactly what `servedQuests()` returns: quest id → quest JSON (§6
  format), in serve order (`sort` = estimated minutes).
- `seed<N>-<difficulty>.explain.txt` — `status()` of the generator, then `explain()` for every quest:
  effort per unit and modifiers, target time and range, tier and unlock reason, family, reward values
  vs. budget, rejected alternatives.

## Catalog report (`CatalogReport.java`)

One table per profile with every target at Minecraft 1.21.1 with all five mods loaded, the Nether,
the End and the Twilight Forest unlocked: count range, minutes at the smallest and largest count,
and whether Easy, Normal and Hard can use it (tier cap, minimum difficulty and the time range all
pass). The summary lists targets per profile and type and the size of the candidate pool per
difficulty. Use it to tune `effort`, `min`/`max` and tiers after editing a profile.

## Simulations (`Simulation.java`)

14 days (28 rotations of 10 quests) with 8 players and all five mods. Every 5 simulated minutes each
idle player takes a random free quest with probability 0.035, and finishes it after 0.7–2.5 times the
estimate (12 % give up instead). Each file reports repeats within the history window (always 0),
distinct signatures, objective types, profiles and families, followed by the statistics text that
`/quest generator stats` would show, including the calibration ratio the self-calibration would use.
