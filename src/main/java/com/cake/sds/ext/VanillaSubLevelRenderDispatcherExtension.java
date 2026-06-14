package com.cake.sds.ext;

import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher;

public interface VanillaSubLevelRenderDispatcherExtension {
    void renderGlobalBlockEntities(final Iterable<ClientSubLevel> sublevels, final SubLevelRenderDispatcher.BlockEntityRenderer blockEntityRenderer, final double cameraX, final double cameraY, final double cameraZ, final float partialTick);
}
