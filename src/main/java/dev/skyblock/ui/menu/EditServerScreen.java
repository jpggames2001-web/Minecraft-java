package dev.skyblock.ui.menu;
import dev.skyblock.persistence.ServerLibrary;
import dev.skyblock.network.ServerAddress;
import dev.skyblock.ui.widget.TextField;
public final class EditServerScreen extends MenuScreen {
    private final ServerLibrary.Entry entry; private final TextField name,address;
    public EditServerScreen(MenuContext c,ServerLibrary.Entry entry) { super(c,entry==null?"Add Server":"Edit Server"); this.entry=entry; name=new TextField("name",entry==null?"Minecraft Server":entry.name(),64); address=new TextField("address",entry==null?"localhost":entry.address(),255); focus("name"); submitId="save"; }
    @Override public void layout() {
        text("name",name,width/2-100,60,200); text("address",address,width/2-100,100,200);
        boolean valid=!name.value().isBlank(); try { ServerAddress.parse(address.value()); } catch(IllegalArgumentException e) { valid=false; }
        button("save","Done",width/2-102,height-30,100,()->context.run(()->{context.servers.put(entry,name.value(),address.value());context.stack.pop();})).enabled=valid;
        button("back","Cancel",width/2+2,height-30,100,context.stack::pop);
    }
}
