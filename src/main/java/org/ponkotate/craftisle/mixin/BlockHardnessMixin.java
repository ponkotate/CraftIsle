package org.ponkotate.craftisle.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(BlockBehaviour.class)
public class BlockHardnessMixin {

    // 石を素手で壊す相当: hardness 1.5 × divisor 100 = 150
    @Unique
    private static final float CRAFT_ISLE_HARDNESS_FACTOR = 150.0f;

    // Blocks クラスの循環初期化を避けるため、静的フィールドでは初期化せず遅延初期化する
    @Unique
    private static volatile Set<Block> craftIsle$hardenedBlocks = null;

    @Unique
    private static Set<Block> craftIsle$getHardenedBlocks() {
        Set<Block> set = craftIsle$hardenedBlocks;
        if (set == null) {
            craftIsle$hardenedBlocks = set = Set.of(
                Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG,
                Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.CHERRY_LOG,
                Blocks.MANGROVE_LOG, Blocks.PALE_OAK_LOG,
                Blocks.SAND, Blocks.RED_SAND,
                Blocks.GRASS_BLOCK,
                Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT, Blocks.PODZOL
            );
        }
        return set;
    }

    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void overrideDestroyProgress(
        BlockState state, Player player, BlockGetter level, BlockPos pos,
        CallbackInfoReturnable<Float> cir
    ) {
        if (craftIsle$getHardenedBlocks().contains(state.getBlock())) {
            cir.setReturnValue(player.getDestroySpeed(state) / CRAFT_ISLE_HARDNESS_FACTOR);
        }
    }
}
