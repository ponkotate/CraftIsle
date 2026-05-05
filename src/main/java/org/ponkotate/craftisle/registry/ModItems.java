package org.ponkotate.craftisle.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import org.ponkotate.craftisle.CraftIsle;

public class ModItems {
	private static Item register(String name, Item item) {
		return Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(CraftIsle.MOD_ID, name), item);
	}

	public static void initialize() {}
}
