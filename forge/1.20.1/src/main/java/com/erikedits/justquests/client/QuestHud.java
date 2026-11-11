package com.erikedits.justquests.client;

import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.objective.QuestObjective;
import com.erikedits.justquests.network.ClientQuestData;
import com.erikedits.justquests.player.QuestProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The quest tracker: a small translucent panel in a screen corner listing the player's pinned
 * quests, or without pins the most recently accepted ones, with their objectives (and, on top, how
 * many finished quests have rewards waiting). Toggled with H or the quest book's
 * HUD button; hidden with F1, F3 and while a screen is open. Drawn with fills (the look of hud_panel
 * from the v2-full set), or with a resource pack's tracker/tracker.png stretched like a frame, so the
 * panel can grow with its text. Places, colours and an optional progress bar per goal come from
 * tracker/settings.json (0.5.0).
 */
public final class QuestHud {
    private static int prunedAt = -1;

    private QuestHud() {}

    /**
     * One quest block: icon (an item, or a quest with its own picture), title and its objective lines,
     * each with how far it is (-1 for a line without a bar).
     */
    private record Block(ItemStack icon, ResourceLocation quest, Quest q, String title, int titleColor,
                         List<String> lines, List<Integer> colors, List<Float> progress) {}

    public static void toggle() {
        ClientSettings.load();
        ClientSettings.hud = !ClientSettings.hud;
        ClientSettings.save();
        Minecraft.getInstance().gui.setOverlayMessage(Component.translatable(ClientSettings.hud ? "justquests.hud.on" : "justquests.hud.off"), false);
    }

