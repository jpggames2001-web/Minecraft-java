package dev.skyblock.game;

import dev.skyblock.world.*;

public final class Drop {
    public Stack stack;
    public double x,y,z,vx,vy,vz;
    public int age,pickupDelay=10;
    public Drop(Stack stack,double x,double y,double z) { this.stack=stack; this.x=x; this.y=y; this.z=z; vy=.2; }
    public boolean tick(World world,Player player) {
        age++; if(pickupDelay>0) pickupDelay--;
        vy-=.04;
        Aabb box=new Aabb(x-.125,y,z-.125,x+.125,y+.25,z+.125);
        double moveY=vy;
        for(Aabb b:world.collisions(box.expand(vx,vy,vz))) moveY=b.clipY(box,moveY);
        y+=moveY; x+=vx; z+=vz;
        if(moveY!=vy) { vy=-vy*.5; vx*=.6; vz*=.6; }
        else vy*=.98;
        if(pickupDelay==0 && player.health>0 && Math.abs(x-player.x)<1.1 && Math.abs(z-player.z)<1.1 && Math.abs(y-player.y)<1.8) stack=player.inventory.add(stack);
        return stack!=null && age<6000 && y> -64;
    }
}
