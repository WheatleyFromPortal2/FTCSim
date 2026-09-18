package ftcsim.hardware;

import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.hardware.ImuOrientationOnRobot;
import ftcsim.physics.Chassis;
import ftcsim.physics.Pose2;
import org.firstinspires.ftc.robotcore.external.navigation.*;

import java.util.Map;
import java.util.Random;

/**
 * The hub's built-in IMU (BHI260/BNO055 as exposed through the universal IMU
 * interface). Yaw follows the simulated robot heading; optional noise/drift.
 */
public class SimIMU implements IMU, SimDevice {
    private final String name;
    private final String hub;
    private final Chassis chassis;
    private volatile double yawOffset;          // radians subtracted from heading (resetYaw)
    private volatile double drift;              // accumulated drift, radians
    private volatile double driftRatePerSec;    // rad/s
    private volatile double noiseStdDev;        // rad
    private volatile boolean initialized;
    private ImuOrientationOnRobot orientationOnRobot;
    private final Random random = new Random(42);
    private volatile long lastDriftNanos = System.nanoTime();

    public SimIMU(String name, String hub, Chassis chassis) { this.name = name; this.hub = hub; this.chassis = chassis; }
    public void setNoise(double noiseDeg, double driftDegPerMin) { noiseStdDev = Math.toRadians(noiseDeg); driftRatePerSec = Math.toRadians(driftDegPerMin) / 60.0; }

    private double yawRad() {
        long now = System.nanoTime();
        drift += driftRatePerSec * (now - lastDriftNanos) / 1e9;
        lastDriftNanos = now;
        double noise = noiseStdDev > 0 ? random.nextGaussian() * noiseStdDev : 0;
        return Pose2.normalize(chassis.pose().heading - yawOffset + drift + noise);
    }

    @Override public boolean initialize(Parameters parameters) {
        orientationOnRobot = parameters == null ? null : parameters.imuOrientationOnRobot;
        initialized = true;
        return true;
    }
    @Override public void resetYaw() { yawOffset = chassis.pose().heading + drift; }
    @Override public YawPitchRollAngles getRobotYawPitchRollAngles() { return new YawPitchRollAngles(AngleUnit.RADIANS, yawRad(), 0, 0, System.nanoTime()); }
    @Override public Orientation getRobotOrientation(AxesReference reference, AxesOrder order, AngleUnit angleUnit) {
        return getRobotOrientationAsQuaternion().toOrientation(reference, order, angleUnit);
    }
    @Override public Quaternion getRobotOrientationAsQuaternion() {
        double h = yawRad();
        return new Quaternion((float) Math.cos(h / 2), 0f, 0f, (float) Math.sin(h / 2), System.nanoTime());
    }
    @Override public AngularVelocity getRobotAngularVelocity(AngleUnit angleUnit) {
        return new AngularVelocity(angleUnit, 0f, 0f, (float) angleUnit.fromRadians(chassis.omega()), System.nanoTime());
    }
    @Override public Manufacturer getManufacturer() { return Manufacturer.Lynx; }
    @Override public String getDeviceName() { return "REV Internal IMU (BHI260AP)"; }
    @Override public String getConnectionInfo() { return hub + "; I2C bus 0"; }
    @Override public int getVersion() { return 1; }
    @Override public void resetDeviceConfigurationForOpMode() {}
    @Override public void close() {}
    @Override public String simName() { return name; }
    @Override public String simType() { return "IMU"; }
    @Override public String simHub() { return hub; }
    @Override public int simPort() { return 0; }
    @Override public void fillView(Map<String, Object> v) {
        v.put("yawDeg", Math.toDegrees(yawRad()));
        v.put("yawRateDegPerSec", Math.toDegrees(chassis.omega()));
        v.put("initialized", initialized);
        v.put("orientation", orientationOnRobot == null ? "-" : orientationOnRobot.getClass().getSimpleName());
    }
}
