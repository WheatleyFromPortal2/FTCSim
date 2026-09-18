package ftcsim.hardware;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.util.RobotLog;
import ftcsim.physics.Chassis;
import ftcsim.physics.Pose2;
import ftcsim.physics.Vec2;
import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.AngularVelocity;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.MagneticFlux;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion;
import org.firstinspires.ftc.robotcore.external.navigation.Temperature;
import org.firstinspires.ftc.robotcore.external.navigation.Velocity;

import java.util.Map;

/**
 * The legacy BNO055 IMU interface (pre-SDK 8.1 code) backed by the simulated chassis. Heading is the
 * Z angle of an intrinsic ZYX orientation, counter-clockwise positive, like the SDK driver reports.
 */
public class SimBNO055IMU implements BNO055IMU, HardwareDevice, SimDevice {
    private final String name, hub;
    private final Chassis chassis;
    private volatile Parameters parameters = new Parameters();
    private volatile boolean initialized;
    private volatile Pose2 integrationOrigin;
    private volatile Position integrationStart = new Position();
    private volatile Velocity velocityStart = new Velocity();

    public SimBNO055IMU(String name, String hub, Chassis chassis) { this.name = name; this.hub = hub; this.chassis = chassis; }

    private double heading() { return chassis.pose().heading; }
    private org.firstinspires.ftc.robotcore.external.navigation.AngleUnit angleUnit() { return parameters.angleUnit == null ? org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.DEGREES : parameters.angleUnit.toAngleUnit(); }

    @Override public boolean initialize(Parameters p) {
        parameters = p == null ? new Parameters() : p;
        initialized = true;
        RobotLog.ii("BNO055", "[sim] %s initialised (mode %s)", name, parameters.mode);
        return true;
    }
    @Override public Parameters getParameters() { return parameters; }
    @Override public void close() {}

    @Override public Orientation getAngularOrientation() { return getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, angleUnit()); }
    @Override public Orientation getAngularOrientation(AxesReference reference, AxesOrder order, org.firstinspires.ftc.robotcore.external.navigation.AngleUnit unit) {
        Orientation o = new Orientation(AxesReference.INTRINSIC, AxesOrder.ZYX, org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.RADIANS, (float) heading(), 0f, 0f, System.nanoTime());
        return o.toAxesReference(reference).toAxesOrder(order).toAngleUnit(unit);
    }
    @Override public Acceleration getOverallAcceleration() { Vec2 a = chassis.accelerationRobot(); return new Acceleration(DistanceUnit.METER, a.x, a.y, 9.81, System.nanoTime()); }
    @Override public AngularVelocity getAngularVelocity() {
        org.firstinspires.ftc.robotcore.external.navigation.AngleUnit u = angleUnit();
        return new AngularVelocity(u, 0f, 0f, (float) u.fromRadians(chassis.omega()), System.nanoTime());
    }
    @Override public Acceleration getLinearAcceleration() { Vec2 a = chassis.accelerationRobot(); return new Acceleration(DistanceUnit.METER, a.x, a.y, 0, System.nanoTime()); }
    @Override public Acceleration getGravity() { return new Acceleration(DistanceUnit.METER, 0, 0, 9.81, System.nanoTime()); }
    @Override public Temperature getTemperature() { return new Temperature(org.firstinspires.ftc.robotcore.external.navigation.TempUnit.CELSIUS, 25, System.nanoTime()); }
    @Override public MagneticFlux getMagneticFieldStrength() { return new MagneticFlux(0, 0, 0, System.nanoTime()); }
    @Override public Quaternion getQuaternionOrientation() { double h = heading(); return new Quaternion((float) Math.cos(h / 2), 0f, 0f, (float) Math.sin(h / 2), System.nanoTime()); }
    @Override public Position getPosition() {
        Pose2 o = integrationOrigin;
        if (o == null) return new Position();
        Pose2 p = chassis.pose();
        return new Position(DistanceUnit.METER, integrationStart.toUnit(DistanceUnit.METER).x + (p.x - o.x), integrationStart.toUnit(DistanceUnit.METER).y + (p.y - o.y), integrationStart.toUnit(DistanceUnit.METER).z, System.nanoTime());
    }
    @Override public Velocity getVelocity() { Vec2 v = chassis.velocityField(); return integrationOrigin == null ? new Velocity() : new Velocity(DistanceUnit.METER, v.x, v.y, 0, System.nanoTime()); }
    @Override public Acceleration getAcceleration() { return getLinearAcceleration(); }
    @Override public void startAccelerationIntegration(Position initialPosition, Velocity initialVelocity, int msPollInterval) {
        integrationStart = initialPosition == null ? new Position(DistanceUnit.METER, 0, 0, 0, 0) : initialPosition;
        velocityStart = initialVelocity == null ? new Velocity() : initialVelocity;
        integrationOrigin = chassis.pose();
    }
    @Override public void stopAccelerationIntegration() { integrationOrigin = null; }
    @Override public SystemStatus getSystemStatus() { return initialized ? SystemStatus.RUNNING_FUSION : SystemStatus.IDLE; }
    @Override public SystemError getSystemError() { return SystemError.NO_ERROR; }
    @Override public CalibrationStatus getCalibrationStatus() { return new CalibrationStatus(0xFF); }
    @Override public boolean isSystemCalibrated() { return true; }
    @Override public boolean isGyroCalibrated() { return true; }
    @Override public boolean isAccelerometerCalibrated() { return true; }
    @Override public boolean isMagnetometerCalibrated() { return true; }
    @Override public CalibrationData readCalibrationData() { return new CalibrationData(); }
    @Override public void writeCalibrationData(CalibrationData data) {}
    @Override public byte read8(Register reg) { return 0; }
    @Override public byte[] read(Register reg, int cb) { return new byte[Math.max(0, cb)]; }
    @Override public void write8(Register reg, int data) {}
    @Override public void write(Register reg, byte[] data) {}

    @Override public Manufacturer getManufacturer() { return Manufacturer.Lynx; }
    @Override public String getDeviceName() { return "BNO055 IMU (simulated)"; }
    @Override public String getConnectionInfo() { return hub + "; bus 0"; }
    @Override public int getVersion() { return 1; }
    @Override public void resetDeviceConfigurationForOpMode() { initialized = false; integrationOrigin = null; }

    @Override public String simName() { return name; }
    @Override public String simType() { return "BNO055IMU"; }
    @Override public String simHub() { return hub; }
    @Override public int simPort() { return 0; }
    @Override public void fillView(Map<String, Object> v) {
        v.put("headingDeg", Math.toDegrees(heading()));
        v.put("yawRateDegPerSec", Math.toDegrees(chassis.omega()));
        v.put("initialized", initialized);
        v.put("mode", parameters.mode == null ? "" : parameters.mode.name());
    }
}
