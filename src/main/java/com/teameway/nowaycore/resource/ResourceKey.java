package com.teameway.nowaycore.resource;

import com.teameway.nowaycore.NoWayCore;
import net.minecraft.resources.Identifier;

/**
 * 索引使用的资源键
 *
 * <p>物品/流体用具体 ID（tag 在编译时展开），能量是固定键。索引只关心“有没有”，
 * 数量校验留给匹配阶段。</p>
 *
 * @param kind 资源种类
 * @param id   资源 ID
 */
public record ResourceKey(ResourceKind kind, Identifier id) {
    /**
     * 能量固定键
     */
    public static final ResourceKey ENERGY = new ResourceKey(ResourceKind.ENERGY, NoWayCore.id("energy"));
}
