package dev.skyblock.ui.menu;
import dev.skyblock.persistence.WorldLibrary;
import dev.skyblock.ui.widget.SelectionList;
import java.util.*;
public final class SelectWorldScreen extends MenuScreen {
    private List<WorldLibrary.Entry> entries=List.of(); private WorldLibrary.Entry selected;
    private final SelectionList<WorldLibrary.Entry> list;
    public SelectWorldScreen(MenuContext c) { super(c,"Select World"); list=new SelectionList<>("worlds",()->entries,WorldLibrary.Entry::name,e->e.type()+" / "+new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(e.modified())),e->selected=e); refresh(); }
    private void refresh() { context.run(()->entries=context.worlds.list()); selected=null; list.selected=-1; }
    @Override public void layout() {
        list.bounds(width/2-155,40,310,Math.max(30,height-116)); widgets.add(list); int y=height-68;
        button("load","Play Selected World",width/2-154,y,152,()->context.run(()->context.actions.openWorld(selected))).enabled=selected!=null;
        button("create","Create New World",width/2+2,y,152,()->context.stack.push(new CreateWorldScreen(context)));
        button("rename","Rename",width/2-154,y+24,74,()->context.stack.push(new RenameWorldScreen(context,selected,this::refresh))).enabled=selected!=null;
        button("delete","Delete",width/2-76,y+24,74,()->context.stack.push(new ConfirmScreen(context,"Delete "+selected.name()+"?",()->{context.worlds.delete(selected);refresh();}))).enabled=selected!=null;
        button("back","Cancel",width/2+2,y+24,152,context.stack::pop);
    }
}
