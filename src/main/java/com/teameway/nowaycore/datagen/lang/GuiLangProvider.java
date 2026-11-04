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
        provider.add("gui.noway_core.controller.info.parallel", "Up to %s runs per recipe");
        provider.add("gui.noway_core.controller.info.threads", "Up to %s recipes at once");
        provider.add("gui.noway_core.controller.thread.title", "Thread #%s");
        provider.add("gui.noway_core.controller.thread.recipe", "Recipe: %s");
        provider.add("gui.noway_core.controller.thread.parallel", "Parallel: %s");
        provider.add("gui.noway_core.controller.thread.progress", "Progress: %s%%");
        provider.add("gui.noway_core.controller.thread.ticks", "%s/%s t");
        provider.add("gui.noway_core.controller.thread.seconds", "%s/%s s");
        provider.add("gui.noway_core.controller.thread.none", "No thread");
        provider.add("gui.noway_core.controller.recipe.hint", "Click a thread to view its recipe");
        provider.add("gui.noway_core.controller.recipe.unavailable", "Recipe data not ready");
        provider.add("gui.noway_core.controller.recipe.hint_rate", "Inputs and outputs are listed per cycle");
        provider.add("gui.noway_core.controller.recipe.inputs", "Inputs");
        provider.add("gui.noway_core.controller.recipe.outputs", "Outputs");
        provider.add("gui.noway_core.controller.recipe.line_item", "%s × %s");
        provider.add("gui.noway_core.controller.recipe.line_fluid", "%s × %s");
        provider.add("gui.noway_core.controller.recipe.rate_item", "avg %s/t · %s/s");
        provider.add("gui.noway_core.controller.recipe.rate_fluid", "avg %s mB/t · %s mB/s");
        provider.add("gui.noway_core.port_kind.item_input", "Item Input Bus");
        provider.add("gui.noway_core.port_kind.item_output", "Item Output Bus");
        provider.add("gui.noway_core.port_kind.fluid_input", "Fluid Input Hatch");
        provider.add("gui.noway_core.port_kind.fluid_output", "Fluid Output Hatch");
        provider.add("gui.noway_core.port_kind.energy_input", "Energy Input");
        provider.add("gui.noway_core.port_kind.energy_output", "Energy Output");
        provider.add("gui.noway_core.port_kind.parallel", "Parallel Hatch");
        provider.add("gui.noway_core.port_kind.thread", "Thread Hatch");
        provider.add("gui.noway_core.port_panel.title", "Ports (%s)");
        provider.add("gui.noway_core.port_panel.tier", "Tier: %s");
        provider.add("gui.noway_core.port_panel.tooltip", "Shift + Left click to open the port");
        provider.add("gui.noway_core.port_panel.toggle", "Toggle port list");
        provider.add("gui.noway_core.port_panel.unknown", "Unknown port");
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