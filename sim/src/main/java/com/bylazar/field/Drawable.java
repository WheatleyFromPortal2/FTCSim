package com.bylazar.field;

public abstract class Drawable {
    private DrawablesTypes type;
    protected Drawable(DrawablesTypes type) { this.type = type; }
    public DrawablesTypes getType() { return type; }
    public void setType(DrawablesTypes t) { type = t; }
}
