package com.teameway.nowaycore.recipe;

import com.teameway.nowaycore.resource.ResourceKey;
import net.minecraft.resources.Identifier;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单一机器类型的配方索引
 *
 * <p>位序与 {@link #recipes()} 一致，位图从低位到高位遍历即按优先级降序。形态过滤为子集语义，
 * 配方要求的形态必须全部出现在机器当前形态里，无形态要求的配方对任何机器有效。</p>
 */
public class TypeIndex {
    private final Identifier machine;
    private final List<CompiledRecipe> recipes;
    private final BitSet all;
    private final Map<ResourceKey, BitSet> byKey;
    private final Map<Set<Identifier>, BitSet> byVariantSet;
    private final Map<Set<Identifier>, BitSet> variantMasks = new ConcurrentHashMap<>();

    TypeIndex(Identifier machine, List<CompiledRecipe> recipes) {
        this.machine = machine;
        this.recipes = List.copyOf(recipes);
        this.all = new BitSet(this.recipes.size());
        this.all.set(0, this.recipes.size());

        Map<ResourceKey, BitSet> keyIndex = new HashMap<>();
        Map<Set<Identifier>, BitSet> variantIndex = new HashMap<>();
        for (int i = 0; i < this.recipes.size(); i++) {
            CompiledRecipe recipe = this.recipes.get(i);
            for (ResourceKey key : recipe.keys()) {
                keyIndex.computeIfAbsent(key, ignored -> new BitSet(this.recipes.size())).set(i);
            }
            variantIndex.computeIfAbsent(Set.copyOf(recipe.variants()), ignored -> new BitSet(this.recipes.size())).set(i);
        }
        this.byKey = Map.copyOf(keyIndex);
        this.byVariantSet = Map.copyOf(variantIndex);
    }

    /**
     * 计算候选配方位图
     *
     * <p>从形态允许的配方里减去需要当前不可用资源的配方。带通配输入的配方不会被剔除，
     * 留到后续数量校验阶段处理。</p>
     *
     * @param available 当前可用的资源键
     * @param variants  机器当前形态，必须是不可变集合
     * @return 可修改的候选位图
     */
    public BitSet candidates(Set<ResourceKey> available, Set<Identifier> variants) {
        BitSet result = (BitSet) this.variantMask(variants).clone();
        for (Map.Entry<ResourceKey, BitSet> entry : this.byKey.entrySet()) {
            if (!available.contains(entry.getKey())) {
                result.andNot(entry.getValue());
            }
        }
        return result;
    }

    /**
     * 取形态过滤位图，按形态集合缓存，缓存命中时只需拷贝一份位图。
     *
     * @param variants 机器当前形态
     * @return 形态允许的配方位图（共享实例，调用方不得修改）
     */
    private BitSet variantMask(Set<Identifier> variants) {
        return this.variantMasks.computeIfAbsent(
            variants, machineVariants -> {
                BitSet mask = new BitSet(this.recipes.size());
                for (Map.Entry<Set<Identifier>, BitSet> entry : this.byVariantSet.entrySet()) {
                    if (machineVariants.containsAll(entry.getKey())) {
                        mask.or(entry.getValue());
                    }
                }
                return mask;
            }
        );
    }

    public Identifier machine() {
        return this.machine;
    }

    /**
     * @return 按优先级降序排列的配方列表
     */
    public List<CompiledRecipe> recipes() {
        return this.recipes;
    }

    public int size() {
        return this.recipes.size();
    }
}