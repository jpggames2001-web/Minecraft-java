package dev.skyblock.ui.menu;
import dev.skyblock.ui.Interface;
public final class FurnaceScreen extends StorageScreen {
    public FurnaceScreen(MenuContext c,Interface gui) { super(c,gui,"Furnace"); }
    @Override public Interface.Screen kind() { return Interface.Screen.FURNACE; }
}
