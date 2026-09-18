package ftcsim.physics;

import com.qualcomm.robotcore.hardware.DcMotor;

/**
 * Simulation state of one motor port: the control loop the REV hub would run
 * for it (open loop, velocity, run-to-position) and, for motors that are not
 * drivetrain wheels, a simple rotational load that the motor drives.
 * <p>
 * "Controller frame" quantities are what the hub sees (positive = the motor's
 * nominal forward); "physical" quantities are in the mechanism's forward
 * direction. {@link #physicalSign} relates the two.
 */
public final class MotorState {
    public final int port;
    public final String name;
    public MotorType type;
    private MotorModel model;

    // ---- commands from the SDK side (written by the controller, read by physics) ----
    public volatile double power;                       // -1..1, controller frame
    public volatile DcMotor.RunMode mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER;
    public volatile DcMotor.ZeroPowerBehavior zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE;
    public volatile boolean enabled = true;
    public volatile int targetPosition;
    public volatile boolean targetPositionSet;
    public volatile int targetTolerance = 5;
    public volatile double targetVelocityTps;           // when using setVelocity()
    public volatile boolean useVelocityTarget;
    /** Max ticks/s of the motor type as configured in software (MotorConfigurationType), before the fraction. */
    public volatile double configuredMaxTps;
    public volatile double achievableMaxTpsFraction = 0.85; // MotorConfigurationType.achieveableMaxRPMFraction
    public volatile double currentAlertAmps = 9.2;
    public final PidfController velocityPidf;
    public volatile double positionP = 5.0;
    public volatile boolean emulateVelocityOverflow = true;

    // ---- physical state ----
    /**
     * Sign relating the controller (raw) frame to the physical "wheel/mechanism forward" frame:
     * {@code physical = physicalSign * raw}. It is the product of {@link #mountSign} (how the motor is
     * mounted: does SDK-FORWARD drive the mechanism forward?) and {@link #orientationSign} (the SDK's own
     * inversion for CCW motor types, see {@link MotorType#ccw}).
     */
    public int physicalSign = 1;
    public int mountSign = 1;
    public int orientationSign = 1;
    public boolean isDriveWheel;
    public double inertia;                // kg m^2 at the output shaft (non-drive motors)
    public double viscousFriction;        // N m s / rad
    public double coulombFriction;        // N m
    public double minAngle = Double.NEGATIVE_INFINITY, maxAngle = Double.POSITIVE_INFINITY; // physical, rad
    public double gravityTorque;          // N m, constant load (e.g. a lift), positive opposes "forward"
    private double angle;                 // physical, rad, output shaft
    private double omega;                 // physical, rad/s
    private double encoderOffset;         // controller frame, rad
    private volatile double appliedVolts;
    private volatile double current;
    private volatile double lastDuty;
    private volatile boolean busy;

    public MotorState(int port, String name, MotorType type) {
        this.port = port; this.name = name;
        setType(type);
        velocityPidf = new PidfController(0, 0, 0, 0);
        resetDefaultVelocityPidf();
    }

    /** Sets how the motor is mounted (+1: SDK FORWARD drives the mechanism forward) and recomputes {@link #physicalSign}. */
    public void setMountSign(int sign) { mountSign = sign < 0 ? -1 : 1; physicalSign = mountSign * orientationSign; }

    public void setType(MotorType t) {
        this.type = t;
        this.model = new MotorModel(t);
        this.configuredMaxTps = t.maxTicksPerSecond();
        this.orientationSign = t.orientationSign();
        this.physicalSign = mountSign * orientationSign;
        if (inertia == 0) inertia = 0.15 * t.stallTorqueNm / t.freeSpeedRadPerSec();
        if (viscousFriction == 0) viscousFriction = 0.02 * t.stallTorqueNm / t.freeSpeedRadPerSec();
        if (coulombFriction == 0) coulombFriction = 0.03 * t.stallTorqueNm;
    }

    /** Software-configured motor type parameters (what the hub scales RUN_USING_ENCODER power with). */
    public void setConfiguredMaxTps(double maxTps, double fraction) {
        boolean changed = Math.abs(maxTps - configuredMaxTps) > 1e-9;
        configuredMaxTps = maxTps;
        achievableMaxTpsFraction = fraction;
        if (changed) resetDefaultVelocityPidf();
    }

    /** Default hub coefficients: F = 32767/maxVel, P = 0.1 F, I = 0.01 F (matches common SDK guidance). */
    public void resetDefaultVelocityPidf() {
        double f = 32767.0 / Math.max(1.0, configuredMaxTps);
        velocityPidf.set(0.1 * f, 0.01 * f, 0, f);
        velocityPidf.reset();
    }

    public MotorModel model() { return model; }

