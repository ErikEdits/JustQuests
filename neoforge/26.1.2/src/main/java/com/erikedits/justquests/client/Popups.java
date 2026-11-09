package com.erikedits.justquests.client;

import com.erikedits.justquests.data.Quest;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

import java.util.ArrayDeque;

/**
 * Small notices in a screen corner (0.5.0, #21): "Quest completed!" (or how to claim the reward) and
 * "New quest!", one after the other. Pictures popup/quest_done.png and popup/quest_new.png, places and
 * colours from popup/settings.json. Players turn them off in the quest book.
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
    public static void render(GuiGraphicsExtractor g) {
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
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        String c = t.pCorner;
        boolean right = c.endsWith("right"), bottom = c.startsWith("bottom"), center = c.endsWith("center");
        int x = center ? (sw - t.pW) / 2 : right ? sw - t.pW - t.pOffX : t.pOffX;
        int y = bottom ? sh - t.pH - t.pOffY : t.pOffY;
        // slides in from its edge and back out
        long age = now - shownAt;
        float in = Math.min(1f, age / (float) SLIDE), out = Math.min(1f, Math.max(0f, (t.pMillis + 2 * SLIDE - age) / (float) SLIDE));
        float shift = 1f - ease(Math.min(in, out));
        if (center) y += (int) ((bottom ? 1 : -1) * shift * (t.pH + t.pOffY));
        else x += (int) ((right ? 1 : -1) * shift * (t.pW + t.pOffX));

        t.stretch(g, shown.done() ? "popup/quest_done" : "popup/quest_new", x, y, t.pW, t.pH, t.pW, t.pH);
        t.icon(g, shown.id(), shown.quest(), x + t.pIconX, y + t.pIconY);
        String title = fit(font, shown.quest().title().get(mc.options.languageCode), t.pW - t.pTitleX - 4);
        g.text(font, Theme.text(title), x + t.pTitleX, y + t.pTitleY, t.pTitle, t.pShadow);
        String line = !shown.done() ? I18n.get("justquests.popup.new")
            : shown.claim() ? I18n.get("justquests.popup.claim", openKey(mc)) : I18n.get("justquests.popup.done");
        g.text(font, Theme.text(fit(font, line, t.pW - t.pLineX - 4)), x + t.pLineX, y + t.pLineY, t.pLine, t.pShadow);
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
