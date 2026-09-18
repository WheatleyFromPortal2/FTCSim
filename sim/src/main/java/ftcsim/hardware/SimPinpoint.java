package ftcsim.hardware;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import ftcsim.physics.Chassis;
import ftcsim.physics.DeadWheel;
import ftcsim.physics.Pose2;
import ftcsim.physics.Units;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit;

import java.util.Map;

/**
 * goBILDA Pinpoint odometry computer. Runs the same dead-reckoning the Pinpoint
 * firmware does (two pods + IMU heading, with the user supplied pod offsets,
 * resolution and directions) on encoder counts produced by the simulated pods,
 * so configuration mistakes show up the way they would on the real robot.
 */
public class SimPinpoint extends GoBildaPinpointDriver implements SimDevice {
    private final String name, hub;
    private final Chassis chassis;
    private final DeadWheel xPod, yPod;
    private final boolean assumeDirectionsCorrect;
    private volatile PoseSnapper snapper;

    // user configuration (what the team writes to the device)
    private volatile double xOffsetMm = 0, yOffsetMm = 0;
    private volatile double mmPerTick = 1.0 / 13.26291192;
    private volatile int dirX = 1, dirY = 1;
    private volatile float yawScalar = 1.0f;

    // firmware state (mm, radians)
    private double posX, posY, heading;
    private double velX, velY, headingVel;
    private double lastTicksX, lastTicksY, lastHeadingRaw;
    private boolean haveLast;
    private double headingOffset;
    private volatile long calibratingUntil;
    private volatile long loopCount;
    private double lastVelTime = -1;
    private int seenTeleportEpoch = -1;

    // snapshot read by update()
    private volatile double rPosX, rPosY, rHeading, rVelX, rVelY, rHeadingVel;
    private volatile int rEncX, rEncY;
    private volatile DeviceStatus rStatus = DeviceStatus.READY;

    public SimPinpoint(String name, String hub, Chassis chassis, DeadWheel xPod, DeadWheel yPod, boolean assumeDirectionsCorrect) {
        super(new VirtualI2cDevice(name), true);
        this.name = name; this.hub = hub; this.chassis = chassis; this.xPod = xPod; this.yPod = yPod;
        this.assumeDirectionsCorrect = assumeDirectionsCorrect;
    }

    public void setPoseSnapper(PoseSnapper s) { snapper = s; }
    private void snap() {
        PoseSnapper s = snapper;
        if (s != null) s.snapTo(Units.mToIn(posX / 1000.0), Units.mToIn(posY / 1000.0), heading);
    }

    /** Called from the physics thread every step: emulates the Pinpoint's internal odometry loop. */
    public synchronized void step(double dt) {
        loopCount++;
        double tx = xPod.ticksExact(), ty = yPod.ticksExact();
        double rawHeading = chassis.pose().heading * yawScalar;
        int epoch = chassis.teleportEpoch();
        if (epoch != seenTeleportEpoch) { seenTeleportEpoch = epoch; haveLast = false; }
        if (!haveLast) { lastTicksX = tx; lastTicksY = ty; lastHeadingRaw = rawHeading; haveLast = true; }
        double dxPod = (tx - lastTicksX) * mmPerTick * dirX;
        double dyPod = (ty - lastTicksY) * mmPerTick * dirY;
        double dTheta = Pose2.normalize(rawHeading - lastHeadingRaw);
        lastTicksX = tx; lastTicksY = ty; lastHeadingRaw = rawHeading;
        if (System.nanoTime() < calibratingUntil) { rStatus = DeviceStatus.CALIBRATING; return; }
        rStatus = DeviceStatus.READY;
        // robot-centre displacement in the robot frame
        double dxR = dxPod + xOffsetMm * dTheta;
        double dyR = dyPod - yOffsetMm * dTheta;
        // pose exponential (arc) integration
        double sinT, cosT;
        if (Math.abs(dTheta) < 1e-9) { sinT = 1; cosT = 0; }
        else { sinT = Math.sin(dTheta) / dTheta; cosT = (1 - Math.cos(dTheta)) / dTheta; }
        double dxA = dxR * sinT - dyR * cosT;
        double dyA = dxR * cosT + dyR * sinT;
        double h = heading;
        posX += dxA * Math.cos(h) - dyA * Math.sin(h);
        posY += dxA * Math.sin(h) + dyA * Math.cos(h);
        heading = Pose2.normalize(rawHeading - headingOffset);
        velX = dxA / dt * Math.cos(h) - dyA / dt * Math.sin(h);
        velY = dxA / dt * Math.sin(h) + dyA / dt * Math.cos(h);
        headingVel = dTheta / dt;
    }

    private synchronized void snapshot() {
        rPosX = posX; rPosY = posY; rHeading = heading; rVelX = velX; rVelY = velY; rHeadingVel = headingVel;
        rEncX = (int) Math.round(xPod.ticksExact() * dirX); rEncY = (int) Math.round(yPod.ticksExact() * dirY);
    }

