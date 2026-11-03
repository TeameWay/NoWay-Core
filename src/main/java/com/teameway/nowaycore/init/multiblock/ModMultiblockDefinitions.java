package com.teameway.nowaycore.init.multiblock;

import com.teameway.nowaycore.init.block.ModBlocks;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Blocks;

public class ModMultiblockDefinitions {
    public static final MultiblockDefinition SIMPLE_PROCESSOR = MultiblockDefinition.builder()
        // 控制器固定在本地坐标 (0, 0, 0)
        .addController(ModBlocks.SIMPLE_CONTROLLER.get())
        // 底座：控制器正下方 3×3 铁块
        .add(new Vec3i(-1, -1, -1), Blocks.IRON_BLOCK)
        .add(new Vec3i(0, -1, -1), Blocks.IRON_BLOCK)
        .add(new Vec3i(1, -1, -1), Blocks.IRON_BLOCK)
        .add(new Vec3i(-1, -1, 0), Blocks.IRON_BLOCK)
        .add(new Vec3i(0, -1, 0), Blocks.IRON_BLOCK)
        .add(new Vec3i(1, -1, 0), Blocks.IRON_BLOCK)
        .add(new Vec3i(-1, -1, 1), Blocks.IRON_BLOCK)
        .add(new Vec3i(0, -1, 1), Blocks.IRON_BLOCK)
        .add(new Vec3i(1, -1, 1), Blocks.IRON_BLOCK)
        // 控制器同层：前后左右各一块金块
        .add(new Vec3i(-1, 0, 0), Blocks.GOLD_BLOCK)
        .add(new Vec3i(1, 0, 0), Blocks.GOLD_BLOCK)
        .add(new Vec3i(0, 0, -1), Blocks.GOLD_BLOCK)
        .add(new Vec3i(0, 0, 1), Blocks.GOLD_BLOCK)
        .build();

}
