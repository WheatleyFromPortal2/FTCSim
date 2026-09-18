package org.firstinspires.ftc.vision.apriltag;

import android.graphics.Canvas;
import com.qualcomm.robotcore.util.RobotLog;
import ftcsim.physics.Field;
import ftcsim.physics.Pose2;
import ftcsim.physics.Units;
import ftcsim.physics.Vec2;
import ftcsim.vision.CameraMount;
import ftcsim.vision.SimVision;
import ftcsim.vision.SimVisionProcessor;
import org.firstinspires.ftc.robotcore.external.matrices.GeneralMatrixF;
import org.firstinspires.ftc.robotcore.external.matrices.MatrixF;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.matrices.VectorF;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibration;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FTCSim replacement for the SDK's AprilTag detector. Detections are computed geometrically from the
 * true robot pose, the camera mount and the tag library (tags the library leaves unplaced, such as
 * the DECODE obelisk, take their position from the simulated field).
 */
public class AprilTagProcessorImpl extends AprilTagProcessor implements SimVisionProcessor {
    public static final String TAG = "AprilTagProcessorImpl";

    private final OpenGLMatrix cameraPoseInverse;
    private final double fx, fy;
    private final DistanceUnit outputUnitsLength;
    private final AngleUnit outputUnitsAngle;
    private final AprilTagLibrary tagLibrary;
    private final Random random = new Random(11);
    private volatile ArrayList<AprilTagDetection> detections = new ArrayList<>();
    private final AtomicBoolean fresh = new AtomicBoolean(false);
    private volatile String summary = "";
    private boolean warnedLookingUp;

    private static final Constructor<?> DETECTION_CTOR;
    private static final boolean DETECTION_HAS_UNIT;
    static {
        Constructor<?> c = null; boolean hasUnit = false;
        try {
            Class<?> single = Class.forName("org.firstinspires.ftc.vision.apriltag.AprilTagSingleDetection");
            c = single.getConstructor(int.class, int.class, float.class, Point.class, Point[].class, AprilTagMetadata.class, AprilTagPoseFtc.class, AprilTagPoseRaw.class, Pose3D.class, long.class, DistanceUnit.class);
            hasUnit = true;
        } catch (Throwable ignored) {
            try {
                c = AprilTagDetection.class.getConstructor(int.class, int.class, float.class, Point.class, Point[].class, AprilTagMetadata.class, AprilTagPoseFtc.class, AprilTagPoseRaw.class, Pose3D.class, long.class);
            } catch (NoSuchMethodException e) { throw new ExceptionInInitializerError(e); }
        }
        DETECTION_CTOR = c; DETECTION_HAS_UNIT = hasUnit;
    }

    public AprilTagProcessorImpl(OpenGLMatrix cameraPose, double fx, double fy, double cx, double cy, DistanceUnit outputUnitsLength, AngleUnit outputUnitsAngle,
                                 AprilTagLibrary tagLibrary, boolean drawAxes, boolean drawCube, boolean drawOutline, boolean drawTagID,
                                 TagFamily tagFamily, int threads, boolean suppressCalibrationWarnings) {
        this.cameraPoseInverse = cameraPose;
        this.fx = fx; this.fy = fy;
        this.outputUnitsLength = outputUnitsLength == null ? DistanceUnit.INCH : outputUnitsLength;
        this.outputUnitsAngle = outputUnitsAngle == null ? AngleUnit.DEGREES : outputUnitsAngle;
        this.tagLibrary = tagLibrary == null ? AprilTagGameDatabase.getCurrentGameTagLibrary() : tagLibrary;
    }

    // ---- VisionProcessor (no real frames) ----
    @Override public void init(int width, int height, CameraCalibration calibration) {}
    @Override public Object processFrame(Mat frame, long captureTimeNanos) { return null; }
    @Override public void onDrawFrame(Canvas canvas, int onscreenWidth, int onscreenHeight, float scaleBmpPxToCanvasPx, float scaleCanvasDensity, Object userContext) {}
    @Override public void setDecimation(float decimation) {}
    @Override public void setPoseSolver(PoseSolver poseSolver) {}
    @Override public int getPerTagAvgPoseSolveTime() { return 0; }
    @Override public ArrayList<AprilTagDetection> getDetections() { return detections; }
    @Override public ArrayList<AprilTagDetection> getFreshDetections() { return fresh.getAndSet(false) ? detections : null; }
    @Override public String simSummary() { return summary; }

