package com.cake.sds.mixin;

import com.cake.sds.diagram.render.NotSoSimpleSubLevelGroupRenderer;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.RopeStrandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RopeStrandRenderer.class)
public class RopeStrandRendererMixin {

    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/sublevel/ClientSubLevel;renderPose()Ldev/ryanhcode/sable/companion/math/Pose3dc;")
    )
    private static Pose3dc sds$skipRenderPoseWhenDiagram(ClientSubLevel subLevel) {
        if (NotSoSimpleSubLevelGroupRenderer.RENDERING_DIAGRAM_SNAPSHOT) {
            return null;
        }
        return subLevel.renderPose();
    }
}
