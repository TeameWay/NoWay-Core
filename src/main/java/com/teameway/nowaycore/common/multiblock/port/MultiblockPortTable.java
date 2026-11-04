package com.teameway.nowaycore.common.multiblock.port;

import com.teameway.nowaycore.NoWayCore;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.EmptyResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.Resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 机器的端口表
 *
 * <p>成型时扫描一次，之后只在部件增删时重建。同种类的端口会被合并成一个
 * {@link CombinedResourceHandler}，即“池”：容量相加、按槽序扣取。</p>
 */
public class MultiblockPortTable {
    private final Map<PortKind, List<MultiblockPort>> byKind;
    private final int portCount;
    private final int threads;
    private final int parallels;
    private final ResourceHandler<ItemResource> itemInputs;
    private final ResourceHandler<ItemResource> itemOutputs;
    private final ResourceHandler<FluidResource> fluidInputs;
    private final ResourceHandler<FluidResource> fluidOutputs;
    private final List<EnergyHandler> energyInputs;
    private final List<EnergyHandler> energyOutputs;

    private MultiblockPortTable(Map<PortKind, List<MultiblockPort>> byKind, int portCount) {
        this.byKind = byKind;
        this.portCount = portCount;
        this.threads = 1 + sumDelta(byKind.getOrDefault(PortKind.THREAD, List.of()), true);
        this.parallels = Math.max(1, sumDelta(byKind.getOrDefault(PortKind.PARALLEL, List.of()), false));

        List<ResourceHandler<ItemResource>> itemIn = new ArrayList<>();
        List<ResourceHandler<ItemResource>> itemOut = new ArrayList<>();
        List<ResourceHandler<FluidResource>> fluidIn = new ArrayList<>();
        List<ResourceHandler<FluidResource>> fluidOut = new ArrayList<>();
        List<EnergyHandler> energyIn = new ArrayList<>();
        List<EnergyHandler> energyOut = new ArrayList<>();

        for (MultiblockPort port : byKind.getOrDefault(PortKind.ITEM_INPUT, List.of())) {
            if (port instanceof ItemPort item) {
                itemIn.add(item.itemHandler());
            } else {
                warnTypeMismatch(port, PortKind.ITEM_INPUT);
            }
        }
        for (MultiblockPort port : byKind.getOrDefault(PortKind.ITEM_OUTPUT, List.of())) {
            if (port instanceof ItemPort item) {
                itemOut.add(item.itemHandler());
            } else {
                warnTypeMismatch(port, PortKind.ITEM_OUTPUT);
            }
        }
        for (MultiblockPort port : byKind.getOrDefault(PortKind.FLUID_INPUT, List.of())) {
            if (port instanceof FluidPort fluid) {
                fluidIn.add(fluid.fluidHandler());
            } else {
                warnTypeMismatch(port, PortKind.FLUID_INPUT);
            }
        }
        for (MultiblockPort port : byKind.getOrDefault(PortKind.FLUID_OUTPUT, List.of())) {
            if (port instanceof FluidPort fluid) {
                fluidOut.add(fluid.fluidHandler());
            } else {
                warnTypeMismatch(port, PortKind.FLUID_OUTPUT);
            }
        }
        for (MultiblockPort port : byKind.getOrDefault(PortKind.ENERGY_INPUT, List.of())) {
            if (port instanceof EnergyPort energy) {
                energyIn.add(energy.energyHandler());
            } else {
                warnTypeMismatch(port, PortKind.ENERGY_INPUT);
            }
        }
        for (MultiblockPort port : byKind.getOrDefault(PortKind.ENERGY_OUTPUT, List.of())) {
            if (port instanceof EnergyPort energy) {
                energyOut.add(energy.energyHandler());
            } else {
                warnTypeMismatch(port, PortKind.ENERGY_OUTPUT);
            }
        }

        this.itemInputs = combine(itemIn);
        this.itemOutputs = combine(itemOut);
        this.fluidInputs = combine(fluidIn);
        this.fluidOutputs = combine(fluidOut);
        this.energyInputs = List.copyOf(energyIn);
        this.energyOutputs = List.copyOf(energyOut);
    }

    /**
     * 由端口集合构建端口表
     *
     * @param ports 端口集合
     * @return 端口表
     */
    public static MultiblockPortTable of(Collection<MultiblockPort> ports) {
        Map<PortKind, List<MultiblockPort>> byKind = new EnumMap<>(PortKind.class);
        for (MultiblockPort port : ports) {
            byKind.computeIfAbsent(port.portKind(), ignored -> new ArrayList<>()).add(port);
        }
        return new MultiblockPortTable(byKind, ports.size());
    }

    private static int sumDelta(List<MultiblockPort> ports, boolean threads) {
        int total = 0;
        for (MultiblockPort port : ports) {
            total += threads ? port.threadsDelta() : port.parallelsDelta();
        }
        return total;
    }

    private static void warnTypeMismatch(MultiblockPort port, PortKind expected) {
        NoWayCore.LOGGER.warn(
            "Port at {} declared {} but does not implement the matching handler interface",
            port, expected
        );
    }

    private static <T extends Resource> ResourceHandler<T> combine(List<ResourceHandler<T>> handlers) {
        if (handlers.isEmpty()) {
            return EmptyResourceHandler.instance();
        }
        if (handlers.size() == 1) {
            return handlers.getFirst();
        }
        return new CombinedResourceHandler<>(handlers);
    }

    /**
     * @param kind 端口种类
     * @return 该种类的端口
     */
    public List<MultiblockPort> ports(PortKind kind) {
        return this.byKind.getOrDefault(kind, List.of());
    }

    /**
     * @return 端口总数
     */
    public int portCount() {
        return this.portCount;
    }

    /**
     * @return 线程数（基础 1 + 线程仓增量）
     */
    public int threads() {
        return this.threads;
    }

    /**
     * @return 每线程并行倍数（并行仓总增量，至少 1）
     */
    public int parallels() {
        return this.parallels;
    }

    /**
     * @return 合并后的物品输入池
     */
    public ResourceHandler<ItemResource> itemInputs() {
        return this.itemInputs;
    }

    /**
     * @return 合并后的物品输出池
     */
    public ResourceHandler<ItemResource> itemOutputs() {
        return this.itemOutputs;
    }

    /**
     * @return 合并后的流体输入池
     */
    public ResourceHandler<FluidResource> fluidInputs() {
        return this.fluidInputs;
    }

    /**
     * @return 合并后的流体输出池
     */
    public ResourceHandler<FluidResource> fluidOutputs() {
        return this.fluidOutputs;
    }

    /**
     * @return 能量输入处理器
     */
    public List<EnergyHandler> energyInputs() {
        return this.energyInputs;
    }

    /**
     * @return 能量输出处理器
     */
    public List<EnergyHandler> energyOutputs() {
        return this.energyOutputs;
    }
}