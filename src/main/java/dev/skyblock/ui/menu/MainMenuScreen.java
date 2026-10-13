package dev.skyblock.ui.menu;
import dev.skyblock.ui.Canvas;
public final class MainMenuScreen extends MenuScreen {
    public MainMenuScreen(MenuContext c) { super(c,""); }
    @Override public void layout() {
        int y=height/4+48;
        button("singleplayer","Singleplayer",width/2-100,y,200,()->context.stack.push(new SelectWorldScreen(context)));
        button("multiplayer","Multiplayer",width/2-100,y+24,200,()->context.stack.push(new MultiplayerScreen(context)));
        button("packs","Mods and Texture Packs",width/2-100,y+48,200,()->context.stack.push(new TexturePacksScreen(context)));
        button("options","Options...",width/2-100,y+84,98,()->context.stack.push(new OptionsScreen(context)));
        button("quit","Quit Game",width/2+2,y+84,98,context.actions::quit);
    }
    @Override public void render(Canvas c,double mx,double my) { super.render(c,mx,my); c.centered("MINECRAFT",height/4f-25,0xffdddddd,4); c.text("Skyblock Classic - independent build",3,c.height-12,0xffaaaaaa,1); }
    @Override public void key(int key,int mods) { if(key!=256) super.key(key,mods); }
}
