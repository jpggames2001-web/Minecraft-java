package dev.skyblock.game;

import java.util.*;

public final class Recipes {
    public record Recipe(String name,int width,int height,List<Item> pattern,Stack output,boolean shapeless) {}
    private static final List<Recipe> ALL=new ArrayList<>();
    static {
        shapeless("PLANKS",new Stack(Item.PLANKS,4),Item.LOG);
        shape("STICKS",1,2,new Stack(Item.STICK,4),Item.PLANKS,Item.PLANKS);
        shape("CRAFTING TABLE",2,2,new Stack(Item.CRAFTING_TABLE,1),Item.PLANKS,Item.PLANKS,Item.PLANKS,Item.PLANKS);
        shape("CHEST",3,3,new Stack(Item.CHEST,1),Item.PLANKS,Item.PLANKS,Item.PLANKS,Item.PLANKS,null,Item.PLANKS,Item.PLANKS,Item.PLANKS,Item.PLANKS);
        shape("FURNACE",3,3,new Stack(Item.FURNACE,1),Item.COBBLESTONE,Item.COBBLESTONE,Item.COBBLESTONE,Item.COBBLESTONE,null,Item.COBBLESTONE,Item.COBBLESTONE,Item.COBBLESTONE,Item.COBBLESTONE);
        shape("TORCHES",1,2,new Stack(Item.TORCH,4),Item.COAL,Item.STICK);
        shape("BUCKET",3,2,new Stack(Item.BUCKET,1),Item.IRON_INGOT,null,Item.IRON_INGOT,null,Item.IRON_INGOT,null);
        shape("BREAD",3,1,new Stack(Item.BREAD,1),Item.WHEAT,Item.WHEAT,Item.WHEAT);
        shapeless("BONE MEAL",new Stack(Item.BONE_MEAL,3),Item.BONE);
        tools(Item.PLANKS,Item.WOOD_PICKAXE,Item.WOOD_AXE,Item.WOOD_SHOVEL,Item.WOOD_HOE,Item.WOOD_SWORD);
        tools(Item.COBBLESTONE,Item.STONE_PICKAXE,Item.STONE_AXE,Item.STONE_SHOVEL,Item.STONE_HOE,Item.STONE_SWORD);
        shape("IRON PICKAXE",3,3,new Stack(Item.IRON_PICKAXE,1),Item.IRON_INGOT,Item.IRON_INGOT,Item.IRON_INGOT,null,Item.STICK,null,null,Item.STICK,null);
    }
    private Recipes() {}
    private static void tools(Item material,Item pick,Item axe,Item shovel,Item hoe,Item sword) {
        shape(pick.label(),3,3,new Stack(pick,1),material,material,material,null,Item.STICK,null,null,Item.STICK,null);
        shape(axe.label(),2,3,new Stack(axe,1),material,material,material,Item.STICK,null,Item.STICK);
        shape(shovel.label(),1,3,new Stack(shovel,1),material,Item.STICK,Item.STICK);
        shape(hoe.label(),2,3,new Stack(hoe,1),material,material,null,Item.STICK,null,Item.STICK);
        shape(sword.label(),1,3,new Stack(sword,1),material,material,Item.STICK);
    }
    private static void shape(String name,int w,int h,Stack output,Item... pattern) {
        ALL.add(new Recipe(name,w,h,Collections.unmodifiableList(Arrays.asList(pattern)),output,false));
    }
    private static void shapeless(String name,Stack output,Item... items) {
        ALL.add(new Recipe(name,items.length,1,List.of(items),output,true));
    }
    public static List<Recipe> all() { return List.copyOf(ALL); }
    public static Recipe match(Inventory grid,int width,int height) {
        for(Recipe r:ALL) {
            if(r.shapeless) {
                List<Item> expected=new ArrayList<>(r.pattern);
                boolean ok=true;
                for(int i=0;i<grid.size();i++) if(grid.get(i)!=null && !expected.remove(grid.get(i).item())) { ok=false; break; }
                if(ok && expected.isEmpty()) return r;
            } else if(r.width<=width && r.height<=height) {
                for(int oy=0;oy<=height-r.height;oy++) for(int ox=0;ox<=width-r.width;ox++) for(int mirror=0;mirror<2;mirror++) {
                    boolean ok=true;
                    for(int y=0;y<height && ok;y++) for(int x=0;x<width;x++) {
                        int rx=x-ox, ry=y-oy;
                        Item want=rx>=0 && ry>=0 && rx<r.width && ry<r.height ? r.pattern.get(ry*r.width+(mirror==1?r.width-1-rx:rx)) : null;
                        Stack s=grid.get(y*width+x);
                        if((s==null ? null : s.item())!=want) { ok=false; break; }
                    }
                    if(ok) return r;
                }
            }
        }
        return null;
    }
    public static Stack craft(Inventory grid,int width,int height) {
        Recipe r=match(grid,width,height); if(r==null) return null;
        for(int i=0;i<grid.size();i++) if(grid.get(i)!=null) grid.take(i,1);
        return r.output;
    }
    public static Item smelt(Item input) { return switch(input) { case COBBLESTONE -> Item.STONE; case SAND -> Item.GLASS; case IRON_ORE -> Item.IRON_INGOT; case LOG -> Item.COAL; case RAW_PORK -> Item.COOKED_PORK; default -> null; }; }
    public static int fuel(Item item) { return switch(item) { case COAL -> 1600; case LOG, PLANKS, CHEST, CRAFTING_TABLE -> 300; case STICK, SAPLING -> 100; case LAVA_BUCKET -> 20000; default -> 0; }; }
}
