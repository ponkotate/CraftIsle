package org.ponkotate.craftisle;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import org.ponkotate.craftisle.registry.ModBlocks;
import org.ponkotate.craftisle.registry.ModItems;
import org.ponkotate.craftisle.registry.ModTriggers;
import org.ponkotate.craftisle.registry.ModWorldGen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CraftIsle implements ModInitializer {
	public static final String MOD_ID = "craft_isle";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.initialize();
		ModItems.initialize();
		ModTriggers.initialize();
		ModWorldGen.initialize();

		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
				ModTriggers.ATTACK_BLOCK.fire(serverPlayer, (ServerLevel) world, pos);
			}
			return InteractionResult.PASS;
		});

		LOGGER.info("Craft Isle initialized!");
	}
}
