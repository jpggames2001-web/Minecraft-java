package dev.skyblock.ui.menu;
import dev.skyblock.client.Binding;
import dev.skyblock.game.MouseSettings;
import dev.skyblock.ui.*;
import dev.skyblock.ui.widget.Slider;
public final class ControlsScreen extends MenuScreen {
    private Binding awaiting;
    private int offset;
    public ControlsScreen(MenuContext c) { super(c,"Controls"); }
    @Override public void layout() {
        button("invertX",()->"Invert Mouse X: "+(context.options.mouse.value().invertX()?"ON":"OFF"),width/2-154,40,152,()->context.run(()->context.options.mouse.save(context.options.mouse.value().toggleX())));
        button("invertY",()->"Invert Mouse Y: "+(context.options.mouse.value().invertY()?"ON":"OFF"),width/2+2,40,152,()->context.run(()->context.options.mouse.save(context.options.mouse.value().toggleY())));
        widgets.add(new Slider("sensitivity",width/2-154,64,308,()->(context.options.mouse.value().sensitivity()-.01)/.99,n->context.run(()->context.options.mouse.save(new MouseSettings(context.options.mouse.value().invertX(),context.options.mouse.value().invertY(),.01+n*.99))),()->"Sensitivity: "+Math.round(context.options.mouse.value().sensitivity()/.15*100)+"%"));
        int count=Math.max(1,(height-146)/22); Binding[] bindings=Binding.values(); offset=Math.clamp(offset,0,Math.max(0,bindings.length-count));
        for(int row=0;row<count && row+offset<bindings.length;row++) {
            Binding b=bindings[row+offset]; int y=92+row*22;
            var button=button("bind."+b.name(),()->b.label+": "+(awaiting==b?"> Press a key <":Binding.keyName(context.options.key(b))),width/2-154,y,260,()->awaiting=b);
            if(context.options.conflicts(b)) button.textColor=0xffff5555;
            button("clear."+b.name(),"Clear",width/2+110,y,44,()->context.run(()->context.options.bind(b,0)));
        }
        button("reset","Reset",width/2-154,height-48,152,()->context.run(()->{context.options.resetBindings();context.options.mouse.save(MouseSettings.DEFAULT);awaiting=null;}));
        button("back","Done",width/2+2,height-48,152,context.stack::pop);
    }
    @Override public void key(int key,int mods) { if(awaiting!=null) { if(key!=256) { Binding b=awaiting; context.run(()->context.options.bind(b,key)); } awaiting=null; } else super.key(key,mods); }
    @Override public void click(double x,double y,int button) { if(awaiting!=null) { Binding b=awaiting; context.run(()->context.options.bind(b,-100-button)); awaiting=null; } else super.click(x,y,button); }
    @Override public void scroll(double amount,double x,double y) { offset=Math.max(0,offset-(int)Math.signum(amount)); }
    @Override public void render(Canvas c,double mx,double my) { super.render(c,mx,my); c.centered(awaiting!=null?"Press a key or mouse button. Esc cancels.":"Conflicting bindings are red. Scroll for more.",height-15,0xffaaaaaa,1); }
}
