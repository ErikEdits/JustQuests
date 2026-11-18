package com.erikedits.justquests.plugin.text;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Sends chat components (0.6.1). Spigot, Paper and their forks take them through {@code spigot()}; plain
 * Bukkit (CraftBukkit) has no such thing, so the Bukkit jar brings the chat components along and the
 * message goes out as the game's own tellraw (the action bar as title ... actionbar), with the same
 * colours and buttons. From 1.21.5 the game writes click and hover events in snake_case.
 */
public final class Chat {
    private static final boolean SPIGOT = hasSpigot();
    private static Boolean modern;

    private Chat() {}

    /** Whether the server has Spigot's chat additions (Spigot, Paper, Purpur, ...). */
    public static boolean spigot() {
        return SPIGOT;
    }

    public static void send(CommandSender to, BaseComponent message) {
        if (to == null || message == null) return;
        if (SPIGOT) {
            Sp.send(to, message);
        } else if (to instanceof Player p) {
            command("tellraw " + p.getName() + " " + json(message));
        } else {
            to.sendMessage(message.toLegacyText());
        }
    }

    public static void actionBar(Player to, BaseComponent message) {
        if (to == null || message == null) return;
        if (SPIGOT) Sp.actionBar(to, message);
        else command("title " + to.getName() + " actionbar " + json(message));
    }

    private static void command(String line) {
        com.erikedits.justquests.plugin.Tasks.console(org.bukkit.plugin.java.JavaPlugin.getPlugin(com.erikedits.justquests.plugin.JustQuestsPlugin.class), line);
    }

    /** The component as the game's text JSON. */
    static String json(BaseComponent message) {
        String json = ComponentSerializer.toString(message);
        if (modern == null) modern = Items.atLeast(Bukkit.getBukkitVersion(), "1.21.5");
        if (!modern) return json;
        JsonElement tree = JsonParser.parseString(json);
        return snake(tree).toString();
    }

    /** clickEvent / hoverEvent the way 1.21.5 and newer write them (click_event, hover_event). */
    private static JsonElement snake(JsonElement e) {
        if (e.isJsonArray()) {
            JsonArray out = new JsonArray();
            for (JsonElement x : e.getAsJsonArray()) out.add(snake(x));
            return out;
        }
        if (!e.isJsonObject()) return e;
        JsonObject out = new JsonObject();
        for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) {
            String key = en.getKey();
            JsonElement value = en.getValue();
            if (key.equals("clickEvent") && value.isJsonObject()) {
                JsonObject c = value.getAsJsonObject();
                String action = c.has("action") ? c.get("action").getAsString() : "";
                JsonObject n = new JsonObject();
                n.addProperty("action", action);
                if (c.has("value")) {
                    String field = switch (action) {
                        case "open_url" -> "url";
                        case "run_command", "suggest_command" -> "command";
                        case "change_page" -> "page";
                        default -> "value";
                    };
                    if (field.equals("page")) n.addProperty(field, c.get("value").getAsInt());
                    else n.add(field, c.get("value"));
                }
                out.add("click_event", n);
            } else if (key.equals("hoverEvent") && value.isJsonObject()) {
                JsonObject h = value.getAsJsonObject();
                JsonObject n = new JsonObject();
                n.addProperty("action", h.has("action") ? h.get("action").getAsString() : "show_text");
                JsonElement shown = h.has("contents") ? h.get("contents") : h.get("value");
                if (shown != null) n.add("value", snake(shown));
                out.add("hover_event", n);
            } else {
                out.add(key, snake(value));
            }
        }
        return out;
    }

    private static boolean hasSpigot() {
        try {
            Class.forName("org.bukkit.entity.Player$Spigot");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /** The calls that only exist on Spigot, kept apart so plain Bukkit never touches them. */
    private static final class Sp {
        static void send(CommandSender to, BaseComponent message) {
            to.spigot().sendMessage(message);
        }

        static void actionBar(Player to, BaseComponent message) {
            to.spigot().sendMessage(ChatMessageType.ACTION_BAR, message);
        }
    }
}
