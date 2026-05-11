package org.ponkotate.craftisle.registry;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.registry.FuelValueEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;

import org.ponkotate.craftisle.CraftIsle;
import org.ponkotate.craftisle.item.StoneKnifeItem;

import java.util.function.Function;

public class ModItems {

    // 攻撃ボーナス1・耐久59（木製ツール同等）・コブルストーンで修理
    private static final ToolMaterial STONE_KNIFE_MATERIAL = new ToolMaterial(
        BlockTags.INCORRECT_FOR_STONE_TOOL,
        59,
        4.0f,
        1.0f,
        5,
        ItemTags.STONE_TOOL_MATERIALS
    );

    public static final Item PEBBLE       = registerBlockItem("pebble", ModBlocks.PEBBLE);
    public static final Item STONE_KNIFE  = register("stone_knife", key ->
        new StoneKnifeItem(STONE_KNIFE_MATERIAL.applySwordProperties(
            new Item.Properties().setId(key),
            0.0f,   // total damage = 1(base) + 1(bonus) + 0 = 2
            -2.0f   // total speed  = 4(base) + (-2) = 2
        ))
    );
    public static final Item PLASTIC_ROPE = registerBlockItem("plastic_rope", ModBlocks.PLASTIC_ROPE);
    public static final Item OAK_BARK     = register("oak_bark",   key -> new Item(new Item.Properties().setId(key)));
    public static final Item BIRCH_BARK   = register("birch_bark", key -> new Item(new Item.Properties().setId(key)));

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
        FuelValueEvents.BUILD.register((builder, context) -> {
            builder.add(OAK_BARK, 100);
            builder.add(BIRCH_BARK, 100);
        });

        CreativeModeTabEvents.modifyOutputEvent(tabKey("ingredients")).register(output -> {
            output.accept(PEBBLE);
            output.accept(OAK_BARK);
            output.accept(BIRCH_BARK);
            output.accept(PLASTIC_ROPE);
        });
        CreativeModeTabEvents.modifyOutputEvent(tabKey("tools_and_utilities")).register(output -> {
            output.accept(STONE_KNIFE);
        });
    }
}