    // ---- GoBildaPinpointDriver API ----
    @Override public void update() { snapshot(); }
    @Override public void update(ReadData data) { snapshot(); }
    @Override public void setOffsets(double xOffset, double yOffset, DistanceUnit unit) { xOffsetMm = unit.toMm(xOffset); yOffsetMm = unit.toMm(yOffset); }
    @Override public synchronized void recalibrateIMU() { headingOffset = chassis.pose().heading * yawScalar - heading; calibratingUntil = System.nanoTime() + 250_000_000L; }
    @Override public synchronized void resetPosAndIMU() {
        posX = posY = heading = velX = velY = headingVel = 0;
        snap();
        headingOffset = chassis.pose().heading * yawScalar;
        haveLast = false;
        calibratingUntil = System.nanoTime() + 250_000_000L;
        snapshot();
    }
    @Override public void setEncoderDirections(EncoderDirection xEncoder, EncoderDirection yEncoder) {
        dirX = xEncoder == EncoderDirection.REVERSED ? -1 : 1;
        dirY = yEncoder == EncoderDirection.REVERSED ? -1 : 1;
        if (assumeDirectionsCorrect) { xPod.physicalSign = dirX; yPod.physicalSign = dirY; }
    }
    @Override public void setEncoderResolution(GoBildaOdometryPods pods) {
        mmPerTick = pods == GoBildaOdometryPods.goBILDA_4_BAR_POD ? 1.0 / 19.89436789 : 1.0 / 13.26291192;
    }
    @Override public void setEncoderResolution(double ticksPerUnit, DistanceUnit unit) { mmPerTick = unit.toMm(1.0) / ticksPerUnit; }
    @Override public void setYawScalar(double scalar) { yawScalar = (float) scalar; }
    @Override public synchronized void setPosition(Pose2D pos) {
        posX = pos.getX(DistanceUnit.MM); posY = pos.getY(DistanceUnit.MM);
        double h = pos.getHeading(AngleUnit.RADIANS);
        heading = h;
        snap(); // may teleport the robot so that its true pose matches
        headingOffset = chassis.pose().heading * yawScalar - h;
        haveLast = false;
        snapshot();
    }
    @Override public synchronized void setPosX(double x, DistanceUnit unit) { posX = unit.toMm(x); snapshot(); snap(); }
    @Override public synchronized void setPosY(double y, DistanceUnit unit) { posY = unit.toMm(y); snapshot(); snap(); }
    @Override public synchronized void setHeading(double h, AngleUnit unit) { double r = unit.toRadians(h); heading = r; snap(); headingOffset = chassis.pose().heading * yawScalar - r; haveLast = false; snapshot(); }
    @Override public int getDeviceID() { return 1; }
    @Override public int getDeviceVersion() { return 1; }
    @Override public float getYawScalar() { return yawScalar; }
    @Override public DeviceStatus getDeviceStatus() { return rStatus; }
    @Override public int getLoopTime() { return 650; }
    @Override public double getFrequency() { return 1500.0; }
    @Override public int getEncoderX() { return rEncX; }
    @Override public int getEncoderY() { return rEncY; }
    @Override public double getPosX(DistanceUnit unit) { return unit.fromMm(rPosX); }
    @Override public double getPosY(DistanceUnit unit) { return unit.fromMm(rPosY); }
    @Override public double getHeading(AngleUnit unit) { return unit.fromRadians(rHeading); }
    @Override public double getHeading(UnnormalizedAngleUnit unit) { return unit.fromRadians(rHeading); }
    @Override public double getVelX(DistanceUnit unit) { return unit.fromMm(rVelX); }
    @Override public double getVelY(DistanceUnit unit) { return unit.fromMm(rVelY); }
    @Override public double getHeadingVelocity(UnnormalizedAngleUnit unit) { return unit.fromRadians(rHeadingVel); }
    @Override public float getXOffset(DistanceUnit unit) { return (float) unit.fromMm(xOffsetMm); }
    @Override public float getYOffset(DistanceUnit unit) { return (float) unit.fromMm(yOffsetMm); }
    @Override public Pose2D getPosition() { return new Pose2D(DistanceUnit.MM, rPosX, rPosY, AngleUnit.RADIANS, rHeading); }
    @Override protected synchronized boolean doInitialize() { return true; }
    @Override public Manufacturer getManufacturer() { return Manufacturer.GoBilda; }
    @Override public String getDeviceName() { return "goBILDA® Pinpoint Odometry Computer"; }
    @Override public String getConnectionInfo() { return hub + "; I2C"; }
    @Override public void resetDeviceConfigurationForOpMode() {}
    @Override public void close() {}

    // ---- SimDevice ----
    @Override public String simName() { return name; }
    @Override public String simType() { return "GoBildaPinpointDriver"; }
    @Override public String simHub() { return hub; }
    @Override public int simPort() { return 0; }
    @Override public void fillView(Map<String, Object> v) {
        v.put("xIn", Units.mToIn(rPosX / 1000.0)); v.put("yIn", Units.mToIn(rPosY / 1000.0)); v.put("headingDeg", Math.toDegrees(rHeading));
        v.put("velXInPerSec", Units.mToIn(rVelX / 1000.0)); v.put("velYInPerSec", Units.mToIn(rVelY / 1000.0));
        v.put("encX", rEncX); v.put("encY", rEncY);
        v.put("xOffsetIn", Units.mToIn(xOffsetMm / 1000.0)); v.put("yOffsetIn", Units.mToIn(yOffsetMm / 1000.0));
        v.put("status", rStatus.name()); v.put("dirX", dirX); v.put("dirY", dirY);
    }
}
