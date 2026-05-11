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

import java.util.HashMap;
import java.util.Map;

public class StoneKnifeItem extends Item {

    private static final Map<Block, Block> STRIPPABLES = Map.of(
        Blocks.OAK_LOG,   Blocks.STRIPPED_OAK_LOG,
        Blocks.OAK_WOOD,  Blocks.STRIPPED_OAK_WOOD,
        Blocks.BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG,
        Blocks.BIRCH_WOOD, Blocks.STRIPPED_BIRCH_WOOD
    );

    // ModItems のフィールドを参照するため、クラスロード時ではなく初回 useOn 時に構築する
    private static volatile Map<Block, Item> barkDrops = null;

    private static Map<Block, Item> getBarkDrops() {
        Map<Block, Item> map = barkDrops;
        if (map == null) {
            map = new HashMap<>();
            map.put(Blocks.OAK_LOG,    ModItems.OAK_BARK);
            map.put(Blocks.OAK_WOOD,   ModItems.OAK_BARK);
            map.put(Blocks.BIRCH_LOG,  ModItems.BIRCH_BARK);
            map.put(Blocks.BIRCH_WOOD, ModItems.BIRCH_BARK);
            barkDrops = map;
        }
        return map;
    }

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

            Item bark = getBarkDrops().get(state.getBlock());
            level.addFreshEntity(new ItemEntity(
                level,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                new ItemStack(bark)
            ));
        }

        context.getItemInHand().hurtAndBreak(1, player, context.getHand());

        return InteractionResult.SUCCESS;
    }
}
