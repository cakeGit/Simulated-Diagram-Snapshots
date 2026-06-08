package com.cake.sds.diagram;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DiagramRadioGroup<E extends Enum<E>> {

    private static final int LABEL_COLOR = 0xffc2937d;
    private static final int SELECTED_COLOR = 0xff6d7177;
    private static final int DESELECTED_COLOR = 0xaaaaaaaa;
    private static final int SHADOW_COLOR = 0xffe2d9c3;
    private static final int OPTION_SPACING = 10;

    private final Component label;
    private final List<E> options = new ArrayList<>();
    private final List<Component> displays = new ArrayList<>();
    private int selectedIndex = 0;
    private Consumer<E> onChange;

    public DiagramRadioGroup(final Component label) {
        this.label = label;
    }

    public DiagramRadioGroup<E> addOption(final E value, final Component display) {
        this.options.add(value);
        this.displays.add(display);
        return this;
    }

    public DiagramRadioGroup<E> onChange(final Consumer<E> onChange) {
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

    public int getHeight() {
        return 10 + this.options.size() * OPTION_SPACING;
    }

    public boolean mouseClicked(final double mouseX, final double mouseY, final int button,
                                final int groupX, final int groupY) {
        final int headerPx = 10;
        for (int i = 0; i < this.options.size(); i++) {
            final int optY = groupY + headerPx + i * OPTION_SPACING - 1;
            if (mouseX >= groupX + 18 && mouseX <= groupX + 18 + 60
                    && mouseY >= optY && mouseY <= optY + OPTION_SPACING) {
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
        return false;
    }

    public void render(final GuiGraphics graphics, final int x, final int y, final Font font) {
        graphics.pose().pushPose();
        graphics.pose().translate(x + 18, y + 1, 0);
        graphics.pose().scale(0.75F, 0.75F, 0.0F);
        graphics.drawString(font, this.label, 0, 0, LABEL_COLOR, false);
        graphics.pose().popPose();

        for (int i = 0; i < this.options.size(); i++) {
            final int optY = y + 10 + i * OPTION_SPACING - 1;

            graphics.pose().pushPose();
            graphics.pose().translate(x + 18, optY, 0);
            graphics.pose().scale(0.75F, 0.75F, 0.0F);

            if (i == this.selectedIndex) {
                graphics.drawString(font, this.displays.get(i), 1, 1, SHADOW_COLOR, false);
                graphics.drawString(font, this.displays.get(i), 0, 0, SELECTED_COLOR, false);
            } else {
                graphics.drawString(font, this.displays.get(i), 0, 0, DESELECTED_COLOR, false);
            }

            graphics.pose().popPose();
        }
    }
}
