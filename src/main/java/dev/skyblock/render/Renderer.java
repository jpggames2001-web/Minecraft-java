package dev.skyblock.render;

import dev.skyblock.game.*;
import dev.skyblock.game.Stack;
import dev.skyblock.world.*;
import org.joml.Matrix4f;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.*;
import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.opengl.GL33C.*;

public final class Renderer implements AutoCloseable {
    private final Shader shader=new Shader("""
        #version 330 core
        layout(location=0) in vec3 position; layout(location=1) in vec2 uv;
        layout(location=2) in vec3 tint; layout(location=3) in float skylight; layout(location=4) in float blocklight;
        uniform mat4 projection; uniform mat4 view;
        out vec2 texcoord; out vec3 color; out float sky; out float lamp; out float distanceToEye;
        void main() { vec4 eye=view*vec4(position,1); gl_Position=projection*eye; texcoord=uv; color=tint; sky=skylight; lamp=blocklight; distanceToEye=length(eye.xyz); }
        ""","""
        #version 330 core
        in vec2 texcoord; in vec3 color; in float sky; in float lamp; in float distanceToEye;
        uniform sampler2D atlas; uniform float fastLeaves; uniform float daylight; uniform vec3 fogColor; uniform float fogStart; uniform float fogEnd;
        out vec4 result;
        void main() {
            vec4 tex=texture(atlas,texcoord); if(fastLeaves>.5 && texcoord.x<.125 && texcoord.y>.125 && texcoord.y<.25) tex.a=1; if(tex.a<0.1) discard;
            float level=max(sky*daylight,lamp); float brightness=0.035+0.965*pow(0.8,15.0*(1.0-level));
            vec3 lit=tex.rgb*color*brightness; float fog=smoothstep(fogStart,fogEnd,distanceToEye);
            result=vec4(mix(lit,fogColor,fog),tex.a);
        }
        """);
    public final Assets assets=new Assets();
    private final Mesh[] opaque=new Mesh[16],transparent=new Mesh[16];
    private final Mesh dynamic=new Mesh(),celestialMesh=new Mesh();
    private long revision=Long.MIN_VALUE;
    private World renderedWorld;
    private String meshOptions="";
    public dev.skyblock.client.ClientOptions options;
    private float eyeOffset;

