package com.teameway.nowaycore.common.recipe;

import com.teameway.nowaycore.resource.ResourceKey;
import net.minecraft.resources.Identifier;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 单一机器类型的配方索引
 *
 * <p>位序与 {@link #recipes()} 一致，且按优先级降序排好，因此从低位到高位遍历即为优先级顺序。</p>
 */
public final class TypeIndex {
    private static final BitSet EMPTY = new BitSet();

    private final Identifier machine;
    private final List<CompiledRecipe> recipes;
    private final BitSet all;
    private final Map<ResourceKey, BitSet> byKey;
    private final Map<Identifier, BitSet> byVariant;

    TypeIndex(Identifier machine, List<CompiledRecipe> recipes) {
        this.machine = machine;
        this.recipes = List.copyOf(recipes);
        this.all = new BitSet(this.recipes.size());
        this.all.set(0, this.recipes.size());

        Map<ResourceKey, BitSet> keyIndex = new HashMap<>();
        Map<Identifier, BitSet> variantIndex = new HashMap<>();
        for (int i = 0; i < this.recipes.size(); i++) {
            CompiledRecipe recipe = this.recipes.get(i);
            for (ResourceKey key : recipe.keys()) {
                keyIndex.computeIfAbsent(key, ignored -> new BitSet(this.recipes.size())).set(i);
            }
            for (Identifier variant : recipe.variants()) {
                variantIndex.computeIfAbsent(variant, ignored -> new BitSet(this.recipes.size())).set(i);
            }
        }
        this.byKey = Collections.unmodifiableMap(keyIndex);
        this.byVariant = Collections.unmodifiableMap(variantIndex);
    }

    /**
     * 计算候选配方位图
     *
     * <p>做法是“剔除”：从全集里减去所有“需要某个当前不可用资源”的配方，剩下的即为超集。
     * 带通配输入的配方不会被剔除，留到后续数量校验阶段处理。</p>
     *
     * @param available 当前可用的资源键
     * @param variants  机器当前形态（空表示不过滤）
     * @return 可修改的候选位图
     */
    public BitSet candidates(Set<ResourceKey> available, Set<Identifier> variants) {
        BitSet result = (BitSet) this.all.clone();
        if (!variants.isEmpty()) {
            BitSet mask = new BitSet(this.recipes.size());
            for (Identifier variant : variants) {
                mask.or(this.byVariant.getOrDefault(variant, EMPTY));
            }
            result.and(mask);
        }
        for (Map.Entry<ResourceKey, BitSet> entry : this.byKey.entrySet()) {
            if (!available.contains(entry.getKey())) {
                result.andNot(entry.getValue());
            }
        }
        return result;
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
