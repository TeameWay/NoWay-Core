package com.teameway.nowaycore.common.multiblock.port.block;

import com.teameway.nowaycore.common.multiblock.port.PortKind;
import com.teameway.nowaycore.common.multiblock.port.PortTier;
import com.teameway.nowaycore.common.multiblock.port.block.entity.EnergyPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 能量端口方块（输入 / 输出）
 */
public class EnergyPortBlock extends AbstractPortBlock {
    public EnergyPortBlock(Properties properties, PortKind portKind, PortTier tier) {
        super(properties, portKind, tier);
    }

    @Override
    protected BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyPortBlockEntity(pos, state);
    }
}
