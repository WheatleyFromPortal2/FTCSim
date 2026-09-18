package ftcsim.hardware;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.AnalogInputController;
import com.qualcomm.robotcore.util.SerialNumber;

import java.util.function.DoubleSupplier;

/** The four analog input ports of a simulated REV hub (0..3.3 V). */
public class SimAnalogController implements AnalogInputController, LynxModule.BulkProvider {
    public static final int PORTS = 4;
    public static final double MAX_VOLTAGE = 3.3;
    private final LynxModule hub;
    private final DoubleSupplier[] sources = new DoubleSupplier[PORTS];
    private final SerialNumber serial = SerialNumber.createFake();

    public SimAnalogController(LynxModule hub) { this.hub = hub; hub.addBulkProvider(this); }
    public int freePort() { for (int i = 0; i < PORTS; i++) if (sources[i] == null) return i; return -1; }
    public void attach(int port, DoubleSupplier source) { sources[port] = source; }
    public boolean isFree(int port) { return sources[port] == null; }
    public LynxModule hub() { return hub; }

    private double live(int port) {
        DoubleSupplier s = sources[port];
        double v = s == null ? 0 : s.getAsDouble();
        return Math.max(0, Math.min(MAX_VOLTAGE, v));
    }
    @Override public void fillBulkData(LynxModule.BulkData data) { for (int i = 0; i < PORTS; i++) data.analogVoltage[i] = live(i); }
    @Override public double getAnalogInputVoltage(int port) { return hub.bulkRead("analog/" + port, () -> live(port), d -> d.getAnalogInputVoltage(port)); }
    @Override public double getMaxAnalogInputVoltage() { return MAX_VOLTAGE; }
    @Override public SerialNumber getSerialNumber() { return serial; }
    @Override public Manufacturer getManufacturer() { return Manufacturer.Lynx; }
    @Override public String getDeviceName() { return "FTCSim Analog Input Controller"; }
    @Override public String getConnectionInfo() { return hub.getName(); }
    @Override public int getVersion() { return 1; }
    @Override public void resetDeviceConfigurationForOpMode() {}
    @Override public void close() {}
}
