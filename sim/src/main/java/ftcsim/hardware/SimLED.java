package ftcsim.hardware;

import com.qualcomm.robotcore.hardware.LED;

import java.util.Map;

public class SimLED extends LED implements SimDevice {
    private final SimDigitalController simController;
    private final int port;
    private final String name;
    public SimLED(SimDigitalController controller, int port, String name) {
        super(controller, port);
        this.simController = controller; this.port = port; this.name = name;
        controller.attach(port, () -> true);
    }
    @Override public String getConnectionInfo() { return simController.hub().getName() + "; digital port " + port; }
    @Override public String simName() { return name; }
    @Override public String simType() { return "LED"; }
    @Override public String simHub() { return simController.hub().getName(); }
    @Override public int simPort() { return port; }
    @Override public void fillView(Map<String, Object> v) { v.put("on", simController.outputState(port)); }
}
