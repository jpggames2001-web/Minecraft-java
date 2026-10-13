package dev.skyblock.ui.menu;
import dev.skyblock.ui.*;
import dev.skyblock.client.Binding;
/** Shared inventory-slot component; each storage screen has its own lifecycle and input. */
public abstract class StorageScreen extends MenuScreen {
    protected final Interface gui;
    protected StorageScreen(MenuContext context,Interface gui,String title) { super(context,title); this.gui=gui; }
    public abstract Interface.Screen kind();
    @Override public void layout() { widgets.clear(); }
    @Override public void render(Canvas c,double mx,double my) { gui.renderStorage(); }
    @Override public void click(double mx,double my,int button) { gui.clickStorage(button); }
    @Override public void key(int key,int mods) { if(key==256 || key==context.options.key(Binding.INVENTORY)) context.stack.pop(); }
    @Override public void close() { super.close(); gui.closeStorage(); }
}
