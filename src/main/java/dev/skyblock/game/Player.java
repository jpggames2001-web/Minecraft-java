package dev.skyblock.game;

import dev.skyblock.world.*;
import java.util.List;

/** Velocities are blocks per fixed 20 Hz tick, as in the release-era simulation. */
public final class Player {
    public double x=31.5,y=65,z=30.5,prevX=x,prevY=y,prevZ=z,vx,vy,vz;
    public float yaw=-135,pitch=12;
    public boolean onGround,sneaking,sprinting,creative;
    public PhysicsProfile physics=PhysicsProfile.SPECIFIED;
    public int health=20,food=20,air=300,hurtTicks,selected,foodTimer;
    public float saturation=5,exhaustion,fallDistance;
    public final Inventory inventory=new Inventory(36);
    public Aabb box() { return new Aabb(x-.3,y,z-.3,x+.3,y+1.8,z+.3); }
    public double eyeY() { return y+(sneaking?1.54:1.62); }
    public double[] direction() {
        double a=Math.toRadians(yaw),p=Math.toRadians(pitch);
        return new double[]{-Math.sin(a)*Math.cos(p),-Math.sin(p),-Math.cos(a)*Math.cos(p)};
    }
    public Stack held() { return inventory.get(selected); }
    public void tick(World world,Controls input) {
        prevX=x; prevY=y; prevZ=z;
        if(hurtTicks>0) hurtTicks--;
        if(health<=0) return;
        sneaking=input.sneak();
        sprinting=input.sprint() && input.forward()>.8f && !sneaking && (food>6 || creative);
        Block medium=medium(world);
        boolean water=medium==Block.WATER,lava=medium==Block.LAVA;
        if(input.jump()) {
            if(water || lava) vy+=.04;
            else if(onGround) {
                vy=.42;
                if(sprinting) { double a=Math.toRadians(yaw); vx-=Math.sin(a)*.2; vz-=Math.cos(a)*.2; }
                exhaust(sprinting?.8f:.2f);
            }
        }
        float forward=input.forward()*.98f,strafe=input.strafe()*.98f;
        if(sneaking) { forward*=.3f; strafe*=.3f; }
        double friction=onGround ? slipperiness(world)*.91 : physics.horizontalAirDrag;
        double acceleration=water || lava ? .02 : onGround ? .1*(sprinting?1.3:1)*(.16277136/(friction*friction*friction)) : sprinting?.026:.02;
        accelerate(forward,strafe,acceleration);
        double oldX=x,oldZ=z;
        move(world,vx,vy,vz,sneaking && onGround);
        if(water) { vx*=.8; vz*=.8; vy=vy*.8-.02; fallDistance=0; }
        else if(lava) { vx*=.5; vz*=.5; vy=vy*.5-.02; fallDistance=0; }
        else { vy=(vy-.08)*.98; vx*=friction; vz*=friction; }
        if(Math.abs(vx)<.005) vx=0; if(Math.abs(vy)<.005) vy=0; if(Math.abs(vz)<.005) vz=0;
        double distance=Math.hypot(x-oldX,z-oldZ);
        if(onGround) exhaust((float)(distance*(sprinting?.1:.01)));
        else if(water) exhaust((float)(distance*.015));
        if(lava) hurt(4);
        if(world.get((int)Math.floor(x), (int)Math.floor(eyeY()),(int)Math.floor(z))==Block.WATER) {
            if(--air<=-20) { air=0; hurt(2); }
        } else air=300;
        for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++) {
            int bx=(int)Math.floor(x)+dx,bz=(int)Math.floor(z)+dz,by=(int)Math.floor(y);
            if(world.get(bx,by,bz)==Block.CACTUS && box().intersects(new Aabb(bx-.05,by,bz-.05,bx+1.05,by+1,bz+1.05))) hurt(1);
        }
        if(y< -64) { hurtTicks=0; hurt(4); }
        if(!creative) tickHunger();
    }
    private Block medium(World world) {
        Aabb box=box();
        for(int by=(int)Math.floor(y+.05);by<y+1.7;by++) for(int bz=(int)Math.floor(z-.29);bz<=Math.floor(z+.29);bz++) for(int bx=(int)Math.floor(x-.29);bx<=Math.floor(x+.29);bx++) {
            Block b=world.get(bx,by,bz);
            if(b.fluid() && box.minY()<by+b.height(world.data(bx,by,bz))) return b;
        }
        return Block.AIR;
    }
    private double slipperiness(World world) { return world.get((int)Math.floor(x),(int)Math.floor(y-.01),(int)Math.floor(z))==Block.ICE ? physics.iceSlipperiness : .6; }
    private void accelerate(float forward,float strafe,double acceleration) {
        double mag=Math.hypot(forward,strafe); if(mag<.01) return;
        double scale=acceleration/Math.max(1,mag),a=Math.toRadians(yaw);
        vx+=(strafe*Math.cos(a)-forward*Math.sin(a))*scale;
        vz+=(-strafe*Math.sin(a)-forward*Math.cos(a))*scale;
    }
    public void move(World world,double dx,double dy,double dz,boolean edgeGuard) {
        Aabb start=box();
        if(edgeGuard) {
            while(Math.abs(dx)>1e-9 && world.collisions(start.move(dx,-1,0)).isEmpty()) dx=approachZero(dx,.05);
            while(Math.abs(dz)>1e-9 && world.collisions(start.move(0,-1,dz)).isEmpty()) dz=approachZero(dz,.05);
            while(Math.abs(dx)>1e-9 && Math.abs(dz)>1e-9 && world.collisions(start.move(dx,-1,dz)).isEmpty()) { dx=approachZero(dx,.05); dz=approachZero(dz,.05); }
        }
        double requestedX=dx,requestedY=dy,requestedZ=dz;
        List<Aabb> obstacles=world.collisions(start.expand(dx,dy,dz));
        Aabb box=start;
        for(Aabb b:obstacles) dy=b.clipY(box,dy); box=box.move(0,dy,0);
        for(Aabb b:obstacles) dx=b.clipX(box,dx); box=box.move(dx,0,0);
        for(Aabb b:obstacles) dz=b.clipZ(box,dz); box=box.move(0,0,dz);
        if((onGround || requestedY<0 && dy!=requestedY) && (dx!=requestedX || dz!=requestedZ)) {
            List<Aabb> stepObstacles=world.collisions(start.expand(requestedX,.5,requestedZ));
            double sy=.5,sx=requestedX,sz=requestedZ;
            Aabb step=start;
            for(Aabb b:stepObstacles) sy=b.clipY(step,sy); step=step.move(0,sy,0);
            for(Aabb b:stepObstacles) sx=b.clipX(step,sx); step=step.move(sx,0,0);
            for(Aabb b:stepObstacles) sz=b.clipZ(step,sz); step=step.move(0,0,sz);
            double down=-sy;
            for(Aabb b:world.collisions(step.expand(0,down,0))) down=b.clipY(step,down);
            if(sx*sx+sz*sz>dx*dx+dz*dz) { box=step.move(0,down,0); dx=sx; dz=sz; dy=sy+down; }
        }
        x=(box.minX()+box.maxX())/2; y=box.minY(); z=(box.minZ()+box.maxZ())/2;
        onGround=requestedY<0 && requestedY!=dy;
        if(onGround) {
            if(fallDistance>3) hurt((int)Math.ceil(fallDistance-3)); fallDistance=0;
        } else if(dy<0) fallDistance-=(float)dy;
        if(dx!=requestedX) vx=0; if(dy!=requestedY) vy=0; if(dz!=requestedZ) vz=0;
    }
    private double approachZero(double value,double step) { return Math.abs(value)<step?0:value-Math.copySign(step,value); }
    private void tickHunger() {
        if(exhaustion>4) { exhaustion-=4; if(saturation>0) saturation=Math.max(0,saturation-1); else food=Math.max(0,food-1); }
        if(food>=18 && health<20) { if(++foodTimer>=80) { health++; exhaust(3); foodTimer=0; } }
        else if(food==0) { if(++foodTimer>=80) { if(health>1) hurt(1); foodTimer=0; } }
        else foodTimer=0;
    }
    public void exhaust(float amount) { if(!creative) exhaustion=Math.min(40,exhaustion+amount); }
    public boolean hurt(int damage) {
        if(creative || hurtTicks>0 || health<=0) return false;
        health=Math.max(0,health-damage); hurtTicks=10; return true;
    }
    public void eat(Item item) { food=Math.min(20,food+item.food()); saturation=Math.min(food,saturation+item.food()*item.saturation()*2); }
    public void respawn() {
        x=prevX=31.5; y=prevY=65; z=prevZ=30.5; vx=vy=vz=0;
        health=food=20; saturation=5; exhaustion=fallDistance=0; air=300; hurtTicks=foodTimer=0; onGround=false;
    }
}
