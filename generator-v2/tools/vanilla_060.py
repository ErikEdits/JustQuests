"""0.6.0 catalog entries: the new objective types (fishing, trading, brewing, deliveries, biomes, structures)."""
from catalog_dsl import T, E

NE = "minecraft:the_nether"
EN = "minecraft:the_end"
SUBMIT = "Hand them in with the quest book or /quest submit."

V060 = [
    # ------------------------------------------------------------ fishing with the rod (fish_item)
    E("fishing_catch", "fishing", 0, [
        T("fish_item", "minecraft:cod", 0.6, 3, 12, name="Raw Cod", hint="Cast a fishing rod into water."),
        T("fish_item", "minecraft:salmon", 1.2, 2, 8, name="Raw Salmon", hints=["river"], hint="Cast a fishing rod into water."),
        T("fish_item", "minecraft:tropical_fish", 4.0, 1, 3, minDifficulty="normal", hints=["rare"], name="Tropical Fish",
          plural="Tropical Fish"),
        T("fish_item", "minecraft:pufferfish", 3.0, 1, 3, minDifficulty="normal", hints=["rare"], name="Pufferfish",
          plural="Pufferfish"),
    ], tool="fishing_rod"),
    # ------------------------------------------------------------ trading with villagers (trade_villager)
    E("trading", "villagers", 1, [
        T("trade_villager", "minecraft:emerald", 1.0, 4, 16, hints=["village"], hint="Sell crops, paper or coal to villagers."),
        T("trade_villager", "minecraft:bread", 0.35, 6, 18, profession="minecraft:farmer", hints=["village"],
          hint="Farmers sell bread for emeralds."),
        T("trade_villager", "minecraft:arrow", 0.06, 16, 64, profession="minecraft:fletcher", hints=["village"],
          hint="Fletchers sell arrows for emeralds."),
        T("trade_villager", "minecraft:redstone", 0.5, 2, 8, profession="minecraft:cleric", hints=["village"],
          name="Redstone Dust", plural="Redstone Dust",
          minDifficulty="normal", hint="Clerics sell redstone for emeralds."),
    ], hints=["village"]),
    # ------------------------------------------------------------ brewing (brew_potion), needs the Nether
    E("brewing_stand_work", "brewing", 3, [
        T("brew_potion", "minecraft:swiftness", 3.0, 1, 3, name="Potion of Swiftness", plural="Potions of Swiftness",
          hint="Brew an Awkward Potion with sugar."),
        T("brew_potion", "minecraft:night_vision", 3.0, 1, 3, name="Potion of Night Vision",
          plural="Potions of Night Vision", hint="Brew an Awkward Potion with a golden carrot."),
        T("brew_potion", "minecraft:fire_resistance", 3.5, 1, 3, name="Potion of Fire Resistance",
          plural="Potions of Fire Resistance", hint="Brew an Awkward Potion with magma cream."),
        T("brew_potion", "minecraft:strength", 3.5, 1, 3, name="Potion of Strength", plural="Potions of Strength",
          hint="Brew an Awkward Potion with blaze powder."),
        T("brew_potion", "minecraft:water_breathing", 4.0, 1, 2, name="Potion of Water Breathing",
          plural="Potions of Water Breathing", hints=["ocean"], hint="Brew an Awkward Potion with a pufferfish."),
    ], requires=["nether"], notes="brew_potion counts potions taken out of a brewing stand; long and strong forms count too."),
    # ------------------------------------------------------------ deliveries (submit_item)
    E("deliveries", "deliveries", 0, [
        T("submit_item", "minecraft:wheat", 0.3, 16, 48, hints=["farm"], hint=SUBMIT),
        T("submit_item", "minecraft:carrot", 0.3, 16, 48, hints=["farm"], hint=SUBMIT),
        T("submit_item", "minecraft:potato", 0.3, 16, 48, hints=["farm"], hint=SUBMIT),
        T("submit_item", "minecraft:oak_log", 0.12, 16, 64, hint=SUBMIT),
        T("submit_item", "minecraft:cobblestone", 0.05, 32, 128, tool="wood", hint=SUBMIT),
        T("submit_item", "minecraft:coal", 0.3, 8, 32, tool="wood", hint=SUBMIT),
        T("submit_item", "minecraft:bread", 0.5, 8, 24, hint=SUBMIT),
        T("submit_item", "minecraft:leather", 0.8, 4, 16, hint=SUBMIT),
        T("submit_item", "minecraft:iron_ingot", 1.2, 4, 16, tier=1, tool="stone", hint=SUBMIT),
    ]),
    # ------------------------------------------------------------ biomes (find_biome)
    E("biomes", "exploration", 0, [
        T("find_biome", "minecraft:desert", 12, name="Desert"),
        T("find_biome", "#minecraft:is_jungle", 18, name="Jungle"),
        T("find_biome", "#minecraft:is_savanna", 12, name="Savanna"),
        T("find_biome", "minecraft:swamp", 10, name="Swamp"),
        T("find_biome", "minecraft:dark_forest", 12, name="Dark Forest"),
        T("find_biome", "minecraft:flower_forest", 18, name="Flower Forest"),
        T("find_biome", "#minecraft:is_taiga", 10, name="Taiga"),
        T("find_biome", "#minecraft:is_badlands", 25, name="Badlands Biome", tier=1),
        T("find_biome", "minecraft:ice_spikes", 30, name="Ice Spikes Biome", tier=1),
        T("find_biome", "minecraft:cherry_grove", 25, name="Cherry Grove", since="1.20"),
        T("find_biome", "minecraft:mangrove_swamp", 25, name="Mangrove Swamp", since="1.19"),
        T("find_biome", "minecraft:mushroom_fields", 40, name="Mushroom Island", tier=2, minDifficulty="normal"),
    ]),
    E("nether_biomes", "nether_travel", 3, [
        T("find_biome", "minecraft:crimson_forest", 15, name="Crimson Forest"),
        T("find_biome", "minecraft:warped_forest", 15, name="Warped Forest"),
        T("find_biome", "minecraft:soul_sand_valley", 15, name="Soul Sand Valley"),
        T("find_biome", "minecraft:basalt_deltas", 20, name="Basalt Delta"),
    ], dim=NE, requires=["nether"]),
    # ------------------------------------------------------------ structures (find_structure)
    E("landmarks", "landmarks", 0, [
        T("find_structure", "#minecraft:village", 15, name="Village"),
        T("find_structure", "#minecraft:ruined_portal", 12, name="Ruined Portal"),
        T("find_structure", "#minecraft:shipwreck", 20, name="Shipwreck", hints=["ocean"]),
        T("find_structure", "minecraft:pillager_outpost", 25, name="Pillager Outpost", tier=1, minDifficulty="normal"),
        T("find_structure", "minecraft:desert_pyramid", 25, name="Desert Temple", tier=1, hints=["desert"]),
        T("find_structure", "minecraft:jungle_pyramid", 30, name="Jungle Temple", tier=1, hints=["jungle"]),
        T("find_structure", "minecraft:swamp_hut", 30, name="Swamp Hut", tier=1, hints=["swamp"]),
        T("find_structure", "minecraft:igloo", 35, name="Igloo", tier=1, hints=["snowy"]),
        T("find_structure", "minecraft:trail_ruins", 35, name="Trail Ruins", tier=1, since="1.20"),
        T("find_structure", "minecraft:trial_chambers", 30, name="Trial Chambers", tier=1, since="1.21", hints=["deep"]),
        T("find_structure", "minecraft:monument", 40, name="Ocean Monument", tier=2, minDifficulty="normal", hints=["ocean"]),
        T("find_structure", "minecraft:ancient_city", 45, name="Ancient City", tier=2, minDifficulty="hard", since="1.19",
          hints=["deep_dark"]),
        T("find_structure", "minecraft:stronghold", 45, name="Stronghold", tier=2, minDifficulty="hard"),
    ]),
    E("nether_landmarks", "nether_travel", 3, [
        T("find_structure", "minecraft:fortress", 20, name="Nether Fortress"),
        T("find_structure", "minecraft:bastion_remnant", 25, name="Bastion Remnant", minDifficulty="normal"),
    ], dim=NE, requires=["nether"]),
    E("end_landmarks", "end_travel", 4, [
        T("find_structure", "minecraft:end_city", 30, name="End City", minDifficulty="hard"),
    ], dim=EN, requires=["end"]),
]

