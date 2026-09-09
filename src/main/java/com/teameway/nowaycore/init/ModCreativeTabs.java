package com.teameway.nowaycore.init;

import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.world.item.CreativeModeTab;

import static com.teameway.nowaycore.NoWayCore.REGISTRATE;

public class ModCreativeTabs {
    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> MAIN_TAB = REGISTRATE
        .defaultCreativeTab(
            REGISTRATE,
            "main_tab",
            builder -> builder
                .build()
        )
        .register();

    public static void register() {
    }
}
