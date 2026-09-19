package ftcsim.hardware;

import com.qualcomm.hardware.andymark.AndyMarkTOF;
import ftcsim.physics.Chassis;
import ftcsim.physics.Field;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

import java.util.Map;
import java.util.function.Supplier;

/** AndyMark time-of-flight sensor (VL53L0X), same model as the REV 2m sensor. */
public class SimAndyMarkTOF extends AndyMarkTOF implements SimDevice {
    private final String name, hub;
    private final DistanceModel model;

    public SimAndyMarkTOF(String name, String hub, Chassis chassis, Supplier<Field> field) {
        super(new VirtualI2cDevice(name), true);
        this.name = name; this.hub = hub; this.model = new DistanceModel(chassis, field);
    }

    public void setMount(double xIn, double yIn, double yawDeg) { model.setMount(xIn, yIn, yawDeg); }

    @Override protected synchronized boolean doInitialize() { return true; }
    @Override public double getDistance(DistanceUnit unit) { double mm = model.measureMm(); HardwareBus.i2c(); return unit.fromMm(mm); }
    @Override public boolean didTimeoutOccur() { return false; }
    @Override public byte getModelID() { return (byte) 0xEE; }
    @Override public String getDeviceName() { return "AndyMark TOF Distance Sensor"; }
    @Override public Manufacturer getManufacturer() { return Manufacturer.Other; }
    @Override public String getConnectionInfo() { return hub + "; I2C"; }
    @Override public void resetDeviceConfigurationForOpMode() {}
    @Override public void close() {}
    @Override public String simName() { return name; }
    @Override public String simType() { return "AndyMarkTOF"; }
    @Override public String simHub() { return hub; }
    @Override public int simPort() { return 0; }
    @Override public void fillView(Map<String, Object> v) { model.fillView(v); }
    @Override public boolean applyInput(String key, Object value) { return model.applyInput(key, value); }
}
