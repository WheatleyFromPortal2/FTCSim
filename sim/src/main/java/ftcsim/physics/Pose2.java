package ftcsim.physics;

/** Immutable 2D pose: position plus heading (radians, CCW positive). */
public final class Pose2 {
    public final double x, y, heading;
    public Pose2(double x, double y, double heading) { this.x = x; this.y = y; this.heading = heading; }
    public Vec2 position() { return new Vec2(x, y); }
    public Pose2 withHeading(double h) { return new Pose2(x, y, h); }
    public static double normalize(double angle) {
        angle %= 2 * Math.PI;
        if (angle > Math.PI) angle -= 2 * Math.PI;
        if (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }
    public static double normalizeDeg(double deg) {
        deg %= 360;
        if (deg > 180) deg -= 360;
        if (deg < -180) deg += 360;
        return deg;
    }
    @Override public String toString() { return String.format("(%.3f, %.3f, %.1f deg)", x, y, Math.toDegrees(heading)); }
}
