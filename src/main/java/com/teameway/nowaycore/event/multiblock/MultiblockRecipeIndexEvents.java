package com.teameway.nowaycore.event.multiblock;

import com.teameway.nowaycore.NoWayCore;
import com.teameway.nowaycore.init.ModRecipeTypes;
import com.teameway.nowaycore.recipe.MultiblockRecipeIndex;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * 服务端配方索引维护
 *
 * <p>服务端启动时建一次；每次数据包同步（玩家加入、{@code /reload}）时重建，并顺带请求把
 * 多方块配方类型同步给客户端，否则客户端拿不到这些配方。</p>
 */
@EventBusSubscriber(modid = NoWayCore.MOD_ID)
public class MultiblockRecipeIndexEvents {
    private MultiblockRecipeIndexEvents() {
    }

    /**
     * 服务端启动后构建索引
     *
     * @param event 服务端启动事件
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MultiblockRecipeIndex.rebuild(event.getServer().getRecipeManager().recipeMap(), "server started");
    }

    /**
     * 数据包同步时重建索引，并请求同步多方块配方类型
     *
     * @param event 数据包同步事件
     */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.sendRecipes(ModRecipeTypes.MULTIBLOCK.get());
        MultiblockRecipeIndex.rebuild(event.getPlayerList().getServer().getRecipeManager().recipeMap(), "datapack sync");
    }
}
