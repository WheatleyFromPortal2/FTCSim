package ftcsim.hardware;

import android.content.Context;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerNotifier;
import com.qualcomm.robotcore.hardware.*;
import com.qualcomm.robotcore.util.RobotLog;

import java.util.ArrayList;
import java.util.List;

/**
 * The SDK's HardwareMap extended with on-demand device creation: when an
 * OpMode asks for a device that the robot configuration does not list, the
 * simulator creates one of the requested type (if enabled) instead of failing.
 */
public class SimHardwareMap extends HardwareMap {
    /** Creates a device for a class requested by team code, or returns null if it cannot. */
    public interface AutoCreator { HardwareDevice create(Class<?> requestedType, String name); }

    private AutoCreator autoCreator;
    private final List<String> autoCreatedNames = new ArrayList<>();

    public SimHardwareMap(Context appContext) {
        super(appContext, new OpModeManagerNotifier() {
            @Override public OpMode registerListener(Notifications listener) { return null; }
            @Override public void unregisterListener(Notifications listener) {}
        });
        // Replace the typed mappings with auto-creating ones.
        dcMotorController = new AutoMapping<>(DcMotorController.class);
        dcMotor = new AutoMapping<>(DcMotor.class);
        servoController = new AutoMapping<>(ServoController.class);
        servo = new AutoMapping<>(Servo.class);
        crservo = new AutoMapping<>(CRServo.class);
        touchSensorMultiplexer = new AutoMapping<>(TouchSensorMultiplexer.class);
        analogInput = new AutoMapping<>(AnalogInput.class);
        digitalChannel = new AutoMapping<>(DigitalChannel.class);
        opticalDistanceSensor = new AutoMapping<>(OpticalDistanceSensor.class);
        touchSensor = new AutoMapping<>(TouchSensor.class);
        pwmOutput = new AutoMapping<>(PWMOutput.class);
        i2cDevice = new AutoMapping<>(I2cDevice.class);
        i2cDeviceSynch = new AutoMapping<>(I2cDeviceSynch.class);
        colorSensor = new AutoMapping<>(ColorSensor.class);
        led = new AutoMapping<>(LED.class);
        accelerationSensor = new AutoMapping<>(AccelerationSensor.class);
        compassSensor = new AutoMapping<>(CompassSensor.class);
        gyroSensor = new AutoMapping<>(GyroSensor.class);
        irSeekerSensor = new AutoMapping<>(IrSeekerSensor.class);
        lightSensor = new AutoMapping<>(LightSensor.class);
        ultrasonicSensor = new AutoMapping<>(UltrasonicSensor.class);
        voltageSensor = new AutoMapping<>(VoltageSensor.class);
        allDeviceMappings.clear();
        allDeviceMappings.add(dcMotorController); allDeviceMappings.add(dcMotor); allDeviceMappings.add(servoController);
        allDeviceMappings.add(servo); allDeviceMappings.add(crservo); allDeviceMappings.add(touchSensorMultiplexer);
        allDeviceMappings.add(analogInput); allDeviceMappings.add(digitalChannel); allDeviceMappings.add(opticalDistanceSensor);
        allDeviceMappings.add(touchSensor); allDeviceMappings.add(pwmOutput); allDeviceMappings.add(i2cDevice);
        allDeviceMappings.add(i2cDeviceSynch); allDeviceMappings.add(colorSensor); allDeviceMappings.add(led);
        allDeviceMappings.add(accelerationSensor); allDeviceMappings.add(compassSensor); allDeviceMappings.add(gyroSensor);
        allDeviceMappings.add(irSeekerSensor); allDeviceMappings.add(lightSensor); allDeviceMappings.add(ultrasonicSensor);
        allDeviceMappings.add(voltageSensor);
    }

    public void setAutoCreator(AutoCreator c) { autoCreator = c; }

    /** Registers a device under a name in the global map and in every typed mapping it belongs to (like the RC's configuration loader). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public synchronized void registerDevice(String name, HardwareDevice device) {
        put(name, device);
        for (DeviceMapping<? extends HardwareDevice> mapping : allDeviceMappings) {
            if (mapping.getDeviceTypeClass().isInstance(device) && !mapping.contains(name)) {
                ((DeviceMapping) mapping).putLocal(name, device);
            }
        }
    }
    public List<String> autoCreatedNames() { return autoCreatedNames; }

    private synchronized HardwareDevice autoCreate(Class<?> type, String name) {
        AutoCreator c = autoCreator;
        if (c == null) return null;
        HardwareDevice d = c.create(type, name);
        if (d != null) {
            registerDevice(name, d);
            autoCreatedNames.add(name);
            RobotLog.ww("FTCSim", "Device \"%s\" (%s) is not in the robot configuration; created a simulated %s", name, type.getSimpleName(), d.getClass().getSimpleName());
        }
        return d;
    }

    @Override public <T> T get(Class<? extends T> classOrInterface, String deviceName) {
        deviceName = deviceName.trim();
        T result = tryGet(classOrInterface, deviceName);
        if (result != null) return result;
        HardwareDevice created = autoCreate(classOrInterface, deviceName);
        if (created != null && classOrInterface.isInstance(created)) return classOrInterface.cast(created);
        throw new IllegalArgumentException(String.format("Unable to find a hardware device with name \"%s\" and type %s", deviceName, classOrInterface.getSimpleName()));
    }

    @Override public HardwareDevice get(String deviceName) {
        HardwareDevice d = super.get(deviceName);
        if (d != null) return d;
        return autoCreate(HardwareDevice.class, deviceName.trim());
    }

    /** Typed mapping (hardwareMap.dcMotor etc.) that auto-creates missing devices. */
    public class AutoMapping<DEVICE_TYPE extends HardwareDevice> extends DeviceMapping<DEVICE_TYPE> {
        private final Class<DEVICE_TYPE> type;
        public AutoMapping(Class<DEVICE_TYPE> deviceTypeClass) { super(deviceTypeClass); this.type = deviceTypeClass; }
        @Override public DEVICE_TYPE get(String deviceName) {
            deviceName = deviceName.trim();
            if (contains(deviceName)) return super.get(deviceName);
            HardwareDevice created = autoCreate(type, deviceName);
            if (created != null && type.isInstance(created)) return type.cast(created);
            return super.get(deviceName); // throws the SDK's IllegalArgumentException
        }
    }
}
