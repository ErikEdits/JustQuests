from catalog_dsl import T, E

FD = "farmersdelight:"
NE = "minecraft:the_nether"

FD_ENTRIES = [
    E("fd_cabbage", "fd_crops", 0, exclusions=[
        "craft_item farmersdelight:carrot_crate/potato_crate/beetroot_crate (recipes depend on the FD config option vanilla_crates_enabled)"],
      targets=[
        T("collect_item", FD + "cabbage", 0.15, 8, 48, hint="Cabbage seeds come from wild cabbages on beaches."),
        T("craft_item", FD + "cabbage_crate", 1.5, 1, 6, hint="Nine cabbages fill a crate."),
        T("consume_item", FD + "cabbage", 0.35, 4, 8),
    ], hints=["farm"]),
    E("fd_tomato", "fd_crops", 0, [
        T("collect_item", FD + "tomato", 0.15, 8, 48, hint="Tomato seeds come from wild tomatoes in warm, dry biomes."),
        T("craft_item", FD + "tomato_crate", 1.5, 1, 6, hint="Nine tomatoes fill a crate."),
        T("consume_item", FD + "tomato", 0.3, 4, 8),
    ], hints=["farm"]),
    E("fd_onion", "fd_crops", 0, [
        T("collect_item", FD + "onion", 0.15, 8, 48, hint="Onions come from wild onions; replant them."),
        T("craft_item", FD + "onion_crate", 1.5, 1, 6, hint="Nine onions fill a crate."),
    ], hints=["farm"]),
    E("fd_rice", "fd_crops", 0, [
        T("collect_item", FD + "rice_panicle", 0.2, 8, 32, hint="Grow rice in shallow water."),
        T("collect_item", FD + "rice", 0.15, 8, 48, hint="Wild rice grows in swamps and jungles."),
        T("craft_item", FD + "rice_bale", 1.9, 1, 4, hint="Nine rice panicles make a bale."),
    ], hints=["swamp"]),
    E("fd_wild", "fd_wild", 0, [
        T("mine_block", FD + "wild_cabbages", 0.6, 2, 12, hints=["beach"], hint="Wild cabbages grow on beaches."),
        T("mine_block", FD + "wild_beetroots", 0.6, 2, 12, hints=["beach"], hint="Wild beetroots grow on beaches."),
        T("mine_block", FD + "wild_tomatoes", 0.6, 2, 12, hints=["savanna"], hint="Wild tomatoes grow in warm, dry biomes."),
        T("mine_block", FD + "wild_onions", 0.5, 2, 12, hint="Wild onions grow in most overworld biomes."),
        T("mine_block", FD + "wild_carrots", 0.5, 2, 12, hint="Wild carrots grow in most overworld biomes."),
        T("mine_block", FD + "wild_potatoes", 0.5, 2, 12, hint="Wild potatoes grow in most overworld biomes."),
        T("mine_block", FD + "wild_rice", 0.6, 2, 12, hints=["swamp"], hint="Wild rice grows in swamps and jungles."),
    ], notes="Wild crops generate naturally (biome modifiers); breaking them counts once per plant."),
    E("fd_straw", "fd_straw", 0, [
        T("collect_item", FD + "straw", 0.12, 8, 48, hint="Cut grass or ripe wheat with a knife."),
        T("craft_item", FD + "rope", 0.08, 8, 64, hint="Two straw make four rope."),
        T("craft_item", FD + "canvas", 0.5, 2, 12, hint="Four straw make a canvas."),
        T("craft_item", FD + "straw_bale", 1.1, 1, 6),
        T("place_block", FD + "rope", 0.1, 8, 48, hint="Hang rope down a cliff or well."),
    ], tool="knife"),
    E("fd_knives", "fd_tools", 0, [
        T("craft_item", FD + "iron_knife", 0.7, 1, 2, tier=1),
        T("craft_item", FD + "diamond_knife", 5.5, 1, 1, tier=2, minDifficulty="hard"),
    ]),
    E("fd_kitchen", "fd_kitchen", 1, [
        T("craft_item", FD + "cutting_board", 0.4, 1, 3, tier=0),
        T("craft_item", FD + "cooking_pot", 3.0, 1, 1, hint="Five iron ingots, bricks, a wooden shovel and water."),
        T("craft_item", FD + "skillet", 2.4, 1, 1),
        T("craft_item", FD + "stove", 2.2, 1, 1, hint="Bricks, a campfire and iron."),
    ]),
    E("fd_cabinets", "fd_furniture", 0, [
        T("craft_item", FD + "oak_cabinet", 0.9, 1, 6, hint="Oak slabs and trapdoors."),
        T("craft_item", FD + "spruce_cabinet", 0.9, 1, 6, hint="Spruce slabs and trapdoors."),
        T("place_block", FD + "oak_cabinet", 0.95, 1, 6),
    ]),
    E("fd_butchery", "fd_butchery", 0, [
        T("collect_item", FD + "ham", 1.3, 1, 8, stack=64, hint="Pigs killed with a knife drop ham."),
        T("smelt_item", FD + "smoked_ham", 2.2, 1, 4, hint="Smoke ham in a smoker.", hints=["cook"]),
        T("smelt_item", FD + "cooked_bacon", 0.5, 4, 32, hint="Cut porkchops on a cutting board, then cook.", hints=["cook"]),
        T("smelt_item", FD + "beef_patty", 0.5, 4, 24, hint="Mince beef on a cutting board, then cook.", hints=["cook"]),
        T("smelt_item", FD + "cooked_chicken_cuts", 0.45, 4, 32, hint="Cut chicken on a cutting board, then cook.", hints=["cook"]),
        T("smelt_item", FD + "cooked_mutton_chops", 0.5, 4, 32, hint="Cut mutton on a cutting board, then cook.", hints=["cook"]),
    ], tool="knife"),
    E("fd_fish", "fd_seafood", 0, [
        T("smelt_item", FD + "cooked_cod_slice", 0.6, 4, 24, hint="Slice cod on a cutting board, then cook.", hints=["cook"]),
        T("smelt_item", FD + "cooked_salmon_slice", 0.9, 4, 16, hint="Slice salmon on a cutting board, then cook.", hints=["cook"]),
        T("craft_item", FD + "kelp_roll", 1.5, 1, 6, hint="Rice, kelp and a carrot."),
    ], tool="fishing_rod"),
    E("fd_breakfast", "fd_breakfast", 0, [
        T("smelt_item", FD + "fried_egg", 0.8, 2, 16, stack=64, hint="Cook eggs in a furnace or skillet.", hints=["cook"]),
        T("craft_item", FD + "egg_sandwich", 1.8, 1, 6),
        T("consume_item", FD + "bacon_and_eggs", 2.4, 1, 4, tier=1),
    ]),
    E("fd_sandwiches", "fd_meals", 1, [
        T("craft_item", FD + "hamburger", 2.6, 1, 6, hint="Bread, a beef patty, cabbage, tomato and onion."),
        T("craft_item", FD + "bacon_sandwich", 1.7, 1, 6),
        T("craft_item", FD + "chicken_sandwich", 1.7, 1, 6),
        T("craft_item", FD + "mutton_wrap", 1.9, 1, 6),
        T("consume_item", FD + "hamburger", 2.7, 1, 4),
        T("consume_item", FD + "bacon_sandwich", 1.8, 1, 4),
    ]),
    E("fd_salads", "fd_salads", 0, [
        T("craft_item", FD + "fruit_salad", 1.3, 1, 6, hint="Apple, melon, berries and a pumpkin slice."),
        T("craft_item", FD + "mixed_salad", 1.3, 1, 6, hint="Cabbage, tomato and beetroot in a bowl."),
        T("consume_item", FD + "fruit_salad", 1.4, 1, 4),
        T("consume_item", FD + "mixed_salad", 1.4, 1, 4),
        T("craft_item", FD + "nether_salad", 1.6, 1, 4, tier=3, hint="Crimson and warped fungi in a bowl."),
    ]),
    E("fd_sweets", "fd_sweets", 0, [
        T("craft_item", FD + "sweet_berry_cookie", 0.12, 8, 64, hints=["taiga"]),
        T("craft_item", FD + "honey_cookie", 0.15, 8, 64, hints=["bees"]),
        T("consume_item", FD + "sweet_berry_cookie", 0.3, 4, 16),
        T("craft_item", FD + "apple_pie", 3.0, 1, 3, hint="Needs a pie crust, apples and sugar."),
        T("craft_item", FD + "sweet_berry_cheesecake", 2.8, 1, 3),
        T("craft_item", FD + "melon_popsicle", 0.8, 2, 8, hint="Melon slices, ice and a stick."),
        T("consume_item", FD + "melon_popsicle", 0.9, 2, 8),
    ]),
    E("fd_stews", "fd_stews", 1, [
        T("consume_item", FD + "beef_stew", 2.2, 1, 4, hint="Cook it in a Cooking Pot."),
        T("consume_item", FD + "chicken_soup", 2.2, 1, 4, hint="Cook it in a Cooking Pot."),
        T("consume_item", FD + "vegetable_soup", 2.2, 1, 4, hint="Cook it in a Cooking Pot."),
        T("consume_item", FD + "fish_stew", 2.6, 1, 4, hint="Cook it in a Cooking Pot."),
        T("consume_item", FD + "pumpkin_soup", 2.4, 1, 4, hint="Cook it in a Cooking Pot."),
        T("consume_item", FD + "baked_cod_stew", 2.6, 1, 4, hint="Cook it in a Cooking Pot."),
        T("consume_item", FD + "noodle_soup", 2.8, 1, 4, hint="Cook it in a Cooking Pot."),
        T("consume_item", FD + "onion_soup", 2.2, 1, 4, hint="Cook it in a Cooking Pot."),
    ], exclusions=["craft_item farmersdelight:beef_stew (Cooking Pot output is not a crafting grid)"]),
    E("fd_plates", "fd_plates", 1, [
        T("consume_item", FD + "fried_rice", 2.4, 1, 4, hint="Cook it in a Cooking Pot."),
        T("consume_item", FD + "pasta_with_meatballs", 3.2, 1, 3, hint="Cook it in a Cooking Pot."),
        T("consume_item", FD + "ratatouille", 2.8, 1, 3, hint="Cook it in a Cooking Pot."),
        T("consume_item", FD + "mushroom_rice", 2.4, 1, 4, hint="Cook it in a Cooking Pot."),
        T("consume_item", FD + "cabbage_rolls", 2.2, 1, 4, hint="Cook it in a Cooking Pot."),
        T("consume_item", FD + "dumplings", 2.2, 1, 4, hint="Cook it in a Cooking Pot."),
        T("consume_item", FD + "squid_ink_pasta", 3.2, 1, 3, hint="Cook it in a Cooking Pot."),
        T("consume_item", FD + "cooked_rice", 1.4, 1, 6, hint="Cook rice in a Cooking Pot."),
        T("consume_item", FD + "steak_and_potatoes", 2.2, 1, 4),
        T("consume_item", FD + "roasted_mutton_chops", 2.4, 1, 4),
        T("consume_item", FD + "grilled_salmon", 2.6, 1, 4),
        T("craft_item", FD + "steak_and_potatoes", 2.1, 1, 4, hint="Steak, a baked potato, onion and cooked rice."),
        T("craft_item", FD + "stuffed_potato", 1.8, 1, 4),
    ]),
    E("fd_drinks", "fd_drinks", 1, [
        T("consume_item", FD + "apple_cider", 1.6, 1, 4, hints=["drink"], hint="Brew it in a Cooking Pot."),
        T("consume_item", FD + "hot_cocoa", 1.6, 1, 4, hints=["drink"], hint="Brew it in a Cooking Pot."),
        T("consume_item", FD + "glow_berry_custard", 2.0, 1, 4, hint="Cook it in a Cooking Pot."),
        T("craft_item", FD + "melon_juice", 0.8, 1, 6, hint="Melon slices, sugar and a glass bottle."),
        T("consume_item", FD + "melon_juice", 0.9, 1, 6, hints=["drink"]),
        T("craft_item", FD + "milk_bottle", 0.2, 4, 16, stack=16, hint="Pour a milk bucket into glass bottles."),
    ]),
    E("fd_feasts", "fd_feasts", 1, [
        T("craft_item", FD + "roast_chicken_block", 5.0, 1, 1, minDifficulty="hard", hint="A feast to share."),
        T("craft_item", FD + "shepherds_pie_block", 6.0, 1, 1, minDifficulty="hard", hint="A feast to share."),
        T("craft_item", FD + "honey_glazed_ham_block", 7.0, 1, 1, minDifficulty="hard", hint="A feast to share."),
        T("craft_item", FD + "rice_roll_medley_block", 5.5, 1, 1, minDifficulty="hard", hint="A feast to share."),
    ]),
    E("fd_bakery", "baking", 0, [
        T("craft_item", FD + "wheat_dough", 0.2, 6, 48, hint="Wheat with an egg or water."),
        T("smelt_item", "minecraft:bread", 0.35, 4, 32, hint="Bake wheat dough in a furnace.", hints=["cook"]),
        T("craft_item", FD + "pie_crust", 0.5, 2, 8),
    ], notes="Baking bread from dough only exists with Farmer's Delight, so this smelt target lives in the profile."),
    E("fd_compost", "fd_soil", 0, [
        T("craft_item", FD + "organic_compost", 2.4, 1, 4, hint="Dirt, rotten flesh, straw and bone meal."),
    ]),
]

