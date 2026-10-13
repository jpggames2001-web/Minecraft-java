package dev.skyblock.persistence;
import dev.skyblock.network.ServerAddress;
import java.nio.file.Path;
import java.io.IOException;
import java.util.*;
public final class ServerLibrary {
    public record Entry(String id,String name,String address) {}
    private final Path file;
    private List<Entry> entries;
    public ServerLibrary(Path file) throws IOException {
        this.file=file; Properties p=AtomicProperties.read(file); entries=new ArrayList<>();
        int count=Integer.parseInt(p.getProperty("count","0")); if(count<0 || count>1000) throw new IOException("Invalid server count");
        for(int i=0;i<count;i++) { String prefix="server."+i+"."; entries.add(new Entry(p.getProperty(prefix+"id",UUID.randomUUID().toString()),p.getProperty(prefix+"name","Server"),p.getProperty(prefix+"address","localhost"))); }
    }
    public List<Entry> list() { return List.copyOf(entries); }
    public void put(Entry existing,String name,String address) throws IOException {
        ServerAddress.parse(address); if(name.isBlank()) throw new IllegalArgumentException("Server name cannot be empty");
        List<Entry> next=new ArrayList<>(entries); Entry e=new Entry(existing==null?UUID.randomUUID().toString():existing.id,name.strip(),address.strip());
        if(existing==null) next.add(e); else { int index=next.indexOf(existing); if(index<0) throw new IllegalArgumentException("Server no longer exists"); next.set(index,e); }
        save(next);
    }
    public void delete(Entry e) throws IOException { List<Entry> next=new ArrayList<>(entries); next.remove(e); save(next); }
    private void save(List<Entry> next) throws IOException {
        Properties p=new Properties(); p.setProperty("count",""+next.size());
        for(int i=0;i<next.size();i++) { String key="server."+i+"."; Entry e=next.get(i); p.setProperty(key+"id",e.id); p.setProperty(key+"name",e.name); p.setProperty(key+"address",e.address); }
        AtomicProperties.write(file,p); entries=next;
    }
}
