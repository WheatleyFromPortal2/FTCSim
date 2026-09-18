package ftcsim.hardware;

import com.qualcomm.hardware.rev.RevColorSensorV3;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.NormalizedRGBA;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

import java.util.Map;

/** REV Color Sensor V3: colour and proximity values are set from the UI. */
public class SimColorSensorV3 extends RevColorSensorV3 implements SimDevice {
    private final String name, hub;
    private volatile float r = 0.05f, g = 0.05f, b = 0.05f, a = 0.15f;
    private volatile double distanceMm = 80;
    private volatile float gain = 1.0f;
    private volatile boolean ledOn = true;

    public SimColorSensorV3(String name, String hub) {
        super(new VirtualI2cDevice(name), true);
        this.name = name; this.hub = hub;
    }

    @Override protected synchronized boolean internalInitialize(Parameters parameters) { return true; }
    @Override public synchronized boolean initialize() { return true; }
    @Override public NormalizedRGBA getNormalizedColors() {
        HardwareBus.i2c();
        NormalizedRGBA c = new NormalizedRGBA();
        c.red = clamp(r * gain); c.green = clamp(g * gain); c.blue = clamp(b * gain); c.alpha = clamp(a * gain);
        return c;
    }
    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
    @Override public int red() { HardwareBus.i2c(); synchronized (this) { return Math.round(clamp(r * gain) * 65535); } }
    @Override public int green() { HardwareBus.i2c(); synchronized (this) { return Math.round(clamp(g * gain) * 65535); } }
    @Override public int blue() { HardwareBus.i2c(); synchronized (this) { return Math.round(clamp(b * gain) * 65535); } }
    @Override public int alpha() { HardwareBus.i2c(); synchronized (this) { return Math.round(clamp(a * gain) * 65535); } }
    @Override public int argb() { return getNormalizedColors().toColor(); }
    @Override public void setGain(float newGain) { gain = newGain; }
    @Override public float getGain() { return gain; }
    @Override public void enableLed(boolean enable) { ledOn = enable; HardwareBus.i2c(); }
    @Override public boolean isLightOn() { return ledOn; }
    @Override public double getDistance(DistanceUnit unit) { HardwareBus.i2c(); return unit.fromMm(distanceMm); }
    @Override public double getLightDetected() { return clamp(a * gain); }
    @Override public double getRawLightDetected() { return clamp(a * gain) * 65535; }
    @Override public double getRawLightDetectedMax() { return 65535; }
    @Override public int rawOptical() { return (int) Math.max(0, 2047 - distanceMm * 10); }
    @Override public String status() { return "FTCSim"; }
    @Override public synchronized I2cAddr getI2cAddress() { return I2cAddr.create7bit(0x52); }
    @Override public synchronized void setI2cAddress(I2cAddr newAddress) {}
    @Override public String getDeviceName() { return "REV Color Sensor V3"; }
    @Override public Manufacturer getManufacturer() { return Manufacturer.Broadcom; }
    @Override public String getConnectionInfo() { return hub + "; I2C"; }
    @Override public void resetDeviceConfigurationForOpMode() {}
    @Override public void close() {}
    @Override public String simName() { return name; }
    @Override public String simType() { return "RevColorSensorV3"; }
    @Override public String simHub() { return hub; }
    @Override public int simPort() { return 0; }
    @Override public void fillView(Map<String, Object> v) {
        v.put("red", r); v.put("green", g); v.put("blue", b); v.put("alpha", a); v.put("distanceMm", distanceMm); v.put("gain", gain); v.put("led", ledOn); v.put("input", true);
    }
    @Override public boolean applyInput(String key, Object value) {
        switch (key) {
            case "red": r = (float) SimDevice.asDouble(value, r); return true;
            case "green": g = (float) SimDevice.asDouble(value, g); return true;
            case "blue": b = (float) SimDevice.asDouble(value, b); return true;
            case "alpha": a = (float) SimDevice.asDouble(value, a); return true;
            case "distanceMm": distanceMm = SimDevice.asDouble(value, distanceMm); return true;
            default: return false;
        }
    }
}
