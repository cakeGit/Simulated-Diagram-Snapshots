package com.cake.sds.diagram.snapshot;

import com.cake.sds.SimulatedDiagramSnapshots;
import com.cake.sds.diagram.CameraMode;
import com.cake.sds.diagram.SnapshotFrame;
import com.cake.sds.diagram.SnapshotResolution;
import com.cake.sds.diagram.SnapshotStyle;
import com.cake.sds.diagram.render.CleanDiagramRenderer;
import com.cake.sds.diagram.render.NotSoSimpleSubLevelGroupRenderer;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.entities.diagram.screen.DiagramScreen;
import dev.simulated_team.simulated.util.SimpleSubLevelGroupRenderer;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.joml.*;

import java.lang.Math;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Locale;

public class SnapshotExportPipeline {

    private static final float BLOCK_PIXEL_SCALE = 1.0f;
    private static final long MEMORY_CONFIRM_THRESHOLD_MB = 256;
    private static final float ORTHO_Z_NEAR = 0.1f;
    private static final float ORTHO_PADDING = 1.10f;
    private static final float MIN_ORTHO_HALF_EXTENT = 2.0f;

    private static final SystemToast.SystemToastId TOAST_ID = new SystemToast.SystemToastId(5000L);

    private AdvancedFbo exportFbo;
    private AdvancedFbo exportOutlineFbo;
    private AdvancedFbo exportFinalFbo;
    private long largeExportArmedUntilMs;

