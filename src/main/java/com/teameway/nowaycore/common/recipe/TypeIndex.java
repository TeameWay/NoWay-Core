package com.teameway.nowaycore.common.recipe;

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
 * <p>位序与 {@link #recipes()} 一致，且按优先级降序排好，因此从低位到高位遍历即为优先级顺序。</p>
 *
 * <p>形态过滤采用子集语义：只有配方要求的全部形态都出现在机器当前形态里，配方才参与匹配。
 * 于是无形态要求的配方对任何机器有效，而没有形态的机器只能匹配无形态要求的配方。</p>
 */
public final class TypeIndex {
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
     * <p>做法是“剔除”：从形态允许的配方里减去所有“需要某个当前不可用资源”的配方，剩下的即为超集。
     * 带通配输入的配方不会被剔除，留到后续数量校验阶段处理。</p>
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
     * 取形态过滤位图，按形态集合缓存
     *
     * <p>同一形态组合会被大量机器复用，缓存后每次查询只需拷贝一份位图。</p>
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

    /**
     * @return 机器类型 ID
     */
    public Identifier machine() {
        return this.machine;
    }

    /**
     * @return 按优先级排序的配方列表
     */
    public List<CompiledRecipe> recipes() {
        return this.recipes;
    }

    /**
     * @return 配方数量
     */
    public int size() {
        return this.recipes.size();
    }
}
