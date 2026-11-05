package com.teameway.nowaycore.init.multiblock;

import com.teameway.nowaycore.NoWayCore;
import com.teameway.nowaycore.common.multiblock.port.PortKind;
import com.teameway.nowaycore.common.multiblock.port.PortRegistry;
import com.teameway.nowaycore.common.multiblock.port.PortTier;
import com.teameway.nowaycore.common.multiblock.port.block.ControlPortBlock;
import com.teameway.nowaycore.common.multiblock.port.block.EnergyPortBlock;
import com.teameway.nowaycore.common.multiblock.port.block.FluidPortBlock;
import com.teameway.nowaycore.common.multiblock.port.block.ItemPortBlock;
import com.teameway.nowaycore.common.multiblock.port.block.entity.ControlPortBlockEntity;
import com.teameway.nowaycore.common.multiblock.port.block.entity.EnergyPortBlockEntity;
import com.teameway.nowaycore.common.multiblock.port.block.entity.FluidPortBlockEntity;
import com.teameway.nowaycore.common.multiblock.port.block.entity.ItemPortBlockEntity;
import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.teameway.nowaycore.NoWayCore.REGISTRUM;

/**
 * 多方块端口方块与方块实体注册
 *
 * <p>每种端口一个方块、一个等级，容量由方块实体按等级创建。</p>
 */
public class ModMultiblockPorts {
    public static final BlockEntry<ItemPortBlock> ITEM_INPUT =
        itemPort("item_input_port", PortKind.ITEM_INPUT, PortTier.items("basic", 9));
    public static final BlockEntry<ItemPortBlock> ITEM_OUTPUT =
        itemPort("item_output_port", PortKind.ITEM_OUTPUT, PortTier.items("basic", 9));
    public static final BlockEntry<FluidPortBlock> FLUID_INPUT =
        fluidPort("fluid_input_port", PortKind.FLUID_INPUT, PortTier.fluids("basic", 8000));
    public static final BlockEntry<FluidPortBlock> FLUID_OUTPUT =
        fluidPort("fluid_output_port", PortKind.FLUID_OUTPUT, PortTier.fluids("basic", 8000));
    public static final BlockEntry<EnergyPortBlock> ENERGY_INPUT =
        energyPort("energy_input_port", PortKind.ENERGY_INPUT, PortTier.energy("basic", 100_000));
    public static final BlockEntry<EnergyPortBlock> ENERGY_OUTPUT =
        energyPort("energy_output_port", PortKind.ENERGY_OUTPUT, PortTier.energy("basic", 100_000));
    public static final BlockEntry<ControlPortBlock> PARALLEL =
        controlPort("parallel_port", PortKind.PARALLEL, PortTier.parallel("basic", 2));
    public static final BlockEntry<ControlPortBlock> THREAD =
        controlPort("thread_port", PortKind.THREAD, PortTier.thread("basic", 1));
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NoWayCore.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemPortBlockEntity>> ITEM_PORT_TYPE =
        BLOCK_ENTITIES.register(
            "item_port",
            () -> new BlockEntityType<>(ItemPortBlockEntity::new, ITEM_INPUT.get(), ITEM_OUTPUT.get())
        );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidPortBlockEntity>> FLUID_PORT_TYPE =
        BLOCK_ENTITIES.register(
            "fluid_port",
            () -> new BlockEntityType<>(FluidPortBlockEntity::new, FLUID_INPUT.get(), FLUID_OUTPUT.get())
        );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyPortBlockEntity>> ENERGY_PORT_TYPE =
        BLOCK_ENTITIES.register(
            "energy_port",
            () -> new BlockEntityType<>(EnergyPortBlockEntity::new, ENERGY_INPUT.get(), ENERGY_OUTPUT.get())
        );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ControlPortBlockEntity>> CONTROL_PORT_TYPE =
        BLOCK_ENTITIES.register(
            "control_port",
            () -> new BlockEntityType<>(ControlPortBlockEntity::new, PARALLEL.get(), THREAD.get())
        );

    /**
     * 挂到模组事件总线
     */
    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }

    private static BlockEntry<ItemPortBlock> itemPort(String name, PortKind kind, PortTier tier) {
        return REGISTRUM
            .block(
                name, properties -> {
                    ItemPortBlock block = new ItemPortBlock(properties, kind, tier);
                    PortRegistry.register(block, kind, tier);
                    return block;
                }
            )
            .properties(properties -> properties.strength(3.0F))
            .simpleItem()
            .register();
    }

    private static BlockEntry<FluidPortBlock> fluidPort(String name, PortKind kind, PortTier tier) {
        return REGISTRUM
            .block(
                name, properties -> {
                    FluidPortBlock block = new FluidPortBlock(properties, kind, tier);
                    PortRegistry.register(block, kind, tier);
                    return block;
                }
            )
            .properties(properties -> properties.strength(3.0F))
            .simpleItem()
            .register();
    }

    private static BlockEntry<EnergyPortBlock> energyPort(String name, PortKind kind, PortTier tier) {
        return REGISTRUM
            .block(
                name, properties -> {
                    EnergyPortBlock block = new EnergyPortBlock(properties, kind, tier);
                    PortRegistry.register(block, kind, tier);
                    return block;
                }
            )
            .properties(properties -> properties.strength(3.0F))
            .simpleItem()
            .register();
    }

    private static BlockEntry<ControlPortBlock> controlPort(String name, PortKind kind, PortTier tier) {
        return REGISTRUM
            .block(
                name, properties -> {
                    ControlPortBlock block = new ControlPortBlock(properties, kind, tier);
                    PortRegistry.register(block, kind, tier);
                    return block;
                }
            )
            .properties(properties -> properties.strength(3.0F))
            .simpleItem()
            .register();
    }
}
