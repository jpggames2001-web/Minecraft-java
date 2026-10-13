package dev.skyblock;
import dev.skyblock.client.*;
import dev.skyblock.persistence.*;
import dev.skyblock.network.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.net.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;
class ClientPersistenceTest {
    @TempDir Path data;
    @Test void namedWorldsUseUniqueFoldersAndPreserveSeedAcrossRename() throws Exception { WorldLibrary library=new WorldLibrary(data,data.resolve("legacy.dat")); var one=library.create("same","123",WorldLibrary.Type.SKYBLOCK); var two=library.create("same","abc",WorldLibrary.Type.DEFAULT); assertNotEquals(one.id(),two.id()); library.rename(one,"../display name"); var reloaded=new WorldLibrary(data,data.resolve("legacy.dat")).list(); assertEquals(2,reloaded.size()); assertTrue(reloaded.stream().anyMatch(e->e.id().equals(one.id()) && e.seed()==123)); assertEquals("abc".hashCode(),two.seed()); library.delete(one); assertEquals(1,library.list().size()); assertTrue(Files.exists(two.save())); }
    @Test void defaultTerrainIsPlayableAndSeedDependent() throws Exception { WorldLibrary library=new WorldLibrary(data,data.resolve("legacy.dat")); var a=library.create("Default","1",WorldLibrary.Type.DEFAULT); var b=library.create("Default","2",WorldLibrary.Type.DEFAULT); var loaded=SaveStore.load(a.save()); assertFalse(loaded.world().collisions(loaded.player().box()).stream().anyMatch(box->box.intersects(loaded.player().box()))); assertNotEquals(loaded.world().randomState,SaveStore.load(b.save()).world().randomState); }
    @Test void serverLibraryRetainsIdentityOnEditAndSurvivesRestart() throws Exception { Path p=data.resolve("servers.properties"); ServerLibrary s=new ServerLibrary(p); s.put(null,"A","localhost:25565"); String id=s.list().getFirst().id(); s.put(s.list().getFirst(),"B","[::1]:25566"); var e=new ServerLibrary(p).list().getFirst(); assertEquals(id,e.id()); assertEquals("B",e.name()); s.delete(e); assertTrue(new ServerLibrary(p).list().isEmpty()); }
    @Test void serverAddressValidationSupportsIpv6AndDefaultPort() { assertEquals(25565,ServerAddress.parse("localhost").port()); assertEquals("::1",ServerAddress.parse("[::1]:25566").host()); for(String bad:new String[]{"","bad host","localhost:0","localhost:65536","[broken"}) assertThrows(IllegalArgumentException.class,()->ServerAddress.parse(bad)); }
    @Test void legacyServerProbeReadsTheActualWireResponse() throws Exception {
        try(ServerSocket server=new ServerSocket(0,1,InetAddress.getLoopbackAddress())) {
            Thread response=new Thread(()-> { try(Socket socket=server.accept()) { assertEquals(0xfe,socket.getInputStream().read()); DataOutputStream out=new DataOutputStream(socket.getOutputStream()); String text="Test MOTD\u00a73\u00a720"; out.writeByte(0xff); out.writeShort(text.length()); out.write(text.getBytes(java.nio.charset.StandardCharsets.UTF_16BE)); out.flush(); } catch(IOException e) { throw new RuntimeException(e); } }); response.start();
            try(LegacyServerProbe probe=new LegacyServerProbe(new ServerAddress("127.0.0.1",server.getLocalPort()))) { LegacyServerProbe.Result result=null; long deadline=System.nanoTime()+5_000_000_000L; while(result==null && System.nanoTime()<deadline) { result=probe.result(); Thread.sleep(5); } assertNotNull(result); assertTrue(result.reachable()); assertEquals("Test MOTD",result.message()); assertEquals(3,result.online()); assertEquals(20,result.maximum()); }
            response.join(1000);
        }
    }
    @Test void texturePacksLoadLocalDirectoriesAndArchivesWithoutExtraction() throws Exception {
        Path root=data.resolve("packs"); TexturePacks packs=new TexturePacks(root); Path dir=root.resolve("Custom"); Files.createDirectory(dir); BufferedImage image=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB); image.setRGB(0,0,0xff123456); ImageIO.write(image,"png",dir.resolve("atlas.png").toFile());
        try(ZipOutputStream zip=new ZipOutputStream(Files.newOutputStream(root.resolve("Zip.zip")))) { zip.putNextEntry(new ZipEntry("atlas.png")); zip.write(Files.readAllBytes(dir.resolve("atlas.png"))); zip.closeEntry(); zip.putNextEntry(new ZipEntry("../escape.txt")); zip.write(1); zip.closeEntry(); }
        assertEquals(3,packs.scan().size()); assertEquals(0xff123456,packs.scan().get(1).atlas().getRGB(0,0)); assertFalse(Files.exists(data.resolve("escape.txt")));
    }
    @Test void invalidTextureDimensionsAreReportedWithoutSwitching() throws Exception { TexturePacks packs=new TexturePacks(data); Path dir=data.resolve("Bad"); Files.createDirectory(dir); ImageIO.write(new BufferedImage(16,16,BufferedImage.TYPE_INT_RGB),"png",dir.resolve("atlas.png").toFile()); var pack=packs.scan().get(1); assertNull(pack.atlas()); assertTrue(pack.description().startsWith("Invalid pack")); }
    @Test void removingAWorldPreservesUnrelatedFiles() throws Exception { WorldLibrary library=new WorldLibrary(data,data.resolve("legacy.dat")); var world=library.create("World","1",WorldLibrary.Type.SKYBLOCK); Path note=world.save().getParent().resolve("user-note.txt"); Files.writeString(note,"Keep me"); library.delete(world); assertEquals("Keep me",Files.readString(note)); assertTrue(library.list().isEmpty()); }
    @Test void bindingNamesShowSpaceAndMouseButtons() { assertEquals("Space",Binding.keyName(32)); assertEquals("Mouse 1",Binding.keyName(-100)); assertEquals("Unbound",Binding.keyName(0)); }
}
