package com.teameway.nowaycore.common.multiblock.machine;

/**
 * 线程间预留账本
 *
 * <p>同一 tick 内多个线程依次匹配，后续线程的可用量要扣掉前面线程的预留，避免重复占用；账本按槽位记录，周期结束清空。</p>
 */
public class ReservationLedger {
    private final long[] items;
    private final long[] fluids;
    private long energy;

    /**
     * @param itemSlots  物品输入槽数量
     * @param fluidSlots 流体输入槽数量
     */
    public ReservationLedger(int itemSlots, int fluidSlots) {
        this.items = new long[itemSlots];
        this.fluids = new long[fluidSlots];
    }

    public long reservedItem(int slot) {
        return this.items[slot];
    }

    public long reservedFluid(int slot) {
        return this.fluids[slot];
    }

    public long reservedEnergy() {
        return this.energy;
    }

    /**
     * 记录一次预留
     *
     * @param allocation 分配结果
     */
    public void reserve(ResourceAllocation allocation) {
        for (int i = 0; i < this.items.length; i++) {
            this.items[i] += allocation.items()[i];
        }
        for (int i = 0; i < this.fluids.length; i++) {
            this.fluids[i] += allocation.fluids()[i];
        }
        this.energy += allocation.energy();
    }

    /**
     * 清空账本（每周期开始时调用）
     */
    public void clear() {
        java.util.Arrays.fill(this.items, 0L);
        java.util.Arrays.fill(this.fluids, 0L);
        this.energy = 0L;
    }
}