package dev.skyblock.ui.menu;
import java.util.*;
public final class ScreenStack {
    private final Deque<MenuScreen> screens=new ArrayDeque<>();
    public MenuScreen current() { return screens.peek(); }
    public int size() { return screens.size(); }
    public void push(MenuScreen screen) { screens.push(Objects.requireNonNull(screen)); }
    public void pop() { if(!screens.isEmpty()) screens.pop().close(); }
    public void clear() { while(!screens.isEmpty()) pop(); }
    public void root(MenuScreen screen) { clear(); push(screen); }
}
