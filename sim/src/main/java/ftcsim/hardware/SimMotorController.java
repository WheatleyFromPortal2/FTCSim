package ftcsim.hardware;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.*;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType;
import com.qualcomm.robotcore.util.SerialNumber;
import ftcsim.physics.MotorState;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;

/** The four motor ports of a simulated REV hub. */
public class SimMotorController implements DcMotorControllerEx, LynxModule.BulkProvider {
    public static final int PORTS = 4;
    private final LynxModule hub;
    private final MotorState[] ports = new MotorState[PORTS];
    private final MotorConfigurationType[] types = new MotorConfigurationType[PORTS];
    private final SerialNumber serial = SerialNumber.createFake();

    public SimMotorController(LynxModule hub) { this.hub = hub; hub.addBulkProvider(this); }

    public LynxModule hub() { return hub; }
    public MotorState state(int port) { return ports[port]; }
    public boolean isFree(int port) { return ports[port] == null; }
    public int freePort() { for (int i = 0; i < PORTS; i++) if (ports[i] == null) return i; return -1; }

    public void attach(int port, MotorState state, MotorConfigurationType type) {
        ports[port] = state;
        types[port] = type;
        applyType(port, type);
    }

    private void applyType(int port, MotorConfigurationType type) {
        MotorState s = ports[port];
        if (s == null || type == null) return;
        double tpr = type.getTicksPerRev() > 0 ? type.getTicksPerRev() : s.type.ticksPerRev;
        double rpm = type.getMaxRPM() > 0 ? type.getMaxRPM() : s.type.maxRpm;
        double frac = type.getAchieveableMaxRPMFraction() > 0 ? type.getAchieveableMaxRPMFraction() : 0.85;
        s.setConfiguredMaxTps(tpr * rpm / 60.0, frac);
    }

    private MotorState s(int port) {
        MotorState st = ports[port];
        if (st == null) throw new IllegalArgumentException("motor port " + port + " has no motor attached");
        return st;
    }

    @Override public void fillBulkData(LynxModule.BulkData data) {
        for (int i = 0; i < PORTS; i++) {
            MotorState st = ports[i];
            if (st == null) continue;
            data.motorPosition[i] = st.encoderTicks();
            data.motorVelocity[i] = st.velocityTps();
            data.motorBusy[i] = st.isBusy();
            data.motorOverCurrent[i] = st.isOverCurrent();
        }
    }

    // ---- DcMotorController ----
    @Override public void setMotorType(int port, MotorConfigurationType motorType) { types[port] = motorType; applyType(port, motorType); }
    @Override public MotorConfigurationType getMotorType(int port) { return types[port]; }
    @Override public void setMotorMode(int port, DcMotor.RunMode mode) {
        MotorState st = s(port);
        HardwareBus.write();
        if (mode == DcMotor.RunMode.STOP_AND_RESET_ENCODER) { st.resetEncoder(); st.power = 0; }
        if (mode != st.mode) { st.velocityPidf.reset(); st.useVelocityTarget = false; }
        st.mode = mode;
    }
    @Override public DcMotor.RunMode getMotorMode(int port) { return s(port).mode; }
    @Override public void setMotorPower(int port, double power) {
        MotorState st = s(port);
        HardwareBus.write();
        st.power = Math.max(-1, Math.min(1, power));
        st.useVelocityTarget = false;
    }
    @Override public double getMotorPower(int port) { return s(port).power; }
    @Override public boolean isBusy(int port) { MotorState st = s(port); return hub.bulkRead("busy/" + port, st::isBusy, d -> d.isMotorBusy(port)); }
    @Override public void setMotorZeroPowerBehavior(int port, DcMotor.ZeroPowerBehavior zeroPowerBehavior) { s(port).zeroPowerBehavior = zeroPowerBehavior; HardwareBus.write(); }
    @Override public DcMotor.ZeroPowerBehavior getMotorZeroPowerBehavior(int port) { return s(port).zeroPowerBehavior; }
    @Override public boolean getMotorPowerFloat(int port) { MotorState st = s(port); return st.zeroPowerBehavior == DcMotor.ZeroPowerBehavior.FLOAT && st.power == 0; }
    @Override public void setMotorTargetPosition(int port, int position) { setMotorTargetPosition(port, position, s(port).targetTolerance); }
    @Override public int getMotorTargetPosition(int port) { return s(port).targetPosition; }
    @Override public int getMotorCurrentPosition(int port) { MotorState st = s(port); return hub.bulkRead("pos/" + port, st::encoderTicks, d -> d.getMotorCurrentPosition(port)); }
    @Override public void resetDeviceConfigurationForOpMode(int port) { MotorState st = ports[port]; if (st != null) { st.resetForOpMode(); applyType(port, types[port]); } }

