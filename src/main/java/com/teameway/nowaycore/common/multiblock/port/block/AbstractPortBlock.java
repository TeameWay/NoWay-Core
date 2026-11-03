package com.teameway.nowaycore.common.multiblock.port.block;

import com.teameway.nowaycore.common.multiblock.port.PortKind;
import com.teameway.nowaycore.common.multiblock.port.PortTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 端口方块基类
 *
 * <p>只保存“种类 + 等级”，对应的方块实体在构造时从这里取等级来创建容量。</p>
 */
public abstract class AbstractPortBlock extends Block implements EntityBlock {
    private final PortKind portKind;
    private final PortTier tier;

    protected AbstractPortBlock(Properties properties, PortKind portKind, PortTier tier) {
        super(properties);
        this.portKind = portKind;
        this.tier = tier;
    }

    /**
     * @return 端口种类
     */
    public PortKind portKind() {
        return this.portKind;
    }

    /**
     * @return 端口等级
     */
    public PortTier tier() {
        return this.tier;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return this.createBlockEntity(pos, state);
    }

    /**
     * 创建对应的方块实体
     *
     * @param pos   位置
     * @param state 方块状态
     * @return 方块实体
     */
    protected abstract BlockEntity createBlockEntity(BlockPos pos, BlockState state);
}
