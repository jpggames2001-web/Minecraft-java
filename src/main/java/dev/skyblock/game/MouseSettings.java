package dev.skyblock.game;

/** Sensitivity is degrees per GLFW cursor pixel; axes are independent. */
public record MouseSettings(boolean invertX, boolean invertY, double sensitivity) {
    public static final double MIN_SENSITIVITY=.01, MAX_SENSITIVITY=1;
    public static final MouseSettings DEFAULT=new MouseSettings(false,false,.15);

    public MouseSettings {
        if(!Double.isFinite(sensitivity) || sensitivity<MIN_SENSITIVITY || sensitivity>MAX_SENSITIVITY)
            throw new IllegalArgumentException("Mouse sensitivity must be between 0.01 and 1 degree per pixel");
    }
    public MouseSettings toggleX() { return new MouseSettings(!invertX,invertY,sensitivity); }
    public MouseSettings toggleY() { return new MouseSettings(invertX,!invertY,sensitivity); }
    public MouseSettings adjustSensitivity(double delta) {
        return new MouseSettings(invertX,invertY,Math.clamp(sensitivity+delta,MIN_SENSITIVITY,MAX_SENSITIVITY));
    }
}
