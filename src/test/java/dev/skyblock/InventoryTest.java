package dev.skyblock;

import dev.skyblock.game.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InventoryTest {
    @Test void stackingPreservesOverflow() { Inventory i=new Inventory(1); i.set(0,new Stack(Item.DIRT,60)); Stack overflow=i.add(new Stack(Item.DIRT,10)); assertEquals(64,i.get(0).count()); assertEquals(6,overflow.count()); }
    @Test void damagedToolsDoNotMerge() { Inventory i=new Inventory(1); i.set(0,new Stack(Item.WOOD_PICKAXE,1,1)); assertNotNull(i.add(new Stack(Item.WOOD_PICKAXE,1,2))); }
    @Test void planksCanBeCraftedFromAnyGridPosition() { Inventory i=new Inventory(4); i.set(3,new Stack(Item.LOG,2)); assertEquals(new Stack(Item.PLANKS,4),Recipes.craft(i,2,2)); assertEquals(1,i.get(3).count()); }
    @Test void mirroredAxeRecipeConsumesExactlyOnePerCell() { Inventory i=new Inventory(9); Item[] items={Item.PLANKS,Item.PLANKS,null,Item.STICK,Item.PLANKS,null,Item.STICK,null,null}; for(int n=0;n<9;n++) if(items[n]!=null) i.set(n,new Stack(items[n],2)); assertEquals(Item.WOOD_AXE,Recipes.craft(i,3,3).item()); assertEquals(1,i.get(0).count()); }
    @Test void extraIngredientsInvalidateRecipe() { Inventory i=new Inventory(4); i.set(0,new Stack(Item.LOG,1)); i.set(3,new Stack(Item.DIRT,1)); assertNull(Recipes.craft(i,2,2)); assertEquals(1,i.get(0).count()); }
    @Test void threeWideRecipesCannotUseTwoWideGrid() { Inventory i=new Inventory(4); for(int n=0;n<4;n++) i.set(n,new Stack(Item.COBBLESTONE,1)); assertNull(Recipes.match(i,2,2)); }
    @Test void furnaceSmeltsAfterTwoHundredTicks() { Container c=new Container(true); c.inventory.set(0,new Stack(Item.SAND,2)); c.inventory.set(1,new Stack(Item.COAL,1)); for(int n=0;n<199;n++) c.tick(); assertNull(c.inventory.get(2)); c.tick(); assertEquals(Item.GLASS,c.inventory.get(2).item()); assertEquals(1,c.inventory.get(0).count()); assertTrue(c.burn>0); }
    @Test void fullFurnaceOutputPreservesFuelAndInput() { Container c=new Container(true); c.inventory.set(0,new Stack(Item.SAND,2)); c.inventory.set(1,new Stack(Item.COAL,1)); c.inventory.set(2,new Stack(Item.GLASS,64)); for(int n=0;n<300;n++) c.tick(); assertEquals(2,c.inventory.get(0).count()); assertEquals(1,c.inventory.get(1).count()); assertEquals(0,c.burn); }
    @Test void lavaFuelReturnsEmptyBucket() { Container c=new Container(true); c.inventory.set(0,new Stack(Item.SAND,1)); c.inventory.set(1,new Stack(Item.LAVA_BUCKET,1)); c.tick(); assertEquals(Item.BUCKET,c.inventory.get(1).item()); assertEquals(20000,c.burn); }
    @Test void durabilityBreaksOnFinalUse() { assertNull(new Stack(Item.WOOD_PICKAXE,1,58).wear()); assertEquals(1,new Stack(Item.WOOD_PICKAXE,1).wear().damage()); }
}
