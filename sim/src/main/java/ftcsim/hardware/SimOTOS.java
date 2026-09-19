package ftcsim.hardware;

import com.qualcomm.hardware.sparkfun.SparkFunOTOS;
import ftcsim.physics.Chassis;
import ftcsim.physics.Pose2;
import ftcsim.physics.Units;
import ftcsim.physics.Vec2;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

import java.util.Map;

/** SparkFun Optical Tracking Odometry Sensor: reports the simulated pose (with an optional mounting offset). */
public class SimOTOS extends SparkFunOTOS implements SimDevice {
    private final String name, hub;
    private final Chassis chassis;
    private volatile double linearScalar = 1.0, angularScalar = 1.0;
    private volatile Pose2D offset = new Pose2D();
    private volatile SignalProcessConfig signalConfig = new SignalProcessConfig();
    // tracking origin: the pose in the field frame that reads as (0,0,0)
    private double originX, originY, originH;
    private volatile PoseSnapper snapper;
    public void setPoseSnapper(PoseSnapper s) { snapper = s; }

    public SimOTOS(String name, String hub, Chassis chassis) {
        super(new VirtualI2cDevice(name));
        this.name = name; this.hub = hub; this.chassis = chassis;
        _distanceUnit = DistanceUnit.INCH;
        _angularUnit = AngleUnit.DEGREES;
        resetTracking();
    }

    /** True sensor pose in metres/radians (robot pose composed with the mounting offset). */
    private Pose2 sensorPose() {
        Pose2 p = chassis.pose();
        Vec2 off = new Vec2(_distanceUnit.toMeters(offset.x), _distanceUnit.toMeters(offset.y)).rotated(p.heading);
        return new Pose2(p.x + off.x, p.y + off.y, Pose2.normalize(p.heading + _angularUnit.toRadians(offset.h)));
    }

    private synchronized Pose2D relative(Pose2 sp) {
        double dx = sp.x - originX, dy = sp.y - originY;
        double c = Math.cos(-originH), s = Math.sin(-originH);
        double lx = (c * dx - s * dy) * linearScalar, ly = (s * dx + c * dy) * linearScalar;
        double h = Pose2.normalize(sp.heading - originH) * angularScalar;
        return new Pose2D(_distanceUnit.fromMeters(lx), _distanceUnit.fromMeters(ly), _angularUnit.fromRadians(h));
    }

