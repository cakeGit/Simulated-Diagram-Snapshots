package com.cake.sds.mixin;

import com.cake.sds.diagram.DiagramSnapshotOverlay;
import dev.simulated_team.simulated.content.entities.diagram.screen.DiagramScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DiagramScreen.class)
public abstract class DiagramScreenMixin {

    @Unique
    private DiagramSnapshotOverlay sds$overlay;

    @Invoker("addRenderableWidget")
    abstract <T extends GuiEventListener & Renderable & NarratableEntry> T invokeAddRenderableWidget(T widget);

    @Inject(method = "init", at = @At("TAIL"))
    private void sds$onInit(final CallbackInfo ci) {
        final DiagramScreen self = (DiagramScreen) (Object) this;
        final DiagramScreenAccessor accessor = (DiagramScreenAccessor) this;

        final int diagramX = self.width / 2 - DiagramScreen.DIAGRAM_TEXTURE.width / 2;
        final int diagramY = self.height / 2 - DiagramScreen.DIAGRAM_TEXTURE.height / 2;

        this.sds$overlay = new DiagramSnapshotOverlay();
        this.sds$overlay.init(self, diagramX, diagramY,
                accessor.simulatedDiagramSnapshots$getConfig(),
                accessor.simulatedDiagramSnapshots$getNote());

        this.invokeAddRenderableWidget(this.sds$overlay.getSaveBtn());
        this.invokeAddRenderableWidget(this.sds$overlay.getConfigureBtn());
    }

    @Inject(method = "renderWindow", at = @At("TAIL"))
    private void sds$onRenderWindow(final GuiGraphics graphics, final int mouseX, final int mouseY,
                                    final float partialTicks, final CallbackInfo ci) {
        if (this.sds$overlay == null) {
            return;
        }
        if (this.sds$overlay.isSidebarVisible() || this.sds$overlay.isAnimating()) {
            final DiagramScreen self = (DiagramScreen) (Object) this;
            final int diagramX = self.width / 2 - DiagramScreen.DIAGRAM_TEXTURE.width / 2;
            final int diagramY = self.height / 2 - DiagramScreen.DIAGRAM_TEXTURE.height / 2;
            this.sds$overlay.renderSidebar(graphics, mouseX, mouseY, partialTicks,
                    diagramX, diagramY, Minecraft.getInstance().font);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void sds$onTick(final CallbackInfo ci) {
        if (this.sds$overlay != null) {
            this.sds$overlay.tick();
        }
    }

    @Inject(method = "mouseClicked", at = @At("TAIL"))
    private void sds$onMouseClicked(final double mouseX, final double mouseY, final int button,
                                    final CallbackInfoReturnable<Boolean> cir) {
        if (this.sds$overlay == null || cir.getReturnValue()) {
            return;
        }
        if (this.sds$overlay.isSidebarVisible()) {
            final DiagramScreen self = (DiagramScreen) (Object) this;
            final int diagramX = self.width / 2 - DiagramScreen.DIAGRAM_TEXTURE.width / 2;
            final int diagramY = self.height / 2 - DiagramScreen.DIAGRAM_TEXTURE.height / 2;
            if (this.sds$overlay.mouseClicked(mouseX, mouseY, button, diagramX, diagramY)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "onClose", at = @At("HEAD"))
    private void sds$onClose(final CallbackInfo ci) {
        if (this.sds$overlay != null) {
            this.sds$overlay.onClose();
        }
    }
}
