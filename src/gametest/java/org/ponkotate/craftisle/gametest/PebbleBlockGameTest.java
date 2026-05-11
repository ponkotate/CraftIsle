package org.ponkotate.craftisle.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import org.ponkotate.craftisle.block.PebbleBlock;
import org.ponkotate.craftisle.registry.ModBlocks;

public class PebbleBlockGameTest {

    @GameTest
    public void pebblePlacesOnSolidSurface(GameTestHelper helper) {
        BlockPos ground = new BlockPos(2, 0, 2);
        BlockPos pebble = ground.above();
        helper.setBlock(ground, Blocks.STONE);
        helper.setBlock(pebble, ModBlocks.PEBBLE);
        helper.assertBlock(pebble, b -> b == ModBlocks.PEBBLE,
            b -> Component.literal("pebble should be placed on solid surface"));
        helper.succeed();
    }

    @GameTest
    public void pebbleStacksToFour(GameTestHelper helper) {
        BlockPos ground = new BlockPos(2, 0, 2);
        BlockPos pebble = ground.above();
        helper.setBlock(ground, Blocks.STONE);
        helper.setBlock(pebble, ModBlocks.PEBBLE.defaultBlockState().setValue(PebbleBlock.COUNT, 4));
        helper.assertBlockState(pebble, s -> s.getValue(PebbleBlock.COUNT) == 4,
            s -> Component.literal("count should be 4"));
        helper.succeed();
    }

    @GameTest
    public void pebbleFallsOnNonSolidSurface(GameTestHelper helper) {
        BlockPos support = new BlockPos(2, 1, 2);
        BlockPos pebble = support.above();
        helper.setBlock(support, Blocks.STONE);
        helper.setBlock(pebble, ModBlocks.PEBBLE);
        helper.assertBlock(pebble, b -> b == ModBlocks.PEBBLE,
            b -> Component.literal("pebble should initially be placed"));
        helper.destroyBlock(support);
        helper.runAfterDelay(2, () -> {
            helper.assertBlockState(pebble, s -> s.isAir(),
                s -> Component.literal("pebble should be removed without solid support"));
            helper.succeed();
        });
    }

    @GameTest
    public void pebbleCountStateIsPreserved(GameTestHelper helper) {
        BlockPos ground = new BlockPos(2, 0, 2);
        BlockPos pebble = ground.above();
        helper.setBlock(ground, Blocks.STONE);
        helper.setBlock(pebble, ModBlocks.PEBBLE.defaultBlockState().setValue(PebbleBlock.COUNT, 3));
        helper.assertBlockState(pebble, s -> s.getValue(PebbleBlock.COUNT) == 3,
            s -> Component.literal("count=3 state should be preserved"));
        helper.succeed();
    }
}
