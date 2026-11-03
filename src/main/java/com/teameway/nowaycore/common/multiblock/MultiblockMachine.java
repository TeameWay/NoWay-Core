package com.teameway.nowaycore.common.multiblock;

import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.IController;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 一台多方块机器：定义 ID、控制器入口、结构形状
 *
 * @param id         结构定义 ID，同时也是 datagen 输出的文件名
 * @param controller 控制器入口（{@code noway_core:multiblock_controllers} 注册表）
 * @param definition 结构形状（延迟构建，只在 datagen 与需要时求值）
 */
public record MultiblockMachine(
    Identifier id,
    RegistryEntry<IController, ? extends IController> controller,
    Supplier<MultiblockDefinition> definition
) {
    /**
     * 开始声明一台机器
     *
     * @param controller 控制器入口，其入口名即定义 ID 的路径
     * @return 链式构建器
     */
    public static Builder builder(RegistryEntry<IController, ? extends IController> controller) {
        return new Builder(controller);
    }

    /**
     * 机器声明的链式构建器
     */
    public static final class Builder {
        private final RegistryEntry<IController, ? extends IController> controller;
        private @Nullable Supplier<MultiblockDefinition> definition;

        private Builder(RegistryEntry<IController, ? extends IController> controller) {
            this.controller = controller;
        }

        /**
         * 结构形状，用 {@code MultiblockDefinition.builder()} 链式声明
         *
         * @param definition 形状（延迟构建）
         * @return this
         */
        public Builder definition(Supplier<MultiblockDefinition> definition) {
            this.definition = definition;
            return this;
        }

        /**
         * 完成声明
         *
         * @return 机器声明
         */
        public MultiblockMachine build() {
            Identifier id = this.controller.getId();
            return new MultiblockMachine(
                id,
                this.controller,
                Objects.requireNonNull(this.definition, () -> "Multiblock " + id + " is missing a definition")
            );
        }
    }
}
