"""Botania profile (Botania License: open, attribution to Vazkii as the creator of Botania).

Ids, names and grid recipes were checked against the mod's data on GitHub: Xplat lang/en_us.json of the
1.18.x, 1.19.x and 1.20.x branches (the released versions; 1.21.1 was still a porting branch) and the
generated recipes and loot tables of 1.20.x. Only facts are used (ids, names, which items have a
crafting-grid recipe, what drops what); nothing is copied. The runtime existence checks keep the profile
safe on versions it was not checked against.

Livingwood and livingrock come from a Pure Daisy (not a grid), so everything built from them is Normal
or Hard and says so in its hint.
"""
from catalog_dsl import E, T

B = "botania:"
FLOWER_HINT = "Mystical flowers grow in forests, plains and meadows."
PETAL_HINT = "One mystical flower makes two petals."

BOT_ENTRIES = [
    E("bot_flowers", "bot_flowers", 0, [
        T("collect_item", B + "white_mystical_flower", 0.35, 4, 16, name="Mystical White Flower", hint=FLOWER_HINT),
        T("collect_item", B + "red_mystical_flower", 0.35, 4, 16, name="Mystical Red Flower", hint=FLOWER_HINT),
        T("collect_item", B + "yellow_mystical_flower", 0.35, 4, 16, name="Mystical Yellow Flower", hint=FLOWER_HINT),
        T("collect_item", B + "blue_mystical_flower", 0.35, 4, 16, name="Mystical Blue Flower", hint=FLOWER_HINT),
        T("collect_item", B + "purple_mystical_flower", 0.35, 4, 16, name="Mystical Purple Flower",
          hint=FLOWER_HINT),
        T("collect_item", B + "pink_mystical_flower", 0.35, 4, 16, name="Mystical Pink Flower", hint=FLOWER_HINT),
    ]),
    E("bot_petals", "bot_petals", 1, [
        T("craft_item", B + "white_petal", 0.2, 8, 32, name="Mystical White Petal", hint=PETAL_HINT),
        T("craft_item", B + "red_petal", 0.2, 8, 32, name="Mystical Red Petal", hint=PETAL_HINT),
        T("craft_item", B + "yellow_petal", 0.2, 8, 32, name="Mystical Yellow Petal", hint=PETAL_HINT),
        T("craft_item", B + "blue_petal", 0.2, 8, 32, name="Mystical Blue Petal", hint=PETAL_HINT),
        T("craft_item", B + "purple_petal", 0.2, 8, 32, name="Mystical Purple Petal", hint=PETAL_HINT),
        T("craft_item", B + "pink_petal", 0.2, 8, 32, name="Mystical Pink Petal", hint=PETAL_HINT),
    ]),
    E("bot_decor", "bot_decor", 1, [
        T("craft_item", B + "white_petal_block", 2.0, 1, 4, hint="Nine mystical white petals."),
        T("craft_item", B + "red_petal_block", 2.0, 1, 4, hint="Nine mystical red petals."),
        T("craft_item", B + "apothecary_mossy", 3.0, 1, 1, name="Mossy Petal Apothecary",
          hint="Mossy cobblestone around a mystical petal."),
    ]),
    E("bot_start", "bot_start", 1, [
        T("craft_item", B + "lexicon", 1.6, 1, 1, name="Lexica Botania", plural="Lexica Botania",
          hint="A book and any sapling."),
        T("craft_item", B + "apothecary_default", 2.6, 1, 1, name="Petal Apothecary",
          hint="Cobblestone around a mystical petal."),
        T("craft_item", B + "flower_bag", 3.0, 1, 1, name="Flower Pouch", minDifficulty="normal",
          hint="Six wool and a mystical petal."),
    ]),
    E("bot_mushrooms", "bot_mushrooms", 1, [
        T("collect_item", B + "white_mushroom", 0.7, 2, 8, name="White Shimmering Mushroom",
          hint="Shimmering mushrooms glow in caves."),
        T("collect_item", B + "red_mushroom", 0.7, 2, 8, name="Red Shimmering Mushroom",
          hint="Shimmering mushrooms glow in caves."),
        T("collect_item", B + "blue_mushroom", 0.7, 2, 8, name="Blue Shimmering Mushroom",
          hint="Shimmering mushrooms glow in caves."),
        T("collect_item", B + "purple_mushroom", 0.7, 2, 8, name="Purple Shimmering Mushroom",
          hint="Shimmering mushrooms glow in caves."),
    ], hints=["caves"]),
    E("bot_livingwood", "bot_livingwood", 2, [
        T("craft_item", B + "livingwood_planks", 1.0, 4, 16,
          hint="A Pure Daisy turns logs placed around it into livingwood."),
        T("craft_item", B + "twig_wand", 9.0, 1, 1, name="Wand of the Forest", plural="Wands of the Forest",
          hint="A livingwood twig and two petals; livingwood comes from a Pure Daisy."),
    ], exclusions=["collect_item botania:livingwood_log (a Pure Daisy converts logs in place; nothing is picked up)"]),
    E("bot_manasteel", "bot_mana", 3, [
        T("craft_item", B + "manasteel_pick", 14.0, 1, 1, name="Manasteel Pickaxe", minDifficulty="hard",
          hint="Manasteel ingots and livingwood twigs; iron becomes manasteel in a mana pool."),
        T("craft_item", B + "manasteel_sword", 13.0, 1, 1, minDifficulty="hard",
          hint="Manasteel ingots and a livingwood twig; iron becomes manasteel in a mana pool."),
        T("craft_item", B + "livingwood_twig", 1.2, 2, 8, hint="Two livingwood logs from a Pure Daisy."),
        T("craft_item", B + "glimmering_livingwood", 1.4, 2, 8, minDifficulty="normal",
          hint="Livingwood and glowstone dust."),
    ]),
    E("bot_alfheim", "bot_alfheim", 4, [
        T("craft_item", B + "dreamwood_planks", 0.2, 8, 32, minDifficulty="hard",
          hint="Dreamwood comes from the elves through an Elven Gateway."),
        T("craft_item", B + "elementium_pickaxe", 20.0, 1, 1, minDifficulty="hard",
          hint="Elementium ingots and dreamwood twigs, both traded with the elves."),
    ], weight=0.5),
    E("bot_mana", "bot_mana", 2, [
        T("craft_item", B + "mana_pool", 16.0, 1, 1, minDifficulty="hard",
          hint="Five livingrock; a Pure Daisy turns stone into livingrock."),
    ], exclusions=["craft_item botania:mana_spreader (its grid recipe varies between versions)"]),
]

