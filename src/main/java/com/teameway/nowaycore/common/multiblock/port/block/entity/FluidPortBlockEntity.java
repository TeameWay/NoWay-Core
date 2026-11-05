package com.teameway.nowaycore.common.multiblock.port.block.entity;

import com.teameway.nowaycore.common.multiblock.port.FluidPort;
import com.teameway.nowaycore.common.multiblock.port.PortKind;
import com.teameway.nowaycore.common.multiblock.port.PortTier;
import com.teameway.nowaycore.common.multiblock.port.block.AbstractPortBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

/**
 * 流体端口方块实体
 */
public class FluidPortBlockEntity extends BlockEntity implements FluidPort {
    private final PortTier tier;
    private final FluidStacksResourceHandler handler;

    public FluidPortBlockEntity(BlockEntityType<FluidPortBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.tier = ((AbstractPortBlock) state.getBlock()).tier();
        this.handler = new FluidStacksResourceHandler(1, this.tier.fluidCapacity());
    }

    @Override
    public PortKind portKind() {
        return ((AbstractPortBlock) this.getBlockState().getBlock()).portKind();
    }

    @Override
    public PortTier tier() {
        return this.tier;
    }

    @Override
    public ResourceHandler<FluidResource> fluidHandler() {
        return this.handler;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.handler.deserialize(input.childOrEmpty("handler"));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.handler.serialize(output.child("handler"));
    }
}
