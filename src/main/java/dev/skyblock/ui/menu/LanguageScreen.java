package dev.skyblock.ui.menu;
public final class LanguageScreen extends MenuScreen {
    public LanguageScreen(MenuContext c) { super(c,"Language"); }
    @Override public void layout() {
        String[] languages={"English","Portugues","Espanol"};
        for(int i=0;i<languages.length;i++) { String language=languages[i]; button(language,()->language+(context.options.text("language","English").equals(language)?" *":""),width/2-100,60+i*24,200,()->context.run(()->context.options.set("language",language))); }
        done();
    }
}
