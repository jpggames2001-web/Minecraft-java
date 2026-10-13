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
    public SynthAudio(boolean enabled) {
        if(!enabled) return;
        try {
            AudioFormat format=new AudioFormat(22050,16,1,true,false);
            line=AudioSystem.getSourceDataLine(format); line.open(format,8192); line.start(); available=true;
        } catch(LineUnavailableException | IllegalArgumentException | SecurityException e) { System.out.println("Audio unavailable; continuing silently."); }
    }
    public void play(Effect effect) {
        if(!available) return;
        worker.execute(()-> {
            byte[] pcm=generate(effect);
            if(available) line.write(pcm,0,pcm.length);
        });
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
