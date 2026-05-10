package org.ponkotate.craftisle.worldgen;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class IslandChunkGeneratorTest {

    private static final long SEED = 98765L;

    @Test
    void hash2DAlwaysInUnitInterval() {
        for (int x = -5; x < 5; x++) {
            for (int z = -5; z < 5; z++) {
                double v = IslandChunkGenerator.hash2D(SEED, x, z);
                assertTrue(v >= 0.0 && v <= 1.0,
                    "hash2D out of [0,1] at (" + x + "," + z + "): " + v);
            }
        }
    }

    @Test
    void hash2DIsDeterministic() {
        double a = IslandChunkGenerator.hash2D(SEED, 3, -7);
        double b = IslandChunkGenerator.hash2D(SEED, 3, -7);
        assertEquals(a, b);
    }

    @Test
    void hash2DProducesVariedOutput() {
        Set<Double> values = new HashSet<>();
        for (int x = 0; x < 10; x++) {
            for (int z = 0; z < 10; z++) {
                values.add(IslandChunkGenerator.hash2D(SEED, x, z));
            }
        }
        assertTrue(values.size() > 50, "hash2D should produce varied output across 100 inputs");
    }

    @Test
    void valueNoiseAlwaysInBounds() {
        for (int i = 0; i < 50; i++) {
            double v = IslandChunkGenerator.valueNoise(SEED, i * 17.3, i * -11.7, 64);
            assertTrue(v >= -1.0 && v <= 1.0,
                "valueNoise out of [-1,1] at i=" + i + ": " + v);
        }
    }

    @Test
    void valueNoiseIsDeterministic() {
        double a = IslandChunkGenerator.valueNoise(SEED, 123.4, -56.7, 128);
        double b = IslandChunkGenerator.valueNoise(SEED, 123.4, -56.7, 128);
        assertEquals(a, b);
    }

    @Test
    void valueNoiseVariesWithPosition() {
        Set<Double> values = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            values.add(IslandChunkGenerator.valueNoise(SEED, i * 100.0, i * 100.0, 64));
        }
        assertTrue(values.size() > 5, "valueNoise should vary with position");
    }
}
