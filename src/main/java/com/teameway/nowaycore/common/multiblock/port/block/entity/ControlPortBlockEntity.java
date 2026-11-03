package com.teameway.nowaycore.common.multiblock.port.block.entity;

import com.teameway.nowaycore.common.multiblock.port.MultiblockPort;
import com.teameway.nowaycore.common.multiblock.port.PortKind;
import com.teameway.nowaycore.common.multiblock.port.PortTier;
import com.teameway.nowaycore.common.multiblock.port.block.AbstractPortBlock;
import com.teameway.nowaycore.init.multiblock.ModMultiblockPorts;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 控制端口方块实体（并行仓 / 线程仓），无资源、无持久化数据
 */
public class ControlPortBlockEntity extends BlockEntity implements MultiblockPort {
    private final PortTier tier;

    public ControlPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModMultiblockPorts.CONTROL_PORT_TYPE.get(), pos, state);
        this.tier = ((AbstractPortBlock) state.getBlock()).tier();
    }

    @Override
    public PortKind portKind() {
        return ((AbstractPortBlock) this.getBlockState().getBlock()).portKind();
    }

    @Override
    public PortTier tier() {
        return this.tier;
    }
}
