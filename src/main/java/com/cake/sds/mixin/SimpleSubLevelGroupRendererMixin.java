package com.cake.sds.mixin;

import com.cake.sds.diagram.LightingMixinHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.simulated_team.simulated.mixin_interface.diagram.LightTextureExtension;
import dev.simulated_team.simulated.util.SimpleSubLevelGroupRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SimpleSubLevelGroupRenderer.class)
public class SimpleSubLevelGroupRendererMixin {

    @WrapOperation(method = "renderGroup", at = @At(value = "INVOKE", target = "Ldev/simulated_team/simulated/mixin_interface/diagram/LightTextureExtension;simulated$makeDiagramLightTexture(F)V", ordinal = 0))
    private static void sds$redirectMakeDiagramLightTexture1(LightTextureExtension instance, float v, Operation<Void> original) {
        if (LightingMixinHelper.isRenderingForSDS) {
//            original.call(instance, 3.0f);
        } else {
            original.call(instance, v);
        }
    }

    @WrapOperation(method = "renderGroup", at = @At(value = "INVOKE", target = "Ldev/simulated_team/simulated/mixin_interface/diagram/LightTextureExtension;simulated$makeDiagramLightTexture(F)V", ordinal = 1))
    private static void sds$redirectMakeDiagramLightTexture2(LightTextureExtension instance, float v, Operation<Void> original) {
        if (LightingMixinHelper.isRenderingForSDS) {
//            original.call(instance, 3.0f);
        } else {
            original.call(instance, v);
        }
    }

}
