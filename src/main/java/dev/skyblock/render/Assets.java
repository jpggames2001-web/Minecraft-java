package dev.skyblock.render;

import dev.skyblock.game.Item;
import java.nio.ByteBuffer;
import java.util.Random;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL33C.*;

/** All artwork is generated here from original algorithms and geometric motifs. */
public final class Assets implements AutoCloseable {
    public static final int TILE=16, COLUMNS=8, SIZE=128;
    private final int[] pixels=new int[SIZE*SIZE];
    public final int texture;
    private final int[] defaults;
    private java.awt.image.BufferedImage previewImage;
    public Assets() {
        generate(); defaults=pixels.clone(); ByteBuffer bytes=BufferUtils.createByteBuffer(SIZE*SIZE*4);
        for(int argb:pixels) bytes.put((byte)(argb>>16)).put((byte)(argb>>8)).put((byte)argb).put((byte)(argb>>>24));
        bytes.flip(); texture=glGenTextures(); glBindTexture(GL_TEXTURE_2D,texture);
        glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,SIZE,SIZE,0,GL_RGBA,GL_UNSIGNED_BYTE,bytes);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST); glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE); glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
    }
    public java.awt.image.BufferedImage proceduralImage() {
        var image=new java.awt.image.BufferedImage(SIZE,SIZE,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0,0,SIZE,SIZE,defaults,0,SIZE); return image;
    }
    public void apply(java.awt.image.BufferedImage image) {
        System.arraycopy(defaults,0,pixels,0,pixels.length);
        if(image!=null) for(int y=0;y<SIZE;y++) for(int x=0;x<SIZE;x++) if((y/16)*8+x/16<59) pixels[y*SIZE+x]=image.getRGB(x,y);
        ByteBuffer data=BufferUtils.createByteBuffer(SIZE*SIZE*4);
        for(int c:pixels) data.put((byte)(c>>16)).put((byte)(c>>8)).put((byte)c).put((byte)(c>>>24));
        data.flip(); glBindTexture(GL_TEXTURE_2D,texture); glTexSubImage2D(GL_TEXTURE_2D,0,0,0,SIZE,SIZE,GL_RGBA,GL_UNSIGNED_BYTE,data); previewImage=null;
    }
    public void preview(java.awt.image.BufferedImage image) {
        if(previewImage==image) return; previewImage=image;
        ByteBuffer bytes=BufferUtils.createByteBuffer(16*16*4);
        for(int y=0;y<16;y++) for(int x=0;x<16;x++) { int c=image.getRGB(x*image.getWidth()/16,y*image.getHeight()/16); bytes.put((byte)(c>>16)).put((byte)(c>>8)).put((byte)c).put((byte)(c>>>24)); }
        bytes.flip(); glBindTexture(GL_TEXTURE_2D,texture); glTexSubImage2D(GL_TEXTURE_2D,0,112,112,16,16,GL_RGBA,GL_UNSIGNED_BYTE,bytes);
    }
    private void generate() {
        int[] bases={0xbcbcbc,0x866446,0x866446,0x898989,0x737575,0x414147,0x795530,0xa3814e,0xa0a0a0,0xa77d4c,0xdccc8b,0xd2edf3,0x315dd4,0xf97514,0x9b733a,0x93632b,0xa87c4b,0xa27643,0x68696a,0x835622,0x221d37,0xeaf4f4,0x457e32,0x528a3a,0x407736,0x805b37,0x799636,0xa5cee7,0x717373,0x828080,0xe3ded7,0x9a553f,0x646464,0x747474};
        for(int tile=0;tile<64;tile++) {
            Random random=new Random(0x51B10CL+tile*371L);
            for(int y=0;y<16;y++) for(int x=0;x<16;x++) {
                int c=shade(tile<bases.length?bases[tile]:0x999999,random.nextInt(35)-17),alpha=255;
                switch(tile) {
                    case 0,8 -> { if(random.nextInt(6)==0) c=shade(c,-35); if(tile==8 && random.nextInt(7)==0) alpha=0; }
                    case 1 -> { if(y<2+Math.floorMod(x*13,3)) c=shade(0x719b3d,random.nextInt(30)-15); }
                    case 4 -> { int row=y/5, xx=(x+(row%2)*3)%8; if(y%5==0 || xx==0) c=0x3c4143; }
                    case 5 -> { if(random.nextBoolean()) c=shade(c,-30); }
                    case 6 -> { if(x%4==0 || (x+y/5)%7==0) c=0x4c3520; }
                    case 7 -> { double r=Math.hypot(x-7.5,y-7.5); if((int)r%3==0) c=0x6b4a2c; }
                    case 9 -> { if(y%4==0) c=0x6b482e; if((x+(y/4)*5)%16==0) c=0x755339; }
                    case 11 -> { alpha=x==0 || y==0 || x==15 || y==15?180:(x+y==13 || x+y==20?100:0); }
                    case 12 -> { alpha=155; c=shade(0x365fcf,(int)(Math.sin(x*.7+y*.4)*18)+random.nextInt(12)); }
                    case 13 -> c=shade(0xef6411,(int)(Math.sin(x*.9+y*.5)*32));
                    case 14 -> { alpha=0; if(x>=6 && x<=9 && y>5) { alpha=255; c=y<9?0xffb431:0x93622d; } }
                    case 15,16 -> { if(x%5==0 || y%5==0) c=0x53321e; if(tile==16 && x>2 && x<13 && y>2 && y<13) c=shade(0x62442b,(x+y)%3*20); }
                    case 17,19 -> { if(x<2 || x>13 || y<2 || y>13 || y==7) c=0x4e3320; if(tile==19 && x>=7 && x<=8 && y>=6 && y<=10) c=0xd5c18e; }
                    case 18,32,58 -> { if(y==0 || y==15 || x==0 || x==15) c=0x3e3e3e; if((tile==32 || tile==58) && x>3 && x<12 && y>4 && y<12) { c=tile==58?shade(0xe38a21,random.nextInt(80)-40):0x222020; alpha=255; } }
                    case 20 -> { if(random.nextInt(7)==0) c=0x564064; }
                    case 22 -> { if(x%4==1) c=0x294e20; if(x%4==2 && y%5==0) c=0xb4ae65; }
                    case 24 -> { alpha=0; if(y>8 && x>=7 && x<=8) { c=0x69482e; alpha=255; } else if(y>=2 && y<=10 && Math.abs(x-7)<(y<6?y/2+1:5)) { c=shade(0x3c8b30,random.nextInt(40)); alpha=255; } }
                    case 59,60,61 -> { c=shade(tile==60?0x929ac0:tile==61?0x383838:0x737373,random.nextInt(14)-7); alpha=255; if(x<2 || y<2) c=tile==61?0x292929:0xa6a6a6; if(x>13 || y>13) c=0x292929; }
                    case 53 -> { c=0xfff3ae; alpha=255; }
                    case 54 -> { c=shade(0xd6dae1,random.nextInt(40)-20); alpha=255; }
                    case 55 -> { c=0xffffff; alpha=255; }
                    case 56 -> { c=shade(0xb88a66,random.nextInt(24)-12); alpha=255; }
                    case 57 -> { c=shade(0x3e8584,random.nextInt(24)-12); alpha=255; }
                    case 25 -> { if(x%4==0) c=0x42301f; }
                    case 26 -> { alpha=0; if((x+y*3)%7<2 && y>3) { c=y<8?0xc4a23c:0x60852d; alpha=255; } }
                    case 27 -> { alpha=185; if(x-y==2 || x-y==-7) c=0xd7ecf5; }
                    case 28,29 -> { if((x*7+y*11)%31<5) c=tile==28?0x252526:0xbf9780; }
                    case 30 -> { if((x+y)%4==0) c=0xcec9c3; }
                    case 31 -> { if(y%4==0 || (x+(y/4%2)*4)%8==0) c=0xc9bfb0; }
                    default -> { if(tile>=34) { c=0; alpha=0; } }
                }
                pixel(tile,x,y,alpha<<24|c);
            }
        }
        for(Item item:Item.values()) if(item.block==null) drawItem(item);
    }
    public static int tile(Item item) {
        if(item.block!=null) return item.block.tile(4);
        if(item.pickaxe()) return 40; if(item.axe()) return 41; if(item.shovel()) return 42; if(item.hoe()) return 43; if(item.sword()) return 44;
        return switch(item) { case STICK -> 34; case COAL -> 35; case IRON_INGOT -> 36; case BUCKET -> 37; case WATER_BUCKET -> 38; case LAVA_BUCKET -> 39; case SEEDS -> 45; case WHEAT -> 46; case BREAD -> 47; case APPLE -> 48; case BONE -> 49; case BONE_MEAL -> 50; case RAW_PORK -> 51; case COOKED_PORK -> 52; default -> 53; };
    }
    private void drawItem(Item item) {
        int tile=tile(item);
        // Shared silhouettes use neutral materials so the same generated tile serves each tool tier.
        for(int y=1;y<15;y++) for(int x=1;x<15;x++) {
            int c=0; boolean draw=false;
            if(item.durability>0) {
                draw=Math.abs(x-(15-y))<=1 && y>=5; c=0x946c3b;
                if(item.pickaxe() && y<6 && x>2 && x<14 || item.axe() && x>8 && y<8 || item.shovel() && x>=9 && y<5 || item.hoe() && y==3 && x>4 || item.sword() && Math.abs(x-(15-y))<=1 && y<10) { draw=true; c=0xb6b4a9; }
            } else switch(item) {
                case STICK -> { draw=Math.abs(x-(15-y))<=1; c=0x9e7740; }
                case BUCKET, WATER_BUCKET, LAVA_BUCKET -> { draw=y>4 && x>2+(y/6) && x<14-(y/6); c=x<5 || x>10 || y>11?0xaeb2b5:item==Item.WATER_BUCKET?0x3e6cd6:item==Item.LAVA_BUCKET?0xf98b22:0x353b42; }
                case APPLE -> { draw=Math.hypot((x-7.5)/1.1,y-9)<5 || x==8 && y<5; c=y<5?0x596d2e:0xc73d28; }
                case BREAD -> { draw=Math.pow((x-8)/6.0,2)+Math.pow((y-9)/3.5,2)<1; c=0xba8642; }
                case WHEAT, SEEDS -> { draw=(x*3+y)%7<2 && y>4; c=item==Item.SEEDS?0x738f3e:0xccb75d; }
                case BONE, BONE_MEAL -> { draw=item==Item.BONE?Math.abs(x+y-15)<2 || x<5 && y>10 || x>10 && y<5:Math.hypot(x-8,y-8)<4; c=0xe8e4cc; }
                case RAW_PORK, COOKED_PORK -> { draw=Math.pow((x-8)/5.5,2)+Math.pow((y-8)/4.5,2)<1; c=item==Item.RAW_PORK?0xd9958b:0x9e6548; }
                case IRON_INGOT -> { draw=y>5 && y<11 && x>2 && x<14; c=y==6?0xe3e4e0:0xa7a8a4; }
                case COAL -> { draw=Math.hypot(x-8,y-8)<5; c=0x2d2e32; }
                default -> { draw=Math.hypot(x-8,y-8)<4; c=0xb3b2ad; }
            }
            if(draw) pixel(tile,x,y,0xff000000|shade(c,(x+y)%3*9-9));
        }
    }
    private void pixel(int tile,int x,int y,int c) { pixels[(tile/COLUMNS*16+y)*SIZE+tile%COLUMNS*16+x]=c; }
    private int shade(int rgb,int delta) { return Math.clamp((rgb>>16&255)+delta,0,255)<<16|Math.clamp((rgb>>8&255)+delta,0,255)<<8|Math.clamp((rgb&255)+delta,0,255); }
    @Override public void close() { glDeleteTextures(texture); }
}
