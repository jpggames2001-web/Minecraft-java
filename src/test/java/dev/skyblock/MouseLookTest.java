package dev.skyblock;

import dev.skyblock.game.*;
import dev.skyblock.world.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MouseLookTest {
    private Player look(double dx,double dy,MouseSettings settings) {
        Player p=new Player(); p.yaw=p.pitch=0;
        MouseLook mouse=new MouseLook(); mouse.capture(true);
        mouse.cursor(p,500,500,settings); mouse.cursor(p,500+dx,500+dy,settings);
        return p;
    }
    @Test void rightMouseIncreasesYawAndReallyTurnsCameraRight() {
        Player p=look(100,0,MouseSettings.DEFAULT); assertEquals(15,p.yaw,1e-6);
        double[] direction=p.direction(); assertTrue(direction[0]>0); assertTrue(direction[2]<0);
        Matrix4f view=new Matrix4f().lookAt(0,0,0,(float)direction[0],(float)direction[1],(float)direction[2],0,1,0);
        Vector3f marker=view.transformPosition(new Vector3f(0,0,-10));
        assertTrue(marker.x<0,"A straight-ahead world marker moves left on screen when the camera turns right");
    }
    @Test void leftMouseDecreasesYawAndTurnsLeft() { Player p=look(-100,0,MouseSettings.DEFAULT); assertEquals(-15,p.yaw,1e-6); assertTrue(p.direction()[0]<0); }
    @Test void downMouseLooksDownByDefault() { Player p=look(0,100,MouseSettings.DEFAULT); assertEquals(15,p.pitch,1e-6); assertTrue(p.direction()[1]<0); }
    @Test void upMouseLooksUpByDefault() { Player p=look(0,-100,MouseSettings.DEFAULT); assertEquals(-15,p.pitch,1e-6); assertTrue(p.direction()[1]>0); }
    @Test void invertXChangesOnlyHorizontalAxis() { Player p=look(100,100,new MouseSettings(true,false,.15)); assertEquals(-15,p.yaw,1e-6); assertEquals(15,p.pitch,1e-6); }
    @Test void invertYChangesOnlyVerticalAxis() { Player p=look(100,100,new MouseSettings(false,true,.15)); assertEquals(15,p.yaw,1e-6); assertEquals(-15,p.pitch,1e-6); }
    @Test void bothAxesCanBeInvertedIndependently() { Player p=look(100,100,new MouseSettings(true,true,.15)); assertEquals(-15,p.yaw,1e-6); assertEquals(-15,p.pitch,1e-6); }
    @Test void sensitivityScalesBothAxesAndPitchClamps() {
        Player p=look(100,100,new MouseSettings(false,false,.3)); assertEquals(30,p.yaw,1e-6); assertEquals(30,p.pitch,1e-6);
        assertEquals(89.9f,look(0,10000,MouseSettings.DEFAULT).pitch); assertEquals(-89.9f,look(0,-10000,MouseSettings.DEFAULT).pitch);
    }
    @Test void pauseAndRecaptureDoNotRotateOrReuseOldCursorOrigin() {
        Player p=new Player(); p.yaw=p.pitch=0; MouseLook mouse=new MouseLook();
        mouse.cursor(p,100,100,MouseSettings.DEFAULT); assertEquals(0,p.yaw);
        mouse.capture(true); mouse.cursor(p,500,500,MouseSettings.DEFAULT); assertEquals(0,p.yaw);
        mouse.cursor(p,510,510,MouseSettings.DEFAULT); assertEquals(1.5,p.yaw,1e-6);
        mouse.capture(false); mouse.cursor(p,99999,99999,MouseSettings.DEFAULT); assertEquals(1.5,p.yaw,1e-6);
        mouse.capture(true); mouse.cursor(p,100,100,MouseSettings.DEFAULT); assertEquals(1.5,p.yaw,1e-6);
        mouse.cursor(p,110,110,MouseSettings.DEFAULT); assertEquals(3,p.yaw,1e-6); assertEquals(3,p.pitch,1e-6);
    }
    @Test void changingSettingsAffectsTheNextDeltaWithoutLosingTheAnchor() {
        Player p=new Player(); p.yaw=p.pitch=0; MouseLook mouse=new MouseLook(); mouse.capture(true);
        mouse.cursor(p,100,100,MouseSettings.DEFAULT); mouse.cursor(p,110,100,MouseSettings.DEFAULT);
        mouse.cursor(p,120,100,MouseSettings.DEFAULT.toggleX()); assertEquals(0,p.yaw,1e-6);
    }
    @Test void movementStrafingAndSprintJumpAgreeWithCameraAtEveryHeading() {
        for(float yaw:new float[]{0,90,180,-90,135}) {
            World w=new World(); Player forward=new Player(); forward.yaw=yaw; forward.pitch=0;
            double[] f=forward.direction(); forward.tick(w,new Controls(1,0,false,false,false));
            assertTrue(forward.vx*f[0]+forward.vz*f[2]>0); assertEquals(0,forward.vx*f[2]-forward.vz*f[0],1e-8);
            Player right=new Player(); right.yaw=yaw; right.tick(w,new Controls(0,1,false,false,false));
            assertTrue(right.vx*-f[2]+right.vz*f[0]>0); assertEquals(0,right.vx*f[0]+right.vz*f[2],1e-8);
            Player jumper=new Player(); jumper.yaw=yaw; jumper.onGround=true;
            jumper.tick(w,new Controls(1,0,true,false,true));
            assertTrue(jumper.vx*f[0]+jumper.vz*f[2]>0); assertEquals(0,jumper.vx*f[2]-jumper.vz*f[0],1e-8);
        }
    }
    @Test void targetRayPointsRightAfterPositiveMouseDelta() {
        World w=new World(); w.set(35,66,30,Block.STONE);
        Player p=look(600,0,MouseSettings.DEFAULT); p.x=31.5; p.y=65; p.z=30.5;
        double[] d=p.direction(); RayHit hit=w.raycast(p.x,p.eyeY(),p.z,d[0],d[1],d[2],4.5,false);
        assertNotNull(hit); assertEquals(35,hit.x()); assertEquals(-1,hit.nx());
    }
}
