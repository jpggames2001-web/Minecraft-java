package dev.skyblock.ui.menu;
import dev.skyblock.ui.Interface;
public final class CraftingScreen extends StorageScreen {
    public CraftingScreen(MenuContext c,Interface gui) { super(c,gui,"Crafting"); }
    @Override public Interface.Screen kind() { return Interface.Screen.CRAFT; }
}
