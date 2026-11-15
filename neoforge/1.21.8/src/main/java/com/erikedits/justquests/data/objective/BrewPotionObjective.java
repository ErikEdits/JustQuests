package com.erikedits.justquests.data.objective;

import com.erikedits.justquests.text.Msg;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;

/**
 * Take X brewed potions out of a brewing stand (0.6.0). "potion" names the potion
 * (minecraft:swiftness also counts its long and strong forms), "item" can ask for a splash or
 * lingering potion or any other item filter.
 */
public record BrewPotionObjective(Optional<String> potion, Optional<ItemMatcher> item, int count, boolean distinct)
        implements QuestObjective {
    public static final String TYPE_ID = "justquests:brew_potion";

    public static final MapCodec<BrewPotionObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.xmap(BrewPotionObjective::ns, s -> s).optionalFieldOf("potion").forGetter(BrewPotionObjective::potion),
        ItemMatcher.CODEC.optionalFieldOf("item").forGetter(BrewPotionObjective::item),
        Codec.INT.fieldOf("count").forGetter(BrewPotionObjective::count),
        // every kind of item, block or mob counts only once
        Codec.BOOL.optionalFieldOf("distinct", false).forGetter(BrewPotionObjective::distinct)
    ).apply(instance, BrewPotionObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    /** The potion of a stack (minecraft:long_swiftness), or null. */
    public static String potionOf(ItemStack stack) {
        return ItemExtras.potion(stack);
    }

    public boolean accepts(ItemStack brewed) {
        if (item.isPresent() && !item.get().matches(brewed)) return false;
        if (potion.isEmpty()) return true;
        String got = ItemExtras.potion(brewed);
        if (got == null) return false;
        String want = potion.get();
        if (got.equals(want)) return true;
        // the long and strong forms count for the plain potion
        String ns = want.substring(0, want.indexOf(':') + 1), path = want.substring(want.indexOf(':') + 1);
        return got.equals(ns + "long_" + path) || got.equals(ns + "strong_" + path);
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return "Brew " + count + "x " + potion.orElse(item.map(ItemMatcher::label).orElse("any potion"));
    }

    /** "Potion of Swiftness" ("Splash Potion of ..." when the item asks for one). */
    private Optional<Component> name() {
        if (potion.isPresent()) {
            ItemMatcher base = item.orElse(ItemMatcher.parse("minecraft:potion"));
            if (base instanceof ItemMatcher.Filtered f) base = f.base();
            return Optional.of(new ItemMatcher.Filtered(base, Map.of(), potion, Optional.empty()).bareName());
        }
        return item.map(ItemMatcher::name);
    }

    @Override
    public Component display() {
        Optional<Component> name = name();
        if (name.isPresent()) return Msg.tr("justquests.goal.brew_count", count, name.get());
        return Msg.tr(count == 1 ? "justquests.goal.brew_one" : "justquests.goal.brew_many", count);
    }

    @Override
    public Component goal() {
        Optional<Component> name = name();
        return name.isPresent() ? Msg.tr("justquests.goal.brew", name.get()) : Msg.tr("justquests.goal.brew_any");
    }

    private static String ns(String id) {
        return id.indexOf(':') >= 0 ? id : "minecraft:" + id;
    }
}