    public int vertices;
    public Renderer() { for(int i=0;i<16;i++) { opaque[i]=new Mesh(); transparent[i]=new Mesh(); } }
    public void draw(Game game,int width,int height,float alpha) {
        boolean stereo=options!=null && options.flag("anaglyph",false);
        if(stereo) { glColorMask(true,false,false,true); eyeOffset=-.035f; drawPass(game,width,height,alpha); glColorMask(false,true,true,true); eyeOffset=.035f; drawPass(game,width,height,alpha); glColorMask(true,true,true,true); }
        else { eyeOffset=0; drawPass(game,width,height,alpha); }
    }
    private void drawPass(Game game,int width,int height,float alpha) {
        World w=game.world; Player p=game.player;
        if(game.paused) alpha=1;
        boolean fancy=options==null || !options.text("graphics","Fancy").equals("Fast"),smooth=options==null || options.flag("smoothLighting",true);
        String currentOptions=""+fancy+smooth;
        if(renderedWorld!=w || revision!=w.revision || !meshOptions.equals(currentOptions)) {
            w.updateLighting(); vertices=0;
            for(int i=0;i<16;i++) {
                Mesh.Builder a=new Mesh.Builder(),b=new Mesh.Builder(); WorldMesher.build(w,i%4,i/4,a,b,fancy,smooth);
                opaque[i].upload(a); transparent[i].upload(b); vertices+=opaque[i].vertexCount()+transparent[i].vertexCount();
            }
            revision=w.revision; renderedWorld=w; meshOptions=currentOptions;
        }
        float cycle=(w.time%24000+alpha)/24000f;
        float celestial=cycle-.25f; if(celestial<0) celestial++; if(celestial>1) celestial--;
        float original=celestial; celestial=1-(float)((Math.cos(celestial*Math.PI)+1)/2); celestial=original+(celestial-original)/3;
        float day=Math.clamp((float)(Math.cos(celestial*Math.PI*2)*2+.5),0,1);
        float ambient=.12f+day*.88f;
        int skyColor=w.biome((int)p.x,(int)p.z).skyColor();
        Block eye=w.get((int)Math.floor(p.x),(int)Math.floor(p.eyeY()),(int)Math.floor(p.z));
        float r=(skyColor>>16&255)/255f*ambient,g=(skyColor>>8&255)/255f*ambient,b=(skyColor&255)/255f*ambient;
        if(eye==Block.WATER) { r=.025f; g=.08f; b=.21f; } else if(eye==Block.LAVA) { r=.6f; g=.12f; b=.015f; }
        glViewport(0,0,width,height); glClearColor(r,g,b,1); glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST); glDisable(GL_CULL_FACE); glEnable(GL_BLEND); glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);
        shader.use(); shader.scalar("fastLeaves",fancy?0:1); glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D,assets.texture);
        double px=p.prevX+(p.x-p.prevX)*alpha,py=p.prevY+(p.y-p.prevY)*alpha+(p.sneaking?1.54:1.62),pz=p.prevZ+(p.z-p.prevZ)*alpha;
        double[] dir=p.direction(); double yaw=Math.toRadians(p.yaw); px+=Math.cos(yaw)*eyeOffset; pz+=Math.sin(yaw)*eyeOffset;
        int distance=options==null?96:options.distance();
        Matrix4f view=new Matrix4f().lookAt((float)px,(float)py,(float)pz,(float)(px+dir[0]),(float)(py+dir[1]),(float)(pz+dir[2]),0,1,0);
        Matrix4f projection=new Matrix4f().perspective((float)Math.toRadians(p.sprinting?78:70),width/(float)Math.max(1,height),.05f,Math.max(100,distance+32));
        shader.matrix("view",view); shader.matrix("projection",projection); shader.scalar("daylight",.18f+day*.82f);
        shader.vector("fogColor",r,g,b); shader.scalar("fogStart",eye.fluid()?0:distance*.5f); shader.scalar("fogEnd",eye.fluid()?eye==Block.WATER?16:4:distance);
        if(!eye.fluid()) drawCelestial(px,py,pz,celestial,day);
        org.joml.FrustumIntersection frustum=new org.joml.FrustumIntersection(new Matrix4f(projection).mul(view));
        boolean[] visible=new boolean[16];
        for(int i=0;i<16;i++) {
            float x=i%4*16,z=i/4*16;
            visible[i]=options==null || !options.flag("advancedOpenGL",false) || frustum.testAab(x,0,z,x+16,128,z+16);
            if(visible[i]) opaque[i].draw();
        }
        Mesh.Builder entities=new Mesh.Builder();
        for(Drop d:game.drops) WorldMesher.cube(entities,(float)d.x-.1f,(float)(d.y+Math.sin((d.age+alpha)*.12)*.05),(float)d.z-.1f,.2f,Assets.tile(d.stack.item()),1);
        if(game.target!=null) {
            RayHit h=game.target;
            // Wireframe uses the same generated cube geometry, so there are no imported models.
            Mesh.Builder outline=new Mesh.Builder(); WorldMesher.cube(outline,h.x()-.002f,h.y()-.002f,h.z()-.002f,1.004f,5,.2f);
            dynamic.upload(outline); glPolygonMode(GL_FRONT_AND_BACK,GL_LINE); glLineWidth(1); dynamic.draw(); glPolygonMode(GL_FRONT_AND_BACK,GL_FILL);
        }
        dynamic.upload(entities); dynamic.draw();
        Integer[] order=new Integer[16]; for(int i=0;i<16;i++) order[i]=i;
        Arrays.sort(order,Comparator.comparingDouble((Integer i)->-(Math.pow(i%4*16+8-p.x,2)+Math.pow(i/4*16+8-p.z,2))));
        glDepthMask(false); for(int i:order) if(visible[i]) transparent[i].draw(); glDepthMask(true);
        if(game.player.health>0 && !game.paused) drawHand(game,alpha,width,height);
    }
    private void drawCelestial(double x,double y,double z,float angle,float day) {
        Mesh.Builder sky=new Mesh.Builder();
        float a=angle*(float)Math.PI*2,sx=(float)Math.sin(a)*75,sy=(float)Math.cos(a)*75;
        WorldMesher.billboard(sky,(float)x+sx-4,(float)y+sy-4,(float)z-25,8,8,53);
        WorldMesher.billboard(sky,(float)x-sx-3,(float)y-sy-3,(float)z+25,6,6,54);
        if(day<.1) {
            Random random=new Random(10842);
            for(int i=0;i<150;i++) {
                double az=random.nextDouble()*Math.PI*2,h=.1+random.nextDouble()*.9,rad=Math.sqrt(1-h*h)*85;
                WorldMesher.billboard(sky,(float)(x+Math.cos(az)*rad),(float)(y+h*85),(float)(z+Math.sin(az)*rad),.18f,.18f,55);
            }
        }
        shader.scalar("fogStart",10000); shader.scalar("fogEnd",10001);
        glDepthMask(false); celestialMesh.upload(sky); celestialMesh.draw(); glDepthMask(true);
        shader.scalar("fogStart",options==null?48:options.distance()*.5f); shader.scalar("fogEnd",options==null?96:options.distance());
    }
    private void drawHand(Game game,float alpha,int width,int height) {
        Player p=game.player; float bob=(float)Math.sin((game.world.time+alpha)*.6)*(.01f+(float)Math.hypot(p.vx,p.vz)*.08f);
        shader.matrix("projection",new Matrix4f().perspective((float)Math.toRadians(70),width/(float)Math.max(1,height),.01f,4));
        shader.matrix("view",new Matrix4f().translate(.40f,-.65f+bob,-.9f).rotateZ(-.25f).rotateY(-.5f));
        shader.scalar("fogStart",10000); shader.scalar("fogEnd",10001); shader.scalar("daylight",1);
        Mesh.Builder hand=new Mesh.Builder();
        WorldMesher.box(hand,-.1f,-.1f,0,.18f,.45f,.18f,57,1);
        WorldMesher.box(hand,-.1f,.35f,0,.18f,.18f,.18f,56,1);
        Stack held=p.held();
        if(held!=null) {
            if(held.item().block!=null) WorldMesher.cube(hand,-.12f,.48f,-.12f,.27f,Assets.tile(held.item()),1);
            else WorldMesher.billboard(hand,-.14f,.40f,-.04f,.40f,.40f,Assets.tile(held.item()));
        }
        glClear(GL_DEPTH_BUFFER_BIT); dynamic.upload(hand); dynamic.draw();
    }
    public void screenshot(Path path,int width,int height) throws IOException {
        ByteBuffer bytes=MemoryUtil.memAlloc(width*height*4);
        try {
            glReadPixels(0,0,width,height,GL_RGBA,GL_UNSIGNED_BYTE,bytes);
            BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
            for(int y=0;y<height;y++) for(int x=0;x<width;x++) {
                int i=((height-1-y)*width+x)*4;
                image.setRGB(x,y,(bytes.get(i)&255)<<16|(bytes.get(i+1)&255)<<8|(bytes.get(i+2)&255));
            }
            Files.createDirectories(path.toAbsolutePath().getParent()); if(!ImageIO.write(image,"png",path.toFile())) throw new IOException("PNG encoder unavailable");
        } finally { MemoryUtil.memFree(bytes); }
    }
    @Override public void close() { for(Mesh m:opaque) m.close(); for(Mesh m:transparent) m.close(); dynamic.close(); celestialMesh.close(); assets.close(); shader.close(); }
}
