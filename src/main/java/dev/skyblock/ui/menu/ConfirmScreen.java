package dev.skyblock.ui.menu;
import dev.skyblock.ui.Canvas;
public final class ConfirmScreen extends MenuScreen {
    private final String message; private final MenuContext.IOAction action;
    public ConfirmScreen(MenuContext c,String message,MenuContext.IOAction action) { super(c,"Are you sure?"); this.message=message; this.action=action; }
    @Override public void layout() { button("yes","Yes",width/2-102,height/2+30,100,()->context.run(()->{action.run();context.stack.pop();})); button("no","No",width/2+2,height/2+30,100,context.stack::pop); }
    @Override public void render(Canvas c,double mx,double my) { super.render(c,mx,my); c.centered(message,height/2f,0xffffffff,1); }
}
