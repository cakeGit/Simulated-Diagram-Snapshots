package com.cake.sds.diagram.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.simulated_team.simulated.index.SimGUITextures;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class OverlayRadioGroup<E extends Enum<E>> {

    private static final int LABEL_COLOR = 0xffc2937d;
    private static final int SELECTED_COLOR = 0xff6d7177;
    private static final int DESELECTED_COLOR = 0xaaaaaaaa;
    private static final int SHADOW_COLOR = 0xffe2d9c3;

    private static final int TAB_WIDTH = SimGUITextures.DIAGRAM_TAB.width;
    private static final int TAB_HEIGHT = SimGUITextures.DIAGRAM_TAB.height;
    private static final int TEXT_RIGHT_PADDING = 7;
    private static final int LABEL_LEFT_PADDING = PaperPanel.ROW_HEIGHT - 4;

    private final Component label;
    private final List<E> options = new ArrayList<>();
    private final List<Component> displays = new ArrayList<>();
    private int selectedIndex;
    private Consumer<E> onChange;

    public OverlayRadioGroup(final Component label) {
        this.label = label;
    }

    public OverlayRadioGroup<E> addOption(final E value, final Component display) {
        this.options.add(value);
        this.displays.add(display);
        return this;
    }

    public OverlayRadioGroup<E> onChange(final Consumer<E> onChange) {
        this.onChange = onChange;
        return this;
    }

    public void setSelected(final E value) {
        for (int i = 0; i < this.options.size(); i++) {
            if (this.options.get(i) == value) {
                this.selectedIndex = i;
                return;
            }
        }
    }

    public E getSelected() {
        return this.options.get(this.selectedIndex);
    }

    public int getRowCount() {
        return 1 + this.options.size();
    }

    /**
     * @param x        paper left edge in screen space
     * @param y        paper top edge in screen space
     * @param rowStart global row index within the paper grid (0 = first row)
     * @param tabHide  pixel offset that hides the tab (positive = tucked into paper)
     */
    public void render(final GuiGraphics graphics, final int x, final int y, final int rowStart,
                       final float tabHide, final Font font) {
        final PoseStack ps = graphics.pose();

        final int labelTextY = y + 11 + rowStart * PaperPanel.ROW_HEIGHT;
        ps.pushPose();
        ps.translate(x + LABEL_LEFT_PADDING, labelTextY, 0);
        ps.scale(0.75F, 0.75F, 0.0F);
        graphics.drawString(font, this.label, 0, 0, LABEL_COLOR, false);
        ps.popPose();

        for (int i = 0; i < this.options.size(); i++) {
            final int globalRow = rowStart + 1 + i;
            final int lineY = y + PaperPanel.HEADER_HEIGHT + globalRow * PaperPanel.ROW_HEIGHT;
            final int textY = lineY - 8;

            final boolean selected = i == this.selectedIndex;
            final int color = selected ? SELECTED_COLOR : DESELECTED_COLOR;

            final int tabX = x + PaperPanel.TAB_CX - (int) tabHide;
            final int tabY = lineY - 10;
            ps.pushPose();
            ps.translate(tabX + (SimGUITextures.DIAGRAM_TAB.width / 2f), tabY + (SimGUITextures.DIAGRAM_TAB.height / 2f), 0);
            TransformStack.of(ps).rotateZ((float) Math.PI);
            ps.translate(-(SimGUITextures.DIAGRAM_TAB.width / 2f), -(SimGUITextures.DIAGRAM_TAB.height / 2f), 0);
            SimGUITextures.DIAGRAM_TAB.render(graphics, 0, 0, new Color(color));
            ps.popPose();

            ps.pushPose();
            ps.scale(0.75F, 0.75F, 0.0F);
            final int textWidth = font.width(this.displays.get(i));
            final int textRightX = (x + PaperPanel.PAPER_WIDTH - TEXT_RIGHT_PADDING);
            ps.translate(textRightX / 0.75F - textWidth, textY / 0.75F, 0);
            if (selected) {
                graphics.drawString(font, this.displays.get(i), 1, 1, SHADOW_COLOR, false);
                graphics.drawString(font, this.displays.get(i), 0, 0, SELECTED_COLOR, false);
            } else {
                graphics.drawString(font, this.displays.get(i), 0, 0, DESELECTED_COLOR, false);
            }
            ps.popPose();
        }
    }

    /**
     * @param x        paper left edge in screen space
     * @param y        paper top edge in screen space
     * @param rowStart global row index within the paper grid
     * @param tabHide  pixel offset that hides the tab (positive = tucked into paper)
     */
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button,
                                final int x, final int y, final int rowStart, final float tabHide) {
        for (int i = 0; i < this.options.size(); i++) {
            final int globalRow = rowStart + 1 + i;
            final int lineY = y + PaperPanel.HEADER_HEIGHT + globalRow * PaperPanel.ROW_HEIGHT;

            final int tabX = x + PaperPanel.TAB_CX - (int) tabHide;
            final int tabY = lineY - 10;
            if (mouseX >= tabX && mouseX < tabX + TAB_WIDTH
                    && mouseY >= tabY && mouseY < tabY + TAB_HEIGHT) {
                return this.select(i);
            }

            final int textRightX = x + PaperPanel.PAPER_WIDTH - TEXT_RIGHT_PADDING;
            final int textY = lineY - 8;
            final Font font = net.minecraft.client.Minecraft.getInstance().font;
            final int textWidth = (int) (font.width(this.displays.get(i)) * 0.75F);
            final int textLeftX = textRightX - textWidth;
            if (mouseX >= textLeftX && mouseX < textRightX
                    && mouseY >= textY && mouseY < textY + font.lineHeight * 0.75F) {
                return this.select(i);
            }
        }
        return false;
    }

    private boolean select(final int i) {
        if (i == this.selectedIndex) {
            return true;
        }
        this.selectedIndex = i;
        if (this.onChange != null) {
            this.onChange.accept(this.options.get(i));
        }
        return true;
    }
}
