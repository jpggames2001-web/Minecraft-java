package dev.skyblock.world;

public record Aabb(double minX,double minY,double minZ,double maxX,double maxY,double maxZ) {
    public Aabb move(double x,double y,double z) { return new Aabb(minX+x,minY+y,minZ+z,maxX+x,maxY+y,maxZ+z); }
    public Aabb expand(double x,double y,double z) {
        return new Aabb(minX+Math.min(0,x),minY+Math.min(0,y),minZ+Math.min(0,z),maxX+Math.max(0,x),maxY+Math.max(0,y),maxZ+Math.max(0,z));
    }
    public boolean intersects(Aabb b) { return maxX>b.minX && minX<b.maxX && maxY>b.minY && minY<b.maxY && maxZ>b.minZ && minZ<b.maxZ; }
    public double clipX(Aabb moving,double delta) {
        if(moving.maxY<=minY || moving.minY>=maxY || moving.maxZ<=minZ || moving.minZ>=maxZ) return delta;
        if(delta>0 && moving.maxX<=minX) delta=Math.min(delta,minX-moving.maxX);
        if(delta<0 && moving.minX>=maxX) delta=Math.max(delta,maxX-moving.minX);
        return delta;
    }
    public double clipY(Aabb moving,double delta) {
        if(moving.maxX<=minX || moving.minX>=maxX || moving.maxZ<=minZ || moving.minZ>=maxZ) return delta;
        if(delta>0 && moving.maxY<=minY) delta=Math.min(delta,minY-moving.maxY);
        if(delta<0 && moving.minY>=maxY) delta=Math.max(delta,maxY-moving.minY);
        return delta;
    }
    public double clipZ(Aabb moving,double delta) {
        if(moving.maxX<=minX || moving.minX>=maxX || moving.maxY<=minY || moving.minY>=maxY) return delta;
        if(delta>0 && moving.maxZ<=minZ) delta=Math.min(delta,minZ-moving.maxZ);
        if(delta<0 && moving.minZ>=maxZ) delta=Math.max(delta,maxZ-moving.minZ);
        return delta;
    }
}
