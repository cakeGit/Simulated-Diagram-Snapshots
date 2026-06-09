package com.cake.sds.diagram.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.simulated_team.simulated.index.SimGUITextures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class PaperPanel {

    public static final int PAPER_WIDTH = SimGUITextures.DIAGRAM_PAPER.width;
    public static final int HEADER_HEIGHT = 41;
    public static final int ROW_HEIGHT = 11;
    public static final int BOTTOM_HEIGHT = SimGUITextures.DIAGRAM_PAPER.height - HEADER_HEIGHT - ROW_HEIGHT;
    public static final int TAB_CX = -6;

    private static final int LINE_Y_FIRST = 41;
    private static final int ATLAS_X = SimGUITextures.DIAGRAM_PAPER.startX;
    private static final int ATLAS_Y = SimGUITextures.DIAGRAM_PAPER.startY;
    private static final float SLIDE_SPEED = 0.4f;

    private boolean visible;
    private float offset;
    private float lastOffset;

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(final boolean visible) {
        this.visible = visible;
    }

    public boolean isAnimating() {
        return Math.abs(this.offset - this.lastOffset) > 0.5f;
    }

    public void tick() {
        this.lastOffset = this.offset;
        this.offset = Mth.lerp(SLIDE_SPEED, this.offset, this.visible ? PAPER_WIDTH : 0);
    }

    public float getOffset(final float partialTicks) {
        return Mth.lerp(partialTicks, this.lastOffset, this.offset);
    }

    public int getContentHeight(final int totalRows) {
        return HEADER_HEIGHT + totalRows * ROW_HEIGHT + BOTTOM_HEIGHT;
    }

    public void render(final GuiGraphics graphics, final int baseX, final int baseY,
                       final int totalRows, final float partialTicks) {
        final float o = this.getOffset(partialTicks);
        if (o < 1) {
            return;
        }

        final ResourceLocation loc = SimGUITextures.DIAGRAM_PAPER.location;
        final int texW = SimGUITextures.DIAGRAM_PAPER.texWidth;
        final int texH = SimGUITextures.DIAGRAM_PAPER.texHeight;

        final PoseStack ps = graphics.pose();
        ps.pushPose();
        ps.translate(baseX, baseY, 0);

        graphics.blit(loc, 0, 0, ATLAS_X, ATLAS_Y, PAPER_WIDTH, HEADER_HEIGHT, texW, texH);

        int renderedMiddleRows = totalRows - 7;
        for (int i = 0; i < renderedMiddleRows; i++) {
            graphics.blit(loc,
                    0, HEADER_HEIGHT + i * ROW_HEIGHT,
                    ATLAS_X, ATLAS_Y + LINE_Y_FIRST,
                    PAPER_WIDTH, ROW_HEIGHT,
                    texW, texH);
        }

        graphics.blit(loc,
                0, HEADER_HEIGHT + renderedMiddleRows * ROW_HEIGHT,
                ATLAS_X, ATLAS_Y + LINE_Y_FIRST + ROW_HEIGHT,
                PAPER_WIDTH, BOTTOM_HEIGHT,
                texW, texH);

        ps.popPose();
    }
}
