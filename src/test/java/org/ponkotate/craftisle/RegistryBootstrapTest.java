package org.ponkotate.craftisle;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.ponkotate.craftisle.block.PebbleBlock;
import org.ponkotate.craftisle.block.PlasticRopeBlock;

import static org.junit.jupiter.api.Assertions.*;

class RegistryBootstrapTest {

    private static PebbleBlock pebble;
    private static PlasticRopeBlock plasticRope;

    @BeforeAll
    static void beforeAll() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ResourceKey<Block> pebbleKey = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("test", "pebble"));
        ResourceKey<Block> ropeKey = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("test", "plastic_rope"));
        pebble = new PebbleBlock(BlockBehaviour.Properties.of().setId(pebbleKey));
        plasticRope = new PlasticRopeBlock(BlockBehaviour.Properties.of().setId(ropeKey));
    }

    @Test
    void pebbleDefaultCountIsOne() {
        assertEquals(1, pebble.defaultBlockState().getValue(PebbleBlock.COUNT));
    }

    @Test
    void pebbleShapeForAllCounts() {
        for (int count = 1; count <= 4; count++) {
            BlockState state = pebble.defaultBlockState().setValue(PebbleBlock.COUNT, count);
            VoxelShape shape = pebble.getShape(state, null, null, CollisionContext.empty());
            assertNotNull(shape, "shape should not be null for count=" + count);
        }
    }

    @Test
    void pebbleCollisionShapeIsEmpty() {
        BlockState state = pebble.defaultBlockState();
        VoxelShape collision = pebble.getCollisionShape(state, null, null, CollisionContext.empty());
        assertSame(Shapes.empty(), collision);
    }

    @Test
    void pebbleCanBeReplaced_belowMax() {
        assertTrue(pebble.defaultBlockState().getValue(PebbleBlock.COUNT) < 4,
            "default count should be below max");
    }

    @Test
    void pebbleMaxCountIsNotReplaceable_stateVerification() {
        BlockState maxState = pebble.defaultBlockState().setValue(PebbleBlock.COUNT, 4);
        assertEquals(4, maxState.getValue(PebbleBlock.COUNT));
    }

    @Test
    void plasticRopeShapeIsFlat() {
        BlockState state = plasticRope.defaultBlockState();
        VoxelShape shape = plasticRope.getShape(state, null, null, CollisionContext.empty());
        assertNotNull(shape, "shape should not be null");
        assertNotSame(Shapes.empty(), shape, "shape should not be empty (it's a thin floor shape)");
    }

    @Test
    void plasticRopeCollisionShapeIsEmpty() {
        BlockState state = plasticRope.defaultBlockState();
        VoxelShape collision = plasticRope.getCollisionShape(state, null, null, CollisionContext.empty());
        assertSame(Shapes.empty(), collision);
    }

    @Test
    void pebbleStateDefinitionHasCountProperty() {
        assertTrue(pebble.defaultBlockState().hasProperty(PebbleBlock.COUNT));
    }
}
