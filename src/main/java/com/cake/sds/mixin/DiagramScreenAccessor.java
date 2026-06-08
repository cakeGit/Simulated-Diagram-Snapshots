package com.cake.sds.mixin;

import dev.simulated_team.simulated.content.entities.diagram.DiagramConfig;
import dev.simulated_team.simulated.content.entities.diagram.screen.DiagramScreen;
import dev.simulated_team.simulated.content.entities.diagram.screen.DiagramStickyNote;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DiagramScreen.class)
public interface DiagramScreenAccessor {

    @Accessor("config")
    DiagramConfig simulatedDiagramSnapshots$getConfig();

    @Accessor("note")
    DiagramStickyNote simulatedDiagramSnapshots$getNote();

}
