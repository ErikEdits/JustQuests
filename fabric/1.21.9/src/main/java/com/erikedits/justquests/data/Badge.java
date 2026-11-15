package com.erikedits.justquests.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * A badge (0.6.0): something a player earns once and keeps, shown on the quest book's badge page and
 * as a sign before the player's name. A datapack defines one in data/&lt;ns&gt;/justquests/badges/&lt;id&gt;.json,
 * a world's custom-quests.json in its "badges" section. The mod's own ones (justquests:first_quest,
 * quests_10 ...) are earned by rules in {@link com.erikedits.justquests.badge.Badges}; every other badge
 * is given by the justquests:badge reward of a quest.
 *
 * @param name        shown on the badge page and in the tooltip of the sign before the name
 * @param description what it stands for
 * @param how         how to earn it, shown on the page while it is not earned
 * @param icon        an item for its picture when no resource pack has textures/gui/badges/&lt;path&gt;.png
 * @param color       #RRGGBB, the colour of its sign and (for the JustQuests Discord) of its role
 * @param symbol      the sign before the player's name (one character)
 * @param discord     whether it may become a role on the JustQuests Discord (custom badges only after
 *                    the Discord owner approves them)
 * @param secret      not listed on the page until earned
 */
public record Badge(LocalizedText name, LocalizedText description, LocalizedText how, Optional<String> icon,
                    String color, String symbol, boolean discord, boolean secret, int sort) {

    public static final Codec<Badge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        LocalizedText.CODEC.fieldOf("name").forGetter(Badge::name),
        LocalizedText.CODEC.optionalFieldOf("description", LocalizedText.EMPTY).forGetter(Badge::description),
        LocalizedText.CODEC.optionalFieldOf("how", LocalizedText.EMPTY).forGetter(Badge::how),
        Codec.STRING.optionalFieldOf("icon").forGetter(Badge::icon),
        Codec.STRING.optionalFieldOf("color", "#F2C14E").forGetter(Badge::color),
        Codec.STRING.optionalFieldOf("symbol", "★").forGetter(Badge::symbol),
        Codec.BOOL.optionalFieldOf("discord", false).forGetter(Badge::discord),
        Codec.BOOL.optionalFieldOf("secret", false).forGetter(Badge::secret),
        Codec.INT.optionalFieldOf("sort", 100).forGetter(Badge::sort)
    ).apply(instance, Badge::new));

    /** The colour as an RGB int (gold if it can't be read). */
    public int rgb() {
        try {
            String c = color.startsWith("#") ? color.substring(1) : color;
            return Integer.parseInt(c, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0xF2C14E;
        }
    }

    /** The sign before the name: its first character (a star if it has none). */
    public String sign() {
        return symbol.isEmpty() ? "★" : symbol.substring(0, symbol.offsetByCodePoints(0, 1));
    }
}
