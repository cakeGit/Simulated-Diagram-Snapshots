package com.cake.sds.mixin;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public interface SectionRenderDispatcher$RenderSectionAccessor {

    @Accessor("globalBlockEntities")
    Set<BlockEntity> sds$getGlobalBlockEntities();

}
