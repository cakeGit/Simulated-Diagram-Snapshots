package com.cake.sds.diagram;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.simulated_team.simulated.content.entities.diagram.screen.DiagramScreen;
import dev.simulated_team.simulated.index.SimSoundEvents;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

import java.util.List;
import java.util.function.Supplier;

public class DiagramSnapshotButton extends AbstractWidget {

    private static final Color BUTTON_COLOR = new Color(109, 113, 119);
    private static final Color DULL_BUTTON_COLOR = new Color(181, 177, 168);

    private final ScreenElement icon;
    private final Runnable onClick;
    private Supplier<Component> tooltipSupplier;

    public DiagramSnapshotButton(final ScreenElement icon, final int x, final int y, final Runnable onClick) {
        super(x, y, 16, 16, Component.empty());
        this.icon = icon;
        this.onClick = onClick;
    }

    @Override
    public void onClick(final double mouseX, final double mouseY) {
        super.onClick(mouseX, mouseY);
        this.onClick.run();
    }

    public DiagramSnapshotButton setTooltip(final Supplier<Component> tooltipSupplier) {
        this.tooltipSupplier = tooltipSupplier;
        return this;
    }

    @Override
    protected void renderWidget(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTicks) {
        final Color color = this.isHovered() ? BUTTON_COLOR : DULL_BUTTON_COLOR;

        SDSGUITextures.BUTTON_BACKGROUND.render(guiGraphics, this.getX() - 1, this.getY() - 1);
        RenderSystem.enableBlend();
        if (this.icon instanceof SDSGUITextures sds) {
            sds.render(guiGraphics, this.getX() - 1, this.getY() - 1, color);
        } else {
            this.icon.render(guiGraphics, this.getX() - 1, this.getY() - 1);
        }
        RenderSystem.disableBlend();

        if (this.tooltipSupplier != null && this.isHovered()) {
            final List<FormattedText> lines = List.of(this.tooltipSupplier.get());
            DiagramScreen.renderTooltip(guiGraphics, mouseX, mouseY, lines);
        }
    }

    @Override
    public void playDownSound(final SoundManager handler) {
        handler.play(SimpleSoundInstance.forUI(SimSoundEvents.DIAGRAM_TAP.event(), 1.0F));
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput narrationElementOutput) {
    }

}
