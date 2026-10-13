package dev.skyblock.persistence;

import dev.skyblock.game.MouseSettings;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Properties;

/** User-wide controls are saved independently of worlds and survive changing save files. */
public final class MouseSettingsStore {
    private final Path path;
    private MouseSettings value;

    private MouseSettingsStore(Path path,MouseSettings value) { this.path=path.toAbsolutePath(); this.value=value; }
    public static MouseSettingsStore load(Path path) throws IOException {
        Properties properties=new Properties();
        if(Files.exists(path)) {
            try(Reader reader=Files.newBufferedReader(path,StandardCharsets.UTF_8)) { properties.load(reader); }
            catch(IllegalArgumentException e) { throw new IOException("Malformed controls settings: "+path,e); }
        }
        double sensitivity=MouseSettings.DEFAULT.sensitivity();
        try {
            double requested=Double.parseDouble(properties.getProperty("mouse.sensitivity",""+sensitivity));
            if(Double.isFinite(requested) && requested>=MouseSettings.MIN_SENSITIVITY && requested<=MouseSettings.MAX_SENSITIVITY) sensitivity=requested;
        } catch(NumberFormatException ignored) { /* Keep the usable default for an invalid individual field. */ }
        return new MouseSettingsStore(path,new MouseSettings(Boolean.parseBoolean(properties.getProperty("mouse.invertX","false")),Boolean.parseBoolean(properties.getProperty("mouse.invertY","false")),sensitivity));
    }
    public MouseSettings value() { return value; }
    public void save(MouseSettings next) throws IOException {
        Files.createDirectories(path.getParent());
        Path temp=Files.createTempFile(path.getParent(),"controls-",".tmp");
        try {
            Properties properties=new Properties();
            properties.setProperty("mouse.invertX",Boolean.toString(next.invertX()));
            properties.setProperty("mouse.invertY",Boolean.toString(next.invertY()));
            properties.setProperty("mouse.sensitivity",Double.toString(next.sensitivity()));
            try(Writer writer=Files.newBufferedWriter(temp,StandardCharsets.UTF_8)) { properties.store(writer,"Skyblock Classic mouse controls"); }
            try { Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch(AtomicMoveNotSupportedException e) { Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING); }
            value=next;
        } finally { Files.deleteIfExists(temp); }
    }
}
