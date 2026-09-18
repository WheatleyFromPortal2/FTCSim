package ftcsim.hardware;

import com.qualcomm.robotcore.hardware.AnalogInput;

import java.util.Map;
import java.util.function.DoubleSupplier;

/** An analog input whose voltage comes from the UI or from a linked servo (absolute encoder). */
public class SimAnalogInput extends AnalogInput implements SimDevice {
    private final SimAnalogController simController;
    private final int port;
    private final String name;
    private volatile double manualVoltage;
    private volatile DoubleSupplier derived; // e.g. servo feedback
    private volatile String sourceDescription = "manual";

    public SimAnalogInput(SimAnalogController controller, int port, String name, double initialVoltage) {
        super(controller, port);
        this.simController = controller; this.port = port; this.name = name; this.manualVoltage = initialVoltage;
        controller.attach(port, this::voltageSource);
    }

    private double voltageSource() { DoubleSupplier d = derived; return d != null ? d.getAsDouble() : manualVoltage; }
    public void setDerivedSource(DoubleSupplier s, String description) { derived = s; sourceDescription = description; }

    @Override public String getDeviceName() { return "Analog Input"; }
    @Override public String getConnectionInfo() { return simController.hub().getName() + "; analog port " + port; }
    @Override public String simName() { return name; }
    @Override public String simType() { return "AnalogInput"; }
    @Override public String simHub() { return simController.hub().getName(); }
    @Override public int simPort() { return port; }
    @Override public void fillView(Map<String, Object> v) { v.put("voltage", voltageSource()); v.put("source", sourceDescription); v.put("input", derived == null); }
    @Override public boolean applyInput(String key, Object value) {
        if ("voltage".equals(key)) { manualVoltage = SimDevice.asDouble(value, manualVoltage); return true; }
        return false;
    }
}
