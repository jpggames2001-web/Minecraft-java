package dev.skyblock;

import dev.skyblock.audio.SynthAudio;
import dev.skyblock.game.*;
import dev.skyblock.game.Stack;
import dev.skyblock.persistence.SaveStore;
import dev.skyblock.persistence.MouseSettingsStore;
import dev.skyblock.persistence.*;
import dev.skyblock.client.*;
import dev.skyblock.ui.menu.*;
import dev.skyblock.render.Renderer;
import dev.skyblock.ui.*;
import dev.skyblock.world.World;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import java.nio.IntBuffer;
import java.nio.file.*;
import java.util.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class Main {
    private long window;
    private int width=1280,height=720,windowWidth=1280,windowHeight=720;
    private double cursorX,cursorY;
    private boolean captured;
    private final MouseLook mouseLook=new MouseLook();
    private MouseSettingsStore mouseSettings;
    private Path activeSettingsPath,currentSave;
    private ClientOptions clientOptions;
    private boolean sessionStarted;
    private SynthAudio synth;
    private int vsync=-1;

    private final Set<Integer> pressed=new HashSet<>();
    private final List<Integer> clicks=new ArrayList<>();
    private double scroll;
    private final Options options;
    private Game game;
    private Interface ui;
    private Renderer renderer;
    private int sprintTicks;
    private long lastForwardPress=-100;
    private Main(Options options) { this.options=options; }
    public static void main(String[] args) {
        try { Options options=Options.parse(args); if(options.help) { System.out.println(Options.HELP); return; } if(options.trace!=null) { PhysicsTrace.write(options.trace,options.physics); System.out.println("Physics trace: "+options.trace.toAbsolutePath()); return; } new Main(options).run(); }
        catch(Exception e) { System.err.println("Unable to run Skyblock Classic: "+e.getMessage()); e.printStackTrace(System.err); System.exit(1); }
    }
    private void run() throws Exception {
        Path temporarySettings=options.smoke>0?Files.createTempDirectory("skyblock-smoke-controls-"):null;
        try {
            activeSettingsPath=temporarySettings==null?options.settings:temporarySettings.resolve("controls.properties");
            mouseSettings=MouseSettingsStore.load(activeSettingsPath);
            runDesktop();
        } finally {
            if(temporarySettings!=null) {
                try(var paths=Files.walk(temporarySettings)) { for(Path path:paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); }
            }
        }
    }
    private void runDesktop() throws Exception {
        GLFWErrorCallback error=GLFWErrorCallback.createPrint(System.err); error.set();
        if(!glfwInit()) { error.free(); throw new IllegalStateException("GLFW initialization failed. A graphical desktop (X11/Wayland or Windows) is required."); }
        try {
            glfwDefaultWindowHints(); glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3); glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3); glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_CORE_PROFILE);
            window=glfwCreateWindow(width,height,"Skyblock Classic - Java 21",NULL,NULL);
            if(window==NULL) throw new IllegalStateException("OpenGL 3.3 window creation failed");
            glfwSetWindowSizeLimits(window,320,240,GLFW_DONT_CARE,GLFW_DONT_CARE);
            glfwMakeContextCurrent(window); glfwSwapInterval(options.smoke>0?0:1); GL.createCapabilities();
            System.out.println("OpenGL: "+glGetString(GL_VERSION)+" / "+glGetString(GL_RENDERER));
            callbacks();
            try(SynthAudio audio=new SynthAudio(!options.noAudio && options.smoke==0); Renderer rendering=new Renderer(); Canvas canvas=new Canvas(rendering.assets)) {
                renderer=rendering; synth=audio; currentSave=options.save;
                Path data=activeSettingsPath.toAbsolutePath().getParent();
                clientOptions=new ClientOptions(data.resolve("options.properties"),mouseSettings);
                WorldLibrary library=new WorldLibrary(data,options.smoke>0?data.resolve("legacy-world.dat"):options.save);
                TexturePacks packs=new TexturePacks(data.resolve("texturepacks"));

                if(!options.fresh && options.smoke==0 && Files.exists(options.save)) {
                    SaveStore.Saved saved=SaveStore.load(options.save); game=new Game(saved.world(),saved.player(),audio); game.drops.addAll(saved.drops());
                } else game=new Game(World.skyblock(),new Player(),audio);
                game.player.physics=options.physics;
                if(options.creative) game.creativeKit();
                if(options.fresh && options.smoke==0) SaveStore.save(options.save,game);
                MenuContext menus=new MenuContext(clientOptions,library,new ServerLibrary(data.resolve("servers.properties")),packs,new MenuContext.Actions() {
                    public void openWorld(WorldLibrary.Entry entry) throws java.io.IOException {
                        if(sessionStarted) { ui.close(); SaveStore.save(currentSave,game); }
                        var loaded=SaveStore.load(entry.save()); game=new Game(loaded.world(),loaded.player(),audio); game.drops.addAll(loaded.drops());
                        if(options.creative) game.creativeKit(); currentSave=entry.save(); sessionStarted=true; ui.replaceGame(game);
                    }
                    public void title() throws java.io.IOException { if(sessionStarted) { ui.close(); SaveStore.save(currentSave,game); sessionStarted=false; } ui.set(Interface.Screen.TITLE); }
                    public void quit() { ui.quit=true; }
                    public void resume() { ui.set(Interface.Screen.GAME); }
                    public void respawn() { game.player.respawn(); ui.set(Interface.Screen.GAME); }
                    public void texture(TexturePacks.Pack pack) { renderer.assets.apply(pack.atlas()); }
                    public void exportTextureTemplate() throws java.io.IOException {
                        Path directory; int n=0;
                        do { directory=packs.directory().resolve("generated-template-"+n++); } while(Files.exists(directory));
                        Files.createDirectory(directory); javax.imageio.ImageIO.write(renderer.assets.proceduralImage(),"png",directory.resolve("atlas.png").toFile());
                        Files.writeString(directory.resolve("pack.txt"),"Original procedural atlas template. Edit atlas.png to create your pack.");
                    }
                    public void openTextureDirectory() throws java.io.IOException {
                        if(!java.awt.Desktop.isDesktopSupported() || !java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) throw new java.io.IOException("Texture folder: "+packs.directory());
                        java.awt.Desktop.getDesktop().open(packs.directory().toFile());
                    }
                });
                menus.clipboard=()->glfwGetClipboardString(window); menus.copy=text->glfwSetClipboardString(window,text);
                ui=new Interface(game,canvas,mouseSettings,menus); game.paused=true;
                for(var pack:packs.scan()) if(pack.id().equals(clientOptions.text("texturePack","default")) && pack.atlas()!=null) renderer.assets.apply(pack.atlas());

                if(options.smoke>0) ui.set(Interface.Screen.GAME);
                loop();
                ui.close();
                if(options.smoke==0 && sessionStarted) { SaveStore.save(currentSave,game); System.out.println("World saved: "+currentSave.toAbsolutePath()); }
            }
        } finally {
            if(window!=NULL) { Callbacks.glfwFreeCallbacks(window); glfwDestroyWindow(window); }
            glfwTerminate(); GLFWErrorCallback callback=glfwSetErrorCallback(null); if(callback!=null) callback.free();
        }
    }
    private void callbacks() {
        glfwSetFramebufferSizeCallback(window,(w,x,y)->{width=x;height=y;});
        glfwSetWindowSizeCallback(window,(w,x,y)->{windowWidth=x;windowHeight=y;});
        glfwSetCursorPosCallback(window,(w,x,y)-> {
            cursorX=x; cursorY=y;
            if(game!=null && ui!=null && ui.screen==Interface.Screen.GAME) mouseLook.cursor(game.player,x,y,mouseSettings.value());
        });
        glfwSetKeyCallback(window,(w,key,scan,action,mods)->{if(action==GLFW_PRESS || action==GLFW_REPEAT) { if(ui!=null && ui.menuActive()) ui.keyEvent(key,mods); else if(action==GLFW_PRESS) pressed.add(key); }});
        glfwSetCharCallback(window,(w,cp)->{if(ui!=null) ui.character(cp);});
        glfwSetMouseButtonCallback(window,(w,button,action,mods)->{if(action==GLFW_PRESS) clicks.add(button);});
        glfwSetScrollCallback(window,(w,x,y)->scroll+=y);
        glfwSetWindowFocusCallback(window,(w,focused)->{if(options.smoke==0 && !focused && ui!=null && ui.screen==Interface.Screen.GAME) ui.set(Interface.Screen.PAUSE);});
        try(MemoryStack stack=MemoryStack.stackPush()) {
            IntBuffer a=stack.mallocInt(1),b=stack.mallocInt(1); glfwGetFramebufferSize(window,a,b); width=a.get(0);height=b.get(0);
        }
    }
    private boolean key(int key) { return glfwGetKey(window,key)==GLFW_PRESS; }
    private boolean mouse(int button) { return glfwGetMouseButton(window,button)==GLFW_PRESS; }
    private boolean down(Binding binding) { int code=clientOptions.key(binding); return code<=-100?mouse(-100-code):code!=0 && key(code); }
    private boolean triggered(Binding binding) { int code=clientOptions.key(binding); return code<=-100?clicks.contains(-100-code):code!=0 && pressed.contains(code); }
    private void pickBlock() {
        if(game.target==null) return;
        Item item=Item.fromBlock(game.world.get(game.target.x(),game.target.y(),game.target.z())); if(item==null) return;
        for(int slot=0;slot<9;slot++) if(game.player.inventory.get(slot)!=null && game.player.inventory.get(slot).item()==item) { game.player.selected=slot; return; }
        if(game.player.creative) game.player.inventory.set(game.player.selected,new Stack(item,item.limit));
    }
    private Controls controls() {
        if(ui.screen!=Interface.Screen.GAME) return Controls.NONE;
        float forward=(down(Binding.FORWARD)?1:0)-(down(Binding.BACK)?1:0),strafe=(down(Binding.RIGHT)?1:0)-(down(Binding.LEFT)?1:0);
        boolean sneak=down(Binding.SNEAK);
        if(triggered(Binding.FORWARD)) {
            if(game.world.time-lastForwardPress<7) sprintTicks=600;
            lastForwardPress=game.world.time;
        }
        if(forward<=0 || sneak || game.player.food<=6 || game.player.onGround && Math.hypot(game.player.vx,game.player.vz)<.001 && sprintTicks>0) sprintTicks=0;
        if(sprintTicks>0) sprintTicks--;
        return new Controls(forward,strafe,down(Binding.JUMP),sneak,down(Binding.SPRINT) || sprintTicks>0);
    }
    private void loop() throws Exception {
        long previous=System.nanoTime(),fpsTime=previous; double accumulator=0;
        int frames=0,fpsFrames=0,fps=0; long lastAutosave=-1;
        while(!glfwWindowShouldClose(window) && !ui.quit) {
            glfwPollEvents();
            if(pressed.contains(GLFW_KEY_ESCAPE)) ui.escape();
            if(triggered(Binding.INVENTORY)) ui.inventoryKey();
            if(triggered(Binding.RECIPE)) ui.recipeKey();
            if(triggered(Binding.DEBUG)) game.debug=!game.debug;
            for(int i=0;i<9;i++) if(triggered(Binding.values()[Binding.SLOT1.ordinal()+i])) game.player.selected=i;
            if(scroll!=0) { if(ui.menuActive()) ui.scrollMenu(scroll); else game.player.selected=Math.floorMod(game.player.selected-(int)Math.signum(scroll),9); scroll=0; }
            if(triggered(Binding.DROP) && ui.screen==Interface.Screen.GAME) game.dropHeld();
            if(triggered(Binding.PICK) && ui.screen==Interface.Screen.GAME) pickBlock();
            boolean wantCapture=ui.screen==Interface.Screen.GAME;
            if(wantCapture!=captured) { captured=wantCapture; mouseLook.capture(captured); glfwSetInputMode(window,GLFW_CURSOR,captured?GLFW_CURSOR_DISABLED:GLFW_CURSOR_NORMAL); }
            long now=System.nanoTime();
            accumulator+=Math.min(.25,(now-previous)/1e9); previous=now;
            Controls controls=controls();
            if(options.smoke>0) accumulator=.05;
            while(accumulator>=.05) {
                game.tick(controls,ui.screen==Interface.Screen.GAME && down(Binding.ATTACK),ui.screen==Interface.Screen.GAME && down(Binding.USE));
                accumulator-=.05;
                if(ui.playingWorld() && game.player.health<=0 && ui.screen!=Interface.Screen.DEAD) ui.set(Interface.Screen.DEAD);
                else if(ui.screen==Interface.Screen.GAME && game.openContainer!=null) ui.set(game.openContainer.furnace?Interface.Screen.FURNACE:Interface.Screen.CHEST);
                else if(ui.screen==Interface.Screen.GAME && game.craftingTable) ui.set(Interface.Screen.CRAFT);
            }
            if(options.smoke>0) {
                if(frames==20) smokeInventory();
                if(frames==40) ui.set(Interface.Screen.GAME);
                if(frames==60) { smokeControls(); if(options.menuSmoke) smokeMenus(); }
                if(frames==80) ui.set(Interface.Screen.GAME);
            }
            if(width>0 && height>0) {
                renderer.options=clientOptions;
                if(options.smoke==0 && vsync!=(clientOptions.flag("vsync",true)?1:0)) { vsync=clientOptions.flag("vsync",true)?1:0; glfwSwapInterval(vsync); }
                synth.volumes(clientOptions.volume("master"),clientOptions.volume("sound"),clientOptions.volume("music")); synth.musicTick(System.nanoTime()/50_000_000L);
                renderer.draw(game,width,height,(float)(accumulator/.05));
                ui.draw(width,height,cursorX*width/Math.max(1,windowWidth),cursorY*height/Math.max(1,windowHeight),fps,renderer.vertices);
                for(int button:clicks) if(ui.screen!=Interface.Screen.GAME) ui.click(button,key(GLFW_KEY_LEFT_SHIFT) || key(GLFW_KEY_RIGHT_SHIFT));
                ui.dragMenu(mouse(GLFW_MOUSE_BUTTON_LEFT));
                if(triggered(Binding.SCREENSHOT)) {
                    Path path=options.save.toAbsolutePath().getParent().resolve("screenshots/skyblock-"+System.currentTimeMillis()+".png"); renderer.screenshot(path,width,height); game.message("SCREENSHOT SAVED"); System.out.println("Screenshot: "+path);
                }
                int error=glGetError(); if(error!=GL_NO_ERROR) throw new IllegalStateException("OpenGL error 0x"+Integer.toHexString(error));
                frames++; fpsFrames++;
                if(options.smoke>0 && frames>=options.smoke) {
                    if(renderer.vertices<=0 || game.world.time!=options.smoke-Math.max(0,Math.min(20,options.smoke-61)) || game.player.health<=0) throw new IllegalStateException("Graphical smoke check failed");
                    if(options.screenshot!=null) renderer.screenshot(options.screenshot,width,height);
                    System.out.println("SMOKE PASS frames="+frames+" ticks="+game.world.time+" vertices="+renderer.vertices+" health="+game.player.health);
                    return;
                }
                glfwSwapBuffers(window);
            } else glfwWaitEventsTimeout(.1);
            if(now-fpsTime>=1_000_000_000L) { fps=fpsFrames; fpsFrames=0; fpsTime=now; }
            if(ui.saveRequested) { ui.close(); SaveStore.save(currentSave,game); ui.saveRequested=false; }
            if(options.smoke==0 && game.world.time>0 && game.world.time%1200==0 && game.world.time!=lastAutosave && sessionStarted && ui.screen==Interface.Screen.GAME) { SaveStore.save(currentSave,game); lastAutosave=game.world.time; }
            if(options.smoke==0) { int limit=(int)clientOptions.number("fpsLimit",0); if(limit>0) java.util.concurrent.locks.LockSupport.parkNanos(Math.max(0,1_000_000_000L/limit-(System.nanoTime()-now))); }
            pressed.clear(); clicks.clear();
        }
    }
    private void smokeInventory() {
        Inventory inventory=game.player.inventory;
        Stack[] original=new Stack[inventory.size()]; for(int i=0;i<original.length;i++) original[i]=inventory.get(i);
        inventory.clear(); inventory.set(0,new Stack(Item.LOG,2)); ui.set(Interface.Screen.INVENTORY);
        int scale=ui.guiScale(width,height),x=width/scale/2-88,y=height/scale/2-83;
        smokeClick(x+16,y+150,0,false,scale);
        require(ui.carried!=null && ui.carried.item()==Item.LOG && ui.carried.count()==2 && inventory.get(0)==null,"take hotbar stack");
        smokeClick(x+90,y+26,1,false,scale);
        require(ui.carried.count()==1,"place one ingredient");
        smokeClick(x+16,y+150,0,false,scale);
        smokeClick(x+151,y+44,0,false,scale);
        require(ui.carried!=null && ui.carried.item()==Item.PLANKS && ui.carried.count()==4,"craft recipe output");
        smokeClick(x+34,y+150,0,false,scale);
        smokeClick(x+34,y+150,1,false,scale);
        require(ui.carried.count()==2 && inventory.get(1).count()==2,"split stack");
        smokeClick(x+34,y+150,0,true,scale);
        require(inventory.get(1)==null,"shift transfer");
        ui.set(Interface.Screen.GAME);
        int planks=0; for(int i=0;i<inventory.size();i++) if(inventory.get(i)!=null && inventory.get(i).item()==Item.PLANKS) planks+=inventory.get(i).count();
        require(planks==4 && inventory.get(0).item()==Item.LOG && inventory.get(0).count()==1 && ui.carried==null,"cursor/grid conservation on close");
        for(int i=0;i<original.length;i++) inventory.set(i,original[i]);
        ui.set(Interface.Screen.INVENTORY);
        System.out.println("UI SMOKE PASS take/place/split/transfer/craft/close");
    }
    private void smokeClick(int x,int y,int button,boolean shift,int scale) { ui.draw(width,height,x*scale,y*scale,0,renderer.vertices); ui.click(button,shift); }
    private void smokeControlClick(String id) { ui.draw(width,height,0,0,0,renderer.vertices); int[] point=ui.menuCenter(id); smokeClick(point[0],point[1],0,false,ui.guiScale(width,height)); }
    private void smokeControls() throws Exception {
        ui.escape(); require(ui.screen==Interface.Screen.PAUSE && game.paused,"ESC pauses gameplay");
        smokeControlClick("options"); smokeControlClick("controls");
        require(ui.screen==Interface.Screen.CONTROLS && game.paused,"open nested controls");
        smokeControlClick("invertX"); smokeControlClick("invertY"); smokeControlClick("sensitivity");
        MouseSettings changed=mouseSettings.value();
        require(changed.invertX() && changed.invertY() && changed.sensitivity()>.15,"mouse options");
        require(MouseSettingsStore.load(activeSettingsPath).value().equals(changed),"persistent mouse settings");
        smokeControlClick("bind.FORWARD"); ui.keyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_UP,0);
        require(clientOptions.key(Binding.FORWARD)==org.lwjgl.glfw.GLFW.GLFW_KEY_UP,"rebind forward");
        renderer.draw(game,width,height,1); ui.draw(width,height,0,0,0,renderer.vertices); renderer.screenshot(Path.of("target/smoke-controls.png"),width,height);
        smokeControlClick("reset"); require(clientOptions.key(Binding.FORWARD)==Binding.FORWARD.defaultCode,"reset bindings");
        ui.escape(); require(ui.screen==Interface.Screen.MENU,"controls returns to options");
        ui.escape(); require(ui.screen==Interface.Screen.PAUSE,"options returns to pause");
        System.out.println("CONTROLS SMOKE PASS nested-menu/inversion/sensitivity/rebinding/persistence/reset");
    }
    private void menuSnapshot(String name) throws Exception {
        renderer.draw(game,width,height,1); ui.draw(width,height,0,0,0,renderer.vertices); renderer.screenshot(Path.of("target/menu-"+name+".png"),width,height);
        require(glGetError()==GL_NO_ERROR,"menu render "+name);
    }
    private void smokeFill(String id,String text) { smokeControlClick(id); ui.keyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_A,org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL); ui.keyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE,0); text.codePoints().forEach(ui::character); }
    private void smokeMenus() throws Exception {
        Game initial=game; Path path=currentSave; boolean started=sessionStarted;
        ui.set(Interface.Screen.TITLE); menuSnapshot("title"); smokeControlClick("singleplayer"); menuSnapshot("worlds");
        smokeControlClick("create"); smokeFill("name","Smoke island"); smokeFill("seed","42"); menuSnapshot("create"); smokeControlClick("create");
        require(ui.screen==Interface.Screen.GAME && game!=initial,"world creation starts real game");
        ui.escape(); smokeControlClick("title"); smokeControlClick("singleplayer");
        ui.draw(width,height,0,0,0,renderer.vertices); var list=ui.menus.stack.current().widget("worlds"); smokeClick(list.x+10,list.y+10,0,false,ui.guiScale(width,height));
        smokeControlClick("rename"); smokeFill("name","Renamed island"); menuSnapshot("rename"); smokeControlClick("rename");
        require(ui.menus.worlds.list().getFirst().name().equals("Renamed island"),"world rename");
        smokeClick(list.x+10,list.y+10,0,false,ui.guiScale(width,height)); smokeControlClick("delete"); menuSnapshot("delete"); smokeControlClick("yes");
        require(ui.menus.worlds.list().isEmpty(),"world deletion"); ui.escape(); smokeControlClick("multiplayer"); smokeControlClick("add"); smokeFill("name","Local test"); smokeFill("address","127.0.0.1:1"); menuSnapshot("server-edit"); smokeControlClick("save"); menuSnapshot("servers");
        var servers=ui.menus.stack.current().widget("servers"); smokeClick(servers.x+10,servers.y+10,0,false,ui.guiScale(width,height)); smokeControlClick("join"); menuSnapshot("connection"); ui.escape(); smokeControlClick("direct"); menuSnapshot("direct"); ui.escape(); ui.escape();
        smokeControlClick("options"); menuSnapshot("options"); smokeControlClick("video"); smokeControlClick("graphics"); smokeControlClick("smoothLighting"); smokeControlClick("anaglyph"); smokeControlClick("advancedOpenGL"); menuSnapshot("video"); ui.escape();
        smokeControlClick("sounds"); smokeControlClick("master"); menuSnapshot("sounds"); ui.escape(); smokeControlClick("language"); smokeControlClick("Portugues"); menuSnapshot("language"); smokeControlClick("English"); ui.escape(); smokeControlClick("packs"); smokeControlClick("template"); menuSnapshot("packs");
        var packs=ui.menus.stack.current().widget("packs"); smokeClick(packs.x+10,packs.y+40,0,false,ui.guiScale(width,height)); smokeControlClick("apply"); require(!clientOptions.text("texturePack","default").equals("default"),"local pack switch");
        ui.escape(); ui.escape(); game=initial; currentSave=path; sessionStarted=started; ui.replaceGame(game); game.player.health=0; ui.set(Interface.Screen.DEAD); menuSnapshot("death"); smokeControlClick("title"); require(!ui.playingWorld() && ui.screen==Interface.Screen.TITLE,"dead player can return to title"); ui.set(Interface.Screen.DEAD); smokeControlClick("respawn"); require(game.player.health==20 && ui.screen==Interface.Screen.GAME,"death respawn"); ui.set(Interface.Screen.PAUSE);
        System.out.println("MENUS SMOKE PASS worlds/servers/video/anaglyph/audio/language/packs/death");
    }
    private static void require(boolean condition,String behavior) { if(!condition) throw new IllegalStateException("UI smoke failed: "+behavior); }
    private static final class Options {
        Path save=Path.of(System.getProperty("user.home"),".skyblock-classic","world.dat"),screenshot,trace;
        Path settings=Path.of(System.getProperty("user.home"),".skyblock-classic","controls.properties");
        boolean fresh,creative,noAudio,help,menuSmoke; int smoke;
        PhysicsProfile physics=PhysicsProfile.SPECIFIED;
        static final String HELP="""
            Skyblock Classic (Java 21 / OpenGL 3.3)
            java -jar target/skyblock-classic-0.1.0.jar [options]
              --save PATH          Save file (default ~/.skyblock-classic/world.dat)
              --settings PATH      Mouse settings (default ~/.skyblock-classic/controls.properties)
              --fresh              Start a new island; replaces selected save on exit
              --creative           Unlimited placement, no hunger/damage, starter palette
              --physics specified  Supplied constants (default); release-reference also available
              --no-audio           Disable procedural sound
              --smoke-menus        Exercise world/server/options/pack menus during smoke run
              --smoke FRAMES       Render/validate fixed ticks without loading or saving a world
              --screenshot PATH    Write final smoke framebuffer to PNG
              --trace PATH         Write a deterministic 160-tick CSV without graphics
              --help               Show this text
            Controls: WASD, mouse, Space, Shift, Ctrl, E, Q, 1-9, wheel, R, F2, F3, Esc.
            """;
        static Options parse(String[] args) {
            Options o=new Options();
            for(int i=0;i<args.length;i++) switch(args[i]) {
                case "--trace" -> o.trace=Path.of(value(args,++i));
                case "--save" -> o.save=Path.of(value(args,++i));
                case "--settings" -> o.settings=Path.of(value(args,++i));
                case "--screenshot" -> o.screenshot=Path.of(value(args,++i));
                case "--smoke" -> { o.smoke=Integer.parseInt(value(args,++i)); if(o.smoke<1 || o.smoke>10000) throw new IllegalArgumentException("Smoke frames must be 1..10000"); }
                case "--physics" -> o.physics=switch(value(args,++i)) { case "specified" -> PhysicsProfile.SPECIFIED; case "release-reference" -> PhysicsProfile.RELEASE_REFERENCE; default -> throw new IllegalArgumentException("Unknown physics profile"); };
                case "--smoke-menus" -> o.menuSmoke=true;
                case "--fresh" -> o.fresh=true;
                case "--creative" -> o.creative=true;
                case "--no-audio" -> o.noAudio=true;
                case "--help", "-h" -> o.help=true;
                default -> throw new IllegalArgumentException("Unknown option: "+args[i]);
            }
            return o;
        }
        static String value(String[] args,int i) { if(i>=args.length) throw new IllegalArgumentException("Missing option value"); return args[i]; }
    }
}
