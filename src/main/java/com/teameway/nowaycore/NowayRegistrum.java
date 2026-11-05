package com.teameway.nowaycore;

import com.teameway.nowaycore.common.multiblock.MultiblockMachine;
import com.teameway.nowaycore.common.multiblock.MultiblockType;
import com.teameway.nowaycore.init.ModRegistries;
import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.IController;
import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.builders.NoConfigBuilder;
import dev.anvilcraft.lib.v2.util.nullness.NonNullSupplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.function.BiFunction;

/**
 * 模组自己的 Registrum
 *
 * <p>在 Registrum 自带的方块与物品入口之外，提供多方块机器、类型与控制器的注册入口，
 * 条目存放在 {@link ModRegistries} 自建的注册表里。</p>
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
        Optional<net.neoforged.bus.api.IEventBus> modEventBus =
            ModList.get().getModContainerById(modid).map(ModContainer::getEventBus);
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

    /**
     * 注册一个多方块控制器
     *
     * <p>入口名同时作为结构定义 ID 的路径，也就决定定义 JSON 的文件名。</p>
     *
     * @param name    入口名
     * @param block   控制器方块
     * @param factory 控制器工厂，入参为控制器方块与定义 ID
     * @param <T>     控制器类型
     * @return 构建器，{@code register()} 得到 {@code RegistryEntry}
     */
    public <T extends IController> NoConfigBuilder<IController, T, NowayRegistrum> multiblockController(
        String name,
        NonNullSupplier<? extends Block> block,
        BiFunction<Block, Identifier, T> factory
    ) {
        Identifier definitionId = Identifier.fromNamespaceAndPath(this.getModid(), name);
        return this.generic(
            name,
            ModRegistries.MULTIBLOCK_CONTROLLERS,
            () -> factory.apply(block.get(), definitionId)
        );
    }

    /**
     * 注册一个多方块机器类型
     *
     * @param name    类型名，同时也是定义 tag 的路径
     * @param factory 类型工厂
     * @return 构建器
     */
    public NoConfigBuilder<MultiblockType, MultiblockType, NowayRegistrum> multiblockType(
        String name,
        NonNullSupplier<MultiblockType> factory
    ) {
        return this.generic(name, ModRegistries.MULTIBLOCK_TYPES, factory);
    }

    /**
     * 注册一台多方块机器声明
     *
     * @param name    入口名
     * @param factory 机器声明工厂
     * @return 构建器，{@code register()} 得到 {@code RegistryEntry}
     */
    public NoConfigBuilder<MultiblockMachine, MultiblockMachine, NowayRegistrum> multiblock(
        String name,
        NonNullSupplier<MultiblockMachine> factory
    ) {
        return this.generic(name, ModRegistries.MULTIBLOCKS, factory);
    }
}