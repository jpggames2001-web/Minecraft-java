package dev.skyblock.ui.menu;
import dev.skyblock.ui.widget.Slider;
public final class SoundsScreen extends MenuScreen {
    public SoundsScreen(MenuContext c) { super(c,"Music and Sounds"); }
    private void volume(String key,String label,int y) { widgets.add(new Slider(key,width/2-100,y,200,()->context.options.volume(key),n->context.run(()->context.options.set(key,n)),()->context.tr(label)+": "+Math.round(context.options.volume(key)*100)+"%")); }
    @Override public void layout() { volume("master","Master Volume",60); volume("music","Music",92); volume("sound","Sound",124); done(); }
}
