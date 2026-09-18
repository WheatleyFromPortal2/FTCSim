package ftcsim.hardware;

import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.ServoImplEx;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType;
import ftcsim.physics.ServoState;

import java.util.Map;

/** A positional servo backed by a simulated servo port (uses the SDK's ServoImplEx logic). */
public class SimServo extends ServoImplEx implements SimDevice {
    private final SimServoController simController;
    private final ServoState state;
    private final String name;

    public SimServo(SimServoController controller, int port, ServoState state, String name) {
        super(controller, port, Servo.Direction.FORWARD, servoType());
        this.simController = controller;
        this.state = state;
        this.name = name;
    }

    static ServoConfigurationType servoType() {
        try { return new ServoConfigurationType(); } catch (RuntimeException e) { return null; }
    }

    public ServoState state() { return state; }
    @Override public String getDeviceName() { return "Servo"; }
    @Override public String getConnectionInfo() { return simController.hub().getName() + "; port " + getPortNumber(); }
    @Override public String simName() { return name; }
    @Override public String simType() { return "Servo"; }
    @Override public String simHub() { return simController.hub().getName(); }
    @Override public int simPort() { return getPortNumber(); }
    @Override public void fillView(Map<String, Object> v) {
        v.put("commanded", state.commanded);
        v.put("position", state.position());
        v.put("pwmEnabled", state.pwmEnabled);
        v.put("direction", getDirection().name());
        v.put("pulseUs", state.pulseWidthUs());
        v.put("current", state.current());
    }
    @Override public boolean applyInput(String key, Object value) {
        if ("position".equals(key)) { state.setPosition(SimDevice.asDouble(value, state.position())); return true; }
        return false;
    }
}
