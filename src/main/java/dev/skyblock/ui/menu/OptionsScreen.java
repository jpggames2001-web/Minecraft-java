package dev.skyblock.ui.menu;
public final class OptionsScreen extends MenuScreen {
    public OptionsScreen(MenuContext c) { super(c,"Options"); }
    @Override public void layout() {
        int y=height/6+20;
        button("video","Video Settings...",width/2-100,y,200,()->context.stack.push(new VideoSettingsScreen(context)));
        button("controls","Controls...",width/2-100,y+24,200,()->context.stack.push(new ControlsScreen(context)));
        button("sounds","Music and Sounds...",width/2-100,y+48,200,()->context.stack.push(new SoundsScreen(context)));
        button("language","Language...",width/2-100,y+72,200,()->context.stack.push(new LanguageScreen(context)));
        button("packs","Texture Packs...",width/2-100,y+96,200,()->context.stack.push(new TexturePacksScreen(context)));
        done();
    }
}
