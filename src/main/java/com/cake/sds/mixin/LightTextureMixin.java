package com.cake.sds.mixin;

import com.cake.sds.ext.CoolerLightTextureExtension;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LightTexture.class)
public abstract class LightTextureMixin implements CoolerLightTextureExtension {

    @Shadow
    private boolean updateLightTexture;
    @Shadow
    @Final
    private DynamicTexture lightTexture;
    @Shadow
    @Final
    private NativeImage lightPixels;

    @Override
    public void sds$makeCleanLightTexture(final float brightnessMultiplier) {
        for (int x = 0; x < 16; ++x) {
            for (int y = 0; y < 16; ++y) {
                this.lightPixels.setPixelRGBA(y, x, 0xffffffff);
            }
        }

        this.updateLightTexture = true;
        this.lightTexture.upload();
    }

}
