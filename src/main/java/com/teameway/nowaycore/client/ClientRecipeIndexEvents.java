package com.teameway.nowaycore.client;

import com.teameway.nowaycore.NoWayCore;
import com.teameway.nowaycore.recipe.MultiblockRecipeIndex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;

/**
 * 客户端配方索引维护
 */
@EventBusSubscriber(modid = NoWayCore.MOD_ID, value = Dist.CLIENT)
public class ClientRecipeIndexEvents {
    private ClientRecipeIndexEvents() {
    }

    /**
     * 收到服务端同步的配方后重建索引
     *
     * @param event 客户端配方接收事件
     */
    @SubscribeEvent
    public static void onRecipesReceived(RecipesReceivedEvent event) {
        MultiblockRecipeIndex.rebuild(event.getRecipeMap(), "client received");
    }
}