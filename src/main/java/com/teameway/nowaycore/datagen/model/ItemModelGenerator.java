package com.teameway.nowaycore.datagen.model;

import appeng.client.item.ColorApplicatorItemModel;
import com.teameway.nowaycore.init.ModItems;
import com.tterrag.registrate.providers.generators.RegistrateItemModelGenerator;
import net.minecraft.resources.Identifier;

public class ItemModelGenerator {
    public static void accept(RegistrateItemModelGenerator generator) {
        generator.itemModelOutput.accept(
            ModItems.INFINITE_COLOR_APPLICATOR.get(),
            new ColorApplicatorItemModel.Unbaked(
                Identifier.parse("ae2:item/color_applicator"),
                Identifier.parse("ae2:item/color_applicator_colored")
            )
        );
    }
}
