package dev.skyblock.ui.menu;
public final class DeathScreen extends MenuScreen {
    public DeathScreen(MenuContext c) { super(c,"You died!"); }
    @Override public void layout() { button("respawn","Respawn",width/2-100,height/2,200,context.actions::respawn); button("title","Title menu",width/2-100,height/2+24,200,()->context.run(context.actions::title)); }
    @Override public void key(int key,int mods) { if(key!=256) super.key(key,mods); }
}
