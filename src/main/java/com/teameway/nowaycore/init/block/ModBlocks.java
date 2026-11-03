package com.teameway.nowaycore.init.block;

import com.teameway.nowaycore.common.multiblock.controller.ControllerBlock;
import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;

import static com.teameway.nowaycore.NoWayCore.REGISTRUM;

public class ModBlocks {
    public static final BlockEntry<ControllerBlock> SIMPLE_CONTROLLER = REGISTRUM
        .block("simple_controller", ControllerBlock::new)
        .properties(properties -> properties.strength(3.5F).requiresCorrectToolForDrops())
        .simpleItem()
        .register();

    public static void register() {
    }
}