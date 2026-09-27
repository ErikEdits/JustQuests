"""Second batch of mod content (extension phase), verified with verify_mods.py."""
from catalog_dsl import T, E

FD = "farmersdelight:"
C = "create:"

FD_MORE = [
    E("fd_rolls", "fd_seafood", 1, [
        T("craft_item", FD + "salmon_roll", 1.6, 1, 6, hint="Salmon slices and cooked rice."),
        T("craft_item", FD + "cod_roll", 1.3, 1, 6, hint="Cod slices and cooked rice."),
        T("consume_item", FD + "kelp_roll_slice", 0.6, 2, 8),
    ]),
    E("fd_snacks", "fd_snacks", 0, [
        T("craft_item", FD + "barbecue_stick", 1.2, 1, 6, hint="Tomato, onion and meat on a stick."),
        T("consume_item", FD + "barbecue_stick", 1.4, 1, 4),
        T("consume_item", FD + "honey_cookie", 0.35, 4, 16, hints=["bees"]),
        T("consume_item", FD + "egg_sandwich", 2.0, 1, 4),
        T("consume_item", FD + "chicken_sandwich", 2.0, 1, 4),
        T("consume_item", FD + "mutton_wrap", 2.2, 1, 4),
        T("consume_item", FD + "stuffed_potato", 2.0, 1, 4),
    ]),
    E("fd_feast_servings", "fd_feasts", 1, [
        T("consume_item", FD + "roast_chicken", 5.5, 1, 2, minDifficulty="hard", hint="Place a roast chicken and serve it into a bowl."),
        T("consume_item", FD + "shepherds_pie", 6.5, 1, 2, minDifficulty="hard", hint="Place a shepherd's pie and serve it into a bowl."),
        T("consume_item", FD + "honey_glazed_ham", 7.5, 1, 2, minDifficulty="hard", hint="Place a glazed ham and serve it into a bowl."),
    ]),
    E("fd_baskets", "fd_furniture", 0, [
        T("craft_item", FD + "wooden_basket", 0.5, 1, 4),
        T("craft_item", FD + "bamboo_basket", 0.6, 1, 4, hints=["jungle"]),
        T("craft_item", FD + "birch_cabinet", 0.9, 1, 6),
        T("craft_item", FD + "jungle_cabinet", 1.0, 1, 6, hints=["jungle"]),
        T("craft_item", FD + "acacia_cabinet", 1.0, 1, 6, hints=["savanna"]),
        T("craft_item", FD + "dark_oak_cabinet", 1.0, 1, 6, hints=["dark_forest"]),
    ]),
    E("fd_straw_crafts", "fd_straw", 0, [
        T("craft_item", FD + "safety_net", 0.9, 1, 6, hint="Made from rope."),
        T("craft_item", FD + "canvas_rug", 0.6, 2, 8),
        T("craft_item", FD + "tatami", 0.8, 2, 8),
        T("place_block", FD + "straw_bale", 1.15, 1, 6),
    ], tool="knife"),
    E("fd_compost_place", "fd_soil", 0, [
        T("place_block", FD + "organic_compost", 2.5, 1, 4, hint="It slowly turns into rich soil."),
    ]),
]

FD_MORE_THEMES = [
    {"key": "fd_breakfast_club", "names": ["Breakfast Club", "Morning Menu"],
     "descriptions": ["Breakfast is served: {list}."], "minDifficulty": "normal",
     "slots": [{"types": ["smelt_item"], "keys": ["fd_breakfast", "fd_butchery"]},
               {"types": ["craft_item", "consume_item"], "keys": ["fd_breakfast", "fd_snacks"]}]},
    {"key": "fd_picnic", "names": ["Picnic Basket", "Picnic Day"],
     "descriptions": ["Pack the picnic basket: {list}."], "minDifficulty": "normal",
     "slots": [{"types": ["craft_item"], "families": ["fd_salads", "fd_snacks"]},
               {"types": ["craft_item"], "families": ["fd_sweets"]}]},
    {"key": "fd_rope_maker", "names": ["Rope Maker", "Straw Work"],
     "descriptions": ["Twist straw into something useful: {list}."], "minDifficulty": "normal",
     "slots": [{"types": ["collect_item"], "keys": ["fd_straw"]},
               {"types": ["craft_item", "place_block"], "keys": ["fd_straw", "fd_straw_crafts"]}]},
    {"key": "fd_crate_stock", "names": ["Crate Stock", "Pantry Stock"],
     "descriptions": ["Fill the pantry: {list}."], "minDifficulty": "normal",
     "slots": [{"types": ["craft_item"], "keys": ["fd_cabbage", "fd_tomato", "fd_onion"]},
               {"types": ["craft_item"], "keys": ["fd_cabbage", "fd_tomato", "fd_onion", "fd_rice"]}]},
    {"key": "fd_fishmonger", "names": ["Fishmonger", "Fish Market"],
     "descriptions": ["The fishmonger needs stock: {list}."], "minDifficulty": "normal",
     "slots": [{"types": ["collect_item"], "families": ["fishing"], "profiles": ["vanilla"]},
               {"types": ["smelt_item", "craft_item"], "keys": ["fd_fish", "fd_rolls"]}]},
]

