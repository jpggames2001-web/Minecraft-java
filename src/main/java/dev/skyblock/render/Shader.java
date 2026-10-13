package dev.skyblock.render;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import java.util.*;
import static org.lwjgl.opengl.GL33C.*;

public final class Shader implements AutoCloseable {
    public final int program;
    private final Map<String,Integer> locations=new HashMap<>();
    public Shader(String vertex,String fragment) {
        int vs=compile(GL_VERTEX_SHADER,vertex),fs=0;
        try {
            fs=compile(GL_FRAGMENT_SHADER,fragment); program=glCreateProgram(); glAttachShader(program,vs); glAttachShader(program,fs); glLinkProgram(program);
            if(glGetProgrami(program,GL_LINK_STATUS)==GL_FALSE) { String log=glGetProgramInfoLog(program); glDeleteProgram(program); throw new IllegalStateException("Shader link: "+log); }
        } finally { glDeleteShader(vs); if(fs!=0) glDeleteShader(fs); }
    }
    private static int compile(int type,String source) {
        int shader=glCreateShader(type); glShaderSource(shader,source); glCompileShader(shader);
        if(glGetShaderi(shader,GL_COMPILE_STATUS)==GL_FALSE) { String log=glGetShaderInfoLog(shader); glDeleteShader(shader); throw new IllegalStateException("Shader compile: "+log); }
        return shader;
    }
    public void use() { glUseProgram(program); }
    private int location(String key) { return locations.computeIfAbsent(key,k->glGetUniformLocation(program,k)); }
    public void scalar(String key,float value) { glUniform1f(location(key),value); }
    public void vector(String key,float x,float y,float z) { glUniform3f(location(key),x,y,z); }
    public void matrix(String key,Matrix4f matrix) { try(MemoryStack stack=MemoryStack.stackPush()) { glUniformMatrix4fv(location(key),false,matrix.get(stack.mallocFloat(16))); } }
    @Override public void close() { glDeleteProgram(program); }
}
