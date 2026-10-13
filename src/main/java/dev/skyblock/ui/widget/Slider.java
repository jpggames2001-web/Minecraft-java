package dev.skyblock.ui.widget;
import dev.skyblock.ui.*;
import java.util.function.*;
public final class Slider extends Widget {
    private final DoubleSupplier getter;
    private final DoubleConsumer setter;
    private final Supplier<String> label;
    public Slider(String id,int x,int y,int width,DoubleSupplier getter,DoubleConsumer setter,Supplier<String> label) { super(id,x,y,width,20); this.getter=getter; this.setter=setter; this.label=label; }
    @Override public void render(Canvas c,double mx,double my) {
        c.nineSlice(61,x,y,width,20); int handle=x+4+(int)(Math.clamp(getter.getAsDouble(),0,1)*(width-12)); c.nineSlice(contains(mx,my)?60:59,handle-4,y,8,20);
        String s=label.get(); c.text(s,x+(width-BitmapFont.width(s,1))/2f,y+6,0xffffffff,1);
    }
    @Override public void click(double mx,double my) { drag(mx,my); }
    @Override public void drag(double mx,double my) { if(enabled) setter.accept(Math.clamp((mx-x-4)/(width-8),0,1)); }
    @Override public void key(int key,int mods) { if(key==263 || key==262) setter.accept(Math.clamp(getter.getAsDouble()+(key==263?-.05:.05),0,1)); }
}
