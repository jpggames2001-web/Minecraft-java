package dev.skyblock.ui.menu;
public final class PauseMenuScreen extends MenuScreen {
    public PauseMenuScreen(MenuContext c) { super(c,"Game menu"); }
    @Override public void layout() {
        int y=height/4+24;
        button("resume","Back to Game",width/2-100,y,200,context.actions::resume);
        button("options","Options...",width/2-100,y+24,200,()->context.stack.push(new OptionsScreen(context)));
        button("title","Save and Quit to Title",width/2-100,y+60,200,()->context.run(context.actions::title));
    }
    @Override public void key(int key,int mods) { if(key==256) context.actions.resume(); else super.key(key,mods); }
}
