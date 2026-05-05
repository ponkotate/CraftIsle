package org.ponkotate.craftisle.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import org.ponkotate.craftisle.CraftIsle;
import org.ponkotate.craftisle.worldgen.IslandChunkGenerator;

public class ModWorldGen {

    public static void initialize() {
        Registry.register(
            BuiltInRegistries.CHUNK_GENERATOR,
            Identifier.fromNamespaceAndPath(CraftIsle.MOD_ID, "island"),
            IslandChunkGenerator.CODEC
        );
    }
}
