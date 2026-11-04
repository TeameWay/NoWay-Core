package com.teameway.nowaycore.datagen.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

/**
 * 图形界面文本
 */
public class GuiLangProvider {

    public static void init(RegistrumLangProvider provider) {
        provider.add("gui.noway_core.controller.status", "Type: %s · Variants: %s");
        provider.add("gui.noway_core.controller.ports", "Threads: %s · Parallel: %s");
        provider.add("gui.noway_core.controller.thread", "T%s");
        provider.add("gui.noway_core.controller.idle", "Idle");
        provider.add("gui.noway_core.controller.unformed", "Structure not formed");
        provider.add("gui.noway_core.controller.working", "%sx %s");
        provider.add("gui.noway_core.port.slots", "Slots: %s");
        provider.add("gui.noway_core.port.fluid", "Capacity: %s mB");
        provider.add("gui.noway_core.port.energy", "%s / %s FE");
        provider.add("gui.noway_core.port.threads", "Threads: +%s");
        provider.add("gui.noway_core.port.parallels", "Parallel: +%s");
        provider.add("gui.noway_core.jei.time", "Time: %ss");
        provider.add("gui.noway_core.jei.energy", "Energy: %s FE");
        provider.add("gui.noway_core.jei.no_structure", "Structure preview unavailable");
        provider.add("gui.noway_core.jei.multiblock_info", "Multiblock Structure");
        provider.add("gui.noway_core.jei.chance", "Chance: %s");
    }
}
