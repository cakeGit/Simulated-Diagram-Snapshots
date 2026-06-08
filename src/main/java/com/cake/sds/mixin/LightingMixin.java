package com.cake.sds.mixin;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.cake.sds.diagram.LightingMixinHelper.isRenderingForSDS;

@Mixin(Lighting.class)
public class LightingMixin {


    @Shadow
    @Final
    private static Vector3f INVENTORY_DIFFUSE_LIGHT_0;

    @Shadow
    @Final
    private static Vector3f INVENTORY_DIFFUSE_LIGHT_1;

    @Inject(method = "setupNetherLevel", at = @At("HEAD"), cancellable = true)
    private static void sds$disableNetherLighting(final CallbackInfo ci) {
        if (isRenderingForSDS) {
            RenderSystem.setupLevelDiffuseLighting(INVENTORY_DIFFUSE_LIGHT_0, INVENTORY_DIFFUSE_LIGHT_1);
            ci.cancel();
        }
    }


}
