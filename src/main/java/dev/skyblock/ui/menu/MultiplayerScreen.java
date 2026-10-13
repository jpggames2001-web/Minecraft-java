package dev.skyblock.ui.menu;
import dev.skyblock.persistence.ServerLibrary;
import dev.skyblock.ui.widget.SelectionList;
public final class MultiplayerScreen extends MenuScreen {
    private ServerLibrary.Entry selected;
    private final SelectionList<ServerLibrary.Entry> list;
    public MultiplayerScreen(MenuContext c) { super(c,"Multiplayer"); list=new SelectionList<>("servers",c.servers::list,ServerLibrary.Entry::name,ServerLibrary.Entry::address,e->selected=e); }
    @Override public void layout() {
        list.bounds(width/2-155,40,310,Math.max(30,height-116)); widgets.add(list); int y=height-68;
        button("join","Join Server",width/2-154,y,100,()->context.stack.push(new ConnectingScreen(context,selected.address()))).enabled=selected!=null;
        button("direct","Direct Connect",width/2-50,y,100,()->context.stack.push(new DirectConnectScreen(context)));
        button("add","Add Server",width/2+54,y,100,()->context.stack.push(new EditServerScreen(context,null)));
        button("edit","Edit",width/2-154,y+24,74,()->context.stack.push(new EditServerScreen(context,selected))).enabled=selected!=null;
        button("delete","Delete",width/2-76,y+24,74,()->context.stack.push(new ConfirmScreen(context,"Delete "+selected.name()+"?",()->{context.servers.delete(selected);selected=null;list.selected=-1;}))).enabled=selected!=null;
        button("back","Cancel",width/2+2,y+24,152,context.stack::pop);
    }
}
