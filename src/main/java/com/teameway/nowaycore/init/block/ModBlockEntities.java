package com.teameway.nowaycore.init.block;

import com.teameway.nowaycore.common.multiblock.controller.ControllerBlockEntity;
import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntityEntry;

import static com.teameway.nowaycore.NoWayCore.REGISTRUM;

public class ModBlockEntities {
    public static final BlockEntityEntry<ControllerBlockEntity> SIMPLE_CONTROLLER = REGISTRUM
        .blockEntity("simple_controller", ControllerBlockEntity::new)
        .validBlock(ModBlocks.SIMPLE_CONTROLLER)
        .register();

    public static void register() {
    }
}
