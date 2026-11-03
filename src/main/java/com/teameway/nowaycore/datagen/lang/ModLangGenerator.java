package com.teameway.nowaycore.datagen.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class ModLangGenerator {
    public static void accept(RegistrumLangProvider provider) {
        TooltipLangProvider.init(provider);
        GuiLangProvider.init(provider);
    }
}
