package com.erikedits.justquests.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

/**
 * The one place that draws a picture. The draw call changes between Minecraft versions, so each
 * version only differs in this line.
 */
final class Draw {
    private Draw() {}

    /** The region (u, v)-(u + uW, v + vH) of a texW x texH picture, drawn at x, y scaled to w x h. */
    static void blit(GuiGraphics g, Identifier tex, int x, int y, int w, int h, float u, float v, int uW, int vH, int texW, int texH) {
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, uW, vH, texW, texH);
    }
}
