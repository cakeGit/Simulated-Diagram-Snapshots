package com.cake.sds;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(SimulatedDiagramSnapshots.MODID)
public class SimulatedDiagramSnapshots {

    public static final String MODID = "simulated_diagram_snapshots";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SimulatedDiagramSnapshots(IEventBus modEventBus, ModContainer modContainer) {
    }

    public static ResourceLocation path(String diagram) {
        return ResourceLocation.fromNamespaceAndPath(MODID, diagram);
    }

}
