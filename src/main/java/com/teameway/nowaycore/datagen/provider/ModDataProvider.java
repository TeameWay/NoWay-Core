package com.teameway.nowaycore.datagen.provider;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;

public class ModDataProvider implements DataProvider {
    private static final String OVERWORLD_DIMENSION_TYPE_JSON = """
        {
          "ambient_light": 0.0,
          "attributes": {
            "minecraft:audio/ambient_sounds": {
              "mood": {
                "block_search_extent": 8,
                "offset": 2.0,
                "sound": "minecraft:ambient.cave",
                "tick_delay": 6000
              }
            },
            "minecraft:audio/background_music": {
              "creative": {
                "max_delay": 24000,
                "min_delay": 12000,
                "sound": "minecraft:music.creative"
              },
              "default": {
                "max_delay": 24000,
                "min_delay": 12000,
                "sound": "minecraft:music.game"
              }
            },
            "minecraft:gameplay/bed_rule": {
              "can_set_spawn": "always",
              "can_sleep": "when_dark",
              "error_message": {
                "translate": "block.minecraft.bed.no_sleep"
              }
            },
            "minecraft:gameplay/nether_portal_spawns_piglin": true,
            "minecraft:gameplay/respawn_anchor_works": false,
            "minecraft:visual/ambient_light_color": "#0a0a0a",
            "minecraft:visual/cloud_color": "#ccffffff",
            "minecraft:visual/cloud_height": 192.33,
            "minecraft:visual/fog_color": "#c0d8ff",
            "minecraft:visual/sky_color": "#78a7ff"
          },
          "coordinate_scale": 1.0,
          "default_clock": "minecraft:overworld",
          "has_ceiling": false,
          "has_ender_dragon_fight": false,
          "has_skylight": true,
          "height": 512,
          "infiniburn": "#minecraft:infiniburn_overworld",
          "logical_height": 512,
          "min_y": 0,
          "monster_spawn_block_light_limit": 0,
          "monster_spawn_light_level": {
            "type": "minecraft:uniform",
            "max_inclusive": 7,
            "min_inclusive": 0
          },
          "timelines": "#minecraft:in_overworld"
        }
        """;
    private static final String NORMAL_WORLD_PRESET_JSON = """
        {
          "dimensions": {
            "minecraft:overworld": {
              "type": "minecraft:overworld",
              "generator": {
                "type": "noway_core:neo_overworld"
              }
            },
            "minecraft:the_nether": {
              "type": "minecraft:the_nether",
              "generator": {
                "type": "minecraft:noise",
                "biome_source": {
                  "type": "minecraft:multi_noise",
                  "preset": "minecraft:nether"
                },
                "settings": "minecraft:nether"
              }
            },
            "minecraft:the_end": {
              "type": "minecraft:the_end",
              "generator": {
                "type": "minecraft:noise",
                "biome_source": {
                  "type": "minecraft:the_end"
                },
                "settings": "minecraft:end"
              }
            }
          }
        }
        """;
    private final PackOutput output;

    public ModDataProvider(PackOutput output) {
        this.output = output;
    }

    private static CompletableFuture<?> save(
        CachedOutput cache,
        PackOutput.PathProvider pathProvider,
        Identifier id,
        String json
    ) {
        JsonElement element = JsonParser.parseString(json);
        return DataProvider.saveStable(cache, element, pathProvider.json(id));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        PackOutput.PathProvider dimensionTypes = output.createRegistryElementsPathProvider(Registries.DIMENSION_TYPE);
        PackOutput.PathProvider worldPresets = output.createRegistryElementsPathProvider(Registries.WORLD_PRESET);

        return CompletableFuture.allOf(
            save(cache, dimensionTypes, Identifier.withDefaultNamespace("overworld"), OVERWORLD_DIMENSION_TYPE_JSON),
            save(cache, worldPresets, Identifier.withDefaultNamespace("normal"), NORMAL_WORLD_PRESET_JSON)
        );
    }

    @Override
    public String getName() {
        return "NoWayCore Minecraft Data Overrides";
    }
}
