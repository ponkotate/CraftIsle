package org.ponkotate.craftisle.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import org.ponkotate.craftisle.block.PebbleBlock;
import org.ponkotate.craftisle.registry.ModBlocks;

public class PebbleBlockGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void pebblePlacesOnSolidSurface(GameTestHelper helper) {
        BlockPos ground = new BlockPos(2, 0, 2);
        BlockPos pebble = ground.above();
        helper.setBlock(ground, Blocks.STONE);
        helper.setBlock(pebble, ModBlocks.PEBBLE);
        helper.assertBlock(pebble, b -> b == ModBlocks.PEBBLE, "pebble should be placed on solid surface");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void pebbleStacksToFour(GameTestHelper helper) {
        BlockPos ground = new BlockPos(2, 0, 2);
        BlockPos pebble = ground.above();
        helper.setBlock(ground, Blocks.STONE);
        helper.setBlock(pebble, ModBlocks.PEBBLE.defaultBlockState().setValue(PebbleBlock.COUNT, 4));
        helper.assertBlockState(pebble, s -> s.getValue(PebbleBlock.COUNT) == 4,
            () -> "count should be 4");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void pebbleFallsOnNonSolidSurface(GameTestHelper helper) {
        BlockPos support = new BlockPos(2, 1, 2);
        BlockPos pebble = support.above();
        helper.setBlock(support, Blocks.STONE);
        helper.setBlock(pebble, ModBlocks.PEBBLE);
        helper.assertBlock(pebble, b -> b == ModBlocks.PEBBLE, "pebble should initially be placed");
        helper.destroyBlock(support);
        helper.runAfterDelay(2, () -> {
            helper.assertBlock(pebble, b -> b.isAir(), "pebble should be removed without solid support");
            helper.succeed();
        });
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void pebbleCountStateIsPreserved(GameTestHelper helper) {
        BlockPos ground = new BlockPos(2, 0, 2);
        BlockPos pebble = ground.above();
        helper.setBlock(ground, Blocks.STONE);
        helper.setBlock(pebble, ModBlocks.PEBBLE.defaultBlockState().setValue(PebbleBlock.COUNT, 3));
        helper.assertBlockState(pebble, s -> s.getValue(PebbleBlock.COUNT) == 3,
            () -> "count=3 state should be preserved");
        helper.succeed();
    }
}
