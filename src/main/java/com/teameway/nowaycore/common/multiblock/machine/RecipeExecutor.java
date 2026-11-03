package com.teameway.nowaycore.common.multiblock.machine;

import com.teameway.nowaycore.NoWayCore;
import com.teameway.nowaycore.common.multiblock.port.MultiblockPortTable;
import com.teameway.nowaycore.resource.ResourceOutput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 配方执行
 *
 * <p>整个过程放进一个 {@link Transaction}：先扣输入，再塞产出；任何一步失败（材料被别处拿走、
 * 输出池放不下）都回滚，保证“要么整批成功，要么完全不动”。</p>
 */
public final class RecipeExecutor {
    private RecipeExecutor() {
    }

    /**
     * 执行一次配方
     *
     * @param level      服务端世界
     * @param table      端口表
     * @param resources  输入快照
     * @param allocation 匹配阶段给出的输入分配
     * @param outputs    产出列表
     * @param batch      本次执行的并行倍数
     * @param random     随机源（用于概率产出）
     * @return 执行结果
     */
    public static CraftResult craft(
        ServerLevel level,
        MultiblockPortTable table,
        MachineResources resources,
        ResourceAllocation allocation,
        List<ResourceOutput> outputs,
        int batch,
        RandomSource random
    ) {
        LootContext context = new LootContext.Builder(
            new LootParams(level, ContextMap.EMPTY, Map.of(), 0)
        ).create(Optional.empty());

        try (Transaction transaction = Transaction.openRoot()) {
            if (!consumeInputs(transaction, resources, allocation)) {
                return CraftResult.MISSING_INPUT;
            }
            if (!insertOutputs(transaction, table, outputs, context, random, Math.max(1, batch))) {
                return CraftResult.OUTPUT_BLOCKED;
            }
            transaction.commit();
            return CraftResult.SUCCESS;
        } catch (RuntimeException exception) {
            NoWayCore.LOGGER.error("Multiblock recipe execution failed", exception);
            return CraftResult.FAILED;
        }
    }

    private static boolean consumeInputs(Transaction transaction, MachineResources resources, ResourceAllocation allocation) {
        for (int slot = 0; slot < allocation.items().length; slot++) {
            long amount = allocation.items()[slot];
            if (amount <= 0) {
                continue;
            }
            ItemResource resource = resources.itemResource(slot);
            if (resources.itemInputs().extract(slot, resource, (int) amount, transaction) != amount) {
                return false;
            }
        }
        for (int slot = 0; slot < allocation.fluids().length; slot++) {
            long amount = allocation.fluids()[slot];
            if (amount <= 0) {
                continue;
            }
            FluidResource resource = resources.fluidResource(slot);
            if (resources.fluidInputs().extract(slot, resource, (int) amount, transaction) != amount) {
                return false;
            }
        }
        long remaining = allocation.energy();
        for (EnergyHandler handler : resources.energyInputs()) {
            if (remaining <= 0) {
                break;
            }
            int chunk = (int) Math.min(remaining, Integer.MAX_VALUE);
            int extracted = handler.extract(chunk, transaction);
            remaining -= extracted;
        }
        return remaining <= 0;
    }

    private static boolean insertOutputs(
        Transaction transaction,
        MultiblockPortTable table,
        List<ResourceOutput> outputs,
        LootContext context,
        RandomSource random,
        int batch
    ) {
        for (ResourceOutput output : outputs) {
            if (random.nextFloat() >= output.chance()) {
                continue;
            }
            long amount = (long) output.amount().getInt(context) * batch;
            if (amount <= 0) {
                continue;
            }
            switch (output) {
                case ResourceOutput.Items items -> {
                    ItemResource resource = ItemResource.of(items.template());
                    if (table.itemOutputs().insert(resource, (int) Math.min(amount, Integer.MAX_VALUE), transaction) != amount) {
                        return false;
                    }
                }
                case ResourceOutput.Fluids fluids -> {
                    FluidResource resource = FluidResource.of(fluids.fluid());
                    if (table.fluidOutputs().insert(resource, (int) Math.min(amount, Integer.MAX_VALUE), transaction) != amount) {
                        return false;
                    }
                }
                case ResourceOutput.Energy energy -> {
                    long remaining = amount;
                    for (EnergyHandler handler : table.energyOutputs()) {
                        if (remaining <= 0) {
                            break;
                        }
                        int chunk = (int) Math.min(remaining, Integer.MAX_VALUE);
                        remaining -= handler.insert(chunk, transaction);
                    }
                    if (remaining > 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}