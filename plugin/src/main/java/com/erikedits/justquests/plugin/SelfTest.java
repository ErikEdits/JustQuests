package com.erikedits.justquests.plugin;

import com.erikedits.justquests.plugin.compat.Compat;
import com.erikedits.justquests.plugin.quest.Objective;
import com.erikedits.justquests.plugin.quest.Quest;
import com.erikedits.justquests.plugin.quest.Reward;
import com.erikedits.justquests.plugin.text.Items;
import com.erikedits.justquests.plugin.text.Lang;
import com.erikedits.justquests.plugin.text.Text;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * /quest test: checks what can be checked without a player - every quest builds its quest-book
 * item in every language, the server takes the component text of those items, and the texts
 * exist in all languages.
 */
final class SelfTest {
    private SelfTest() {}

    static List<String> run(JustQuestsPlugin plugin) {
        List<String> out = new ArrayList<>();
        out.add("§eJustQuests self-test §7(" + Bukkit.getName() + " " + Bukkit.getBukkitVersion()
            + ", plugin jar for " + Compat.RANGE + ")");
        int quests = 0, items = 0, problems = 0;
        boolean components = true;
        String sample = null;
        for (Quest q : plugin.quests().all().values()) {
            quests++;
            for (String lang : Lang.CODES) {
                try {
                    List<BaseComponent> lore = new ArrayList<>();
                    for (String line : q.description().get(lang).split("\n")) lore.add(Text.lit(line));
                    for (Objective o : q.objectives()) lore.add(o.display(lang));
                    for (Reward r : q.rewards()) lore.add(r.display(lang));
                    ItemStack stack = Items.make(q.iconOrGoal(), 1, Text.lit(q.title().get(lang)), lore, true);
                    ItemMeta meta = stack.getItemMeta();
                    if (meta == null || !meta.hasDisplayName() || !meta.hasLore() || meta.getLore().size() != lore.size()) {
                        problems++;
                        out.add("§c  " + q.id() + " [" + lang + "]: item text missing");
                    } else {
                        for (String line : meta.getLore()) {
                            // component text that was taken as plain text would show its JSON
                            if (line.contains("{\"") || line.contains("\"translate\"") || line.contains("translate:")) {
                                problems++;
                                out.add("§c  " + q.id() + " [" + lang + "]: raw component text: " + line);
                                break;
                            }
                        }
                        if (sample == null && lang.equals("de_de")) sample = meta.getLore().get(lore.size() - q.rewards().size() - 1);
                    }
                    items++;
                } catch (RuntimeException e) {
                    problems++;
                    out.add("§c  " + q.id() + " [" + lang + "]: " + e);
                }
            }
        }
        if (Items.fellBack()) {
            components = false;
            out.add("§e  Menu items use plain text (the server did not take component text); names show in English.");
        }
        if (plugin.settings().generator) {
            List<String> gen = plugin.generator().selfTest();
            for (String g : gen) out.add("§c  Generator: " + g);
            problems += gen.size();
            if (gen.isEmpty()) out.add("§7  Generator: §aOK");
        }
        int missing = 0;
        for (String lang : Lang.CODES) missing += Lang.missing(lang);
        out.add("§7  Quests: §f" + quests + "§7, items built: §f" + items
            + "§7, component text: " + (components ? "§aOK" : "§eplain") + "§7, missing texts: §f" + missing);
        if (sample != null) out.add("§7  Sample goal line (de_de, names as the server language sees them): §r" + sample);
        out.add(problems == 0 ? "§a  Passed." : "§c  " + problems + " problem(s).");
        return out;
    }
}
