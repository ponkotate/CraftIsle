package org.ponkotate.craftisle.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import org.ponkotate.craftisle.CraftIsle;
import org.ponkotate.craftisle.block.PebbleBlock;

import java.util.function.Function;

public class ModBlocks {

    public static final Block PEBBLE = register("pebble", key -> new PebbleBlock(
        BlockBehaviour.Properties.of()
            .setId(key)
            .instabreak()
            .sound(SoundType.STONE)
            .noOcclusion()
    ));

    private static Block register(String name, Function<ResourceKey<Block>, Block> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(CraftIsle.MOD_ID, name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(key));
    }

    public static void initialize() {}
}
