package dev.skyblock;

import dev.skyblock.world.*;
import dev.skyblock.game.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorldTest {
    @Test void skyblockHasStarterChestTreeAndVoid() {
        World w=World.skyblock(); assertEquals(Block.GRASS,w.get(29,64,29)); assertEquals(Block.LOG,w.get(32,65,34));
        assertEquals(Block.AIR,w.get(20,64,20)); assertEquals(Block.BEDROCK,w.get(32,62,32));
        var chest=w.containers.get(World.index(29,65,31)); assertEquals(Item.ICE,chest.inventory.get(0).item()); assertEquals(Item.LAVA_BUCKET,chest.inventory.get(1).item());
        assertEquals(Biome.FOREST,w.biome(32,32)); assertEquals(Biome.DESERT,w.biome(51,31)); assertEquals(Biome.TAIGA,w.biome(30,51));
    }
    @Test void torchLightFallsByOneAndOpaqueWallsBlockIt() {
        World w=new World(); w.set(32,64,32,Block.TORCH); w.updateLighting();
        assertEquals(14,w.light(32,64,32)); assertEquals(13,w.light(33,64,32)); assertEquals(12,w.light(34,64,32));
        w.set(33,64,32,Block.STONE); w.updateLighting(); assertEquals(0,w.light(33,64,32)); assertEquals(10,w.light(34,64,32));
        w.set(32,64,32,Block.AIR); w.updateLighting(); assertEquals(0,w.light(34,64,32));
    }
    @Test void directSkyRemainsFifteenAndRoofCreatesSidewaysAttenuation() {
        World w=new World(); w.updateLighting(); assertEquals(15,w.sky(32,64,32)); w.set(32,65,32,Block.STONE); w.updateLighting();
        assertEquals(15,w.sky(31,64,32)); assertEquals(14,w.sky(32,64,32)); assertEquals(0,w.sky(32,65,32));
    }
    @Test void waterSpreadsAndTwoSourcesCreateRenewableSource() {
        World w=new World(); for(int x=24;x<40;x++) for(int z=24;z<40;z++) w.set(x,63,z,Block.STONE);
        w.set(31,64,32,Block.WATER); w.set(33,64,32,Block.WATER);
        for(int i=0;i<11;i++) w.tick(); assertEquals(Block.WATER,w.get(32,64,32)); assertEquals(0,w.data(32,64,32));
    }
    @Test void sourceAndFlowingLavaProduceDifferentBlocks() {
        World w=new World(); w.set(32,63,32,Block.STONE); w.set(33,63,32,Block.STONE);
        w.set(32,64,32,Block.LAVA); w.set(33,64,32,Block.WATER);
        w.set(36,64,32,Block.LAVA,2); w.set(37,64,32,Block.WATER);
        for(int i=0;i<30;i++) w.tick(); assertEquals(Block.OBSIDIAN,w.get(32,64,32)); assertEquals(Block.COBBLESTONE,w.get(36,64,32));
    }
    @Test void downwardLavaOntoWaterMakesStone() {
        World w=new World(); w.set(32,65,32,Block.LAVA); w.set(32,64,32,Block.WATER);
        for(int i=0;i<30;i++) w.tick(); assertEquals(Block.STONE,w.get(32,64,32)); assertEquals(Block.LAVA,w.get(32,65,32));
    }
    @Test void raycastReturnsPlacementFaceAndRespectsReach() {
        World w=new World(); w.set(32,64,32,Block.STONE);
        RayHit hit=w.raycast(32.5,64.5,36,0,0,-1,4.5,false); assertNotNull(hit); assertEquals(32,hit.z()); assertEquals(1,hit.nz()); assertEquals(3,hit.distance(),1e-12);
        assertNull(w.raycast(32.5,64.5,40,0,0,-1,4.5,false)); assertNull(w.raycast(0,0,0,0,0,0,4.5,false));
    }
    @Test void unsupportedSandFallsUntilSupported() {
        World w=new World(); w.set(32,60,32,Block.STONE); w.set(32,65,32,Block.SAND); w.tick(); assertEquals(Block.AIR,w.get(32,65,32)); assertEquals(Block.SAND,w.get(32,61,32));
    }
    @Test void treeGrowthRequiresSpaceAndValidSoil() {
        World w=new World(); assertFalse(w.growTree(32,65,32)); w.set(32,64,32,Block.GRASS); w.set(32,65,32,Block.SAPLING);
        w.set(32,68,32,Block.STONE); assertFalse(w.growTree(32,65,32)); w.set(32,68,32,Block.AIR); assertTrue(w.growTree(32,65,32)); assertEquals(Block.LOG,w.get(32,69,32));
    }
    @Test void restoreRejectsMalformedBlockIds() { World w=new World(); byte[] b=new byte[World.VOLUME]; b[0]=(byte)255; assertThrows(IllegalArgumentException.class,()->w.restore(b,new byte[World.VOLUME],new byte[World.SIZE*World.SIZE])); }
    @Test void biomeClimateControlsColorsAndSky() { assertEquals(.7,Biome.FOREST.temperature,1e-6); assertEquals(0,Biome.DESERT.rainfall); assertNotEquals(Biome.FOREST.grassColor(),Biome.DESERT.grassColor()); assertNotEquals(Biome.DESERT.skyColor(),Biome.TAIGA.skyColor()); }
}
