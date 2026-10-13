package dev.skyblock.client;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
/** Local user-authored packs only. No remote downloads or original game artwork. */
public final class TexturePacks {
    public record Pack(String id,String name,String description,BufferedImage atlas) {}
    private final Path directory;
    public TexturePacks(Path directory) throws IOException { this.directory=directory; Files.createDirectories(directory); }
    public Path directory() { return directory; }
    public List<Pack> scan() throws IOException {
        List<Pack> result=new ArrayList<>(); result.add(new Pack("default","Default","Original generated textures",null));
        try(var files=Files.list(directory)) {
            for(Path file:files.sorted().limit(100).toList()) {
                try {
                    BufferedImage image; String description="Custom local atlas";
                    if(Files.isDirectory(file,LinkOption.NOFOLLOW_LINKS)) { if(!Files.isRegularFile(file.resolve("atlas.png"))) continue; try(InputStream in=Files.newInputStream(file.resolve("atlas.png"))) { image=readImage(in.readNBytes(1048577)); } if(Files.isRegularFile(file.resolve("pack.txt"))) try(InputStream in=Files.newInputStream(file.resolve("pack.txt"))) { description=new String(in.readNBytes(512),java.nio.charset.StandardCharsets.UTF_8).lines().findFirst().orElse(description); } }
                    else if(file.toString().endsWith(".zip")) {
                        try(ZipFile zip=new ZipFile(file.toFile())) { ZipEntry atlas=zip.getEntry("atlas.png"); if(atlas==null) throw new IOException("Missing atlas.png (128x128)"); try(InputStream in=zip.getInputStream(atlas)) { image=readImage(in.readNBytes(1048577)); } ZipEntry info=zip.getEntry("pack.txt"); if(info!=null) try(InputStream in=zip.getInputStream(info)) { description=new String(in.readNBytes(512),java.nio.charset.StandardCharsets.UTF_8).lines().findFirst().orElse(description); } }
                    } else continue;
                    result.add(new Pack(file.getFileName().toString(),file.getFileName().toString(),description,image));
                } catch(IOException | IllegalArgumentException e) { result.add(new Pack(file.getFileName().toString(),file.getFileName().toString(),"Invalid pack: "+e.getMessage(),null)); }
            }
        }
        return result;
    }
    public static BufferedImage readImage(byte[] data) throws IOException {
        if(data.length>1048576) throw new IOException("Atlas exceeds 1 MB");
        try(var in=ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            var readers=ImageIO.getImageReaders(in); if(!readers.hasNext()) throw new IOException("Not an image"); var reader=readers.next();
            try { reader.setInput(in); if(reader.getWidth(0)!=128 || reader.getHeight(0)!=128) throw new IOException("atlas.png must be 128x128"); return reader.read(0); } finally { reader.dispose(); }
        }
    }
}
