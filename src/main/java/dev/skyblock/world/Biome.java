package dev.skyblock.world;

import java.awt.Color;

/** The sixteen release-era biome IDs and climate parameters. */
public enum Biome {
    OCEAN(.5f,.5f), PLAINS(.8f,.4f), DESERT(2f,0f), EXTREME_HILLS(.2f,.3f),
    FOREST(.7f,.8f), TAIGA(.05f,.8f), SWAMP(.8f,.9f), RIVER(.5f,.5f),
    HELL(2f,0f), SKY(.5f,.5f), FROZEN_OCEAN(0f,.5f), FROZEN_RIVER(0f,.5f),
    ICE_PLAINS(0f,.5f), ICE_MOUNTAINS(0f,.5f), MUSHROOM_ISLAND(.9f,1f), MUSHROOM_SHORE(.9f,1f);
    public final float temperature,rainfall;
    private static final Biome[] VALUES=values();
    Biome(float temperature,float rainfall) { this.temperature=temperature; this.rainfall=rainfall; }
    public int grassColor() { return this==SWAMP?0x6a7039:ClimateColorMap.GRASS.sample(temperature,rainfall); }
    public int foliageColor() { return this==SWAMP?0x6a7039:ClimateColorMap.FOLIAGE.sample(temperature,rainfall); }
    public int skyColor() {
        float t=Math.clamp(temperature/3f,-1,1);
        return Color.HSBtoRGB(.62222224f-t*.05f,.5f+t*.1f,1f)&0xffffff;
    }
    public static Biome byId(int id) {
        if(id<0 || id>=VALUES.length) throw new IllegalArgumentException("Invalid biome ID");
        return VALUES[id];
    }
}
