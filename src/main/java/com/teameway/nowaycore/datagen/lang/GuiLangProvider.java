package com.teameway.nowaycore.datagen.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

/**
 * 图形界面文本
 */
public class GuiLangProvider {

    public static void init(RegistrumLangProvider provider) {
        provider.add("gui.noway_core.controller.state.unformed", "Structure not formed");
        provider.add("gui.noway_core.controller.state.idle", "Idle");
        provider.add("gui.noway_core.controller.state.working", "Working");
        provider.add("gui.noway_core.controller.info.variants", "Variants: %s");
        provider.add("gui.noway_core.controller.info.parallel", "Parallel: %s");
        provider.add("gui.noway_core.controller.info.threads", "Threads: %s");
        provider.add("gui.noway_core.controller.thread.title", "Thread #%s");
        provider.add("gui.noway_core.controller.thread.recipe", "Recipe: %s");
        provider.add("gui.noway_core.controller.thread.meta", "Parallel %s · Run %s%% (%s/%s t)");
        provider.add("gui.noway_core.controller.thread.none", "No thread");
        provider.add("gui.noway_core.controller.recipe.hint", "Click a thread to view its recipe");
        provider.add("gui.noway_core.controller.recipe.unavailable", "Recipe data not ready");
        provider.add("gui.noway_core.controller.recipe.inputs", "Inputs");
        provider.add("gui.noway_core.controller.recipe.outputs", "Outputs");
        provider.add("gui.noway_core.controller.recipe.energy", "Energy: %s FE");
        provider.add("gui.noway_core.controller.recipe.power", "Power: %s FE/t");
        provider.add("gui.noway_core.controller.recipe.time", "Time: %ss");
        provider.add("gui.noway_core.controller.recipe.parallel", "Parallel: %s");
        provider.add("gui.noway_core.port.slots", "Slots: %s");
        provider.add("gui.noway_core.port.fluid", "Capacity: %s mB");
        provider.add("gui.noway_core.port.energy", "%s / %s FE");
        provider.add("gui.noway_core.port.threads", "Threads: +%s");
        provider.add("gui.noway_core.port.parallels", "Parallel: +%s");
        provider.add("gui.noway_core.jei.time", "Time: %ss");
        provider.add("gui.noway_core.jei.energy_total", "Total: %s FE");
        provider.add("gui.noway_core.jei.energy_rate", "Power: %s FE/t");
        provider.add("gui.noway_core.jei.no_structure", "Structure preview unavailable");
        provider.add("gui.noway_core.jei.multiblock_info", "Multiblock Structure");
        provider.add("gui.noway_core.jei.chance", "Chance: %s");
        provider.add("gui.noway_core.slot.count", "Count: %s");
    }
}