BOT_REWARDS = [
    {"id": B + "fertilizer", "value": 0.6, "tier": 1, "max": 16, "family": "bot_flowers"},
    {"id": B + "white_petal", "value": 0.2, "tier": 1, "max": 32, "family": "bot_petals"},
    {"id": B + "manasteel_ingot", "value": 3.5, "tier": 2, "max": 8, "family": "bot_mana"},
    {"id": B + "mana_pearl", "value": 5.0, "tier": 3, "max": 2, "family": "bot_mana"},
]

BOT_THEMES = [
    {"key": "bot_first_steps", "names": ["Botanist's Start", "Garden Magic", "Flower Power"],
     "descriptions": ["Every botanist starts in a meadow: {list}."],
     "slots": [{"types": ["collect_item"], "keys": ["bot_flowers"]},
               {"types": ["craft_item"], "keys": ["bot_start", "bot_petals"]}]},
    {"key": "bot_florist", "names": ["Florist", "Bouquet", "Petal Picker"],
     "descriptions": ["The florist wants colour: {list}."],
     "slots": [{"types": ["collect_item"], "keys": ["bot_flowers"]},
               {"types": ["collect_item", "craft_item"], "keys": ["bot_flowers", "bot_petals"]}]},
]

BOTANIA = {
    "format": 1,
    "id": "botania",
    "name": "Botania",
    "requiresMod": ["botania"],
    "minecraft": {"min": "1.18.2"},
    "entries": BOT_ENTRIES,
    "rewards": {"items": BOT_REWARDS},
    "themes": BOT_THEMES,
}

BOT_FAMILY_NAMES = {
    "bot_flowers": "Florist", "bot_petals": "Petal", "bot_start": "Botanist", "bot_mushrooms": "Shimmering",
    "bot_livingwood": "Livingwood", "bot_mana": "Mana", "bot_decor": "Petal Decor", "bot_alfheim": "Alfheim",
}
