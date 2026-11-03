package com.teameway.nowaycore.common.recipe;

import com.teameway.nowaycore.NoWayCore;
import com.teameway.nowaycore.init.ModRecipeTypes;
import com.teameway.nowaycore.resource.ResourceIngredient;
import com.teameway.nowaycore.resource.ResourceKey;
import com.teameway.nowaycore.resource.ResourceKind;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 多方块配方索引
 *
 * <p>不可变快照，配方重载时整体替换；{@link #version()} 变化即代表需要清理机器侧查询缓存。</p>
 */
public final class MultiblockRecipeIndex {
    private static final AtomicLong VERSION = new AtomicLong();

    private static volatile @Nullable MultiblockRecipeIndex current;

    private final long version;
    private final Map<Identifier, TypeIndex> types;
    private final int recipeCount;
    private final int keyCount;

    private MultiblockRecipeIndex(long version, Map<Identifier, TypeIndex> types, int recipeCount, int keyCount) {
        this.version = version;
        this.types = Map.copyOf(types);
        this.recipeCount = recipeCount;
        this.keyCount = keyCount;
    }

    /**
     * @return 当前索引，未构建时返回 {@code null}
     */
    public static @Nullable MultiblockRecipeIndex current() {
        return current;
    }

    /**
     * 从配方表重建索引
     *
     * @param recipeMap 配方表
     * @param reason    触发原因，仅用于日志
     * @return 新索引
     */
    public static MultiblockRecipeIndex rebuild(RecipeMap recipeMap, String reason) {
        long started = System.nanoTime();
        long version = VERSION.incrementAndGet();

        Collection<RecipeHolder<MultiblockRecipe>> holders = recipeMap.byType(ModRecipeTypes.MULTIBLOCK.get());
        Map<Identifier, List<CompiledRecipe>> grouped = new HashMap<>();
        Set<ResourceKey> allKeys = new HashSet<>();
        for (RecipeHolder<MultiblockRecipe> holder : holders) {
            MultiblockRecipe recipe = holder.value();
            Set<ResourceKey> keys = new HashSet<>();
            boolean wildcard = false;
            for (ResourceIngredient ingredient : recipe.inputs()) {
                switch (ingredient) {
                    case ResourceIngredient.Items items -> {
                        if (items.predicate().items().isEmpty()) {
                            wildcard = true;
                        } else {
                            for (Holder<Item> item : items.predicate().items().orElseThrow()) {
                                item.unwrapKey().ifPresent(key -> keys.add(new ResourceKey(ResourceKind.ITEM, key.identifier())));
                            }
                        }
                    }
                    case ResourceIngredient.Fluids fluids -> {
                        List<Holder<Fluid>> fluidHolders = fluids.ingredient().ingredient().fluids();
                        if (fluidHolders.isEmpty()) {
                            wildcard = true;
                        } else {
                            for (Holder<Fluid> fluid : fluidHolders) {
                                fluid.unwrapKey().ifPresent(key -> keys.add(new ResourceKey(ResourceKind.FLUID, key.identifier())));
                            }
                        }
                    }
                    case ResourceIngredient.Energy ignored -> keys.add(ResourceKey.ENERGY);
                }
            }
            allKeys.addAll(keys);
            grouped.computeIfAbsent(recipe.machine(), ignored -> new java.util.ArrayList<>()).add(
                new CompiledRecipe(holder.id().identifier(), recipe, recipe.priority(), recipe.time(), recipe.variants(), keys, wildcard)
            );
        }

        Map<Identifier, TypeIndex> types = new HashMap<>();
        grouped.forEach((machine, recipes) -> {
            recipes.sort(
                Comparator.comparingInt(CompiledRecipe::priority).reversed()
                    .thenComparing(recipe -> recipe.id().toString())
            );
            types.put(machine, new TypeIndex(machine, recipes));
        });

        MultiblockRecipeIndex index = new MultiblockRecipeIndex(version, types, holders.size(), allKeys.size());
        current = index;
        NoWayCore.LOGGER.info(
            "Rebuilt multiblock recipe index #{} ({}): {} type(s), {} recipe(s), {} resource key(s), {} ms",
            version, reason, types.size(), index.recipeCount, index.keyCount,
            (System.nanoTime() - started) / 1_000_000
        );
        return index;
    }

    /**
     * @return 索引版本号，配方重载后递增
     */
    public long version() {
        return this.version;
    }

    /**
     * 查询指定机器类型的索引
     *
     * @param machine 机器类型 ID
     * @return 索引，不存在时返回 {@code null}
     */
    public @Nullable TypeIndex type(Identifier machine) {
        return this.types.get(machine);
    }

    /**
     * @return 配方总数
     */
    public int recipeCount() {
        return this.recipeCount;
    }
}