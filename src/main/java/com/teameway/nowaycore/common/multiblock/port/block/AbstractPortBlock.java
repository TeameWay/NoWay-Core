package com.teameway.nowaycore.common.multiblock.port.block;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.teameway.nowaycore.common.multiblock.port.MultiblockPort;
import com.teameway.nowaycore.common.multiblock.port.PortKind;
import com.teameway.nowaycore.common.multiblock.port.PortTier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 端口方块基类
 *
 * <p>只保存“种类 + 等级”，对应的方块实体在构造时从这里取等级来创建容量；右键打开该端口自己的
 * 界面，界面上可直接操作库存或查看容量。</p>
 */
public abstract class AbstractPortBlock extends Block implements EntityBlock, BlockUIMenuType.BlockUI {
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

    @Override
    public InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hit
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            BlockUIMenuType.openUI(serverPlayer, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        if (holder.player.level().getBlockEntity(holder.pos) instanceof MultiblockPort port) {
            return PortUI.create(port, holder);
        }
        return new ModularUI(UI.of(new UIElement()), holder.player);
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