FD_REWARDS = [
    {"id": FD + "tomato", "value": 0.15, "tier": 0, "max": 32, "family": "fd_crops"},
    {"id": FD + "cabbage", "value": 0.15, "tier": 0, "max": 32, "family": "fd_crops"},
    {"id": FD + "onion", "value": 0.15, "tier": 0, "max": 32, "family": "fd_crops"},
    {"id": FD + "rice", "value": 0.12, "tier": 0, "max": 32, "family": "fd_crops"},
    {"id": FD + "rope", "value": 0.08, "tier": 0, "max": 32, "family": "fd_straw"},
    {"id": FD + "flint_knife", "value": 0.6, "tier": 0, "max": 1, "stack": 1, "family": "fd_tools"},
    {"id": FD + "iron_knife", "value": 1.0, "tier": 1, "max": 1, "stack": 1, "family": "fd_tools"},
    {"id": FD + "hamburger", "value": 2.6, "tier": 1, "max": 6, "family": "fd_meals"},
    {"id": FD + "bacon_sandwich", "value": 1.8, "tier": 1, "max": 8, "family": "fd_meals"},
    {"id": FD + "sweet_berry_cookie", "value": 0.15, "tier": 0, "max": 32, "family": "fd_sweets"},
    {"id": FD + "honey_cookie", "value": 0.2, "tier": 0, "max": 32, "family": "fd_sweets"},
    {"id": FD + "apple_pie", "value": 3.0, "tier": 1, "max": 2, "family": "fd_sweets"},
    {"id": FD + "beef_stew", "value": 2.2, "tier": 1, "max": 4, "stack": 16, "family": "fd_stews"},
    {"id": FD + "fried_rice", "value": 2.4, "tier": 1, "max": 4, "stack": 16, "family": "fd_plates"},
    {"id": FD + "hot_cocoa", "value": 1.6, "tier": 1, "max": 4, "stack": 16, "family": "fd_drinks"},
    {"id": FD + "cooking_pot", "value": 5.0, "tier": 1, "max": 1, "family": "fd_kitchen"},
]

