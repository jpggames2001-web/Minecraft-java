package dev.skyblock.audio;

import javax.sound.sampled.*;
import java.util.Random;
import java.util.concurrent.*;

/** Original PCM generated in memory; a missing audio device never stops the game. */
public final class SynthAudio implements AutoCloseable {
    public enum Effect { STEP, DIG, PLACE, HURT, PICKUP, CLICK }
    private final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(16),r->{Thread t=new Thread(r,"synthetic-audio"); t.setDaemon(true); return t;},new ThreadPoolExecutor.DiscardPolicy());
    private volatile boolean available;
    private SourceDataLine line;
    private volatile float master=1,sound=1,music=.3f;
    private long lastMusic=-1;
    public SynthAudio(boolean enabled) {
        if(!enabled) return;
        try {
            AudioFormat format=new AudioFormat(22050,16,1,true,false);
            line=AudioSystem.getSourceDataLine(format); line.open(format,8192); line.start(); available=true;
        } catch(LineUnavailableException | IllegalArgumentException | SecurityException e) { System.out.println("Audio unavailable; continuing silently."); }
    }
    public void volumes(float master,float sound,float music) { this.master=Math.clamp(master,0,1); this.sound=Math.clamp(sound,0,1); this.music=Math.clamp(music,0,1); }
    public void musicTick(long tick) {
        if(available && music>0 && tick/160!=lastMusic) { lastMusic=tick/160; worker.execute(()->write(generateMusic((int)(tick/160)),true)); }
    }
    public void play(Effect effect) { if(available && sound>0) worker.execute(()->write(generate(effect),false)); }
    private void write(byte[] source,boolean musical) {
        byte[] chunk=new byte[1024];
        for(int offset=0;offset<source.length && available;offset+=chunk.length) {
            int count=Math.min(chunk.length,source.length-offset); float gain=master*(musical?music:sound);
            for(int i=0;i<count;i+=2) { short raw=(short)((source[offset+i]&255)|source[offset+i+1]<<8); int n=(int)(raw*gain); chunk[i]=(byte)n; chunk[i+1]=(byte)(n>>8); }
            if(available) line.write(chunk,0,count);
        }
    }
    public static byte[] generateMusic(int phrase) {
        int samples=22050; byte[] pcm=new byte[samples*2]; double[] notes={261.626,329.628,391.995,440,293.665,349.228,391.995,329.628};
        for(int i=0;i<samples;i++) { double t=i/22050.0,envelope=Math.sin(Math.PI*i/samples); double frequency=notes[Math.floorMod(phrase,notes.length)]; int value=(int)((Math.sin(t*frequency*2*Math.PI)+.4*Math.sin(t*frequency*3*Math.PI))*envelope*3000); pcm[i*2]=(byte)value; pcm[i*2+1]=(byte)(value>>8); }
        return pcm;
    }
    public static byte[] generate(Effect effect) {
        int samples=switch(effect) { case HURT -> 5500; case PICKUP -> 2600; default -> 1800; };
        byte[] pcm=new byte[samples*2]; Random random=new Random(31L+effect.ordinal());
        double base=switch(effect) { case STEP -> 85; case DIG -> 160; case PLACE -> 120; case HURT -> 70; case PICKUP -> 660; case CLICK -> 400; };
        for(int i=0;i<samples;i++) {
            double t=i/22050.0,envelope=Math.pow(1-i/(double)samples,2);
            double tone=Math.sin(2*Math.PI*base*t*(effect==Effect.PICKUP?1+t*8:1));
            double noise=random.nextDouble()*2-1;
            int sample=(int)((effect==Effect.PICKUP ? tone*.8 : noise*.7+tone*.3)*envelope*6500);
            pcm[i*2]=(byte)sample; pcm[i*2+1]=(byte)(sample>>8);
        }
        return pcm;
    }
    @Override public void close() {
        available=false; worker.shutdownNow();
        if(line!=null) { line.stop(); line.flush(); line.close(); }
    }
}
