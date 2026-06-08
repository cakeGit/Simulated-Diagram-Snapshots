package com.cake.sds.diagram;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.cake.sds.SimulatedDiagramSnapshots;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.simulated_team.simulated.content.entities.diagram.DiagramConfig;
import dev.simulated_team.simulated.content.entities.diagram.screen.DiagramScreen;
import dev.simulated_team.simulated.content.entities.diagram.screen.DiagramStickyNote;
import dev.simulated_team.simulated.index.SimGUITextures;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class DiagramSnapshotOverlay {

    // ============================================================================
    // Tweakable knobs
    // ============================================================================

    // The existing DiagramScreen button column sits at x = diagramX + 9, y = diagramY + 9 + 20*i
    // for i in 0..3 (forces / merge / com / mass). We append at i=4 and i=5.
    private static final int BUTTON_X_OFFSET = 9;
    private static final int BUTTON_Y_BASE_OFFSET = 9;
    private static final int BUTTON_Y_STRIDE = 20;
    private static final int SAVE_BUTTON_INDEX = 4;
    private static final int CONFIGURE_BUTTON_INDEX = 5;

    // Pixels per world-block at "Block xN". Tweak this scalar if you want a global
    // bump (e.g. for higher-DPI exports across all Block modes) without editing each enum.
    private static final float BLOCK_PIXEL_SCALE = 1.0f;

    // Estimated FBO + image memory above this many megabytes triggers a confirmation dialog
    // before allocating. Covers: 2 colour FBOs + 1 colour+depth FBO + the CPU-side NativeImage
    // (all 4 bytes per pixel, RGBA8).
    private static final long MEMORY_CONFIRM_THRESHOLD_MB = 256;

    // ----------------------------------------------------------------------------

    private static final float SLIDE_SPEED = 0.4f;
    private static final int PAPER_WIDTH = SimGUITextures.DIAGRAM_PAPER.width;

    private CameraMode cameraMode = CameraMode.NORMAL;
    private SnapshotStyle snapshotStyle = SnapshotStyle.DIAGRAM;
    private SnapshotResolution resolution = SnapshotResolution.PIXELATED;
    private long largeExportArmedUntilMs;

    private boolean sidebarVisible;
    private float sidebarOffset = 0;
    private float lastSidebarOffset = 0;

    private DiagramSnapshotButton saveBtn;
    private DiagramSnapshotButton configureBtn;

    private DiagramRadioGroup<CameraMode> cameraGroup;
    private DiagramRadioGroup<SnapshotStyle> styleGroup;
    private DiagramRadioGroup<SnapshotResolution> resolutionGroup;

    private DiagramScreen screen;
    private DiagramConfig config;
    private DiagramStickyNote note;

    private AdvancedFbo exportFbo, exportOutlineFbo, exportFinalFbo;

    private static final SystemToastId TOAST_ID = new SystemToastId(5000L);

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

        this.cameraGroup = new DiagramRadioGroup<CameraMode>(
                Component.translatable("simulated_diagram_snapshots.camera"))
                .addOption(CameraMode.NORMAL, Component.translatable("simulated_diagram_snapshots.camera.normal"))
                .addOption(CameraMode.ISOMETRIC, Component.translatable("simulated_diagram_snapshots.camera.isometric"))
                .addOption(CameraMode.DIMETRIC, Component.translatable("simulated_diagram_snapshots.camera.dimetric"))
                .addOption(CameraMode.TRIMETRIC, Component.translatable("simulated_diagram_snapshots.camera.trimetric"))
                .onChange(mode -> this.cameraMode = mode);

        this.styleGroup = new DiagramRadioGroup<SnapshotStyle>(
                Component.translatable("simulated_diagram_snapshots.style"))
                .addOption(SnapshotStyle.DIAGRAM, Component.translatable("simulated_diagram_snapshots.style.diagram"))
                .addOption(SnapshotStyle.NORMAL, Component.translatable("simulated_diagram_snapshots.style.normal"))
                .onChange(style -> this.snapshotStyle = style);

        this.resolutionGroup = new DiagramRadioGroup<SnapshotResolution>(
                Component.translatable("simulated_diagram_snapshots.resolution"))
                .addOption(SnapshotResolution.PIXELATED, Component.translatable("simulated_diagram_snapshots.resolution.pixelated"))
                .addOption(SnapshotResolution.BLOCK_X4, Component.translatable("simulated_diagram_snapshots.resolution.block_x4"))
                .addOption(SnapshotResolution.BLOCK_X8, Component.translatable("simulated_diagram_snapshots.resolution.block_x8"))
                .addOption(SnapshotResolution.BLOCK_X16, Component.translatable("simulated_diagram_snapshots.resolution.block_x16"))
                .addOption(SnapshotResolution.BLOCK_X32, Component.translatable("simulated_diagram_snapshots.resolution.block_x32"))
                .addOption(SnapshotResolution.BLOCK_X64, Component.translatable("simulated_diagram_snapshots.resolution.block_x64"))
                .onChange(r -> this.resolution = r);
    }

    public boolean isSidebarVisible() {
        return this.sidebarVisible;
    }

    public boolean isAnimating() {
        return Mth.abs(this.sidebarOffset - this.lastSidebarOffset) > 0.5f;
    }

    public void tick() {
        this.lastSidebarOffset = this.sidebarOffset;
        this.sidebarOffset = Mth.lerp(SLIDE_SPEED, this.sidebarOffset,
                this.sidebarVisible ? PAPER_WIDTH : 0);
    }

    public void renderSidebar(final GuiGraphics graphics, final int mouseX, final int mouseY,
                              final float partialTicks, final int diagramX, final int diagramY,
                              final Font font) {
        final float offset = Mth.lerp(partialTicks, this.lastSidebarOffset, this.sidebarOffset);
        if (offset < 1) {
            return;
        }

        final PoseStack ps = graphics.pose();
        ps.pushPose();
        ps.translate(diagramX + DiagramScreen.DIAGRAM_TEXTURE.width - offset, diagramY, 0);

        final int contentHeight = 14 + this.cameraGroup.getHeight()
                + this.styleGroup.getHeight()
                + this.resolutionGroup.getHeight();

        if (contentHeight > SimGUITextures.DIAGRAM_PAPER.height) {
            SimGUITextures.DIAGRAM_PAPER.render(graphics, 0, 0, PAPER_WIDTH, contentHeight);
        } else {
            SimGUITextures.DIAGRAM_PAPER.render(graphics, 0, 0);
        }

        ps.pushPose();
        ps.translate(0, 0, 1);
        ps.scale(0.75F, 0.75F, 0.0F);
        final Component titleComp = Component.translatable("simulated_diagram_snapshots.diagram_options");
        graphics.drawCenteredString(font, titleComp, 48, 3, 0xffc2937d);
        ps.popPose();

        int y = 14;
        this.cameraGroup.render(graphics, 0, y, font);
        y += this.cameraGroup.getHeight();
        this.styleGroup.render(graphics, 0, y, font);
        y += this.styleGroup.getHeight();
        this.resolutionGroup.render(graphics, 0, y, font);

        ps.popPose();
    }

    public boolean mouseClicked(final double mouseX, final double mouseY, final int button,
                                final int diagramX, final int diagramY) {
        if (!this.sidebarVisible) {
            return false;
        }

        final int sidebarX = diagramX + DiagramScreen.DIAGRAM_TEXTURE.width - (int) this.sidebarOffset;
        int y = diagramY + 14;

        if (this.cameraGroup.mouseClicked(mouseX, mouseY, button, sidebarX, y)) {
            return true;
        }
        y += this.cameraGroup.getHeight();
        if (this.styleGroup.mouseClicked(mouseX, mouseY, button, sidebarX, y)) {
            return true;
        }
        y += this.styleGroup.getHeight();
        if (this.resolutionGroup.mouseClicked(mouseX, mouseY, button, sidebarX, y)) {
            return true;
        }

        return false;
    }

    public DiagramSnapshotButton getSaveBtn() {
        return this.saveBtn;
    }

    public DiagramSnapshotButton getConfigureBtn() {
        return this.configureBtn;
    }

    public void onClose() {
        this.freeExportFbos();
    }

    private void toggleSidebar() {
        this.sidebarVisible = !this.sidebarVisible;

        if (this.sidebarVisible) {
            this.note.deactivate();
        } else {
            final DiagramConfig.NoteConfigs noteConfigs = this.config.getNoteConfigs();
            if (noteConfigs.isActive()) {
                this.note.activate();
            }
        }

        final Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }

    private void exportScreenshot() {
        final Minecraft mc = Minecraft.getInstance();
        final ClientSubLevel subLevel = this.screen.subLevel;

        final PlotCamera plotCamera = this.computePlotCamera();
        final int[] dims = this.computeExportDimensions(plotCamera);
        final int w = dims[0];
        final int h = dims[1];

        final long estimatedMB = estimateMemoryUsageMB(w, h);
        if (estimatedMB >= MEMORY_CONFIRM_THRESHOLD_MB) {
            final long now = System.currentTimeMillis();
            if (now >= this.largeExportArmedUntilMs) {
                this.largeExportArmedUntilMs = now + 5000L;
                this.warnLargeExport(estimatedMB, w, h);
                return;
            }
            this.largeExportArmedUntilMs = 0L;
        }

        this.freeExportFbos();
        this.exportFbo = AdvancedFbo.withSize(w, h).addColorTextureBuffer().setDepthTextureBuffer().build(true);
        this.exportOutlineFbo = AdvancedFbo.withSize(w, h).addColorTextureBuffer().build(true);
        this.exportFinalFbo = AdvancedFbo.withSize(w, h).addColorTextureBuffer().build(true);

        final float paletteOffset = this.snapshotStyle == SnapshotStyle.DIAGRAM ? 0.25f : 1.0f;
        final float fadeScale = this.snapshotStyle == SnapshotStyle.DIAGRAM ? 1.0f : 0.5f;

        // partialTicks=0 is intentional: exports a stable frame, not an interpolated one.
        DiagramScreen.draw(subLevel, 0, plotCamera.orientation, plotCamera.projection, plotCamera.cameraPos,
                w, h, this.exportFbo, this.exportOutlineFbo, this.exportFinalFbo,
                paletteOffset, fadeScale, 0x2E3032, 0x696965);

        final AdvancedFbo readFbo = this.snapshotStyle == SnapshotStyle.DIAGRAM
                ? this.exportFinalFbo : this.exportFbo;
        readFbo.bindRead();
        final NativeImage image = new NativeImage(w, h, false);
        RenderSystem.bindTexture(readFbo.getColorTextureAttachment(0).getId());
        image.downloadTexture(0, true);
        image.flipY();
        AdvancedFbo.unbind();

        final File dir = new File(mc.gameDirectory, "screenshots/diagrams");
        dir.mkdirs();

        final String name = subLevel.getName();
        final String safeName = name != null && !name.isEmpty()
                ? name.replaceAll("[^a-zA-Z0-9_\\-]", "_") + "_"
                : "";
        final String timestamp = Util.getFilenameFormattedDateTime();
        final File file = findFreeFile(dir, "diagram_" + safeName + timestamp + ".png");

        Util.ioPool().execute(() -> {
            try {
                image.writeToFile(file);
            } catch (IOException e) {
                SimulatedDiagramSnapshots.LOGGER.error("Failed to write diagram snapshot to {}", file, e);
            } finally {
                image.close();
            }
        });

        mc.getToasts().addToast(SystemToast.multiline(mc,
                TOAST_ID,
                Component.translatable("simulated_diagram_snapshots.exported_diagram"),
                Component.translatable("simulated_diagram_snapshots.saved_as", file.getName())));

        if (mc.player != null) {
            final Component clickable = Component.literal(file.getName())
                    .withStyle(ChatFormatting.UNDERLINE)
                    .withStyle(s -> s.withClickEvent(
                            new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath())));
            mc.player.sendSystemMessage(
                    Component.translatable("simulated_diagram_snapshots.exported_chat", clickable));
        }
    }

    private record PlotCamera(Quaternionf orientation, Matrix4f projection, Vector3d cameraPos) {}

    private PlotCamera computePlotCamera() {
        final ClientSubLevel subLevel = this.screen.subLevel;
        final LevelPlot plot = subLevel.getPlot();
        final BoundingBox3ic plotBounds = plot.getBoundingBox();
        float radius = Math.max(Math.max(plotBounds.maxX() - plotBounds.minX(),
                        plotBounds.maxY() - plotBounds.minY()),
                plotBounds.maxZ() - plotBounds.minZ()) + 1;
        radius *= 0.55F;
        radius = Math.max(radius, 2.0f);

        final Vector3d plotBoundsCenter = new Vector3d(
                (plotBounds.minX() + plotBounds.maxX() + 1) / 2.0,
                (plotBounds.minY() + plotBounds.maxY() + 1) / 2.0,
                (plotBounds.minZ() + plotBounds.maxZ() + 1) / 2.0);

        final float aspect = (float) DiagramScreen.DIAGRAM_TEXTURE.width / DiagramScreen.DIAGRAM_TEXTURE.height;
        final Matrix4f proj = new Matrix4f().ortho(
                -radius * aspect, radius * aspect, -radius, radius, 0.1f, radius * 2.0f);

        final Quaternionf orientation = new Quaternionf();
        switch (this.cameraMode) {
            case ISOMETRIC -> orientation.rotateY((float) Math.toRadians(45.0))
                    .rotateX((float) Math.toRadians(35.264));
            case DIMETRIC -> orientation.rotateY((float) Math.toRadians(45.0))
                    .rotateX((float) Math.toRadians(28.12));
            case TRIMETRIC -> orientation.rotateY((float) Math.toRadians(30.0))
                    .rotateX((float) Math.toRadians(50.0));
            default -> orientation.rotateY((float) Math.toRadians(this.config.yaw()))
                    .rotateX((float) Math.toRadians(this.config.pitch()));
        }

        final Vector3d localCameraPos = new Vector3d(plotBoundsCenter.add(
                orientation.transform(new Vector3d(0, 0, radius))));
        final Pose3dc renderPose = subLevel.renderPose(0);
        renderPose.transformPosition(localCameraPos);

        return new PlotCamera(orientation, proj, localCameraPos);
    }

    private int[] computeExportDimensions(final PlotCamera plotCamera) {
        if (this.resolution == SnapshotResolution.PIXELATED) {
            return new int[] { DiagramScreen.DIAGRAM_TEXTURE.width, DiagramScreen.DIAGRAM_TEXTURE.height };
        }

        final float pixelsPerBlock = this.resolution.scale() * BLOCK_PIXEL_SCALE;
        // Visible world dimensions in blocks: width = 2*radius*aspect, height = 2*radius.
        final float[] ortho = extractOrthoExtents(plotCamera.projection);
        final int w = Math.max(1, Math.round(ortho[0] * pixelsPerBlock));
        final int h = Math.max(1, Math.round(ortho[1] * pixelsPerBlock));
        return new int[] { w, h };
    }

    private static float[] extractOrthoExtents(final Matrix4f proj) {
        // For an ortho matrix built via JOML, the translation column holds the negated midpoints.
        // The half-extents live on the diagonal of the upper-left 3x3.
        final float halfWidth = Math.abs(proj.m00());
        final float halfHeight = Math.abs(proj.m11());
        return new float[] { halfWidth * 2.0f, halfHeight * 2.0f };
    }

    private static long estimateMemoryUsageMB(final int w, final int h) {
        // 3 FBOs (2 colour + 1 colour+depth) + NativeImage. Each 4 bytes/pixel RGBA8.
        // Depth buffer at 4 bytes/px is a fair worst case for GL_DEPTH_COMPONENT24/32.
        final long bytesPerBuffer = (long) w * h * 4L;
        final long total = bytesPerBuffer * 4L;
        return total / (1024L * 1024L);
    }

    private void warnLargeExport(final long estimatedMB, final int w, final int h) {
        final Minecraft mc = Minecraft.getInstance();
        final Component title = Component.translatable("simulated_diagram_snapshots.large_export_warning.title");
        final Component detail = Component.translatable(
                "simulated_diagram_snapshots.large_export_warning.detail",
                String.format(Locale.ROOT, "%,d", w),
                String.format(Locale.ROOT, "%,d", h),
                estimatedMB);
        if (mc.player != null) {
            mc.gui.getChat().addMessage(title.copy().append("\n").append(detail));
        }
    }

    private static File findFreeFile(final File dir, final String desiredName) {
        final File direct = new File(dir, desiredName);
        if (!direct.exists()) {
            return direct;
        }
        final int dot = desiredName.lastIndexOf('.');
        final String base = dot < 0 ? desiredName : desiredName.substring(0, dot);
        final String ext = dot < 0 ? "" : desiredName.substring(dot);
        for (int i = 2; i < 1000; i++) {
            final File candidate = new File(dir, base + "_" + i + ext);
            if (!candidate.exists()) {
                return candidate;
            }
        }
        return direct;
    }

    private void freeExportFbos() {
        if (this.exportFbo != null) {
            this.exportFbo.free();
            this.exportFbo = null;
        }
        if (this.exportOutlineFbo != null) {
            this.exportOutlineFbo.free();
            this.exportOutlineFbo = null;
        }
        if (this.exportFinalFbo != null) {
            this.exportFinalFbo.free();
            this.exportFinalFbo = null;
        }
    }
}