    /** A tag's placement on the field: inches, yaw = direction the face points (CCW from +X). */
    private static final class Placement { double x, y, z, yawDeg, sizeIn; }

    private Placement placement(AprilTagMetadata m) {
        VectorF p = m.fieldPosition;
        Quaternion q = m.fieldOrientation;
        boolean placed = p != null && (Math.abs(p.get(0)) > 1e-6 || Math.abs(p.get(1)) > 1e-6 || Math.abs(p.get(2)) > 1e-6);
        Placement pl = new Placement();
        DistanceUnit unit = m.distanceUnit == null ? DistanceUnit.INCH : m.distanceUnit;
        pl.sizeIn = DistanceUnit.INCH.fromUnit(unit, m.tagsize);
        if (placed && q != null) {
            pl.x = DistanceUnit.INCH.fromUnit(unit, p.get(0)); pl.y = DistanceUnit.INCH.fromUnit(unit, p.get(1)); pl.z = DistanceUnit.INCH.fromUnit(unit, p.get(2));
            VectorF z = q.applyToVector(new VectorF(0, 0, 1)); // the tag's +Z points into the tag; the face points the other way
            pl.yawDeg = Math.toDegrees(Math.atan2(-z.get(1), -z.get(0)));
            return pl;
        }
        Field f = SimVision.field();
        if (f == null) return null;
        for (Field.AprilTag t : f.tags) {
            if (t.id != m.id) continue;
            if (t.id >= 21 && t.id <= 23 && t.id != SimVision.obeliskTag()) return null; // only the chosen obelisk face is on the field
            pl.x = t.x; pl.y = t.y; pl.z = t.z; pl.yawDeg = t.yawDeg; if (pl.sizeIn <= 0) pl.sizeIn = t.sizeIn;
            return pl;
        }
        return null;
    }

