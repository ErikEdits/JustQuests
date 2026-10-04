"""Writes the bundled data files into src/main/resources/justquests_genv2/."""
import os, sys
sys.path.insert(0, os.path.dirname(__file__))
from catalog_dsl import dump
import lang_data
from vanilla_catalog import VANILLA
from mod_profiles import FARMERS_DELIGHT, CREATE
from other_data import REWARDS, TEMPLATES, THEMES, BALANCE, TAGS
from mod_mekanism import MEKANISM, MEK_FAMILY_NAMES, MEK_TAGS
from mod_twilightforest import TWILIGHT_FOREST, TF_FAMILY_NAMES, TF_DIMENSION_NAMES
from mod_botania import BOTANIA, BOT_FAMILY_NAMES
from mod_ae2 import AE2, AE2_FAMILY_NAMES
from mod_immersive import IMMERSIVE, IE_FAMILY_NAMES
from mod_bop import BIOMES_O_PLENTY, BOP_FAMILY_NAMES
from mod_alexsmobs import ALEXS_MOBS, AM_FAMILY_NAMES
from mod_tinkers import TINKERS, TC_FAMILY_NAMES
from mod_ars import ARS_NOUVEAU, AN_FAMILY_NAMES

TEMPLATES["familyNames"].update(MEK_FAMILY_NAMES)
TEMPLATES["familyNames"].update(TF_FAMILY_NAMES)
TEMPLATES["familyNames"].update(BOT_FAMILY_NAMES)
for names in (AE2_FAMILY_NAMES, IE_FAMILY_NAMES, BOP_FAMILY_NAMES, AM_FAMILY_NAMES, TC_FAMILY_NAMES, AN_FAMILY_NAMES):
    TEMPLATES["familyNames"].update(names)
TEMPLATES["dimensionNames"].update(TF_DIMENSION_NAMES)
TAGS["concepts"].update(MEK_TAGS)

ROOT = os.path.join(os.path.dirname(__file__), "..", "justquests-generator-v2", "src", "main", "resources", "justquests_genv2")
os.makedirs(os.path.join(ROOT, "catalog", "profiles"), exist_ok=True)
dump(VANILLA, os.path.join(ROOT, "catalog", "vanilla.json"))
dump(FARMERS_DELIGHT, os.path.join(ROOT, "catalog", "profiles", "farmersdelight.json"))
dump(CREATE, os.path.join(ROOT, "catalog", "profiles", "create.json"))
dump(MEKANISM, os.path.join(ROOT, "catalog", "profiles", "mekanism.json"))
dump(TWILIGHT_FOREST, os.path.join(ROOT, "catalog", "profiles", "twilightforest.json"))
dump(BOTANIA, os.path.join(ROOT, "catalog", "profiles", "botania.json"))
EXTRA = {"ae2.json": AE2, "immersiveengineering.json": IMMERSIVE, "biomesoplenty.json": BIOMES_O_PLENTY,
         "alexsmobs.json": ALEXS_MOBS, "tconstruct.json": TINKERS, "ars_nouveau.json": ARS_NOUVEAU}
for name, profile in EXTRA.items():
    dump(profile, os.path.join(ROOT, "catalog", "profiles", name))
dump({"format": 1, "profiles": ["farmersdelight.json", "create.json", "mekanism.json", "twilightforest.json",
                                "botania.json"] + list(EXTRA)},
     os.path.join(ROOT, "catalog", "profiles", "index.json"))
dump(REWARDS, os.path.join(ROOT, "rewards.json"))
dump(TEMPLATES, os.path.join(ROOT, "templates.json"))
dump(THEMES, os.path.join(ROOT, "themes.json"))
dump(BALANCE, os.path.join(ROOT, "balance.json"))
dump(TAGS, os.path.join(ROOT, "tags.json"))
PROFILES = [VANILLA, FARMERS_DELIGHT, CREATE, MEKANISM, TWILIGHT_FOREST, BOTANIA] + list(EXTRA.values())
os.makedirs(os.path.join(ROOT, "lang"), exist_ok=True)
for code, data in lang_data.build(TEMPLATES, THEMES["themes"], PROFILES, REWARDS["messages"], TAGS).items():
    dump(data, os.path.join(ROOT, "lang", code + ".json"))
print("written to", os.path.normpath(ROOT))
