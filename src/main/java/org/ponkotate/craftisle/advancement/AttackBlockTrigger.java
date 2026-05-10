package org.ponkotate.craftisle.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.criterion.BlockPredicate;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class AttackBlockTrigger extends SimpleCriterionTrigger<AttackBlockTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void fire(ServerPlayer player, ServerLevel level, BlockPos pos) {
        ItemStack heldItem = player.getMainHandItem();
        this.trigger(player, instance -> instance.matches(level, pos, heldItem));
    }

    public record TriggerInstance(
        Optional<ContextAwarePredicate> player,
        Optional<BlockPredicate> block,
        Optional<ItemPredicate> item
    ) implements SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                BlockPredicate.CODEC.optionalFieldOf("block").forGetter(TriggerInstance::block),
                ItemPredicate.CODEC.optionalFieldOf("item").forGetter(TriggerInstance::item)
            ).apply(inst, TriggerInstance::new)
        );

        public boolean matches(ServerLevel level, BlockPos pos, ItemStack heldItem) {
            if (block.isPresent() && !block.get().matches(level, pos)) return false;
            if (item.isPresent() && !item.get().test(heldItem)) return false;
            return true;
        }
    }
}
