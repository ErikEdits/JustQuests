package com.erikedits.justquests.client;

import com.erikedits.justquests.data.Quest;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayDeque;

/**
 * Small notices in a screen corner (0.5.0, #21): "Quest completed!" (or how to claim the reward) and
 * "New quest!", one after the other. Pictures popup/quest_done.png and popup/quest_new.png, places and
 * colours from popup/settings.json. Players turn them off in the quest book, and can put them anywhere
 * on the screen with its popup button (0.5.1, #24).
 */
public final class Popups {
    private static final long SLIDE = 300;
    private static final ArrayDeque<Popup> QUEUE = new ArrayDeque<>();
    private static Popup shown;
    private static long shownAt;

    private Popups() {}

    private record Popup(boolean done, Identifier id, Quest quest, boolean claim) {}

    public static void questDone(Identifier id, Quest q, boolean claim) {
        add(new Popup(true, id, q, claim));
    }

    public static void questNew(Identifier id, Quest q) {
        add(new Popup(false, id, q, false));
    }

    private static void add(Popup p) {
        ClientSettings.load();
        if (ClientSettings.popups && QUEUE.size() < 20) QUEUE.add(p);
    }

    /** Drawn with the HUD (not while a screen is open, so nothing is missed behind the quest book). */
    public static void render(GuiGraphics g) {
        long now = System.currentTimeMillis();
        Theme t = Theme.get();
        if (shown != null && now - shownAt > t.pMillis + 2 * SLIDE) shown = null;
        if (shown == null) {
            shown = QUEUE.poll();
            if (shown == null) return;
            shownAt = now;
            QuestEvents.sound("popup.show");
        }
        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        int[] p = place(t, sw, sh);
        // slides in from the nearest screen edge and back out
        long age = now - shownAt;
        float in = Math.min(1f, age / (float) SLIDE), out = Math.min(1f, Math.max(0f, (t.pMillis + 2 * SLIDE - age) / (float) SLIDE));
        float shift = 1f - ease(Math.min(in, out));
        int x = p[0] + (int) (shift * p[2]), y = p[1] + (int) (shift * p[3]);
        String title = shown.quest().title().get(mc.options.languageCode);
        String line = !shown.done() ? I18n.get("justquests.popup.new")
            : shown.claim() ? I18n.get("justquests.popup.claim", openKey(mc)) : I18n.get("justquests.popup.done");
        draw(g, t, shown.done(), shown.id(), shown.quest(), title, line, x, y);
    }

    /** One popup with its picture, the quest's icon (a book without a quest), its title and line. */
    private static void draw(GuiGraphics g, Theme t, boolean done, Identifier id, Quest quest, String title, String line, int x, int y) {
        Font font = Minecraft.getInstance().font;
        t.stretch(g, done ? "popup/quest_done" : "popup/quest_new", x, y, t.pW, t.pH, t.pW, t.pH);
        if (quest != null) t.icon(g, id, quest, x + t.pIconX, y + t.pIconY);
        else g.renderItem(new ItemStack(Items.BOOK), x + t.pIconX, y + t.pIconY);
        g.drawString(font, Theme.text(fit(font, title, t.pW - t.pTitleX - 4)), x + t.pTitleX, y + t.pTitleY, t.pTitle, t.pShadow);
        g.drawString(font, Theme.text(fit(font, line, t.pW - t.pLineX - 4)), x + t.pLineX, y + t.pLineY, t.pLine, t.pShadow);
    }

    /**
     * Where a popup sits: {x, y} of its top left corner and {dx, dy}, the way out of the screen it
     * slides. The player's own place, else the resource pack's corner, moved below a minimap in the same
     * top corner (#24).
     */
    private static int[] place(Theme t, int sw, int sh) {
        int w = t.pW, h = t.pH, x, y;
        String corner = ClientSettings.popupAnchor;
        if (!corner.isEmpty()) {
            x = corner.endsWith("center") ? (sw - w) / 2 + ClientSettings.popupX
                : corner.endsWith("right") ? sw - w - ClientSettings.popupX : ClientSettings.popupX;
            y = corner.startsWith("bottom") ? sh - h - ClientSettings.popupY : ClientSettings.popupY;
        } else {
            corner = t.pCorner;
            x = corner.endsWith("center") ? (sw - w) / 2 : corner.endsWith("right") ? sw - w - t.pOffX : t.pOffX;
            y = corner.startsWith("bottom") ? sh - h - t.pOffY : t.pOffY;
            if (corner.startsWith("top") && Minimaps.at(corner)) y = Math.max(y, Minimaps.below(corner, sh));
        }
        x = Math.max(0, Math.min(x, sw - w));
        y = Math.max(0, Math.min(y, sh - h));
        if (corner.endsWith("center")) return new int[] {x, y, 0, corner.startsWith("bottom") ? sh - y : -(y + h)};
        return new int[] {x, y, corner.endsWith("right") ? sw - x : -(x + w), 0};
    }

    /** The top left corner of a popup centred on the mouse, kept on the screen. */
    private static int[] at(Theme t, int mx, int my, int sw, int sh) {
        return new int[] {Math.max(0, Math.min(mx - t.pW / 2, sw - t.pW)), Math.max(0, Math.min(my - t.pH / 2, sh - t.pH))};
    }

    /** While the player places the popups: a sample one under the mouse. */
    public static void preview(GuiGraphics g, int mx, int my, int sw, int sh) {
        Theme t = Theme.get();
        int[] p = at(t, mx, my, sw, sh);
        draw(g, t, true, null, null, I18n.get("justquests.popup.sample"), I18n.get("justquests.popup.done"), p[0], p[1]);
    }

    /**
     * Keeps the popups where the player clicked, as the screen edges they are nearest to, so the place
     * holds when the window or the GUI scale changes.
     */
    public static void keep(int mx, int my, int sw, int sh) {
        Theme t = Theme.get();
        int[] p = at(t, mx, my, sw, sh);
        int cx = p[0] + t.pW / 2, cy = p[1] + t.pH / 2;
        String side;
        if (cx < sw / 3) {
            side = "left";
            ClientSettings.popupX = p[0];
        } else if (cx > sw * 2 / 3) {
            side = "right";
            ClientSettings.popupX = sw - t.pW - p[0];
        } else {
            side = "center";
            ClientSettings.popupX = p[0] - (sw - t.pW) / 2;
        }
        boolean top = cy < sh / 2;
        ClientSettings.popupY = top ? p[1] : sh - t.pH - p[1];
        ClientSettings.popupAnchor = (top ? "top_" : "bottom_") + side;
        ClientSettings.save();
    }

    /** Back to the automatic place. */
    public static void reset() {
        ClientSettings.popupAnchor = "";
        ClientSettings.popupX = 0;
        ClientSettings.popupY = 0;
        ClientSettings.save();
    }

    private static float ease(float f) {
        return 1f - (1f - f) * (1f - f);
    }

    /** The key that opens the quest book (J unless the player changed it). */
    private static String openKey(Minecraft mc) {
        for (KeyMapping k : mc.options.keyMappings) {
            if (k.getName().equals("key.justquests.open")) return k.getTranslatedKeyMessage().getString();
        }
        return "J";
    }

    private static String fit(Font font, String s, int maxW) {
        if (font.width(Theme.text(s)) <= maxW) return s;
        return font.getSplitter().plainHeadByWidth(s, maxW - font.width(Theme.text("...")), Theme.style()) + "...";
    }
}
