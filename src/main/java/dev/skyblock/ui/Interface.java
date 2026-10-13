package dev.skyblock.ui;

import dev.skyblock.game.*;
import dev.skyblock.game.Stack;
import dev.skyblock.world.*;
import java.util.*;

public final class Interface {
    public enum Screen { TITLE, GAME, PAUSE, INVENTORY, CRAFT, CHEST, FURNACE, RECIPES, DEAD }
    public Screen screen=Screen.TITLE;
    public Stack carried;
    private Inventory grid=new Inventory(4);
    private final Game game;
    private final Canvas canvas;
    private final List<Slot> slots=new ArrayList<>();
    private final List<Button> buttons=new ArrayList<>();
    private int mouseX,mouseY,recipePage;
    private Screen previous=Screen.PAUSE;
    public boolean quit,saveRequested;
    private record Slot(Inventory inventory,int index,int x,int y,boolean output,boolean crafted) {}
    private record Button(int x,int y,int width,int height,Runnable action) {}
    public Interface(Game game,Canvas canvas) { this.game=game; this.canvas=canvas; }
    public void set(Screen next) {
        if(screen==Screen.INVENTORY || screen==Screen.CRAFT) {
            for(int i=0;i<grid.size();i++) returnItem(grid.take(i,64));
        }
        if(next==Screen.GAME || next==Screen.PAUSE || next==Screen.DEAD) {
            returnItem(carried); carried=null;
            game.openContainer=null; game.craftingTable=false;
        }
        screen=next; game.paused=next!=Screen.GAME && next!=Screen.CHEST && next!=Screen.FURNACE && next!=Screen.INVENTORY && next!=Screen.CRAFT;
        if(next==Screen.INVENTORY || next==Screen.CRAFT) grid=new Inventory(next==Screen.CRAFT?9:4);
    }
    private void returnItem(Stack stack) { Stack remainder=game.player.inventory.add(stack); if(remainder!=null) game.spawnDrop(remainder,game.player.x,game.player.y,game.player.z); }
    public void escape() {
        switch(screen) {
            case GAME -> set(Screen.PAUSE);
            case TITLE -> quit=true;
            case RECIPES -> set(previous);
            default -> set(Screen.GAME);
        }
    }
    public void inventoryKey() { if(screen==Screen.GAME) set(Screen.INVENTORY); else if(screen!=Screen.TITLE && screen!=Screen.DEAD) set(Screen.GAME); }
    public void recipeKey() { if(screen!=Screen.RECIPES) { previous=screen; set(Screen.RECIPES); } else set(previous); }
    public void draw(int width,int height,double mx,double my,int fps,int vertices) {
        canvas.begin(width,height); mouseX=(int)mx/canvas.scale; mouseY=(int)my/canvas.scale;
        slots.clear(); buttons.clear();
        if(screen==Screen.GAME) hud(fps,vertices);
        else if(screen==Screen.TITLE) {
            canvas.rect(0,0,canvas.width,canvas.height,0xa522211c);
            canvas.centered("SKYBLOCK CLASSIC",canvas.height/2f-90,0xffe3dfd0,4);
            canvas.centered("ORIGINAL PROCEDURAL ART - JAVA 21",canvas.height/2f-46,0xffcbbc85,1);
            button("PLAY SKYBLOCK",canvas.width/2-110,canvas.height/2-5,220,()->set(Screen.GAME));
            button("CONTROLS AND RECIPES",canvas.width/2-110,canvas.height/2+22,220,()->{previous=Screen.TITLE;set(Screen.RECIPES);});
            button("SAVE AND QUIT",canvas.width/2-110,canvas.height/2+49,220,()->{saveRequested=true;quit=true;});
            canvas.centered("INDEPENDENT IMPLEMENTATION - SEE FIDELITY.MD",canvas.height-20,0xffbbb8b0,1);
        } else if(screen==Screen.PAUSE) {
            dim(); canvas.centered("GAME PAUSED",canvas.height/2f-60,0xffffffff,2);
            button("RETURN TO GAME",canvas.width/2-100,canvas.height/2-20,200,()->set(Screen.GAME));
            button("CONTROLS AND RECIPES",canvas.width/2-100,canvas.height/2+7,200,()->{previous=Screen.PAUSE;set(Screen.RECIPES);});
            button("SAVE AND QUIT",canvas.width/2-100,canvas.height/2+34,200,()->{saveRequested=true;quit=true;});
        } else if(screen==Screen.DEAD) {
            canvas.rect(0,0,canvas.width,canvas.height,0xb0751515); canvas.centered("YOU DIED",canvas.height/2f-50,0xffffffff,3);
            button("RESPAWN",canvas.width/2-90,canvas.height/2,180,()->{game.player.respawn();set(Screen.GAME);});
            button("SAVE AND QUIT",canvas.width/2-90,canvas.height/2+27,180,()->{saveRequested=true;quit=true;});
        } else if(screen==Screen.RECIPES) recipes();
        else inventory();
        if(game.messageTicks>0) canvas.centered(game.message,canvas.height-50,0xffffff80,1);
        canvas.finish();
    }
    private void dim() { canvas.rect(0,0,canvas.width,canvas.height,0x90000000); }
    private void button(String label,int x,int y,int width,Runnable action) {
        boolean hover=inside(x,y,width,21);
        canvas.rect(x,y,width,21,hover?0xff858f9b:0xff626268); canvas.border(x,y,width,21,0xff15151a);
        canvas.rect(x+1,y+1,width-2,1,0xffb8b8ba);
        canvas.text(label,x+(width-BitmapFont.width(label,1))/2f,y+7,hover?0xffffffad:0xffeeeeee,1);
        buttons.add(new Button(x,y,width,21,action));
    }
    private boolean inside(int x,int y,int w,int h) { return mouseX>=x && mouseY>=y && mouseX<x+w && mouseY<y+h; }
    private void hud(int fps,int vertices) {
        Player p=game.player; int x=canvas.width/2-91,y=canvas.height-24;
        canvas.rect(x-1,y-1,184,23,0xd0363636); canvas.border(x-1,y-1,184,23,0xff1c1c1c);
        for(int i=0;i<9;i++) {
            canvas.rect(x+i*20+2,y+2,16,16,0xaa171717); canvas.item(p.inventory.get(i),x+i*20+2,y+2);
            if(i==p.selected) canvas.border(x+i*20,y,20,20,0xffeeead7);
        }
        if(!p.creative) {
            for(int i=0;i<10;i++) { heart(x+i*8,y-13,p.health>=i*2+2,p.health==i*2+1); food(x+103+i*8,y-13,p.food>=i*2+2,p.food==i*2+1); }
            if(p.air<300) for(int i=0;i<(p.air+29)/30;i++) canvas.border(x+174-i*8,y-24,6,6,0xff81b8ed);
        }
        canvas.rect(canvas.width/2f-4,canvas.height/2f,9,1,0xeaffffff); canvas.rect(canvas.width/2f,canvas.height/2f-4,1,9,0xeaffffff);
        if(game.breakProgress>0) { canvas.rect(canvas.width/2f-16,canvas.height/2f+10,32,3,0xb0202020); canvas.rect(canvas.width/2f-16,canvas.height/2f+10,32*game.breakProgress,3,0xffe4ded0); }
        if(p.held()!=null) canvas.centered(p.held().item().label(),y-28,0xffffffff,1);
        if(game.debug) {
            String[] lines={"SKYBLOCK CLASSIC / "+fps+" FPS / "+vertices+" VERTICES",String.format(Locale.ROOT,"XYZ: %.3f / %.3f / %.3f",p.x,p.y,p.z),"BIOME: "+game.world.biome((int)p.x,(int)p.z)+" / TICK: "+game.world.time,"LIGHT: "+game.world.sky((int)p.x,(int)p.y,(int)p.z)+" / "+game.world.light((int)p.x,(int)p.y,(int)p.z),"PHYSICS: "+p.physics,"E INVENTORY / R RECIPES / ESC PAUSE"};
            for(int i=0;i<lines.length;i++) { canvas.rect(3,3+i*11,BitmapFont.width(lines[i],1)+4,10,0x88000000); canvas.text(lines[i],5,5+i*11,0xffffffff,1); }
        }
    }
    private void heart(int x,int y,boolean full,boolean half) { canvas.rect(x,y+1,3,3,0xff321e1e); canvas.rect(x+4,y+1,3,3,0xff321e1e); canvas.rect(x+1,y+3,5,3,0xff321e1e); canvas.rect(x+2,y+6,3,1,0xff321e1e); if(full || half) { canvas.rect(x+1,y+2,half?2:5,3,0xffd53c33); canvas.rect(x+2,y+5,half?1:3,1,0xffd53c33); } }
    private void food(int x,int y,boolean full,boolean half) { canvas.rect(x+1,y+1,5,5,0xff35251e); canvas.rect(x+3,y+5,2,2,0xffbbb6a8); if(full || half) canvas.rect(x+2,y+2,half?1:3,3,0xffa87040); }
    private void inventory() {
        dim(); boolean chest=screen==Screen.CHEST,furnace=screen==Screen.FURNACE;
        int h=chest?206:166,x=canvas.width/2-88,y=canvas.height/2-h/2;
        canvas.rect(x,y,176,h,0xffc7c7c7); canvas.border(x,y,176,h,0xffeeeeee); canvas.border(x+1,y+1,174,h-2,0xff555555);
        canvas.text(chest?"CHEST":furnace?"FURNACE":screen==Screen.CRAFT?"CRAFTING":"INVENTORY",x+8,y+7,0xff303030,1);
        if(chest && game.openContainer!=null) for(int i=0;i<27;i++) slot(game.openContainer.inventory,i,x+8+i%9*18,y+18+i/9*18,false,false);
        else if(furnace && game.openContainer!=null) {
            Container c=game.openContainer;
            slot(c.inventory,0,x+54,y+23,false,false); slot(c.inventory,1,x+54,y+59,false,false); slot(c.inventory,2,x+114,y+41,true,false);
            canvas.rect(x+57,y+44,10,12,0xff49453b); if(c.burn>0) canvas.rect(x+57,y+44+12*(1-c.burn/(float)Math.max(1,c.burnTotal)),10,12*c.burn/(float)Math.max(1,c.burnTotal),0xffe59529);
            canvas.text(">",x+83,y+46,0xff444444,2); canvas.rect(x+78,y+63,27*c.progress/200f,2,0xffb18e47);
        } else {
            int size=screen==Screen.CRAFT?3:2,gx=screen==Screen.CRAFT?30:82;
            for(int i=0;i<grid.size();i++) slot(grid,i,x+gx+i%size*18,y+18+i/size*18,false,false);
            Recipes.Recipe r=Recipes.match(grid,size,size);
            Inventory output=new Inventory(1); if(r!=null) output.set(0,r.output());
            slot(output,0,x+143,y+36,true,true); canvas.text(">",x+121,y+42,0xff444444,1);
            if(screen==Screen.INVENTORY) { canvas.rect(x+16,y+20,28,48,0xff737373); canvas.rect(x+25,y+24,10,10,0xffb68d69); canvas.rect(x+23,y+35,14,16,0xff3e8584); canvas.rect(x+24,y+51,5,13,0xff435879); canvas.rect(x+31,y+51,5,13,0xff435879); }
        }
        canvas.text("INVENTORY",x+8,y+h-94,0xff303030,1);
        for(int i=9;i<36;i++) slot(game.player.inventory,i,x+8+(i-9)%9*18,y+h-82+(i-9)/9*18,false,false);
        for(int i=0;i<9;i++) slot(game.player.inventory,i,x+8+i*18,y+h-24,false,false);
        Slot hover=slots.stream().filter(s->inside(s.x,s.y,16,16)).findFirst().orElse(null);
        if(carried!=null) canvas.item(carried,mouseX-8,mouseY-8);
        else if(hover!=null && hover.inventory.get(hover.index)!=null) {
            String label=hover.inventory.get(hover.index).item().label(); int tw=BitmapFont.width(label,1)+8,tx=Math.min(mouseX+10,canvas.width-tw),ty=Math.max(0,mouseY-10);
            canvas.rect(tx,ty,tw,15,0xeb171122); canvas.border(tx,ty,tw,15,0xff7156a0); canvas.text(label,tx+4,ty+4,0xffffffff,1);
        }
        canvas.centered("LEFT MOVE / RIGHT SPLIT / SHIFT TRANSFER / E CLOSE",y+h+9,0xffcccccc,1);
    }
    private void slot(Inventory inventory,int index,int x,int y,boolean output,boolean crafted) {
        canvas.rect(x-1,y-1,18,18,0xff373737); canvas.rect(x,y,17,17,0xfff1f1f1); canvas.rect(x,y,16,16,inside(x,y,16,16)?0xffb1b1b1:0xff898989);
        canvas.item(inventory.get(index),x,y); slots.add(new Slot(inventory,index,x,y,output,crafted));
    }
    public void click(int button,boolean shift) {
        for(Button b:buttons) if(inside(b.x,b.y,b.width,b.height)) { if(button==0) b.action.run(); return; }
        if(screen==Screen.GAME || screen==Screen.PAUSE || screen==Screen.TITLE || screen==Screen.DEAD) return;
        Slot s=slots.stream().filter(slot->inside(slot.x,slot.y,16,16)).findFirst().orElse(null);
        if(s==null) { if(carried!=null && screen!=Screen.RECIPES) { game.spawnDrop(carried,game.player.x,game.player.y,game.player.z); carried=null; } return; }
        Stack in=s.inventory.get(s.index);
        if(s.crafted) { craft(shift); return; }
        if(shift && in!=null) {
            Inventory destination=s.inventory==game.player.inventory && game.openContainer!=null ? game.openContainer.inventory : game.player.inventory;
            if(destination==s.inventory) {
                int lo=s.index<9?9:0,hi=s.index<9?36:9; Stack remaining=in;
                for(int i=lo;i<hi && remaining!=null;i++) remaining=insert(destination,i,remaining);
                s.inventory.set(s.index,remaining);
            } else if(game.openContainer!=null && destination==game.openContainer.inventory && game.openContainer.furnace) {
                int slot=Recipes.fuel(in.item())>0?1:Recipes.smelt(in.item())!=null?0:-1;
                if(slot>=0) s.inventory.set(s.index,insert(destination,slot,in));
            } else s.inventory.set(s.index,destination.add(in));
            return;
        }
        if(s.output) {
            if(in!=null && (carried==null || carried.merges(in) && carried.count()+in.count()<=carried.item().limit)) { carried=carried==null?in:carried.withCount(carried.count()+in.count()); s.inventory.set(s.index,null); }
            return;
        }
        if(button==1) {
            if(carried==null && in!=null) carried=s.inventory.take(s.index,(in.count()+1)/2);
            else if(carried!=null && (in==null || in.merges(carried) && in.count()<in.item().limit)) { s.inventory.set(s.index,new Stack(carried.item(),in==null?1:in.count()+1,carried.damage())); carried=carried.withCount(carried.count()-1); }
        } else if(carried!=null && carried.merges(in)) carried=insert(s.inventory,s.index,carried);
        else { s.inventory.set(s.index,carried); carried=in; }
    }
    private Stack insert(Inventory inventory,int index,Stack stack) {
        Stack in=inventory.get(index);
        if(in==null) { inventory.set(index,stack); return null; }
        if(!in.merges(stack)) return stack;
        int n=Math.min(stack.count(),in.item().limit-in.count()); inventory.set(index,in.withCount(in.count()+n)); return stack.withCount(stack.count()-n);
    }
    private boolean fits(Inventory inventory,Stack stack) { int space=0; for(int i=0;i<inventory.size();i++) { Stack s=inventory.get(i); if(s==null) space+=stack.item().limit; else if(s.merges(stack)) space+=s.item().limit-s.count(); } return space>=stack.count(); }
    private void craft(boolean shift) {
        int size=screen==Screen.CRAFT?3:2;
        for(int n=0;n<(shift?64:1);n++) {
            Recipes.Recipe recipe=Recipes.match(grid,size,size); if(recipe==null) return;
            Stack out=recipe.output();
            if(shift) { if(!fits(game.player.inventory,out)) return; game.player.inventory.add(Recipes.craft(grid,size,size)); }
            else {
                if(carried!=null && (!carried.merges(out) || carried.count()+out.count()>carried.item().limit)) return;
                Stack made=Recipes.craft(grid,size,size); carried=carried==null?made:carried.withCount(carried.count()+made.count());
            }
        }
    }
    private void recipes() {
        dim(); int x=canvas.width/2-245,y=canvas.height/2-150;
        canvas.rect(x,y,490,300,0xf02b2b29); canvas.border(x,y,490,300,0xffb4a587);
        canvas.text("CONTROLS",x+12,y+12,0xffe8d39a,2);
        String[] controls={"WASD MOVE / MOUSE LOOK","SPACE JUMP / SHIFT SNEAK","CTRL OR DOUBLE W SPRINT","LEFT MOUSE BREAK / RIGHT USE","1-9 OR WHEEL SELECT / Q DROP","E INVENTORY / R RECIPE GUIDE","F3 DEBUG / F2 SCREENSHOT","ESC PAUSE / FOOD: HOLD RIGHT","CHEST: TAKE ICE AND LAVA","LOG > PLANKS > TABLE > TOOLS","WATER + FLOWING LAVA = COBBLE"};
        for(int i=0;i<controls.length;i++) canvas.text(controls[i],x+12,y+42+i*17,0xffdedbd3,1);
        canvas.text("RECIPES",x+265,y+12,0xffe8d39a,2);
        List<Recipes.Recipe> all=Recipes.all(); int perPage=6,start=recipePage*perPage;
        for(int i=start;i<Math.min(start+perPage,all.size());i++) {
            Recipes.Recipe r=all.get(i); int ry=y+38+(i-start)*36;
            canvas.item(r.output(),x+265,ry+7); canvas.text(r.name(),x+286,ry+2,0xffffffff,1);
            if(r.shapeless()) canvas.text("SHAPELESS",x+286,ry+16,0xffaaa693,1);
            else {
                for(int row=0;row<r.height();row++) for(int col=0;col<r.width();col++) {
                    Item item=r.pattern().get(row*r.width()+col);
                    if(item!=null) canvas.tile(dev.skyblock.render.Assets.tile(item),x+360+col*10,ry+row*10,9,0xffffffff);
                }
            }
        }
        button("<",x+265,y+264,30,()->recipePage=Math.max(0,recipePage-1));
        button(">",x+302,y+264,30,()->recipePage=Math.min((all.size()-1)/perPage,recipePage+1));
        button("BACK",x+12,y+264,180,()->set(previous));
    }
    public void close() { for(int i=0;i<grid.size();i++) returnItem(grid.take(i,64)); returnItem(carried); carried=null; }
}
