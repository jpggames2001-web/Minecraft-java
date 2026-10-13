package dev.skyblock.ui.menu;
import dev.skyblock.ui.Interface;
public final class InventoryScreen extends StorageScreen {
    public InventoryScreen(MenuContext c,Interface gui) { super(c,gui,"Inventory"); }
    @Override public Interface.Screen kind() { return Interface.Screen.INVENTORY; }
}
