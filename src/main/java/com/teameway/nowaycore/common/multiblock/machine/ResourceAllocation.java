package com.teameway.nowaycore.common.multiblock.machine;

/**
 * 一次配方匹配的输入分配结果
 *
 * <p>下标是端口池内的槽位序号，值是从该槽取走的数量；能量单独记一个总数。</p>
 *
 * @param items  物品槽取用数量
 * @param fluids 流体槽取用数量
 * @param energy 能量取用量
 */
public record ResourceAllocation(long[] items, long[] fluids, long energy) {
}
