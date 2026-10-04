package com.erikedits.justquests.plugin.quest;

import com.erikedits.justquests.plugin.quest.Matchers.BlockMatcher;
import com.erikedits.justquests.plugin.quest.Matchers.EntityMatcher;
import com.erikedits.justquests.plugin.quest.Matchers.ItemMatcher;
import com.erikedits.justquests.plugin.text.Lang;
import com.erikedits.justquests.plugin.text.Text;
import com.google.gson.JsonObject;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Locale;

/** The quest goals, read from the same JSON as the mod ("type": "justquests:collect_item", ...). */
public sealed interface Objective {
    String typeId();

    int requiredCount();

    /** Plain English, for logs. */
    String label();

    /** The goal line in the player's language; names stay translation keys for the client. */
    BaseComponent display(String lang);

    /** An item to show for the goal in the quest book. */
    Material icon();

    static Objective parse(JsonObject o) {
        String type = Quest.string(o, "type");
        if (!type.contains(":")) type = "justquests:" + type;
        return switch (type) {
            case "justquests:collect_item" -> new Collect(ItemMatcher.parse(o.get("item")), count(o));
            case "justquests:craft_item" -> new Craft(ItemMatcher.parse(o.get("item")), count(o));
            case "justquests:smelt_item" -> new Smelt(ItemMatcher.parse(o.get("item")), count(o));
            case "justquests:consume_item" -> new Consume(ItemMatcher.parse(o.get("item")), count(o));
            case "justquests:use_item" -> new Use(ItemMatcher.parse(o.get("item")), count(o));
            case "justquests:enchant_item" -> new Enchant(o.has("item") ? ItemMatcher.parse(o.get("item")) : null, count(o));
            case "justquests:mine_block" -> new Mine(BlockMatcher.parse(Quest.string(o, "block")), count(o));
            case "justquests:place_block" -> new Place(BlockMatcher.parse(Quest.string(o, "block")), count(o));
            case "justquests:kill_mob" -> new Kill(EntityMatcher.parse(Quest.string(o, "entity")), count(o));
            case "justquests:tame_animal" -> new Tame(EntityMatcher.parse(Quest.string(o, "entity")), count(o));
            case "justquests:breed_animal" -> new Breed(EntityMatcher.parse(Quest.string(o, "entity")), count(o));
            case "justquests:gain_advancement" -> new Advancement(Matchers.ns(Quest.string(o, "advancement")));
            case "justquests:visit_dimension" -> new Dimension(Matchers.ns(Quest.string(o, "dimension")));
            case "justquests:reach_level" -> new Level(o.get("level").getAsInt());
            case "justquests:reach_location" -> new Location(
                o.has("dimension") ? Matchers.ns(o.get("dimension").getAsString()) : null,
                o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt(),
                o.has("radius") ? o.get("radius").getAsInt() : 4);
            default -> throw new IllegalArgumentException("unknown objective type: " + type);
        };
    }

    private static int count(JsonObject o) {
        if (!o.has("count")) throw new IllegalArgumentException("objective needs a \"count\"");
        return o.get("count").getAsInt();
    }

    private static BaseComponent goal(String lang, String key, int count, BaseComponent name) {
        return Text.tr(lang, key, count, name);
    }

    record Collect(ItemMatcher item, int count) implements Objective {
        public String typeId() { return "justquests:collect_item"; }
        public int requiredCount() { return count; }
        public String label() { return "Collect " + count + "x " + item.label(); }
        public BaseComponent display(String lang) { return goal(lang, "justquests.goal.collect_count", count, item.name(lang)); }
        public Material icon() { return item.icon(); }
    }

    record Craft(ItemMatcher item, int count) implements Objective {
        public String typeId() { return "justquests:craft_item"; }
        public int requiredCount() { return count; }
        public String label() { return "Craft " + count + "x " + item.label(); }
        public BaseComponent display(String lang) { return goal(lang, "justquests.goal.craft_count", count, item.name(lang)); }
        public Material icon() { return item.icon(); }
    }

    record Smelt(ItemMatcher item, int count) implements Objective {
        public String typeId() { return "justquests:smelt_item"; }
        public int requiredCount() { return count; }
        public String label() { return "Smelt " + count + "x " + item.label(); }
        public BaseComponent display(String lang) { return goal(lang, "justquests.goal.smelt_count", count, item.name(lang)); }
        public Material icon() { return item.icon(); }
    }

    record Consume(ItemMatcher item, int count) implements Objective {
        public String typeId() { return "justquests:consume_item"; }
        public int requiredCount() { return count; }
        public String label() { return "Consume " + count + "x " + item.label(); }
        public BaseComponent display(String lang) { return goal(lang, "justquests.goal.consume_count", count, item.name(lang)); }
        public Material icon() { return item.icon(); }
    }

    /** Counted from the item's "Times Used" statistic, as in the mod. */
    record Use(ItemMatcher item, int count) implements Objective {
        public String typeId() { return "justquests:use_item"; }
        public int requiredCount() { return count; }
        public String label() { return "Use " + count + "x " + item.label(); }
        public BaseComponent display(String lang) { return goal(lang, "justquests.goal.use_count", count, item.name(lang)); }
        public Material icon() { return item.icon(); }
    }

