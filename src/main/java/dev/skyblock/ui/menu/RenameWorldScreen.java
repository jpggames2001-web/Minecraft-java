package dev.skyblock.ui.menu;
import dev.skyblock.persistence.WorldLibrary;
import dev.skyblock.ui.widget.TextField;
public final class RenameWorldScreen extends MenuScreen {
    private final WorldLibrary.Entry entry; private final TextField name; private final Runnable changed;
    public RenameWorldScreen(MenuContext c,WorldLibrary.Entry entry,Runnable changed) { super(c,"Rename World"); this.entry=entry; this.changed=changed; name=new TextField("name",entry.name(),64); focus("name"); submitId="rename"; }
    @Override public void layout() { text("name",name,width/2-100,80,200); button("rename","Rename",width/2-102,height-30,100,()->context.run(()->{context.worlds.rename(entry,name.value()); context.stack.pop(); changed.run();})).enabled=!name.value().isBlank(); button("back","Cancel",width/2+2,height-30,100,context.stack::pop); }
}
