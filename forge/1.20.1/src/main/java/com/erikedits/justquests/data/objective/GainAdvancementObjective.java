package com.erikedits.justquests.data.objective;

import com.erikedits.justquests.text.Msg;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Earn a specific advancement. */
public record GainAdvancementObjective(ResourceLocation advancement) implements QuestObjective {
    public static final String TYPE_ID = "justquests:gain_advancement";

    /** Advancement titles from the last quest sync, for a client on a server (it knows only the advancements it was shown). */
    private static final Map<ResourceLocation, Component> SYNCED = new ConcurrentHashMap<>();

    public static final MapCodec<GainAdvancementObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ResourceLocation.CODEC.fieldOf("advancement").forGetter(GainAdvancementObjective::advancement)
    ).apply(instance, GainAdvancementObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    public boolean matches(ResourceLocation id) {
        return id.equals(this.advancement);
    }

    /** Whether the player has the advancement already - one earned before the quest was taken counts too. */
    public boolean earnedBy(net.minecraft.server.level.ServerPlayer player) {
        var adv = player.level().getServer().getAdvancements().getAdvancement(advancement);
        return adv != null && player.getAdvancements().getOrStartProgress(adv).isDone();
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
        return Msg.tr("justquests.goal.advancement", name());
    }

    /** The advancement's title, or its id made readable ("Mine Stone") when it has no title or is unknown. */
    public Component name() {
        MinecraftServer server = com.erikedits.justquests.generator.GenV2.server();
        Component title = server != null ? title(server, advancement) : SYNCED.get(advancement);
        return title != null ? title : Msg.lit(readable(advancement.getPath()));
    }

    /** The title an advancement shows in the advancements screen, null if it has none (or does not exist). */
    private static Component title(MinecraftServer server, ResourceLocation id) {
        var adv = server.getAdvancements().getAdvancement(id);
        return adv == null || adv.getDisplay() == null ? null : adv.getDisplay().getTitle();
    }

    private static String readable(String path) {
        StringBuilder out = new StringBuilder();
        for (String word : Msg.pretty(path).split(" ")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    /** For the quest sync: {id: {"key": translation key, "text": title}} for every advancement the quests name. */
    public static JsonObject titlesJson(MinecraftServer server, Iterable<com.erikedits.justquests.data.Quest> quests) {
        JsonObject out = new JsonObject();
        for (com.erikedits.justquests.data.Quest q : quests) {
            for (QuestObjective o : q.objectives()) {
                if (!(o instanceof GainAdvancementObjective g) || out.has(g.advancement().toString())) continue;
                Component title = title(server, g.advancement());
                if (title == null) continue;
                JsonObject t = new JsonObject();
                if (title.getContents() instanceof TranslatableContents tr) t.addProperty("key", tr.getKey());
                t.addProperty("text", title.getString());
                out.add(g.advancement().toString(), t);
            }
        }
        return out;
    }

    /** On the client: the titles from a quest sync, translated where this client knows the key. */
    public static void readTitles(JsonObject titles) {
        SYNCED.clear();
        for (Map.Entry<String, JsonElement> e : titles.entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(e.getKey());
            if (id == null || !e.getValue().isJsonObject()) continue;
            JsonObject t = e.getValue().getAsJsonObject();
            String key = t.has("key") ? t.get("key").getAsString() : null;
            String text = t.has("text") ? t.get("text").getAsString() : readable(id.getPath());
            SYNCED.put(id, key != null && net.minecraft.locale.Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(text));
        }
    }
}
