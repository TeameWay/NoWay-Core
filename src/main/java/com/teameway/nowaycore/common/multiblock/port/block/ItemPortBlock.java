package com.teameway.nowaycore.common.multiblock.port.block;

import com.teameway.nowaycore.common.multiblock.port.PortKind;
import com.teameway.nowaycore.common.multiblock.port.PortTier;
import com.teameway.nowaycore.common.multiblock.port.block.entity.ItemPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 物品端口方块（输入总线 / 输出总线）
 */
public class ItemPortBlock extends AbstractPortBlock {
    public ItemPortBlock(Properties properties, PortKind portKind, PortTier tier) {
        super(properties, portKind, tier);
    }

    @Override
    protected BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ItemPortBlockEntity(pos, state);
    }
}
