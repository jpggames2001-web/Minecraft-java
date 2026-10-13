package dev.skyblock.persistence;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Properties;
public final class AtomicProperties {
    private AtomicProperties() {}
    public static Properties read(Path path) throws IOException {
        Properties p=new Properties();
        if(Files.exists(path)) try(Reader r=Files.newBufferedReader(path,StandardCharsets.UTF_8)) { p.load(r); }
        catch(IllegalArgumentException e) { throw new IOException("Malformed properties: "+path,e); }
        return p;
    }
    public static void write(Path path,Properties p) throws IOException {
        path=path.toAbsolutePath(); Files.createDirectories(path.getParent());
        Path temporary=Files.createTempFile(path.getParent(),"settings-",".tmp");
        try {
            try(Writer w=Files.newBufferedWriter(temporary,StandardCharsets.UTF_8)) { p.store(w,"Skyblock Classic"); }
            try { Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
            catch(AtomicMoveNotSupportedException e) { Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
