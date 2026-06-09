package com.cake.sds.diagram;

import com.cake.sds.diagram.overlay.OverlayRadioGroup;
import com.cake.sds.diagram.overlay.PaperPanel;
import com.cake.sds.diagram.snapshot.SnapshotExportPipeline;
import com.cake.sds.diagram.snapshot.SnapshotSettings;
import dev.simulated_team.simulated.content.entities.diagram.DiagramConfig;
import dev.simulated_team.simulated.content.entities.diagram.screen.DiagramScreen;
import dev.simulated_team.simulated.content.entities.diagram.screen.DiagramStickyNote;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class DiagramSnapshotOverlay {

    private static final int BUTTON_X_OFFSET = 9;
    private static final int BUTTON_Y_BASE_OFFSET = 9;
    private static final int BUTTON_Y_STRIDE = 20;
    private static final int SAVE_BUTTON_INDEX = 4;
    private static final int CONFIGURE_BUTTON_INDEX = 5;

    private static final int GAP = 2;

    private DiagramScreen screen;
    private DiagramConfig config;
    private DiagramStickyNote note;

    private DiagramSnapshotButton saveBtn;
    private DiagramSnapshotButton configureBtn;

    private final SnapshotSettings settings = new SnapshotSettings();
    private final PaperPanel paperPanel = new PaperPanel();
    private final SnapshotExportPipeline pipeline = new SnapshotExportPipeline();

    private OverlayRadioGroup<CameraMode> cameraGroup;
    private OverlayRadioGroup<SnapshotStyle> styleGroup;
    private OverlayRadioGroup<SnapshotFrame> frameGroup;
    private OverlayRadioGroup<SnapshotResolution> resolutionGroup;

    private int totalRows;

    public void init(final DiagramScreen screen, final int diagramX, final int diagramY,
                     final DiagramConfig config, final DiagramStickyNote note) {
        this.screen = screen;
        this.config = config;
        this.note = note;

        final int saveY = diagramY + BUTTON_Y_BASE_OFFSET + BUTTON_Y_STRIDE * SAVE_BUTTON_INDEX;
        final int configY = diagramY + BUTTON_Y_BASE_OFFSET + BUTTON_Y_STRIDE * CONFIGURE_BUTTON_INDEX;

        this.saveBtn = new DiagramSnapshotButton(
                SDSGUITextures.SNAPSHOT_SAVE,
                diagramX + BUTTON_X_OFFSET, saveY,
                this::exportScreenshot
        ).setTooltip(() -> Component.translatable("simulated_diagram_snapshots.save_snapshot"));

        this.configureBtn = new DiagramSnapshotButton(
                SDSGUITextures.SNAPSHOT_CONFIGURE,
                diagramX + BUTTON_X_OFFSET, configY,
                this::toggleSidebar
        ).setTooltip(() -> Component.translatable("simulated_diagram_snapshots.configure_snapshot"));

        this.cameraGroup = new OverlayRadioGroup<CameraMode>(
                Component.translatable("simulated_diagram_snapshots.camera"))
                .addOption(CameraMode.NORMAL, Component.translatable("simulated_diagram_snapshots.camera.normal"))
                .addOption(CameraMode.ISOMETRIC, Component.translatable("simulated_diagram_snapshots.camera.isometric"))
                .addOption(CameraMode.DIMETRIC, Component.translatable("simulated_diagram_snapshots.camera.dimetric"))
                .addOption(CameraMode.TRIMETRIC, Component.translatable("simulated_diagram_snapshots.camera.trimetric"))
                .onChange(mode -> this.settings.cameraMode = mode);

        this.styleGroup = new OverlayRadioGroup<SnapshotStyle>(
                Component.translatable("simulated_diagram_snapshots.style"))
                .addOption(SnapshotStyle.DIAGRAM, Component.translatable("simulated_diagram_snapshots.style.diagram"))
                .addOption(SnapshotStyle.NORMAL, Component.translatable("simulated_diagram_snapshots.style.normal"))
                .onChange(style -> this.settings.snapshotStyle = style);

        this.frameGroup = new OverlayRadioGroup<SnapshotFrame>(
                Component.translatable("simulated_diagram_snapshots.frame"))
                .addOption(SnapshotFrame.NORMAL, Component.translatable("simulated_diagram_snapshots.frame.normal"))
                .addOption(SnapshotFrame.CHAIN, Component.translatable("simulated_diagram_snapshots.frame.chain"))
                .onChange(frame -> this.settings.frame = frame);

        this.resolutionGroup = new OverlayRadioGroup<SnapshotResolution>(
                Component.translatable("simulated_diagram_snapshots.resolution"))
                .addOption(SnapshotResolution.PIXELATED, Component.translatable("simulated_diagram_snapshots.resolution.pixelated"))
                .addOption(SnapshotResolution.BLOCK_X4, Component.translatable("simulated_diagram_snapshots.resolution.block_x4"))
                .addOption(SnapshotResolution.BLOCK_X8, Component.translatable("simulated_diagram_snapshots.resolution.block_x8"))
                .addOption(SnapshotResolution.BLOCK_X16, Component.translatable("simulated_diagram_snapshots.resolution.block_x16"))
                .addOption(SnapshotResolution.BLOCK_X32, Component.translatable("simulated_diagram_snapshots.resolution.block_x32"))
                .addOption(SnapshotResolution.BLOCK_X64, Component.translatable("simulated_diagram_snapshots.resolution.block_x64"))
                .onChange(r -> this.settings.resolution = r);

        this.totalRows = this.cameraGroup.getRowCount()
                + this.styleGroup.getRowCount()
                + this.frameGroup.getRowCount()
                + this.resolutionGroup.getRowCount();
    }

    public boolean isSidebarVisible() {
        return this.paperPanel.isVisible();
    }

    public boolean isAnimating() {
        return this.paperPanel.isAnimating();
    }

    public void tick() {
        this.paperPanel.tick();
    }

    public void renderSidebar(final GuiGraphics graphics, final int mouseX, final int mouseY,
                               final float partialTicks, final int diagramX, final int diagramY,
                               final Font font) {
        final float offset = this.paperPanel.getOffset(partialTicks);
        if (offset < 1) {
            return;
        }

        final int panelX = diagramX + DiagramScreen.DIAGRAM_TEXTURE.width + GAP - PaperPanel.PAPER_WIDTH;
        final int panelY = diagramY;

        final int currentX = panelX + (int) offset;

        this.paperPanel.render(graphics, currentX, panelY, this.totalRows, partialTicks);

        int rowStart = 0;
        this.cameraGroup.render(graphics, currentX, panelY, rowStart, font);
        rowStart += this.cameraGroup.getRowCount();
        this.styleGroup.render(graphics, currentX, panelY, rowStart, font);
        rowStart += this.styleGroup.getRowCount();
        this.frameGroup.render(graphics, currentX, panelY, rowStart, font);
        rowStart += this.frameGroup.getRowCount();
        this.resolutionGroup.render(graphics, currentX, panelY, rowStart, font);
    }

    public boolean mouseClicked(final double mouseX, final double mouseY, final int button,
                                 final int diagramX, final int diagramY) {
        if (!this.paperPanel.isVisible()) {
            return false;
        }

        final float offset = this.paperPanel.getOffset(1.0f);
        final int panelX = diagramX + DiagramScreen.DIAGRAM_TEXTURE.width + GAP - PaperPanel.PAPER_WIDTH;
        final int panelY = diagramY;
        final int currentX = panelX + (int) offset;

        int rowStart = 0;
        if (this.cameraGroup.mouseClicked(mouseX, mouseY, button, currentX, panelY, rowStart)) {
            return true;
        }
        rowStart += this.cameraGroup.getRowCount();
        if (this.styleGroup.mouseClicked(mouseX, mouseY, button, currentX, panelY, rowStart)) {
            return true;
        }
        rowStart += this.styleGroup.getRowCount();
        if (this.frameGroup.mouseClicked(mouseX, mouseY, button, currentX, panelY, rowStart)) {
            return true;
        }
        rowStart += this.frameGroup.getRowCount();
        return this.resolutionGroup.mouseClicked(mouseX, mouseY, button, currentX, panelY, rowStart);
    }

    public DiagramSnapshotButton getSaveBtn() {
        return this.saveBtn;
    }

    public DiagramSnapshotButton getConfigureBtn() {
        return this.configureBtn;
    }

    public void onClose() {
        this.pipeline.freeFbos();
    }

    private void toggleSidebar() {
        this.paperPanel.setVisible(!this.paperPanel.isVisible());
        final Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }

    private void exportScreenshot() {
        this.pipeline.export(
                this.screen.subLevel,
                this.screen.subLevel,
                this.screen,
                this.settings,
                (float) this.config.yaw(),
                (float) this.config.pitch()
        );
    }
}
