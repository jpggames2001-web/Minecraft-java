package dev.skyblock.render;

import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;
import java.util.Arrays;
import static org.lwjgl.opengl.GL33C.*;

public final class Mesh implements AutoCloseable {
    private final int vao=glGenVertexArrays(),vbo=glGenBuffers();
    private int count;
    public Mesh() {
        glBindVertexArray(vao); glBindBuffer(GL_ARRAY_BUFFER,vbo);
        int[] sizes={3,2,3,1,1}; int offset=0;
        for(int i=0;i<sizes.length;i++) { glEnableVertexAttribArray(i); glVertexAttribPointer(i,sizes[i],GL_FLOAT,false,40,offset*4L); offset+=sizes[i]; }
        glBindVertexArray(0);
    }
    public void upload(Builder b) {
        count=b.size/10; FloatBuffer buffer=MemoryUtil.memAllocFloat(b.size);
        try { buffer.put(b.values,0,b.size).flip(); glBindBuffer(GL_ARRAY_BUFFER,vbo); glBufferData(GL_ARRAY_BUFFER,buffer,GL_DYNAMIC_DRAW); }
        finally { MemoryUtil.memFree(buffer); }
    }
    public void draw() { if(count>0) { glBindVertexArray(vao); glDrawArrays(GL_TRIANGLES,0,count); } }
    public int vertexCount() { return count; }
    @Override public void close() { glDeleteBuffers(vbo); glDeleteVertexArrays(vao); }
    public static final class Builder {
        private float[] values=new float[10240]; private int size;
        public void vertex(float x,float y,float z,float u,float v,float r,float g,float b,float sky,float light) {
            if(size+10>values.length) values=Arrays.copyOf(values,values.length*2);
            values[size++]=x; values[size++]=y; values[size++]=z; values[size++]=u; values[size++]=v;
            values[size++]=r; values[size++]=g; values[size++]=b; values[size++]=sky; values[size++]=light;
        }
    }
}