FD_THEMES = [
    {"key": "fd_farmers_market", "names": ["Farmer's Market", "Market Day", "Fresh Produce"],
     "descriptions": ["The market needs fresh produce: {list}."],
     "minDifficulty": "normal",
     "slots": [{"types": ["collect_item"], "keys": ["fd_cabbage", "fd_tomato", "fd_onion", "fd_rice"]},
               {"types": ["collect_item"], "keys": ["fd_cabbage", "fd_tomato", "fd_onion", "fd_rice"]},
               {"types": ["collect_item"], "families": ["crops"], "optional": True}]},
    {"key": "fd_harvest_festival", "names": ["Harvest Festival", "Harvest Time"],
     "descriptions": ["Bring in the harvest! {list}."],
     "minDifficulty": "normal",
     "slots": [{"types": ["collect_item"], "families": ["crops"], "profiles": ["vanilla"]},
               {"types": ["collect_item"], "families": ["fd_crops"]}]},
    {"key": "fd_chefs_special", "names": ["Chef's Special", "Kitchen Rush", "Dinner Service"],
     "descriptions": ["Tonight's menu: {list}."],
     "minDifficulty": "normal",
     "slots": [{"types": ["craft_item"], "families": ["fd_meals", "fd_salads", "fd_breakfast"]},
               {"types": ["consume_item"], "families": ["fd_stews", "fd_plates", "fd_drinks"]}]},
    {"key": "fd_butcher", "names": ["Butcher's Order", "Meat Market"],
     "descriptions": ["The butcher needs help: {list}."],
     "minDifficulty": "normal",
     "slots": [{"types": ["collect_item"], "keys": ["fd_butchery", "cows", "pigs"]},
               {"types": ["smelt_item"], "keys": ["fd_butchery"]}]},
    {"key": "fd_kitchen_setup", "names": ["Kitchen Setup", "New Kitchen"],
     "descriptions": ["Every cook needs tools: {list}."],
     "slots": [{"types": ["craft_item"], "keys": ["fd_kitchen"]},
               {"types": ["craft_item", "place_block"], "keys": ["fd_knives", "fd_kitchen", "fd_cabinets"]}]},
    {"key": "fd_wild_forager", "names": ["Wild Forager", "Forager's Walk"],
     "descriptions": ["Search the wilds: {list}."],
     "minDifficulty": "normal",
     "slots": [{"types": ["mine_block"], "keys": ["fd_wild"]},
               {"types": ["mine_block"], "keys": ["fd_wild"]},
               {"types": ["collect_item"], "keys": ["fd_straw"], "optional": True}]},
    {"key": "fd_village_bakery", "names": ["Village Bakery", "Fresh Bread"],
     "descriptions": ["The bakery is short-handed: {list}."],
     "minDifficulty": "normal",
     "slots": [{"types": ["craft_item"], "keys": ["fd_bakery"]},
               {"types": ["smelt_item"], "keys": ["fd_bakery"]},
               {"types": ["craft_item"], "keys": ["baking", "fd_sweets"], "optional": True}]},
]

