package com.cake.sds.diagram.render;

import com.cake.sds.SimulatedDiagramSnapshots;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.util.SimpleSubLevelGroupRenderer;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import net.createmod.catnip.theme.Color;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;

public class CleanDiagramRenderer {

    public static void draw(final SubLevel subLevel, final float partialTicks, final Quaternionf localOrientation, final Matrix4f projMatrix, final Vector3d cameraPos, final float inWidth, final float inHeight, final AdvancedFbo fbo, final AdvancedFbo outlineFbo, final AdvancedFbo finalFbo, final float paletteOffset, final float fadeScale, final int lineColor, final int lineShadowColor) {
        fbo.bind(true);
        fbo.clear();

        final Pose3dc renderPose = ((ClientSubLevel) subLevel).renderPose(partialTicks);
        final Quaternionf orientation = new Quaternionf(renderPose.orientation()).conjugate();
        orientation.premul(localOrientation.conjugate(new Quaternionf()));

        SimpleSubLevelGroupRenderer.renderChain(subLevel, fbo, new Matrix4f(), projMatrix, cameraPos, orientation, partialTicks);

        final PostProcessingManager manager = VeilRenderSystem.renderer().getPostProcessingManager();
        final PostPipeline pipeline = manager.getPipeline(SimulatedDiagramSnapshots.path("diagram_clean"));

        if (pipeline != null) {
            final Color LINE_SHADOW_COLOR = new Color(lineShadowColor);
            final Color LINE_COLOR = new Color(lineColor);

            pipeline.getUniformSafe("LineColor").setVector((LINE_COLOR.getRed()) / 255.0f, (LINE_COLOR.getGreen()) / 255.0f, (LINE_COLOR.getBlue()) / 255.0f, 1.0f);
            pipeline.getUniformSafe("LineShadowColor").setVector((LINE_SHADOW_COLOR.getRed()) / 255.0f, (LINE_SHADOW_COLOR.getGreen()) / 255.0f, (LINE_SHADOW_COLOR.getBlue()) / 255.0f, 1.0f);
            pipeline.getUniformSafe("InSize").setVector(inWidth, inHeight);
            pipeline.getUniformSafe("PaletteOffset").setFloat(paletteOffset);
            pipeline.getUniformSafe("FadeScale").setFloat(fadeScale);
        }

        final PostPipeline.Context context = manager.getPostPipelineContext();
        context.setFramebuffer(Simulated.path("diagram"), fbo);
        context.setFramebuffer(Simulated.path("diagram_outlined"), outlineFbo);
        context.setFramebuffer(Simulated.path("diagram_final"), finalFbo);

        manager.runPipeline(pipeline, false);
    }

}