    // ---- DcMotorControllerEx ----
    @Override public void setMotorEnable(int port) { s(port).enabled = true; HardwareBus.write(); }
    @Override public void setMotorDisable(int port) { s(port).enabled = false; HardwareBus.write(); }
    @Override public boolean isMotorEnabled(int port) { return s(port).enabled; }
    @Override public void setMotorVelocity(int port, double ticksPerSecond) {
        MotorState st = s(port);
        HardwareBus.write();
        st.targetVelocityTps = ticksPerSecond;
        st.useVelocityTarget = true;
        st.mode = DcMotor.RunMode.RUN_USING_ENCODER;
    }
    @Override public void setMotorVelocity(int port, double angularRate, AngleUnit unit) {
        MotorState st = s(port);
        double revPerSec = unit.toDegrees(angularRate) / 360.0;
        setMotorVelocity(port, revPerSec * st.type.ticksPerRev);
    }
    @Override public double getMotorVelocity(int port) { MotorState st = s(port); return hub.bulkRead("vel/" + port, () -> (double) st.velocityTps(), d -> (double) d.getMotorVelocity(port)); }
    @Override public double getMotorVelocity(int port, AngleUnit unit) {
        MotorState st = s(port);
        double revPerSec = getMotorVelocity(port) / st.type.ticksPerRev;
        return unit.fromDegrees(revPerSec * 360.0);
    }
    @Override public void setPIDCoefficients(int port, DcMotor.RunMode mode, PIDCoefficients pid) { setPIDFCoefficients(port, mode, new PIDFCoefficients(pid)); }
    @Override public void setPIDFCoefficients(int port, DcMotor.RunMode mode, PIDFCoefficients pidf) {
        MotorState st = s(port);
        HardwareBus.write();
        if (mode == DcMotor.RunMode.RUN_TO_POSITION) st.positionP = pidf.p;
        else st.velocityPidf.set(pidf.p, pidf.i, pidf.d, pidf.f);
    }
    @Override public PIDCoefficients getPIDCoefficients(int port, DcMotor.RunMode mode) { PIDFCoefficients c = getPIDFCoefficients(port, mode); return new PIDCoefficients(c.p, c.i, c.d); }
    @Override public PIDFCoefficients getPIDFCoefficients(int port, DcMotor.RunMode mode) {
        MotorState st = s(port);
        HardwareBus.read();
        if (mode == DcMotor.RunMode.RUN_TO_POSITION) return new PIDFCoefficients(st.positionP, 0, 0, 0);
        return new PIDFCoefficients(st.velocityPidf.p, st.velocityPidf.i, st.velocityPidf.d, st.velocityPidf.f);
    }
    @Override public void setMotorTargetPosition(int port, int position, int tolerance) {
        MotorState st = s(port);
        HardwareBus.write();
        st.targetPosition = position; st.targetTolerance = tolerance; st.targetPositionSet = true;
    }
    @Override public double getMotorCurrent(int port, CurrentUnit unit) { double a = s(port).currentAmps(); HardwareBus.read(); return unit.convert(a, CurrentUnit.AMPS); }
    @Override public double getMotorCurrentAlert(int port, CurrentUnit unit) { return unit.convert(s(port).currentAlertAmps, CurrentUnit.AMPS); }
    @Override public void setMotorCurrentAlert(int port, double current, CurrentUnit unit) { s(port).currentAlertAmps = unit.toAmps(current); HardwareBus.write(); }
    @Override public boolean isMotorOverCurrent(int port) { MotorState st = s(port); return hub.bulkRead("overcurrent/" + port, st::isOverCurrent, d -> d.isMotorOverCurrent(port)); }

    // ---- HardwareDevice ----
    @Override public Manufacturer getManufacturer() { return Manufacturer.Lynx; }
    @Override public String getDeviceName() { return "FTCSim Motor Controller"; }
    @Override public String getConnectionInfo() { return hub.getName(); }
    @Override public int getVersion() { return 1; }
    @Override public void resetDeviceConfigurationForOpMode() { for (int i = 0; i < PORTS; i++) resetDeviceConfigurationForOpMode(i); }
    @Override public void close() {}
    public SerialNumber getSerialNumber() { return serial; }
}