from mod_more import (FD_MORE, FD_MORE_THEMES, FD_MORE_REWARDS, CREATE_MORE, CREATE_MORE_THEMES,  # noqa: E402
                      CREATE_MORE_REWARDS)
FD_ENTRIES = FD_ENTRIES + FD_MORE
FD_THEMES = FD_THEMES + FD_MORE_THEMES
FD_REWARDS = FD_REWARDS + FD_MORE_REWARDS

FARMERS_DELIGHT = {
    "format": 1,
    "id": "farmersdelight",
    "name": "Farmer's Delight",
    "requiresMod": ["farmersdelight"],
    "minecraft": {"min": "1.18.2"},
    "entries": FD_ENTRIES,
    "rewards": {"items": FD_REWARDS},
    "themes": FD_THEMES,
}

C = "create:"
CREATE_ENTRIES = [
    E("create_zinc", "create_zinc", 1, [
        T("mine_block", C + "zinc_ore", 0.8, 4, 24),
        T("mine_block", C + "deepslate_zinc_ore", 0.9, 4, 24, hints=["deep"]),
        T("collect_item", C + "raw_zinc", 0.8, 4, 24, hint="Zinc ore drops raw zinc."),
        T("smelt_item", C + "zinc_ingot", 0.85, 4, 32, tag="zinc_ingots", hint="Smelt raw zinc in a furnace."),
        T("craft_item", C + "zinc_block", 7.5, 1, 2, minDifficulty="hard"),
    ], tool="iron", hints=["caves"], exclusions=["collect_item create:zinc_ore (drops raw zinc, needs Silk Touch)"]),
    E("create_layers", "create_stone", 1, [
        T("mine_block", C + "asurine", 0.09, 16, 64),
        T("mine_block", C + "crimsite", 0.09, 16, 64),
        T("mine_block", C + "ochrum", 0.09, 16, 64),
        T("mine_block", C + "veridium", 0.09, 16, 64),
        T("mine_block", C + "limestone", 0.09, 16, 64),
        T("mine_block", C + "scoria", 0.09, 16, 64),
    ], tool="wood", hints=["layers"]),
    E("create_scorchia", "create_stone", 3, [T("mine_block", C + "scorchia", 0.09, 16, 64)], dim=NE, tool="wood",
      hints=["layers"]),
    E("create_masonry", "create_masonry", 1, [
        T("place_block", C + "cut_limestone", 0.14, 16, 64, hint="Cut limestone on a stonecutter, then build."),
        T("place_block", C + "cut_asurine", 0.14, 16, 64, hint="Cut asurine on a stonecutter, then build."),
        T("place_block", C + "cut_crimsite", 0.14, 16, 64, hint="Cut crimsite on a stonecutter, then build."),
        T("craft_item", C + "cut_limestone", 0.11, 16, 64, hint="Cut limestone on a stonecutter."),
        T("craft_item", C + "cut_asurine", 0.11, 16, 64, hint="Cut asurine on a stonecutter."),
    ], hints=["layers"]),
    E("create_cut_stone", "create_masonry", 0, [
        T("craft_item", C + "cut_granite", 0.07, 16, 64, hint="Cut granite on a stonecutter."),
        T("craft_item", C + "cut_diorite", 0.07, 16, 64, hint="Cut diorite on a stonecutter."),
        T("craft_item", C + "cut_andesite", 0.07, 16, 64, hint="Cut andesite on a stonecutter."),
        T("craft_item", C + "cut_tuff", 0.08, 16, 64, tier=1, hint="Cut tuff on a stonecutter."),
    ], tool="wood", notes="Stonecutter outputs count as crafting since mod 0.3.3."),
    E("create_alloy", "create_alloy", 1, [
        T("craft_item", C + "andesite_alloy", 0.35, 8, 48, hint="Two andesite and two iron nuggets."),
        T("place_block", C + "andesite_casing", 0.6, 4, 16, hint="Use andesite alloy on stripped logs to make casings."),
        T("craft_item", C + "basin", 1.8, 1, 4, hint="Five andesite alloy."),
    ]),
    E("create_kinetics", "create_kinetics", 1, [
        T("craft_item", C + "shaft", 0.06, 16, 64, hint="Two andesite alloy make eight shafts."),
        T("craft_item", C + "cogwheel", 0.2, 8, 48, hint="A shaft and wooden planks."),
        T("craft_item", C + "large_cogwheel", 0.3, 4, 24),
        T("craft_item", C + "hand_crank", 0.5, 1, 4),
        T("craft_item", C + "gearbox", 1.8, 1, 4, hint="Four cogwheels and an andesite casing."),
        T("place_block", C + "shaft", 0.08, 16, 64),
        T("place_block", C + "cogwheel", 0.25, 8, 32),
    ]),
    E("create_power", "create_power", 1, [
        T("craft_item", C + "water_wheel", 1.0, 1, 4, hint="A shaft surrounded by planks."),
        T("craft_item", C + "large_water_wheel", 3.0, 1, 2),
        T("craft_item", C + "white_sail", 0.6, 2, 12, hint="Andesite alloy, sticks and wool."),
        T("craft_item", C + "millstone", 2.0, 1, 2, hint="A cogwheel, stone and an andesite casing."),
    ]),
    E("create_logistics", "create_logistics", 1, [
        T("craft_item", C + "belt_connector", 0.7, 2, 8, hint="Six dried kelp."),
        T("craft_item", C + "andesite_funnel", 0.5, 2, 12, hint="Andesite alloy and dried kelp."),
        T("craft_item", C + "andesite_tunnel", 0.8, 2, 8),
        T("craft_item", C + "depot", 1.2, 1, 4),
    ]),
    E("create_machines", "create_machines", 2, [
        T("craft_item", C + "mechanical_press", 6.0, 1, 1, hint="An iron block, a shaft and an andesite casing.",
          minDifficulty="normal"),
        T("craft_item", C + "mechanical_drill", 3.5, 1, 2, minDifficulty="normal"),
        T("craft_item", C + "mechanical_mixer", 5.0, 1, 1, hint="Needs a whisk made from iron sheets.",
          minDifficulty="normal"),
        T("craft_item", C + "encased_fan", 4.0, 1, 2, hint="Needs a propeller made from iron sheets.",
          minDifficulty="normal"),
        T("craft_item", C + "mechanical_saw", 4.0, 1, 1, minDifficulty="normal"),
    ], exclusions=["craft_item create:crushing_wheel (mechanical crafter only, not counted)"]),
    E("create_tools", "create_tools", 2, [
        T("craft_item", C + "wrench", 3.5, 1, 1, hint="Needs golden sheets from a press.", minDifficulty="normal"),
        T("craft_item", C + "goggles", 3.0, 1, 1, hint="Needs a golden sheet from a press.", minDifficulty="normal"),
        T("craft_item", C + "super_glue", 3.0, 1, 2, minDifficulty="normal"),
    ]),
    E("create_seats", "create_decor", 0, [
        T("craft_item", C + "white_seat", 0.4, 1, 8, hint="White wool on a wooden slab."),
        T("place_block", C + "white_seat", 0.45, 1, 8),
    ]),
    E("create_sweets", "create_sweets", 2, [
        T("consume_item", C + "bar_of_chocolate", 4.0, 1, 4, minDifficulty="hard", hint="Made with a mixer and a press."),
        T("consume_item", C + "honeyed_apple", 4.0, 1, 4, minDifficulty="hard", hint="Fill apples with honey using a spout."),
        T("consume_item", C + "builders_tea", 4.5, 1, 3, minDifficulty="hard", hints=["drink"], hint="Brewed by a mixer, bottled by a spout."),
        T("consume_item", C + "sweet_roll", 4.0, 1, 4, minDifficulty="hard", hint="Fill bread with milk using a spout."),
        T("consume_item", C + "chocolate_glazed_berries", 4.0, 1, 4, minDifficulty="hard"),
    ], exclusions=["craft_item create:bar_of_chocolate (made by mixing and compacting, not a crafting grid)"]),
    E("create_rose_quartz", "create_quartz", 3, [
        T("craft_item", C + "rose_quartz", 1.2, 2, 16, hint="Nether quartz and eight redstone dust."),
    ], requires=["nether"], exclusions=["craft_item create:polished_rose_quartz (sandpaper polishing is not crafting)"]),
]

