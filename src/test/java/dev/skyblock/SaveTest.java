package dev.skyblock;

import dev.skyblock.game.*;
import dev.skyblock.persistence.SaveStore;
import dev.skyblock.audio.SynthAudio;
import dev.skyblock.world.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.nio.ByteBuffer;
import java.io.*;
import java.util.zip.*;
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
            assertEquals(g.player.yaw,loaded.player().yaw);
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
    @Test void legacySaveMigratesYawOnceWithoutChangingViewOrWorldVelocity() throws Exception {
        Path path=directory.resolve("legacy.dat");
        try(SynthAudio audio=new SynthAudio(false)) {
            Player player=new Player(); player.vx=.2; player.vy=-.08; player.vz=-.3; player.pitch=23;
            SaveStore.save(path,new Game(new World(),player,audio));
            byte[] data=uncompressed(path); ByteBuffer bytes=ByteBuffer.wrap(data); assertEquals(2,bytes.getInt(4));
            int scheduledOffset=24+World.VOLUME*2+World.SIZE*World.SIZE;
            int yawOffset=scheduledOffset+4+12*bytes.getInt(scheduledOffset)+48;
            bytes.putInt(4,1); bytes.putFloat(yawOffset,-53.25f); writeCompressed(path,data);
            byte[] original=Files.readAllBytes(path);
            SaveStore.Saved loaded=SaveStore.load(path); assertEquals(53.25f,loaded.player().yaw);
            double a=Math.toRadians(-53.25),p=Math.toRadians(23);
            assertArrayEquals(new double[]{-Math.sin(a)*Math.cos(p),-Math.sin(p),-Math.cos(a)*Math.cos(p)},loaded.player().direction(),1e-12);
            assertEquals(.2,loaded.player().vx); assertEquals(-.08,loaded.player().vy); assertEquals(-.3,loaded.player().vz);
            assertArrayEquals(original,Files.readAllBytes(path),"Loading must not rewrite the legacy file");
            Path upgraded=directory.resolve("upgraded.dat"); SaveStore.save(upgraded,new Game(loaded.world(),loaded.player(),audio));
            assertEquals(2,ByteBuffer.wrap(uncompressed(upgraded)).getInt(4));
            assertEquals(53.25f,SaveStore.load(upgraded).player().yaw,"The upgraded format must not migrate yaw twice");
        }
    }
    @Test void unsupportedFutureSaveVersionIsRejected() throws Exception {
        Path path=directory.resolve("future.dat");
        try(SynthAudio audio=new SynthAudio(false)) { SaveStore.save(path,new Game(new World(),new Player(),audio)); }
        byte[] data=uncompressed(path); ByteBuffer.wrap(data).putInt(4,3); writeCompressed(path,data);
        assertThrows(IOException.class,()->SaveStore.load(path));
    }
    private byte[] uncompressed(Path path) throws IOException {
        try(GZIPInputStream in=new GZIPInputStream(Files.newInputStream(path))) { return in.readAllBytes(); }
    }
    private void writeCompressed(Path path,byte[] data) throws IOException {
        try(GZIPOutputStream out=new GZIPOutputStream(Files.newOutputStream(path))) { out.write(data); }
    }
}
