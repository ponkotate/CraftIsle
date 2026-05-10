package org.ponkotate.craftisle.advancement;

import net.minecraft.SharedConstants;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AttackBlockTriggerTest {

    @BeforeAll
    static void beforeAll() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void matchesWithEmptyPredicatesReturnsTrue() {
        AttackBlockTrigger.TriggerInstance instance =
            new AttackBlockTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty());
        assertTrue(instance.matches(null, null, ItemStack.EMPTY));
    }

    @Test
    void matchesWithNonMatchingItemReturnsFalse() {
        // Count predicate requiring exactly 1; ItemStack.EMPTY has count=0 → no match
        ItemPredicate countOnePredicate = new ItemPredicate(
            Optional.empty(),
            MinMaxBounds.Ints.exactly(1),
            DataComponentMatchers.ANY
        );
        AttackBlockTrigger.TriggerInstance instance =
            new AttackBlockTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.of(countOnePredicate));
        assertFalse(instance.matches(null, null, ItemStack.EMPTY));
    }

    @Test
    void matchesWithMatchingItemReturnsTrue() {
        // Count predicate requiring exactly 0; ItemStack.EMPTY has count=0 → match
        ItemPredicate countZeroPredicate = new ItemPredicate(
            Optional.empty(),
            MinMaxBounds.Ints.exactly(0),
            DataComponentMatchers.ANY
        );
        AttackBlockTrigger.TriggerInstance instance =
            new AttackBlockTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.of(countZeroPredicate));
        assertTrue(instance.matches(null, null, ItemStack.EMPTY));
    }
}
