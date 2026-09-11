package com.teameway.nowaycore.init;

import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.world.item.CreativeModeTab;

import static com.teameway.nowaycore.NoWayCore.REGISTRATE;

public class ModCreativeTabs {
    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> MAIN_TAB = REGISTRATE
        .defaultCreativeTab(
            REGISTRATE,
            "main",
            builder -> builder
                .icon(ModItems.INFINITE_COLOR_APPLICATOR::asStack)
                .build()
        )
        .lang(_ -> "itemGroup.noway_core.main", "Noway Core")
        .register();

    public static void register() {
    }
}
