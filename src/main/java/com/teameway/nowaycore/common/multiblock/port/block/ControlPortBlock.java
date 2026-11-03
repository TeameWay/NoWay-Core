package com.teameway.nowaycore.common.multiblock.port.block;

import com.teameway.nowaycore.common.multiblock.port.PortKind;
import com.teameway.nowaycore.common.multiblock.port.PortTier;
import com.teameway.nowaycore.common.multiblock.port.block.entity.ControlPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 控制端口方块（并行仓 / 线程仓），不承载资源，只贡献数值
 */
public class ControlPortBlock extends AbstractPortBlock {
    public ControlPortBlock(Properties properties, PortKind portKind, PortTier tier) {
        super(properties, portKind, tier);
    }

    @Override
    protected BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ControlPortBlockEntity(pos, state);
    }
}
