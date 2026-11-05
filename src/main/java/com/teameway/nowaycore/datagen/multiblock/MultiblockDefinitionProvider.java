package com.teameway.nowaycore.datagen.multiblock;

import com.teameway.nowaycore.common.multiblock.MultiblockMachine;
import com.teameway.nowaycore.init.ModRegistries;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.multiblock.init.LibRegistries;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.teameway.nowaycore.NoWayCore.REGISTRUM;

/**
 * 多方块定义数据生成器
 *
 * <p>遍历 {@code noway_core:multiblocks} 注册表，输出路径由注册表 {@code anvillib:definitions} 推导，
 * 即 data/&lt;namespace&gt;/anvillib/definitions/&lt;path&gt;.json。</p>
 */
public class MultiblockDefinitionProvider implements DataProvider {
    private static final Logger LOGGER = LogManager.getLogger(MultiblockDefinitionProvider.class);

    private final PackOutput output;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public MultiblockDefinitionProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this.output = output;
        this.registries = registries;
    }

    private static CompletableFuture<?> save(
        CachedOutput cache,
        HolderLookup.Provider registries,
        PackOutput.PathProvider pathProvider,
        MultiblockMachine machine
    ) {
        Identifier id = machine.id();
        return DataProvider.saveStable(
            cache, registries, MultiblockDefinition.CODEC.codec(), machine.definition().get(), pathProvider.json(id)
        );
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        PackOutput.PathProvider pathProvider = this.output.createRegistryElementsPathProvider(LibRegistries.DEFINITIONS_KEY);
        return this.registries.thenCompose(lookup -> {
            List<MultiblockMachine> machines = REGISTRUM.getAll(ModRegistries.MULTIBLOCKS)
                .stream()
                .map(RegistryEntry::get)
                .toList();
            LOGGER.info("Generating {} multiblock definition(s)", machines.size());
            CompletableFuture<?>[] futures = machines.stream()
                .map(machine -> save(cache, lookup, pathProvider, machine))
                .toArray(CompletableFuture[]::new);
            return CompletableFuture.allOf(futures);
        });
    }

    @Override
    public String getName() {
        return "NoWayCore Multiblock Definitions";
    }
}
