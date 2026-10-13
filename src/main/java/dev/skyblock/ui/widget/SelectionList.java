package dev.skyblock.ui.widget;
import dev.skyblock.ui.*;
import java.util.*;
import java.util.function.*;
public final class SelectionList<T> extends Widget {
    private final Supplier<List<T>> values;
    private final Function<T,String> label,detail;
    private final Consumer<T> select;
    public int offset,selected=-1;
    public SelectionList(String id,Supplier<List<T>> values,Function<T,String> label,Function<T,String> detail,Consumer<T> select) { super(id,0,0,300,100); this.values=values; this.label=label; this.detail=detail; this.select=select; }
    @Override public void render(Canvas c,double mx,double my) {
        c.rect(x,y,width,height,0x99000000); List<T> list=values.get(); int count=Math.max(1,height/30); offset=Math.clamp(offset,0,Math.max(0,list.size()-count));
        for(int i=offset;i<Math.min(list.size(),offset+count);i++) { int ry=y+(i-offset)*30; if(i==selected) c.border(x+2,ry+1,width-8,28,0xffbbbbbb); c.text(clip(label.apply(list.get(i)),width-16),x+7,ry+5,0xffffffff,1); c.text(clip(detail.apply(list.get(i)),width-16),x+7,ry+17,0xff888888,1); }
        if(list.size()>count) { c.rect(x+width-4,y,3,height,0xff222222); c.rect(x+width-4,y+(int)(offset/(double)Math.max(1,list.size()-count)*(height-15)),3,15,0xffbbbbbb); }
    }
    private String clip(String s,int width) { while(BitmapFont.width(s,1)>width && !s.isEmpty()) s=s.substring(0,s.length()-1); return s; }
    @Override public void click(double mx,double my) { int index=offset+(int)(my-y)/30; List<T> list=values.get(); if(index>=0 && index<list.size()) { selected=index; select.accept(list.get(index)); } }
    @Override public void scroll(double amount) { offset=Math.clamp(offset-(int)Math.signum(amount),0,Math.max(0,values.get().size()-Math.max(1,height/30))); }
    @Override public void key(int key,int mods) { if(key==264 || key==265) { List<T> list=values.get(); if(!list.isEmpty()) { selected=Math.clamp(selected+(key==264?1:-1),0,list.size()-1); select.accept(list.get(selected)); offset=Math.clamp(selected-Math.max(1,height/30)+1,0,Math.max(0,list.size()-1)); } } }
}
