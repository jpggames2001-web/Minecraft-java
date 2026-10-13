package dev.skyblock;
import dev.skyblock.client.*;
import dev.skyblock.persistence.*;
import dev.skyblock.ui.menu.*;
import dev.skyblock.ui.widget.*;
import dev.skyblock.game.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;
class MenuTest {
    @TempDir Path data;
    MenuContext c; WorldLibrary.Entry opened; boolean quit,resumed,respawned;
    @BeforeEach void setup() throws Exception {
        c=new MenuContext(new ClientOptions(data.resolve("options.properties"),MouseSettingsStore.load(data.resolve("controls.properties"))),new WorldLibrary(data,data.resolve("legacy.dat")),new ServerLibrary(data.resolve("servers.properties")),new TexturePacks(data.resolve("texturepacks")),new MenuContext.Actions() {
            public void openWorld(WorldLibrary.Entry e) { opened=e; c.stack.clear(); }
            public void title() { c.stack.root(new MainMenuScreen(c)); }
            public void quit() { quit=true; }
            public void resume() { resumed=true; c.stack.clear(); }
            public void respawn() { respawned=true; c.stack.clear(); }
            public void texture(TexturePacks.Pack p) { }
            public void exportTextureTemplate() { }
            public void openTextureDirectory() { }
        });
        c.stack.root(new MainMenuScreen(c));
    }
    void prepare() { c.stack.current().prepare(640,360); }
    void click(String id) { prepare(); Widget w=c.stack.current().widget(id); c.stack.current().click(w.x+w.width/2,w instanceof SelectionList?w.y+10:w.y+w.height/2,0); }
    void fill(String id,String text) { click(id); c.stack.current().key(GLFW_KEY_A,GLFW_MOD_CONTROL); if(text.isEmpty()) c.stack.current().key(GLFW_KEY_BACKSPACE,0); else text.codePoints().forEach(cp->c.stack.current().character(cp)); }
    @Test void mainMenuHasAllRequestedEntriesAndEscapeDoesNotQuit() { prepare(); for(String id:new String[]{"singleplayer","multiplayer","packs","options","quit"}) assertNotNull(c.stack.current().widget(id)); c.stack.current().key(GLFW_KEY_ESCAPE,0); assertEquals(1,c.stack.size()); assertFalse(quit); click("quit"); assertTrue(quit); }
    @Test void nestedOptionsBackReturnsThroughTheParentScreens() { click("options"); click("video"); assertEquals(3,c.stack.size()); click("back"); assertInstanceOf(OptionsScreen.class,c.stack.current()); click("back"); assertInstanceOf(MainMenuScreen.class,c.stack.current()); }
    @Test void pauseMenuResumesOrReturnsToTitle() { c.stack.root(new PauseMenuScreen(c)); click("resume"); assertTrue(resumed); c.stack.root(new PauseMenuScreen(c)); click("title"); assertInstanceOf(MainMenuScreen.class,c.stack.current()); }
    @Test void deathButtonsHaveDistinctWorkingActions() { c.stack.root(new DeathScreen(c)); click("respawn"); assertTrue(respawned); c.stack.root(new DeathScreen(c)); click("title"); assertInstanceOf(MainMenuScreen.class,c.stack.current()); }
    @Test void selectedWorldLoadsAndUnselectedActionsStayDisabled() throws Exception { var entry=c.worlds.create("Island","42",WorldLibrary.Type.SKYBLOCK); click("singleplayer"); prepare(); assertFalse(c.stack.current().widget("load").enabled); click("load"); assertNull(opened); click("worlds"); click("load"); assertEquals(entry.id(),opened.id()); }
    @Test void createWorldInputsAndTypeToggleReachRealPersistence() throws Exception { click("singleplayer"); click("create"); fill("name","My World"); fill("seed","17"); click("type"); click("create"); assertEquals("My World",opened.name()); assertEquals(17,opened.seed()); assertEquals(WorldLibrary.Type.DEFAULT,opened.type()); assertTrue(Files.exists(opened.save())); }
    @Test void blankWorldNameDisablesCreation() { click("singleplayer"); click("create"); fill("name",""); prepare(); assertFalse(c.stack.current().widget("create").enabled); click("create"); assertNull(opened); }
    @Test void renameUpdatesSelectedWorldAndCancelDeletePreservesIt() throws Exception { c.worlds.create("Original","1",WorldLibrary.Type.SKYBLOCK); click("singleplayer"); click("worlds"); click("rename"); fill("name","Renamed"); click("rename"); assertEquals("Renamed",c.worlds.list().getFirst().name()); click("worlds"); click("delete"); click("no"); assertEquals(1,c.worlds.list().size()); }
    @Test void confirmedDeleteRemovesOnlySelectedWorld() throws Exception { c.worlds.create("First","1",WorldLibrary.Type.SKYBLOCK); c.worlds.create("Second","2",WorldLibrary.Type.SKYBLOCK); click("singleplayer"); click("worlds"); click("delete"); click("yes"); assertEquals(1,c.worlds.list().size()); }
    @Test void serversCanBeAddedEditedAndDeletedThroughMenus() throws Exception { click("multiplayer"); click("add"); fill("name","Test Server"); fill("address","localhost:25565"); click("save"); assertEquals(1,c.servers.list().size()); click("servers"); click("edit"); fill("name","Renamed Server"); click("save"); assertEquals("Renamed Server",c.servers.list().getFirst().name()); click("servers"); click("delete"); click("yes"); assertTrue(c.servers.list().isEmpty()); }
    @Test void invalidServerAddressesDisableJoinAndSave() { click("multiplayer"); click("direct"); fill("address","localhost:0"); prepare(); assertFalse(c.stack.current().widget("join").enabled); click("back"); click("add"); fill("address","bad address"); prepare(); assertFalse(c.stack.current().widget("save").enabled); }
    @Test void videoOptionsPersistAndGuiScaleChanges() throws Exception { click("options"); click("video"); click("graphics"); click("distance"); click("smoothLighting"); click("anaglyph"); click("guiScale"); click("advancedOpenGL"); click("vsync"); click("fpsLimit"); var reload=new ClientOptions(data.resolve("options.properties"),c.options.mouse); assertEquals("Fast",reload.text("graphics","")); assertEquals("Far",reload.text("distance","")); assertFalse(reload.flag("smoothLighting",true)); assertTrue(reload.flag("anaglyph",false)); assertTrue(reload.flag("advancedOpenGL",false)); assertFalse(reload.flag("vsync",true)); assertEquals(1,reload.guiScale(1280,720)); assertEquals("120",reload.text("fpsLimit","")); }
    @Test void mouseInversionSensitivityAndBindingsAreInteractive() throws Exception { click("options"); click("controls"); click("invertX"); click("invertY"); click("sensitivity"); assertTrue(c.options.mouse.value().invertX()); assertTrue(c.options.mouse.value().invertY()); assertTrue(c.options.mouse.value().sensitivity()>.15); click("bind.FORWARD"); c.stack.current().key(GLFW_KEY_S,0); assertTrue(c.options.conflicts(Binding.FORWARD)); assertTrue(c.options.conflicts(Binding.BACK)); click("clear.FORWARD"); assertEquals(0,c.options.key(Binding.FORWARD)); assertFalse(c.options.conflicts(Binding.BACK)); click("reset"); assertEquals(GLFW_KEY_W,c.options.key(Binding.FORWARD)); assertEquals(MouseSettings.DEFAULT,c.options.mouse.value()); }
    @Test void bindingCanUseMouseAndEscapeCancelsWaitingWithoutLeavingScreen() { click("options"); click("controls"); click("bind.FORWARD"); c.stack.current().key(GLFW_KEY_ESCAPE,0); assertInstanceOf(ControlsScreen.class,c.stack.current()); assertEquals(GLFW_KEY_W,c.options.key(Binding.FORWARD)); click("bind.FORWARD"); c.stack.current().click(0,0,2); assertEquals(-102,c.options.key(Binding.FORWARD)); }
    @Test void scrollRevealsAllBindingsIncludingHotbar() { click("options"); click("controls"); c.stack.current().scroll(-1,0,0); for(int i=0;i<40;i++) c.stack.current().scroll(-1,0,0); prepare(); assertNotNull(c.stack.current().widget("bind.SLOT9")); }
    @Test void audioSlidersAndLanguageSelectionPersist() throws Exception { click("options"); click("sounds"); click("master"); click("music"); click("sound"); assertEquals(.5,c.options.volume("master"),.01); click("back"); click("language"); click("Portugues"); assertEquals("Opcoes",c.tr("Options")); assertEquals("Portugues",new ClientOptions(data.resolve("options.properties"),c.options.mouse).text("language","")); }
    @Test void texturePackMenuDisablesApplyUntilAValidSelection() { click("packs"); prepare(); assertFalse(c.stack.current().widget("apply").enabled); click("packs"); prepare(); assertTrue(c.stack.current().widget("apply").enabled); click("apply"); assertEquals("default",c.options.text("texturePack","")); }
    @Test void stackClosesConnectionWorkOnPop() { c.stack.push(new ConnectingScreen(c,"127.0.0.1:1")); assertEquals(2,c.stack.size()); c.stack.pop(); assertInstanceOf(MainMenuScreen.class,c.stack.current()); }
    @Test void focusedTextInputAcceptsKeyboardSubmission() throws Exception { click("singleplayer"); click("create"); prepare(); c.stack.current().key(GLFW_KEY_A,GLFW_MOD_CONTROL); "Keyboard world".codePoints().forEach(cp->c.stack.current().character(cp)); prepare(); c.stack.current().key(GLFW_KEY_ENTER,0); assertNotNull(opened); assertEquals("Keyboard world",opened.name()); }
}
