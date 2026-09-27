"""Writes the bundled data files into src/main/resources/justquests_genv2/."""
import os, sys
sys.path.insert(0, os.path.dirname(__file__))
from catalog_dsl import dump
from vanilla_catalog import VANILLA
from mod_profiles import FARMERS_DELIGHT, CREATE
from other_data import REWARDS, TEMPLATES, THEMES, BALANCE, TAGS
from mod_mekanism import MEKANISM, MEK_FAMILY_NAMES, MEK_TAGS
from mod_twilightforest import TWILIGHT_FOREST, TF_FAMILY_NAMES, TF_DIMENSION_NAMES
from mod_botania import BOTANIA, BOT_FAMILY_NAMES

TEMPLATES["familyNames"].update(MEK_FAMILY_NAMES)
TEMPLATES["familyNames"].update(TF_FAMILY_NAMES)
TEMPLATES["familyNames"].update(BOT_FAMILY_NAMES)
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
dump({"format": 1, "profiles": ["farmersdelight.json", "create.json", "mekanism.json", "twilightforest.json",
                                "botania.json"]},
     os.path.join(ROOT, "catalog", "profiles", "index.json"))
dump(REWARDS, os.path.join(ROOT, "rewards.json"))
dump(TEMPLATES, os.path.join(ROOT, "templates.json"))
dump(THEMES, os.path.join(ROOT, "themes.json"))
dump(BALANCE, os.path.join(ROOT, "balance.json"))
dump(TAGS, os.path.join(ROOT, "tags.json"))
print("written to", os.path.normpath(ROOT))
