# tools — data scripts

Python 3 scripts that write and check the bundled data of `justquests-generator-v2`. The JSON files
under `justquests-generator-v2/src/main/resources/justquests_genv2/` are the source of truth that
ships; these scripts are only needed to change or re-verify them. Standard library only.

| Script | Purpose |
|---|---|
| `build_data.py` | writes every JSON data file from the modules below |
| `catalog_dsl.py` | the tiny `E(...)`/`T(...)` helpers used to write entries and targets |
| `vanilla_catalog.py`, `vanilla_more.py` | the vanilla catalog (458 targets) |
| `mod_profiles.py`, `mod_more.py` | Farmer's Delight and Create |
| `mod_mekanism.py`, `mod_twilightforest.py`, `mod_botania.py` | Mekanism, The Twilight Forest, Botania |
| `mod_ae2.py`, `mod_immersive.py`, `mod_bop.py`, `mod_alexsmobs.py`, `mod_tinkers.py`, `mod_ars.py` | Applied Energistics 2, Immersive Engineering, Biomes O' Plenty, Alex's Mobs, Tinkers' Construct, Ars Nouveau (0.4.0) |
| `other_data.py` | rewards, templates, vanilla themes, balance, tag concepts |
| `verify_vanilla.py [research_dir]` | checks the vanilla catalog, rewards, effects, loot tables and tags against Minecraft's registries and recipes for 17 versions (expects `<research_dir>/mcmeta`, clones of misode/mcmeta per version) |
| `verify_jars.py <facts_dir>` | checks the profiles against facts read from the mods' released jars (ids, crafting-table and furnace recipes, mobs, block loot tables, tool tags; see `MODS.md` §8) |
| `verify_mods.py [research_dir]` | checks the mod profiles against the mods' language files and generated data (expects `<research_dir>/lang/*.json` and sparse clones of the mods' repositories; see `MODS.md` §7) |
| `lang_data.py` | builds `lang/<code>.json` (German, French, Spanish, Japanese) from `i18n/` and stops on anything that does not line up with the English data; called by `build_data.py` |
| `lang_names.py <.minecraft> <mod_jars_dir>` | collects the official translated names of every catalog target from a local Minecraft install and the mods' jars into `i18n/names/` |

Typical workflow after editing a module:

```
python3 tools/build_data.py
python3 tools/verify_vanilla.py /path/to/research
python3 tools/verify_mods.py /path/to/research
cd justquests-generator-v2 && ./gradlew build catalogReport simulate
```

## Languages of the generated quests

The generator picks every title template, phrase, theme text and hint in English, then renders the
same picks in each extra language, so a translation always says what the English says. The sources
live in `i18n/`:

| File | Content |
|---|---|
| `templates/<lang>.json` | grammar (sentence end, list words, lower-casing) and the title/phrase templates, index-aligned with `templates.json` |
| `themes.json` | theme key → language → `[names..., description]`, index-aligned with the English theme |
| `common.json` | tool and biome hints, tag nouns, reward messages, dimension names, family names, name overrides |
| `hints/*.json` | each English catalog hint → `[de_de, fr_fr, es_es, ja_jp]` |
| `names/<lang>.json` | official item, block and mob names (written by `lang_names.py`; fix single names in `common.json` → `nameOverrides`) |

Placeholders: `{Name}` (name as written), `{name}` (lower-cased mid-sentence in French and Spanish),
`{de_name}` (French "de"/"d'" + name), `{count}`, `{Dimension}`, `{dimension_to}` / `{dimension_in}`
(travel and location phrases), `{Family}`, `{list}`, `{noun}`. A new catalog hint, theme, template or
family needs its translations before `build_data.py` runs through; `LocalizationTest` checks the result.

