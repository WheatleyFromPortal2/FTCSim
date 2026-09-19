package ftcsim.hardware;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.DigitalChannelController;
import com.qualcomm.robotcore.util.SerialNumber;

import java.util.function.BooleanSupplier;

/** The eight digital I/O ports of a simulated REV hub. */
public class SimDigitalController implements DigitalChannelController, LynxModule.BulkProvider {
    public static final int PORTS = 8;
    private final LynxModule hub;
    private final BooleanSupplier[] inputs = new BooleanSupplier[PORTS];
    private final boolean[] attached = new boolean[PORTS];
    private final boolean[] outputState = new boolean[PORTS];
    private final DigitalChannel.Mode[] modes = new DigitalChannel.Mode[PORTS];
    private final SerialNumber serial = SerialNumber.createFake();

    public SimDigitalController(LynxModule hub) {
        this.hub = hub; hub.addBulkProvider(this);
        for (int i = 0; i < PORTS; i++) modes[i] = DigitalChannel.Mode.INPUT;
    }
    public int freePort() { for (int i = 0; i < PORTS; i++) if (!attached[i]) return i; return -1; }
    public boolean isFree(int port) { return !attached[port]; }
    public void attach(int port, BooleanSupplier input) { attached[port] = true; inputs[port] = input; }
    public LynxModule hub() { return hub; }
    public boolean outputState(int port) { return outputState[port]; }

    private boolean live(int port) {
        if (modes[port] == DigitalChannel.Mode.OUTPUT) return outputState[port];
        BooleanSupplier s = inputs[port];
        return s == null || s.getAsBoolean(); // REV digital inputs are pulled up: true when nothing pulls low
    }
    @Override public void fillBulkData(LynxModule.BulkData data) { for (int i = 0; i < PORTS; i++) data.digitalState[i] = live(i); }
    @Override public SerialNumber getSerialNumber() { return serial; }
    @Override public DigitalChannel.Mode getDigitalChannelMode(int port) { return modes[port]; }
    @Override public void setDigitalChannelMode(int port, DigitalChannel.Mode mode) { modes[port] = mode; HardwareBus.write(); }
    @Override public void setDigitalChannelMode(int port, DigitalChannelController.Mode mode) { modes[port] = mode.migrate(); }
    @Override public boolean getDigitalChannelState(int port) { return hub.bulkRead("digital/" + port, () -> live(port), d -> d.getDigitalChannelState(port)); }
    @Override public void setDigitalChannelState(int port, boolean state) { outputState[port] = state; HardwareBus.write(); }
    @Override public Manufacturer getManufacturer() { return Manufacturer.Lynx; }
    @Override public String getDeviceName() { return "FTCSim Digital Controller"; }
    @Override public String getConnectionInfo() { return hub.getName(); }
    @Override public int getVersion() { return 1; }
    @Override public void resetDeviceConfigurationForOpMode() { for (int i = 0; i < PORTS; i++) { modes[i] = DigitalChannel.Mode.INPUT; outputState[i] = false; } }
    @Override public void close() {}
}
