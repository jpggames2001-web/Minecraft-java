package dev.skyblock.ui.menu;
import dev.skyblock.client.*;
import dev.skyblock.persistence.*;
import java.io.IOException;
import java.util.function.*;
public final class MenuContext {
    public interface Actions {
        void openWorld(WorldLibrary.Entry world) throws IOException;
        void title() throws IOException;
        void quit();
        void resume();
        void respawn();
        void texture(TexturePacks.Pack pack) throws IOException;
        void exportTextureTemplate() throws IOException;
        void openTextureDirectory() throws IOException;
    }
    @FunctionalInterface public interface IOAction { void run() throws Exception; }
    public final ClientOptions options;
    public final WorldLibrary worlds;
    public final ServerLibrary servers;
    public final TexturePacks packs;
    public final Actions actions;
    public final ScreenStack stack=new ScreenStack();
    public Supplier<String> clipboard=()->"";
    public Consumer<String> copy=s->{};
    public MenuContext(ClientOptions options,WorldLibrary worlds,ServerLibrary servers,TexturePacks packs,Actions actions) { this.options=options; this.worlds=worlds; this.servers=servers; this.packs=packs; this.actions=actions; }
    public void run(IOAction action) { try { action.run(); } catch(Exception e) { stack.push(new DialogScreen(this,"Error",e.getMessage()==null?e.getClass().getSimpleName():e.getMessage())); } }
    public String tr(String text) {
        String language=options.text("language","English");
        if(language.equals("Portugues")) return Translation.PORTUGUESE.getOrDefault(text,text);
        if(language.equals("Espanol")) return Translation.SPANISH.getOrDefault(text,text);
        return text;
    }
}
