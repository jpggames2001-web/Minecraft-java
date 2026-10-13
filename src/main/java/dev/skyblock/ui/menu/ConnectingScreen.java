package dev.skyblock.ui.menu;
import dev.skyblock.network.*;
import dev.skyblock.ui.Canvas;
public final class ConnectingScreen extends MenuScreen {
    private final String address;
    private LegacyServerProbe probe;
    public ConnectingScreen(MenuContext c,String address) { super(c,"Connecting to server..."); this.address=address; probe=new LegacyServerProbe(ServerAddress.parse(address)); }
    @Override public void layout() { button("cancel","Cancel",width/2-100,height-30,200,context.stack::pop); }
    @Override public void render(Canvas c,double mx,double my) {
        super.render(c,mx,my); LegacyServerProbe.Result result=probe.result();
        c.centered(address,60,0xffaaaaaa,1);
        if(result==null) c.centered("Pinging legacy server...",85,0xffffffff,1);
        else {
            String message=result.reachable()?result.message()+" ("+result.online()+"/"+result.maximum()+")":result.message();
            if(message.length()>50) message=message.substring(0,50); c.centered(message,85,result.reachable()?0xff99dd99:0xffff8888,1);
            if(result.reachable()) { c.centered("Server answered the legacy ping.",110,0xffaaaaaa,1); c.centered("Multiplayer world login is not supported",126,0xffffaaaa,1); c.centered("by this development build.",138,0xffffaaaa,1); }
        }
    }
    @Override public void close() { super.close(); probe.close(); }
}
