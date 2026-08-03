package com.erikedits.justquests.client;

import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.objective.QuestObjective;
import com.erikedits.justquests.data.reward.QuestReward;
import com.erikedits.justquests.player.QuestProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Interim quest-book screen (v0.2), rendered from the JustQuests v2-full
 * texture set (no vanilla widgets — manual blit + click hit-testing so the
 * look is fully custom). Reads quest data directly (singleplayer) and runs
 * /quest accept|abandon for actions. Textures live in
 * assets/justquests/textures/gui/. Fixed 248x184 window.
 */
public class QuestScreen extends Screen {
    private static final int W = 248, H = 184;
    private static final int PER_PAGE = 7, ROW_W = 80, ROW_H = 18;
    // darker text reads clearly on the light-grey panes
    private static final int TITLE_DARK = 0x161616, TEXT = 0x282828, MUTED = 0x4C4C4C, HEAD = 0x24395C;

    private final List<Map.Entry<ResourceLocation, Quest>> quests = new ArrayList<>();
    private ResourceLocation selected;
    private int page = 0, left, top;

    public QuestScreen() {
        super(Component.literal("Quests"));
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation("justquests", "textures/gui/" + name + ".png");
    }

    /** Draw a whole texture PNG (size natW x natH) at (x,y). */
    private void blit(GuiGraphics g, String name, int x, int y, int natW, int natH) {
        blitPart(g, name, x, y, natW, natH, natW, natH);
    }

    /**
     * Draw the top-left w x h region of a texW x texH texture. This is the ONLY
     * place that calls GuiGraphics.blit, so porting to another MC version only
     * needs this one line changed (the blit signature shifts at 1.21.2/1.21.4).
     */
    private void blitPart(GuiGraphics g, String name, int x, int y, int w, int h, int texW, int texH) {
        g.blit(tex(name), x, y, 0, 0, w, h, texW, texH);
    }

    private String lang() {
        return Minecraft.getInstance().options.languageCode;
    }

    private PlayerQuestData data() {
        // client-side synced copy (works on servers and in singleplayer)
        return com.erikedits.justquests.network.ClientQuestData.getData();
    }

    @Override
    protected void init() {
        left = (this.width - W) / 2;
        top = (this.height - H) / 2;
        if (quests.isEmpty()) {
            quests.addAll(com.erikedits.justquests.network.ClientQuestData.getQuests().entrySet());
            quests.sort(Comparator
                .comparing((Map.Entry<ResourceLocation, Quest> e) -> e.getValue().category(), String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(e -> e.getValue().sort())
                .thenComparing(e -> e.getKey().toString()));
        }
    }

    // --- geometry helpers ---
    private int listX() { return left + 8; }
    private int listY() { return top + 26; }
    private int rowY(int i) { return listY() + i * ROW_H; }
    private int prevX() { return left + 8; }
    private int nextX() { return left + 8 + ROW_W - 12; }
    private int navY() { return top + H - 20; }
    private int closeX() { return left + W - 20; }
    private int closeY() { return top + 6; }
    private int detailX() { return left + 100; }
    private int detailW() { return W - 100 - 8; }

    /** Truncate to width with an ellipsis so titles never run past their row. */
    private String fit(String s, int maxW) {
        if (this.font.width(s) <= maxW) return s;
        return this.font.plainSubstrByWidth(s, maxW - this.font.width("...")) + "...";
    }
    private int actionX() { return detailX(); }
    private int actionY() { return top + H - 28; }
    private boolean hasPrev() { return page > 0; }
    private boolean hasNext() { return (page + 1) * PER_PAGE < quests.size(); }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        this.renderBackground(g);
        blit(g, "window", left, top, W, H);
        g.drawString(this.font, Component.literal("Quests"), left + 9, closeY() + 1, TITLE_DARK, false);

        // close button
        blit(g, in(mouseX, mouseY, closeX(), closeY(), 11, 11) ? "button_close_hover" : "button_close_normal",
            closeX(), closeY(), 11, 11);

        // quest list
        if (quests.isEmpty()) {
            g.drawString(this.font, Component.literal("No quests available yet."),
                listX(), listY(), MUTED, false);
        } else {
            PlayerQuestData d = data();
            int start = page * PER_PAGE;
            for (int i = 0; i < PER_PAGE && start + i < quests.size(); i++) {
                Map.Entry<ResourceLocation, Quest> e = quests.get(start + i);
                ResourceLocation id = e.getKey();
                int ry = rowY(i);
                boolean hov = in(mouseX, mouseY, listX(), ry, ROW_W, ROW_H);
                String state = id.equals(selected) ? "selected"
                    : (d != null && d.isCompleted(id)) ? "completed"
                    : (d != null && d.isActive(id)) ? "active"
                    : hov ? "hover" : "available";
                blit(g, "quest_row_" + state, listX(), ry, ROW_W, ROW_H);
                boolean hasGlyph = state.equals("completed") || state.equals("active") || state.equals("claimable");
                String title = fit(e.getValue().title().get(lang()), ROW_W - (hasGlyph ? 18 : 8));
                g.drawString(this.font, title, listX() + 5, ry + 5, TITLE_DARK, false);
            }
            // page arrows
            blit(g, hasPrev() ? (in(mouseX, mouseY, prevX(), navY(), 12, 12) ? "page_prev_hover" : "page_prev_normal") : "page_prev_disabled",
                prevX(), navY(), 12, 12);
            blit(g, hasNext() ? (in(mouseX, mouseY, nextX(), navY(), 12, 12) ? "page_next_hover" : "page_next_normal") : "page_next_disabled",
                nextX(), navY(), 12, 12);
        }

        renderDetail(g, mouseX, mouseY);
    }

