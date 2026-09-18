package com.bylazar.gamepad;

/**
 * FTCSim stand-in for Panels' GamepadManager. Panels lets a browser act as a
 * gamepad; in FTCSim the simulator's own gamepads already do that, so this
 * manager reports no input of its own and combines transparently.
 */
public class GamepadManager {
    public final long DELETION_INTERVAL = 60L;
    private Gamepad currentState = new Gamepad();

    public com.qualcomm.robotcore.hardware.Gamepad asCombinedFTCGamepad(com.qualcomm.robotcore.hardware.Gamepad gamepad) { return gamepad; }
    public com.qualcomm.robotcore.hardware.Gamepad getAsFTCGamepad() { return new com.qualcomm.robotcore.hardware.Gamepad(); }
    public boolean getL1() { return currentState.l1; } public double getL2() { return currentState.l2; }
    public boolean getR1() { return currentState.r1; } public double getR2() { return currentState.r2; }
    public boolean getCross() { return currentState.cross; } public long getCrossTimestamp() { return 0; }
    public boolean getCircle() { return currentState.circle; } public boolean getSquare() { return currentState.square; } public boolean getTriangle() { return currentState.triangle; }
    public boolean getDpadUp() { return currentState.dpad_up; } public boolean getDpadLeft() { return currentState.dpad_left; } public boolean getDpadRight() { return currentState.dpad_right; } public boolean getDpadDown() { return currentState.dpad_down; }
    public boolean getTouchpad() { return currentState.touchpad; } public boolean getOptions() { return currentState.options; } public boolean getShare() { return currentState.share; } public boolean getPs() { return currentState.ps; }
    public double getLeftStickX() { return currentState.leftStick.getX(); } public double getLeftStickY() { return currentState.leftStick.getY(); } public boolean getLeftStickPressed() { return currentState.leftStick.getValue(); }
    public double getRightStickX() { return currentState.rightStick.getX(); } public double getRightStickY() { return currentState.rightStick.getY(); } public boolean getRightStickPressed() { return currentState.rightStick.getValue(); }
    public Stick getLeftStick() { return currentState.leftStick; } public Stick getRightStick() { return currentState.rightStick; }
}
