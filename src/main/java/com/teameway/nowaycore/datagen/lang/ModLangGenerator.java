package com.teameway.nowaycore.datagen.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public class ModLangGenerator {
    public static void accept(RegistrateLangProvider provider) {
        TooltipLangProvider.init(provider);
    }
}
