package dev.skyblock;

import dev.skyblock.audio.SynthAudio;
import dev.skyblock.game.*;
import dev.skyblock.game.Stack;
import dev.skyblock.persistence.SaveStore;
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
    private double cursorX,cursorY,lastMouseX,lastMouseY;
    private boolean firstMouse=true,captured;
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
        GLFWErrorCallback error=GLFWErrorCallback.createPrint(System.err); error.set();
        if(!glfwInit()) { error.free(); throw new IllegalStateException("GLFW initialization failed. A graphical desktop (X11/Wayland or Windows) is required."); }
        try {
            glfwDefaultWindowHints(); glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3); glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3); glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_CORE_PROFILE);
            window=glfwCreateWindow(width,height,"Skyblock Classic - Java 21",NULL,NULL);
            if(window==NULL) throw new IllegalStateException("OpenGL 3.3 window creation failed");
            glfwMakeContextCurrent(window); glfwSwapInterval(options.smoke>0?0:1); GL.createCapabilities();
            System.out.println("OpenGL: "+glGetString(GL_VERSION)+" / "+glGetString(GL_RENDERER));
            callbacks();
            try(SynthAudio audio=new SynthAudio(!options.noAudio && options.smoke==0); Renderer rendering=new Renderer(); Canvas canvas=new Canvas(rendering.assets)) {
                renderer=rendering;
                if(!options.fresh && options.smoke==0 && Files.exists(options.save)) {
                    SaveStore.Saved saved=SaveStore.load(options.save); game=new Game(saved.world(),saved.player(),audio); game.drops.addAll(saved.drops());
                } else game=new Game(World.skyblock(),new Player(),audio);
                game.player.physics=options.physics;
                if(options.creative) game.creativeKit();
                ui=new Interface(game,canvas); game.paused=true;
                if(options.smoke>0) ui.set(Interface.Screen.GAME);
                loop();
                ui.close();
                if(options.smoke==0) { SaveStore.save(options.save,game); System.out.println("World saved: "+options.save.toAbsolutePath()); }
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
            if(captured && game!=null) {
                if(!firstMouse) { game.player.yaw+=(float)((x-lastMouseX)*.15); game.player.pitch=Math.clamp(game.player.pitch+(float)((y-lastMouseY)*.15),-89.9f,89.9f); }
                firstMouse=false; lastMouseX=x; lastMouseY=y;
            }
        });
        glfwSetKeyCallback(window,(w,key,scan,action,mods)->{if(action==GLFW_PRESS) pressed.add(key);});
        glfwSetMouseButtonCallback(window,(w,button,action,mods)->{if(action==GLFW_PRESS) clicks.add(button);});
        glfwSetScrollCallback(window,(w,x,y)->scroll+=y);
        glfwSetWindowFocusCallback(window,(w,focused)->{if(options.smoke==0 && !focused && ui!=null && ui.screen==Interface.Screen.GAME) ui.set(Interface.Screen.PAUSE);});
        try(MemoryStack stack=MemoryStack.stackPush()) {
            IntBuffer a=stack.mallocInt(1),b=stack.mallocInt(1); glfwGetFramebufferSize(window,a,b); width=a.get(0);height=b.get(0);
        }
    }
    private boolean key(int key) { return glfwGetKey(window,key)==GLFW_PRESS; }
    private boolean mouse(int button) { return glfwGetMouseButton(window,button)==GLFW_PRESS; }
    private Controls controls() {
        if(ui.screen!=Interface.Screen.GAME) return Controls.NONE;
        float forward=(key(GLFW_KEY_W)?1:0)-(key(GLFW_KEY_S)?1:0),strafe=(key(GLFW_KEY_D)?1:0)-(key(GLFW_KEY_A)?1:0);
        boolean sneak=key(GLFW_KEY_LEFT_SHIFT) || key(GLFW_KEY_RIGHT_SHIFT);
        if(pressed.contains(GLFW_KEY_W)) {
            if(game.world.time-lastForwardPress<7) sprintTicks=600;
            lastForwardPress=game.world.time;
        }
        if(forward<=0 || sneak || game.player.food<=6 || game.player.onGround && Math.hypot(game.player.vx,game.player.vz)<.001 && sprintTicks>0) sprintTicks=0;
        if(sprintTicks>0) sprintTicks--;
        return new Controls(forward,strafe,key(GLFW_KEY_SPACE),sneak,key(GLFW_KEY_LEFT_CONTROL) || key(GLFW_KEY_RIGHT_CONTROL) || sprintTicks>0);
    }
    private void loop() throws Exception {
        long previous=System.nanoTime(),fpsTime=previous; double accumulator=0;
        int frames=0,fpsFrames=0,fps=0; long lastAutosave=-1;
        while(!glfwWindowShouldClose(window) && !ui.quit) {
            glfwPollEvents();
            if(pressed.contains(GLFW_KEY_ESCAPE)) ui.escape();
            if(pressed.contains(GLFW_KEY_E)) ui.inventoryKey();
            if(pressed.contains(GLFW_KEY_R)) ui.recipeKey();
            if(pressed.contains(GLFW_KEY_F3)) game.debug=!game.debug;
            for(int i=0;i<9;i++) if(pressed.contains(GLFW_KEY_1+i)) game.player.selected=i;
            if(scroll!=0) { game.player.selected=Math.floorMod(game.player.selected-(int)Math.signum(scroll),9); scroll=0; }
            if(pressed.contains(GLFW_KEY_Q) && ui.screen==Interface.Screen.GAME) game.dropHeld();
            boolean wantCapture=ui.screen==Interface.Screen.GAME;
            if(wantCapture!=captured) { captured=wantCapture; firstMouse=true; glfwSetInputMode(window,GLFW_CURSOR,captured?GLFW_CURSOR_DISABLED:GLFW_CURSOR_NORMAL); }
            long now=System.nanoTime();
            accumulator+=Math.min(.25,(now-previous)/1e9); previous=now;
            Controls controls=controls();
            if(options.smoke>0) accumulator=.05;
            while(accumulator>=.05) {
                game.tick(controls,ui.screen==Interface.Screen.GAME && mouse(GLFW_MOUSE_BUTTON_LEFT),ui.screen==Interface.Screen.GAME && mouse(GLFW_MOUSE_BUTTON_RIGHT));
                accumulator-=.05;
                if(game.player.health<=0 && ui.screen!=Interface.Screen.DEAD) ui.set(Interface.Screen.DEAD);
                else if(ui.screen==Interface.Screen.GAME && game.openContainer!=null) ui.set(game.openContainer.furnace?Interface.Screen.FURNACE:Interface.Screen.CHEST);
                else if(ui.screen==Interface.Screen.GAME && game.craftingTable) ui.set(Interface.Screen.CRAFT);
            }
            if(options.smoke>0) {
                if(frames==20) smokeInventory();
                if(frames==40) ui.set(Interface.Screen.GAME);
                if(frames==60) ui.recipeKey();
                if(frames==80) ui.set(Interface.Screen.GAME);
            }
            if(width>0 && height>0) {
                renderer.draw(game,width,height,(float)(accumulator/.05));
                ui.draw(width,height,cursorX*width/Math.max(1,windowWidth),cursorY*height/Math.max(1,windowHeight),fps,renderer.vertices);
                for(int button:clicks) if(ui.screen!=Interface.Screen.GAME) ui.click(button,key(GLFW_KEY_LEFT_SHIFT) || key(GLFW_KEY_RIGHT_SHIFT));
                if(pressed.contains(GLFW_KEY_F2)) {
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
            if(ui.saveRequested) { ui.close(); SaveStore.save(options.save,game); ui.saveRequested=false; }
            if(options.smoke==0 && game.world.time>0 && game.world.time%1200==0 && game.world.time!=lastAutosave && ui.screen==Interface.Screen.GAME) { SaveStore.save(options.save,game); lastAutosave=game.world.time; }
            pressed.clear(); clicks.clear();
        }
    }
    private void smokeInventory() {
        Inventory inventory=game.player.inventory;
        Stack[] original=new Stack[inventory.size()]; for(int i=0;i<original.length;i++) original[i]=inventory.get(i);
        inventory.clear(); inventory.set(0,new Stack(Item.LOG,2)); ui.set(Interface.Screen.INVENTORY);
        int scale=Math.max(1,Math.min(width/640,height/360)),x=width/scale/2-88,y=height/scale/2-83;
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
    private static void require(boolean condition,String behavior) { if(!condition) throw new IllegalStateException("UI smoke failed: "+behavior); }
    private static final class Options {
        Path save=Path.of(System.getProperty("user.home"),".skyblock-classic","world.dat"),screenshot,trace;
        boolean fresh,creative,noAudio,help; int smoke;
        PhysicsProfile physics=PhysicsProfile.SPECIFIED;
        static final String HELP="""
            Skyblock Classic (Java 21 / OpenGL 3.3)
            java -jar target/skyblock-classic-0.1.0.jar [options]
              --save PATH          Save file (default ~/.skyblock-classic/world.dat)
              --fresh              Start a new island; replaces selected save on exit
              --creative           Unlimited placement, no hunger/damage, starter palette
              --physics specified  Supplied constants (default); release-reference also available
              --no-audio           Disable procedural sound
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
                case "--screenshot" -> o.screenshot=Path.of(value(args,++i));
                case "--smoke" -> { o.smoke=Integer.parseInt(value(args,++i)); if(o.smoke<1 || o.smoke>10000) throw new IllegalArgumentException("Smoke frames must be 1..10000"); }
                case "--physics" -> o.physics=switch(value(args,++i)) { case "specified" -> PhysicsProfile.SPECIFIED; case "release-reference" -> PhysicsProfile.RELEASE_REFERENCE; default -> throw new IllegalArgumentException("Unknown physics profile"); };
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
