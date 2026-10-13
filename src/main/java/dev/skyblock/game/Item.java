package dev.skyblock.game;

import dev.skyblock.world.Block;

public enum Item {
    GRASS(Block.GRASS), DIRT(Block.DIRT), STONE(Block.STONE), COBBLESTONE(Block.COBBLESTONE),
    BEDROCK(Block.BEDROCK), LOG(Block.LOG), LEAVES(Block.LEAVES), PLANKS(Block.PLANKS), SAND(Block.SAND),
    GLASS(Block.GLASS), TORCH(Block.TORCH), CRAFTING_TABLE(Block.CRAFTING_TABLE), CHEST(Block.CHEST),
    FURNACE(Block.FURNACE), OBSIDIAN(Block.OBSIDIAN), SNOW(Block.SNOW), CACTUS(Block.CACTUS),
    SAPLING(Block.SAPLING), ICE(Block.ICE), WOOL(Block.WOOL), BRICKS(Block.BRICKS),
    STICK, COAL, IRON_INGOT, BUCKET(null,16), WATER_BUCKET(null,1), LAVA_BUCKET(null,1),
    WOOD_PICKAXE(null,1,59), STONE_PICKAXE(null,1,131), IRON_PICKAXE(null,1,250),
    WOOD_AXE(null,1,59), STONE_AXE(null,1,131), WOOD_SHOVEL(null,1,59), STONE_SHOVEL(null,1,131),
    WOOD_HOE(null,1,59), STONE_HOE(null,1,131), WOOD_SWORD(null,1,59), STONE_SWORD(null,1,131),
    SEEDS, WHEAT, BREAD, APPLE, BONE, BONE_MEAL, RAW_PORK, COOKED_PORK, IRON_ORE(Block.IRON_ORE);
    public final Block block;
    public final int limit, durability;
    Item() { this(null,64,0); }
    Item(Block block) { this(block,64,0); }
    Item(Block block,int limit) { this(block,limit,0); }
    Item(Block block,int limit,int durability) { this.block=block; this.limit=limit; this.durability=durability; }
    public String label() { return name().replace('_',' '); }
    public boolean pickaxe() { return name().endsWith("PICKAXE"); }
    public boolean axe() { return name().endsWith("_AXE"); }
    public boolean shovel() { return name().endsWith("SHOVEL"); }
    public boolean hoe() { return name().endsWith("HOE"); }
    public boolean sword() { return name().endsWith("SWORD"); }
    public int tier() { return name().startsWith("IRON_") ? 2 : name().startsWith("STONE_") ? 1 : 0; }
    public double speed(Block block) {
        boolean effective=pickaxe() && (block==Block.STONE || block==Block.COBBLESTONE || block==Block.FURNACE || block==Block.COAL_ORE || block==Block.IRON_ORE || block==Block.OBSIDIAN)
            || axe() && (block==Block.LOG || block==Block.PLANKS || block==Block.CHEST || block==Block.CRAFTING_TABLE)
            || shovel() && (block==Block.DIRT || block==Block.GRASS || block==Block.SAND || block==Block.SNOW);
        return effective ? tier()==2 ? 6 : tier()==1 ? 4 : 2 : 1;
    }
    public int food() { return switch(this) { case APPLE -> 4; case BREAD -> 5; case RAW_PORK -> 3; case COOKED_PORK -> 8; default -> 0; }; }
    public float saturation() { return switch(this) { case APPLE, RAW_PORK -> .3f; case BREAD -> .6f; case COOKED_PORK -> .8f; default -> 0; }; }
    public static Item fromBlock(Block block) {
        for(Item i:values()) if(i.block==block) return i;
        return null;
    }
}
