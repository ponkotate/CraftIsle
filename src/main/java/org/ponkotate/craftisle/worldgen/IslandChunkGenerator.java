package org.ponkotate.craftisle.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.ponkotate.craftisle.CraftIsle;
import org.ponkotate.craftisle.block.PebbleBlock;
import org.ponkotate.craftisle.registry.ModBlocks;

public class IslandChunkGenerator extends ChunkGenerator {

    private static final int SEA_LEVEL            = 63;
    private static final int OCEAN_FLOOR_Y        = 0;
    private static final int MIN_Y                = -64;
    private static final int GEN_DEPTH            = 384;
    private static final int ISLAND_MAX_ELEVATION = 50;
    private static final int SOIL_DEPTH           = 10;

    public static final MapCodec<IslandChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
        ).apply(instance, IslandChunkGenerator::new)
    );

    // Double-checked locking for thread-safe seed initialisation
    private volatile RandomState initializedState = null;
    private volatile long islandGridSeed;
    private volatile long noiseSeed1;
    private volatile long noiseSeed2;
    private volatile long noiseSeed3;
    private volatile long pebbleSeed;

    public IslandChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    // ─────────────────────────────────────────────────────────────
    // Seed initialisation
    // ─────────────────────────────────────────────────────────────

    private void initSeedIfNeeded(RandomState randomState) {
        if (initializedState != randomState) {
            synchronized (this) {
                if (initializedState != randomState) {
                    PositionalRandomFactory factory = randomState.getOrCreateRandomFactory(
                        Identifier.fromNamespaceAndPath(CraftIsle.MOD_ID, "island")
                    );
                    islandGridSeed = factory.fromHashOf("grid").nextLong();
                    noiseSeed1     = factory.fromHashOf("noise1").nextLong();
                    noiseSeed2     = factory.fromHashOf("noise2").nextLong();
                    noiseSeed3     = factory.fromHashOf("noise3").nextLong();
                    pebbleSeed     = factory.fromHashOf("pebble").nextLong();
                    if (getBiomeSource() instanceof IslandBiomeSource islandBiomeSource) {
                        islandBiomeSource.setGridSeed(islandGridSeed);
                    }
                    initializedState = randomState;
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Value noise — bilinear interpolation with smoothstep
    // ─────────────────────────────────────────────────────────────

    private static double hash2D(long seed, int x, int z) {
        long h = seed;
        h ^= (long) x * 0x9e3779b97f4a7c15L;
        h ^= (long) z * 0x6c62272e07bb0142L;
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return (double) (h & 0xFFFFFFFFL) / (double) 0xFFFFFFFFL; // [0, 1]
    }

    private static double valueNoise(long seed, double x, double z, int scale) {
        double sx = x / scale;
        double sz = z / scale;
        int xi = (int) Math.floor(sx);
        int zi = (int) Math.floor(sz);
        double fx = sx - xi;
        double fz = sz - zi;
        fx = fx * fx * (3.0 - 2.0 * fx); // smoothstep
        fz = fz * fz * (3.0 - 2.0 * fz);
        double v00 = hash2D(seed, xi,     zi)     * 2.0 - 1.0;
        double v10 = hash2D(seed, xi + 1, zi)     * 2.0 - 1.0;
        double v01 = hash2D(seed, xi,     zi + 1) * 2.0 - 1.0;
        double v11 = hash2D(seed, xi + 1, zi + 1) * 2.0 - 1.0;
        return v00 * (1 - fx) * (1 - fz)
             + v10 * fx       * (1 - fz)
             + v01 * (1 - fx) * fz
             + v11 * fx       * fz;
    }

    // ─────────────────────────────────────────────────────────────
    // Terrain height
    // ─────────────────────────────────────────────────────────────

    private int computeSurfaceY(int wx, int wz) {
        long[] nearest = IslandGrid.nearestIslandCenter(islandGridSeed, wx, wz);
        double dist = Math.sqrt(nearest[2]);
        double t = Math.max(0.0, (IslandGrid.ISLAND_RADIUS - dist) / IslandGrid.ISLAND_RADIUS);
        double islandFactor = t * t * (3.0 - 2.0 * t); // smoothstep [0,1]
        if (islandFactor <= 0.0) {
            return OCEAN_FLOOR_Y;
        }
        // Three octaves: broad peaks (55), ridges (22), surface roughness (9)
        double noise = valueNoise(noiseSeed1, wx, wz, 55) * 0.60
                     + valueNoise(noiseSeed2, wx, wz, 22) * 0.28
                     + valueNoise(noiseSeed3, wx, wz,  9) * 0.12; // ≈ [-1, 1]
        double peakFactor = (noise + 1.0) * 0.5; // [0, 1]
        // Smooth transition from ocean floor to island peaks: no vertical cliff at island border.
        // islandFactor shapes the island; peakFactor creates multiple peaks within it.
        int y = (int)(OCEAN_FLOOR_Y + islandFactor * (SEA_LEVEL - OCEAN_FLOOR_Y + ISLAND_MAX_ELEVATION * (0.10 + peakFactor * 0.90)));
        return Math.max(OCEAN_FLOOR_Y, y);
    }

    // ─────────────────────────────────────────────────────────────
    // ChunkGenerator implementation
    // ─────────────────────────────────────────────────────────────

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
            Blender blender,
            RandomState randomState,
            StructureManager structureManager,
            ChunkAccess chunk) {
        initSeedIfNeeded(randomState);

        int minBlockX = chunk.getPos().getMinBlockX();
        int minBlockZ = chunk.getPos().getMinBlockZ();
        int minY      = chunk.getMinY();
        int height    = chunk.getHeight();

        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        BlockState stone   = Blocks.STONE.defaultBlockState();
        BlockState water   = Blocks.WATER.defaultBlockState();
        BlockState air     = Blocks.AIR.defaultBlockState();

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx       = minBlockX + lx;
                int wz       = minBlockZ + lz;
                int surfaceY = computeSurfaceY(wx, wz);

                for (int i = 0; i < height; i++) {
                    int y = minY + i;
                    BlockState state;
                    if (y == minY) {
                        state = bedrock;
                    } else if (y <= surfaceY) {
                        state = stone;
                    } else if (y <= SEA_LEVEL) {
                        state = water;
                    } else {
                        state = air;
                    }
                    mutablePos.set(wx, y, wz);
                    chunk.setBlockState(mutablePos, state);
                }
            }
        }

        // Prime heightmaps after all blocks are placed so they are computed in one scan
        // rather than being updated incrementally on every setBlockState call.
        Heightmap.primeHeightmaps(chunk, Set.of(
            Heightmap.Types.OCEAN_FLOOR_WG,
            Heightmap.Types.WORLD_SURFACE_WG
        ));

        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(
            WorldGenRegion region,
            StructureManager structureManager,
            RandomState randomState,
            ChunkAccess chunk) {
        initSeedIfNeeded(randomState);

        int minBlockX = chunk.getPos().getMinBlockX();
        int minBlockZ = chunk.getPos().getMinBlockZ();
        int chunkMinY = chunk.getMinY();

        BlockState grassBlock = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState dirt       = Blocks.DIRT.defaultBlockState();
        BlockState sand       = Blocks.SAND.defaultBlockState();
        BlockState gravel     = Blocks.GRAVEL.defaultBlockState();

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = minBlockX + lx;
                int wz = minBlockZ + lz;
                int topSolid = computeSurfaceY(wx, wz);

                if (topSolid <= OCEAN_FLOOR_Y) {
                    mutablePos.set(wx, topSolid, wz);
                    chunk.setBlockState(mutablePos, gravel);
                } else if (topSolid <= SEA_LEVEL + 4) {
                    // Underwater slope and beach → 3 layers of sand
                    for (int i = 0; i < 3; i++) {
                        int sy = topSolid - i;
                        if (sy >= chunkMinY) {
                            mutablePos.set(wx, sy, wz);
                            chunk.setBlockState(mutablePos, sand);
                        }
                    }
                } else {
                    // Inland → grass cap + SOIL_DEPTH dirt layers
                    mutablePos.set(wx, topSolid, wz);
                    chunk.setBlockState(mutablePos, grassBlock);
                    for (int i = 1; i <= SOIL_DEPTH; i++) {
                        if (topSolid - i >= chunkMinY) {
                            mutablePos.set(wx, topSolid - i, wz);
                            chunk.setBlockState(mutablePos, dirt);
                        }
                    }
                    // ~5% chance to scatter pebbles on inland grass
                    if (hash2D(pebbleSeed, wx, wz) < 0.05) {
                        mutablePos.set(wx, topSolid + 1, wz);
                        int count = (int)(hash2D(~pebbleSeed, wx, wz) * 4) + 1;
                        chunk.setBlockState(mutablePos, ModBlocks.PEBBLE.defaultBlockState().setValue(PebbleBlock.COUNT, count));
                    }
                }
            }
        }
    }

    @Override
    public int getBaseHeight(
            int x, int z,
            Heightmap.Types type,
            LevelHeightAccessor heightAccessor,
            RandomState randomState) {
        initSeedIfNeeded(randomState);
        int surfaceY = computeSurfaceY(x, z);
        if (type == Heightmap.Types.WORLD_SURFACE || type == Heightmap.Types.WORLD_SURFACE_WG) {
            return Math.max(surfaceY + 1, SEA_LEVEL + 1);
        }
        return Math.max(surfaceY + 1, MIN_Y + 1);
    }

    @Override
    public NoiseColumn getBaseColumn(
            int x, int z,
            LevelHeightAccessor heightAccessor,
            RandomState randomState) {
        initSeedIfNeeded(randomState);
        int minY     = heightAccessor.getMinY();
        int height   = heightAccessor.getHeight();
        int surfaceY = computeSurfaceY(x, z);

        BlockState[] column = new BlockState[height];
        for (int i = 0; i < height; i++) {
            int y = minY + i;
            if (y == minY) {
                column[i] = Blocks.BEDROCK.defaultBlockState();
            } else if (y <= surfaceY) {
                column[i] = Blocks.STONE.defaultBlockState();
            } else if (y <= SEA_LEVEL) {
                column[i] = Blocks.WATER.defaultBlockState();
            } else {
                column[i] = Blocks.AIR.defaultBlockState();
            }
        }
        return new NoiseColumn(minY, column);
    }

    @Override
    public void createStructures(
            RegistryAccess registryAccess,
            ChunkGeneratorStructureState structureState,
            StructureManager structureManager,
            ChunkAccess chunk,
            StructureTemplateManager structureTemplateManager,
            ResourceKey<Level> level) {
        // No structures
    }

    @Override
    public void createReferences(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkAccess chunk) {
        // No structures
    }

    @Override
    public void applyCarvers(
            WorldGenRegion region,
            long seed,
            RandomState randomState,
            BiomeManager biomeManager,
            StructureManager structureManager,
            ChunkAccess chunk) {
        // No caves in island world
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        // No forced initial mob spawning
    }

    @Override
    public int getGenDepth() { return GEN_DEPTH; }

    @Override
    public int getSeaLevel() { return SEA_LEVEL; }

    @Override
    public int getMinY() { return MIN_Y; }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        if (initializedState == null) return;
        long[] nearest = IslandGrid.nearestIslandCenter(islandGridSeed, pos.getX(), pos.getZ());
        int dist = (int) Math.sqrt(nearest[2]);
        info.add("Island: center=(" + nearest[0] + "," + nearest[1] + ") dist=" + dist);
    }
}
