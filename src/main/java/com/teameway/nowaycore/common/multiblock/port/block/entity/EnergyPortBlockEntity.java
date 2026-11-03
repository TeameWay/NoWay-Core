package com.teameway.nowaycore.common.multiblock.port.block.entity;

import com.teameway.nowaycore.common.multiblock.port.EnergyPort;
import com.teameway.nowaycore.common.multiblock.port.PortKind;
import com.teameway.nowaycore.common.multiblock.port.PortTier;
import com.teameway.nowaycore.common.multiblock.port.block.AbstractPortBlock;
import com.teameway.nowaycore.init.multiblock.ModMultiblockPorts;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * 能量端口方块实体
 */
public class EnergyPortBlockEntity extends BlockEntity implements EnergyPort {
    private final PortTier tier;
    private final SimpleEnergyHandler handler;

    public EnergyPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModMultiblockPorts.ENERGY_PORT_TYPE.get(), pos, state);
        this.tier = ((AbstractPortBlock) state.getBlock()).tier();
        this.handler = new SimpleEnergyHandler(this.tier.energyCapacity());
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
    public EnergyHandler energyHandler() {
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
