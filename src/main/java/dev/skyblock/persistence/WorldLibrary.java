package dev.skyblock.persistence;
import dev.skyblock.world.*;
import dev.skyblock.game.*;
import dev.skyblock.audio.SynthAudio;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
public final class WorldLibrary {
    public enum Type { DEFAULT, SKYBLOCK }
    public record Entry(String id,String name,long seed,Type type,Path save,long modified) {}
    private final Path root,legacy;
    public WorldLibrary(Path dataDirectory,Path legacy) throws IOException { root=dataDirectory.resolve("worlds").toAbsolutePath(); this.legacy=legacy.toAbsolutePath(); Files.createDirectories(root); }
    public List<Entry> list() throws IOException {
        List<Entry> result=new ArrayList<>();
        if(Files.isRegularFile(legacy)) { Properties p=AtomicProperties.read(root.resolve("legacy.properties")); result.add(new Entry("legacy",p.getProperty("name","Skyblock"),0,Type.SKYBLOCK,legacy,Files.getLastModifiedTime(legacy).toMillis())); }
        try(var directories=Files.list(root)) {
            for(Path dir:directories.filter(p->Files.isDirectory(p,LinkOption.NOFOLLOW_LINKS)).toList()) {
                try { UUID.fromString(dir.getFileName().toString()); } catch(IllegalArgumentException e) { continue; }
                if(Files.isRegularFile(dir.resolve("world.dat"))) {
                    Properties p=AtomicProperties.read(dir.resolve("world.properties"));
                    result.add(new Entry(dir.getFileName().toString(),p.getProperty("name","World"),Long.parseLong(p.getProperty("seed","0")),Type.valueOf(p.getProperty("type","SKYBLOCK")),dir.resolve("world.dat"),Files.getLastModifiedTime(dir.resolve("world.dat")).toMillis()));
                }
            }
        }
        result.sort(Comparator.comparingLong(Entry::modified).reversed()); return result;
    }
    public Entry create(String name,String seedText,Type type) throws IOException {
        if(name==null || name.isBlank()) throw new IllegalArgumentException("World name cannot be empty");
        long seed; try { seed=seedText.isBlank()?new Random().nextLong():Long.parseLong(seedText); } catch(NumberFormatException e) { seed=seedText.hashCode(); }
        String id=UUID.randomUUID().toString(); Path dir=root.resolve(id); Files.createDirectory(dir);
        try {
            World w=type==Type.SKYBLOCK?World.skyblock():World.defaultTerrain(seed);
            if(type==Type.SKYBLOCK) w.randomState=seed==0?1:seed;
            Player p=new Player(); if(type==Type.DEFAULT) { int top=World.HEIGHT-1; while(top>0 && !w.get((int)p.x,top,(int)p.z).solid) top--; p.y=top+1; p.prevY=p.y; }
            try(SynthAudio silent=new SynthAudio(false)) { SaveStore.save(dir.resolve("world.dat"),new Game(w,p,silent)); }
            Properties metadata=new Properties(); metadata.setProperty("name",name.strip()); metadata.setProperty("seed",""+seed); metadata.setProperty("type",type.name()); AtomicProperties.write(dir.resolve("world.properties"),metadata);
            return new Entry(id,name.strip(),seed,type,dir.resolve("world.dat"),System.currentTimeMillis());
        } catch(IOException | RuntimeException e) { Files.deleteIfExists(dir.resolve("world.dat")); Files.deleteIfExists(dir.resolve("world.properties")); Files.deleteIfExists(dir); throw e; }
    }
    private Path metadata(Entry e) { if(e.id.equals("legacy")) return root.resolve("legacy.properties"); UUID.fromString(e.id); return root.resolve(e.id).resolve("world.properties"); }
    public void rename(Entry entry,String name) throws IOException { if(name.isBlank()) throw new IllegalArgumentException("Empty world name"); Path file=metadata(entry); Properties p=AtomicProperties.read(file); p.setProperty("name",name.strip()); AtomicProperties.write(file,p); }
    public void delete(Entry entry) throws IOException {
        if(entry.id.equals("legacy")) { Files.deleteIfExists(legacy); Files.deleteIfExists(metadata(entry)); return; }
        Path dir=metadata(entry).getParent(); Files.deleteIfExists(dir.resolve("world.dat")); Files.deleteIfExists(dir.resolve("world.properties")); try { Files.delete(dir); } catch(DirectoryNotEmptyException ignored) { /* Preserve unrelated user files in a removed world folder. */ }
    }
}
