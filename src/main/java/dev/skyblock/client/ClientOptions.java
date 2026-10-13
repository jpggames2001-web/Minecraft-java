package dev.skyblock.client;
import dev.skyblock.persistence.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
public final class ClientOptions {
    public final MouseSettingsStore mouse;
    private final Path file;
    private Properties values;
    public long revision;
    public ClientOptions(Path file,MouseSettingsStore mouse) throws IOException { this.file=file; this.mouse=mouse; values=AtomicProperties.read(file); }
    public String text(String key,String fallback) { return values.getProperty(key,fallback); }
    public boolean flag(String key,boolean fallback) { return Boolean.parseBoolean(text(key,""+fallback)); }
    public double number(String key,double fallback) { try { double n=Double.parseDouble(text(key,""+fallback)); return Double.isFinite(n)?n:fallback; } catch(NumberFormatException e) { return fallback; } }
    public int key(Binding b) { int n=(int)number("key."+b.name(),b.defaultCode); return n<=-100 && n>=-107 || n==0 || n>=32 && n<=348?n:b.defaultCode; }
    public boolean conflicts(Binding binding) { int code=key(binding); return code!=0 && Arrays.stream(Binding.values()).anyMatch(other->other!=binding && key(other)==code); }
    public void set(String key,Object value) throws IOException { Properties next=new Properties(); next.putAll(values); next.setProperty(key,String.valueOf(value)); AtomicProperties.write(file,next); values=next; revision++; }
    public void bind(Binding binding,int code) throws IOException { set("key."+binding.name(),code); }
    public void resetBindings() throws IOException {
        Properties next=new Properties(); next.putAll(values);
        for(Binding b:Binding.values()) next.remove("key."+b.name());
        AtomicProperties.write(file,next); values=next; revision++;
    }
    public void toggle(String key,boolean fallback) throws IOException { set(key,!flag(key,fallback)); }
    public void cycle(String key,String fallback,String... choices) throws IOException {
        int index=Arrays.asList(choices).indexOf(text(key,fallback)); set(key,choices[(index+1)%choices.length]);
    }
    public int guiScale(int width,int height) {
        int max=switch(text("guiScale","Auto")) { case "Small" -> 1; case "Normal" -> 2; case "Large" -> 3; default -> 8; };
        return Math.max(1,Math.min(max,Math.min(width/320,height/240)));
    }
    public int distance() { return switch(text("distance","Normal")) { case "Tiny" -> 24; case "Short" -> 48; case "Far" -> 160; default -> 96; }; }
    public float volume(String channel) { return (float)Math.clamp(number(channel,channel.equals("music")?.3:1),0,1); }
}
