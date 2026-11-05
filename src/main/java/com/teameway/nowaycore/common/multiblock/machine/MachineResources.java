package com.teameway.nowaycore.common.multiblock.machine;

import com.teameway.nowaycore.common.multiblock.port.MultiblockPortTable;
import com.teameway.nowaycore.resource.ResourceKey;
import com.teameway.nowaycore.resource.ResourceKind;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 机器输入快照
 *
 * <p>把端口表的输入池展开成按槽位的可用量，供匹配与事务使用；输出池不在这里，由产出阶段单独处理。</p>
 */
public class MachineResources {
    private final ResourceHandler<ItemResource> itemInputs;
    private final ResourceHandler<FluidResource> fluidInputs;
    private final List<EnergyHandler> energyInputs;

    private MachineResources(
        ResourceHandler<ItemResource> itemInputs,
        ResourceHandler<FluidResource> fluidInputs,
        List<EnergyHandler> energyInputs
    ) {
        this.itemInputs = itemInputs;
        this.fluidInputs = fluidInputs;
        this.energyInputs = energyInputs;
    }

    /**
     * 从端口表构建输入快照
     *
     * @param table 端口表
     * @return 输入快照
     */
    public static MachineResources of(MultiblockPortTable table) {
        return new MachineResources(table.itemInputs(), table.fluidInputs(), table.energyInputs());
    }

    public int itemSlotCount() {
        return this.itemInputs.size();
    }

    public int fluidSlotCount() {
        return this.fluidInputs.size();
    }

    public ItemResource itemResource(int slot) {
        return this.itemInputs.getResource(slot);
    }

    public FluidResource fluidResource(int slot) {
        return this.fluidInputs.getResource(slot);
    }

    /**
     * 计算每个物品槽的可用量（扣除预留）
     *
     * @param ledger 预留账本
     * @return 按槽位的可用量
     */
    public long[] freeItemAmounts(ReservationLedger ledger) {
        long[] free = new long[this.itemSlotCount()];
        for (int slot = 0; slot < free.length; slot++) {
            free[slot] = Math.max(0L, this.itemInputs.getAmountAsLong(slot) - ledger.reservedItem(slot));
        }
        return free;
    }

    /**
     * 计算每个流体槽的可用量（扣除预留）
     *
     * @param ledger 预留账本
     * @return 按槽位的可用量
     */
    public long[] freeFluidAmounts(ReservationLedger ledger) {
        long[] free = new long[this.fluidSlotCount()];
        for (int slot = 0; slot < free.length; slot++) {
            free[slot] = Math.max(0L, this.fluidInputs.getAmountAsLong(slot) - ledger.reservedFluid(slot));
        }
        return free;
    }

    /**
     * 计算可用能量（扣除预留）
     *
     * @param ledger 预留账本
     * @return 可用能量
     */
    public long freeEnergy(ReservationLedger ledger) {
        long total = 0L;
        for (EnergyHandler handler : this.energyInputs) {
            total += handler.getAmountAsLong();
        }
        return Math.max(0L, total - ledger.reservedEnergy());
    }

    /**
     * 收集当前可用资源键，供索引查询使用
     *
     * @return 资源键集合
     */
    public Set<ResourceKey> keys() {
        Set<ResourceKey> keys = new HashSet<>();
        for (int slot = 0; slot < this.itemSlotCount(); slot++) {
            if (this.itemInputs.getAmountAsLong(slot) <= 0) {
                continue;
            }
            ItemResource resource = this.itemInputs.getResource(slot);
            if (!resource.isEmpty()) {
                keys.add(new ResourceKey(ResourceKind.ITEM, BuiltInRegistries.ITEM.getKey(resource.getItem())));
            }
        }
        for (int slot = 0; slot < this.fluidSlotCount(); slot++) {
            if (this.fluidInputs.getAmountAsLong(slot) <= 0) {
                continue;
            }
            FluidResource resource = this.fluidInputs.getResource(slot);
            if (!resource.isEmpty()) {
                keys.add(new ResourceKey(ResourceKind.FLUID, BuiltInRegistries.FLUID.getKey(resource.getFluid())));
            }
        }
        for (EnergyHandler handler : this.energyInputs) {
            if (handler.getAmountAsLong() > 0) {
                keys.add(ResourceKey.ENERGY);
                break;
            }
        }
        return keys;
    }

    /**
     * @return 物品输入池（供事务取用）
     */
    public ResourceHandler<ItemResource> itemInputs() {
        return this.itemInputs;
    }

    /**
     * @return 流体输入池
     */
    public ResourceHandler<FluidResource> fluidInputs() {
        return this.fluidInputs;
    }

    /**
     * @return 能量输入处理器
     */
    public List<EnergyHandler> energyInputs() {
        return this.energyInputs;
    }
}