CREATE_REWARDS = [
    {"id": C + "andesite_alloy", "value": 0.35, "tier": 1, "max": 32, "family": "create_alloy"},
    {"id": C + "zinc_ingot", "value": 0.9, "tier": 1, "max": 32, "family": "create_zinc"},
    {"id": C + "shaft", "value": 0.08, "tier": 1, "max": 32, "family": "create_kinetics"},
    {"id": C + "cogwheel", "value": 0.25, "tier": 1, "max": 32, "family": "create_kinetics"},
    {"id": C + "large_cogwheel", "value": 0.35, "tier": 1, "max": 16, "family": "create_kinetics"},
    {"id": C + "belt_connector", "value": 0.7, "tier": 1, "max": 16, "family": "create_logistics"},
    {"id": C + "experience_nugget", "value": 0.3, "tier": 1, "max": 32, "family": "create_misc"},
    {"id": C + "brass_ingot", "value": 2.0, "tier": 2, "max": 16, "family": "create_brass"},
    {"id": C + "super_glue", "value": 3.0, "tier": 2, "max": 1, "stack": 1, "family": "create_tools"},
    {"id": C + "wrench", "value": 3.5, "tier": 2, "max": 1, "stack": 1, "family": "create_tools"},
    {"id": C + "goggles", "value": 3.0, "tier": 2, "max": 1, "stack": 1, "family": "create_tools"},
    {"id": C + "builders_tea", "value": 4.0, "tier": 2, "max": 4, "stack": 16, "family": "create_sweets"},
    {"id": C + "bar_of_chocolate", "value": 3.0, "tier": 2, "max": 8, "family": "create_sweets"},
    {"id": C + "honeyed_apple", "value": 3.0, "tier": 2, "max": 8, "family": "create_sweets"},
]

