package ftcsim.hardware;

import com.qualcomm.robotcore.hardware.CRServoImplEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import ftcsim.physics.ServoState;

import java.util.Map;

/** A continuous rotation servo backed by a simulated servo port. */
public class SimCRServo extends CRServoImplEx implements SimDevice {
    private final SimServoController simController;
    private final ServoState state;
    private final String name;

    public SimCRServo(SimServoController controller, int port, ServoState state, String name) {
        super(controller, port, DcMotorSimple.Direction.FORWARD, SimServo.servoType());
        this.simController = controller;
        this.state = state;
        this.name = name;
        state.continuous = true;
    }

    public ServoState state() { return state; }
    @Override public String getDeviceName() { return "Continuous Rotation Servo"; }
    @Override public String getConnectionInfo() { return simController.hub().getName() + "; port " + getPortNumber(); }
    @Override public String simName() { return name; }
    @Override public String simType() { return "CRServo"; }
    @Override public String simHub() { return simController.hub().getName(); }
    @Override public int simPort() { return getPortNumber(); }
    @Override public void fillView(Map<String, Object> v) {
        v.put("power", getPower());
        v.put("commanded", state.commanded);
        v.put("rpm", state.omega() * 60 / (2 * Math.PI));
        v.put("revolutions", state.angle() / (2 * Math.PI));
        v.put("pwmEnabled", state.pwmEnabled);
        v.put("direction", getDirection().name());
        v.put("current", state.current());
    }
}