    // ---- reads (controller frame) ----
    public double angleControllerFrame() { return physicalSign * angle - encoderOffset; }
    public int encoderTicks() { return (int) Math.round(angleControllerFrame() / (2 * Math.PI) * type.ticksPerRev); }
    public double velocityTpsExact() { return physicalSign * omega / (2 * Math.PI) * type.ticksPerRev; }
    /** Velocity as the hub reports it (int16 counts per second, with the well known overflow). */
    public int velocityTps() {
        long v = Math.round(velocityTpsExact());
        if (emulateVelocityOverflow) return (short) v;
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, v));
    }
    public boolean isBusy() { return busy; }
    public double currentAmps() { return current; }
    public double appliedVolts() { return appliedVolts; }
    public double duty() { return lastDuty; }
    public boolean isOverCurrent() { return current > currentAlertAmps; }
    public double physicalAngle() { return angle; }
    public double physicalOmega() { return omega; }
    public void setPhysicalAngle(double a) { angle = a; }
    public void setPhysicalOmega(double w) { omega = w; }

    public void resetEncoder() { encoderOffset = physicalSign * angle; }

    /** Maximum ticks/s the hub will target for power = 1 in RUN_USING_ENCODER. */
    public double achievableMaxTps() { return configuredMaxTps * achievableMaxTpsFraction; }

    /** Runs the hub's control loop and returns the duty cycle (-1..1) to apply (controller frame). */
    public double controlStep(double dt) {
        if (!enabled) { busy = false; return 0; }
        double measured = velocityTpsExact();
        double duty;
        switch (mode) {
            case RUN_USING_ENCODER: {
                double target = useVelocityTarget ? targetVelocityTps : power * achievableMaxTps();
                duty = velocityPidf.update(target, measured, dt);
                busy = false;
                break;
            }
            case RUN_TO_POSITION: {
                double error = targetPosition - encoderTicks();
                busy = Math.abs(error) > targetTolerance;
                double maxTps = Math.abs(power) * achievableMaxTps();
                double vt = Math.max(-maxTps, Math.min(maxTps, positionP * error));
                duty = velocityPidf.update(vt, measured, dt);
                break;
            }
            case STOP_AND_RESET_ENCODER:
                duty = 0; busy = false; break;
            case RUN_WITHOUT_ENCODER:
            default:
                duty = power; busy = false; break;
        }
        lastDuty = duty;
        return duty;
    }

    /**
     * Computes the torque (N·m, physical frame) the motor exerts at its output
     * shaft this step, updating current draw. Must be called once per physics step.
     */
    public double torqueStep(double dt, double batteryVolts) {
        double duty = controlStep(dt);
        double omegaC = physicalSign * omega;
        double volts = duty * batteryVolts;
        double tauC;
        if (!enabled || (duty == 0 && zeroPowerBehavior == DcMotor.ZeroPowerBehavior.FLOAT)) {
            tauC = 0; current = 0; appliedVolts = 0;
        } else if (duty == 0) {
            tauC = model.brakeTorque(omegaC); current = 0; appliedVolts = 0;
        } else {
            tauC = model.torque(volts, omegaC);
            current = model.current(volts, omegaC);
            appliedVolts = volts;
        }
        return physicalSign * tauC;
    }

    /** For drivetrain wheels: the chassis tells the motor how fast its shaft is turning. */
    public void observeShaftSpeed(double omegaPhysical, double dt) {
        omega = omegaPhysical;
        angle += omega * dt;
    }

    /** For non-drive motors: integrates the motor and its rotational load. */
    public void stepLoad(double dt, double batteryVolts) {
        double tauMotor = torqueStep(dt, batteryVolts);
        double tauNet = tauMotor - viscousFriction * omega - gravityTorque;
        if (Math.abs(omega) < 1e-4 && Math.abs(tauNet) <= coulombFriction) {
            omega = 0;
            applyLimits();
            return;
        }
        double tauC = -Math.signum(omega != 0 ? omega : tauNet) * coulombFriction;
        double alpha = (tauNet + tauC) / Math.max(1e-9, inertia);
        double newOmega = omega + alpha * dt;
        if (omega != 0 && Math.signum(newOmega) != Math.signum(omega) && Math.abs(tauMotor) < coulombFriction) newOmega = 0;
        omega = newOmega;
        angle += omega * dt;
        applyLimits();
    }

    private void applyLimits() {
        if (angle < minAngle) { angle = minAngle; if (omega < 0) omega = 0; }
        if (angle > maxAngle) { angle = maxAngle; if (omega > 0) omega = 0; }
    }

    /** Called when an OpMode ends: hub puts motors in a safe state. */
    public void resetForOpMode() {
        power = 0; useVelocityTarget = false; targetPositionSet = false; enabled = true;
        mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER;
        zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE;
        velocityPidf.reset();
    }
}
