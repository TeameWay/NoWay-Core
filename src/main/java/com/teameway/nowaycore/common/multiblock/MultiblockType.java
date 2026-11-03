package com.teameway.nowaycore.common.multiblock;

import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.IController;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;

/**
 * 多方块机器类型
 *
 * <p>类型在 Java 注册表里声明，决定“哪种控制器、哪套配方”；具体形态通过定义 tag 关联，
 * 加形态只需要加定义 JSON 与 tag 条目。</p>
 *
 * @param controller 控制器入口
 */
public record MultiblockType(RegistryEntry<IController, ? extends IController> controller) {
}
