package dev.skyblock;

import dev.skyblock.game.*;
import dev.skyblock.persistence.SaveStore;
import dev.skyblock.audio.SynthAudio;
import dev.skyblock.world.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class SaveTest {
    @TempDir Path directory;
    @Test void roundTripPreservesWorldPlayerChestFurnaceAndDrops() throws Exception {
        try(SynthAudio audio=new SynthAudio(false)) {
            Game g=new Game(World.skyblock(),new Player(),audio);
            g.player.physics=PhysicsProfile.RELEASE_REFERENCE; g.player.x=30.25; g.player.inventory.set(3,new Stack(Item.WOOD_PICKAXE,1,17));
            g.world.set(30,65,30,Block.FURNACE); Container c=g.world.containers.get(World.index(30,65,30));
            c.inventory.set(0,new Stack(Item.SAND,4)); c.inventory.set(1,new Stack(Item.COAL,1));
            for(int i=0;i<31;i++) g.world.tick();
            g.spawnDrop(new Stack(Item.LOG,3),30,65,30);
            Path path=directory.resolve("world.dat"); SaveStore.save(path,g); SaveStore.Saved loaded=SaveStore.load(path);
            assertArrayEquals(g.world.blockSnapshot(),loaded.world().blockSnapshot()); assertArrayEquals(g.world.dataSnapshot(),loaded.world().dataSnapshot()); assertArrayEquals(g.world.biomeSnapshot(),loaded.world().biomeSnapshot());
            assertEquals(g.world.time,loaded.world().time); assertEquals(g.world.randomState,loaded.world().randomState); assertEquals(g.world.pendingTicks(),loaded.world().pendingTicks());
            assertEquals(g.player.x,loaded.player().x); assertEquals(g.player.physics,loaded.player().physics); assertEquals(g.player.inventory.get(3),loaded.player().inventory.get(3));
            assertEquals(c.progress,loaded.world().containers.get(World.index(30,65,30)).progress); assertEquals(3,loaded.drops().getFirst().stack.count());
        }
    }
    @Test void reloadContinuesScheduledFluidEvolutionDeterministically() throws Exception {
        try(SynthAudio audio=new SynthAudio(false)) {
            Game g=new Game(new World(),new Player(),audio); g.world.set(32,64,32,Block.WATER); for(int i=0;i<3;i++) g.world.tick();
            Path path=directory.resolve("water.dat"); SaveStore.save(path,g); World loaded=SaveStore.load(path).world();
            for(int i=0;i<12;i++) { g.world.tick(); loaded.tick(); }
            assertArrayEquals(g.world.blockSnapshot(),loaded.blockSnapshot()); assertArrayEquals(g.world.dataSnapshot(),loaded.dataSnapshot()); assertEquals(g.world.randomState,loaded.randomState);
        }
    }
    @Test void damagedSaveIsRejectedWithoutChangingTheFile() throws Exception {
        Path path=directory.resolve("broken.dat"); byte[] bad={1,2,3,4}; Files.write(path,bad); assertThrows(java.io.IOException.class,()->SaveStore.load(path)); assertArrayEquals(bad,Files.readAllBytes(path));
    }
    @Test void repeatedAtomicSaveLeavesNoTemporaryFiles() throws Exception {
        try(SynthAudio audio=new SynthAudio(false)) { Game g=new Game(World.skyblock(),new Player(),audio); Path p=directory.resolve("world.dat"); SaveStore.save(p,g); g.player.food=10; SaveStore.save(p,g); assertEquals(10,SaveStore.load(p).player().food); }
        try(var files=Files.list(directory)) { assertEquals(1,files.count()); }
    }
}
