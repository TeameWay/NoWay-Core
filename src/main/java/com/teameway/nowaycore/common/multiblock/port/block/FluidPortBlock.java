package com.teameway.nowaycore.common.multiblock.port.block;

import com.teameway.nowaycore.common.multiblock.port.PortKind;
import com.teameway.nowaycore.common.multiblock.port.PortTier;
import com.teameway.nowaycore.common.multiblock.port.block.entity.FluidPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 流体端口方块（输入仓 / 输出仓）
 */
public class FluidPortBlock extends AbstractPortBlock {
    public FluidPortBlock(Properties properties, PortKind portKind, PortTier tier) {
        super(properties, portKind, tier);
    }

    @Override
    protected BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FluidPortBlockEntity(pos, state);
    }
}
