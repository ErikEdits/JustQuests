package com.erikedits.justquests.generator.v2.internal.util;

import java.util.Locale;
import java.util.Set;

/** Tiny English helpers for generated text: plurals, articles, casing. */
public final class English {
    /** Last words that are mass nouns or invariant plurals ("24 raw iron", "8 sheep"). */
    private static final Set<String> INVARIANT = new java.util.HashSet<>(java.util.Arrays.asList("iron", "gold", "copper", "coal", "cobblestone", "dirt", "sand",
        "gravel", "clay", "wheat", "kelp", "bamboo", "cane", "beef", "mutton", "porkchop", "cod", "salmon", "dust",
        "lazuli", "quartz", "netherrack", "stone", "deepslate", "gunpowder", "leather", "string", "wool", "snow", "ice",
        "obsidian", "bread", "rice", "straw", "glass", "terracotta", "basalt", "blackstone", "tuff", "calcite", "andesite",
        "granite", "diorite", "sandstone", "sugar", "dough", "ore", "sheep", "fish", "silverfish", "debris", "scrap",
        "wart", "meal", "cocoa", "pasta", "stew", "soup", "cider", "juice", "milk", "honey", "slime", "moss", "mud",
        "concrete", "powder", "chocolate", "tea", "custard", "salad", "steak", "bacon", "ham", "limestone", "asurine",
        "crimsite", "ochrum", "veridium", "scoria", "scorchia", "zinc", "brass", "cabbage", "chicken", "rabbit", "squid",
        "cheesecake", "compost", "soil", "grass", "purpur", "prismarine", "shrub", "cactus", "seagrass", "coral",
        "firewood", "flesh", "ink", "wood", "alloy", "meat", "sulfur", "paper", "flint", "charcoal", "fruit", "cream",
        "scaffolding", "tnt", "lava", "water", "netherite", "ratatouille", "osmium", "tin", "uranium", "venison",
        "liveroot", "ironwood", "meef", "sculk", "nylium", "lichen", "steel"));
    /** Invariant entity plurals ("8 sheep"). */
    private static final Set<String> INVARIANT_ENTITY = Set.of("sheep", "fish", "cod", "salmon", "squid", "silverfish",
        "bison", "deer", "moose", "drowned", "bogged");

    private English() {
    }

    /** Longest host-supplied display name that is used as is; longer names fall back to the id. */
    public static final int MAX_HOST_NAME = 40;
    private static final java.util.regex.Pattern TRANSLATION_KEY =
        java.util.regex.Pattern.compile("[a-z0-9_]+(\\.[a-z0-9_]+){2,}");

    /**
     * Cleans a display name supplied by the host: strips legacy formatting codes, control
     * characters and template braces and collapses whitespace. Returns null when nothing usable is
     * left, when the name is too long, or when it is an untranslated key such as
     * {@code item.examplemod.widget} (the caller then prettifies the id instead).
     *
     * @param raw name as reported by the host, may be null
     * @return cleaned name or null
     */
    public static String cleanHostName(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.replaceAll("\u00a7.?", "")
            .replaceAll("[\\p{Cntrl}{}<>\\[\\]]", " ")
            .replaceAll("\\s+", " ")
            .trim();
        if (s.isEmpty() || s.length() > MAX_HOST_NAME || TRANSLATION_KEY.matcher(s).matches()) {
            return null;
        }
        return s;
    }

    /** Plural of an item/block display name, keeping mass nouns unchanged. Works on the last word. */
    public static String plural(String name) {
        return plural(name, false);
    }

    /**
     * Plural of a display name. Entities ignore the item mass-noun list ("8 chickens" but "8 raw chicken").
     *
     * @param name   display name
     * @param entity true for mobs/animals
     * @return plural form
     */
    public static String plural(String name, boolean entity) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        int sp = name.lastIndexOf(' ');
        String head = sp < 0 ? "" : name.substring(0, sp + 1);
        String last = sp < 0 ? name : name.substring(sp + 1);
        String lower = last.toLowerCase(Locale.ROOT);
        boolean invariant = entity ? INVARIANT_ENTITY.contains(lower) : INVARIANT.contains(lower);
        if (invariant || lower.endsWith("s") && !lower.endsWith("ss") && !lower.endsWith("us")) {
            return name;
        }
        String out;
        if (lower.endsWith("fungus")) {
            out = last.substring(0, last.length() - 2) + "i";
        } else if (lower.endsWith("man")) {
            out = last.substring(0, last.length() - 3) + keepCase(last, "men");
        } else if (lower.endsWith("f") && (lower.endsWith("lf") || lower.endsWith("af") || lower.endsWith("rf"))) {
            out = last.substring(0, last.length() - 1) + "ves";
        } else if (lower.endsWith("fe")) {
            out = last.substring(0, last.length() - 2) + "ves";
        } else if (lower.endsWith("y") && lower.length() > 1 && "aeiou".indexOf(lower.charAt(lower.length() - 2)) < 0) {
            out = last.substring(0, last.length() - 1) + "ies";
        } else if (lower.endsWith("ato")) {
            out = last + "es";
        } else if (lower.endsWith("s") || lower.endsWith("x") || lower.endsWith("z") || lower.endsWith("ch")
            || lower.endsWith("sh")) {
            out = last + "es";
        } else {
            out = last + "s";
        }
        return head + out;
    }

    private static String keepCase(String word, String suffix) {
        return word.isEmpty() || Character.isLowerCase(word.charAt(word.length() - 1)) ? suffix : suffix.toUpperCase(Locale.ROOT);
    }

    /** "a wolf" / "an ocelot". */
    public static String withArticle(String noun) {
        if (noun == null || noun.isEmpty()) {
            return noun;
        }
        char c = Character.toLowerCase(noun.charAt(0));
        return ("aeiou".indexOf(c) >= 0 ? "an " : "a ") + noun;
    }

    /** Lower-cases a display name for use mid-sentence, keeping proper nouns (Nether, End, Overworld). */
    public static String inSentence(String name) {
        if (name == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String w : name.split(" ")) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            if (w.equals("Nether") || w.equals("End") || w.equals("Overworld") || w.equals("TNT") || w.startsWith("O'")
                || w.length() > 1 && Character.isUpperCase(w.charAt(1))) {
                sb.append(w);
            } else {
                sb.append(w.toLowerCase(Locale.ROOT));
            }
        }
        return sb.toString();
    }

    /** Upper-cases the first character. */
    public static String capitalize(String s) {
        return s == null || s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    /** Title Case for every word. */
    public static String titleCase(String s) {
        if (s == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String w : s.split(" ")) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(capitalize(w));
        }
        return sb.toString();
    }
}
