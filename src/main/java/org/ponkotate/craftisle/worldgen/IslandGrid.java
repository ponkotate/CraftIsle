package org.ponkotate.craftisle.worldgen;

final class IslandGrid {

    static final int CELL_SIZE     = 1500;
    static final int OFFSET_MAX    = 150;
    static final int ISLAND_RADIUS = 120;

    private IslandGrid() {}

    static long[] islandCenterForCell(long seed, int cx, int cz) {
        if (cx == 0 && cz == 0) return new long[]{0L, 0L};
        long h = seed;
        h ^= (long) cx * 0x9e3779b97f4a7c15L;
        h ^= (long) cz * 0x6c62272e07bb0142L;
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        long rx = (h & 0xFFFFFFFFL) % (OFFSET_MAX * 2L + 1) - OFFSET_MAX;
        long rz = ((h >>> 32) & 0xFFFFFFFFL) % (OFFSET_MAX * 2L + 1) - OFFSET_MAX;
        return new long[]{(long) cx * CELL_SIZE + rx, (long) cz * CELL_SIZE + rz};
    }

    /** Returns {centerX, centerZ, distSq} for the nearest island center. */
    static long[] nearestIslandCenter(long seed, int wx, int wz) {
        int cellX = Math.floorDiv(wx, CELL_SIZE);
        int cellZ = Math.floorDiv(wz, CELL_SIZE);
        long bestDistSq = Long.MAX_VALUE;
        long bestX = 0, bestZ = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long[] c = islandCenterForCell(seed, cellX + dx, cellZ + dz);
                long ddx = c[0] - wx;
                long ddz = c[1] - wz;
                long d2  = ddx * ddx + ddz * ddz;
                if (d2 < bestDistSq) {
                    bestDistSq = d2;
                    bestX = c[0];
                    bestZ = c[1];
                }
            }
        }
        return new long[]{bestX, bestZ, bestDistSq};
    }
}
