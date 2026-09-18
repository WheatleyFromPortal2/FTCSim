package ftcsim.physics;

/** Immutable 2D vector (SI units unless stated otherwise). */
public final class Vec2 {
    public static final Vec2 ZERO = new Vec2(0, 0);
    public final double x, y;
    public Vec2(double x, double y) { this.x = x; this.y = y; }
    public Vec2 plus(Vec2 o) { return new Vec2(x + o.x, y + o.y); }
    public Vec2 minus(Vec2 o) { return new Vec2(x - o.x, y - o.y); }
    public Vec2 times(double s) { return new Vec2(x * s, y * s); }
    public double dot(Vec2 o) { return x * o.x + y * o.y; }
    public double cross(Vec2 o) { return x * o.y - y * o.x; }
    public double norm() { return Math.hypot(x, y); }
    public Vec2 rotated(double theta) {
        double c = Math.cos(theta), s = Math.sin(theta);
        return new Vec2(c * x - s * y, s * x + c * y);
    }
    public static Vec2 polar(double r, double theta) { return new Vec2(r * Math.cos(theta), r * Math.sin(theta)); }
    @Override public String toString() { return String.format("(%.4f, %.4f)", x, y); }
}
