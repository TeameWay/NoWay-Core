package com.teameway.nowaycore.common.multiblock.machine;

import com.teameway.nowaycore.NoWayCore;
import com.teameway.nowaycore.common.multiblock.port.MultiblockPortTable;
import com.teameway.nowaycore.resource.ResourceOutput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.Resource;
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
public class RecipeExecutor {
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

    /**
     * 相乘并饱和到 {@link Long#MAX_VALUE}
     *
     * <p>产出数量与并行倍数都可能很大，直接相乘会溢出成负数，那一步会被当成“没有产出”而静默跳过。</p>
     *
     * @param value 单次产出
     * @param batch 并行倍数
     * @return 总产出
     */
    private static long saturatedMultiply(long value, int batch) {
        return value > Long.MAX_VALUE / Math.max(1, batch) ? Long.MAX_VALUE : value * Math.max(1, batch);
    }

    private static boolean consumeInputs(Transaction transaction, MachineResources resources, ResourceAllocation allocation) {
        for (int slot = 0; slot < allocation.items().length; slot++) {
            long amount = allocation.items()[slot];
            if (amount <= 0) {
                continue;
            }
            ItemResource resource = resources.itemResource(slot);
            if (!extractAll(resources.itemInputs(), slot, resource, amount, transaction)) {
                return false;
            }
        }
        for (int slot = 0; slot < allocation.fluids().length; slot++) {
            long amount = allocation.fluids()[slot];
            if (amount <= 0) {
                continue;
            }
            FluidResource resource = resources.fluidResource(slot);
            if (!extractAll(resources.fluidInputs(), slot, resource, amount, transaction)) {
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

    /**
     * 按 {@code int} 上限分批取出，直到取满
     *
     * <p>Transfer API 的 {@code extract} 只接受 {@code int}，直接强转会让超出部分变成负数。
     * 分批取同时也保留了“取不满就整批回滚”的语义。</p>
     *
     * @return 是否取满
     */
    private static <T extends Resource> boolean extractAll(
        ResourceHandler<T> handler,
        int slot,
        T resource,
        long amount,
        Transaction transaction
    ) {
        long remaining = amount;
        while (remaining > 0) {
            int chunk = (int) Math.min(remaining, Integer.MAX_VALUE);
            int extracted = handler.extract(slot, resource, chunk, transaction);
            if (extracted <= 0) {
                return false;
            }
            remaining -= extracted;
        }
        return true;
    }

    /**
     * 按 {@code int} 上限分批塞入，直到塞满
     *
     * @return 是否塞满了要求的数量
     */
    private static <T extends Resource> boolean insertAll(
        ResourceHandler<T> handler,
        T resource,
        long amount,
        Transaction transaction
    ) {
        long remaining = amount;
        while (remaining > 0) {
            int chunk = (int) Math.min(remaining, Integer.MAX_VALUE);
            int inserted = handler.insert(resource, chunk, transaction);
            if (inserted <= 0) {
                return false;
            }
            remaining -= inserted;
        }
        return true;
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
            long amount = saturatedMultiply(output.amount().getInt(context), batch);
            if (amount <= 0) {
                continue;
            }
            switch (output) {
                case ResourceOutput.Items items -> {
                    if (!insertAll(table.itemOutputs(), ItemResource.of(items.template()), amount, transaction)) {
                        return false;
                    }
                }
                case ResourceOutput.Fluids fluids -> {
                    if (!insertAll(table.fluidOutputs(), FluidResource.of(fluids.fluid()), amount, transaction)) {
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