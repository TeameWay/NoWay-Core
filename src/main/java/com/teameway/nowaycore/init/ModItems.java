package com.teameway.nowaycore.init;

import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import com.teameway.nowaycore.common.item.InfiniteColorApplicatorItem;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;

import static com.teameway.nowaycore.NoWayCore.REGISTRATE;

public class ModItems {
    public static final ItemEntry<InfiniteColorApplicatorItem> INFINITE_COLOR_APPLICATOR = REGISTRATE.item(
            "infinite_color_applicator",
            InfiniteColorApplicatorItem::new
        )
        .model(NonNullBiConsumer::noop)
        .recipe((context, provider) -> ShapedRecipeBuilder.shaped(provider.itemLookup(), RecipeCategory.TOOLS, context.get())
            .pattern("ABC")
            .pattern("DEF")
            .pattern("GHI")
            .define('A', AEParts.TERMINAL)
            .define('B', AEItems.MATTER_BALL)
            .define('C', AEBlocks.DENSE_ENERGY_CELL)
            .define('D', AEItems.SPATIAL_128_CELL_COMPONENT)
            .define('E', AEItems.COLOR_APPLICATOR)
            .define('F', AEItems.CELL_COMPONENT_256K)
            .define('G', AEItems.ANNIHILATION_CORE)
            .define('H', AEItems.SINGULARITY)
            .define('I', AEItems.FORMATION_CORE)
            .unlockedBy("has_color_applicator", provider.has(AEItems.COLOR_APPLICATOR))
            .save(provider))
        .register();

    public static void register() {
    }
}
