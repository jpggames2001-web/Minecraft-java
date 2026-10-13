package dev.skyblock;

import dev.skyblock.world.*;
import dev.skyblock.game.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClimateTest {
    @Test void classicBiomeIdsAreStable() { assertEquals(Biome.OCEAN,Biome.byId(0)); assertEquals(Biome.DESERT,Biome.byId(2)); assertEquals(Biome.FOREST,Biome.byId(4)); assertEquals(Biome.TAIGA,Biome.byId(5)); assertEquals(Biome.MUSHROOM_SHORE,Biome.byId(15)); assertThrows(IllegalArgumentException.class,()->Biome.byId(16)); }
    @Test void climateMapClampsTemperatureAndUsesWetnessProduct() { assertEquals(ClimateColorMap.GRASS.sample(1,0),ClimateColorMap.GRASS.sample(2,0)); assertEquals(ClimateColorMap.GRASS.sample(0,1),ClimateColorMap.GRASS.sample(0,0)); assertNotEquals(ClimateColorMap.GRASS.sample(1,1),ClimateColorMap.GRASS.sample(1,0)); }
    @Test void swampUsesExplicitOverride() { assertEquals(0x6a7039,Biome.SWAMP.grassColor()); assertEquals(0x6a7039,Biome.SWAMP.foliageColor()); }
    @Test void burningFurnaceEmitsThirteenAndExtinguishesAfterFuelExpires() {
        World w=new World(); w.set(32,64,32,Block.FURNACE); Container c=w.containers.get(World.index(32,64,32)); c.inventory.set(0,new Stack(Item.SAND,1)); c.inventory.set(1,new Stack(Item.STICK,1)); w.tick(); assertEquals(13,w.light(32,64,32)); assertEquals(12,w.light(33,64,32));
        for(int i=0;i<100;i++) w.tick(); assertEquals(0,w.light(32,64,32)); assertEquals(0,w.light(33,64,32));
    }
}
