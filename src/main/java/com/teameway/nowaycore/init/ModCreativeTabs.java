package com.teameway.nowaycore.init;

import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import net.minecraft.world.item.CreativeModeTab;

import static com.teameway.nowaycore.NoWayCore.REGISTRUM;

public class ModCreativeTabs {
    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> MAIN = REGISTRUM
        .creativeTab("main", ModItems.INFINITE_COLOR_APPLICATOR)
        .lang(_ -> "itemGroup.noway_core.main", "Noway Core - Main")
        .register();

    public static void register() {
    }
}
