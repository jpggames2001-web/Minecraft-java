package dev.skyblock.ui;

import dev.skyblock.render.*;
import dev.skyblock.game.Stack;
import org.joml.Matrix4f;
import static org.lwjgl.opengl.GL33C.*;

public final class Canvas implements AutoCloseable {
    private final Shader shader=new Shader("""
        #version 330 core
        layout(location=0) in vec3 position; layout(location=1) in vec2 uv;
        layout(location=2) in vec3 color; layout(location=3) in float alpha; layout(location=4) in float textured;
        uniform mat4 projection; out vec2 tc; out vec4 tint; out float sampleTexture;
        void main() { gl_Position=projection*vec4(position,1); tc=uv; tint=vec4(color,alpha); sampleTexture=textured; }
        ""","""
        #version 330 core
        in vec2 tc; in vec4 tint; in float sampleTexture; uniform sampler2D atlas; out vec4 result;
        void main() { result=tint*(sampleTexture>.5?texture(atlas,tc):vec4(1)); }
        """);
    private final Mesh mesh=new Mesh();
    private Mesh.Builder builder;
    public int width,height,scale;
    private final Assets assets;
    public Canvas(Assets assets) { this.assets=assets; }
    public void begin(int pixelsWide,int pixelsHigh) { begin(pixelsWide,pixelsHigh,Math.max(1,Math.min(pixelsWide/640,pixelsHigh/360))); }
    public void begin(int pixelsWide,int pixelsHigh,int requestedScale) { scale=requestedScale; width=pixelsWide/scale; height=pixelsHigh/scale; builder=new Mesh.Builder(); }
    public void rect(float x,float y,float w,float h,int argb) { quad(x,y,w,h,0,0,0,0,argb,false); }
    public void tile(int tile,float x,float y,float size,int color) {
        float u=(tile%8*16+.01f)/128,v=(tile/8*16+.01f)/128;
        quad(x,y,size,size,u,v,u+15.98f/128,v+15.98f/128,color,true);
    }
    public void nineSlice(int tile,int x,int y,int w,int h) {
        int[] source={0,2,14,16},dx={x,x+2,x+w-2,x+w},dy={y,y+2,y+h-2,y+h};
        for(int row=0;row<3;row++) for(int col=0;col<3;col++) quad(dx[col],dy[row],dx[col+1]-dx[col],dy[row+1]-dy[row],(tile%8*16+source[col]+.01f)/128,(tile/8*16+source[row]+.01f)/128,(tile%8*16+source[col+1]-.01f)/128,(tile/8*16+source[row+1]-.01f)/128,0xffffffff,true);
    }
    public void dirtBackground() { for(int y=0;y<height;y+=32) for(int x=0;x<width;x+=32) tile(2,x,y,32,0xff686868); }
    public void preview(java.awt.image.BufferedImage image,int x,int y,int size) { assets.preview(image); tile(63,x,y,size,0xffffffff); }
    public void item(Stack stack,float x,float y) {
        if(stack==null) return;
        tile(Assets.tile(stack.item()),x,y,16,0xffffffff);
        if(stack.item().durability>0 && stack.damage()>0) {
            rect(x,y+14,14,2,0xff191919); float fraction=1-stack.damage()/(float)stack.item().durability;
            rect(x,y+14,14*fraction,1,0xff000000|((int)(255*(1-fraction))<<16)|((int)(255*fraction)<<8));
        }
        if(stack.count()>1) { String n=""+stack.count(); text(n,x+17-BitmapFont.width(n,1),y+10,0xffffffff,1); }
    }
    public void text(String text,float x,float y,int color,int scale) { BitmapFont.draw(this,text,x+scale,y+scale,0x99000000,scale); BitmapFont.draw(this,text,x,y,color,scale); }
    public void centered(String text,float y,int color,int size) { text(text,(width-BitmapFont.width(text,size))/2f,y,color,size); }
    public void border(float x,float y,float w,float h,int color) { rect(x,y,w,1,color); rect(x,y+h-1,w,1,color); rect(x,y,1,h,color); rect(x+w-1,y,1,h,color); }
    private void quad(float x,float y,float w,float h,float u0,float v0,float u1,float v1,int argb,boolean textured) {
        float[][] vertices={{x,y,u0,v0},{x+w,y,u1,v0},{x+w,y+h,u1,v1},{x,y+h,u0,v1}};
        int[] order={0,1,2,0,2,3};
        for(int i:order) { float[] a=vertices[i]; builder.vertex(a[0],a[1],0,a[2],a[3],(argb>>16&255)/255f,(argb>>8&255)/255f,(argb&255)/255f,(argb>>>24)/255f,textured?1:0); }
    }
    public void finish() {
        glDisable(GL_DEPTH_TEST); glEnable(GL_BLEND); glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);
        shader.use(); shader.matrix("projection",new Matrix4f().ortho(0,width,height,0,-1,1));
        glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D,assets.texture); mesh.upload(builder); mesh.draw();
    }
    @Override public void close() { mesh.close(); shader.close(); }
}
