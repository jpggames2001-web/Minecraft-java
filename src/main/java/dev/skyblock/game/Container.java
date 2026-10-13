package dev.skyblock.game;

public final class Container {
    public final Inventory inventory;
    public final boolean furnace;
    public int burn, burnTotal, progress;
    public Container(boolean furnace) { this.furnace=furnace; inventory=new Inventory(furnace ? 3 : 27); }
    public void tick() {
        if(!furnace) return;
        if(burn>0) burn--;
        Stack input=inventory.get(0), fuel=inventory.get(1), output=inventory.get(2);
        Item result=input==null ? null : Recipes.smelt(input.item());
        boolean can=result!=null && (output==null || output.item()==result && output.count()<result.limit);
        if(burn==0 && can && fuel!=null && Recipes.fuel(fuel.item())>0) {
            burn=burnTotal=Recipes.fuel(fuel.item());
            inventory.take(1,1);
            if(fuel.item()==Item.LAVA_BUCKET) inventory.set(1,new Stack(Item.BUCKET,1));
        }
        if(can && burn>0) {
            if(++progress>=200) {
                progress=0; inventory.take(0,1);
                inventory.set(2,new Stack(result,output==null ? 1 : output.count()+1));
            }
        } else progress=0;
    }
}
