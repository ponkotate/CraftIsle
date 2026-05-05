package org.ponkotate.craftisle.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

public class IslandBiomeSource extends BiomeSource {

    public static final MapCodec<IslandBiomeSource> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Biome.CODEC.fieldOf("land_biome").forGetter(o -> o.landBiome),
            Biome.CODEC.fieldOf("ocean_biome").forGetter(o -> o.oceanBiome)
        ).apply(instance, IslandBiomeSource::new)
    );

    private final Holder<Biome> landBiome;
    private final Holder<Biome> oceanBiome;
    private volatile long islandGridSeed = 0L;

    public IslandBiomeSource(Holder<Biome> landBiome, Holder<Biome> oceanBiome) {
        this.landBiome  = landBiome;
        this.oceanBiome = oceanBiome;
    }

    void setGridSeed(long seed) {
        this.islandGridSeed = seed;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(landBiome, oceanBiome);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler climateSampler) {
        long[] nearest = IslandGrid.nearestIslandCenter(islandGridSeed, x << 2, z << 2);
        double dist = Math.sqrt(nearest[2]);
        double t = Math.max(0.0, (IslandGrid.ISLAND_RADIUS - dist) / IslandGrid.ISLAND_RADIUS);
        return (t * t * (3.0 - 2.0 * t)) > 0.0 ? landBiome : oceanBiome;
    }
}
