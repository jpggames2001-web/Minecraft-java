package dev.skyblock.ui.menu;
import dev.skyblock.persistence.WorldLibrary;
import dev.skyblock.ui.*;
import dev.skyblock.ui.widget.TextField;
public final class CreateWorldScreen extends MenuScreen {
    private final TextField name=new TextField("name","New World",64),seed=new TextField("seed","",64);
    private WorldLibrary.Type type=WorldLibrary.Type.SKYBLOCK;
    public CreateWorldScreen(MenuContext c) { super(c,"Create New World"); focus("name"); submitId="create"; }
    @Override public void layout() {
        int x=width/2-100;
        text("name",name,x,58,200); text("seed",seed,x,101,200);
        button("type",()->context.tr("World Type")+": "+(type==WorldLibrary.Type.DEFAULT?"Default":"SkyBlock"),x,136,200,()->type=type==WorldLibrary.Type.DEFAULT?WorldLibrary.Type.SKYBLOCK:WorldLibrary.Type.DEFAULT);
        button("create","Create New World",width/2-102,height-30,100,()->context.run(()->context.actions.openWorld(context.worlds.create(name.value(),seed.value(),type)))).enabled=!name.value().isBlank();
        button("cancel","Cancel",width/2+2,height-30,100,context.stack::pop);
    }
    @Override public void render(Canvas c,double mx,double my) { super.render(c,mx,my); c.text(context.tr("World Name"),width/2-100,45,0xffaaaaaa,1); c.text(context.tr("Seed (blank for random)"),width/2-100,88,0xffaaaaaa,1); }
}
