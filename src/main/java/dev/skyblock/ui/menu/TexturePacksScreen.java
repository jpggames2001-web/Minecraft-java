package dev.skyblock.ui.menu;
import dev.skyblock.client.TexturePacks;
import dev.skyblock.ui.*;
import dev.skyblock.ui.widget.SelectionList;
import java.util.*;
public final class TexturePacksScreen extends MenuScreen {
    private List<TexturePacks.Pack> packs=List.of(); private TexturePacks.Pack selected;
    private final SelectionList<TexturePacks.Pack> list;
    public TexturePacksScreen(MenuContext c) { super(c,"Texture Packs"); list=new SelectionList<>("packs",()->packs,TexturePacks.Pack::name,TexturePacks.Pack::description,p->selected=p); refresh(); }
    private void refresh() { context.run(()->packs=context.packs.scan()); selected=null; list.selected=-1; }
    @Override public void layout() {
        list.bounds(width/2-154,42,308,Math.max(30,height-126)); widgets.add(list);
        button("apply","Apply",width/2-154,height-76,152,()->context.run(()->{context.actions.texture(selected);context.options.set("texturePack",selected.id());})).enabled=selected!=null && (selected.id().equals("default") || selected.atlas()!=null);
        button("refresh","Refresh",width/2+2,height-76,152,this::refresh);
        button("folder","Open Folder",width/2-154,height-52,152,()->context.run(context.actions::openTextureDirectory));
        button("template","Export Template",width/2+2,height-52,152,()->context.run(()->{context.actions.exportTextureTemplate();refresh();}));
        button("back","Done",width/2-100,height-28,200,context.stack::pop);
    }
    @Override public void render(Canvas c,double mx,double my) { super.render(c,mx,my); c.text("Selected: "+context.options.text("texturePack","default"),8,height-12,0xffaaaaaa,1); if(selected!=null && selected.atlas()!=null) { c.preview(selected.atlas(),width-42,40,32); } }
}
