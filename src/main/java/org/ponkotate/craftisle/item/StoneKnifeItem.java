package org.ponkotate.craftisle.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.ponkotate.craftisle.registry.ModItems;

import java.util.Map;

public class StoneKnifeItem extends Item {

    private static final Map<Block, Block> STRIPPABLES = Map.ofEntries(
        Map.entry(Blocks.OAK_LOG,          Blocks.STRIPPED_OAK_LOG),
        Map.entry(Blocks.OAK_WOOD,         Blocks.STRIPPED_OAK_WOOD),
        Map.entry(Blocks.SPRUCE_LOG,       Blocks.STRIPPED_SPRUCE_LOG),
        Map.entry(Blocks.SPRUCE_WOOD,      Blocks.STRIPPED_SPRUCE_WOOD),
        Map.entry(Blocks.BIRCH_LOG,        Blocks.STRIPPED_BIRCH_LOG),
        Map.entry(Blocks.BIRCH_WOOD,       Blocks.STRIPPED_BIRCH_WOOD),
        Map.entry(Blocks.JUNGLE_LOG,       Blocks.STRIPPED_JUNGLE_LOG),
        Map.entry(Blocks.JUNGLE_WOOD,      Blocks.STRIPPED_JUNGLE_WOOD),
        Map.entry(Blocks.ACACIA_LOG,       Blocks.STRIPPED_ACACIA_LOG),
        Map.entry(Blocks.ACACIA_WOOD,      Blocks.STRIPPED_ACACIA_WOOD),
        Map.entry(Blocks.DARK_OAK_LOG,     Blocks.STRIPPED_DARK_OAK_LOG),
        Map.entry(Blocks.DARK_OAK_WOOD,    Blocks.STRIPPED_DARK_OAK_WOOD),
        Map.entry(Blocks.PALE_OAK_LOG,     Blocks.STRIPPED_PALE_OAK_LOG),
        Map.entry(Blocks.PALE_OAK_WOOD,    Blocks.STRIPPED_PALE_OAK_WOOD),
        Map.entry(Blocks.MANGROVE_LOG,     Blocks.STRIPPED_MANGROVE_LOG),
        Map.entry(Blocks.MANGROVE_WOOD,    Blocks.STRIPPED_MANGROVE_WOOD),
        Map.entry(Blocks.CHERRY_LOG,       Blocks.STRIPPED_CHERRY_LOG),
        Map.entry(Blocks.CHERRY_WOOD,      Blocks.STRIPPED_CHERRY_WOOD),
        Map.entry(Blocks.BAMBOO_BLOCK,     Blocks.STRIPPED_BAMBOO_BLOCK),
        Map.entry(Blocks.CRIMSON_STEM,     Blocks.STRIPPED_CRIMSON_STEM),
        Map.entry(Blocks.CRIMSON_HYPHAE,   Blocks.STRIPPED_CRIMSON_HYPHAE),
        Map.entry(Blocks.WARPED_STEM,      Blocks.STRIPPED_WARPED_STEM),
        Map.entry(Blocks.WARPED_HYPHAE,    Blocks.STRIPPED_WARPED_HYPHAE)
    );

    public StoneKnifeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        Block strippedBlock = STRIPPABLES.get(state.getBlock());
        if (strippedBlock == null) return InteractionResult.PASS;

        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        level.playSound(player, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0f, 1.0f);

        if (!level.isClientSide()) {
            BlockState newState = strippedBlock.defaultBlockState();
            if (state.hasProperty(BlockStateProperties.AXIS)) {
                newState = newState.setValue(BlockStateProperties.AXIS, state.getValue(BlockStateProperties.AXIS));
            }
            level.setBlock(pos, newState, Block.UPDATE_ALL);

            level.addFreshEntity(new ItemEntity(
                level,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                new ItemStack(ModItems.OAK_BARK)
            ));
        }

        context.getItemInHand().hurtAndBreak(1, player, context.getHand());

        return InteractionResult.SUCCESS;
    }
}
