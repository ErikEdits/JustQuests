package com.erikedits.justquests.plugin.text;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;

/**
 * Builds chat components from the translated texts. Arguments can be components, so item, block,
 * mob and effect names are sent as translation keys and every client shows them in its own
 * language. A colour code in the text carries into the argument that follows it, as in the mod.
 */
public final class Text {
    private Text() {}

    /** The translated text for the key, with arguments (components, strings or numbers). */
    public static BaseComponent tr(String lang, String key, Object... args) {
        return format(Lang.get(lang, key), args);
    }

    /** As {@link #tr}, flattened to a legacy string (for inventory titles and the boss bar). */
    public static String legacy(String lang, String key, Object... args) {
        return tr(lang, key, args).toLegacyText();
    }

    public static BaseComponent lit(String legacyText) {
        TextComponent root = new TextComponent("");
        root.addExtra(TextComponent.fromLegacy(legacyText));
        return root;
    }

    public static BaseComponent format(String template, Object... args) {
        TextComponent root = new TextComponent("");
        StringBuilder chunk = new StringBuilder();
        String codes = "";
        int next = 0;
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            if (c == '§' && i + 1 < template.length()) {
                chunk.append(c).append(template.charAt(i + 1));
                codes = track(codes, template.charAt(i + 1));
                i++;
                continue;
            }
            if (c != '%' || i + 1 >= template.length()) {
                chunk.append(c);
                continue;
            }
            int j = i + 1;
            int index = -1;
            while (j < template.length() && Character.isDigit(template.charAt(j))) j++;
            if (j > i + 1 && j < template.length() && template.charAt(j) == '$') {
                index = Integer.parseInt(template.substring(i + 1, j)) - 1;
                j++;
            } else {
                j = i + 1;
            }
            char type = j < template.length() ? template.charAt(j) : 0;
            if (type == '%' && index < 0) {
                chunk.append('%');
                i = j;
            } else if (type == 's' || type == 'd') {
                flush(root, chunk);
                int a = index >= 0 ? index : next++;
                root.addExtra(argument(a < args.length ? args[a] : "", codes));
                chunk.append(codes);   // the text after the argument keeps the colour
                i = j;
            } else {
                chunk.append(c);
            }
        }
        flush(root, chunk);
        return root;
    }

    private static void flush(TextComponent root, StringBuilder chunk) {
        if (chunk.length() == 0) return;
        root.addExtra(TextComponent.fromLegacy(chunk.toString()));
        chunk.setLength(0);
    }

    /** The colour and format codes in force after code {@code code}. */
    private static String track(String codes, char code) {
        ChatColor color = ChatColor.getByChar(Character.toLowerCase(code));
        if (color == null) return codes;
        if (color == ChatColor.RESET) return "";
        if (isColor(code)) return "§" + Character.toLowerCase(code);
        return codes + "§" + Character.toLowerCase(code);
    }

    private static boolean isColor(char code) {
        return "0123456789abcdef".indexOf(Character.toLowerCase(code)) >= 0;
    }

    private static BaseComponent argument(Object a, String codes) {
        if (a instanceof BaseComponent c) {
            BaseComponent copy = c.duplicate();
            for (int i = 1; i < codes.length(); i += 2) {
                char code = codes.charAt(i);
                if (isColor(code)) {
                    if (copy.getColorRaw() == null) copy.setColor(ChatColor.getByChar(code));
                } else {
                    switch (code) {
                        case 'l' -> { if (copy.isBoldRaw() == null) copy.setBold(true); }
                        case 'o' -> { if (copy.isItalicRaw() == null) copy.setItalic(true); }
                        case 'n' -> { if (copy.isUnderlinedRaw() == null) copy.setUnderlined(true); }
                        case 'm' -> { if (copy.isStrikethroughRaw() == null) copy.setStrikethrough(true); }
                        case 'k' -> { if (copy.isObfuscatedRaw() == null) copy.setObfuscated(true); }
                        default -> { }
                    }
                }
            }
            return copy;
        }
        return lit(codes + a);
    }

    /** A clickable chat button that runs the command and shows the hover text. */
    public static BaseComponent button(BaseComponent label, String command, BaseComponent hover) {
        BaseComponent b = label.duplicate();
        b.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        b.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text(new BaseComponent[]{hover})));
        return b;
    }

    /** A link that opens the URL when clicked. */
    public static BaseComponent link(BaseComponent label, String url, BaseComponent hover) {
        BaseComponent b = label.duplicate();
        b.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        b.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text(new BaseComponent[]{hover})));
        return b;
    }

    // --- names the client translates -------------------------------------------------------

    public static BaseComponent item(Material m) {
        String key = m.isItem() ? m.getItemTranslationKey() : m.getBlockTranslationKey();
        return key == null ? new TextComponent(pretty(m.getKey().getKey())) : new TranslatableComponent(key);
    }

    public static BaseComponent block(Material m) {
        String key = m.isBlock() ? m.getBlockTranslationKey() : m.getItemTranslationKey();
        return key == null ? new TextComponent(pretty(m.getKey().getKey())) : new TranslatableComponent(key);
    }

    public static BaseComponent entity(EntityType t) {
        String key = t.getTranslationKey();
        return key == null ? new TextComponent(pretty(t.getKey().getKey())) : new TranslatableComponent(key);
    }

    public static BaseComponent effect(PotionEffectType t) {
        return new TranslatableComponent(t.getTranslationKey());
    }

    public static BaseComponent enchantment(Enchantment e) {
        return new TranslatableComponent(e.getTranslationKey());
    }

    /** A vanilla advancement's title ("Acquire Hardware"); the id where the client has no title for it. */
    public static BaseComponent advancement(String id) {
        NamespacedKey key = NamespacedKey.fromString(id);
        if (key == null || !NamespacedKey.MINECRAFT.equals(key.getNamespace())) return new TextComponent(id);
        TranslatableComponent t = new TranslatableComponent("advancements." + key.getKey().replace('/', '.') + ".title");
        t.setFallback(id);
        return t;
    }

    /** Last path segment, readable: "chests/simple_dungeon" -> "simple dungeon". */
    public static String pretty(String path) {
        return path.substring(path.lastIndexOf('/') + 1).replace('_', ' ');
    }
}
