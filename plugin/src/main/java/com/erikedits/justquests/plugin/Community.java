package com.erikedits.justquests.plugin;

import com.erikedits.justquests.plugin.text.Lang;
import com.erikedits.justquests.plugin.text.Text;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A player's first join: the one-time Discord welcome (as in the mod) and the quest book. Who has
 * been here is kept in plugins/JustQuests/seen-players.json, so neither repeats.
 */
public final class Community {
    public static final String INVITE = "https://discord.gg/cMTGE9QCja";

    private final JustQuestsPlugin plugin;
    private final Path file;
    private final Set<UUID> seen = new HashSet<>();

    public Community(JustQuestsPlugin plugin, Path file) {
        this.plugin = plugin;
        this.file = file;
        try {
            if (Files.exists(file)) {
                JsonElement root = JsonParser.parseString(Files.readString(file));
                if (root.isJsonArray()) {
                    for (JsonElement e : root.getAsJsonArray()) {
                        try {
                            seen.add(UUID.fromString(e.getAsString()));
                        } catch (IllegalArgumentException ignored) {
                            // not a uuid
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not read seen-players.json: " + e.getMessage());
        }
    }

    public void onJoin(Player player) {
        if (!seen.add(player.getUniqueId())) return;
        save();
        if (plugin.settings().giveBookOnFirstJoin) plugin.book().give(player);
        if (plugin.settings().discordWelcome) player.spigot().sendMessage(welcome(Lang.of(player)));
    }

    private void save() {
        try {
            JsonArray arr = new JsonArray();
            for (UUID id : seen) arr.add(id.toString());
            Files.createDirectories(file.getParent());
            Files.writeString(file, arr.toString());
        } catch (Exception e) {
            plugin.getLogger().warning("Could not save seen-players.json: " + e.getMessage());
        }
    }

    public static BaseComponent link(String lang) {
        return Text.link(Text.lit("§9§n" + INVITE), INVITE, Text.tr(lang, "justquests.hint.link_hover"));
    }

    public static BaseComponent welcome(String lang) {
        TextComponent m = new TextComponent("");
        for (String key : new String[]{"justquests.hint.welcome_title", "justquests.hint.welcome_join",
            "justquests.hint.welcome_vote", "justquests.hint.welcome_help", "justquests.hint.welcome_peek"}) {
            m.addExtra(Text.tr(lang, key));
        }
        m.addExtra(Text.lit("§7  "));
        m.addExtra(link(lang));
        m.addExtra(Text.tr(lang, "justquests.hint.welcome_anytime"));
        return m;
    }

    public static BaseComponent discord(String lang) {
        TextComponent m = new TextComponent("");
        m.addExtra(Text.tr(lang, "justquests.hint.discord"));
        m.addExtra(link(lang));
        return m;
    }
}
