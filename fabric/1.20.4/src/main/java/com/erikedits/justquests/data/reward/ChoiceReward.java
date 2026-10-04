package com.erikedits.justquests.data.reward;

import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.text.Msg;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The player picks one of several rewards when claiming the quest. Quests with a choice always
 * wait to be claimed (also with claimRewards off), since nobody else can pick. Without a pick the
 * first option is given. A quest has one choice; if a pack gives several, the others give their
 * first option.
 */
public record ChoiceReward(List<QuestReward> options) implements QuestReward {
    public static final String TYPE_ID = "justquests:choice";

    /** The options are rewards themselves: read through QuestReward.CODEC when used, not at class init. */
    private static final Codec<QuestReward> OPTION = new Codec<>() {
        @Override
        public <T> DataResult<Pair<QuestReward, T>> decode(DynamicOps<T> ops, T input) {
            return QuestReward.CODEC.decode(ops, input);
        }

        @Override
        public <T> DataResult<T> encode(QuestReward input, DynamicOps<T> ops, T prefix) {
            return QuestReward.CODEC.encode(input, ops, prefix);
        }
    };

    public static final MapCodec<ChoiceReward> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        OPTION.listOf().fieldOf("options").forGetter(ChoiceReward::options)
    ).apply(instance, ChoiceReward::new));

    /** The quest's choice (the first one, if a pack gives several), or null. */
    public static ChoiceReward of(Quest quest) {
        for (QuestReward reward : quest.rewards()) {
            if (reward instanceof ChoiceReward c && !c.options().isEmpty()) return c;
        }
        return null;
    }

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    /** Without a pick: the first option. */
    @Override
    public void grant(ServerPlayer player) {
        grant(player, 0);
    }

    /** Gives option {@code index} (0-based); an index out of range gives the first option. */
    public void grant(ServerPlayer player, int index) {
        if (options.isEmpty()) return;
        options.get(index >= 0 && index < options.size() ? index : 0).grant(player);
    }

    @Override
    public String displayName() {
        return "one of: " + options.stream().map(QuestReward::displayName).collect(Collectors.joining(" / "));
    }

    @Override
    public Component display() {
        MutableComponent list = Msg.empty();
        for (int i = 0; i < options.size(); i++) {
            if (i > 0) list.append(Msg.lit(" / "));
            list.append(options.get(i).display());
        }
        return Msg.tr("justquests.reward.choice", list);
    }
}
