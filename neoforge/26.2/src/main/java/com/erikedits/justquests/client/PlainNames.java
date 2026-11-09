package com.erikedits.justquests.client;

import com.erikedits.justquests.generator.v2.internal.util.Ids;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Names without pictures (#19). Some resource packs replace a name with a single special character
 * that their font draws as a picture: Enhanced Boss Bars turns "Ender Dragon" into the dragon's boss
 * bar. In the quest book that picture would stand in the goal line, so a name that the current
 * language gives as such a character is looked up in the packs below it (the game's own name), or
 * made from its key if none has one: "Kill 1 Ender Dragon".
 */
public final class PlainNames {
    /** Translation key -> the plain name; cleared whenever the game loads its languages again. */
    private static final Map<String, String> CACHE = new HashMap<>();
    private static Language cachedFor;

    private PlainNames() {}

    /** The text with every name that the current language shows as a picture replaced by a plain one. */
    public static Component of(Component text) {
        return hasPicture(text.getString()) ? rebuild(text) : text;
    }

    static boolean hasPicture(String s) {
        return s.codePoints().anyMatch(PlainNames::picture);
    }

    /** Characters a font draws as pictures: the private use areas, which no language uses for words. */
    private static boolean picture(int c) {
        return c >= 0xE000 && c <= 0xF8FF || c >= 0xF0000;
    }

    private static MutableComponent rebuild(Component text) {
        MutableComponent out;
        if (text.getContents() instanceof TranslatableContents t) {
            String shown = Language.getInstance().getOrDefault(t.getKey(), t.getFallback() != null ? t.getFallback() : t.getKey());
            if (hasPicture(shown)) {
                out = Component.literal(plain(t.getKey(), shown));
            } else {
                Object[] args = t.getArgs().clone();
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof Component c) args[i] = rebuild(c);
                }
                out = Component.translatableWithFallback(t.getKey(), t.getFallback(), args);
            }
        } else {
            out = MutableComponent.create(text.getContents());
        }
        out.setStyle(text.getStyle());
        for (Component sibling : text.getSiblings()) out.append(rebuild(sibling));
        return out;
    }

    private static String plain(String key, String shown) {
        if (cachedFor != Language.getInstance()) {
            CACHE.clear();
            cachedFor = Language.getInstance();
        }
        return CACHE.computeIfAbsent(key, k -> {
            String found = fromPacks(k);
            if (found != null) return found;
            // the pack kept some words next to its picture
            StringBuilder rest = new StringBuilder();
            shown.codePoints().filter(c -> !picture(c)).forEach(rest::appendCodePoint);
            if (!rest.toString().isBlank()) return rest.toString().trim();
            // "entity.minecraft.ender_dragon" -> "Ender Dragon"
            return Ids.prettify(k.substring(k.lastIndexOf('.') + 1));
        });
    }

    /**
     * The key's text in the player's language (then in English) from the highest pack that has it
     * without a picture. Language files are looked up under the key's own namespace
     * ("entity.twilightforest.hydra") and under minecraft.
     */
    private static String fromPacks(String key) {
        Minecraft mc = Minecraft.getInstance();
        String[] parts = key.split("\\.");
        String ns = parts.length > 2 ? parts[1] : "minecraft";
        for (String lang : List.of(mc.options.languageCode, "en_us")) {
            for (String space : ns.equals("minecraft") ? List.of(ns) : List.of(ns, "minecraft")) {
                Identifier file = Identifier.tryBuild(space, "lang/" + lang + ".json");
                if (file == null) continue;
                List<Resource> stack = mc.getResourceManager().getResourceStack(file);
                // the stack runs from the lowest pack to the highest: the highest clean text wins
                for (int i = stack.size() - 1; i >= 0; i--) {
                    Map<String, String> texts = new HashMap<>();
                    try (InputStream in = stack.get(i).open()) {
                        Language.loadFromJson(in, texts::put);
                    } catch (Exception e) {
                        continue;   // a broken language file of some pack
                    }
                    String text = texts.get(key);
                    if (text != null && !text.isBlank() && !hasPicture(text)) return text;
                }
            }
        }
        return null;
    }
}
