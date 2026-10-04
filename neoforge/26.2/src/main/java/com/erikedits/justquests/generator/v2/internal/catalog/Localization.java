package com.erikedits.justquests.generator.v2.internal.catalog;

import com.erikedits.justquests.generator.v2.internal.util.Json;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Text data for one language of the generated quests (lang/&lt;code&gt;.json). English is the source: the
 * generator picks every template, theme text and hint in English, then renders the same picks in each
 * extra language. Lists (title/phrase templates, theme names and descriptions, messages) are therefore
 * index-aligned with the English lists; hints are keyed by the English hint text; names are the official
 * translated names of the catalog targets ({@code id} or {@code id{potion}}).
 *
 * <p>The English instance ({@link #english}) only carries grammar settings; its text comes from
 * {@link Templates} and the catalog directly.
 */
public final class Localization {
    /** Extra languages written into generated quests, in output order. */
    public static final List<String> CODES = List.of("de_de", "fr_fr", "es_es", "ja_jp");
    /** Words kept capitalised when a name is lower-cased mid-sentence (French, Spanish). */
    private static final Set<String> KEEP_CASE = Set.of("Nether", "End", "Ender", "Overworld", "TNT", "ME", "Amérique",
        "Rocosas", "Crépuscule", "Twilight", "Archwood", "Create", "Mekanism", "Botania", "Farmer's", "Delight");
    private static final String VOWELS = "aeiouyàâäéèêëîïôöùûüœæAEIOUYÀÂÄÉÈÊËÎÏÔÖÙÛÜŒÆ";

    public final String code;
    public final boolean english;
    public final Templates templates;
    /** Theme key → names, index-aligned with the English theme names. */
    public final Map<String, List<String>> themeNames;
    /** Theme key → descriptions, index-aligned with the English theme descriptions. */
    public final Map<String, List<String>> themeDescriptions;
    /** Tag concept key → noun ("Stamm", "bûche" …). */
    public final Map<String, String> tagNouns;
    /** English catalog hint → translated hint. */
    public final Map<String, String> hints;
    /** Target id (or {@code id{potion}}) → official translated name. */
    public final Map<String, String> names;
    /** Reward messages, index-aligned with rewards.json. */
    public final List<String> messages;
    /** Dimension → travel phrase form ("in den Nether"). */
    public final Map<String, String> dimensionTo;
    /** Dimension → location phrase form ("im Nether"). */
    public final Map<String, String> dimensionIn;
    /** Sentence end added when a sentence has none ("." or "。"). */
    public final String sentenceEnd;
    /** Between sentences (" ", or "" for Japanese). */
    public final String sentenceSep;
    /** Between list items but the last two (", " or "、"). */
    public final String listSep;
    /** Between the last two list items (" and ", " und ", "と" …). */
    public final String listAnd;
    /** Lower-case names mid-sentence (French, Spanish). */
    public final boolean lowerNames;
    /** Lower-case the first list item after a colon in a theme description. */
    public final boolean lowerAfterColon;

    private Localization(String code, boolean english, Templates templates, JsonObject root) {
        this.code = code;
        this.english = english;
        this.templates = templates;
        this.themeNames = new LinkedHashMap<>();
        this.themeDescriptions = new LinkedHashMap<>();
        JsonObject themes = Json.obj(root, "themes");
        if (themes != null) {
            for (Map.Entry<String, JsonElement> e : themes.entrySet()) {
                if (e.getValue().isJsonObject()) {
                    themeNames.put(e.getKey(), Json.strings(e.getValue().getAsJsonObject(), "names"));
                    themeDescriptions.put(e.getKey(), Json.strings(e.getValue().getAsJsonObject(), "descriptions"));
                }
            }
        }
        this.tagNouns = strings(Json.obj(root, "tagNouns"));
        this.hints = strings(Json.obj(root, "hints"));
        this.names = strings(Json.obj(root, "names"));
        this.messages = new ArrayList<>(Json.strings(root, "messages"));
        JsonObject t = Json.obj(root, "templates");
        this.dimensionTo = strings(Json.obj(t, "dimensionTo"));
        this.dimensionIn = strings(Json.obj(t, "dimensionIn"));
        JsonObject g = Json.obj(root, "grammar");
        this.sentenceEnd = Json.str(g, "sentenceEnd", ".");
        this.sentenceSep = Json.str(g, "sentenceSep", " ");
        this.listSep = Json.str(g, "listSep", ", ");
        this.listAnd = Json.str(g, "listAnd", " and ");
        this.lowerNames = Json.bool(g, "lowerNames", false);
        this.lowerAfterColon = Json.bool(g, "lowerAfterColon", english);
    }

    /** The English source language (grammar only). */
    public static Localization english(Templates templates) {
        return new Localization("en_us", true, templates, new JsonObject());
    }

    /** Parses lang/&lt;code&gt;.json; returns null for a missing root. */
    public static Localization parse(String code, JsonObject root) {
        if (root == null) {
            return null;
        }
        return new Localization(code, false, Templates.parse(Json.obj(root, "templates")), root);
    }

    private static Map<String, String> strings(JsonObject o) {
        Map<String, String> out = new LinkedHashMap<>();
        if (o != null) {
            for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                if (e.getValue().isJsonPrimitive()) {
                    out.put(e.getKey(), e.getValue().getAsString());
                }
            }
        }
        return Collections.unmodifiableMap(out);
    }

    /** A name as used mid-sentence: lower-cased word by word for French and Spanish, unchanged otherwise. */
    public String inSentence(String name) {
        if (!lowerNames || name == null) {
            return name;
        }
        StringBuilder sb = new StringBuilder();
        for (String w : name.split(" ")) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            int ap = Math.max(w.lastIndexOf('\''), w.lastIndexOf('’'));
            String head = ap >= 0 ? w.substring(0, ap + 1) : "";
            String tail = ap >= 0 ? w.substring(ap + 1) : w;
            sb.append(lowerWord(head)).append(lowerWord(tail));
        }
        return sb.toString();
    }

    private static String lowerWord(String w) {
        if (w.isEmpty() || KEEP_CASE.contains(w) || w.length() > 1 && Character.isUpperCase(w.charAt(1))) {
            return w;
        }
        return w.toLowerCase(Locale.ROOT);
    }

    /** French "de"/"d'" before a name ("de fer", "d'émeraude"). */
    public String de(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return (VOWELS.indexOf(name.charAt(0)) >= 0 ? "d'" : "de ") + name;
    }

    /** Upper-cases the first character (a no-op for scripts without case). */
    public static String capitalize(String s) {
        return s == null || s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    /** Lower-cases the first character. */
    public static String lowerFirst(String s) {
        return s == null || s.isEmpty() ? s : s.substring(0, 1).toLowerCase(Locale.ROOT) + s.substring(1);
    }

    /** Adds the sentence end unless the text already ends with one. */
    public String ensureEnd(String s) {
        if (s.isEmpty()) {
            return s;
        }
        char last = s.charAt(s.length() - 1);
        return ".!?。！？".indexOf(last) >= 0 ? s : s + sentenceEnd;
    }
}