CREATE_THEMES = [
    {"key": "create_zinc_rush", "names": ["Zinc Rush", "Zinc Supply", "Zinc Prospector"],
     "descriptions": ["The workshop runs on zinc: {list}."],
     "minDifficulty": "normal",
     "slots": [{"types": ["mine_block", "collect_item"], "keys": ["create_zinc"]},
               {"types": ["smelt_item"], "keys": ["create_zinc"]}]},
    {"key": "create_first_steps", "names": ["Engineer's Start", "First Contraption", "Gear Up"],
     "descriptions": ["Every machine starts small: {list}."],
     "minDifficulty": "normal",
     "slots": [{"types": ["craft_item"], "keys": ["create_alloy"]},
               {"types": ["craft_item"], "keys": ["create_kinetics"]},
               {"types": ["craft_item"], "keys": ["create_power"], "optional": True}]},
    {"key": "create_water_mill", "names": ["Water Mill", "Mill Works"],
     "descriptions": ["Build a mill by the river: {list}."],
     "minDifficulty": "normal",
     "slots": [{"types": ["craft_item"], "keys": ["create_power"]},
               {"types": ["place_block"], "keys": ["create_kinetics", "create_alloy"]}]},
    {"key": "create_stone_layers", "names": ["Stone Layers", "Deep Layers", "Rock Survey"],
     "descriptions": ["Survey the stone layers underground: {list}."],
     "minDifficulty": "normal",
     "slots": [{"types": ["mine_block"], "keys": ["create_layers"]},
               {"types": ["mine_block"], "keys": ["create_layers"]}]},
    {"key": "create_metalworks", "names": ["Metalworks", "Iron and Zinc", "Foundry Order"],
     "descriptions": ["The foundry needs metal: {list}."],
     "minDifficulty": "normal",
     "slots": [{"types": ["smelt_item"], "keys": ["iron", "copper"], "profiles": ["vanilla"]},
               {"types": ["smelt_item", "collect_item"], "keys": ["create_zinc"]}]},
]

CREATE_ENTRIES = CREATE_ENTRIES + CREATE_MORE
CREATE_THEMES = CREATE_THEMES + CREATE_MORE_THEMES
CREATE_REWARDS = CREATE_REWARDS + CREATE_MORE_REWARDS

CREATE = {
    "format": 1,
    "id": "create",
    "name": "Create",
    "requiresMod": ["create"],
    "minecraft": {"min": "1.18.2"},
    "entries": CREATE_ENTRIES,
    "rewards": {"items": CREATE_REWARDS},
    "themes": CREATE_THEMES,
}