    public void freeFbos() {
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

    public void export(final SubLevel subLevel, final ClientSubLevel clientSubLevel, final DiagramScreen screen,
                       final SnapshotSettings settings, final float configYaw, final float configPitch) {
        final Minecraft mc = Minecraft.getInstance();

        final BoundingBox3ic framingBounds = settings.frame == SnapshotFrame.CHAIN
                ? computeChainBounds(clientSubLevel)
                : clientSubLevel.getPlot().getBoundingBox();

        final PlotCamera plotCamera = this.computePlotCamera(subLevel, framingBounds, settings, configYaw, configPitch);
        final int[] dims = this.computeExportDimensions(plotCamera, settings.resolution);
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

        this.freeFbos();
        this.exportFbo = AdvancedFbo.withSize(w, h).addColorTextureBuffer().setDepthTextureBuffer().build(true);
        this.exportFbo.clear(0, 0, 0, 0);
        this.exportOutlineFbo = AdvancedFbo.withSize(w, h).addColorTextureBuffer().build(true);
        this.exportFinalFbo = AdvancedFbo.withSize(w, h).addColorTextureBuffer().build(true);
        this.exportFinalFbo.clear(0, 0, 0, 0);

        final float paletteOffset = settings.snapshotStyle == SnapshotStyle.DIAGRAM ? 0.25f : 1.0f;
        final float fadeScale = settings.snapshotStyle == SnapshotStyle.DIAGRAM ? 0.0f : 0.5f;

        if (settings.snapshotStyle == SnapshotStyle.DIAGRAM) {
            CleanDiagramRenderer.draw(subLevel, 0, plotCamera.orientation, plotCamera.projection, plotCamera.cameraPos,
                    w, h, this.exportFbo, this.exportOutlineFbo, this.exportFinalFbo,
                    paletteOffset, fadeScale, 0x2E3032, 0x696965);
        } else {
            drawClean(subLevel, 0, plotCamera.orientation, plotCamera.projection, plotCamera.cameraPos, this.exportFbo);
        }

        final AdvancedFbo finalFbo = settings.snapshotStyle == SnapshotStyle.DIAGRAM ? this.exportFinalFbo : this.exportFbo;

        finalFbo.bindRead();
        final NativeImage image = new NativeImage(w, h, false);
        RenderSystem.bindTexture(finalFbo.getColorTextureAttachment(0).getId());
        image.downloadTexture(0, false);
        image.flipY();
        AdvancedFbo.unbind();

        final File dir = new File(mc.gameDirectory, "screenshots/diagrams");
        dir.mkdirs();

        final String name = clientSubLevel.getName();
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

    public static void drawClean(final SubLevel subLevel, final float partialTicks, final Quaternionf localOrientation,
                                  final Matrix4f projMatrix, final Vector3d cameraPos, final AdvancedFbo fbo) {
        fbo.bind(true);
        fbo.clear();

        final Pose3dc renderPose = ((ClientSubLevel) subLevel).renderPose(partialTicks);
        final Quaternionf orientation = new Quaternionf(renderPose.orientation()).conjugate();
        orientation.premul(localOrientation.conjugate(new Quaternionf()));

        NotSoSimpleSubLevelGroupRenderer.renderChain(subLevel, fbo, new Matrix4f(), projMatrix, cameraPos, orientation, partialTicks, NotSoSimpleSubLevelGroupRenderer.LightingStyle.LEVEL);
    }

    private PlotCamera computePlotCamera(final SubLevel subLevel, final BoundingBox3ic framingBounds,
                                          final SnapshotSettings settings, final float configYaw, final float configPitch) {
        final ClientSubLevel clientSubLevel = (ClientSubLevel) subLevel;

        final Vector3d plotCenter = plotCenterOf(framingBounds);
        final Vector3f plotHalfExtents = plotHalfExtents(framingBounds);
        final Quaternionf orientation = buildCameraOrientation(settings.cameraMode, configYaw, configPitch);
        final Vector2f rotatedHalfExtents = rotatedPlotHalfExtents(orientation, plotHalfExtents);

        final Vector2f orthoHalfExtents = fitOrthoExtents(rotatedHalfExtents, settings.resolution);

        final float sphereRadius = boundingSphereRadius(plotHalfExtents);
        final float cameraDistance = Math.max(sphereRadius + ORTHO_Z_NEAR, MIN_ORTHO_HALF_EXTENT);
        final float farPlane = cameraDistance + sphereRadius;

        final Matrix4f projection = new Matrix4f().ortho(
                -orthoHalfExtents.x, orthoHalfExtents.x,
                -orthoHalfExtents.y, orthoHalfExtents.y,
                ORTHO_Z_NEAR, farPlane);

        final Vector3d localCameraPos = new Vector3d(plotCenter.add(
                orientation.transform(new Vector3d(0, 0, cameraDistance))));
        final Pose3dc renderPose = clientSubLevel.renderPose(0);
        renderPose.transformPosition(localCameraPos);

        return new PlotCamera(orientation, projection, localCameraPos,
                new Vector2f(orthoHalfExtents.x * 2, orthoHalfExtents.y * 2));
    }

    private Vector2f fitOrthoExtents(final Vector2f rotatedHalfExtents, final SnapshotResolution resolution) {
        if (resolution != SnapshotResolution.PIXELATED) {
            return naturalOrthoExtents(rotatedHalfExtents);
        }
        final float diagramAspect = (float) DiagramScreen.DIAGRAM_TEXTURE.width / DiagramScreen.DIAGRAM_TEXTURE.height;
        return aspectFitOrthoExtents(rotatedHalfExtents, diagramAspect, ORTHO_PADDING);
    }

    private static Vector2f naturalOrthoExtents(final Vector2f halfExtents) {
        final float paddedX = halfExtents.x * ORTHO_PADDING;
        final float paddedY = halfExtents.y * ORTHO_PADDING;
        final float halfWidth = Math.max(paddedX, MIN_ORTHO_HALF_EXTENT);
        final float halfHeight = Math.max(paddedY, MIN_ORTHO_HALF_EXTENT);
        return new Vector2f(halfWidth, halfHeight);
    }

    private static Quaternionf buildCameraOrientation(final CameraMode cameraMode,
                                                       final float configYaw, final float configPitch) {
        final Quaternionf orientation = new Quaternionf()
                .rotateY(radians(configYaw))
                .rotateX(radians(configPitch));
        switch (cameraMode) {
            case ISOMETRIC -> orientation.rotateY(radians(45.0f))
                    .rotateX(radians(-35.264f));
            case DIMETRIC -> orientation.rotateY(radians(45.0f))
                    .rotateX(radians(-28.12f));
            case TRIMETRIC -> orientation.rotateY(radians(30.0f))
                    .rotateX(radians(-50.0f));
        }
        return orientation;
    }

    private static Vector3d plotCenterOf(final BoundingBox3ic plotBounds) {
        return new Vector3d(
                (plotBounds.minX() + plotBounds.maxX() + 1) / 2.0,
                (plotBounds.minY() + plotBounds.maxY() + 1) / 2.0,
                (plotBounds.minZ() + plotBounds.maxZ() + 1) / 2.0);
    }

    private static Vector3f plotHalfExtents(final BoundingBox3ic plotBounds) {
        return new Vector3f(
                (plotBounds.maxX() - plotBounds.minX() + 1) / 2.0f,
                (plotBounds.maxY() - plotBounds.minY() + 1) / 2.0f,
                (plotBounds.maxZ() - plotBounds.minZ() + 1) / 2.0f);
    }

    private static Vector2f rotatedPlotHalfExtents(final Quaternionf orientation, final Vector3f halfExtents) {
        final Vector3f corner = new Vector3f();
        final Quaternionf invOrientation = new Quaternionf(orientation).conjugate();
        float maxAbsX = 0;
        float maxAbsY = 0;
        for (int cornerIndex = 0; cornerIndex < 8; cornerIndex++) {
            corner.set(
                    (cornerIndex & 1) == 0 ? -halfExtents.x : halfExtents.x,
                    (cornerIndex & 2) == 0 ? -halfExtents.y : halfExtents.y,
                    (cornerIndex & 4) == 0 ? -halfExtents.z : halfExtents.z);
            invOrientation.transform(corner);
            final float absX = Math.abs(corner.x);
            final float absY = Math.abs(corner.y);
            if (absX > maxAbsX) {
                maxAbsX = absX;
            }
            if (absY > maxAbsY) {
                maxAbsY = absY;
            }
        }
        return new Vector2f(maxAbsX, maxAbsY);
    }

    private static Vector2f aspectFitOrthoExtents(final Vector2f halfExtents, final float aspect,
                                                   final float padding) {
        final float paddedX = halfExtents.x * padding;
        final float paddedY = halfExtents.y * padding;
        final float halfWidth;
        final float halfHeight;
        if (paddedX / aspect >= paddedY) {
            halfWidth = Math.max(paddedX, MIN_ORTHO_HALF_EXTENT * aspect);
            halfHeight = halfWidth / aspect;
        } else {
            halfHeight = Math.max(paddedY, MIN_ORTHO_HALF_EXTENT);
            halfWidth = halfHeight * aspect;
        }
        return new Vector2f(halfWidth, halfHeight);
    }

    private static float boundingSphereRadius(final Vector3f halfExtents) {
        return (float) Math.sqrt(
                halfExtents.x * halfExtents.x +
                        halfExtents.y * halfExtents.y +
                        halfExtents.z * halfExtents.z);
    }

    private static float radians(final float degrees) {
        return (float) Math.toRadians(degrees);
    }

    private int[] computeExportDimensions(final PlotCamera plotCamera, final SnapshotResolution resolution) {
        if (resolution == SnapshotResolution.PIXELATED) {
            return new int[]{DiagramScreen.DIAGRAM_TEXTURE.width, DiagramScreen.DIAGRAM_TEXTURE.height};
        }

        final float pixelsPerBlock = resolution.scale() * BLOCK_PIXEL_SCALE;
        final int w = Math.max(1, Math.round(plotCamera.worldViewExtents.x * pixelsPerBlock));
        final int h = Math.max(1, Math.round(plotCamera.worldViewExtents.y * pixelsPerBlock));
        return new int[]{w, h};
    }

    private static long estimateMemoryUsageMB(final int w, final int h) {
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

    private record PlotCamera(Quaternionf orientation, Matrix4f projection, Vector3d cameraPos,
                               Vector2f worldViewExtents) {
    }

    private static BoundingBox3ic computeChainBounds(final ClientSubLevel root) {
        final Collection<ClientSubLevel> chain = SimpleSubLevelGroupRenderer.getRenderedChain(root);

        BoundingBox3i union = null;
        for (final ClientSubLevel subLevel : chain) {
            final BoundingBox3ic bounds = subLevel.getPlot().getBoundingBox();
            if (union == null) {
                union = new BoundingBox3i(bounds);
            } else {
                union.expandTo(bounds);
            }
        }

        if (union == null) {
            return root.getPlot().getBoundingBox();
        }

        return union;
    }
}
