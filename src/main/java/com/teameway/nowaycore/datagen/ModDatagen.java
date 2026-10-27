package com.teameway.nowaycore.datagen;

import com.teameway.nowaycore.NoWayCore;
import com.teameway.nowaycore.datagen.lang.ModLangGenerator;
import com.teameway.nowaycore.datagen.model.ItemModelGenerator;
import com.teameway.nowaycore.datagen.provider.ModDataProvider;
import com.teameway.nowaycore.datagen.recipe.ModRecipeGenerator;
import com.tterrag.registrate.providers.ProviderType;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

import static com.teameway.nowaycore.NoWayCore.REGISTRATE;

@EventBusSubscriber(modid = NoWayCore.MOD_ID)
public class ModDatagen {
    public static void configureDataGen() {
        REGISTRATE.addDataGenerator(ProviderType.LANG, ModLangGenerator::accept);
        REGISTRATE.addDataGenerator(ProviderType.RECIPE, ModRecipeGenerator::accept);
        REGISTRATE.addDataGenerator(ProviderType.ITEM_MODEL, ItemModelGenerator::accept);
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getWorldLookupProvider();

        event.addProvider(new ModDataProvider(packOutput));
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Server event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getReloadableLookupProvider();
    }
}
