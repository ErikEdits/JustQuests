package com.erikedits.justquests.text;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

/**
 * Player-facing text as translation keys (assets/justquests/lang/*.json): each player's game shows it
 * in their own language. (This Minecraft version has no translation fallback; clients need the mod.)
 */
public final class Msg {
    private Msg() {}

    public static MutableComponent tr(String key, Object... args) {
        return Component.translatable(key, carry(key, args));
    }

    /** Text that needs no translation (names, ids, symbols). */
    public static MutableComponent lit(String text) {
        return Component.literal(text);
    }

    public static MutableComponent empty() {
        return lit("");
    }

    /** A chat button: runs the command when clicked and shows the hover text. */
    public static MutableComponent button(MutableComponent label, String command, Component hover) {
        return label.withStyle(s -> s
            .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, command))
            .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, hover)));
    }

    /** A category: translated for the bundled categories, the id itself for pack-defined ones. */
    public static MutableComponent category(String category) {
        String key = "justquests.category." + category.toLowerCase(Locale.ROOT);
        return English.has(key) ? tr(key) : lit(category);
    }

    /** The vanilla dimensions by name ("the Nether"), others by id. */
    public static MutableComponent dimension(String id) {
        String key = "justquests.dimension." + id.replace(':', '.');
        return English.has(key) ? tr(key) : lit(id);
    }

    /** Last path segment, readable: "chests/simple_dungeon" -> "simple dungeon". */
    public static String pretty(String path) {
        return path.substring(path.lastIndexOf('/') + 1).replace('_', ' ');
    }

    /**
     * Arguments as text or components (ids go as text), each carrying the colour codes that stand
     * before its %s in the English text: a code in the text does not reach into an argument, so
     * "§a✓ Accepted: %s" would otherwise show the quest name in white.
     */
    private static Object[] carry(String key, Object[] args) {
        Object[] out = new Object[args.length];
        String fmt = English.get(key);
        String codes = "";
        int arg = 0;
        for (int i = 0; i < args.length; i++) {
            Object a = args[i];
            out[i] = a instanceof Component || a instanceof Number || a instanceof Boolean || a instanceof String ? a : String.valueOf(a);
        }
        for (int i = 0; i + 1 < fmt.length() && arg < out.length; i++) {
            char c = fmt.charAt(i), n = fmt.charAt(i + 1);
            if (c == '\u00a7') {
                ChatFormatting f = ChatFormatting.getByCode(n);
                if (f == ChatFormatting.RESET) codes = "";
                else if (f != null) codes = f.isColor() ? "\u00a7" + n : codes + "\u00a7" + n;
                i++;
            } else if (c == '%') {
                i++;
                if (n == 's' || n == 'd') {
                    out[arg] = withCodes(out[arg], codes);
                    arg++;
                }
            }
        }
        return out;
    }

    private static Object withCodes(Object a, String codes) {
        if (codes.isEmpty() || a instanceof Boolean) return a;
        if (!(a instanceof Component c)) return codes + a;
        MutableComponent copy = c.copy();
        for (int i = 1; i < codes.length(); i += 2) {
            ChatFormatting f = ChatFormatting.getByCode(codes.charAt(i));
            if (f != null) copy = copy.withStyle(f);
        }
        return copy;
    }
}