    public static void render(GuiGraphics g) {
        ClientSettings.load();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null || debugShown(mc)) return;
        if (ClientSettings.hud) draw(g, false);
        Popups.render(g);
    }

    /** The tracker drawn by the quest book while its corner is picked; shows a sample panel without quests. */
    public static void preview(GuiGraphics g) {
        if (Minecraft.getInstance().player != null) draw(g, true);
    }

    private static void draw(GuiGraphics g, boolean preview) {
        Minecraft mc = Minecraft.getInstance();
        PlayerQuestData data = ClientQuestData.getData();
        if (data == null) return;
        if (!preview && data.active.isEmpty() && data.pendingClaim.isEmpty()) return;
        Font font = mc.font;
        String lang = mc.options.languageCode;
        Theme t = Theme.get();
        prunePins();

        // pinned quests in pin order; without pins the newest active ones
        List<ResourceLocation> pins = new ArrayList<>();
        for (String p : ClientSettings.pinned) {
            for (ResourceLocation id : data.active.keySet()) {
                if (id.toString().equals(p)) pins.add(id);
            }
        }
        List<Block> blocks = new ArrayList<>();
        int textW = 0;
        // finished quests whose rewards wait: one block on top, so they are not forgotten
        int ready = 0;
        for (ResourceLocation id : data.pendingClaim.keySet()) {
            if (ClientQuestData.get(id) != null) ready++;
        }
        if (ready > 0) {
            String title = fit(font, I18n.get("justquests.hud.rewards", ready));
            String hint = fit(font, I18n.get("justquests.hud.rewards_hint"));
            textW = Math.max(width(font, title), width(font, hint));
            blocks.add(new Block(new ItemStack(Items.CHEST), null, null, title, t.tClaim, List.of(hint), List.of(t.tMore), List.of(-1f)));
        }
        int shown = 0;
        for (ResourceLocation id : pins.isEmpty() ? ClientQuestData.activeOrder() : pins) {
            if (shown >= ClientSettings.hudMax) break;
            Quest q = ClientQuestData.get(id);
            QuestProgress prog = data.active.get(id);
            if (q == null || prog == null) continue;
            String title = fit(font, q.title().get(lang));
            textW = Math.max(textW, width(font, title));
            List<String> lines = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();
            List<Float> progress = new ArrayList<>();
            List<QuestObjective> objs = q.objectives();
            List<Integer> order = q.shownOrder();
            for (int n = 0; n < order.size() && n < t.tMaxGoals; n++) {
                int i = order.get(n);
                int need = objs.get(i).requiredCount();
                int cur = Math.min(prog.get(i), need);
                boolean done = cur >= need;
                // an alternative of the goal above it reads "or ..."
                String or = q.orBefore(i) ? I18n.get("justquests.goal.or") + " " : "";
                String line = fit(font, or + (done ? "✓ " : cur + "/" + need + " ") + QuestIcons.label(q, i).getString());
                lines.add(line);
                colors.add(done ? t.tDone : t.tGoal);
                progress.add(need > 0 ? cur / (float) need : 1f);
                textW = Math.max(textW, width(font, line));
            }
            if (objs.size() > t.tMaxGoals) {
                lines.add(I18n.get("justquests.hud.more", objs.size() - t.tMaxGoals));
                colors.add(t.tMore);
                progress.add(-1f);
            }
            blocks.add(new Block(ItemStack.EMPTY, id, q, title, t.tTitle, lines, colors, progress));
            shown++;
        }
        if (blocks.isEmpty()) {
            if (!preview) return;
            String title = fit(font, I18n.get("justquests.hud.preview"));
            String line = fit(font, I18n.get("justquests.hud.preview_empty"));
            textW = Math.max(width(font, title), width(font, line));
            blocks.add(new Block(new ItemStack(Items.BOOK), null, null, title, t.tTitle, List.of(line), List.of(t.tMore), List.of(-1f)));
        }

        // block places are counted from the top left of the box, as in tracker/settings.json
        int w = t.tTitleX + textW + 5;
        int h = 0;
        for (Block b : blocks) h += height(t, b);
        h += 5;
        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        boolean right = ClientSettings.hudCorner.endsWith("right"), bottom = ClientSettings.hudCorner.startsWith("bottom");
        int x = right ? sw - w - t.tOffX : t.tOffX;
        int y = bottom ? sh - h - t.tOffY : t.tOffY;
        // a minimap in the same corner draws over the tracker: the tracker goes below it (#24)
        if (!bottom && Minimaps.at(right ? "top_right" : "top_left")) y = Math.max(y, Minimaps.below(right ? "top_right" : "top_left", sh));
        if (bottom && !right) y -= 40;   // stay above the chat input line

        if (t.has("tracker/tracker")) t.frame(g, "tracker/tracker", x, y, w, h, t.tBorder);
        else panel(g, x, y, w, h, t.tBox);
        int by = y;
        for (Block b : blocks) {
            if (b.quest() != null) t.icon(g, b.quest(), b.q(), x + t.tIconX, by + t.tIconY);
            else g.renderItem(b.icon(), x + t.tIconX, by + t.tIconY);
            g.drawString(font, Theme.text(b.title()), x + t.tTitleX, by + t.tTitleY, b.titleColor(), t.tShadow);
            int ly = by + t.tGoalY;
            for (int i = 0; i < b.lines().size(); i++) {
                g.drawString(font, Theme.text(b.lines().get(i)), x + t.tGoalX, ly, b.colors().get(i), t.tShadow);
                ly += t.tLine;
                float f = b.progress().get(i);
                if (t.tBar && f >= 0) {
                    int bw = t.trackerBarW > 0 ? t.trackerBarW : textW;
                    t.progress(g, true, x + t.tGoalX, ly - 1, bw, t.trackerBarH, f);
                    ly += t.trackerBarH + 1;
                }
            }
            by += height(t, b);
        }
    }

    private static int width(Font font, String s) {
        return font.width(Theme.text(s));
    }

    /** Unpin quests that are loaded but no longer active (finished or abandoned); once per sync. */
    static void prunePins() {
        int s = ClientQuestData.syncCount();
        if (s == prunedAt) return;
        prunedAt = s;
        if (ClientSettings.pinned.isEmpty() || ClientQuestData.getQuests().isEmpty()) return;
        Set<String> known = new HashSet<>(), active = new HashSet<>();
        ClientQuestData.getQuests().keySet().forEach(id -> known.add(id.toString()));
        ClientQuestData.getData().active.keySet().forEach(id -> active.add(id.toString()));
        if (ClientSettings.pinned.removeIf(p -> known.contains(p) && !active.contains(p))) ClientSettings.save();
    }

    /**
     * Height of one quest block: icon or title + objective lines (and their bars), plus a gap. The
     * box's top margin (the icon's y) counts once for the whole box, not per block.
     */
    private static int height(Theme t, Block b) {
        int lines = 0;
        for (float f : b.progress()) lines += t.tLine + (t.tBar && f >= 0 ? t.trackerBarH + 1 : 0);
        return Math.max(t.tIconY + 18, t.tGoalY + lines) + 3 - t.tIconY;
    }

    /** Translucent body, black outline with cut corners and a faint top highlight. */
    private static void panel(GuiGraphics g, int x, int y, int w, int h, int body) {
        int x2 = x + w, y2 = y + h;
        g.fill(x + 1, y + 1, x2 - 1, y2 - 1, body);
        g.fill(x + 1, y, x2 - 1, y + 1, 0xFF000000);
        g.fill(x + 1, y2 - 1, x2 - 1, y2, 0xFF000000);
        g.fill(x, y + 1, x + 1, y2 - 1, 0xFF000000);
        g.fill(x2 - 1, y + 1, x2, y2 - 1, 0xFF000000);
        g.fill(x + 2, y + 1, x2 - 2, y + 2, 0x78FFFFFF);
    }

    private static String fit(Font font, String s) {
        int max = Theme.get().tMaxText;
        if (width(font, s) <= max) return s;
        return font.getSplitter().plainHeadByWidth(s, max - width(font, "..."), Theme.style()) + "...";
    }

    private static boolean debugShown(Minecraft mc) {
        return mc.options.renderDebug;
    }
}
