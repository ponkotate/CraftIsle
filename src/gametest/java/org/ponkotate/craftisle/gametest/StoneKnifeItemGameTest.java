package org.ponkotate.craftisle.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.ponkotate.craftisle.registry.ModItems;

public class StoneKnifeItemGameTest {

    @GameTest
    public void stoneKnifeStripsOakLog(GameTestHelper helper) {
        BlockPos logPos = new BlockPos(2, 1, 2);
        helper.setBlock(logPos, Blocks.OAK_LOG);

        ItemStack knifeStack = new ItemStack(ModItems.STONE_KNIFE);
        BlockHitResult hit = new BlockHitResult(
            Vec3.atCenterOf(helper.absolutePos(logPos)),
            Direction.UP,
            helper.absolutePos(logPos),
            false
        );
        UseOnContext ctx = new UseOnContext(
            helper.getLevel(),
            helper.makeMockPlayer(GameType.SURVIVAL),
            InteractionHand.MAIN_HAND,
            knifeStack,
            hit
        );
        knifeStack.useOn(ctx);

        helper.assertBlock(logPos, b -> b == Blocks.STRIPPED_OAK_LOG,
            b -> Component.literal("oak_log should become stripped_oak_log"));
        helper.succeed();
    }

    @GameTest
    public void stoneKnifeDropsBark(GameTestHelper helper) {
        BlockPos logPos = new BlockPos(2, 1, 2);
        helper.setBlock(logPos, Blocks.OAK_LOG);

        ItemStack knifeStack = new ItemStack(ModItems.STONE_KNIFE);
        BlockHitResult hit = new BlockHitResult(
            Vec3.atCenterOf(helper.absolutePos(logPos)),
            Direction.UP,
            helper.absolutePos(logPos),
            false
        );
        UseOnContext ctx = new UseOnContext(
            helper.getLevel(),
            helper.makeMockPlayer(GameType.SURVIVAL),
            InteractionHand.MAIN_HAND,
            knifeStack,
            hit
        );
        knifeStack.useOn(ctx);

        helper.assertItemEntityPresent(ModItems.OAK_BARK, logPos, 2.0);
        helper.succeed();
    }

    @GameTest
    public void stoneKnifeIgnoresNonLog(GameTestHelper helper) {
        BlockPos stonePos = new BlockPos(2, 1, 2);
        helper.setBlock(stonePos, Blocks.STONE);

        ItemStack knifeStack = new ItemStack(ModItems.STONE_KNIFE);
        BlockHitResult hit = new BlockHitResult(
            Vec3.atCenterOf(helper.absolutePos(stonePos)),
            Direction.UP,
            helper.absolutePos(stonePos),
            false
        );
        UseOnContext ctx = new UseOnContext(
            helper.getLevel(),
            helper.makeMockPlayer(GameType.SURVIVAL),
            InteractionHand.MAIN_HAND,
            knifeStack,
            hit
        );
        knifeStack.useOn(ctx);

        helper.assertBlock(stonePos, b -> b == Blocks.STONE,
            b -> Component.literal("stone block should not change when right-clicked with stone knife"));
        helper.succeed();
    }
}