    /** Enchanting at an enchanting table, optionally only a given item. */
    record Enchant(ItemMatcher item, int count) implements Objective {
        public String typeId() { return "justquests:enchant_item"; }
        public int requiredCount() { return count; }
        public String label() { return "Enchant " + count + "x " + (item == null ? "any item" : item.label()); }
        public BaseComponent display(String lang) {
            if (item != null) return goal(lang, "justquests.goal.enchant_count", count, item.name(lang));
            return Text.tr(lang, count == 1 ? "justquests.goal.enchant_one" : "justquests.goal.enchant_many", count);
        }
        public Material icon() { return item != null ? item.icon() : Material.ENCHANTING_TABLE; }
    }

    record Mine(BlockMatcher block, int count) implements Objective {
        public String typeId() { return "justquests:mine_block"; }
        public int requiredCount() { return count; }
        public String label() { return "Mine " + count + "x " + block.label(); }
        public BaseComponent display(String lang) { return goal(lang, "justquests.goal.mine_count", count, block.name(lang)); }
        public Material icon() { return block.icon(); }
    }

    record Place(BlockMatcher block, int count) implements Objective {
        public String typeId() { return "justquests:place_block"; }
        public int requiredCount() { return count; }
        public String label() { return "Place " + count + "x " + block.label(); }
        public BaseComponent display(String lang) { return goal(lang, "justquests.goal.place_count", count, block.name(lang)); }
        public Material icon() { return block.icon(); }
    }

    record Kill(EntityMatcher entity, int count) implements Objective {
        public String typeId() { return "justquests:kill_mob"; }
        public int requiredCount() { return count; }
        public String label() { return "Kill " + count + "x " + entity.label(); }
        public BaseComponent display(String lang) { return goal(lang, "justquests.goal.kill_count", count, entity.name(lang)); }
        public Material icon() { return entity.icon(); }
    }

    record Tame(EntityMatcher entity, int count) implements Objective {
        public String typeId() { return "justquests:tame_animal"; }
        public int requiredCount() { return count; }
        public String label() { return "Tame " + count + "x " + entity.label(); }
        public BaseComponent display(String lang) { return goal(lang, "justquests.goal.tame_count", count, entity.name(lang)); }
        public Material icon() { return entity.icon(); }
    }

    record Breed(EntityMatcher entity, int count) implements Objective {
        public String typeId() { return "justquests:breed_animal"; }
        public int requiredCount() { return count; }
        public String label() { return "Breed " + count + "x " + entity.label(); }
        public BaseComponent display(String lang) { return goal(lang, "justquests.goal.breed_count", count, entity.name(lang)); }
        public Material icon() { return entity.icon(); }
    }

    record Advancement(String advancement) implements Objective {
        public String typeId() { return "justquests:gain_advancement"; }
        public int requiredCount() { return 1; }
        public String label() { return "Earn advancement " + advancement; }
        public BaseComponent display(String lang) { return Text.tr(lang, "justquests.goal.advancement", Text.advancement(advancement)); }
        public Material icon() { return Material.KNOWLEDGE_BOOK; }
    }

    record Dimension(String dimension) implements Objective {
        public String typeId() { return "justquests:visit_dimension"; }
        public int requiredCount() { return 1; }
        public String label() { return "Visit " + dimension; }
        public BaseComponent display(String lang) { return Text.tr(lang, "justquests.goal.dimension", dimensionName(lang, dimension)); }
        public Material icon() {
            return switch (dimension) {
                case "minecraft:the_nether" -> Material.NETHERRACK;
                case "minecraft:the_end" -> Material.END_STONE;
                default -> Material.GRASS_BLOCK;
            };
        }

        /** Whether the world counts as the dimension: by its kind (any nether world is the Nether) or its own key. */
        public boolean matches(World world) {
            return dimension.equals(dimensionOf(world)) || dimension.equals(world.getKey().toString());
        }
    }

    record Level(int level) implements Objective {
        public String typeId() { return "justquests:reach_level"; }
        public int requiredCount() { return 1; }
        public String label() { return "Reach level " + level; }
        public BaseComponent display(String lang) { return Text.tr(lang, "justquests.goal.level", level); }
        public Material icon() { return Material.EXPERIENCE_BOTTLE; }
        public boolean reached(Player p) { return p.getLevel() >= level; }
    }

    record Location(String dimension, int x, int y, int z, int radius) implements Objective {
        public String typeId() { return "justquests:reach_location"; }
        public int requiredCount() { return 1; }
        public String label() { return "Reach (" + x + ", " + y + ", " + z + ")"; }
        public BaseComponent display(String lang) { return Text.tr(lang, "justquests.goal.location", x, y, z); }
        public Material icon() { return Material.COMPASS; }

        public boolean isAt(Player p) {
            org.bukkit.Location l = p.getLocation();
            if (dimension != null && !dimension.equals(dimensionOf(l.getWorld())) && !dimension.equals(l.getWorld().getKey().toString())) {
                return false;
            }
            double dx = l.getX() - (x + 0.5), dy = l.getY() - (y + 0.5), dz = l.getZ() - (z + 0.5);
            return dx * dx + dy * dy + dz * dz <= (double) radius * radius;
        }
    }

    /** The vanilla dimension id of a world's kind; extra worlds of a kind count as that dimension. */
    static String dimensionOf(World world) {
        return switch (world.getEnvironment()) {
            case NETHER -> "minecraft:the_nether";
            case THE_END -> "minecraft:the_end";
            case NORMAL -> "minecraft:overworld";
            default -> world.getKey().toString();
        };
    }

    /** "the Nether" for the vanilla dimensions, the id for others. */
    static BaseComponent dimensionName(String lang, String id) {
        String key = "justquests.dimension." + id.replace(':', '.');
        return Lang.has(key) ? Text.tr(lang, key) : new TextComponent(id.toLowerCase(Locale.ROOT));
    }
}
