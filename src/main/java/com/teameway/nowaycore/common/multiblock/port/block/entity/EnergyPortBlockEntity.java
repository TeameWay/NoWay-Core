package com.teameway.nowaycore.common.multiblock.port.block.entity;

import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.lowdragmc.lowdraglib2.syncdata.storage.IManagedStorage;
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
 *
 * <p>储量由端口方块每 10 tick 采样一次，经托管字段同步到客户端。</p>
 */
public class EnergyPortBlockEntity extends BlockEntity implements EnergyPort, ISyncPersistRPCBlockEntity {
    private static final int SAMPLE_INTERVAL = 10;

    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);
    private final PortTier tier;
    private final SimpleEnergyHandler handler;
    private int sampleCounter;

    @DescSynced
    private long energyStored;

    public EnergyPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModMultiblockPorts.ENERGY_PORT_TYPE.get(), pos, state);
        this.tier = ((AbstractPortBlock) state.getBlock()).tier();
        this.handler = new SimpleEnergyHandler(this.tier.energyCapacity());
    }

    @Override
    public IManagedStorage getSyncStorage() {
        return this.syncStorage;
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

    /**
     * 服务端每 tick 调用，每 {@value #SAMPLE_INTERVAL} tick 采样一次储量
     */
    public void serverTick() {
        if (++this.sampleCounter < SAMPLE_INTERVAL) {
            return;
        }
        this.sampleCounter = 0;
        this.energyStored = this.handler.getAmountAsLong();
    }

    /**
     * @return 上一次采样的储量
     */
    public long energyStored() {
        return this.energyStored;
    }

    public int energyCapacity() {
        return this.tier.energyCapacity();
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
