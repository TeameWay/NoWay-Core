package com.teameway.nowaycore.recipe;

import com.teameway.nowaycore.resource.ResourceKey;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Set;

/**
 * 编译后的配方
 *
 * <p>索引层只保留匹配需要的字段：排序用的 priority、位图用的资源键、形态过滤用的 variant。
 * 原始配方通过 {@link #recipe()} 取回，用于数量校验和产出结算。</p>
 *
 * @param id       配方 ID
 * @param recipe   原始配方
 * @param priority 排序优先级（越大越先）
 * @param time     单周期耗时
 * @param variants 允许的形态
 * @param keys     输入涉及的资源键（tag 已展开）
 * @param wildcard 是否包含无法展开的输入（如“任意物品”），这类配方不能被“缺资源”规则剔除
 */
public record CompiledRecipe(
    Identifier id,
    MultiblockRecipe recipe,
    int priority,
    int time,
    List<Identifier> variants,
    Set<ResourceKey> keys,
    boolean wildcard
) {
    public CompiledRecipe {
        variants = List.copyOf(variants);
        keys = Set.copyOf(keys);
    }
}
