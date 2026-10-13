package dev.skyblock.game;

import dev.skyblock.world.*;
import java.io.*;
import java.nio.file.*;
import java.util.Locale;

/** Produces reproducible position/velocity evidence without a display or native graphics. */
public final class PhysicsTrace {
    private PhysicsTrace() {}
    public static void write(Path path,PhysicsProfile profile) throws IOException {
        World world=new World();
        for(int z=0;z<World.SIZE;z++) for(int x=0;x<World.SIZE;x++) world.set(x,60,z,Block.STONE);
        Player player=new Player(); player.physics=profile; player.x=32.5; player.z=55.5; player.y=61; player.yaw=0; player.creative=true;
        Path absolute=path.toAbsolutePath(); Files.createDirectories(absolute.getParent());
        try(PrintWriter out=new PrintWriter(Files.newBufferedWriter(absolute))) {
            out.println("profile,tick,forward,strafe,jump,sneak,sprint,x,y,z,vx,vy,vz,on_ground");
            for(int tick=0;tick<160;tick++) {
                Controls controls=tick<10?Controls.NONE:tick<50?new Controls(1,0,tick==30,false,false):tick<85?new Controls(1,0,false,false,true):tick<110?new Controls(0,1,false,true,false):Controls.NONE;
                player.tick(world,controls);
                out.printf(Locale.ROOT,"%s,%d,%.1f,%.1f,%s,%s,%s,%.12f,%.12f,%.12f,%.12f,%.12f,%.12f,%s%n",profile,tick,controls.forward(),controls.strafe(),controls.jump(),controls.sneak(),controls.sprint(),player.x,player.y,player.z,player.vx,player.vy,player.vz,player.onGround);
            }
            if(out.checkError()) throw new IOException("Unable to write physics trace");
        }
    }
}
