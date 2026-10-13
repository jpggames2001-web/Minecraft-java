package dev.skyblock.ui.widget;
import dev.skyblock.ui.Canvas;
public abstract class Widget {
    public final String id;
    public int x,y,width,height;
    public boolean enabled=true,focused;
    protected Widget(String id,int x,int y,int width,int height) { this.id=id; bounds(x,y,width,height); }
    public Widget bounds(int x,int y,int width,int height) { this.x=x; this.y=y; this.width=width; this.height=height; return this; }
    public boolean contains(double px,double py) { return px>=x && py>=y && px<x+width && py<y+height; }
    public abstract void render(Canvas c,double mx,double my);
    public abstract void click(double mx,double my);
    public void drag(double mx,double my) { }
    public void key(int key,int mods) { }
    public void character(int codepoint) { }
    public void scroll(double amount) { }
}
