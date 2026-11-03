package com.teameway.nowaycore.common.multiblock.machine;

import com.teameway.nowaycore.common.recipe.CompiledRecipe;
import com.teameway.nowaycore.common.recipe.MultiblockRecipeIndex;
import com.teameway.nowaycore.common.recipe.TypeIndex;
import com.teameway.nowaycore.resource.ResourceIngredient;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

import java.util.BitSet;
import java.util.Set;

/**
 * 配方匹配
 *
 * <p>先用索引位图剔除“缺资源”的候选，再按优先级从低到高逐条尝试分配输入。分配结果同时可以作为
 * 事务的扣取计划，保证“匹配”和“消耗”用的是同一份数据。</p>
 */
public final class RecipeMatcher {
    private RecipeMatcher() {
    }

    /**
     * 找到当前线程可以执行且优先级最高的配方
     *
     * @param index     配方索引
     * @param machine   机器类型
     * @param variants  机器形态
     * @param resources 输入快照
     * @param ledger    预留账本
     * @return 配方，找不到返回 {@code null}
     */
    public static @Nullable Match findBest(
        MultiblockRecipeIndex index,
        Identifier machine,
        Set<Identifier> variants,
        MachineResources resources,
        ReservationLedger ledger,
        int maxBatch
    ) {
        TypeIndex typeIndex = index.type(machine);
        if (typeIndex == null) {
            return null;
        }
        BitSet candidates = typeIndex.candidates(resources.keys(), variants);
        for (int bit = candidates.nextSetBit(0); bit >= 0; bit = candidates.nextSetBit(bit + 1)) {
            CompiledRecipe recipe = typeIndex.recipes().get(bit);
            for (int repeats = Math.max(1, maxBatch); repeats >= 1; repeats--) {
                ResourceAllocation allocation = plan(recipe, resources, ledger, repeats);
                if (allocation != null) {
                    return new Match(recipe, repeats, allocation);
                }
            }
        }
        return null;
    }

    /**
     * 计算一次配方执行的输入分配
     *
     * @param recipe    配方
     * @param resources 输入快照
     * @param ledger    预留账本
     * @return 分配结果；材料不足返回 {@code null}
     */
    public static @Nullable ResourceAllocation plan(
        CompiledRecipe recipe,
        MachineResources resources,
        ReservationLedger ledger
    ) {
        return plan(recipe, resources, ledger, 1);
    }

    /**
     * 计算指定并行倍数的输入分配
     *
     * @param recipe    配方
     * @param resources 输入快照
     * @param ledger    预留账本
     * @param repeats   并行倍数
     * @return 分配结果；材料不足返回 {@code null}
     */
    public static @Nullable ResourceAllocation plan(
        CompiledRecipe recipe,
        MachineResources resources,
        ReservationLedger ledger,
        int repeats
    ) {
        long[] freeItems = resources.freeItemAmounts(ledger);
        long[] freeFluids = resources.freeFluidAmounts(ledger);
        long freeEnergy = resources.freeEnergy(ledger);
        long[] takeItems = new long[freeItems.length];
        long[] takeFluids = new long[freeFluids.length];
        long takeEnergy = 0L;

        // 先处理会被消耗的输入，催化剂在扣完之后再校验，避免“消耗 + 催化剂”共享同一批材料
        for (ResourceIngredient ingredient : recipe.recipe().inputs()) {
            if (!ingredient.consumed()) {
                continue;
            }
            switch (ingredient) {
                case ResourceIngredient.Items items -> {
                    long remaining = (long) items.predicate().count() * repeats;
                    for (int slot = 0; slot < freeItems.length && remaining > 0; slot++) {
                        if (freeItems[slot] <= 0 || !matches(resources, slot, items)) {
                            continue;
                        }
                        long taken = Math.min(remaining, freeItems[slot]);
                        takeItems[slot] += taken;
                        freeItems[slot] -= taken;
                        remaining -= taken;
                    }
                    if (remaining > 0) {
                        return null;
                    }
                }
                case ResourceIngredient.Fluids fluids -> {
                    long remaining = (long) fluids.ingredient().amount() * repeats;
                    for (int slot = 0; slot < freeFluids.length && remaining > 0; slot++) {
                        if (freeFluids[slot] <= 0 || !matches(resources, slot, fluids)) {
                            continue;
                        }
                        long taken = Math.min(remaining, freeFluids[slot]);
                        takeFluids[slot] += taken;
                        freeFluids[slot] -= taken;
                        remaining -= taken;
                    }
                    if (remaining > 0) {
                        return null;
                    }
                }
                case ResourceIngredient.Energy energy -> {
                    long needed = energy.amount() * repeats;
                    if (freeEnergy < needed) {
                        return null;
                    }
                    freeEnergy -= needed;
                    takeEnergy += needed;
                }
            }
        }

        // 催化剂只要求“消耗后仍然存在”
        for (ResourceIngredient ingredient : recipe.recipe().inputs()) {
            if (ingredient.consumed()) {
                continue;
            }
            long available = switch (ingredient) {
                case ResourceIngredient.Items items -> {
                    long total = 0L;
                    for (int slot = 0; slot < freeItems.length; slot++) {
                        if (freeItems[slot] > 0 && matches(resources, slot, items)) {
                            total += freeItems[slot];
                        }
                    }
                    yield total;
                }
                case ResourceIngredient.Fluids fluids -> {
                    long total = 0L;
                    for (int slot = 0; slot < freeFluids.length; slot++) {
                        if (freeFluids[slot] > 0 && matches(resources, slot, fluids)) {
                            total += freeFluids[slot];
                        }
                    }
                    yield total;
                }
                case ResourceIngredient.Energy ignored -> freeEnergy;
            };
            long required = required(ingredient);
            if (available < required) {
                return null;
            }
        }

        return new ResourceAllocation(takeItems, takeFluids, takeEnergy);
    }

    /**
     * 匹配结果
     *
     * @param recipe     配方
     * @param batch      并行倍数
     * @param allocation 输入分配
     */
    public record Match(CompiledRecipe recipe, int batch, ResourceAllocation allocation) {
    }

    private static boolean matches(MachineResources resources, int slot, ResourceIngredient.Items items) {
        ItemResource resource = resources.itemResource(slot);
        return !resource.isEmpty() && items.predicate().testIgnoreCount(resource.toStack(1));
    }

    private static boolean matches(MachineResources resources, int slot, ResourceIngredient.Fluids fluids) {
        FluidResource resource = resources.fluidResource(slot);
        return !resource.isEmpty() && fluids.ingredient().ingredient().test(resource.toStack(1));
    }

    private static long required(ResourceIngredient ingredient) {
        return switch (ingredient) {
            case ResourceIngredient.Items items -> items.predicate().count();
            case ResourceIngredient.Fluids fluids -> fluids.ingredient().amount();
            case ResourceIngredient.Energy energy -> energy.amount();
        };
    }
}