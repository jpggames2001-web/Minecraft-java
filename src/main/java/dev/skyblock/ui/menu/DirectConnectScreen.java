package dev.skyblock.ui.menu;
import dev.skyblock.network.ServerAddress;
import dev.skyblock.ui.widget.TextField;
public final class DirectConnectScreen extends MenuScreen {
    private final TextField address=new TextField("address","localhost",255);
    public DirectConnectScreen(MenuContext c) { super(c,"Direct Connect"); focus("address"); submitId="join"; }
    @Override public void layout() {
        text("address",address,width/2-100,80,200);
        boolean valid; try { ServerAddress.parse(address.value()); valid=true; } catch(IllegalArgumentException e) { valid=false; }
        button("join","Join Server",width/2-102,height-30,100,()->context.stack.push(new ConnectingScreen(context,address.value()))).enabled=valid;
        button("back","Cancel",width/2+2,height-30,100,context.stack::pop);
    }
}
