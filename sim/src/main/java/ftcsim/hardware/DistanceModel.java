package ftcsim.hardware;

import ftcsim.physics.Chassis;
import ftcsim.physics.Field;
import ftcsim.physics.Pose2;
import ftcsim.physics.Units;
import ftcsim.physics.Vec2;

import java.util.Map;
import java.util.function.Supplier;

/** Shared model of a time-of-flight distance sensor: a value set in the UI, or a ray cast against the field. */
final class DistanceModel {
    static final double MAX_MM = 2000;
    private final Chassis chassis;
    private final Supplier<Field> field;
    private volatile double manualMm = 8190; // out of range like the real sensor
    private volatile boolean raycast;
    private volatile double mountX, mountY, mountYaw; // metres / radians, robot frame

    DistanceModel(Chassis chassis, Supplier<Field> field) { this.chassis = chassis; this.field = field; }

    void setMount(double xIn, double yIn, double yawDeg) { mountX = Units.inToM(xIn); mountY = Units.inToM(yIn); mountYaw = Math.toRadians(yawDeg); raycast = true; }

    double measureMm() {
        if (!raycast) return manualMm;
        Pose2 p = chassis.pose();
        Vec2 origin = new Vec2(p.x, p.y).plus(new Vec2(mountX, mountY).rotated(p.heading));
        double dir = p.heading + mountYaw;
        Vec2 d = new Vec2(Math.cos(dir), Math.sin(dir));
        double half = Field.HALF_SIZE_IN * Units.INCH;
        double best = rayToAabb(origin, d, -half, -half, half, half, true);
        Field f = field.get();
        if (f != null) for (Field.Obstacle o : f.obstacles) {
            best = Math.min(best, rayToAabb(origin, d, o.minX * Units.INCH, o.minY * Units.INCH, o.maxX * Units.INCH, o.maxY * Units.INCH, false));
        }
        double mm = best * 1000.0;
        return mm > MAX_MM ? 8190 : mm;
    }

    /** Distance along the ray to an axis-aligned box; from inside (walls) the exit distance, from outside the entry distance. */
    static double rayToAabb(Vec2 o, Vec2 d, double minX, double minY, double maxX, double maxY, boolean inside) {
        double tmin = Double.NEGATIVE_INFINITY, tmax = Double.POSITIVE_INFINITY;
        for (int axis = 0; axis < 2; axis++) {
            double oa = axis == 0 ? o.x : o.y, da = axis == 0 ? d.x : d.y;
            double lo = axis == 0 ? minX : minY, hi = axis == 0 ? maxX : maxY;
            if (Math.abs(da) < 1e-12) { if (oa < lo || oa > hi) return Double.POSITIVE_INFINITY; continue; }
            double t1 = (lo - oa) / da, t2 = (hi - oa) / da;
            if (t1 > t2) { double t = t1; t1 = t2; t2 = t; }
            tmin = Math.max(tmin, t1); tmax = Math.min(tmax, t2);
            if (tmin > tmax) return Double.POSITIVE_INFINITY;
        }
        if (inside) return tmax >= 0 ? tmax : Double.POSITIVE_INFINITY;
        return tmin >= 0 ? tmin : (tmax >= 0 ? 0 : Double.POSITIVE_INFINITY);
    }

    void fillView(Map<String, Object> v) { v.put("distanceMm", measureMm()); v.put("raycast", raycast); v.put("input", !raycast); }
    boolean applyInput(String key, Object value) {
        if ("distanceMm".equals(key)) { manualMm = SimDevice.asDouble(value, manualMm); raycast = false; return true; }
        if ("raycast".equals(key)) { raycast = SimDevice.asBool(value, raycast); return true; }
        return false;
    }
}
