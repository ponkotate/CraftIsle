package org.ponkotate.craftisle.worldgen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IslandGridTest {

    private static final long SEED = 12345L;

    @Test
    void originCellAlwaysReturnsZero() {
        long[] result = IslandGrid.islandCenterForCell(SEED, 0, 0);
        assertArrayEquals(new long[]{0L, 0L}, result);
    }

    @Test
    void centerPositionNearCellOrigin() {
        long[] result = IslandGrid.islandCenterForCell(SEED, 3, 5);
        long cx = 3L * IslandGrid.CELL_SIZE;
        long cz = 5L * IslandGrid.CELL_SIZE;
        assertTrue(Math.abs(result[0] - cx) <= IslandGrid.OFFSET_MAX,
            "X offset out of range: " + (result[0] - cx));
        assertTrue(Math.abs(result[1] - cz) <= IslandGrid.OFFSET_MAX,
            "Z offset out of range: " + (result[1] - cz));
    }

    @Test
    void resultIsDeterministic() {
        long[] a = IslandGrid.islandCenterForCell(SEED, 7, -3);
        long[] b = IslandGrid.islandCenterForCell(SEED, 7, -3);
        assertArrayEquals(a, b);
    }

    @Test
    void differentSeedsProduceDifferentResults() {
        long[] a = IslandGrid.islandCenterForCell(SEED, 1, 1);
        long[] b = IslandGrid.islandCenterForCell(SEED + 1, 1, 1);
        assertFalse(a[0] == b[0] && a[1] == b[1], "different seeds should produce different centers");
    }

    @Test
    void differentCellsProduceDifferentResults() {
        long[] a = IslandGrid.islandCenterForCell(SEED, 1, 1);
        long[] b = IslandGrid.islandCenterForCell(SEED, 2, 1);
        assertFalse(a[0] == b[0] && a[1] == b[1], "different cells should produce different centers");
    }

    @Test
    void nearestAtWorldOriginIsIslandOrigin() {
        long[] result = IslandGrid.nearestIslandCenter(SEED, 0, 0);
        assertEquals(0L, result[0], "nearest X should be 0");
        assertEquals(0L, result[1], "nearest Z should be 0");
    }

    @Test
    void nearestDistSqMatchesActualDistance() {
        long[] result = IslandGrid.nearestIslandCenter(SEED, 500, 300);
        long dx = result[0] - 500L;
        long dz = result[1] - 300L;
        assertEquals(dx * dx + dz * dz, result[2], "distSq should match actual squared distance");
    }

    @Test
    void nearestAlwaysReturnsOneOfNineNeighbors() {
        int wx = 750, wz = -200;
        int cellX = Math.floorDiv(wx, IslandGrid.CELL_SIZE);
        int cellZ = Math.floorDiv(wz, IslandGrid.CELL_SIZE);

        long[] result = IslandGrid.nearestIslandCenter(SEED, wx, wz);
        boolean matchesNeighbor = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long[] c = IslandGrid.islandCenterForCell(SEED, cellX + dx, cellZ + dz);
                if (c[0] == result[0] && c[1] == result[1]) {
                    matchesNeighbor = true;
                }
            }
        }
        assertTrue(matchesNeighbor, "nearest center should be one of the 3x3 neighbor cells");
    }
}
