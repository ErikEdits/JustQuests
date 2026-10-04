package com.erikedits.justquests.plugin.quest;

import com.erikedits.justquests.plugin.text.Lang;
import com.erikedits.justquests.plugin.text.Text;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/** The quest rewards, read from the same JSON as the mod ("type": "justquests:give_item", ...). */
public sealed interface Reward {
    String typeId();

    void grant(Player player);

    /** Plain English, for logs. */
    String label();

    BaseComponent display(String lang);

    /** An item to show for the reward in the quest book. */
    Material icon();

    /** How many to show on the icon. */
    default int iconCount() {
        return 1;
    }

    static Reward parse(JsonObject o) {
        String type = Quest.string(o, "type");
        if (!type.contains(":")) type = "justquests:" + type;
        return switch (type) {
            case "justquests:give_item" -> {
                Material m = Registry.MATERIAL.get(Matchers.key(Quest.string(o, "item")));
                if (m == null || !m.isItem()) throw new IllegalArgumentException("unknown item: " + Quest.string(o, "item"));
                yield new GiveItem(m, o.get("count").getAsInt());
            }
            case "justquests:command" -> new Command(Quest.string(o, "command"));
            case "justquests:loot_table" -> new Loot(Matchers.ns(Quest.string(o, "loot_table")));
            case "justquests:xp" -> new Xp(o.get("amount").getAsInt());
            case "justquests:effect" -> {
                PotionEffectType e = Registry.EFFECT.get(Matchers.key(Quest.string(o, "effect")));
                if (e == null) throw new IllegalArgumentException("unknown effect: " + Quest.string(o, "effect"));
                yield new Effect(e, o.has("seconds") ? o.get("seconds").getAsInt() : 30,
                    o.has("amplifier") ? o.get("amplifier").getAsInt() : 0);
            }
            case "justquests:message" -> new Message(LocalizedText.parse(o.get("message")));
            case "justquests:title" -> new Title(LocalizedText.parse(o.get("title")),
                o.has("subtitle") ? LocalizedText.parse(o.get("subtitle")) : null,
                o.has("fade_in") ? o.get("fade_in").getAsInt() : 10,
                o.has("stay") ? o.get("stay").getAsInt() : 70,
                o.has("fade_out") ? o.get("fade_out").getAsInt() : 20);
            case "justquests:money" -> new Money(o.get("amount").getAsInt());
            case "justquests:choice" -> {
                List<Reward> options = new ArrayList<>();
                for (JsonElement e : o.getAsJsonArray("options")) options.add(parse(e.getAsJsonObject()));
                yield new Choice(options);
            }
            default -> throw new IllegalArgumentException("unknown reward type: " + type);
        };
    }

    /** Puts the stacks into the inventory and drops what doesn't fit at the player's feet. */
    static void give(Player player, ItemStack stack) {
        for (ItemStack left : player.getInventory().addItem(stack).values()) {
            player.getWorld().dropItem(player.getLocation(), left);
        }
    }

    Logger LOG = Logger.getLogger("JustQuests");

    record GiveItem(Material item, int count) implements Reward {
        public String typeId() { return "justquests:give_item"; }

        public void grant(Player player) {
            int max = item.getMaxStackSize();
            for (int left = count; left > 0; left -= max) give(player, new ItemStack(item, Math.min(left, max)));
        }

        public String label() { return count + "x " + item.getKey(); }
        public BaseComponent display(String lang) { return Text.tr(lang, "justquests.reward.item", count, Text.item(item)); }
        public Material icon() { return item; }
        public int iconCount() { return count; }
    }

    /**
     * Runs a server command from the console; "{player}" becomes the player's name. A command that
     * needs the player as @s or relative coordinates (~) runs through "execute as ... at @s".
     */
    record Command(String command) implements Reward {
        public String typeId() { return "justquests:command"; }

        public void grant(Player player) {
            String cmd = command.replace("{player}", player.getName());
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            if (cmd.contains("@s") || cmd.contains("~")) cmd = "execute as " + player.getUniqueId() + " at @s run " + cmd;
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            } catch (RuntimeException e) {
                LOG.log(Level.SEVERE, "Command reward failed: /" + cmd, e);
            }
        }

