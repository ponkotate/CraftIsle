package org.ponkotate.craftisle.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import org.ponkotate.craftisle.CraftIsle;

public class ModBlocks {
	private static Block register(String name, Block block) {
		return Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(CraftIsle.MOD_ID, name), block);
	}

	public static void initialize() {}
}
