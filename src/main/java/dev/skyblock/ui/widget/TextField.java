package dev.skyblock.ui.widget;
import dev.skyblock.ui.*;
import java.util.function.*;
import static org.lwjgl.glfw.GLFW.*;
public final class TextField extends Widget {
    private String value;
    private int cursor;
    private boolean selected;
    private final int limit;
    private Supplier<String> paste=()->"";
    private Consumer<String> copy=s->{};
    public TextField(String id,String initial,int limit) { super(id,0,0,200,20); this.value=initial; this.limit=limit; cursor=initial.length(); }
    public String value() { return value; }
    public TextField clipboard(Supplier<String> paste,Consumer<String> copy) { this.paste=paste; this.copy=copy; return this; }
    @Override public void render(Canvas c,double mx,double my) {
        c.rect(x,y,width,height,0xff000000); c.border(x,y,width,height,focused?0xffcccccc:0xff777777);
        String visible=value; while(BitmapFont.width(visible,1)>width-10 && !visible.isEmpty()) visible=visible.substring(1);
        if(selected && focused) c.rect(x+4,y+4,Math.min(width-8,BitmapFont.width(visible,1)),10,0xff394d78);
        c.text(visible,x+4,y+6,0xffeeeeee,1);
        if(focused && System.currentTimeMillis()/500%2==0) { int start=value.length()-visible.length(),position=Math.clamp(cursor-start,0,visible.length()); c.rect(x+4+BitmapFont.width(visible.substring(0,position),1),y+5,1,10,0xffffffff); }
    }
    @Override public void click(double mx,double my) { focused=true; cursor=value.length(); selected=false; }
    @Override public void character(int codepoint) { if(focused && Character.isValidCodePoint(codepoint) && !Character.isISOControl(codepoint)) insert(new String(Character.toChars(codepoint))); }
    private void insert(String s) {
        s=s.replaceAll("[\\p{Cntrl}]","");
        if(selected) { value=""; cursor=0; selected=false; }
        int room=limit-value.length(); if(room<=0) return; if(s.length()>room) s=s.substring(0,room);
        value=value.substring(0,cursor)+s+value.substring(cursor); cursor+=s.length();
    }
    @Override public void key(int key,int mods) {
        if(!focused) return;
        if((mods&GLFW_MOD_CONTROL)!=0) {
            if(key==GLFW_KEY_A) { selected=true; return; }
            if(key==GLFW_KEY_C) { if(selected) copy.accept(value); return; }
            if(key==GLFW_KEY_V) { String s=paste.get(); if(s!=null) insert(s); return; }
        }
        if(key==GLFW_KEY_BACKSPACE && (selected || cursor>0)) { if(selected) { value=""; cursor=0; } else { value=value.substring(0,cursor-1)+value.substring(cursor); cursor--; } selected=false; }
        else if(key==GLFW_KEY_DELETE && (selected || cursor<value.length())) { if(selected) { value=""; cursor=0; } else value=value.substring(0,cursor)+value.substring(cursor+1); selected=false; }
        else if(key==GLFW_KEY_LEFT) { cursor=Math.max(0,cursor-1); selected=false; }
        else if(key==GLFW_KEY_RIGHT) { cursor=Math.min(value.length(),cursor+1); selected=false; }
        else if(key==GLFW_KEY_HOME) { cursor=0; selected=false; }
        else if(key==GLFW_KEY_END) { cursor=value.length(); selected=false; }
    }
}
