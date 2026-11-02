package com.teameway.nowaycore.datagen.model;

import appeng.client.item.ColorApplicatorItemModel;
import com.teameway.nowaycore.init.ModItems;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumItemModelGenerator;
import net.minecraft.resources.Identifier;

public class ItemModelGenerator {
    public static void accept(RegistrumItemModelGenerator generator) {
        generator.itemModelOutput.accept(
            ModItems.INFINITE_COLOR_APPLICATOR.get(),
            new ColorApplicatorItemModel.Unbaked(
                Identifier.parse("ae2:item/color_applicator"),
                Identifier.parse("ae2:item/color_applicator_colored")
            )
        );
    }
}
