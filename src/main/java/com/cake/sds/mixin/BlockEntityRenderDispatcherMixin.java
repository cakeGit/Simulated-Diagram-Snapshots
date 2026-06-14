package com.cake.sds.mixin;

import com.cake.sds.diagram.render.NotSoSimpleSubLevelGroupRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {

    @WrapOperation(method = "setupAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I"))
    private static int sds$forceFullBrightBlockEntities(BlockAndTintGetter level, BlockPos pos, Operation<Integer> original) {
        return NotSoSimpleSubLevelGroupRenderer.RENDERING_DIAGRAM_SNAPSHOT ? LightTexture.FULL_SKY : original.call(level, pos);
    }

}