    @Override public void simFrame(CameraMount mount, int widthPx, int heightPx, long frameNanos) {
        Pose2 p = SimVision.robotPose();
        double rxIn = Units.mToIn(p.x), ryIn = Units.mToIn(p.y);
        // camera on the robot: from setCameraPose() when the team gave one, else the configured mount
        double camX = mount.xIn, camY = mount.yIn, camZ = mount.zIn, camYawRel = Math.toRadians(mount.yawDeg), camPitch = Math.toRadians(mount.pitchDeg);
        if (cameraPoseInverse != null) {
            OpenGLMatrix cam = cameraPoseInverse.inverted();
            float fX = cam.get(0, 2), fY = cam.get(1, 2), fZ = cam.get(2, 2); // camera optical axis in the robot frame (X right, Y forward, Z up)
            boolean identity = Math.abs(cam.get(0, 3)) < 1e-6 && Math.abs(cam.get(1, 3)) < 1e-6 && Math.abs(cam.get(2, 3)) < 1e-6 && Math.abs(fZ - 1) < 1e-6;
            if (!identity) {
                if (Math.abs(fZ) > 0.99) {
                    if (!warnedLookingUp) { warnedLookingUp = true; RobotLog.ww(TAG, "[sim] setCameraPose() leaves the camera looking straight up/down; using the configured camera mount instead"); }
                } else {
                    camX = cam.get(1, 3); camY = -cam.get(0, 3); camZ = cam.get(2, 3);
                    camYawRel = Math.atan2(-fX, fY);
                    camPitch = Math.atan2(fZ, Math.hypot(fX, fY));
                }
            }
        }
        Vec2 camWorld = new Vec2(rxIn, ryIn).plus(new Vec2(camX, camY).rotated(p.heading));
        double camYaw = Pose2.normalize(p.heading + camYawRel);
        double hfov = Math.toRadians(mount.hfovDeg), vfov = Math.toRadians(mount.vfovDeg);
        double focal = fx > 0 ? fx : (widthPx / 2.0) / Math.tan(hfov / 2);
        double focalV = fy > 0 ? fy : (heightPx / 2.0) / Math.tan(vfov / 2);
        ArrayList<AprilTagDetection> found = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();
        for (AprilTagMetadata meta : tagLibrary.getAllTags()) {
            Placement t = placement(meta);
            if (t == null) continue;
            Vec2 rel = new Vec2(t.x - camWorld.x, t.y - camWorld.y);
            double dist = rel.norm();
            if (dist < 2) continue;
            Vec2 normal = Vec2.polar(1, Math.toRadians(t.yawDeg));
            double facing = normal.dot(rel.times(-1 / dist));
            if (facing < 0.25) continue;
            Vec2 inCam = rel.rotated(-camYaw); // x forward, y left
            if (inCam.x <= 0) continue;
            double az = Math.atan2(inCam.y, inCam.x);
            if (Math.abs(az) > hfov / 2) continue;
            double dz = t.z - camZ;
            double el = Math.atan2(dz, inCam.x) - camPitch;
            if (Math.abs(el) > vfov / 2) continue;
            double range3 = Math.sqrt(inCam.x * inCam.x + inCam.y * inCam.y + dz * dz);
            if (range3 > mount.maxRangeIn) continue;
            double apparentPx = t.sizeIn * focal / range3 * facing;
            if (apparentPx < 6) continue;
            // camera frame used by ftcPose: x right, y forward (along the optical axis), z up
            double xF = -inCam.y;
            double yF = inCam.x * Math.cos(camPitch) + dz * Math.sin(camPitch);
            double zF = -inCam.x * Math.sin(camPitch) + dz * Math.cos(camPitch);
            double noise = mount.noiseIn * Math.max(1, range3 / 48);
            xF += random.nextGaussian() * noise; yF += random.nextGaussian() * noise; zF += random.nextGaussian() * noise * 0.5;
            double range = Math.sqrt(xF * xF + yF * yF + zF * zF);
            double bearing = Math.toDegrees(Math.atan2(-xF, yF));
            double elevation = Math.toDegrees(Math.atan2(zF, Math.hypot(xF, yF)));
            double yaw = Pose2.normalizeDeg(t.yawDeg - Math.toDegrees(camYaw) + 180) + random.nextGaussian() * mount.noiseDeg;
            double pitch = random.nextGaussian() * mount.noiseDeg, roll = random.nextGaussian() * mount.noiseDeg;
            DistanceUnit du = outputUnitsLength; AngleUnit au = outputUnitsAngle;
            AprilTagPoseFtc ftcPose = new AprilTagPoseFtc(du.fromInches(xF), du.fromInches(yF), du.fromInches(zF), au.fromDegrees(yaw), au.fromDegrees(pitch), au.fromDegrees(roll), du.fromInches(range), au.fromDegrees(bearing), au.fromDegrees(elevation));
            double yr = Math.toRadians(yaw);
            MatrixF R = new GeneralMatrixF(3, 3, new float[] { (float) Math.cos(yr), 0, (float) Math.sin(yr), 0, 1, 0, (float) -Math.sin(yr), 0, (float) Math.cos(yr) });
            AprilTagPoseRaw rawPose = new AprilTagPoseRaw(du.fromInches(xF), du.fromInches(-zF), du.fromInches(yF), R);
            double nx = rxIn + random.nextGaussian() * noise * 0.5, ny = ryIn + random.nextGaussian() * noise * 0.5;
            double nh = Math.toDegrees(p.heading) + random.nextGaussian() * mount.noiseDeg;
            Pose3D robotPose = new Pose3D(new Position(du, du.fromInches(nx), du.fromInches(ny), du.fromInches(0), frameNanos), new YawPitchRollAngles(au, au.fromDegrees(nh), 0, 0, frameNanos));
            double txp = widthPx / 2.0 - Math.tan(az) * focal, typ = heightPx / 2.0 - Math.tan(el) * focalV;
            double half = t.sizeIn / 2 * focal / range3;
            Point center = new Point(txp, typ);
            Point[] corners = { new Point(txp - half, typ + half), new Point(txp + half, typ + half), new Point(txp + half, typ - half), new Point(txp - half, typ - half) };
            float margin = (float) Math.max(5, 120 - range3 / 2);
            try {
                Object det = DETECTION_HAS_UNIT
                    ? DETECTION_CTOR.newInstance(meta.id, 0, margin, center, corners, meta, ftcPose, rawPose, robotPose, frameNanos, du)
                    : DETECTION_CTOR.newInstance(meta.id, 0, margin, center, corners, meta, ftcPose, rawPose, robotPose, frameNanos);
                found.add((AprilTagDetection) det);
                ids.add(meta.id);
            } catch (ReflectiveOperationException e) {
                RobotLog.ee(TAG, e, "[sim] could not create AprilTagDetection");
            }
        }
        detections = found;
        fresh.set(true);
        summary = ids.isEmpty() ? "no tags" : "tags " + ids;
    }
}
