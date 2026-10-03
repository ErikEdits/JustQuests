package com.erikedits.justquests.client;

import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.objective.QuestObjective;
import com.erikedits.justquests.network.ClientQuestData;
import com.erikedits.justquests.player.QuestProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The quest tracker: a small translucent panel in a screen corner listing the player's active
 * quests (most recently accepted first) with their objectives. Toggled with H or the quest book's
 * HUD button; hidden with F1, F3 and while a screen is open. Drawn with fills, not a texture, so the
 * panel can grow with its text (the look matches hud_panel from the v2-full set).
 */
public final class QuestHud {
    private static final int MAX_OBJECTIVES = 3, MAX_TEXT = 140, LINE = 9;
    private static final int TITLE = 0xFFFFFFFF, OPEN = 0xFFD0D0D0, DONE = 0xFF55FF55, MORE = 0xFFA0A0A0;

    private QuestHud() {}

    /** One quest block: icon, title and its objective lines. */
    private record Block(ItemStack icon, String title, List<String> lines, List<Integer> colors) {}

    public static void toggle() {
        ClientSettings.load();
        ClientSettings.hud = !ClientSettings.hud;
        ClientSettings.save();
        Minecraft.getInstance().gui.setOverlayMessage(Component.literal(ClientSettings.hud ? "Quest HUD on" : "Quest HUD off"), false);
    }

    public static void render(GuiGraphics g) {
        ClientSettings.load();
        Minecraft mc = Minecraft.getInstance();
        if (!ClientSettings.hud || mc.player == null || mc.options.hideGui || mc.screen != null || debugShown(mc)) return;
        PlayerQuestData data = ClientQuestData.getData();
        if (data == null || data.active.isEmpty()) return;
        Font font = mc.font;
        String lang = mc.options.languageCode;

        List<Block> blocks = new ArrayList<>();
        int textW = 0;
        for (ResourceLocation id : ClientQuestData.activeOrder()) {
            if (blocks.size() >= ClientSettings.hudMax) break;
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
                lines.add("+" + (objs.size() - MAX_OBJECTIVES) + " more");
                colors.add(MORE);
            }
            blocks.add(new Block(QuestIcons.of(id, q), title, lines, colors));
        }
        if (blocks.isEmpty()) return;

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
            g.renderItem(b.icon(), x + 4, cy);
            g.drawString(font, b.title(), x + 23, cy + 1, TITLE, true);
            int ly = cy + 11;
            for (int i = 0; i < b.lines().size(); i++) {
                g.drawString(font, b.lines().get(i), x + 23, ly, b.colors().get(i), true);
                ly += LINE;
            }
            cy += height(b);
        }
    }

    /** Height of one quest block: icon or title + objective lines, plus a gap. */
    private static int height(Block b) {
        return Math.max(18, 11 + b.lines().size() * LINE) + 3;
    }

    /** Translucent black body, black outline with cut corners and a faint top highlight. */
    private static void panel(GuiGraphics g, int x, int y, int w, int h) {
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
        return mc.options.renderDebug;
    }
}
