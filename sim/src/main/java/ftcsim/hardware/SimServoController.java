package ftcsim.hardware;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.PwmControl;
import com.qualcomm.robotcore.hardware.ServoControllerEx;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType;
import ftcsim.physics.ServoState;

/** The six servo ports of a simulated REV hub. */
public class SimServoController implements ServoControllerEx {
    public static final int PORTS = 6;
    private final LynxModule hub;
    private final ServoState[] ports = new ServoState[PORTS];

    public SimServoController(LynxModule hub) { this.hub = hub; }
    public LynxModule hub() { return hub; }
    public ServoState state(int port) { return ports[port]; }
    public int freePort() { for (int i = 0; i < PORTS; i++) if (ports[i] == null) return i; return -1; }
    public void attach(int port, ServoState st) { ports[port] = st; }

    private ServoState s(int port) {
        ServoState st = ports[port];
        if (st == null) throw new IllegalArgumentException("servo port " + port + " has no servo attached");
        return st;
    }

    @Override public void pwmEnable() { for (ServoState st : ports) if (st != null) st.pwmEnabled = true; }
    @Override public void pwmDisable() { for (ServoState st : ports) if (st != null) st.pwmEnabled = false; }
    @Override public PwmStatus getPwmStatus() {
        boolean any = false, all = true, present = false;
        for (ServoState st : ports) if (st != null) { present = true; any |= st.pwmEnabled; all &= st.pwmEnabled; }
        if (!present) return PwmStatus.DISABLED;
        return all ? PwmStatus.ENABLED : any ? PwmStatus.MIXED : PwmStatus.DISABLED;
    }
    @Override public void setServoPosition(int port, double position) {
        ServoState st = s(port);
        st.commanded = Math.max(0, Math.min(1, position));
        st.everCommanded = true;
        st.pwmEnabled = true;
    }
    @Override public double getServoPosition(int port) { return s(port).commanded; }
    @Override public void setServoPwmRange(int port, PwmControl.PwmRange range) { s(port).pwmRange = range; }
    @Override public PwmControl.PwmRange getServoPwmRange(int port) { return s(port).pwmRange; }
    @Override public void setServoPwmEnable(int port) { s(port).pwmEnabled = true; }
    @Override public void setServoPwmDisable(int port) { s(port).pwmEnabled = false; }
    @Override public boolean isServoPwmEnabled(int port) { return s(port).pwmEnabled; }
    @Override public void setServoType(int port, ServoConfigurationType type) {
        if (type == null) return;
        try {
            double lo = type.getUsPulseLower(), hi = type.getUsPulseUpper(), frame = type.getUsFrame();
            if (lo > 0 && hi > lo) s(port).pwmRange = new PwmControl.PwmRange(lo, hi, frame > 0 ? frame : PwmControl.PwmRange.usFrameDefault);
        } catch (RuntimeException ignored) {}
    }
    // SDK 12 additions (plain methods so the class also compiles against SDK 11): direct pulse-width control.
    public void setPulseWidth(int port, double pulseWidthUs) {
        ServoState st = s(port);
        PwmControl.PwmRange r = st.pwmRange;
        double span = r.usPulseUpper - r.usPulseLower;
        st.commanded = span > 0 ? Math.max(0, Math.min(1, (pulseWidthUs - r.usPulseLower) / span)) : 0.5;
        st.everCommanded = true;
        st.pwmEnabled = true;
    }
    public double getPulseWidth(int port) { return s(port).pulseWidthUs(); }
    /** SDK 12: the hub forgets the last commanded position (the next read returns 0 until a new command). */
    public void forgetLastKnownPosition(int port) { s(port).everCommanded = false; }
    @Override public Manufacturer getManufacturer() { return Manufacturer.Lynx; }
    @Override public String getDeviceName() { return "FTCSim Servo Controller"; }
    @Override public String getConnectionInfo() { return hub.getName(); }
    @Override public int getVersion() { return 1; }
    @Override public void resetDeviceConfigurationForOpMode() { for (ServoState st : ports) if (st != null) st.resetForOpMode(); }
    @Override public void close() {}
}
