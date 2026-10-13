package dev.skyblock.world;

/** Stable numeric IDs belong to this project, not to Mojang's serialization. */
public enum Block {
    AIR(0,false,false,0,0,0), GRASS(1,true,true,.6f,0,1), DIRT(2,true,true,.5f,0,2),
    STONE(3,true,true,1.5f,0,3), COBBLESTONE(4,true,true,2,0,4), BEDROCK(5,true,true,-1,0,5),
    LOG(6,true,true,2,0,6), LEAVES(7,true,false,.2f,0,8), PLANKS(8,true,true,2,0,9),
    SAND(9,true,true,.5f,0,10), GLASS(10,true,false,.3f,0,11), WATER(11,false,false,100,0,12),
    LAVA(12,false,false,100,15,13), TORCH(13,false,false,0,14,14),
    CRAFTING_TABLE(14,true,true,2.5f,0,15), CHEST(15,true,true,2.5f,0,17),
    FURNACE(16,true,true,3.5f,0,18), OBSIDIAN(17,true,true,50,0,20),
    SNOW(18,true,true,.2f,0,21), CACTUS(19,true,true,.4f,0,22),
    SAPLING(20,false,false,0,0,24), FARMLAND(21,true,true,.6f,0,25),
    WHEAT(22,false,false,0,0,26), ICE(23,true,false,.5f,0,27),
    COAL_ORE(24,true,true,3,0,28), IRON_ORE(25,true,true,3,0,29),
    WOOL(26,true,true,.8f,0,30), BRICKS(27,true,true,2,0,31);

    public final int id, light, texture;
    public final boolean solid, opaque;
    public final float hardness;
    Block(int id, boolean solid, boolean opaque, float hardness, int light, int texture) {
        this.id=id; this.solid=solid; this.opaque=opaque; this.hardness=hardness; this.light=light; this.texture=texture;
    }
    public boolean fluid() { return this==WATER || this==LAVA; }
    public boolean plant() { return this==SAPLING || this==WHEAT || this==TORCH; }
    public int opacity() { return opaque ? 15 : this==LEAVES || fluid() || this==ICE ? 2 : 0; }
    public double height(int data) { return this==FARMLAND ? .9375 : fluid() ? (data==0 || data>=8 ? .875 : 1-(data+1)/9.0) : 1; }
    public int tile(int face) {
        return switch(this) {
            case GRASS -> face==2 ? 0 : face==3 ? DIRT.texture : texture;
            case LOG -> face==2 || face==3 ? 7 : texture;
            case CRAFTING_TABLE -> face==2 ? 16 : face==3 ? PLANKS.texture : texture;
            case CHEST -> face==4 ? 19 : texture;
            case FURNACE -> face==4 ? 32 : face==2 || face==3 ? 33 : texture;
            default -> texture;
        };
    }
    public boolean tinted(int face) { return this==LEAVES || this==GRASS && face==2; }
    private static final Block[] VALUES=values();
    public static Block byId(int id) {
        if(id<0 || id>=VALUES.length) throw new IllegalArgumentException("Unknown block: "+id);
        return VALUES[id];
    }
}
