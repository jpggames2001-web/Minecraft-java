package dev.skyblock.ui.menu;
import dev.skyblock.ui.*;
import dev.skyblock.ui.widget.*;
import java.util.*;
import java.util.function.*;
import static org.lwjgl.glfw.GLFW.*;
public abstract class MenuScreen {
    protected final MenuContext context;
    protected final List<Widget> widgets=new ArrayList<>();
    protected int width=640,height=360;
    private String focus;
    protected String submitId;
    protected void focus(String id) { focus=id; }
    private Widget dragged;
    public final String title;
    protected MenuScreen(MenuContext context,String title) { this.context=context; this.title=title; }
    public abstract void layout();
    public void prepare(int width,int height) { this.width=width; this.height=height; widgets.clear(); layout(); for(Widget w:widgets) w.focused=w.id.equals(focus); }
    public void render(Canvas c,double mx,double my) {
        prepare(c.width,c.height);
        c.centered(context.tr(title),20,0xffffffff,1);
        for(Widget w:widgets) w.render(c,mx,my);
    }
    protected Button button(String id,String label,int x,int y,int w,Runnable action) { return button(id,()->context.tr(label),x,y,w,action); }
    protected Button button(String id,Supplier<String> label,int x,int y,int w,Runnable action) { Button b=new Button(id,x,y,w,label,action); widgets.add(b); return b; }
    protected void done() { button("back","Done",width/2-100,height-28,200,context.stack::pop); }
    protected void text(String id,TextField field,int x,int y,int w) { field.bounds(x,y,w,20); field.clipboard(context.clipboard,context.copy); widgets.add(field); }
    public Widget widget(String id) { return widgets.stream().filter(w->w.id.equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown widget "+id)); }
    public void click(double x,double y,int button) {
        if(button!=0) return;
        for(Widget w:widgets) if(w.enabled && w.contains(x,y)) { focus=w.id; for(Widget other:widgets) other.focused=other==w; dragged=w; w.click(x,y); return; }
        focus=null;
    }
    public void drag(double x,double y,boolean down) { if(!down) dragged=null; else if(dragged!=null) dragged.drag(x,y); }
    public void character(int cp) { for(Widget w:widgets) if(w.id.equals(focus)) w.character(cp); }
    public void key(int key,int mods) {
        if(key==GLFW_KEY_ESCAPE) { context.stack.pop(); return; }
        if(key==GLFW_KEY_TAB) { List<Widget> active=widgets.stream().filter(w->w.enabled).toList(); if(!active.isEmpty()) { int n=-1; for(int i=0;i<active.size();i++) if(active.get(i).id.equals(focus)) n=i; focus=active.get(Math.floorMod(n+((mods&GLFW_MOD_SHIFT)!=0?-1:1),active.size())).id; } return; }
        if(key==GLFW_KEY_ENTER && submitId!=null) { Widget primary=widget(submitId); if(primary.enabled) primary.click(primary.x,primary.y); return; }
        for(Widget w:widgets) if(w.id.equals(focus)) { if(key==GLFW_KEY_ENTER && w instanceof Button) w.click(w.x,w.y); else w.key(key,mods); return; }
    }
    public void scroll(double amount,double x,double y) { for(Widget w:widgets) if(w.contains(x,y)) w.scroll(amount); }
    public void close() { dragged=null; }
}
