package dev.skyblock.world;

import dev.skyblock.game.*;
import dev.skyblock.game.Stack;
import java.util.*;

/** A finite 4x4-chunk, 128-block-high world with scheduled and random block ticks. */
public final class World {
    public static final int SIZE=64, HEIGHT=128, VOLUME=SIZE*SIZE*HEIGHT;
    private final byte[] blocks=new byte[VOLUME], data=new byte[VOLUME], sky=new byte[VOLUME], light=new byte[VOLUME];
    private final byte[] biomes=new byte[SIZE*SIZE];
    public final Map<Integer,Container> containers=new HashMap<>();
    public long time, randomState=0x51b10c1234L, revision;
    private boolean lightingDirty=true;
    private final PriorityQueue<Scheduled> scheduled=new PriorityQueue<>(Comparator.comparingLong(Scheduled::due).thenComparingInt(Scheduled::index));
    private final Map<Integer,Long> scheduledTimes=new HashMap<>();
    private record Scheduled(int index,long due) {}
    public static boolean inside(int x,int y,int z) { return x>=0 && x<SIZE && z>=0 && z<SIZE && y>=0 && y<HEIGHT; }
    public static int index(int x,int y,int z) { return (y*SIZE+z)*SIZE+x; }
    public static int x(int i) { return i%SIZE; }
    public static int y(int i) { return i/(SIZE*SIZE); }
    public static int z(int i) { return i/SIZE%SIZE; }
    public Block get(int x,int y,int z) { return inside(x,y,z) ? Block.byId(Byte.toUnsignedInt(blocks[index(x,y,z)])) : Block.AIR; }
    public int data(int x,int y,int z) { return inside(x,y,z) ? Byte.toUnsignedInt(data[index(x,y,z)]) : 0; }
    public int sky(int x,int y,int z) { return inside(x,y,z) ? sky[index(x,y,z)] : 15; }
    public int light(int x,int y,int z) { return inside(x,y,z) ? light[index(x,y,z)] : 0; }
    public Biome biome(int x,int z) { return Biome.byId(biomes[Math.clamp(z,0,SIZE-1)*SIZE+Math.clamp(x,0,SIZE-1)]); }
    public void set(int x,int y,int z,Block block) { set(x,y,z,block,0); }
    public void set(int x,int y,int z,Block block,int value) {
        if(!inside(x,y,z)) return;
        if(value<0 || value>15) throw new IllegalArgumentException("Block metadata out of range");
        int i=index(x,y,z);
        if(blocks[i]==block.id && data[i]==value) return;
        Block before=get(x,y,z); blocks[i]=(byte)block.id; data[i]=(byte)value;
        if(before!=block) containers.remove(i);
        if(block==Block.CHEST || block==Block.FURNACE) containers.computeIfAbsent(i,k->new Container(block==Block.FURNACE));
        revision++; lightingDirty=true;
        schedule(x,y,z);
        for(int[] d:DIRECTIONS) schedule(x+d[0],y+d[1],z+d[2]);
    }
    private void schedule(int x,int y,int z) {
        if(!inside(x,y,z)) return;
        Block b=get(x,y,z);
        if(!b.fluid() && b!=Block.SAND && !b.plant() && b!=Block.CACTUS) return;
        int i=index(x,y,z); long due=time+(b==Block.LAVA?30:b==Block.WATER?5:1);
        Long old=scheduledTimes.get(i);
        if(old==null || due<old) { scheduledTimes.put(i,due); scheduled.add(new Scheduled(i,due)); }
    }
    public static final int[][] DIRECTIONS={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    private static final int[][] HORIZONTAL={{1,0,0},{-1,0,0},{0,0,1},{0,0,-1}};
    public static World skyblock() {
        World w=new World();
        for(int z=0;z<SIZE;z++) for(int x=0;x<SIZE;x++) w.biomes[z*SIZE+x]=(byte)(x>=48?Biome.DESERT.ordinal():z>=48?Biome.TAIGA.ordinal():Biome.FOREST.ordinal());
        for(int z=29;z<=35;z++) for(int x=29;x<=35;x++) {
            if(x>=33 && z<=31) continue;
            for(int y=62;y<=64;y++) w.set(x,y,z,y==64?Block.GRASS:Block.DIRT);
        }
        w.set(32,62,32,Block.BEDROCK);
        w.growTree(32,65,34);
        w.set(29,65,31,Block.CHEST);
        Inventory chest=w.containers.get(index(29,65,31)).inventory;
        chest.set(0,new Stack(Item.ICE,2)); chest.set(1,new Stack(Item.LAVA_BUCKET,1));
        chest.set(2,new Stack(Item.SAPLING,2)); chest.set(3,new Stack(Item.SEEDS,3));
        chest.set(4,new Stack(Item.BONE,3)); chest.set(5,new Stack(Item.APPLE,3));
        for(int z=30;z<=32;z++) for(int x=50;x<=52;x++) { w.set(x,62,z,Block.SAND); w.set(x,61,z,Block.SAND); w.set(x,60,z,Block.SAND); }
        w.set(51,59,31,Block.BEDROCK); w.set(51,63,31,Block.CACTUS);
        for(int z=50;z<=52;z++) for(int x=29;x<=31;x++) { w.set(x,63,z,Block.DIRT); w.set(x,64,z,Block.SNOW); }
        w.set(30,65,51,Block.ICE);
        w.updateLighting(); return w;
    }
    /** Finite development terrain for the requested Default menu choice, not the 1.0 generator. */
    public static World defaultTerrain(long seed) {
        World w=new World(); w.randomState=seed==0?1:seed;
        for(int z=0;z<SIZE;z++) for(int x=0;x<SIZE;x++) {
            w.biomes[z*SIZE+x]=(byte)Biome.PLAINS.ordinal();
            int top=64+(int)(Math.sin((x+(seed&255))*.15)*2+Math.cos((z+(seed>>>8&255))*.13)*2);
            w.set(x,0,z,Block.BEDROCK);
            for(int y=1;y<=top;y++) w.set(x,y,z,y==top?Block.GRASS:y>=top-3?Block.DIRT:Block.STONE);
        }
        for(int z=8;z<56;z+=12) for(int x=8;x<56;x+=12) { int y=127; while(y>0 && w.get(x,y,z)==Block.AIR) y--; w.growTree(x,y+1,z); }
        w.updateLighting(); return w;
    }
    public void tick() {
        time++;
        int budget=4096;
        while(budget-->0 && !scheduled.isEmpty() && scheduled.peek().due<=time) {
            Scheduled s=scheduled.poll();
            if(!Objects.equals(scheduledTimes.get(s.index),s.due)) continue;
            scheduledTimes.remove(s.index);
            updateBlock(x(s.index),y(s.index),z(s.index));
        }
        for(Container c:containers.values()) {
            boolean lit=c.burn>0; c.tick();
            if(c.furnace && lit!=(c.burn>0)) { lightingDirty=true; revision++; }
        }
        for(int chunkZ=0;chunkZ<4;chunkZ++) for(int chunkX=0;chunkX<4;chunkX++) for(int section=0;section<8;section++) for(int r=0;r<3;r++) {
            int n=randomInt(4096);
            randomTick(chunkX*16+(n&15),section*16+(n>>8&15),chunkZ*16+(n>>4&15));
        }
        updateLighting();
    }
    private void updateBlock(int x,int y,int z) {
        Block b=get(x,y,z);
        if(b.fluid()) { flow(x,y,z,b); return; }
        if(b==Block.SAND && y>0 && passable(get(x,y-1,z))) {
            int dest=y; while(dest>0 && passable(get(x,dest-1,z))) dest--;
            set(x,y,z,Block.AIR); if(dest>0) set(x,dest,z,b); return;
        }
        if(b.plant() && !get(x,y-1,z).solid) set(x,y,z,Block.AIR);
        if(b==Block.CACTUS) {
            boolean supported=get(x,y-1,z)==Block.SAND || get(x,y-1,z)==Block.CACTUS;
            for(int[] d:HORIZONTAL) if(get(x+d[0],y,z+d[2]).solid) supported=false;
            if(!supported) set(x,y,z,Block.AIR);
        }
    }
    private boolean passable(Block b) { return b==Block.AIR || b.fluid() || b.plant(); }
    private void flow(int x,int y,int z,Block fluid) {
        int level=data(x,y,z);
        if(fluid==Block.LAVA) {
            for(int[] d:DIRECTIONS) if(d[1]!=-1 && get(x+d[0],y+d[1],z+d[2])==Block.WATER) {
                set(x,y,z,level==0?Block.OBSIDIAN:Block.COBBLESTONE); return;
            }
        }
        if(level!=0) {
            int incoming=16, sources=0;
            for(int[] d:HORIZONTAL) if(get(x+d[0],y,z+d[2])==fluid) {
                int n=data(x+d[0],y,z+d[2]); if(n==0) sources++;
                incoming=Math.min(incoming,n>=8?0:n);
            }
            int next=get(x,y+1,z)==fluid ? 8 : incoming+(fluid==Block.LAVA?2:1);
            if(fluid==Block.WATER && sources>=2 && (get(x,y-1,z).solid || get(x,y-1,z)==fluid && data(x,y-1,z)==0)) next=0;
            if(next>=8 && next!=8 || next==8 && get(x,y+1,z)!=fluid) { set(x,y,z,Block.AIR); return; }
            if(next!=level) { set(x,y,z,fluid,next); level=next; }
        }
        if(y==0) return;
        Block below=get(x,y-1,z);
        if(fluid==Block.LAVA && below==Block.WATER) { set(x,y-1,z,Block.STONE); return; }
        if(canFlowInto(below,fluid)) { set(x,y-1,z,fluid,8); return; }
        if(below==fluid && data(x,y-1,z)>=8 && level!=0) return;
        int spread=(level>=8?0:level)+(fluid==Block.LAVA?2:1);
        if(spread>7) return;
        // Prefer the shortest nearby drop, retaining a bounded, deterministic search.
        int best=1000; int[] costs=new int[4];
        for(int i=0;i<4;i++) {
            int nx=x+HORIZONTAL[i][0], nz=z+HORIZONTAL[i][2];
            costs[i]=canSpread(nx,y,nz,fluid,spread) ? dropCost(nx,y,nz,fluid,0,x,z,new HashSet<>()) : 1001;
            best=Math.min(best,costs[i]);
        }
        for(int i=0;i<4;i++) if(costs[i]==best && costs[i]<=1000) {
            int nx=x+HORIZONTAL[i][0],nz=z+HORIZONTAL[i][2]; set(nx,y,nz,fluid,spread);
        }
    }
    private boolean canFlowInto(Block target,Block fluid) { return target==Block.AIR || target.plant() || target==Block.WATER && fluid==Block.LAVA; }
    private boolean canSpread(int x,int y,int z,Block fluid,int level) { return inside(x,y,z) && (canFlowInto(get(x,y,z),fluid) || get(x,y,z)==fluid && data(x,y,z)>level); }
    private int dropCost(int x,int y,int z,Block fluid,int depth,int fromX,int fromZ,Set<Integer> visited) {
        int i=index(x,y,z); if(!visited.add(i)) return 1000;
        if(canFlowInto(get(x,y-1,z),fluid)) return depth;
        if(depth==4) return 1000;
        int best=1000;
        for(int[] d:HORIZONTAL) {
            int nx=x+d[0],nz=z+d[2];
            if((nx!=fromX || nz!=fromZ) && inside(nx,y,nz) && canFlowInto(get(nx,y,nz),fluid))
                best=Math.min(best,dropCost(nx,y,nz,fluid,depth+1,x,z,visited));
        }
        return best;
    }
    private void randomTick(int x,int y,int z) {
        Block b=get(x,y,z);
        if(b==Block.SAPLING && sky(x,y+1,z)>=9 && randomInt(7)==0) {
            if(data(x,y,z)==0) set(x,y,z,b,1); else growTree(x,y,z);
        } else if(b==Block.WHEAT && data(x,y,z)<7 && sky(x,y+1,z)>=9 && randomInt(10)==0) set(x,y,z,b,data(x,y,z)+1);
        else if(b==Block.FARMLAND) {
            boolean water=false;
            for(int dz=-4;dz<=4;dz++) for(int dx=-4;dx<=4;dx++) for(int dy=0;dy<=1;dy++) water|=get(x+dx,y+dy,z+dz)==Block.WATER;
            if(water) set(x,y,z,b,7);
            else if(data(x,y,z)>0) set(x,y,z,b,data(x,y,z)-1);
            else if(get(x,y+1,z)!=Block.WHEAT) set(x,y,z,Block.DIRT);
        } else if(b==Block.GRASS && get(x,y+1,z).opaque) set(x,y,z,Block.DIRT);
        else if(b==Block.DIRT && sky(x,y+1,z)>=9 && !get(x,y+1,z).opaque) {
            for(int[] d:HORIZONTAL) if(get(x+d[0],y,z+d[2])==Block.GRASS && randomInt(4)==0) { set(x,y,z,Block.GRASS); break; }
        } else if(b==Block.LEAVES && randomInt(4)==0) {
            boolean log=false;
            for(int dz=-4;dz<=4;dz++) for(int dy=-4;dy<=4;dy++) for(int dx=-4;dx<=4;dx++) if(Math.abs(dx)+Math.abs(dy)+Math.abs(dz)<=4 && get(x+dx,y+dy,z+dz)==Block.LOG) log=true;
            if(!log) set(x,y,z,Block.AIR);
        }
    }
    public boolean growTree(int x,int y,int z) {
        if(y+6>=HEIGHT || get(x,y-1,z)!=Block.GRASS && get(x,y-1,z)!=Block.DIRT) return false;
        for(int dy=0;dy<6;dy++) for(int dz=-2;dz<=2;dz++) for(int dx=-2;dx<=2;dx++) {
            if(dy<3 && (dx!=0 || dz!=0)) continue;
            Block b=get(x+dx,y+dy,z+dz);
            if(!inside(x+dx,y+dy,z+dz) || b!=Block.AIR && b!=Block.SAPLING && b!=Block.LEAVES) return false;
        }
        set(x,y-1,z,Block.DIRT);
        for(int dy=3;dy<=5;dy++) {
            int radius=dy==5?1:2;
            for(int dz=-radius;dz<=radius;dz++) for(int dx=-radius;dx<=radius;dx++)
                if(!(Math.abs(dx)==radius && Math.abs(dz)==radius && (dy==5 || randomInt(2)==0))) set(x+dx,y+dy,z+dz,Block.LEAVES);
        }
        for(int dy=0;dy<5;dy++) set(x,y+dy,z,Block.LOG);
        return true;
    }
    public int randomInt(int bound) {
        if(bound<=0) throw new IllegalArgumentException("bound");
        randomState^=randomState<<13; randomState^=randomState>>>7; randomState^=randomState<<17;
        return (int)Long.remainderUnsigned(randomState,bound);
    }
    public List<Aabb> collisions(Aabb area) {
        List<Aabb> result=new ArrayList<>();
        for(int y=Math.max(0,(int)Math.floor(area.minY()));y<Math.min(HEIGHT,(int)Math.floor(area.maxY())+1);y++)
            for(int z=Math.max(0,(int)Math.floor(area.minZ()));z<Math.min(SIZE,(int)Math.floor(area.maxZ())+1);z++)
                for(int x=Math.max(0,(int)Math.floor(area.minX()));x<Math.min(SIZE,(int)Math.floor(area.maxX())+1);x++) {
                    Block b=get(x,y,z); if(b.solid) {
                        Aabb box=new Aabb(x,y,z,x+1,y+b.height(data(x,y,z)),z+1);
                        if(box.intersects(area)) result.add(box);
                    }
                }
        return result;
    }
    public RayHit raycast(double ox,double oy,double oz,double dx,double dy,double dz,double reach,boolean fluids) {
        double length=Math.sqrt(dx*dx+dy*dy+dz*dz); if(length<1e-12) return null;
        dx/=length; dy/=length; dz/=length;
        int x=(int)Math.floor(ox), y=(int)Math.floor(oy), z=(int)Math.floor(oz);
        int sx=dx>=0?1:-1, sy=dy>=0?1:-1, sz=dz>=0?1:-1, nx=0,ny=0,nz=0;
        double tx=dx==0?Double.POSITIVE_INFINITY:((sx>0?x+1:x)-ox)/dx;
        double ty=dy==0?Double.POSITIVE_INFINITY:((sy>0?y+1:y)-oy)/dy;
        double tz=dz==0?Double.POSITIVE_INFINITY:((sz>0?z+1:z)-oz)/dz;
        double ax=dx==0?Double.POSITIVE_INFINITY:Math.abs(1/dx), ay=dy==0?Double.POSITIVE_INFINITY:Math.abs(1/dy), az=dz==0?Double.POSITIVE_INFINITY:Math.abs(1/dz), distance=0;
        while(distance<=reach) {
            Block b=get(x,y,z);
            if(b!=Block.AIR && (!b.fluid() || fluids && data(x,y,z)==0)) return new RayHit(x,y,z,nx,ny,nz,distance);
            if(tx<ty && tx<tz) { x+=sx; distance=tx; tx+=ax; nx=-sx; ny=0; nz=0; }
            else if(ty<tz) { y+=sy; distance=ty; ty+=ay; nx=0; ny=-sy; nz=0; }
            else { z+=sz; distance=tz; tz+=az; nx=0; ny=0; nz=-sz; }
        }
        return null;
    }
    public void invalidateLighting() { lightingDirty=true; revision++; }
    public int emission(int x,int y,int z) {
        Block b=get(x,y,z);
        if(b==Block.FURNACE) {
            Container c=containers.get(index(x,y,z));
            if(c!=null && c.burn>0) return 13;
        }
        return b.light;
    }
    public void updateLighting() {
        if(!lightingDirty) return;
        lightingDirty=false; Arrays.fill(sky,(byte)0); Arrays.fill(light,(byte)0);
        int[] queue=new int[VOLUME*2]; int tail=0;
        for(int z=0;z<SIZE;z++) for(int x=0;x<SIZE;x++) {
            int sunlight=15;
            for(int y=HEIGHT-1;y>=0;y--) {
                int i=index(x,y,z), opacity=get(x,y,z).opacity();
                sunlight=Math.max(0,sunlight-opacity); sky[i]=(byte)sunlight;
                if(sunlight>0) queue[tail++]=i;
            }
        }
        propagate(sky,queue,tail);
        tail=0;
        for(int i=0;i<VOLUME;i++) { int emission=emission(x(i),y(i),z(i)); if(emission>0) { light[i]=(byte)emission; queue[tail++]=i; } }
        propagate(light,queue,tail);
    }
    private void propagate(byte[] values,int[] queue,int tail) {
        for(int head=0;head<tail;head++) {
            int i=queue[head], x=x(i),y=y(i),z=z(i), current=values[i];
            if(current<=1) continue;
            for(int[] d:DIRECTIONS) {
                int nx=x+d[0],ny=y+d[1],nz=z+d[2]; if(!inside(nx,ny,nz)) continue;
                int ni=index(nx,ny,nz), next=current-Math.max(1,get(nx,ny,nz).opacity());
                if(next>values[ni]) {
                    values[ni]=(byte)next;
                    if(tail==queue.length) queue=Arrays.copyOf(queue,queue.length*2);
                    queue[tail++]=ni;
                }
            }
        }
    }
    public record PendingTick(int index,long due) {}
    public List<PendingTick> pendingTicks() {
        return scheduledTimes.entrySet().stream().map(e->new PendingTick(e.getKey(),e.getValue()))
            .sorted(Comparator.comparingInt(PendingTick::index)).toList();
    }
    public void restoreTicks(List<PendingTick> ticks) {
        scheduled.clear(); scheduledTimes.clear();
        for(PendingTick tick:ticks) {
            if(tick.index<0 || tick.index>=VOLUME || tick.due<time || tick.due>time+30 || scheduledTimes.putIfAbsent(tick.index,tick.due)!=null) throw new IllegalArgumentException("Invalid scheduled block tick");
            scheduled.add(new Scheduled(tick.index,tick.due));
        }
    }
    public byte[] blockSnapshot() { return blocks.clone(); }
    public byte[] dataSnapshot() { return data.clone(); }
    public byte[] biomeSnapshot() { return biomes.clone(); }
    public void restore(byte[] blocks,byte[] metadata,byte[] biomeIds) {
        if(blocks.length!=VOLUME || metadata.length!=VOLUME || biomeIds.length!=SIZE*SIZE) throw new IllegalArgumentException("Invalid world dimensions");
        for(byte b:blocks) Block.byId(Byte.toUnsignedInt(b));
        for(byte b:metadata) if(Byte.toUnsignedInt(b)>15) throw new IllegalArgumentException("Invalid metadata");
        for(byte b:biomeIds) if(Byte.toUnsignedInt(b)>=Biome.values().length) throw new IllegalArgumentException("Invalid biome");
        System.arraycopy(blocks,0,this.blocks,0,VOLUME); System.arraycopy(metadata,0,data,0,VOLUME); System.arraycopy(biomeIds,0,biomes,0,biomeIds.length);
        containers.clear(); scheduled.clear(); scheduledTimes.clear();
        for(int i=0;i<VOLUME;i++) {
            Block b=Block.byId(Byte.toUnsignedInt(blocks[i]));
            if(b==Block.CHEST || b==Block.FURNACE) containers.put(i,new Container(b==Block.FURNACE));
            if(b.fluid() || b==Block.SAND || b.plant() || b==Block.CACTUS) schedule(x(i),y(i),z(i));
        }
        lightingDirty=true; revision++; updateLighting();
    }
}
