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

    /** "distinct": every kind of item, block or mob counts only once (eat 10 different foods). */
    default boolean distinct() {
        return false;
    }

    /** {@link #display(String)}, marked "(all different)" for a distinct objective. */
    static BaseComponent shown(Objective o, String lang) {
        return o.distinct() ? Text.tr(lang, "justquests.goal.distinct", o.display(lang)) : o.display(lang);
    }

    static Objective parse(JsonObject o) {
        String type = Quest.string(o, "type");
        if (!type.contains(":")) type = "justquests:" + type;
        return switch (type) {
            case "justquests:collect_item" -> new Collect(ItemMatcher.parse(o.get("item")), count(o), distinct(o));
            case "justquests:have_item" -> new Have(ItemMatcher.parse(o.get("item")), count(o));
            case "justquests:craft_item" -> new Craft(ItemMatcher.parse(o.get("item")), count(o), distinct(o));
            case "justquests:smelt_item" -> new Smelt(ItemMatcher.parse(o.get("item")), count(o), distinct(o));
            case "justquests:consume_item" -> new Consume(ItemMatcher.parse(o.get("item")), count(o), distinct(o));
            case "justquests:use_item" -> new Use(ItemMatcher.parse(o.get("item")), count(o), distinct(o));
            case "justquests:enchant_item" -> new Enchant(o.has("item") ? ItemMatcher.parse(o.get("item")) : null, count(o), distinct(o), via(o));
            case "justquests:anvil_item" -> new Anvil(o.has("item") ? ItemMatcher.parse(o.get("item")) : null,
                o.has("with") ? ItemMatcher.parse(o.get("with")) : null, count(o), distinct(o));
            case "justquests:grindstone_item" -> new Grindstone(o.has("item") ? ItemMatcher.parse(o.get("item")) : null,
                o.has("disenchant") && o.get("disenchant").getAsBoolean(), count(o), distinct(o));
            case "justquests:mine_block" -> new Mine(BlockMatcher.parse(Quest.string(o, "block")), count(o), distinct(o));
            case "justquests:place_block" -> new Place(BlockMatcher.parse(Quest.string(o, "block")), count(o), distinct(o));
            case "justquests:kill_mob" -> new Kill(EntityMatcher.parse(Quest.string(o, "entity")), count(o), distinct(o));
            case "justquests:tame_animal" -> new Tame(EntityMatcher.parse(Quest.string(o, "entity")), count(o), distinct(o));
            case "justquests:breed_animal" -> new Breed(EntityMatcher.parse(Quest.string(o, "entity")), count(o), distinct(o));
            case "justquests:gain_advancement" -> new Advancement(Matchers.ns(Quest.string(o, "advancement")));
            case "justquests:visit_dimension" -> new Dimension(Matchers.ns(Quest.string(o, "dimension")));
            case "justquests:reach_level" -> new Level(o.get("level").getAsInt());
            case "justquests:reach_location" -> new Location(
                o.has("dimension") ? Matchers.ns(o.get("dimension").getAsString()) : null,
                o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt(),
                o.has("radius") ? o.get("radius").getAsInt() : 4);
            // 0.6.0
            case "justquests:submit_item" -> new Submit(ItemMatcher.parse(o.get("item")), count(o));
            case "justquests:fish_item" -> new Fish(o.has("item") ? ItemMatcher.parse(o.get("item")) : null, count(o), distinct(o));
            case "justquests:trade_villager" -> new Trade(o.has("item") ? ItemMatcher.parse(o.get("item")) : null,
                o.has("profession") ? Matchers.ns(o.get("profession").getAsString().toLowerCase(Locale.ROOT)) : null, count(o), distinct(o));
            case "justquests:brew_potion" -> new Brew(o.has("potion") ? Matchers.ns(o.get("potion").getAsString().toLowerCase(Locale.ROOT)) : null,
                o.has("item") ? ItemMatcher.parse(o.get("item")) : null, count(o), distinct(o));
            case "justquests:stat" -> {
                Stat st = new Stat(Matchers.ns(Quest.string(o, "stat").toLowerCase(Locale.ROOT)), count(o));
                // 0.6.1: an unknown statistic makes the quest invalid (it never counted)
                if (st.statistic() == null) {
                    throw new IllegalArgumentException("unknown statistic " + st.stat() + " (use an id of the statistics screen's General list, like minecraft:jump)");
                }
                yield st;
            }
            case "justquests:interact_entity" -> new Interact(EntityMatcher.parse(Quest.string(o, "entity")),
                o.has("item") ? ItemMatcher.parse(o.get("item")) : null, count(o), distinct(o));
            case "justquests:use_block" -> new UseBlock(BlockMatcher.parse(Quest.string(o, "block")),
                o.has("item") ? ItemMatcher.parse(o.get("item")) : null, count(o), distinct(o));
            case "justquests:find_structure" -> new FindStructure(tagOrId(Quest.string(o, "structure")), o.has("count") ? o.get("count").getAsInt() : 1);
            case "justquests:find_biome" -> new FindBiome(tagOrId(Quest.string(o, "biome")), o.has("count") ? o.get("count").getAsInt() : 1);
            default -> throw new IllegalArgumentException("unknown objective type: " + type);
        };
    }

    private static int count(JsonObject o) {
        if (!o.has("count")) throw new IllegalArgumentException("objective needs a \"count\"");
        return o.get("count").getAsInt();
    }

    private static boolean distinct(JsonObject o) {
        return o.has("distinct") && o.get("distinct").getAsBoolean();
    }

    /** "#minecraft:village" or "village" -> "#minecraft:village", "minecraft:village". */
    private static String tagOrId(String s) {
        s = s.toLowerCase(Locale.ROOT);
        return s.startsWith("#") ? "#" + Matchers.ns(s.substring(1)) : Matchers.ns(s);
    }

    /** enchant_item "via": table (the default), anvil or any. */
    private static String via(JsonObject o) {
        String via = o.has("via") ? o.get("via").getAsString() : "table";
        if (!via.equals("table") && !via.equals("anvil") && !via.equals("any")) {
            throw new IllegalArgumentException("via must be table, anvil or any, not " + via);
        }
        return via;
    }

    private static BaseComponent goal(String lang, String key, int count, BaseComponent name) {
        return Text.tr(lang, key, count, name);
    }

    record Collect(ItemMatcher item, int count, boolean distinct) implements Objective {
        public String typeId() { return "justquests:collect_item"; }
        public int requiredCount() { return count; }
        public String label() { return "Collect " + count + "x " + item.label(); }
        public BaseComponent display(String lang) { return goal(lang, "justquests.goal.collect_count", count, item.name(lang)); }
        public Material icon() { return item.icon(); }
    }

    /**
     * Hold X of an item in the inventory, wherever it came from. The most the player has held at once
     * counts, so moving items in and out does not add up, and the progress does not go back.
     */
    record Have(ItemMatcher item, int count) implements Objective {
        public String typeId() { return "justquests:have_item"; }
        public int requiredCount() { return count; }
        public String label() { return "Have " + count + "x " + item.label(); }
        public BaseComponent display(String lang) { return goal(lang, "justquests.goal.have_count", count, item.name(lang)); }
        public Material icon() { return item.icon(); }

        /** How many matching items the player has in the whole inventory right now. */
        public int held(Player p) {
            int total = 0;
            for (org.bukkit.inventory.ItemStack stack : p.getInventory().getContents()) {
                if (stack != null && item.matches(stack)) total += stack.getAmount();
            }
            return total;
        }
    }

    record Craft(ItemMatcher item, int count, boolean distinct) implements Objective {
        public String typeId() { return "justquests:craft_item"; }
        public int requiredCount() { return count; }
        public String label() { return "Craft " + count + "x " + item.label(); }
        public BaseComponent display(String lang) { return goal(lang, "justquests.goal.craft_count", count, item.name(lang)); }
        public Material icon() { return item.icon(); }
    }

    record Smelt(ItemMatcher item, int count, boolean distinct) implements Objective {
        public String typeId() { return "justquests:smelt_item"; }
        public int requiredCount() { return count; }
        public String label() { return "Smelt " + count + "x " + item.label(); }
        public BaseComponent display(String lang) { return goal(lang, "justquests.goal.smelt_count", count, item.name(lang)); }
        public Material icon() { return item.icon(); }
    }

    record Consume(ItemMatcher item, int count, boolean distinct) implements Objective {
        public String typeId() { return "justquests:consume_item"; }
        public int requiredCount() { return count; }
        public String label() { return "Consume " + count + "x " + item.label(); }
        public BaseComponent display(String lang) { return goal(lang, "justquests.goal.consume_count", count, item.name(lang)); }
        public Material icon() { return item.icon(); }
    }

    /** Counted from the item's "Times Used" statistic, as in the mod. */
    record Use(ItemMatcher item, int count, boolean distinct) implements Objective {
        public String typeId() { return "justquests:use_item"; }
        public int requiredCount() { return count; }
        public String label() { return "Use " + count + "x " + item.label(); }
        public BaseComponent display(String lang) { return goal(lang, "justquests.goal.use_count", count, item.name(lang)); }
        public Material icon() { return item.icon(); }
    }

    /**
     * Enchanting, optionally only a given item. "via" (0.5.2, #25): "table" (the default), "anvil" (a
     * result taken out of an anvil with a new or higher enchantment than the item put in) or "any".
     */
    record Enchant(ItemMatcher item, int count, boolean distinct, String via) implements Objective {
        public String typeId() { return "justquests:enchant_item"; }
        public int requiredCount() { return count; }
        public String label() {
            return "Enchant " + count + "x " + (item == null ? "any item" : item.label()) + (via.equals("table") ? "" : " (via " + via + ")");
        }
        public BaseComponent display(String lang) {
            String key = via.equals("anvil") ? "justquests.goal.enchant_anvil" : "justquests.goal.enchant";
            if (item == null) return Text.tr(lang, count == 1 ? key + "_one" : key + "_many", count);
            // the enchantments a filter asks for come last (0.5.3, #25)
            BaseComponent enchants = item.enchantmentNames(lang);
            if (enchants != null) return Text.tr(lang, key + "_with_count", count, item.plainName(lang), enchants);
            return goal(lang, key + "_count", count, item.name(lang));
        }
        public Material icon() { return item != null ? item.icon() : via.equals("anvil") ? Material.ANVIL : Material.ENCHANTING_TABLE; }
        public boolean table() { return !via.equals("anvil"); }
        public boolean anvil() { return !via.equals("table"); }

        /** Whether the anvil gave the item a new enchantment or a higher level of one it had. */
        public static boolean raised(org.bukkit.inventory.ItemStack before, org.bukkit.inventory.ItemStack after) {
            java.util.Map<String, Integer> had = Matchers.enchantments(before);
            for (java.util.Map.Entry<String, Integer> e : Matchers.enchantments(after).entrySet()) {
                if (had.getOrDefault(e.getKey(), 0) < e.getValue()) return true;
            }
            return false;
        }
    }

    /**
     * Take X results out of an anvil (0.5.2, #25): enchanting with a book, combining, repairing or
     * renaming. "item" checks what comes out, "with" what lay in the right slot. Both are optional.
     */
    record Anvil(ItemMatcher item, ItemMatcher with, int count, boolean distinct) implements Objective {
        public String typeId() { return "justquests:anvil_item"; }
        public int requiredCount() { return count; }
        public String label() {
            return "Anvil " + count + "x " + (item == null ? "any item" : item.label()) + (with == null ? "" : " with " + with.label());
        }
        public BaseComponent display(String lang) {
            if (item != null && with != null) {
                // the enchantments the result needs go behind the item that brings them (0.5.3, #25)
                BaseComponent needed = item.enchantmentNames(lang);
                BaseComponent own = with.enchantmentNames(lang);
                BaseComponent first = item.name(lang);
                BaseComponent second = with.name(lang);
                if (needed != null && own == null) {
                    first = item.plainName(lang);
                    second = Text.tr(lang, "justquests.goal.with_extras", with.plainName(lang), needed);
                } else if (needed != null && needed.toPlainText().equals(own.toPlainText())) {
                    first = item.plainName(lang);
                }
                return Text.tr(lang, "justquests.goal.anvil_combine_count", count, first, second);
            }
            if (item != null && item.anvilName() != null && item.enchantmentNames(lang) == null) {
                return Text.tr(lang, "justquests.goal.anvil_rename_count", count, item.bareName(lang), item.anvilName());
            }
            if (item != null) return goal(lang, "justquests.goal.anvil_make_count", count, item.name(lang));
            if (with != null) return goal(lang, "justquests.goal.anvil_with_count", count, with.name(lang));
            return Text.tr(lang, count == 1 ? "justquests.goal.anvil_one" : "justquests.goal.anvil_many", count);
        }
        public Material icon() { return item != null ? item.icon() : with != null ? with.icon() : Material.ANVIL; }

        public boolean accepts(org.bukkit.inventory.ItemStack result, org.bukkit.inventory.ItemStack right) {
            return (item == null || item.matches(result)) && (with == null || (right != null && with.matches(right)));
        }
    }

    /**
     * Take X results out of a grindstone (0.5.2, #25). "item" checks what comes out, "disenchant":
     * true counts only when enchantments came off.
     */
    record Grindstone(ItemMatcher item, boolean disenchant, int count, boolean distinct) implements Objective {
        public String typeId() { return "justquests:grindstone_item"; }
        public int requiredCount() { return count; }
        public String label() { return (disenchant ? "Disenchant " : "Grindstone ") + count + "x " + (item == null ? "any item" : item.label()); }
        public BaseComponent display(String lang) {
            String key = disenchant ? "justquests.goal.disenchant" : "justquests.goal.grindstone";
            if (item != null) return goal(lang, key + "_count", count, item.name(lang));
            return Text.tr(lang, count == 1 ? key + "_one" : key + "_many", count);
        }
        public Material icon() { return item != null ? item.icon() : Material.GRINDSTONE; }

        public boolean accepts(org.bukkit.inventory.ItemStack result, org.bukkit.inventory.ItemStack top, org.bukkit.inventory.ItemStack bottom) {
            return (item == null || item.matches(result)) && (!disenchant || removed(result, top, bottom));
        }

        /** Whether an enchantment of the items put in is gone (or lower) on the result. */
        public static boolean removed(org.bukkit.inventory.ItemStack result, org.bukkit.inventory.ItemStack top, org.bukkit.inventory.ItemStack bottom) {
            java.util.Map<String, Integer> after = Matchers.enchantments(result);
            for (org.bukkit.inventory.ItemStack in : new org.bukkit.inventory.ItemStack[] {top, bottom}) {
                for (java.util.Map.Entry<String, Integer> e : Matchers.enchantments(in).entrySet()) {
                    if (after.getOrDefault(e.getKey(), 0) < e.getValue()) return true;
                }
            }
            return false;
        }
    }

    record Mine(BlockMatcher block, int count, boolean distinct) implements Objective {
        public String typeId() { return "justquests:mine_block"; }
        public int requiredCount() { return count; }
        public String label() { return "Mine " + count + "x " + block.label(); }
        public BaseComponent display(String lang) { return goal(lang, "justquests.goal.mine_count", count, block.name(lang)); }
        public Material icon() { return block.icon(); }
    }

    record Place(BlockMatcher block, int count, boolean distinct) implements Objective {
        public String typeId() { return "justquests:place_block"; }
        public int requiredCount() { return count; }
        public String label() { return "Place " + count + "x " + block.label(); }
        public BaseComponent display(String lang) { return goal(lang, "justquests.goal.place_count", count, block.name(lang)); }
        public Material icon() { return block.icon(); }
    }

    record Kill(EntityMatcher entity, int count, boolean distinct) implements Objective {
        public String typeId() { return "justquests:kill_mob"; }
        public int requiredCount() { return count; }
        public String label() { return "Kill " + count + "x " + entity.label(); }
        public BaseComponent display(String lang) { return goal(lang, "justquests.goal.kill_count", count, entity.name(lang)); }
        public Material icon() { return entity.icon(); }
    }

    record Tame(EntityMatcher entity, int count, boolean distinct) implements Objective {
        public String typeId() { return "justquests:tame_animal"; }
        public int requiredCount() { return count; }
        public String label() { return "Tame " + count + "x " + entity.label(); }
        public BaseComponent display(String lang) { return goal(lang, "justquests.goal.tame_count", count, entity.name(lang)); }
        public Material icon() { return entity.icon(); }
    }

    record Breed(EntityMatcher entity, int count, boolean distinct) implements Objective {
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

        /** Whether the player has the advancement already - one earned before the quest was taken counts too. */
        public boolean earnedBy(Player p) {
            org.bukkit.advancement.Advancement adv = org.bukkit.Bukkit.getAdvancement(Matchers.key(advancement));
            return adv != null && p.getAdvancementProgress(adv).isDone();
        }
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

    // --- 0.6.0 ------------------------------------------------------------------------------

    /** Hand in X of an item: /quest submit or the quest menu's Hand in button takes them out of the inventory. */
    record Submit(ItemMatcher item, int count) implements Objective {
        public String typeId() { return "justquests:submit_item"; }
        public int requiredCount() { return count; }
        public String label() { return "Hand in " + count + "x " + item.label(); }
        public BaseComponent display(String lang) { return goal(lang, "justquests.goal.submit_count", count, item.name(lang)); }
        public Material icon() { return item.icon(); }

        /** How many matching items the player has, worn armor left out (0.6.1). */
        public int held(Player p) {
            org.bukkit.inventory.ItemStack[] contents = p.getInventory().getContents();
            int total = 0;
            for (int i = 0; i < contents.length; i++) {
                org.bukkit.inventory.ItemStack stack = contents[i];
                if (!worn(i) && stack != null && item.matches(stack)) total += stack.getAmount();
            }
            return total;
        }

        /** The armor slots of a player inventory's contents: what the player wears is never handed in. */
        private static boolean worn(int slot) {
            return slot >= 36 && slot < 40;
        }

        /** Takes up to n matching items out of the inventory; returns how many it took. */
        public int take(Player p, int n) {
            org.bukkit.inventory.PlayerInventory inv = p.getInventory();
            org.bukkit.inventory.ItemStack[] contents = inv.getContents();
            int taken = 0;
            for (int i = 0; i < contents.length && taken < n; i++) {
                org.bukkit.inventory.ItemStack stack = contents[i];
                if (worn(i) || stack == null || !item.matches(stack)) continue;
                int k = Math.min(stack.getAmount(), n - taken);
                if (k >= stack.getAmount()) {
                    inv.setItem(i, null);
                } else {
                    stack.setAmount(stack.getAmount() - k);
                    inv.setItem(i, stack);
                }
                taken += k;
            }
            return taken;
        }
    }

    /** Catch X things with a fishing rod; "item" checks what was caught. */
    record Fish(ItemMatcher item, int count, boolean distinct) implements Objective {
        public String typeId() { return "justquests:fish_item"; }
        public int requiredCount() { return count; }
        public String label() { return "Fish " + count + "x " + (item == null ? "anything" : item.label()); }
        public BaseComponent display(String lang) {
            if (item != null) return goal(lang, "justquests.goal.fish_count", count, item.name(lang));
            return Text.tr(lang, count == 1 ? "justquests.goal.fish_one" : "justquests.goal.fish_many", count);
        }
        public Material icon() { return item != null ? item.icon() : Material.FISHING_ROD; }
        public boolean accepts(org.bukkit.inventory.ItemStack caught) { return item == null || item.matches(caught); }
    }

    /**
     * Trade with a villager or the wandering trader. "item" checks what the player gets (then the items
     * count, else the trades), "profession" who trades (minecraft:librarian, minecraft:wandering_trader).
     */
    record Trade(ItemMatcher item, String profession, int count, boolean distinct) implements Objective {
        public String typeId() { return "justquests:trade_villager"; }
        public int requiredCount() { return count; }
        public String label() {
            return "Trade " + count + "x " + (item == null ? "anything" : item.label()) + (profession == null ? "" : " with " + profession);
        }
        public BaseComponent display(String lang) {
            if (profession != null) {
                if (item != null) return Text.tr(lang, "justquests.goal.trade_with_count", count, item.name(lang), trader());
                return Text.tr(lang, count == 1 ? "justquests.goal.trade_with_one" : "justquests.goal.trade_with_many", trader(), count);
            }
            if (item != null) return goal(lang, "justquests.goal.trade_count", count, item.name(lang));
            return Text.tr(lang, count == 1 ? "justquests.goal.trade_one" : "justquests.goal.trade_many", count);
        }
        public Material icon() { return item != null ? item.icon() : Material.EMERALD; }
        public boolean accepts(org.bukkit.inventory.ItemStack bought, String who) {
            return (item == null || item.matches(bought)) && (profession == null || profession.equals(who));
        }
        private BaseComponent trader() {
            if (profession.equals("minecraft:wandering_trader")) {
                return new net.md_5.bungee.api.chat.TranslatableComponent("entity.minecraft.wandering_trader");
            }
            return new net.md_5.bungee.api.chat.TranslatableComponent("entity.minecraft.villager." + profession.substring(profession.indexOf(':') + 1));
        }
    }

    /** Take X potions out of a brewing stand; "potion" (its long and strong forms count too) and "item" filter. */
    record Brew(String potion, ItemMatcher item, int count, boolean distinct) implements Objective {
        public String typeId() { return "justquests:brew_potion"; }
        public int requiredCount() { return count; }
        public String label() { return "Brew " + count + "x " + (potion != null ? potion : item != null ? item.label() : "any potion"); }
        public BaseComponent display(String lang) {
            BaseComponent name = name(lang);
            if (name != null) return goal(lang, "justquests.goal.brew_count", count, name);
            return Text.tr(lang, count == 1 ? "justquests.goal.brew_one" : "justquests.goal.brew_many", count);
        }
        private BaseComponent name(String lang) {
            if (potion != null) {
                ItemMatcher base = item instanceof Matchers.Filtered f ? f.base() : item != null ? item : ItemMatcher.parse("minecraft:potion");
                return new Matchers.Filtered(base, java.util.Map.of(), potion, null).bareName(lang);
            }
            return item != null ? item.name(lang) : null;
        }
        public Material icon() { return item != null ? item.icon() : Material.BREWING_STAND; }
        public boolean accepts(org.bukkit.inventory.ItemStack brewed) {
            if (item != null && !item.matches(brewed)) return false;
            if (potion == null) return true;
            String got = brewed.getItemMeta() instanceof org.bukkit.inventory.meta.PotionMeta pm
                ? com.erikedits.justquests.plugin.compat.Compat.basePotion(pm) : null;
            if (got == null) return false;
            String ns = potion.substring(0, potion.indexOf(':') + 1);
            String path = potion.substring(potion.indexOf(':') + 1);
            return got.equals(potion) || got.equals(ns + "long_" + path) || got.equals(ns + "strong_" + path);
        }
    }

    /** Raise a statistic after taking the quest: distances in blocks, times in minutes, the rest as counted. */
    record Stat(String stat, int count) implements Objective {
        public String typeId() { return "justquests:stat"; }
        public int requiredCount() { return count; }
        public String label() { return "Statistic " + stat + " +" + count; }
        public int unit() {
            String p = stat.substring(stat.indexOf(':') + 1);
            if (p.endsWith("_one_cm")) return 100;
            if (p.equals("play_time") || p.equals("play_one_minute") || p.equals("total_world_time") || p.startsWith("time_since_")
                || p.equals("sneak_time")) return 1200;
            return 1;
        }
        private String units() { return unit() == 100 ? "_blocks" : unit() == 1200 ? "_minutes" : ""; }
        public BaseComponent display(String lang) {
            BaseComponent name = new net.md_5.bungee.api.chat.TranslatableComponent("stat." + stat.replace(':', '.'));
            return Text.tr(lang, "justquests.goal.stat_count" + units(), name, count);
        }
        public Material icon() {
            String p = stat.substring(stat.indexOf(':') + 1);
            if (p.endsWith("_one_cm")) return Material.LEATHER_BOOTS;
            return switch (p) {
                case "play_time", "play_one_minute", "total_world_time", "time_since_death", "time_since_rest" -> Material.CLOCK;
                case "sleep_in_bed" -> Material.RED_BED;
                case "jump" -> Material.RABBIT_FOOT;
                case "fish_caught" -> Material.FISHING_ROD;
                default -> Material.PAPER;
            };
        }
        /** The Bukkit statistic of the id, or null. */
        public org.bukkit.Statistic statistic() {
            // Bukkit names some statistics differently from the game (0.6.1): minecraft:play_time is PLAY_ONE_MINUTE
            String alias = BUKKIT_NAMES.get(stat);
            if (alias != null) {
                try {
                    return org.bukkit.Statistic.valueOf(alias);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
            for (org.bukkit.Statistic s : org.bukkit.Statistic.values()) {
                if (s.getType() == org.bukkit.Statistic.Type.UNTYPED
                    && com.erikedits.justquests.plugin.compat.Compat.key(s).toString().equals(stat)) return s;
            }
            return null;
        }

        /** The game's statistic ids whose Bukkit name is not the id in capitals. */
        private static final java.util.Map<String, String> BUKKIT_NAMES = java.util.Map.ofEntries(
            java.util.Map.entry("minecraft:play_time", "PLAY_ONE_MINUTE"),
            java.util.Map.entry("minecraft:drop", "DROP_COUNT"),
            java.util.Map.entry("minecraft:eat_cake_slice", "CAKE_SLICES_EATEN"),
            java.util.Map.entry("minecraft:fill_cauldron", "CAULDRON_FILLED"),
            java.util.Map.entry("minecraft:use_cauldron", "CAULDRON_USED"),
            java.util.Map.entry("minecraft:clean_armor", "ARMOR_CLEANED"),
            java.util.Map.entry("minecraft:clean_banner", "BANNER_CLEANED"),
            java.util.Map.entry("minecraft:interact_with_brewingstand", "BREWINGSTAND_INTERACTION"),
            java.util.Map.entry("minecraft:interact_with_beacon", "BEACON_INTERACTION"),
            java.util.Map.entry("minecraft:inspect_dropper", "DROPPER_INSPECTED"),
            java.util.Map.entry("minecraft:inspect_hopper", "HOPPER_INSPECTED"),
            java.util.Map.entry("minecraft:inspect_dispenser", "DISPENSER_INSPECTED"),
            java.util.Map.entry("minecraft:play_noteblock", "NOTEBLOCK_PLAYED"),
            java.util.Map.entry("minecraft:tune_noteblock", "NOTEBLOCK_TUNED"),
            java.util.Map.entry("minecraft:pot_flower", "FLOWER_POTTED"),
            java.util.Map.entry("minecraft:trigger_trapped_chest", "TRAPPED_CHEST_TRIGGERED"),
            java.util.Map.entry("minecraft:open_enderchest", "ENDERCHEST_OPENED"),
            java.util.Map.entry("minecraft:enchant_item", "ITEM_ENCHANTED"),
            java.util.Map.entry("minecraft:play_record", "RECORD_PLAYED"),
            java.util.Map.entry("minecraft:interact_with_furnace", "FURNACE_INTERACTION"),
            java.util.Map.entry("minecraft:interact_with_crafting_table", "CRAFTING_TABLE_INTERACTION"),
            java.util.Map.entry("minecraft:open_chest", "CHEST_OPENED"),
            java.util.Map.entry("minecraft:open_shulker_box", "SHULKER_BOX_OPENED"));
    }

    /** Right-click a mob, optionally holding an item; the same mob again counts after a second. */
    record Interact(EntityMatcher entity, ItemMatcher item, int count, boolean distinct) implements Objective {
        public String typeId() { return "justquests:interact_entity"; }
        public int requiredCount() { return count; }
        public String label() { return "Interact " + count + "x " + entity.label() + (item == null ? "" : " with " + item.label()); }
        public BaseComponent display(String lang) {
            if (item != null) return Text.tr(lang, "justquests.goal.interact_item_count", item.name(lang), count, entity.name(lang));
            return goal(lang, "justquests.goal.interact_count", count, entity.name(lang));
        }
        public Material icon() { return item != null ? item.icon() : entity.icon(); }
        public boolean accepts(org.bukkit.entity.EntityType type, org.bukkit.inventory.ItemStack held) {
            return entity.matches(type) && (item == null || (held != null && item.matches(held)));
        }
    }

    /** Right-click a block, optionally holding an item; the same block again counts after a second. */
    record UseBlock(BlockMatcher block, ItemMatcher item, int count, boolean distinct) implements Objective {
        public String typeId() { return "justquests:use_block"; }
        public int requiredCount() { return count; }
        public String label() { return "Use " + count + "x " + block.label() + (item == null ? "" : " with " + item.label()); }
        public BaseComponent display(String lang) {
            if (item != null) return Text.tr(lang, "justquests.goal.use_block_item_count", item.name(lang), count, block.name(lang));
            return goal(lang, "justquests.goal.use_block_count", count, block.name(lang));
        }
        public Material icon() { return block.icon(); }
        public boolean accepts(Material used, org.bukkit.inventory.ItemStack held) {
            return block.matches(used) && (item == null || (held != null && item.matches(held)));
        }
    }

    /** Stand inside a structure (id or #tag); with a count above 1 every structure counts once. */
    record FindStructure(String structure, int count) implements Objective {
        public String typeId() { return "justquests:find_structure"; }
        public int requiredCount() { return count; }
        public boolean distinct() { return count > 1; }
        public String label() { return "Find " + count + "x " + structure; }
        public BaseComponent display(String lang) {
            BaseComponent name = named(lang, "justquests.structure.", structure);
            return count == 1 ? Text.tr(lang, "justquests.goal.find_structure", name)
                : Text.tr(lang, "justquests.goal.find_structure_count", count, name);
        }
        public Material icon() { return Material.FILLED_MAP; }
        /**
         * The server API knows no structure tags: a tag (#minecraft:village) matches the structures whose
         * id starts with its name (minecraft:village_plains, ...).
         */
        public boolean accepts(String id) {
            if (!structure.startsWith("#")) return structure.equals(id);
            return id.startsWith(structure.substring(1));
        }
    }

    /** Stand in a biome (id or #tag); with a count above 1 every biome counts once. */
    record FindBiome(String biome, int count) implements Objective {
        public String typeId() { return "justquests:find_biome"; }
        public int requiredCount() { return count; }
        public boolean distinct() { return count > 1; }
        public String label() { return "Find " + count + "x " + biome; }
        public BaseComponent display(String lang) {
            BaseComponent name = biome.startsWith("#") ? named(lang, "justquests.biome.", biome)
                : new net.md_5.bungee.api.chat.TranslatableComponent("biome." + biome.replace(':', '.'));
            return count == 1 ? Text.tr(lang, "justquests.goal.find_biome", name)
                : Text.tr(lang, "justquests.goal.find_biome_count", count, name);
        }
        public Material icon() { return Material.OAK_SAPLING; }
        public boolean accepts(String id, World.Environment environment) {
            if (!biome.startsWith("#")) return biome.equals(id);
            return BiomeTags.has(biome.substring(1), id, environment);
        }
    }

    /** A structure or tag name from the language files, a #tag as "any ...", else made from the id. */
    private static BaseComponent named(String lang, String prefix, String id) {
        boolean tag = id.startsWith("#");
        String bare = tag ? id.substring(1) : id;
        String key = prefix + (tag ? "tag." : "") + bare.replace(':', '.');
        String path = bare.substring(bare.indexOf(':') + 1);
        if (prefix.startsWith("justquests.biome") && path.startsWith("is_")) path = path.substring(3);
        BaseComponent name = Lang.has(key) ? Text.tr(lang, key) : new TextComponent(Text.readable(path));
        return tag ? Text.tr(lang, "justquests.goal.any", name) : name;
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

    /** "the Nether" for the vanilla dimensions, a name made from the id for others ("stellaris:moon" -> "Moon"). */
    static BaseComponent dimensionName(String lang, String id) {
        String key = "justquests.dimension." + id.replace(':', '.');
        if (Lang.has(key)) return Text.tr(lang, key);
        String path = id.toLowerCase(Locale.ROOT);
        return new TextComponent(Text.readable(path.substring(path.indexOf(':') + 1)));
    }
}
