package dev.skyblock.game;

import dev.skyblock.world.*;
import dev.skyblock.audio.SynthAudio;
import java.util.*;

public final class Game {
    public final World world;
    public final Player player;
    public final List<Drop> drops=new ArrayList<>();
    public RayHit target;
    public float breakProgress;
    public boolean paused,debug;
    public Container openContainer;
    public boolean craftingTable;
    public String message="";
    public int messageTicks;
    private RayHit breaking;
    private int useCooldown,eatingTicks,stepTicks,miningCooldown;
    private Item eatingItem;
    private final SynthAudio audio;
    public Game(World world,Player player,SynthAudio audio) { this.world=world; this.player=player; this.audio=audio; }
    public void tick(Controls controls,boolean attack,boolean use) {
        if(paused) return;
        if(messageTicks>0) messageTicks--;
        int oldHealth=player.health;
        player.tick(world,controls);
        if(oldHealth>0 && player.health==0) {
            for(int i=0;i<player.inventory.size();i++) { Stack s=player.inventory.take(i,64); if(s!=null) spawnDrop(s,player.x,player.y,player.z); }
        }
        if(player.health<oldHealth) audio.play(SynthAudio.Effect.HURT);
        world.tick();
        Iterator<Drop> it=drops.iterator();
        while(it.hasNext()) { Drop d=it.next(); if(!d.tick(world,player)) { if(d.stack==null) audio.play(SynthAudio.Effect.PICKUP); it.remove(); } }
        if(++stepTicks>=7 && player.onGround && Math.hypot(player.vx,player.vz)>.02) { stepTicks=0; audio.play(SynthAudio.Effect.STEP); }
        double[] direction=player.direction();
        target=world.raycast(player.x,player.eyeY(),player.z,direction[0],direction[1],direction[2],player.creative?5:4.5,player.held()!=null && player.held().item()==Item.BUCKET);
        if(player.health<=0) return;
        if(miningCooldown>0) miningCooldown--;
        if(attack && target!=null && miningCooldown==0) mine(); else { breakProgress=0; breaking=null; }
        if(useCooldown>0) useCooldown--;
        Stack held=player.held();
        if(use && held!=null && held.item().food()>0 && player.food<20) {
            if(eatingItem!=held.item()) { eatingItem=held.item(); eatingTicks=0; }
            if(++eatingTicks>=32) { player.eat(held.item()); consumeHeld(); eatingTicks=0; audio.play(SynthAudio.Effect.PICKUP); }
        } else { eatingTicks=0; eatingItem=null; if(use && useCooldown==0 && target!=null) { useCooldown=4; use(); } }
    }
    private boolean same(RayHit a,RayHit b) { return a!=null && b!=null && a.x()==b.x() && a.y()==b.y() && a.z()==b.z(); }
    private void mine() {
        if(!same(target,breaking)) { breaking=target; breakProgress=0; }
        Block b=world.get(target.x(),target.y(),target.z());
        if(b.hardness<0 && !player.creative || b.fluid()) return;
        Stack tool=player.held(); Item item=tool==null?null:tool.item();
        boolean harvest=canHarvest(b,item);
        double speed=item==null?1:item.speed(b);
        if(!player.onGround) speed/=5;
        if(world.get((int)player.x,(int)player.eyeY(),(int)player.z)==Block.WATER) speed/=5;
        breakProgress+=player.creative || b.hardness==0 ? 1 : (float)(speed/b.hardness/(harvest?30:100));
        if(breakProgress>=1) {
            Container c=world.containers.get(World.index(target.x(),target.y(),target.z()));
            if(c!=null && !player.creative) for(int i=0;i<c.inventory.size();i++) spawnDrop(c.inventory.get(i),target.x()+.5,target.y()+.5,target.z()+.5);
            if(!player.creative && harvest) {
                Item drop=switch(b) { case STONE -> Item.COBBLESTONE; case GRASS, FARMLAND -> Item.DIRT; case COAL_ORE -> Item.COAL; case LEAVES -> world.randomInt(20)==0?Item.SAPLING:world.randomInt(200)==0?Item.APPLE:null; case WHEAT -> world.data(target.x(),target.y(),target.z())==7?Item.WHEAT:Item.SEEDS; case GLASS, ICE, SNOW -> null; default -> Item.fromBlock(b); };
                if(drop!=null) spawnDrop(new Stack(drop,1),target.x()+.5,target.y()+.25,target.z()+.5);
                if(b==Block.WHEAT && world.data(target.x(),target.y(),target.z())==7) spawnDrop(new Stack(Item.SEEDS,1+world.randomInt(3)),target.x()+.5,target.y(),target.z()+.5);
            }
            world.set(target.x(),target.y(),target.z(),b==Block.ICE && world.get(target.x(),target.y()-1,target.z()).solid ? Block.WATER : Block.AIR);
            if(tool!=null && !player.creative) player.inventory.set(player.selected,tool.wear());
            player.exhaust(.025f); audio.play(SynthAudio.Effect.DIG); breakProgress=0; breaking=null; miningCooldown=5;
        }
    }
    public static boolean canHarvest(Block b,Item item) {
        return switch(b) {
            case OBSIDIAN -> false; // No diamond tools are implemented in this release of this project.
            case STONE, COBBLESTONE, FURNACE, COAL_ORE, BRICKS -> item!=null && item.pickaxe();
            case IRON_ORE -> item!=null && item.pickaxe() && item.tier()>=1;
            case BEDROCK -> false;
            default -> true;
        };
    }
    public void use() {
        if(target==null) return;
        Block clicked=world.get(target.x(),target.y(),target.z()); Stack held=player.held();
        if(!player.sneaking) {
            if(clicked==Block.CHEST || clicked==Block.FURNACE) { openContainer=world.containers.get(World.index(target.x(),target.y(),target.z())); return; }
            if(clicked==Block.CRAFTING_TABLE) { craftingTable=true; return; }
        }
        if(held==null) return;
        Item item=held.item();
        if(item==Item.BUCKET && clicked.fluid() && world.data(target.x(),target.y(),target.z())==0) {
            world.set(target.x(),target.y(),target.z(),Block.AIR);
            replaceOneHeld(new Stack(clicked==Block.WATER?Item.WATER_BUCKET:Item.LAVA_BUCKET,1)); return;
        }
        if(item.hoe() && (clicked==Block.GRASS || clicked==Block.DIRT) && world.get(target.x(),target.y()+1,target.z())==Block.AIR) {
            world.set(target.x(),target.y(),target.z(),Block.FARMLAND); if(!player.creative) player.inventory.set(player.selected,held.wear()); return;
        }
        if(item==Item.BONE_MEAL && clicked==Block.SAPLING) { if(world.growTree(target.x(),target.y(),target.z())) consumeHeld(); return; }
        if(item==Item.BONE_MEAL && clicked==Block.WHEAT) { world.set(target.x(),target.y(),target.z(),Block.WHEAT,7); consumeHeld(); return; }
        int x=target.x()+target.nx(),y=target.y()+target.ny(),z=target.z()+target.nz();
        if(clicked.plant()) { x=target.x(); y=target.y(); z=target.z(); }
        Block place=item==Item.WATER_BUCKET?Block.WATER:item==Item.LAVA_BUCKET?Block.LAVA:item==Item.SEEDS?Block.WHEAT:item.block;
        if(place==null || !World.inside(x,y,z) || world.get(x,y,z)!=Block.AIR && !world.get(x,y,z).fluid() && !world.get(x,y,z).plant()) return;
        if(place.solid && player.box().intersects(new Aabb(x,y,z,x+1,y+place.height(0),z+1))) return;
        Block below=world.get(x,y-1,z);
        if(place==Block.SAPLING && below!=Block.GRASS && below!=Block.DIRT || place==Block.WHEAT && below!=Block.FARMLAND || place==Block.TORCH && !below.solid || place==Block.CACTUS && below!=Block.SAND && below!=Block.CACTUS) return;
        world.set(x,y,z,place);
        if(item==Item.WATER_BUCKET || item==Item.LAVA_BUCKET) { if(!player.creative) player.inventory.set(player.selected,new Stack(Item.BUCKET,1)); }
        else consumeHeld();
        audio.play(SynthAudio.Effect.PLACE);
    }
    public void consumeHeld() { if(!player.creative) player.inventory.take(player.selected,1); }
    private void replaceOneHeld(Stack replacement) {
        if(player.creative) { player.inventory.set(player.selected,replacement); return; }
        consumeHeld();
        if(player.held()==null) player.inventory.set(player.selected,replacement);
        else { Stack remainder=player.inventory.add(replacement); if(remainder!=null) spawnDrop(remainder,player.x,player.y,player.z); }
    }
    public void dropHeld() { Stack s=player.inventory.take(player.selected,1); if(s!=null) { Drop d=spawnDrop(s,player.x,player.eyeY()-.2,player.z); double[] v=player.direction(); d.vx=v[0]*.3; d.vy=v[1]*.3+.1; d.vz=v[2]*.3; d.pickupDelay=40; } }
    public Drop spawnDrop(Stack stack,double x,double y,double z) {
        if(stack==null) return null;
        Drop d=new Drop(stack,x,y,z); drops.add(d); return d;
    }
    public void message(String text) { message=text; messageTicks=100; }
    public void creativeKit() {
        player.creative=true;
        Item[] kit={Item.COBBLESTONE,Item.PLANKS,Item.GRASS,Item.LOG,Item.GLASS,Item.TORCH,Item.WATER_BUCKET,Item.LAVA_BUCKET,Item.SAPLING};
        for(int i=0;i<kit.length;i++) player.inventory.set(i,new Stack(kit[i],kit[i].limit));
    }
}
