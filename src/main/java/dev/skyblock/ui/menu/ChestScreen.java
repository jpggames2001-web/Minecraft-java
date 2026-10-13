package dev.skyblock.ui.menu;
import dev.skyblock.ui.Interface;
public final class ChestScreen extends StorageScreen {
    public ChestScreen(MenuContext c,Interface gui) { super(c,gui,"Chest"); }
    @Override public Interface.Screen kind() { return Interface.Screen.CHEST; }
}