    private void renderDetail(GuiGraphics g, int mouseX, int mouseY) {
        int dx = detailX(), dy = top + 27, dw = detailW();
        if (selected == null) {
            g.drawString(this.font, Component.literal("Select a quest"), dx, dy, MUTED, false);
            g.drawString(this.font, Component.literal("on the left."), dx, dy + 11, MUTED, false);
            return;
        }
        Quest q = com.erikedits.justquests.network.ClientQuestData.get(selected);
        if (q == null) return;
        PlayerQuestData d = data();
        QuestProgress prog = d != null ? d.active.get(selected) : null;

        for (var line : this.font.split(Component.literal(q.title().get(lang())), dw)) {
            g.drawString(this.font, line, dx, dy, TITLE_DARK, false); dy += 10;
        }
        dy += 2;
        String desc = q.description().get(lang());
        if (!desc.isBlank()) {
            for (var line : this.font.split(Component.literal(desc), dw)) {
                g.drawString(this.font, line, dx, dy, MUTED, false); dy += 9;
            }
        }
        dy += 3;
        g.drawString(this.font, Component.literal("Objectives"), dx, dy, HEAD, false); dy += 11;
        List<QuestObjective> objs = q.objectives();
        for (int i = 0; i < objs.size() && dy < actionY() - 12; i++) {
            int need = objs.get(i).requiredCount();
            int cur = prog != null ? Math.min(prog.get(i), need) : 0;
            boolean done = cur >= need;
            g.drawString(this.font, this.font.plainSubstrByWidth((done ? "✓ " : cur + "/" + need + " ")
                + objs.get(i).display().getString(), dw), dx, dy, done ? 0x2E7D32 : TEXT, false);
            dy += 10;
            // progress bar
            int barW = Math.min(100, dw);
            blit(g, "progress_track", dx, dy, barW, 6);
            int fillW = need > 0 ? (int) (barW * (cur / (float) need)) : 0;
            if (fillW > 0) blitPart(g, "progress_fill", dx, dy, fillW, 6, 100, 6);
            dy += 9;
        }
        dy += 2;
        if (dy < actionY() - 10) {
            g.drawString(this.font, Component.literal("Rewards"), dx, dy, HEAD, false); dy += 11;
            for (QuestReward r : q.rewards()) {
                if (dy >= actionY() - 2) break;
                g.drawString(this.font, this.font.plainSubstrByWidth(r.display().getString(), dw), dx, dy, TEXT, false);
                dy += 10;
            }
        }

        // accept / abandon button
        boolean active = d != null && d.isActive(selected);
        boolean completed = d != null && d.isCompleted(selected);
        boolean repeatable = q.repeatable();
        if (active) {
            blit(g, in(mouseX, mouseY, actionX(), actionY(), 72, 20) ? "button_abandon_hover" : "button_abandon_normal",
                actionX(), actionY(), 72, 20);
            g.drawString(this.font, Component.literal("Abandon"), actionX() + 16, actionY() + 6, TEXT, false);
        } else if (!completed || repeatable) {
            blit(g, in(mouseX, mouseY, actionX(), actionY(), 72, 20) ? "button_claim_hover" : "button_claim_normal",
                actionX(), actionY(), 72, 20);
            g.drawString(this.font, Component.literal("Accept"), actionX() + 20, actionY() + 6, TEXT, false);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (in(mx, my, closeX(), closeY(), 11, 11)) { onClose(); return true; }
            if (!quests.isEmpty()) {
                if (hasPrev() && in(mx, my, prevX(), navY(), 12, 12)) { page--; return true; }
                if (hasNext() && in(mx, my, nextX(), navY(), 12, 12)) { page++; return true; }
                int start = page * PER_PAGE;
                for (int i = 0; i < PER_PAGE && start + i < quests.size(); i++) {
                    if (in(mx, my, listX(), rowY(i), ROW_W, ROW_H)) {
                        selected = quests.get(start + i).getKey();
                        return true;
                    }
                }
            }
            if (selected != null && in(mx, my, actionX(), actionY(), 72, 20)) {
                PlayerQuestData d = data();
                Quest q = com.erikedits.justquests.network.ClientQuestData.get(selected);
                boolean active = d != null && d.isActive(selected);
                boolean completed = d != null && d.isCompleted(selected);
                boolean repeatable = q != null && q.repeatable();
                if (active) send("quest abandon " + selected);
                else if (!completed || repeatable) send("quest accept " + selected);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void send(String cmd) {
        if (minecraft != null && minecraft.getConnection() != null) {
            minecraft.getConnection().sendCommand(cmd);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
