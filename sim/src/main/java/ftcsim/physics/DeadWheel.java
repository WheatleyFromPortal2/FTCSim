package ftcsim.physics;

/**
 * An odometry pod: a free-spinning encoder wheel at a fixed position on the
 * robot measuring motion along one direction.
 */
public final class DeadWheel {
    public final String name;
    public final Vec2 position;      // robot frame, metres (x forward, y left)
    public final double direction;   // radians; 0 = measures forward motion, PI/2 = measures leftward motion
    public final double radius;      // metres
    public final double ticksPerRev;
    public int physicalSign = 1;
    private double angle;

    public DeadWheel(String name, Vec2 position, double direction, double radius, double ticksPerRev) {
        this.name = name; this.position = position; this.direction = direction; this.radius = radius; this.ticksPerRev = ticksPerRev;
    }

    /** @param vRobot robot-frame velocity (m/s), @param omega yaw rate (rad/s) */
    public void step(Vec2 vRobot, double omega, double dt) {
        Vec2 contact = new Vec2(vRobot.x - omega * position.y, vRobot.y + omega * position.x);
        double s = contact.x * Math.cos(direction) + contact.y * Math.sin(direction);
        angle += s / radius * dt;
    }

    public int ticks() { return (int) Math.round(physicalSign * angle / (2 * Math.PI) * ticksPerRev); }
    public double ticksExact() { return physicalSign * angle / (2 * Math.PI) * ticksPerRev; }
    public double distanceMeters() { return physicalSign * angle * radius; }
    public void reset() { angle = 0; }
}
