package dev.skyblock.persistence;

import dev.skyblock.game.*;
import dev.skyblock.game.Stack;
import dev.skyblock.world.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public final class SaveStore {
    private static final int MAGIC=0x534B4231, VERSION=2;
    public record Saved(World world,Player player,List<Drop> drops) {}
    private SaveStore() {}
    public static void save(Path path,Game game) throws IOException {
        Path absolute=path.toAbsolutePath(); Files.createDirectories(absolute.getParent());
        Path temp=Files.createTempFile(absolute.getParent(),"world-",".tmp");
        try {
            try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(temp))))) {
                out.writeInt(MAGIC); out.writeInt(VERSION); out.writeLong(game.world.time); out.writeLong(game.world.randomState);
                out.write(game.world.blockSnapshot()); out.write(game.world.dataSnapshot()); out.write(game.world.biomeSnapshot());
                out.writeInt(game.world.pendingTicks().size());
                for(World.PendingTick tick:game.world.pendingTicks()) { out.writeInt(tick.index()); out.writeLong(tick.due()); }
                Player p=game.player;
                for(double n:new double[]{p.x,p.y,p.z,p.vx,p.vy,p.vz}) out.writeDouble(n);
                out.writeFloat(p.yaw); out.writeFloat(p.pitch); out.writeBoolean(p.onGround); out.writeBoolean(p.creative); out.writeInt(p.physics.ordinal());
                out.writeInt(p.health); out.writeInt(p.food); out.writeInt(p.air); out.writeInt(p.hurtTicks); out.writeInt(p.selected); out.writeInt(p.foodTimer);
                out.writeFloat(p.saturation); out.writeFloat(p.exhaustion); out.writeFloat(p.fallDistance);
                writeInventory(out,p.inventory);
                out.writeInt(game.world.containers.size());
                for(var entry:new TreeMap<>(game.world.containers).entrySet()) {
                    Container c=entry.getValue(); out.writeInt(entry.getKey()); out.writeBoolean(c.furnace);
                    out.writeInt(c.burn); out.writeInt(c.burnTotal); out.writeInt(c.progress); writeInventory(out,c.inventory);
                }
                out.writeInt(game.drops.size());
                for(Drop d:game.drops) {
                    writeStack(out,d.stack); for(double v:new double[]{d.x,d.y,d.z,d.vx,d.vy,d.vz}) out.writeDouble(v);
                    out.writeInt(d.age); out.writeInt(d.pickupDelay);
                }
            }
            try { Files.move(temp,absolute,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch(AtomicMoveNotSupportedException e) { Files.move(temp,absolute,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
    public static Saved load(Path path) throws IOException {
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(path))))) {
            int magic=in.readInt(),version=in.readInt();
            if(magic!=MAGIC || version<1 || version>VERSION) throw new IOException("Unsupported save format");
            World w=new World(); w.time=in.readLong(); w.randomState=in.readLong();
            if(w.time<0 || w.time>Long.MAX_VALUE-100000 || w.randomState==0) throw new IOException("Invalid world clock or random state");
            w.restore(in.readNBytes(World.VOLUME),in.readNBytes(World.VOLUME),in.readNBytes(World.SIZE*World.SIZE));
            int scheduled=range(in,0,World.VOLUME); List<World.PendingTick> ticks=new ArrayList<>();
            for(int i=0;i<scheduled;i++) ticks.add(new World.PendingTick(in.readInt(),in.readLong()));
            w.restoreTicks(ticks);
            Player p=new Player();
            p.x=coordinate(in); p.y=coordinate(in); p.z=coordinate(in); p.vx=coordinate(in); p.vy=coordinate(in); p.vz=coordinate(in);
            p.prevX=p.x; p.prevY=p.y; p.prevZ=p.z;
            p.yaw=finite(in.readFloat()); p.pitch=finite(in.readFloat()); p.onGround=in.readBoolean(); p.creative=in.readBoolean(); p.physics=PhysicsProfile.values()[range(in,0,PhysicsProfile.values().length-1)];
            if(version==1) p.yaw=-p.yaw; // Preserve the view direction of saves using the old yaw convention.
            p.health=range(in,0,20); p.food=range(in,0,20); p.air=range(in,-20,300); p.hurtTicks=range(in,0,20); p.selected=range(in,0,8); p.foodTimer=range(in,0,80);
            p.saturation=finite(in.readFloat()); p.exhaustion=finite(in.readFloat()); p.fallDistance=finite(in.readFloat());
            if(p.saturation<0 || p.saturation>20 || p.exhaustion<0 || p.exhaustion>40 || p.fallDistance<0) throw new IOException("Invalid player state");
            readInventory(in,p.inventory);
            int count=range(in,0,World.VOLUME);
            Set<Integer> seen=new HashSet<>();
            for(int i=0;i<count;i++) {
                int key=range(in,0,World.VOLUME-1); boolean furnace=in.readBoolean();
                Container c=w.containers.get(key);
                if(c==null || c.furnace!=furnace || !seen.add(key)) throw new IOException("Invalid container location");
                c.burn=range(in,0,20000); c.burnTotal=range(in,0,20000); c.progress=range(in,0,199); readInventory(in,c.inventory);
            }
            if(count!=w.containers.size()) throw new IOException("Missing container data");
            count=range(in,0,8192); List<Drop> drops=new ArrayList<>();
            for(int i=0;i<count;i++) {
                Stack stack=readStack(in); if(stack==null) throw new IOException("Empty item entity");
                Drop d=new Drop(stack,coordinate(in),coordinate(in),coordinate(in));
                d.vx=coordinate(in); d.vy=coordinate(in); d.vz=coordinate(in); d.age=range(in,0,6000); d.pickupDelay=range(in,0,1000); drops.add(d);
            }
            if(in.read()!=-1) throw new IOException("Unexpected trailing save data");
            w.invalidateLighting(); w.updateLighting();
            return new Saved(w,p,drops);
        } catch(IllegalArgumentException | IndexOutOfBoundsException e) { throw new IOException("Invalid save contents",e); }
    }
    private static float finite(float n) throws IOException { if(!Float.isFinite(n)) throw new IOException("Non-finite value"); return n; }
    private static double coordinate(DataInputStream in) throws IOException { double n=in.readDouble(); if(!Double.isFinite(n) || Math.abs(n)>1_000_000) throw new IOException("Invalid coordinate"); return n; }
    private static int range(DataInputStream in,int min,int max) throws IOException { int n=in.readInt(); if(n<min || n>max) throw new IOException("Out-of-range save value"); return n; }
    private static void writeInventory(DataOutputStream out,Inventory inventory) throws IOException { out.writeInt(inventory.size()); for(int i=0;i<inventory.size();i++) writeStack(out,inventory.get(i)); }
    private static void readInventory(DataInputStream in,Inventory inventory) throws IOException { if(in.readInt()!=inventory.size()) throw new IOException("Inventory size mismatch"); for(int i=0;i<inventory.size();i++) inventory.set(i,readStack(in)); }
    private static void writeStack(DataOutputStream out,Stack stack) throws IOException { out.writeInt(stack==null?-1:stack.item().ordinal()); if(stack!=null) { out.writeInt(stack.count()); out.writeInt(stack.damage()); } }
    private static Stack readStack(DataInputStream in) throws IOException { int id=in.readInt(); if(id==-1) return null; if(id<0 || id>=Item.values().length) throw new IOException("Invalid item"); return new Stack(Item.values()[id],in.readInt(),in.readInt()); }
}
