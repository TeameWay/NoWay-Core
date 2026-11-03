package com.teameway.nowaycore.event;

import com.teameway.nowaycore.NoWayCore;
import com.teameway.nowaycore.common.command.export.MultiblockExportCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = NoWayCore.MOD_ID)
public class ModCommandEvent {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        MultiblockExportCommand.register(event.getDispatcher());
    }
}
