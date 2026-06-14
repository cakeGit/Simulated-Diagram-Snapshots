package com.cake.sds.mixin;

import com.cake.sds.ext.VanillaSubLevelRenderDispatcherExtension;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.mixinterface.BlockEntityRenderDispatcherExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher;
import dev.ryanhcode.sable.sublevel.render.dispatcher.VanillaSubLevelRenderDispatcher;
import dev.ryanhcode.sable.sublevel.render.vanilla.VanillaChunkedSubLevelRenderData;
import foundry.veil.api.client.render.MatrixStack;
import foundry.veil.api.client.render.VeilRenderBridge;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;
import java.util.List;

@Mixin(VanillaSubLevelRenderDispatcher.class)
public class VanillaSubLevelRenderDispatcherMixin implements VanillaSubLevelRenderDispatcherExtension {

    @Override
    public void renderGlobalBlockEntities(final Iterable<ClientSubLevel> sublevels, final SubLevelRenderDispatcher.BlockEntityRenderer blockEntityRenderer, final double cameraX, final double cameraY, final double cameraZ, final float partialTick) {
        final Vector3f cameraPosition = new Vector3f();
        final Vector3d chunkOffset = new Vector3d();
        final Matrix4f transformation = new Matrix4f();
        final Matrix4f transformationInverse = new Matrix4f();
        final BlockEntityRenderDispatcherExtension dispatcher = (BlockEntityRenderDispatcherExtension) blockEntityRenderer.getBlockEntityRenderDispatcher();
        final PoseStack matrices = new PoseStack();
        final MatrixStack matrixStack = VeilRenderBridge.create(matrices);

        for (final ClientSubLevel sublevel : sublevels) {
            final SubLevelRenderData data = sublevel.getRenderData();

            sublevel.renderPose().rotationPoint().negate(chunkOffset.zero());
            data.getTransformation(cameraX, cameraY, cameraZ, transformation);

            transformation.invert(transformationInverse).transformPosition(cameraPosition.zero());
            dispatcher.sable$setCameraPosition(new Vec3(cameraPosition.x - chunkOffset.x(), cameraPosition.y - chunkOffset.y(), cameraPosition.z - chunkOffset.z()));

            matrixStack.clear();
            matrices.mulPose(transformation);
            if (data instanceof final VanillaChunkedSubLevelRenderData chunkedRenderData) {
                for (final SectionRenderDispatcher.RenderSection renderSection : chunkedRenderData.allRenderSections()) {
                    final List<BlockEntity> blockEntities = new ArrayList<>(((SectionRenderDispatcher$RenderSectionAccessor) renderSection).sds$getGlobalBlockEntities());
                    if (!blockEntities.isEmpty()) {
                        blockEntityRenderer.renderBlockEntities(blockEntities, matrices, partialTick, -chunkOffset.x, -chunkOffset.y, -chunkOffset.z);
                    }
                }
            }
        }

        dispatcher.sable$setCameraPosition(null);
    }

}
