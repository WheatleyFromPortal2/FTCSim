package com.bylazar.gamepad;

/** Panels' gamepad state data class (Java version). */
public final class Gamepad {
    public boolean l1, r1, cross, circle, square, triangle, dpad_up, dpad_left, dpad_right, dpad_down, touchpad, options, share, ps;
    public double l2, r2;
    public Stick leftStick = new Stick(), rightStick = new Stick();
    public Gamepad() {}
    public boolean getL1() { return l1; } public double getL2() { return l2; } public boolean getR1() { return r1; } public double getR2() { return r2; }
    public Stick getLeftStick() { return leftStick; } public Stick getRightStick() { return rightStick; }
    public boolean getCross() { return cross; } public boolean getCircle() { return circle; } public boolean getSquare() { return square; } public boolean getTriangle() { return triangle; }
    public boolean getDpad_up() { return dpad_up; } public boolean getDpad_left() { return dpad_left; } public boolean getDpad_right() { return dpad_right; } public boolean getDpad_down() { return dpad_down; }
    public boolean getTouchpad() { return touchpad; } public boolean getOptions() { return options; } public boolean getShare() { return share; } public boolean getPs() { return ps; }
}
