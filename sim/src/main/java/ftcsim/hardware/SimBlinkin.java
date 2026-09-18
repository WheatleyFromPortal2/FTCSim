package ftcsim.hardware;

import com.qualcomm.hardware.rev.RevBlinkinLedDriver;
import ftcsim.physics.ServoState;

import java.util.Map;

/** REV Blinkin LED driver (a servo-signal driven LED controller). */
public class SimBlinkin extends RevBlinkinLedDriver implements SimDevice {
    private final SimServoController simController;
    private final int port;
    private final String name;
    private final ServoState state;
    private volatile BlinkinPattern pattern = BlinkinPattern.BLACK;

    public SimBlinkin(SimServoController controller, int port, ServoState state, String name) {
        super(controller, port);
        this.simController = controller; this.port = port; this.state = state; this.name = name;
    }
    @Override public void setPattern(BlinkinPattern p) { pattern = p; super.setPattern(p); }
    @Override public String getDeviceName() { return "REV Blinkin LED Driver"; }
    @Override public String getConnectionInfo() { return simController.hub().getName() + "; servo port " + port; }
    @Override public String simName() { return name; }
    @Override public String simType() { return "RevBlinkinLedDriver"; }
    @Override public String simHub() { return simController.hub().getName(); }
    @Override public int simPort() { return port; }
    @Override public void fillView(Map<String, Object> v) { v.put("pattern", pattern.name()); v.put("pulseUs", state.pulseWidthUs()); }
}
