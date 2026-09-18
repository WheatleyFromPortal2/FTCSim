package ftcsim.hardware;

import com.qualcomm.hardware.rev.RevTouchSensor;

import java.util.Map;

/** A REV touch sensor: pressed state toggled from the UI. */
public class SimTouchSensor extends RevTouchSensor implements SimDevice {
    private final SimDigitalController simController;
    private final int port;
    private final String name;
    private volatile boolean pressed;

    public SimTouchSensor(SimDigitalController controller, int port, String name) {
        super(controller, port);
        this.simController = controller; this.port = port; this.name = name;
        controller.attach(port, () -> !pressed); // active low
    }
    @Override public String getConnectionInfo() { return simController.hub().getName() + "; digital port " + port; }
    @Override public String simName() { return name; }
    @Override public String simType() { return "TouchSensor"; }
    @Override public String simHub() { return simController.hub().getName(); }
    @Override public int simPort() { return port; }
    @Override public void fillView(Map<String, Object> v) { v.put("pressed", pressed); v.put("input", true); }
    @Override public boolean applyInput(String key, Object value) {
        if ("pressed".equals(key)) { pressed = SimDevice.asBool(value, pressed); return true; }
        return false;
    }
}