CREATE_MORE = [
    E("create_contraptions", "create_contraptions", 1, [
        T("craft_item", C + "linear_chassis", 0.6, 2, 12, hint="Andesite alloy and logs."),
        T("craft_item", C + "radial_chassis", 0.6, 2, 12),
        T("craft_item", C + "mechanical_bearing", 1.8, 1, 3),
        T("craft_item", C + "windmill_bearing", 2.2, 1, 2),
        T("craft_item", C + "piston_extension_pole", 0.3, 4, 16, hint="Andesite alloy and planks."),
        T("craft_item", C + "mechanical_piston", 3.2, 1, 2, minDifficulty="normal"),
        T("craft_item", C + "sticker", 1.4, 1, 4),
        T("place_block", C + "linear_chassis", 0.65, 2, 12),
    ]),
    E("create_small_parts", "create_kinetics", 1, [
        T("craft_item", C + "encased_chain_drive", 1.2, 1, 6),
        T("craft_item", C + "turntable", 0.5, 1, 4),
        T("craft_item", C + "wooden_bracket", 0.3, 2, 12),
        T("craft_item", C + "cart_assembler", 1.8, 1, 3),
    ]),
    E("create_alloy_blocks", "create_alloy", 1, [
        T("craft_item", C + "andesite_alloy_block", 3.1, 1, 4, hint="Nine andesite alloy."),
        T("place_block", C + "andesite_alloy_block", 3.2, 1, 4),
    ]),
    E("create_gadgets", "create_tools", 2, [
        T("craft_item", C + "cuckoo_clock", 5.5, 1, 1, minDifficulty="normal", hint="A clock in an andesite casing."),
        T("craft_item", C + "item_vault", 4.0, 1, 2, minDifficulty="hard"),
        T("craft_item", C + "fluid_tank", 3.5, 1, 2, minDifficulty="normal"),
        T("craft_item", C + "empty_blaze_burner", 4.5, 1, 1, tier=3, minDifficulty="hard", hint="Iron sheets and netherrack."),
    ]),
    E("create_polished", "create_masonry", 1, [
        T("place_block", C + "polished_cut_limestone", 0.16, 16, 64, hint="Polish limestone on a stonecutter, then build."),
        T("place_block", C + "polished_cut_ochrum", 0.16, 16, 64),
        T("place_block", C + "polished_cut_veridium", 0.16, 16, 64),
    ], hints=["layers"]),
]

CREATE_MORE_THEMES = [
    {"key": "create_logistics_line", "names": ["Logistics Line", "Conveyor Setup"],
     "descriptions": ["Move items automatically: {list}."], "minDifficulty": "normal",
     "slots": [{"types": ["craft_item"], "keys": ["create_logistics"]},
               {"types": ["craft_item"], "keys": ["create_logistics", "create_kinetics"]}]},
    {"key": "create_contraption_crew", "names": ["Contraption Crew", "Moving Parts"],
     "descriptions": ["Build something that moves: {list}."], "minDifficulty": "normal",
     "slots": [{"types": ["craft_item"], "keys": ["create_contraptions"]},
               {"types": ["craft_item", "place_block"], "keys": ["create_contraptions", "create_small_parts"]}]},
    {"key": "create_masonry_works", "names": ["Layered Masonry", "Stone Workshop"],
     "descriptions": ["Decorate with Create stone: {list}."], "minDifficulty": "normal",
     "slots": [{"types": ["mine_block"], "keys": ["create_layers"]},
               {"types": ["place_block"], "keys": ["create_masonry", "create_polished"]}]},
    {"key": "create_andesite_age", "names": ["Andesite Age", "Alloy Foundry"],
     "descriptions": ["Everything starts with andesite: {list}."], "minDifficulty": "normal",
     "slots": [{"types": ["mine_block"], "keys": ["andesite"], "profiles": ["vanilla"]},
               {"types": ["craft_item"], "keys": ["create_alloy", "create_alloy_blocks"]}]},
    {"key": "create_engineers_kit", "names": ["Engineer's Kit", "Workshop Tools"],
     "descriptions": ["Every engineer needs gear: {list}."], "minDifficulty": "hard",
     "slots": [{"types": ["craft_item"], "keys": ["create_tools", "create_gadgets"]},
               {"types": ["craft_item"], "keys": ["create_machines"]}]},
]

FD_MORE_REWARDS = [
    {"id": FD + "carrot_crate", "value": 1.5, "tier": 0, "max": 4, "family": "fd_crops"},
    {"id": FD + "rope", "value": 0.08, "tier": 0, "max": 32, "family": "fd_straw"},
    {"id": FD + "barbecue_stick", "value": 1.3, "tier": 0, "max": 8, "family": "fd_snacks"},
    {"id": FD + "honey_glazed_ham", "value": 7.0, "tier": 1, "max": 2, "stack": 16, "family": "fd_feasts",
     "minDifficulty": "hard"},
]
CREATE_MORE_REWARDS = [
    {"id": C + "linear_chassis", "value": 0.6, "tier": 1, "max": 16, "family": "create_contraptions"},
    {"id": C + "andesite_alloy_block", "value": 3.2, "tier": 1, "max": 4, "family": "create_alloy"},
    {"id": C + "zinc_block", "value": 7.6, "tier": 1, "max": 2, "family": "create_zinc", "minDifficulty": "normal"},
]
