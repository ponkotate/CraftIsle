package org.ponkotate.craftisle.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.ponkotate.craftisle.CraftIsle;
import org.ponkotate.craftisle.advancement.AttackBlockTrigger;

public class ModTriggers {
    public static final AttackBlockTrigger ATTACK_BLOCK = Registry.register(
        BuiltInRegistries.TRIGGER_TYPES,
        Identifier.fromNamespaceAndPath(CraftIsle.MOD_ID, "attack_block"),
        new AttackBlockTrigger()
    );

    public static void initialize() {}
}
