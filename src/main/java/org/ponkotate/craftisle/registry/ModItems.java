package org.ponkotate.craftisle.registry;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import org.ponkotate.craftisle.CraftIsle;

import java.util.function.Function;

public class ModItems {

    public static final Item PEBBLE      = registerBlockItem("pebble", ModBlocks.PEBBLE);
    public static final Item STONE_KNIFE = register("stone_knife", key -> new Item(new Item.Properties().setId(key)));
    public static final Item PLASTIC_ROPE = registerBlockItem("plastic_rope", ModBlocks.PLASTIC_ROPE);

    private static Item register(String name, Function<ResourceKey<Item>, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(CraftIsle.MOD_ID, name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(key));
    }

    private static Item registerBlockItem(String name, Block block) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(CraftIsle.MOD_ID, name));
        return Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(block, new Item.Properties().setId(key)));
    }

    private static ResourceKey<CreativeModeTab> tabKey(String name) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace(name));
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(tabKey("ingredients")).register(output -> {
            output.accept(PEBBLE);
            output.accept(PLASTIC_ROPE);
        });
        CreativeModeTabEvents.modifyOutputEvent(tabKey("tools_and_utilities")).register(output -> {
            output.accept(STONE_KNIFE);
        });
    }
}