FD_060 = [
    # Farmer's Delight (#22): cutting board and cooking (cut_item, cook_item)
    E("fd_cutting", "fd_kitchen", 0, [
        T("cut_item", "farmersdelight:cabbage", 0.6, 3, 12, hint="Put it on a cutting board and use a knife."),
        T("cut_item", "minecraft:beef", 1.0, 2, 8, hint="Put it on a cutting board and use a knife."),
        T("cut_item", "minecraft:porkchop", 1.0, 2, 8, hint="Put it on a cutting board and use a knife."),
        T("cut_item", "minecraft:salmon", 1.4, 2, 6, hint="Put it on a cutting board and use a knife."),
        T("cut_item", "minecraft:pumpkin", 0.8, 2, 6, hint="Put it on a cutting board and use a knife."),
    ], tool="knife"),
    E("fd_cooking", "fd_stews", 1, [
        T("cook_item", "farmersdelight:beef_stew", 4.0, 1, 3, via="cooking_pot", hint="Cook it in a cooking pot on a heat source."),
        T("cook_item", "farmersdelight:vegetable_soup", 4.0, 1, 3, via="cooking_pot", hint="Cook it in a cooking pot on a heat source."),
        T("cook_item", "farmersdelight:pumpkin_soup", 4.0, 1, 3, via="cooking_pot", hint="Cook it in a cooking pot on a heat source."),
        T("cook_item", "farmersdelight:fried_egg", 0.8, 2, 8, via="skillet", hint="Fry it in a skillet."),
        T("cook_item", "minecraft:cooked_beef", 0.8, 3, 12, via="stove", hint="Cook it on a stove."),
        T("cook_item", "farmersdelight:roast_chicken", 3.0, 1, 2, via="feast", minDifficulty="normal",
          hint="Take a serving from a roast chicken feast with a bowl."),
    ]),
]
