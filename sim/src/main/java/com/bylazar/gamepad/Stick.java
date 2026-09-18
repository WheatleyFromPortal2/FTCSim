package com.bylazar.gamepad;

public final class Stick {
    private double x, y; private boolean value;
    public Stick() {}
    public Stick(double x, double y, boolean value) { this.x = x; this.y = y; this.value = value; }
    public double getX() { return x; } public void setX(double v) { x = v; }
    public double getY() { return y; } public void setY(double v) { y = v; }
    public boolean getValue() { return value; } public void setValue(boolean v) { value = v; }
}
