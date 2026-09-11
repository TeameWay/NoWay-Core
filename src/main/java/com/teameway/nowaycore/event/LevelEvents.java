package com.teameway.nowaycore.event;

import com.teameway.nowaycore.NoWayCore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = NoWayCore.MOD_ID)
public class LevelEvents {
    @SubscribeEvent
    public static void setGameRules(LevelEvent.Load event) {
        var level = event.getLevel();
        if (level instanceof ServerLevel serverLevel) {
            if (serverLevel.dimension() == Level.OVERWORLD) {
                MinecraftServer server = serverLevel.getServer();
                GameRules rules = server.getGameRules();
                rules.set(GameRules.SPAWN_MONSTERS, false, server);
                rules.set(GameRules.ADVANCE_TIME, false, server);
                rules.set(GameRules.ADVANCE_WEATHER, false, server);
                serverLevel.dimensionType()
                    .defaultClock()
                    .ifPresent((clock) -> serverLevel.clockManager().moveToTimeMarker(clock, ClockTimeMarkers.NOON));
                serverLevel.resetWeatherCycle();
            }
        }
    }
}
