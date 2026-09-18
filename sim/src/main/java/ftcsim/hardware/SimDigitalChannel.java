package ftcsim.hardware;

import com.qualcomm.robotcore.hardware.DigitalChannelImpl;

import java.util.Map;
import java.util.function.BooleanSupplier;

/** A digital channel whose input level is set from the UI (or a linked simulation source). */
public class SimDigitalChannel extends DigitalChannelImpl implements SimDevice {
    private final SimDigitalController simController;
    private final int port;
    private final String name;
    private volatile boolean manualState;
    private volatile BooleanSupplier derived;
    private volatile String sourceDescription = "manual";

    public SimDigitalChannel(SimDigitalController controller, int port, String name, boolean initialState) {
        super(controller, port);
        this.simController = controller; this.port = port; this.name = name; this.manualState = initialState;
        controller.attach(port, this::inputSource);
    }

    private boolean inputSource() { BooleanSupplier d = derived; return d != null ? d.getAsBoolean() : manualState; }
    public void setDerivedSource(BooleanSupplier s, String description) { derived = s; sourceDescription = description; }
    public boolean manualState() { return manualState; }

    @Override public String getDeviceName() { return "Digital Device"; }
    @Override public String getConnectionInfo() { return simController.hub().getName() + "; digital port " + port; }
    @Override public String simName() { return name; }
    @Override public String simType() { return "DigitalChannel"; }
    @Override public String simHub() { return simController.hub().getName(); }
    @Override public int simPort() { return port; }
    @Override public void fillView(Map<String, Object> v) {
        v.put("state", getMode() == Mode.OUTPUT ? simController.outputState(port) : inputSource());
        v.put("mode", getMode().name());
        v.put("source", sourceDescription);
        v.put("input", derived == null && getMode() == Mode.INPUT);
    }
    @Override public boolean applyInput(String key, Object value) {
        if ("state".equals(key)) { manualState = SimDevice.asBool(value, manualState); return true; }
        return false;
    }
}
