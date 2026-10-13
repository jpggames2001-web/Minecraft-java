package dev.skyblock;

import dev.skyblock.game.*;
import dev.skyblock.world.*;
import dev.skyblock.audio.SynthAudio;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class GameTest {
    SynthAudio audio; Game game;
    @BeforeEach void setup() {
        audio=new SynthAudio(false); World world=new World(); Player p=new Player();
        p.x=p.z=30.5; p.y=65; p.yaw=0;
        for(int z=27;z<=38;z++) for(int x=27;x<=38;x++) world.set(x,64,z,Block.STONE);
        game=new Game(world,p,audio); game.player.tick(world,Controls.NONE); game.player.tick(world,Controls.NONE);
    }
    @AfterEach void cleanup() { audio.close(); }
    @Test void placingConsumesOneBlockAndRejectsPlayerOverlap() {
        game.player.inventory.set(0,new Stack(Item.DIRT,3)); game.target=new RayHit(32,64,32,0,1,0,3); game.use();
        assertEquals(Block.DIRT,game.world.get(32,65,32)); assertEquals(2,game.player.held().count());
        game.target=new RayHit(30,64,30,0,1,0,1); game.use(); assertEquals(Block.AIR,game.world.get(30,65,30)); assertEquals(2,game.player.held().count());
    }
    @Test void chestAndCraftingTableOpenTheirInterfaces() {
        game.world.set(32,65,32,Block.CHEST); game.target=new RayHit(32,65,32,0,1,0,3); game.use(); assertNotNull(game.openContainer); assertFalse(game.openContainer.furnace);
        game.openContainer=null; game.world.set(32,65,32,Block.CRAFTING_TABLE); game.use(); assertTrue(game.craftingTable);
    }
    @Test void bucketCollectsSourcesAndReturnsEmptyBucketWhenPlaced() {
        game.world.set(32,65,32,Block.WATER); game.player.inventory.set(0,new Stack(Item.BUCKET,2)); game.target=new RayHit(32,65,32,0,1,0,3); game.use();
        assertEquals(Block.AIR,game.world.get(32,65,32)); assertEquals(Item.BUCKET,game.player.held().item()); assertEquals(1,game.player.held().count()); assertEquals(Item.WATER_BUCKET,game.player.inventory.get(1).item());
        game.player.selected=1; game.target=new RayHit(32,64,32,0,1,0,3); game.use(); assertEquals(Block.WATER,game.world.get(32,65,32)); assertEquals(Item.BUCKET,game.player.held().item());
    }
    @Test void bucketCannotCollectFlowingWater() {
        game.world.set(32,65,32,Block.WATER,2); game.player.inventory.set(0,new Stack(Item.BUCKET,1)); game.target=new RayHit(32,65,32,0,1,0,3); game.use();
        assertEquals(Item.BUCKET,game.player.held().item()); assertEquals(Block.WATER,game.world.get(32,65,32));
    }
    @Test void hoeAndSeedsCreateFarmlandAndCrop() {
        game.world.set(32,64,32,Block.DIRT); game.player.inventory.set(0,new Stack(Item.WOOD_HOE,1)); game.target=new RayHit(32,64,32,0,1,0,3); game.use();
        assertEquals(Block.FARMLAND,game.world.get(32,64,32)); assertEquals(1,game.player.held().damage());
        game.player.inventory.set(0,new Stack(Item.SEEDS,2)); game.use(); assertEquals(Block.WHEAT,game.world.get(32,65,32)); assertEquals(1,game.player.held().count());
    }
    @Test void miningStoneProducesCobblestoneAndToolWear() {
        game.player.x=32.5; game.player.z=35.5; game.player.pitch=15; game.world.set(32,65,32,Block.STONE); game.player.inventory.set(0,new Stack(Item.STONE_PICKAXE,1));
        for(int i=0;i<30;i++) game.tick(Controls.NONE,true,false);
        assertEquals(Block.AIR,game.world.get(32,65,32)); assertEquals(1,game.player.held().damage()); assertTrue(game.drops.stream().anyMatch(d->d.stack.item()==Item.COBBLESTONE));
    }
    @Test void harvestingRestrictionsDistinguishToolsAndMaterials() { assertFalse(Game.canHarvest(Block.STONE,null)); assertTrue(Game.canHarvest(Block.STONE,Item.WOOD_PICKAXE)); assertFalse(Game.canHarvest(Block.IRON_ORE,Item.WOOD_PICKAXE)); assertTrue(Game.canHarvest(Block.IRON_ORE,Item.STONE_PICKAXE)); assertFalse(Game.canHarvest(Block.BEDROCK,Item.IRON_PICKAXE)); }
    @Test void heldFoodRequiresThirtyTwoTicksAndConsumesOne() {
        game.player.food=10; game.player.inventory.set(0,new Stack(Item.APPLE,2));
        for(int i=0;i<31;i++) game.tick(Controls.NONE,false,true); assertEquals(10,game.player.food); game.tick(Controls.NONE,false,true); assertEquals(14,game.player.food); assertEquals(1,game.player.held().count());
    }
    @Test void itemPickupPreservesUnacceptedRemainder() {
        for(int i=0;i<36;i++) game.player.inventory.set(i,new Stack(Item.DIRT,64));
        game.player.inventory.set(0,new Stack(Item.LOG,60)); Drop d=game.spawnDrop(new Stack(Item.LOG,9),game.player.x,game.player.y,game.player.z); d.pickupDelay=0; d.vy=0;
        d.tick(game.world,game.player); assertEquals(64,game.player.held().count()); assertEquals(5,d.stack.count());
    }
    @Test void voidDeathClearsInventoryAndRespawnRestoresVitals() {
        game.player.y=-65; game.player.health=1; game.player.inventory.set(0,new Stack(Item.LOG,4)); game.tick(Controls.NONE,false,false);
        assertEquals(0,game.player.health); assertNull(game.player.held());
        game.player.respawn(); assertEquals(20,game.player.health); assertEquals(65,game.player.y); assertEquals(300,game.player.air);
    }
    @Test void creativePlacementDoesNotConsumeItems() { game.creativeKit(); game.target=new RayHit(32,64,32,0,1,0,3); int count=game.player.held().count(); game.use(); assertEquals(count,game.player.held().count()); assertFalse(game.player.hurt(20)); }
    @Test void generatedAudioHasNonzeroPcmAndNoExternalFiles() { byte[] pcm=SynthAudio.generate(SynthAudio.Effect.HURT); assertEquals(11000,pcm.length); boolean nonzero=false; for(byte b:pcm) nonzero|=b!=0; assertTrue(nonzero); }
}
