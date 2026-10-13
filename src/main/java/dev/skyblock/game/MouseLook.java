package dev.skyblock.game;

/** Capture transitions discard the first cursor sample to prevent recentering jumps. */
public final class MouseLook {
    private boolean captured,anchored;
    private double lastX,lastY;

    public void capture(boolean enabled) {
        if(captured!=enabled) { captured=enabled; anchored=false; }
    }
    public void cursor(Player player,double x,double y,MouseSettings settings) {
        if(!captured || !Double.isFinite(x) || !Double.isFinite(y)) return;
        if(anchored) {
            // Positive yaw turns right; positive pitch looks down (GLFW Y grows downwards).
            player.yaw+=(float)((x-lastX)*settings.sensitivity()*(settings.invertX()?-1:1));
            player.pitch=Math.clamp(player.pitch+(float)((y-lastY)*settings.sensitivity()*(settings.invertY()?-1:1)),-89.9f,89.9f);
        }
        lastX=x; lastY=y; anchored=true;
    }
}
