package com.erikedits.justquests.client;

import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.QuestManager;
import com.erikedits.justquests.data.objective.QuestObjective;
import com.erikedits.justquests.data.reward.QuestReward;
import com.erikedits.justquests.player.QuestProgress;
import com.erikedits.justquests.storage.WorldQuestStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Interim quest-book screen (v0.2). Vanilla-grey look from stable GUI
 * primitives so it works across MC versions; reads quest data directly
 * (singleplayer) and runs /quest accept|abandon for actions. The final
 * design comes from the community vote.
 */
public class QuestScreen extends Screen {
    private static final int FACE = 0xFFC6C6C6, INSET = 0xFF8B8B8B, BLACK = 0xFF000000;
    private static final int W = 320, H = 200, PER_PAGE = 8, ROW_H = 18;

    private final List<Map.Entry<ResourceLocation, Quest>> quests = new ArrayList<>();
    private ResourceLocation selected;
    private int page = 0, left, top;

    public QuestScreen() {
        super(Component.literal("Quests"));
    }

    private String lang() {
        return Minecraft.getInstance().options.languageCode;
    }

    private PlayerQuestData data() {
        WorldQuestStore s = WorldQuestStore.get();
        return (s != null && minecraft != null && minecraft.player != null)
            ? s.peek(minecraft.player.getUUID()) : null;
    }

    @Override
    protected void init() {
        left = (this.width - W) / 2;
        top = (this.height - H) / 2;
        if (quests.isEmpty()) {
            quests.addAll(QuestManager.INSTANCE.getQuests().entrySet());
            quests.sort(Comparator
                .comparing((Map.Entry<ResourceLocation, Quest> e) -> e.getValue().category(), String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(e -> e.getValue().sort())
                .thenComparing(e -> e.getKey().toString()));
        }
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        int listX = left + 8, listY = top + 24;
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < quests.size(); i++) {
            Map.Entry<ResourceLocation, Quest> e = quests.get(start + i);
            ResourceLocation id = e.getKey();
            String title = e.getValue().title().get(lang());
            addRenderableWidget(Button.builder(Component.literal(title), b -> { selected = id; rebuild(); })
                .bounds(listX, listY + i * ROW_H, 150, ROW_H - 2).build());
        }
        int navY = listY + PER_PAGE * ROW_H;
        if (page > 0) addRenderableWidget(Button.builder(Component.literal("<"),
            b -> { page--; rebuild(); }).bounds(listX, navY, 20, 16).build());
        if (start + PER_PAGE < quests.size()) addRenderableWidget(Button.builder(Component.literal(">"),
            b -> { page++; rebuild(); }).bounds(listX + 130, navY, 20, 16).build());

        if (selected != null) {
            PlayerQuestData d = data();
            int bx = left + 172, by = top + H - 26;
            boolean active = d != null && d.isActive(selected);
            boolean completed = d != null && d.isCompleted(selected);
            Quest q = QuestManager.INSTANCE.get(selected);
            boolean repeatable = q != null && q.repeatable();
            if (active) {
                addRenderableWidget(Button.builder(Component.literal("Abandon"),
                    b -> send("quest abandon " + selected)).bounds(bx, by, 90, 20).build());
            } else if (!completed || repeatable) {
                addRenderableWidget(Button.builder(Component.literal("Accept"),
                    b -> send("quest accept " + selected)).bounds(bx, by, 90, 20).build());
            }
        }
        addRenderableWidget(Button.builder(Component.literal("Close"),
            b -> onClose()).bounds(left + W - 58, top + 4, 50, 16).build());
    }

    private void send(String cmd) {
        if (minecraft != null && minecraft.getConnection() != null) {
            minecraft.getConnection().sendCommand(cmd);
        }
        rebuild();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        this.renderBackground(g);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, BLACK);
        g.fill(left, top, left + W, top + H, FACE);
        g.fill(left + 6, top + 22, left + 162, top + H - 6, INSET);
        g.fill(left + 166, top + 22, left + W - 6, top + H - 6, INSET);
        g.drawString(this.font, Component.literal("§lQuests"), left + 8, top + 8, 0x404040, false);

        super.render(g, mouseX, mouseY, pt);

        int dx = left + 172, dy = top + 26;
        if (quests.isEmpty()) {
            g.drawString(this.font, Component.literal("No quests loaded (singleplayer only for now)."),
                left + 10, dy, 0xAAAAAA, false);
            return;
        }
        if (selected == null) {
            g.drawString(this.font, Component.literal("Select a quest on the left."), dx, dy, 0xAAAAAA, false);
            return;
        }
        Quest q = QuestManager.INSTANCE.get(selected);
        if (q == null) return;
        g.drawString(this.font, Component.literal(q.title().get(lang())), dx, dy, 0xFFFFFF, false);
        dy += 12;
        String desc = q.description().get(lang());
        if (!desc.isBlank()) {
            for (var line : this.font.split(Component.literal("§7" + desc), W - 184)) {
                g.drawString(this.font, line, dx, dy, 0xFFFFFF, false);
                dy += 10;
            }
        }
        dy += 4;
        PlayerQuestData d = data();
        QuestProgress prog = d != null ? d.active.get(selected) : null;
        g.drawString(this.font, Component.literal("§6Objectives:"), dx, dy, 0xFFFFFF, false);
        dy += 11;
        List<QuestObjective> objs = q.objectives();
        for (int i = 0; i < objs.size(); i++) {
            int need = objs.get(i).requiredCount();
            int cur = prog != null ? Math.min(prog.get(i), need) : 0;
            String pfx = (prog != null && cur >= need) ? "§a✓ " : "§7" + cur + "/" + need + " ";
            g.drawString(this.font, Component.literal(pfx).append(objs.get(i).display()), dx + 4, dy, 0xCCCCCC, false);
            dy += 10;
        }
        dy += 2;
        g.drawString(this.font, Component.literal("§6Rewards:"), dx, dy, 0xFFFFFF, false);
        dy += 11;
        for (QuestReward r : q.rewards()) {
            g.drawString(this.font, Component.literal("§a").append(r.display()), dx + 4, dy, 0xAAFFAA, false);
            dy += 10;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
