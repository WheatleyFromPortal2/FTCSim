package ftcsim.hardware;

import com.qualcomm.robotcore.hardware.VoltageSensor;

import java.util.Map;
import java.util.function.DoubleSupplier;

public class SimVoltageSensor implements VoltageSensor, SimDevice {
    private final String name;
    private final String hub;
    private final DoubleSupplier volts;
    public SimVoltageSensor(String name, String hub, DoubleSupplier volts) { this.name = name; this.hub = hub; this.volts = volts; }
    @Override public double getVoltage() { return volts.getAsDouble(); }
    @Override public Manufacturer getManufacturer() { return Manufacturer.Lynx; }
    @Override public String getDeviceName() { return "Voltage Sensor"; }
    @Override public String getConnectionInfo() { return hub; }
    @Override public int getVersion() { return 1; }
    @Override public void resetDeviceConfigurationForOpMode() {}
    @Override public void close() {}
    @Override public String simName() { return name; }
    @Override public String simType() { return "VoltageSensor"; }
    @Override public String simHub() { return hub; }
    @Override public int simPort() { return 0; }
    @Override public void fillView(Map<String, Object> v) { v.put("voltage", getVoltage()); }
}
