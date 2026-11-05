package com.teameway.nowaycore.init.multiblock;

import com.teameway.nowaycore.common.multiblock.controller.SimpleProcessorController;
import com.teameway.nowaycore.init.block.ModBlocks;
import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.IController;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;

import static com.teameway.nowaycore.NoWayCore.REGISTRUM;

public class ModMultiblockControllers {
    public static final RegistryEntry<IController, SimpleProcessorController> SIMPLE_PROCESSOR_CONTROLLER = REGISTRUM.multiblockController(
        "simple_processor",
        ModBlocks.SIMPLE_CONTROLLER,
        SimpleProcessorController::new
    ).register();

    public static void register() {
    }
}
