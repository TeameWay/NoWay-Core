package com.teameway.nowaycore.common.multiblock.port;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * 多方块端口种类
 *
 * <p>输入/输出方向写死在种类里，便于扫描后直接分组；扩展新资源时加一个种类即可。</p>
 */
public enum PortKind implements StringRepresentable {
    /**
     * 物品输入（输入总线）
     */
    ITEM_INPUT("item_input", true),
    /**
     * 物品输出（输出总线）
     */
    ITEM_OUTPUT("item_output", false),
    /**
     * 流体输入（输入仓）
     */
    FLUID_INPUT("fluid_input", true),
    /**
     * 流体输出（输出仓）
     */
    FLUID_OUTPUT("fluid_output", false),
    /**
     * 能量输入
     */
    ENERGY_INPUT("energy_input", true),
    /**
     * 能量输出
     */
    ENERGY_OUTPUT("energy_output", false),
    /**
     * 并行仓：提供每线程并行倍数
     */
    PARALLEL("parallel", false),
    /**
     * 线程仓：提供线程数量
     */
    THREAD("thread", false);

    public static final Codec<PortKind> CODEC = StringRepresentable.fromEnum(PortKind::values);

    private final String name;
    private final boolean input;

    PortKind(String name, boolean input) {
        this.name = name;
        this.input = input;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /**
     * @return 是否为输入方向
     */
    public boolean input() {
        return this.input;
    }
}
