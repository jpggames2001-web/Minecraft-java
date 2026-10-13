package dev.skyblock.ui.menu;
public final class VideoSettingsScreen extends MenuScreen {
    public VideoSettingsScreen(MenuContext c) { super(c,"Video Settings"); }
    private void cycle(String key,String label,String fallback,int index,String... choices) {
        button(key,()->context.tr(label)+": "+context.tr(context.options.text(key,fallback)),width/2-154+(index%2)*156,48+index/2*24,152,()->context.run(()->context.options.cycle(key,fallback,choices)));
    }
    private void toggle(String key,String label,boolean fallback,int index) {
        button(key,()->context.tr(label)+": "+context.tr(context.options.flag(key,fallback)?"ON":"OFF"),width/2-154+(index%2)*156,48+index/2*24,152,()->context.run(()->context.options.toggle(key,fallback)));
    }
    @Override public void layout() {
        cycle("graphics","Graphics","Fancy",0,"Fancy","Fast"); cycle("distance","Render Distance","Normal",1,"Tiny","Short","Normal","Far");
        toggle("smoothLighting","Smooth Lighting",true,2); toggle("anaglyph","3D Anaglyph",false,3);
        cycle("guiScale","GUI Scale","Auto",4,"Auto","Small","Normal","Large"); toggle("advancedOpenGL","Advanced OpenGL",false,5);
        cycle("fpsLimit","Framerate Limit","Unlimited",6,"Unlimited","120","60","30"); toggle("vsync","VSync",true,7); done();
    }
}
