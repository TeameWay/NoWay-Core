package com.teameway.nowaycore.common.multiblock.port;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 端口等级
 *
 * <p>各等级共用同一记录，按端口种类取对应字段。</p>
 *
 * @param name           等级名
 * @param slots          物品槽位数量
 * @param fluidCapacity  流体容量（mB）
 * @param energyCapacity 能量容量（FE）
 * @param threads        线程增量
 * @param parallels      并行增量
 */
public record PortTier(
    String name,
    int slots,
    int fluidCapacity,
    int energyCapacity,
    int threads,
    int parallels
) {
    public static final Codec<PortTier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("name").forGetter(PortTier::name),
        Codec.INT.optionalFieldOf("slots", 9).forGetter(PortTier::slots),
        Codec.INT.optionalFieldOf("fluid_capacity", 8000).forGetter(PortTier::fluidCapacity),
        Codec.INT.optionalFieldOf("energy_capacity", 100_000).forGetter(PortTier::energyCapacity),
        Codec.INT.optionalFieldOf("threads", 0).forGetter(PortTier::threads),
        Codec.INT.optionalFieldOf("parallels", 0).forGetter(PortTier::parallels)
    ).apply(instance, PortTier::new));

    /**
     * 只有槽位的物品端口等级
     *
     * @param name  等级名
     * @param slots 槽位数量
     * @return 等级
     */
    public static PortTier items(String name, int slots) {
        return new PortTier(name, slots, 0, 0, 0, 0);
    }

    /**
     * 只有容量的流体端口等级
     *
     * @param name     等级名
     * @param capacity 容量（mB）
     * @return 等级
     */
    public static PortTier fluids(String name, int capacity) {
        return new PortTier(name, 0, capacity, 0, 0, 0);
    }

    /**
     * 只有容量的能量端口等级
     *
     * @param name     等级名
     * @param capacity 容量（FE）
     * @return 等级
     */
    public static PortTier energy(String name, int capacity) {
        return new PortTier(name, 0, 0, capacity, 0, 0);
    }

    /**
     * 并行仓等级
     *
     * @param name      等级名
     * @param parallels 并行增量
     * @return 等级
     */
    public static PortTier parallel(String name, int parallels) {
        return new PortTier(name, 0, 0, 0, 0, parallels);
    }

    /**
     * 线程仓等级
     *
     * @param name    等级名
     * @param threads 线程增量
     * @return 等级
     */
    public static PortTier thread(String name, int threads) {
        return new PortTier(name, 0, 0, 0, threads, 0);
    }
}
