package com.teameway.nowaycore.event.multiblock;

import com.teameway.nowaycore.NoWayCore;
import com.teameway.nowaycore.init.ModRegistries;
import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.ControllerRecord;
import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.SimpleController;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import static com.teameway.nowaycore.NoWayCore.REGISTRUM;

@EventBusSubscriber(modid = NoWayCore.MOD_ID)
public class MultiblockEvent {
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
