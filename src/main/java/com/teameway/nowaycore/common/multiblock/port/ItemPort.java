package com.teameway.nowaycore.common.multiblock.port;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * 物品端口
 *
 * <p>方块实体应把同一个 {@link ResourceHandler} 注册到 {@code Capabilities.Item.BLOCK}，
 * 这样管道与控制器拿到的是同一份库存。</p>
 */
public interface ItemPort extends MultiblockPort {
    /**
     * @return 物品处理器
     */
    ResourceHandler<ItemResource> itemHandler();
}
