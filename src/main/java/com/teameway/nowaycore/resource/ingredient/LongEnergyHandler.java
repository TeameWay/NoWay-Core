package com.teameway.nowaycore.resource.ingredient;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * 容量以 {@code long} 记录的能量处理器
 *
 * <p>{@link net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler} 的储量与容量都是 {@code int}，
 * 承载不了超过 {@link Integer#MAX_VALUE} 的能量端口等级；本实现把两者改为 {@code long}。
 * 单次 {@code insert}/{@code extract} 仍按 {@link EnergyHandler} 的约定以 {@code int} 计量。</p>
 */
public class LongEnergyHandler implements EnergyHandler, ValueIOSerializable {
    private final long capacity;
    private final long maxTransfer;
    private final EnergyJournal energyJournal = new EnergyJournal();
    private long energy;

    /**
     * 以给定容量创建处理器，单次操作上限等于容量
     *
     * @param capacity 容量（FE）
     */
    public LongEnergyHandler(long capacity) {
        this(capacity, capacity);
    }

    /**
     * @param capacity    容量（FE）
     * @param maxTransfer 单次存入与取出的上限（FE）
     * @throws IllegalArgumentException 任一参数为负
     */
    public LongEnergyHandler(long capacity, long maxTransfer) {
        if (capacity < 0 || maxTransfer < 0) {
            throw new IllegalArgumentException(
                "Expected non-negative capacity and transfer limit: " + capacity + ", " + maxTransfer
            );
        }
        this.capacity = capacity;
        this.maxTransfer = maxTransfer;
    }

    @Override
    public long getAmountAsLong() {
        return this.energy;
    }

    @Override
    public long getCapacityAsLong() {
        return this.capacity;
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        long inserted = Math.min(this.capacity - this.energy, Math.min(amount, this.maxTransfer));
        if (inserted <= 0) {
            return 0;
        }
        this.energyJournal.updateSnapshots(transaction);
        this.energy += inserted;
        return (int) inserted;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        long extracted = Math.min(this.energy, Math.min(amount, this.maxTransfer));
        if (extracted <= 0) {
            return 0;
        }
        this.energyJournal.updateSnapshots(transaction);
        this.energy -= extracted;
        return (int) extracted;
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putLong("energy", this.energy);
    }

    @Override
    public void deserialize(ValueInput input) {
        this.energy = Math.max(0L, input.getLongOr("energy", 0L));
    }

    private class EnergyJournal extends SnapshotJournal<Long> {
        @Override
        protected Long createSnapshot() {
            return energy;
        }

        @Override
        protected void revertToSnapshot(Long snapshot) {
            energy = snapshot;
        }
    }
}
