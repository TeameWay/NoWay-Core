package com.teameway.nowaycore;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

/**
 * 模组自己的 Registrum
 *
 * <p>多方块相关入口已拆分到 BnvilMultiblock，这里只保留主模组自身内容的注册能力。</p>
 */
public class NowayRegistrum extends AbstractRegistrum<NowayRegistrum> {
    private static final Logger LOGGER = LogManager.getLogger(NowayRegistrum.class);

    private NowayRegistrum(String modid) {
        super(modid);
    }

    /**
     * 创建注册入口，并挂上模组事件总线
     *
     * @param modid 模组 ID
     * @return Registrum 实例
     */
    public static NowayRegistrum create(String modid) {
        NowayRegistrum registrum = new NowayRegistrum(modid);
        Optional<IEventBus> modEventBus = ModList.get().getModContainerById(modid).map(ModContainer::getEventBus);
        modEventBus.ifPresentOrElse(
            registrum::registerEventListeners, () -> {
                String message = "# [NowayRegistrum] Failed to register eventListeners for mod " + modid
                                 + ", This should be reported to this mod's dev #";
                StringBuilder hashtags = new StringBuilder().repeat("#", message.length());
                LOGGER.fatal(hashtags.toString());
                LOGGER.fatal(message);
                LOGGER.fatal(hashtags.toString());
            }
        );
        return registrum;
    }
}
