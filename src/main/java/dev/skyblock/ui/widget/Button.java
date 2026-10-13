package dev.skyblock.ui.widget;
import dev.skyblock.ui.*;
import java.util.function.Supplier;
public class Button extends Widget {
    private final Supplier<String> label;
    protected final Runnable action;
    public int textColor;
    public Button(String id,int x,int y,int width,Supplier<String> label,Runnable action) { super(id,x,y,width,20); this.label=label; this.action=action; }
    @Override public void render(Canvas c,double mx,double my) {
        boolean hover=enabled && (contains(mx,my) || focused);
        c.nineSlice(enabled?hover?60:59:61,x,y,width,height);
        String text=label.get(); c.text(text,x+(width-BitmapFont.width(text,1))/2f,y+6,!enabled?0xff999999:textColor!=0?textColor:hover?0xffffffaa:0xffe0e0e0,1);
    }
    @Override public void click(double mx,double my) { if(enabled) action.run(); }
}
