package com.cake.sds.diagram;

import com.mojang.blaze3d.systems.RenderSystem;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public enum SDSGUITextures implements ScreenElement {

    SNAPSHOT_SAVE("snapshot_icons", 0, 0, 16, 16, 32, 32),
    SNAPSHOT_CONFIGURE("snapshot_icons", 16, 0, 16, 16, 32, 32),
    BUTTON_BACKGROUND("snapshot_icons", 0, 16, 16, 16, 32, 32);

    @NotNull
    public final ResourceLocation location;
    public final int width, height;
    public final int startX, startY;
    public final int texWidth, texHeight;

    SDSGUITextures(final String path, final int startX, final int startY, final int width, final int height, final int texWidth, final int texHeight) {
        this.location = ResourceLocation.fromNamespaceAndPath("simulated_diagram_snapshots", "textures/gui/" + path + ".png");
        this.width = width;
        this.height = height;
        this.startX = startX;
        this.startY = startY;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
    }

    @Override
    public void render(final GuiGraphics graphics, final int x, final int y) {
        graphics.blit(this.location, x, y, this.startX, this.startY, this.width, this.height, this.texWidth, this.texHeight);
    }

    public void render(final GuiGraphics graphics, final int x, final int y, final Color c) {
        RenderSystem.setShaderTexture(0, this.location);
        UIRenderHelper.drawColoredTexture(graphics, c, x, y, 0, this.startX, this.startY, this.width, this.height, this.texWidth, this.texHeight);
    }
}
