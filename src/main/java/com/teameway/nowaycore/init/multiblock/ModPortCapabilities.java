package com.teameway.nowaycore.init.multiblock;

import com.teameway.nowaycore.NoWayCore;
import com.teameway.nowaycore.common.multiblock.port.block.entity.EnergyPortBlockEntity;
import com.teameway.nowaycore.common.multiblock.port.block.entity.FluidPortBlockEntity;
import com.teameway.nowaycore.common.multiblock.port.block.entity.ItemPortBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * 端口能力注册
 *
 * <p>端口方块实体对外暴露的处理器与控制器内部用的是同一个实例，因此管道与机器共享库存，
 * 控制器不需要再做一层中转。</p>
 */
@EventBusSubscriber(modid = NoWayCore.MOD_ID)
public class ModPortCapabilities {

    /**
     * 注册物品/流体/能量能力
     *
     * @param event 能力注册事件
     */
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(
            Capabilities.Item.BLOCK,
            (_, _, _, blockEntity, _) ->
                blockEntity instanceof ItemPortBlockEntity port ? port.itemHandler() : null,
            ModMultiblockPorts.ITEM_INPUT.get(),
            ModMultiblockPorts.ITEM_OUTPUT.get()
        );
        event.registerBlock(
            Capabilities.Fluid.BLOCK,
            (_, _, _, blockEntity, _) ->
                blockEntity instanceof FluidPortBlockEntity port ? port.fluidHandler() : null,
            ModMultiblockPorts.FLUID_INPUT.get(),
            ModMultiblockPorts.FLUID_OUTPUT.get()
        );
        event.registerBlock(
            Capabilities.Energy.BLOCK,
            (_, _, _, blockEntity, _) ->
                blockEntity instanceof EnergyPortBlockEntity port ? port.energyHandler() : null,
            ModMultiblockPorts.ENERGY_INPUT.get(),
            ModMultiblockPorts.ENERGY_OUTPUT.get()
        );
        NoWayCore.LOGGER.debug("Registered multiblock port capabilities");
    }
}