    @Override protected boolean doInitialize() { return true; }
    @Override public Manufacturer getManufacturer() { return Manufacturer.SparkFun; }
    @Override public String getDeviceName() { return "SparkFun OTOS"; }
    @Override public String getConnectionInfo() { return hub + "; I2C"; }
    @Override public boolean begin() { return true; }
    @Override public boolean isConnected() { return true; }
    @Override public void getVersionInfo(Version hwVersion, Version fwVersion) { hwVersion.major = 1; hwVersion.minor = 0; fwVersion.major = 1; fwVersion.minor = 0; }
    @Override public boolean selfTest() { return true; }
    @Override public boolean calibrateImu() { return true; }
    @Override public boolean calibrateImu(int numSamples, boolean waitUntilDone) { return true; }
    @Override public int getImuCalibrationProgress() { return 0; }
    @Override public DistanceUnit getLinearUnit() { return _distanceUnit; }
    @Override public void setLinearUnit(DistanceUnit unit) { _distanceUnit = unit; }
    @Override public AngleUnit getAngularUnit() { return _angularUnit; }
    @Override public void setAngularUnit(AngleUnit unit) { _angularUnit = unit; }
    @Override public double getLinearScalar() { return linearScalar; }
    @Override public boolean setLinearScalar(double scalar) { if (scalar < MIN_SCALAR || scalar > MAX_SCALAR) return false; linearScalar = scalar; return true; }
    @Override public double getAngularScalar() { return angularScalar; }
    @Override public boolean setAngularScalar(double scalar) { if (scalar < MIN_SCALAR || scalar > MAX_SCALAR) return false; angularScalar = scalar; return true; }
    @Override public void resetTracking() {
        HardwareBus.i2c(); // charged outside the lock: the physics thread samples this device too
        synchronized (this) { Pose2 sp = sensorPose(); originX = sp.x; originY = sp.y; originH = sp.heading; }
    }
    @Override public SignalProcessConfig getSignalProcessConfig() { return signalConfig; }
    @Override public void setSignalProcessConfig(SignalProcessConfig config) { signalConfig = config; }
    @Override public Status getStatus() { return new Status(); }
    @Override public Pose2D getOffset() { return offset; }
    @Override public void setOffset(Pose2D pose) { offset = pose; }
    @Override public Pose2D getPosition() { Pose2D p = relative(sensorPose()); HardwareBus.i2c(); return p; }
    @Override public void setPosition(Pose2D pose) {
        HardwareBus.i2c();
        PoseSnapper sn;
        synchronized (this) {
            // choose the origin so the current sensor pose reads as the given pose
            Pose2 sp = sensorPose();
            double h = _angularUnit.toRadians(pose.h) / angularScalar;
            originH = Pose2.normalize(sp.heading - h);
            double lx = _distanceUnit.toMeters(pose.x) / linearScalar, ly = _distanceUnit.toMeters(pose.y) / linearScalar;
            double c = Math.cos(originH), s = Math.sin(originH);
            originX = sp.x - (c * lx - s * ly);
            originY = sp.y - (s * lx + c * ly);
            sn = snapper;
        }
        if (sn != null) sn.snapTo(_distanceUnit.toInches(pose.x), _distanceUnit.toInches(pose.y), _angularUnit.toRadians(pose.h));
    }
    @Override public Pose2D getVelocity() {
        HardwareBus.i2c();
        Vec2 v = chassis.velocityField().rotated(-originH).times(linearScalar);
        return new Pose2D(_distanceUnit.fromMeters(v.x), _distanceUnit.fromMeters(v.y), _angularUnit.fromRadians(chassis.omega() * angularScalar));
    }
    @Override public Pose2D getAcceleration() {
        Vec2 a = chassis.accelerationRobot().rotated(chassis.pose().heading - originH);
        return new Pose2D(_distanceUnit.fromMeters(a.x), _distanceUnit.fromMeters(a.y), 0);
    }
    @Override public Pose2D getPositionStdDev() { return new Pose2D(0.01, 0.01, 0.1); }
    @Override public Pose2D getVelocityStdDev() { return new Pose2D(0.01, 0.01, 0.1); }
    @Override public Pose2D getAccelerationStdDev() { return new Pose2D(0.01, 0.01, 0.1); }
    @Override public void getPosVelAcc(Pose2D pos, Pose2D vel, Pose2D acc) { pos.set(getPosition()); vel.set(getVelocity()); acc.set(getAcceleration()); }
    @Override public void getPosVelAccStdDev(Pose2D pos, Pose2D vel, Pose2D acc) { pos.set(getPositionStdDev()); vel.set(getVelocityStdDev()); acc.set(getAccelerationStdDev()); }
    @Override public void getPosVelAccAndStdDev(Pose2D pos, Pose2D vel, Pose2D acc, Pose2D posStdDev, Pose2D velStdDev, Pose2D accStdDev) { getPosVelAcc(pos, vel, acc); getPosVelAccStdDev(posStdDev, velStdDev, accStdDev); }
    @Override public void resetDeviceConfigurationForOpMode() {}
    @Override public void close() {}
    @Override public String simName() { return name; }
    @Override public String simType() { return "SparkFunOTOS"; }
    @Override public String simHub() { return hub; }
    @Override public int simPort() { return 0; }
    @Override public void fillView(Map<String, Object> v) {
        Pose2D p = getPosition();
        v.put("x", p.x); v.put("y", p.y); v.put("h", p.h); v.put("units", _distanceUnit + "/" + _angularUnit);
        v.put("linearScalar", linearScalar); v.put("angularScalar", angularScalar);
    }
}
