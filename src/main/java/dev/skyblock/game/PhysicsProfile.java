package dev.skyblock.game;

/** Separates the user's explicit constants from commonly documented legacy constants. */
public enum PhysicsProfile {
    SPECIFIED(.98,.91), RELEASE_REFERENCE(.91,.98);
    public final double horizontalAirDrag, iceSlipperiness;
    PhysicsProfile(double horizontalAirDrag,double iceSlipperiness) { this.horizontalAirDrag=horizontalAirDrag; this.iceSlipperiness=iceSlipperiness; }
}
