package com.erikedits.justquests.client;

import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.objective.BlockMatcher;
import com.erikedits.justquests.data.objective.BreedAnimalObjective;
import com.erikedits.justquests.data.objective.CollectItemObjective;
import com.erikedits.justquests.data.objective.ConsumeItemObjective;
import com.erikedits.justquests.data.objective.CraftItemObjective;
import com.erikedits.justquests.data.objective.EnchantItemObjective;
import com.erikedits.justquests.data.objective.EntityMatcher;
import com.erikedits.justquests.data.objective.GainAdvancementObjective;
import com.erikedits.justquests.data.objective.ItemMatcher;
import com.erikedits.justquests.data.objective.KillMobObjective;
import com.erikedits.justquests.data.objective.MineBlockObjective;
import com.erikedits.justquests.data.objective.PlaceBlockObjective;
import com.erikedits.justquests.data.objective.QuestObjective;
import com.erikedits.justquests.data.objective.ReachLevelObjective;
import com.erikedits.justquests.data.objective.ReachLocationObjective;
import com.erikedits.justquests.data.objective.SmeltItemObjective;
import com.erikedits.justquests.data.objective.TameAnimalObjective;
import com.erikedits.justquests.data.objective.UseItemObjective;
import com.erikedits.justquests.data.objective.VisitDimensionObjective;
import com.erikedits.justquests.network.ClientQuestData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What the quest book and the HUD show for a quest: its item icon (the quest's "icon" field, else
 * picked from the first objective), short objective labels and the category icons.
 */
public final class QuestIcons {
    /** Categories with their own pixel icon (textures/gui/cat_<name>.png); others use "custom". */
    private static final Set<String> CATEGORY_ICONS = Set.of("gathering", "farming", "combat", "survival", "daily",
        "building", "crafting", "exploration", "challenges", "generated", "custom");

    private static final Map<Identifier, ItemStack> CACHE = new HashMap<>();
    private static int cacheVersion = -1;

    private QuestIcons() {}

    /** The item drawn for a quest; cached until the quest list changes. */
    public static ItemStack of(Identifier id, Quest quest) {
        int v = ClientQuestData.version();
        if (v != cacheVersion) {
            CACHE.clear();
            cacheVersion = v;
        }
        return CACHE.computeIfAbsent(id, k -> compute(quest));
    }

    private static ItemStack compute(Quest quest) {
        if (quest.icon().isPresent()) {
            Item item = BuiltInRegistries.ITEM.getOptional(quest.icon().get()).orElse(Items.AIR);
            if (item != Items.AIR) return new ItemStack(item);
        }
        for (QuestObjective o : quest.objectives()) {
            Item item = fromObjective(o);
            if (item != null && item != Items.AIR) return new ItemStack(item);
        }
        return new ItemStack(Items.BOOK);
    }

    private static Item fromObjective(QuestObjective o) {
        if (o instanceof CollectItemObjective c) return first(c.item());
        if (o instanceof CraftItemObjective c) return first(c.item());
        if (o instanceof SmeltItemObjective s) return first(s.item());
        if (o instanceof ConsumeItemObjective c) return first(c.item());
        if (o instanceof UseItemObjective u) return first(u.item());
        if (o instanceof EnchantItemObjective e) return e.item().map(QuestIcons::first).orElse(Items.ENCHANTED_BOOK);
        if (o instanceof MineBlockObjective m) return block(m.block());
        if (o instanceof PlaceBlockObjective p) return block(p.block());
        if (o instanceof KillMobObjective k) return egg(k.entity(), Items.IRON_SWORD);
        if (o instanceof TameAnimalObjective t) return egg(t.entity(), Items.LEAD);
        if (o instanceof BreedAnimalObjective b) return egg(b.entity(), Items.WHEAT);
        if (o instanceof ReachLevelObjective) return Items.EXPERIENCE_BOTTLE;
        if (o instanceof ReachLocationObjective) return Items.COMPASS;
        if (o instanceof VisitDimensionObjective v) {
            String dim = v.dimension().toString();
            return dim.equals("minecraft:the_nether") ? Items.NETHERRACK
                : dim.equals("minecraft:the_end") ? Items.END_STONE
                : dim.equals("minecraft:overworld") ? Items.GRASS_BLOCK : Items.ENDER_EYE;
        }
        if (o instanceof GainAdvancementObjective) return Items.KNOWLEDGE_BOOK;
        return null;
    }

    private static Item first(ItemMatcher m) {
        List<Item> items = m.items();
        return items.isEmpty() ? null : items.get(0);
    }

    private static Item block(BlockMatcher m) {
        if (m instanceof BlockMatcher.Single s) return s.block().asItem();
        for (Block b : BuiltInRegistries.BLOCK) {
            if (m.matches(b) && b.asItem() != Items.AIR) return b.asItem();
        }
        return null;
    }

    private static Item egg(EntityMatcher m, Item fallback) {
        if (m instanceof EntityMatcher.Single s) {
            SpawnEggItem egg = SpawnEggItem.byId(s.type());
            return egg != null ? egg : fallback;
        }
        for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
            if (m.matches(t)) {
                SpawnEggItem egg = SpawnEggItem.byId(t);
                if (egg != null) return egg;
            }
        }
        return fallback;
    }

    /** "Mine Iron Ore": the objective without its count (the book and the HUD show "5/8" in front). */
    public static Component label(QuestObjective o) {
        if (o instanceof CollectItemObjective c) return verb("Collect", c.item().name());
        if (o instanceof CraftItemObjective c) return verb("Craft", c.item().name());
        if (o instanceof SmeltItemObjective s) return verb("Smelt", s.item().name());
        if (o instanceof ConsumeItemObjective c) return verb("Consume", c.item().name());
        if (o instanceof UseItemObjective u) return verb("Use", u.item().name());
        if (o instanceof EnchantItemObjective e) {
            return e.item().isPresent() ? verb("Enchant", e.item().get().name()) : Component.literal("Enchant any item");
        }
        if (o instanceof MineBlockObjective m) return verb("Mine", m.block().name());
        if (o instanceof PlaceBlockObjective p) return verb("Place", p.block().name());
        if (o instanceof KillMobObjective k) return verb("Kill", k.entity().name());
        if (o instanceof TameAnimalObjective t) return verb("Tame", t.entity().name());
        if (o instanceof BreedAnimalObjective b) return verb("Breed", b.entity().name());
        return o.display();
    }

    /** Verb + name; a tag ("#minecraft:logs") reads as "any logs". */
    private static Component verb(String verb, Component name) {
        String s = name.getString();
        if (s.startsWith("#")) {
            return Component.literal(verb + " any " + s.substring(s.indexOf(':') + 1).replace('_', ' ').replace('/', ' '));
        }
        return Component.literal(verb + " ").append(name);
    }

    /** Texture name of a category's pixel icon. */
    public static String categoryIcon(String category) {
        String c = category.toLowerCase(java.util.Locale.ROOT);
        return "cat_" + (CATEGORY_ICONS.contains(c) ? c : "custom");
    }

    /** "farming" -> "Farming", "my_pack" -> "My pack". */
    public static String categoryName(String category) {
        if (category.isEmpty()) return category;
        String s = category.replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
