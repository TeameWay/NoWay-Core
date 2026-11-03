package com.teameway.nowaycore.init.multiblock;

import com.teameway.nowaycore.NoWayCore;
import com.teameway.nowaycore.common.multiblock.MultiblockMachine;
import com.teameway.nowaycore.init.ModRegistries;
import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.ControllerRecord;
import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.SimpleController;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import static com.teameway.nowaycore.NoWayCore.REGISTRUM;

/**
 * 多方块机器与控制器的注册
 *
 * <p>两者都是 {@link RegistryEntry}：控制器进入 {@code noway_core:multiblock_controllers}，
 * 机器声明进入 {@code noway_core:multiblocks}，datagen 直接遍历后者输出定义 JSON。</p>
 */
@EventBusSubscriber(modid = NoWayCore.MOD_ID)
public final class ModMultiblockMachines {

    public static final RegistryEntry<MultiblockMachine, MultiblockMachine> SIMPLE_PROCESSOR = REGISTRUM.multiblock(
        "simple_processor",
        () -> MultiblockMachine.builder(ModControllers.SIMPLE_PROCESSOR_CONTROLLER)
            .definition(() -> ModMultiblockDefinitions.SIMPLE_PROCESSOR)
            .build()
    ).register();

    /**
     * 把注册表里的控制器交给 anvillib 的 {@link ControllerRecord}
     *
     * <p>控制器在注册表注册阶段就已构造完成，这里只需登记；控制器的查询会同时发生在服务端与客户端，
     * 因此必须双端执行。</p>
     *
     * @param event 公共初始化事件
     */
    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> REGISTRUM.getAll(ModRegistries.MULTIBLOCK_CONTROLLERS).forEach(entry -> {
            if (!(entry.get() instanceof SimpleController controller)) {
                throw new IllegalStateException("Controller " + entry.getId() + " must extend SimpleController");
            }
            ControllerRecord.register(controller);
        }));
    }
}
