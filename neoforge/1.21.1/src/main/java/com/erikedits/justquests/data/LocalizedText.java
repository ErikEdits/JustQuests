package com.erikedits.justquests.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

import java.util.Map;
import java.util.Objects;

/**
 * A piece of quest text that is either a single string or a per-language
 * map (e.g. {@code {"en_us": "First Steps", "de_de": "Erste Schritte"}}).
 *
 * <p>Resolved per player from their client language with an English
 * fallback (Q21). Backward compatible: a plain JSON string still parses,
 * so existing quests keep working untouched. The mod's own UI words stay
 * English (Q22); only the data-driven title/description and the vanilla
 * content names (item/mob/block, localized client-side) translate.
 */
public final class LocalizedText {
    public static final String DEFAULT_LANG = "en_us";
    public static final LocalizedText EMPTY = new LocalizedText("");

    /** Non-null for the plain-string form. */
    private final String single;
    /** Non-null for the per-language map form. */
    private final Map<String, String> byLang;

    private LocalizedText(String single) {
        this.single = single;
        this.byLang = null;
    }

    private LocalizedText(Map<String, String> byLang) {
        this.single = null;
        this.byLang = byLang;
    }

    public static final Codec<LocalizedText> CODEC = Codec.either(
        Codec.STRING,
        Codec.unboundedMap(Codec.STRING, Codec.STRING)
    ).xmap(
        either -> either.map(LocalizedText::new, LocalizedText::new),
        lt -> lt.single != null ? Either.left(lt.single) : Either.right(lt.byLang)
    );

    /** Text for the given client language code, falling back to English, then to any entry. */
    public String get(String lang) {
        if (single != null) return single;
        if (byLang == null || byLang.isEmpty()) return "";
        String exact = byLang.get(lang);
        if (exact != null) return exact;
        String en = byLang.get(DEFAULT_LANG);
        if (en != null) return en;
        return byLang.values().iterator().next();
    }

    public String getDefault() {
        return get(DEFAULT_LANG);
    }

    public boolean isBlank() {
        return getDefault().isBlank();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocalizedText other)) return false;
        return Objects.equals(single, other.single) && Objects.equals(byLang, other.byLang);
    }

    @Override
    public int hashCode() {
        return Objects.hash(single, byLang);
    }
}
