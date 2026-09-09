package com.teameway.nowaycore.levelgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class NeoOverWorldChunkGenerator extends ChunkGenerator {
    public static final int MIN_BUILD_HEIGHT = 0;
    public static final int MAX_BUILD_HEIGHT = 512;
    public static final int PLATFORM_Y = 64;
    public static final int SEA_LEVEL = 64;
    public static final MapCodec<NeoOverWorldChunkGenerator> CODEC;
    private static final BlockState AIR;
    private static final BlockState BORDER;
    private static final BlockState FILL;

    static {
        AIR = Blocks.AIR.defaultBlockState();
        BORDER = Blocks.GRAY_CONCRETE.defaultBlockState();
        FILL = Blocks.WHITE_CONCRETE.defaultBlockState();
        CODEC = RecordCodecBuilder.mapCodec((inst) -> inst.group(RegistryOps.retrieveElement(Biomes.PLAINS))
            .apply(inst, inst.stable(NeoOverWorldChunkGenerator::new)));
    }

    private NeoOverWorldChunkGenerator(Holder<Biome> biome) {
        super(new FixedBiomeSource(biome));
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return NeoOverWorldChunkGenerator.CODEC;
    }

    @Override
    public ChunkGeneratorStructureState createState(
        HolderLookup<StructureSet> structureSets,
        RandomState randomState,
        long legacyLevelSeed
    ) {
        return ChunkGeneratorStructureState.createForFlat(randomState, legacyLevelSeed, this.biomeSource, Stream.of());
    }

    @Override
    public void applyCarvers(
        WorldGenRegion worldGenRegion,
        long l,
        RandomState randomState,
        BiomeManager biomeManager,
        StructureManager structureManager,
        ChunkAccess chunkAccess
    ) {
    }

    @Override
    public void buildSurface(
        WorldGenRegion worldGenRegion,
        StructureManager structureManager,
        RandomState randomState,
        ChunkAccess chunkAccess
    ) {
        this.fillPlatform(chunkAccess);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion worldGenRegion) {
    }

    @Override
    public int getGenDepth() {
        return MAX_BUILD_HEIGHT;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
        Blender blender,
        RandomState randomState,
        StructureManager structureManager,
        ChunkAccess chunkAccess
    ) {
        return CompletableFuture.completedFuture(chunkAccess);
    }

    @Override
    public int getSeaLevel() {
        return SEA_LEVEL;
    }

    @Override
    public int getMinY() {
        return MIN_BUILD_HEIGHT;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types types, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        return PLATFORM_Y + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        BlockState[] column = new BlockState[level.getHeight()];
        int minBuild = level.getMinY();
        int maxBuild = level.getMaxY();
        boolean edge = x % 16 == 0 || z % 16 == 0 || x % 16 == 15 || z % 16 == 15;
        BlockState platformState = edge ? BORDER : FILL;

        for (int y = minBuild; y < maxBuild; ++y) {
            column[y - minBuild] = y == 64 ? platformState : AIR;
        }

        return new NoiseColumn(minBuild, column);
    }

    @Override
    public void addDebugScreenInfo(List<String> list, RandomState randomState, BlockPos blockPos) {
    }

    private void fillPlatform(ChunkAccess chunk) {
        int minX = chunk.getPos().getMinBlockX();
        int maxX = chunk.getPos().getMaxBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int maxZ = chunk.getPos().getMaxBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; ++x) {
            boolean edgeX = x == minX || x == maxX;

            for (int z = minZ; z <= maxZ; ++z) {
                boolean edgeZ = z == minZ || z == maxZ;
                pos.set(x, PLATFORM_Y, z);
                chunk.setBlockState(pos, !edgeX && !edgeZ ? FILL : BORDER);
            }
        }

    }
}
