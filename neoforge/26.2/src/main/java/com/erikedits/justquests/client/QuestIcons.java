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
import net.minecraft.client.resources.language.I18n;
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
            return SpawnEggItem.byId(s.type()).map(net.minecraft.core.Holder::value).orElse(fallback);
        }
        for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
            if (m.matches(t)) {
                var egg = SpawnEggItem.byId(t);
                if (egg.isPresent()) return egg.get().value();
            }
        }
        return fallback;
    }

    /** "Mine Iron Ore": the objective without its count (the book and the HUD show "5/8" in front). */
    public static Component label(QuestObjective o) {
        if (o instanceof CollectItemObjective c) return verb("collect", c.item().name());
        if (o instanceof CraftItemObjective c) return verb("craft", c.item().name());
        if (o instanceof SmeltItemObjective s) return verb("smelt", s.item().name());
        if (o instanceof ConsumeItemObjective c) return verb("consume", c.item().name());
        if (o instanceof UseItemObjective u) return verb("use", u.item().name());
        if (o instanceof EnchantItemObjective e) {
            return e.item().isPresent() ? verb("enchant", e.item().get().name()) : Component.translatable("justquests.goal.enchant_any");
        }
        if (o instanceof MineBlockObjective m) return verb("mine", m.block().name());
        if (o instanceof PlaceBlockObjective p) return verb("place", p.block().name());
        if (o instanceof KillMobObjective k) return verb("kill", k.entity().name());
        if (o instanceof TameAnimalObjective t) return verb("tame", t.entity().name());
        if (o instanceof BreedAnimalObjective b) return verb("breed", b.entity().name());
        return o.display();
    }

    /** "Mine %s" with the target's name (a tag's name already reads "any logs"). */
    private static Component verb(String goal, Component name) {
        return Component.translatable("justquests.goal." + goal, name);
    }

    /** Texture name of a category's pixel icon. */
    public static String categoryIcon(String category) {
        String c = category.toLowerCase(java.util.Locale.ROOT);
        return "cat_" + (CATEGORY_ICONS.contains(c) ? c : "custom");
    }

    /** The translated name of a bundled category, else the id made readable ("my_pack" -> "My pack"). */
    public static String categoryName(String category) {
        String key = "justquests.category." + category.toLowerCase(java.util.Locale.ROOT);
        if (net.minecraft.locale.Language.getInstance().has(key)) return I18n.get(key);
        if (category.isEmpty()) return category;
        String s = category.replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
