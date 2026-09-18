package ftcsim.physics;

import java.util.ArrayList;
import java.util.List;

/**
 * The playing field in the FTC field coordinate system: origin at the centre,
 * +X to the right when the goal wall is on the left, +Y up, +Z up, heading CCW
 * from +X. Lengths in inches.
 */
public final class Field {
    public static final double HALF_SIZE_IN = 72.0;

    public static final class Obstacle {
        public final String name;
        public final double minX, minY, maxX, maxY; // inches
        public final double height;
        public final String color;
        public Obstacle(String name, double minX, double minY, double maxX, double maxY, double height, String color) {
            this.name = name; this.minX = minX; this.minY = minY; this.maxX = maxX; this.maxY = maxY; this.height = height; this.color = color;
        }
    }

    public static final class AprilTag {
        public final int id;
        public final String name;
        public final double x, y, z;      // inches
        public final double yawDeg;       // direction the tag face points (normal), CCW from +X
        public final double sizeIn;
        public AprilTag(int id, String name, double x, double y, double z, double yawDeg, double sizeIn) {
            this.id = id; this.name = name; this.x = x; this.y = y; this.z = z; this.yawDeg = yawDeg; this.sizeIn = sizeIn;
        }
    }

    public String name = "DECODE";
    public final List<Obstacle> obstacles = new ArrayList<>();
    public final List<AprilTag> tags = new ArrayList<>();

    /** The 2025-2026 DECODE field. Tag poses from the SDK's AprilTagGameDatabase. */
    public static Field decode() {
        Field f = new Field();
        f.name = "DECODE";
        // Goals occupy the two corners on the -X wall; tag faces point toward the field centre diagonally.
        f.tags.add(new AprilTag(20, "BlueTarget", -58.3727, -55.6425, 29.5, 54.0, 6.5));
        f.tags.add(new AprilTag(24, "RedTarget", -58.3727, 55.6425, 29.5, -54.0, 6.5));
        // Obelisk (tags 21-23) sits outside the field on the +X side, centred; randomised per match.
        f.tags.add(new AprilTag(21, "Obelisk GPP", 78.0, 0.0, 12.0, 180.0, 6.5));
        f.tags.add(new AprilTag(22, "Obelisk PGP", 78.0, 0.0, 12.0, 180.0, 6.5));
        f.tags.add(new AprilTag(23, "Obelisk PPG", 78.0, 0.0, 12.0, 180.0, 6.5));
        // Goal structures (approximate footprints) in the -X corners.
        f.obstacles.add(new Obstacle("Blue goal", -72, -72, -48, -48, 40, "#2b5fd9"));
        f.obstacles.add(new Obstacle("Red goal", -72, 48, -48, 72, 40, "#d93b2b"));
        return f;
    }

    public static Field empty() { Field f = new Field(); f.name = "Empty"; return f; }
}
