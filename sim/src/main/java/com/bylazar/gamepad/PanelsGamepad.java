package com.bylazar.gamepad;

public final class PanelsGamepad {
    public static final PanelsGamepad INSTANCE = new PanelsGamepad();
    private static final GamepadManager first = new GamepadManager(), second = new GamepadManager();
    private PanelsGamepad() {}
    public GamepadManager getFirstManager() { return first; }
    public GamepadManager getSecondManager() { return second; }
}
