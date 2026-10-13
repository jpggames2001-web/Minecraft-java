package dev.skyblock.ui.menu;
import dev.skyblock.ui.Canvas;
public final class DialogScreen extends MenuScreen {
    private final String message;
    public DialogScreen(MenuContext c,String title,String message) { super(c,title); this.message=message; }
    @Override public void layout() { done(); }
    @Override public void render(Canvas c,double mx,double my) { super.render(c,mx,my); String text=message; int line=0; while(!text.isEmpty() && line<8) { int n=Math.min(text.length(),Math.max(10,(width-40)/6)); c.centered(text.substring(0,n),60+line++*12,0xffcccccc,1); text=text.substring(n); } }
}
