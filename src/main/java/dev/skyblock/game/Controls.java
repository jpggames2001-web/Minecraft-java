package dev.skyblock.game;

public record Controls(float forward,float strafe,boolean jump,boolean sneak,boolean sprint) {
    public static final Controls NONE=new Controls(0,0,false,false,false);
}
