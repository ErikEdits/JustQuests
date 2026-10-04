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

Typical workflow after editing a module:

```
python3 tools/build_data.py
python3 tools/verify_vanilla.py /path/to/research
python3 tools/verify_mods.py /path/to/research
cd justquests-generator-v2 && ./gradlew build catalogReport simulate
```
