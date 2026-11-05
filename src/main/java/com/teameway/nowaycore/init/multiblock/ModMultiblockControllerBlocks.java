package com.teameway.nowaycore.init.multiblock;

import com.teameway.nowaycore.common.multiblock.controller.ControllerBlock;
import com.teameway.nowaycore.init.ModCreativeTabs;
import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;

import static com.teameway.nowaycore.NoWayCore.REGISTRUM;

public class ModMultiblockControllerBlocks {

    public static final BlockEntry<ControllerBlock> SIMPLE_CONTROLLER = REGISTRUM
        .block("simple_controller", ControllerBlock::new)
        .properties(properties -> properties.strength(3.5F).requiresCorrectToolForDrops())
        .simpleItem()
        .register();

    static {
        REGISTRUM.defaultCreativeTab(ModCreativeTabs.MULTIBLOCK);
    }

    public static void register() {
    }
}
