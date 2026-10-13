package dev.skyblock;

import dev.skyblock.game.*;
import dev.skyblock.world.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class PhysicsTest {
    World world; Player player;
    @BeforeEach void setup() {
        world=new World(); player=new Player(); player.x=player.z=32.5; player.y=65;
        for(int z=10;z<54;z++) for(int x=10;x<54;x++) world.set(x,64,z,Block.STONE);
        player.tick(world,Controls.NONE); player.tick(world,Controls.NONE);
    }
    @Test void dimensionsAndEyeHeight() { assertEquals(.6,player.box().maxX()-player.box().minX(),1e-12); assertEquals(1.8,player.box().maxY()-player.box().minY(),1e-12); assertEquals(1.62,player.eyeY()-player.y,1e-12); }
    @Test void gravityAndVerticalDrag() { player.y=75; player.onGround=false; player.vy=0; player.tick(world,Controls.NONE); assertEquals(-.08*.98,player.vy,1e-12); player.tick(world,Controls.NONE); assertEquals((-.08*.98-.08)*.98,player.vy,1e-12); }
    @Test void suppliedAirDragIsDistinctFromReferenceProfile() {
        player.y=75; player.onGround=false; player.vx=.2; player.tick(world,Controls.NONE); assertEquals(.196,player.vx,1e-12);
        player.physics=PhysicsProfile.RELEASE_REFERENCE; player.vx=.2; player.tick(world,Controls.NONE); assertEquals(.182,player.vx,1e-12);
    }
    @Test void jumpHeightAndLanding() {
        assertTrue(player.onGround); double start=player.y;
        player.tick(world,new Controls(0,0,true,false,false)); assertEquals(.42,player.y-start,1e-12);
        double top=player.y; for(int i=0;i<25;i++) { player.tick(world,Controls.NONE); top=Math.max(top,player.y); }
        assertEquals(1.2491870944,top-start,1e-8); assertEquals(65,player.y,1e-10); assertTrue(player.onGround); assertEquals(20,player.health);
    }
    @Test void walkingAndSprintingReachExpectedSpeed() {
        player.yaw=0;
        for(int i=0;i<30;i++) player.tick(world,new Controls(1,0,false,false,false));
        double before=player.z; player.tick(world,new Controls(1,0,false,false,false)); assertEquals(.21586,before-player.z,.0001);
        for(int i=0;i<15;i++) player.tick(world,new Controls(1,0,false,false,true));
        before=player.z; player.tick(world,new Controls(1,0,false,false,true)); assertEquals(.28062,before-player.z,.0002);
    }
    @Test void diagonalMovementNormalizesInput() {
        player.yaw=0; player.tick(world,new Controls(1,1,false,false,false));
        assertEquals(Math.abs(player.vx),Math.abs(player.vz),1e-12); assertTrue(Math.hypot(player.vx,player.vz)<.06);
    }
    @Test void waterDragAndJumpBuoyancy() {
        for(int y=65;y<69;y++) world.set(32,y,32,Block.WATER);
        player.onGround=false; player.vx=.2; player.vy=.1;
        player.tick(world,Controls.NONE); assertEquals(.16,player.vx,1e-12); assertEquals(.06,player.vy,1e-12);
        player.vy=0; player.tick(world,new Controls(0,0,true,false,false)); assertEquals(.012,player.vy,1e-12);
    }
    @Test void wallCollisionKeepsBoundingBoxOutsideBlock() {
        world.set(33,65,32,Block.STONE); player.vx=1; player.move(world,1,0,0,false);
        assertEquals(32.7,player.x,1e-12); assertEquals(0,player.vx);
    }
    @Test void sneakPreventsWalkingIntoVoid() {
        world=new World(); world.set(32,64,32,Block.STONE); player.x=player.z=32.5; player.y=65;
        for(int i=0;i<100;i++) { player.vx=.1; player.move(world,.1,-.08,0,true); }
        assertTrue(player.x<33.3); assertEquals(65,player.y,1e-12);
    }
    @Test void fallDamageIsAppliedOnLanding() {
        player.y=72; player.onGround=false; for(int i=0;i<50;i++) player.tick(world,Controls.NONE);
        assertEquals(65,player.y,1e-12); assertEquals(16,player.health);
    }
    @Test void aabbAxisClippingAndTouchingFaces() {
        Aabb block=new Aabb(1,0,0,2,1,1),body=new Aabb(0,0,0,1,1,1);
        assertFalse(block.intersects(body)); assertEquals(0,block.clipX(body,1)); assertEquals(-1,block.clipX(body,-1));
    }
}
