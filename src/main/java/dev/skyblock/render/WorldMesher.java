package dev.skyblock.render;

import dev.skyblock.world.*;

public final class WorldMesher {
    private static final float[][][] FACES={
        {{1,0,0},{1,1,0},{1,1,1},{1,0,1}},
        {{0,0,1},{0,1,1},{0,1,0},{0,0,0}},
        {{0,1,1},{1,1,1},{1,1,0},{0,1,0}},
        {{0,0,0},{1,0,0},{1,0,1},{0,0,1}},
        {{1,0,1},{1,1,1},{0,1,1},{0,0,1}},
        {{0,0,0},{0,1,0},{1,1,0},{1,0,0}}};
    private static final int[] ORDER={0,1,2,0,2,3};
    private static final float[] SHADES={.6f,.6f,1f,.5f,.8f,.8f};
    private WorldMesher() {}
    public static void build(World w,int chunkX,int chunkZ,Mesh.Builder opaque,Mesh.Builder transparent) { build(w,chunkX,chunkZ,opaque,transparent,true,true); }
    public static void build(World w,int chunkX,int chunkZ,Mesh.Builder opaque,Mesh.Builder transparent,boolean fancy,boolean smooth) {
        for(int y=0;y<World.HEIGHT;y++) for(int z=chunkZ*16;z<(chunkZ+1)*16;z++) for(int x=chunkX*16;x<(chunkX+1)*16;x++) {
            Block b=w.get(x,y,z); if(b==Block.AIR) continue;
            Mesh.Builder target=b==Block.WATER || b==Block.ICE || b==Block.GLASS ? transparent : opaque;
            if(b.plant()) { plant(w,x,y,z,b,target); continue; }
            for(int face=0;face<6;face++) {
                int[] d=World.DIRECTIONS[face]; Block neighbor=w.get(x+d[0],y+d[1],z+d[2]);
                if(neighbor.opaque || neighbor==Block.LEAVES && !fancy && b==Block.LEAVES || neighbor==b && (b.fluid() || b==Block.GLASS || b==Block.LEAVES || b==Block.ICE)) continue;
                int color=b.tinted(face)?b==Block.LEAVES?w.biome(x,z).foliageColor():w.biome(x,z).grassColor():0xffffff;
                float sky=w.sky(x+d[0],y+d[1],z+d[2])/15f,light=Math.max(w.emission(x,y,z),w.light(x+d[0],y+d[1],z+d[2]))/15f;
                float[][] corners=FACES[face];
                for(int order:ORDER) {
                    float[] v=corners[order]; float px=v[0],pz=v[2];
                    if(b==Block.CACTUS) { px=.0625f+px*.875f; pz=.0625f+pz*.875f; }
                    float py=v[1]*(float)b.height(w.data(x,y,z));
                    float shade=SHADES[face]*(smooth?occlusion(w,x,y,z,face,order):1);
                    emit(target,x+px,y+py,z+pz,b==Block.FURNACE && face==4 && w.emission(x,y,z)>0?58:b.tile(face),order,color,shade,sky,light);
                }
            }
        }
    }
    private static float occlusion(World w,int x,int y,int z,int face,int vertex) {
        float[] v=FACES[face][vertex]; int[] d=World.DIRECTIONS[face];
        int[] axes=face<2?new int[]{1,2}:face<4?new int[]{0,2}:new int[]{0,1};
        int[] c={x+d[0],y+d[1],z+d[2]},a=c.clone(),b=c.clone(),corner=c.clone();
        int da=v[axes[0]]==0?-1:1,db=v[axes[1]]==0?-1:1;
        a[axes[0]]+=da; b[axes[1]]+=db; corner[axes[0]]+=da; corner[axes[1]]+=db;
        boolean sa=w.get(a[0],a[1],a[2]).opaque,sb=w.get(b[0],b[1],b[2]).opaque,sc=w.get(corner[0],corner[1],corner[2]).opaque;
        int blockers=sa && sb?3:(sa?1:0)+(sb?1:0)+(sc?1:0);
        return 1-blockers*.12f;
    }
    private static void plant(World w,int x,int y,int z,Block b,Mesh.Builder target) {
        float sky=w.sky(x,y,z)/15f,light=Math.max(w.emission(x,y,z),w.light(x,y,z))/15f;
        for(int diagonal=0;diagonal<2;diagonal++) {
            float[][] quad=diagonal==0?new float[][]{{0,0,0},{0,1,0},{1,1,1},{1,0,1}}:new float[][]{{1,0,0},{1,1,0},{0,1,1},{0,0,1}};
            for(int v:ORDER) emit(target,x+quad[v][0],y+quad[v][1],z+quad[v][2],b.texture,v,0xffffff,1,sky,light);
        }
    }
    public static void cube(Mesh.Builder builder,float x,float y,float z,float size,int tile,float brightness) {
        box(builder,x,y,z,size,size,size,tile,brightness);
    }
    public static void box(Mesh.Builder builder,float x,float y,float z,float width,float height,float depth,int tile,float brightness) {
        for(int f=0;f<6;f++) for(int order:ORDER) {
            float[] v=FACES[f][order]; emit(builder,x+v[0]*width,y+v[1]*height,z+v[2]*depth,tile,order,0xffffff,SHADES[f]*brightness,1,0);
        }
    }
    public static void billboard(Mesh.Builder builder,float x,float y,float z,float width,float height,int tile) {
        float[][] corners={{x,y,z},{x,y+height,z},{x+width,y+height,z},{x+width,y,z}};
        for(int order:ORDER) { float[] v=corners[order]; emit(builder,v[0],v[1],v[2],tile,order,0xffffff,1,1,1); }
    }
    private static void emit(Mesh.Builder b,float x,float y,float z,int tile,int corner,int color,float shade,float sky,float light) {
        float u=(tile%8*16+(corner==2 || corner==3?15.99f:.01f))/128;
        float v=(tile/8*16+(corner==0 || corner==3?15.99f:.01f))/128;
        b.vertex(x,y,z,u,v,(color>>16&255)/255f*shade,(color>>8&255)/255f*shade,(color&255)/255f*shade,sky,light);
    }
}
