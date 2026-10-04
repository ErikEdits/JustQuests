package com.erikedits.justquests.data.objective;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Earn a specific advancement. */
public record GainAdvancementObjective(Identifier advancement) implements QuestObjective {
    public static final String TYPE_ID = "justquests:gain_advancement";

    public static final MapCodec<GainAdvancementObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Identifier.CODEC.fieldOf("advancement").forGetter(GainAdvancementObjective::advancement)
    ).apply(instance, GainAdvancementObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    public boolean matches(Identifier id) {
        return id.equals(this.advancement);
    }

    @Override
    public int requiredCount() {
        return 1;
    }

    @Override
    public String displayName() {
        return "Earn advancement " + advancement;
    }

    @Override
    public Component display() {
        return Component.literal(displayName());
    }
}
