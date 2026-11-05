package com.teameway.nowaycore.init.multiblock;

import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Blocks;

public class ModMultiblockDefinitions {
    public static final MultiblockDefinition SIMPLE_PROCESSOR = MultiblockDefinition.builder()
        // 控制器固定在本地坐标 (0, 0, 0)
        .addController(ModMultiblockControllerBlocks.SIMPLE_CONTROLLER.get())
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
        // 四角：输入总线 / 输出总线 / 输入仓 / 并行仓
        .add(new Vec3i(-1, 0, -1), ModMultiblockPorts.ITEM_INPUT.get())
        .add(new Vec3i(1, 0, -1), ModMultiblockPorts.ITEM_OUTPUT.get())
        .add(new Vec3i(-1, 0, 1), ModMultiblockPorts.FLUID_INPUT.get())
        .add(new Vec3i(1, 0, 1), ModMultiblockPorts.PARALLEL.get())
        .build();

}
