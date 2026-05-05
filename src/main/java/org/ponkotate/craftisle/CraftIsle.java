package org.ponkotate.craftisle;

import net.fabricmc.api.ModInitializer;

import org.ponkotate.craftisle.registry.ModBlocks;
import org.ponkotate.craftisle.registry.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CraftIsle implements ModInitializer {
	public static final String MOD_ID = "craft_isle";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.initialize();
		ModItems.initialize();

		LOGGER.info("Craft Isle initialized!");
	}
}
