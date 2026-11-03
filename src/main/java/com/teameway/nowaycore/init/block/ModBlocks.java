package com.teameway.nowaycore.init.block;

import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import net.minecraft.world.level.block.Block;

import static com.teameway.nowaycore.NoWayCore.REGISTRUM;

public class ModBlocks {
    public static final BlockEntry<Block> SIMPLE_CONTROLLER = REGISTRUM
        .block("simple_controller", Block::new)
        .properties(properties -> properties.strength(3.5F).requiresCorrectToolForDrops())
        .simpleItem()
        .register();

    public static void register() {
    }
}
