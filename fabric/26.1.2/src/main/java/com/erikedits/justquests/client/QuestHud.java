package com.erikedits.justquests.client;

import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.objective.QuestObjective;
import com.erikedits.justquests.network.ClientQuestData;
import com.erikedits.justquests.player.QuestProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
 * HUD button; hidden with F1, F3 and while a screen is open. Drawn with fills, not a texture, so the
 * panel can grow with its text (the look matches hud_panel from the v2-full set).
 */
public final class QuestHud {
    private static final int MAX_OBJECTIVES = 3, MAX_TEXT = 140, LINE = 9;
    private static final int TITLE = 0xFFFFFFFF, OPEN = 0xFFD0D0D0, DONE = 0xFF55FF55, MORE = 0xFFA0A0A0, GOLD = 0xFFFFAA00;

    private static int prunedAt = -1;

    private QuestHud() {}

    /** One quest block: icon, title and its objective lines. */
    private record Block(ItemStack icon, String title, int titleColor, List<String> lines, List<Integer> colors) {}

    public static void toggle() {
        ClientSettings.load();
        ClientSettings.hud = !ClientSettings.hud;
        ClientSettings.save();
        Minecraft.getInstance().gui.setOverlayMessage(Component.translatable(ClientSettings.hud ? "justquests.hud.on" : "justquests.hud.off"), false);
    }

    public static void render(GuiGraphicsExtractor g) {
        ClientSettings.load();
        Minecraft mc = Minecraft.getInstance();
        if (!ClientSettings.hud || mc.player == null || mc.options.hideGui || mc.screen != null || debugShown(mc)) return;
        draw(g, false);
    }

    /** The tracker drawn by the quest book while its corner is picked; shows a sample panel without quests. */
    public static void preview(GuiGraphicsExtractor g) {
        if (Minecraft.getInstance().player != null) draw(g, true);
    }

    private static void draw(GuiGraphicsExtractor g, boolean preview) {
        Minecraft mc = Minecraft.getInstance();
        PlayerQuestData data = ClientQuestData.getData();
        if (data == null) return;
        if (!preview && data.active.isEmpty() && data.pendingClaim.isEmpty()) return;
        Font font = mc.font;
        String lang = mc.options.languageCode;
        prunePins();

        // pinned quests in pin order; without pins the newest active ones
        List<Identifier> pins = new ArrayList<>();
        for (String p : ClientSettings.pinned) {
            for (Identifier id : data.active.keySet()) {
                if (id.toString().equals(p)) pins.add(id);
            }
        }
        List<Block> blocks = new ArrayList<>();
        int textW = 0;
        // finished quests whose rewards wait: one block on top, so they are not forgotten
        int ready = 0;
        for (Identifier id : data.pendingClaim.keySet()) {
            if (ClientQuestData.get(id) != null) ready++;
        }
        if (ready > 0) {
            String title = fit(font, I18n.get("justquests.hud.rewards", ready));
            String hint = fit(font, I18n.get("justquests.hud.rewards_hint"));
            textW = Math.max(font.width(title), font.width(hint));
            blocks.add(new Block(new ItemStack(Items.CHEST), title, GOLD, List.of(hint), List.of(MORE)));
        }
        int shown = 0;
        for (Identifier id : pins.isEmpty() ? ClientQuestData.activeOrder() : pins) {
            if (shown >= ClientSettings.hudMax) break;
            Quest q = ClientQuestData.get(id);
            QuestProgress prog = data.active.get(id);
            if (q == null || prog == null) continue;
            String title = fit(font, q.title().get(lang));
            textW = Math.max(textW, font.width(title));
            List<String> lines = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();
            List<QuestObjective> objs = q.objectives();
            for (int i = 0; i < objs.size() && i < MAX_OBJECTIVES; i++) {
                int need = objs.get(i).requiredCount();
                int cur = Math.min(prog.get(i), need);
                boolean done = cur >= need;
                String line = fit(font, (done ? "✓ " : cur + "/" + need + " ") + QuestIcons.label(objs.get(i)).getString());
                lines.add(line);
                colors.add(done ? DONE : OPEN);
                textW = Math.max(textW, font.width(line));
            }
            if (objs.size() > MAX_OBJECTIVES) {
                lines.add(I18n.get("justquests.hud.more", objs.size() - MAX_OBJECTIVES));
                colors.add(MORE);
            }
            blocks.add(new Block(QuestIcons.of(id, q), title, TITLE, lines, colors));
            shown++;
        }
        if (blocks.isEmpty()) {
            if (!preview) return;
            String title = fit(font, I18n.get("justquests.hud.preview"));
            String line = fit(font, I18n.get("justquests.hud.preview_empty"));
            textW = Math.max(font.width(title), font.width(line));
            blocks.add(new Block(new ItemStack(Items.BOOK), title, TITLE, List.of(line), List.of(MORE)));
        }

        int w = 23 + textW + 5;
        int h = 4;
        for (Block b : blocks) h += height(b);
        h += 1;
        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        boolean right = ClientSettings.hudCorner.endsWith("right"), bottom = ClientSettings.hudCorner.startsWith("bottom");
        int x = right ? sw - w - 4 : 4;
        int y = bottom ? sh - h - 4 : 4;
        if (bottom && !right) y -= 40;   // stay above the chat input line

        panel(g, x, y, w, h);
        int cy = y + 4;
        for (Block b : blocks) {
            g.item(b.icon(), x + 4, cy);
            g.text(font, b.title(), x + 23, cy + 1, b.titleColor(), true);
            int ly = cy + 11;
            for (int i = 0; i < b.lines().size(); i++) {
                g.text(font, b.lines().get(i), x + 23, ly, b.colors().get(i), true);
                ly += LINE;
            }
            cy += height(b);
        }
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

    /** Height of one quest block: icon or title + objective lines, plus a gap. */
    private static int height(Block b) {
        return Math.max(18, 11 + b.lines().size() * LINE) + 3;
    }

    /** Translucent black body, black outline with cut corners and a faint top highlight. */
    private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        int x2 = x + w, y2 = y + h;
        g.fill(x + 1, y + 1, x2 - 1, y2 - 1, 0xAA000000);
        g.fill(x + 1, y, x2 - 1, y + 1, 0xFF000000);
        g.fill(x + 1, y2 - 1, x2 - 1, y2, 0xFF000000);
        g.fill(x, y + 1, x + 1, y2 - 1, 0xFF000000);
        g.fill(x2 - 1, y + 1, x2, y2 - 1, 0xFF000000);
        g.fill(x + 2, y + 1, x2 - 2, y + 2, 0x78FFFFFF);
    }

    private static String fit(Font font, String s) {
        if (font.width(s) <= MAX_TEXT) return s;
        return font.plainSubstrByWidth(s, MAX_TEXT - font.width("...")) + "...";
    }

    private static boolean debugShown(Minecraft mc) {
        return mc.getDebugOverlay().showDebugScreen();
    }
}
