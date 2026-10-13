package dev.skyblock.world;

/** Independently generated 256x256 climate palettes with bilinear sampling. */
public final class ClimateColorMap {
    public static final ClimateColorMap GRASS=new ClimateColorMap(0x77a56b,0xb8b35d,0x72a84a,0x459532);
    public static final ClimateColorMap FOLIAGE=new ClimateColorMap(0x80a878,0xa6a344,0x4b873e,0x28872b);
    private final int[] colors=new int[256*256];
    private ClimateColorMap(int coldDry,int hotDry,int coldWet,int hotWet) {
        for(int y=0;y<256;y++) for(int x=0;x<256;x++) {
            float temperature=1-x/255f,humidity=1-y/255f;
            colors[y*256+x]=blend(blend(coldDry,hotDry,temperature),blend(coldWet,hotWet,temperature),humidity);
        }
    }
    public int sample(float temperature,float rainfall) {
        float t=Math.clamp(temperature,0,1),rain=Math.clamp(rainfall,0,1)*t;
        float x=(1-t)*255,y=(1-rain)*255; int ix=(int)x,iy=(int)y;
        int x1=Math.min(255,ix+1),y1=Math.min(255,iy+1);
        return blend(blend(colors[iy*256+ix],colors[iy*256+x1],x-ix),blend(colors[y1*256+ix],colors[y1*256+x1],x-ix),y-iy);
    }
    public static int blend(int a,int b,float t) {
        int r=Math.round((a>>16&255)*(1-t)+(b>>16&255)*t),g=Math.round((a>>8&255)*(1-t)+(b>>8&255)*t),blue=Math.round((a&255)*(1-t)+(b&255)*t);
        return r<<16|g<<8|blue;
    }
}