        public String label() { return "Run: /" + command; }
        public BaseComponent display(String lang) { return Text.tr(lang, "justquests.reward.command", command); }
        public Material icon() { return Material.COMMAND_BLOCK; }
    }

    record Loot(String table) implements Reward {
        public String typeId() { return "justquests:loot_table"; }

        public void grant(Player player) {
            try {
                LootTable loot = Bukkit.getLootTable(Matchers.key(table));
                if (loot == null) {
                    LOG.warning("Loot-table reward: no loot table " + table);
                    return;
                }
                LootContext ctx = new LootContext.Builder(player.getLocation()).lootedEntity(player).killer(player).build();
                for (ItemStack stack : loot.populateLoot(new Random(), ctx)) {
                    if (stack != null && !stack.getType().isAir()) give(player, stack);
                }
            } catch (RuntimeException e) {
                LOG.log(Level.SEVERE, "Loot-table reward failed for " + table, e);
            }
        }

        public String label() { return "Loot: " + table; }
        public BaseComponent display(String lang) { return Text.tr(lang, "justquests.reward.loot", Text.pretty(table)); }
        public Material icon() { return Material.CHEST; }
    }

    /** Money through the server's economy plugin (see {@link Economy}); the plugin's own reward type. */
    record Money(int amount) implements Reward {
        public String typeId() { return "justquests:money"; }

        public void grant(Player player) {
            String cmd = Economy.command(player.getName(), amount);
            if (cmd == null) return;
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            } catch (RuntimeException e) {
                LOG.log(Level.SEVERE, "Money reward failed: /" + cmd, e);
            }
        }

        public String label() { return amount + " money"; }
        public BaseComponent display(String lang) { return Text.tr(lang, "justquests.plugin.reward.money", amount, Economy.name()); }
        public Material icon() { return Material.GOLD_NUGGET; }
        public int iconCount() { return Math.max(1, Math.min(64, amount)); }
    }

    record Xp(int amount) implements Reward {
        public String typeId() { return "justquests:xp"; }
        public void grant(Player player) { player.giveExp(amount); }
        public String label() { return amount + " XP"; }
        public BaseComponent display(String lang) { return Text.tr(lang, "justquests.reward.xp", amount); }
        public Material icon() { return Material.EXPERIENCE_BOTTLE; }
    }

    record Effect(PotionEffectType effect, int seconds, int amplifier) implements Reward {
        public String typeId() { return "justquests:effect"; }
        public void grant(Player player) { player.addPotionEffect(new PotionEffect(effect, seconds * 20, amplifier)); }
        public String label() { return "Effect " + effect.getKey() + " (" + seconds + "s)"; }
        public BaseComponent display(String lang) { return Text.tr(lang, "justquests.reward.effect", Text.effect(effect), seconds); }
        public Material icon() { return Material.POTION; }
    }

    record Message(LocalizedText message) implements Reward {
        public String typeId() { return "justquests:message"; }
        public void grant(Player player) { player.sendMessage(message.get(Lang.of(player))); }
        public String label() { return "Message"; }
        public BaseComponent display(String lang) { return Text.tr(lang, "justquests.reward.message"); }
        public Material icon() { return Material.PAPER; }
    }

    record Title(LocalizedText title, LocalizedText subtitle, int fadeIn, int stay, int fadeOut) implements Reward {
        public String typeId() { return "justquests:title"; }

        public void grant(Player player) {
            String lang = Lang.of(player);
            player.sendTitle(title.get(lang), subtitle == null ? "" : subtitle.get(lang), fadeIn, stay, fadeOut);
        }

        public String label() { return "Title: " + title.getDefault(); }
        public BaseComponent display(String lang) { return Text.tr(lang, "justquests.reward.title", title.get(lang)); }
        public Material icon() { return Material.NAME_TAG; }
    }

    /**
     * The player picks one option when claiming; without a pick the first is given. A quest has one
     * choice; if a pack gives several, the others give their first option.
     */
    record Choice(List<Reward> options) implements Reward {
        public String typeId() { return "justquests:choice"; }
        public void grant(Player player) { grant(player, 0); }

        /** Gives option {@code index} (0-based); an index out of range gives the first option. */
        public void grant(Player player, int index) {
            if (options.isEmpty()) return;
            options.get(index >= 0 && index < options.size() ? index : 0).grant(player);
        }

        public String label() {
            StringBuilder sb = new StringBuilder("one of: ");
            for (int i = 0; i < options.size(); i++) sb.append(i > 0 ? " / " : "").append(options.get(i).label());
            return sb.toString();
        }

        public BaseComponent display(String lang) {
            TextComponent list = new TextComponent("");
            for (int i = 0; i < options.size(); i++) {
                if (i > 0) list.addExtra(" / ");
                list.addExtra(options.get(i).display(lang));
            }
            return Text.tr(lang, "justquests.reward.choice", list);
        }

        public Material icon() { return Material.CHEST; }

        /** The quest's choice (the first one, if a pack gives several), or null. */
        public static Choice of(Quest quest) {
            for (Reward r : quest.rewards()) {
                if (r instanceof Choice c && !c.options().isEmpty()) return c;
            }
            return null;
        }
    }
}
