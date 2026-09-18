package com.qualcomm.hardware.lynx;

import com.qualcomm.hardware.lynx.commands.LynxCommand;
import com.qualcomm.hardware.lynx.commands.LynxInterface;
import com.qualcomm.hardware.lynx.commands.LynxMessage;
import com.qualcomm.robotcore.hardware.Blinker;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.HardwareDeviceHealth;
import com.qualcomm.robotcore.hardware.LynxModuleImuType;
import com.qualcomm.robotcore.hardware.VisuallyIdentifiableHardwareDevice;
import com.qualcomm.robotcore.hardware.usb.RobotArmingStateNotifier;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;
import org.firstinspires.ftc.robotcore.external.navigation.TempUnit;
import org.firstinspires.ftc.robotcore.external.navigation.VoltageUnit;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * FTCSim replacement for the SDK's LynxModule (REV Control Hub / Expansion Hub).
 * <p>
 * Presents the same public API the real class does (bulk caching modes, voltage
 * and current monitoring, firmware version, LED patterns, ...) while the data
 * comes from the simulation. The bulk-caching semantics of the real hub are
 * reproduced: in MANUAL mode reads return the values captured by the last
 * {@link #clearBulkCache()}; in AUTO mode the cache is refreshed whenever a read
 * repeats since the last refresh.
 */
public class LynxModule extends LynxCommExceptionHandler implements LynxModuleIntf, RobotArmingStateNotifier, RobotArmingStateNotifier.Callback, Blinker, VisuallyIdentifiableHardwareDevice {
    public static final String TAG = "LynxModule";

    public enum BulkCachingMode { OFF, MANUAL, AUTO }
    public enum DebugGroup { NONE, MAIN, TOHOST, FROMHOST, ADC, PWMSERVO, MODULELED, DIGITALIO, I2C, MOTOR0, MOTOR1, MOTOR2, MOTOR3;
        public final byte bVal = (byte) ordinal();
        public static DebugGroup fromInt(int b) { return values()[Math.max(0, Math.min(values().length - 1, b))]; }
    }
    public enum DebugVerbosity { OFF, LOW, MEDIUM, HIGH;
        public final byte bVal = (byte) ordinal();
        public static DebugVerbosity fromInt(int b) { return values()[Math.max(0, Math.min(values().length - 1, b))]; }
    }
    public interface BlinkerPolicy {
        List<Blinker.Step> getIdlePattern(LynxModule lynxModule);
        List<Blinker.Step> getVisuallyIdentifyPattern(LynxModule lynxModule);
    }
    public static BlinkerPolicy blinkerPolicy = new BlinkerPolicy() {
        @Override public List<Blinker.Step> getIdlePattern(LynxModule lynxModule) { return Collections.emptyList(); }
        @Override public List<Blinker.Step> getVisuallyIdentifyPattern(LynxModule lynxModule) { return Collections.emptyList(); }
    };

    /** Snapshot of all bulk-readable inputs of the hub, like the real hub's bulk read response. */
    public static class BulkData {
        public final int[] motorPosition = new int[4];
        public final int[] motorVelocity = new int[4];
        public final boolean[] motorBusy = new boolean[4];
        public final boolean[] motorOverCurrent = new boolean[4];
        public final boolean[] digitalState = new boolean[8];
        public final double[] analogVoltage = new double[4];
        private final boolean fake;
        public BulkData(boolean fake) { this.fake = fake; }
        public boolean getDigitalChannelState(int digitalInputZ) { return digitalState[digitalInputZ]; }
        public int getMotorCurrentPosition(int motorZ) { return motorPosition[motorZ]; }
        public int getMotorVelocity(int motorZ) { return motorVelocity[motorZ]; }
        public boolean isMotorBusy(int motorZ) { return motorBusy[motorZ]; }
        public boolean isMotorOverCurrent(int motorZ) { return motorOverCurrent[motorZ]; }
        public double getAnalogInputVoltage(int inputZ) { return analogVoltage[inputZ]; }
        public double getAnalogInputVoltage(int inputZ, VoltageUnit unit) { return unit.convert(analogVoltage[inputZ], VoltageUnit.VOLTS); }
        public boolean isFake() { return fake; }
    }

    /** Implemented by the simulated controllers attached to this hub to fill a bulk read. */
    public interface BulkProvider { void fillBulkData(BulkData data); }

    // ---- simulation state ----
    private final String name;
    private final int moduleAddress;
    private final boolean parent;
    private final SerialNumber serialNumber;
    private final List<BulkProvider> bulkProviders = new CopyOnWriteArrayList<>();
    private final Object bulkCachingLock = new Object();
    private BulkCachingMode bulkCachingMode = BulkCachingMode.OFF;
    private BulkData lastBulkData = null;
    private final Set<String> bulkReadsSinceRefresh = new HashSet<>();
    private volatile double inputVoltage = 12.8;
    private volatile double current = 0.0;
    private volatile double temperatureC = 35.0;
    private volatile boolean engaged = true;
    private volatile boolean open = true;
    private Collection<Blinker.Step> pattern = new ArrayList<>();
    private final Deque<Collection<Blinker.Step>> patternStack = new ArrayDeque<>();
    private volatile boolean visuallyIdentifying;
    private final Map<RobotArmingStateNotifier.Callback, Boolean> callbacks = new WeakHashMap<>();
    private LynxModuleImuType imuType = LynxModuleImuType.BHI260;
    private long bulkReadCount;

    public LynxModule(String name, int moduleAddress, boolean parent) {
        super(TAG);
        this.name = name;
        this.moduleAddress = moduleAddress;
        this.parent = parent;
        this.serialNumber = SerialNumber.createFake();
    }

    // ---- simulation hooks ----
    public void addBulkProvider(BulkProvider p) { bulkProviders.add(p); }
    public void setInputVoltage(double volts) { inputVoltage = volts; }
    public void setCurrent(double amps) { current = amps; }
    public void setTemperature(double c) { temperatureC = c; }
    public void setImuType(LynxModuleImuType t) { imuType = t; }
    public long getBulkReadCount() { return bulkReadCount; }
    public String getName() { return name; }

    /**
     * Performs a read of a bulk-readable quantity honouring the bulk caching mode.
     * @param key identifies the read (e.g. "motorpos/2"); used for AUTO mode refresh detection
     */
    public <T> T bulkRead(String key, Supplier<T> live, Function<BulkData, T> fromCache) {
        synchronized (bulkCachingLock) {
            switch (bulkCachingMode) {
                case OFF:
                    return live.get();
                case MANUAL:
                    if (lastBulkData == null) refreshBulkData();
                    return fromCache.apply(lastBulkData);
                case AUTO:
                default:
                    if (lastBulkData == null || bulkReadsSinceRefresh.contains(key)) refreshBulkData();
                    bulkReadsSinceRefresh.add(key);
                    return fromCache.apply(lastBulkData);
            }
        }
    }

    private void refreshBulkData() {
        BulkData d = new BulkData(false);
        for (BulkProvider p : bulkProviders) p.fillBulkData(d);
        lastBulkData = d;
        bulkReadsSinceRefresh.clear();
        bulkReadCount++;
    }

    // ---- public API mirrored from the SDK ----
    @Override public String toString() { return String.format("LynxModule(%s, addr=%d)", name, moduleAddress); }
    public void close() { open = false; }
    public boolean isOpen() { return open; }
    public boolean isUserModule() { return true; }
    public void setUserModule(boolean b) {}
    public boolean isSystemSynthetic() { return false; }
    public void setSystemSynthetic(boolean b) {}
    public int getRevProductNumber() { return parent ? 0x11 : 0x10; }
    public int getModuleAddress() { return moduleAddress; }
    public void setNewModuleAddress(int address) {}
    public void setAttentionRequired(boolean b) {}
    public void noteNotResponding() {}
    public boolean isNotResponding() { return false; }
    public HardwareDevice.Manufacturer getManufacturer() { return HardwareDevice.Manufacturer.Lynx; }
    public String getDeviceName() { return parent ? "Control Hub" : "Expansion Hub"; }
    public String getFirmwareVersionString() { return "HW: 20, Maj: 1, Min: 8, Eng: 2"; }
    public String getNullableFirmwareVersionString() { return getFirmwareVersionString(); }
    public String getConnectionInfo() { return "FTCSim virtual hub " + name + " (address " + moduleAddress + ")"; }
    public int getVersion() { return 1; }
    public void resetDeviceConfigurationForOpMode() {
        synchronized (bulkCachingLock) { bulkCachingMode = BulkCachingMode.OFF; lastBulkData = null; bulkReadsSinceRefresh.clear(); }
    }
    public List<String> getGlobalWarnings() { return new ArrayList<>(); }
    public static String getHealthStatusWarningMessage(HardwareDeviceHealth h) { return ""; }
    public SerialNumber getModuleSerialNumber() { return serialNumber; }
    public SerialNumber getSerialNumber() { return serialNumber; }
    public RobotArmingStateNotifier.ARMINGSTATE getArmingState() { return RobotArmingStateNotifier.ARMINGSTATE.ARMED; }
    public void registerCallback(RobotArmingStateNotifier.Callback callback, boolean doInitialCallback) {
        synchronized (callbacks) { callbacks.put(callback, true); }
        if (doInitialCallback) callback.onModuleStateChange(this, getArmingState());
    }
    public void unregisterCallback(RobotArmingStateNotifier.Callback callback) { synchronized (callbacks) { callbacks.remove(callback); } }
    public void onModuleStateChange(RobotArmingStateNotifier module, RobotArmingStateNotifier.ARMINGSTATE state) {}
    public void engage() { engaged = true; }
    public void disengage() { engaged = false; }
    public boolean isEngaged() { return engaged; }
    public void visuallyIdentify(boolean shouldIdentify) { visuallyIdentifying = shouldIdentify; }
    public boolean isVisuallyIdentifying() { return visuallyIdentifying; }
    public int getBlinkerPatternMaxLength() { return 16; }
    public void setConstant(int color) { Blinker.Step step = new Blinker.Step(color, 1, java.util.concurrent.TimeUnit.SECONDS); setPattern(Collections.singletonList(step)); }
    public void stopBlinking() { setPattern(Collections.emptyList()); }
    public synchronized void setPattern(Collection<Blinker.Step> steps) { pattern = steps == null ? new ArrayList<>() : new ArrayList<>(steps); }
    public synchronized Collection<Blinker.Step> getPattern() { return new ArrayList<>(pattern); }
    public synchronized void pushPattern(Collection<Blinker.Step> steps) { patternStack.push(pattern); setPattern(steps); }
    public synchronized boolean patternStackNotEmpty() { return !patternStack.isEmpty(); }
    public synchronized boolean popPattern() { if (patternStack.isEmpty()) return false; pattern = patternStack.pop(); return true; }
    public boolean isParent() { return parent; }
    public void pingAndQueryKnownInterfacesAndEtc() {}
    public void validateCommand(LynxMessage command) {}
    public boolean isCommandSupported(Class<? extends LynxCommand> clazz) { return true; }
    public LynxInterface getInterface(String interfaceName) { return null; }
    public void resetPingTimer(LynxMessage message) {}
    public BulkData getBulkData() { synchronized (bulkCachingLock) { refreshBulkData(); return lastBulkData; } }
    public BulkCachingMode getBulkCachingMode() { synchronized (bulkCachingLock) { return bulkCachingMode; } }
    public void setBulkCachingMode(BulkCachingMode mode) {
        synchronized (bulkCachingLock) {
            bulkCachingMode = mode;
            lastBulkData = null;
            bulkReadsSinceRefresh.clear();
        }
    }
    public void clearBulkCache() {
        synchronized (bulkCachingLock) {
            if (bulkCachingMode == BulkCachingMode.OFF) {
                RobotLog.ww(TAG, "clearBulkCache() called while bulk caching mode is OFF (%s)", name);
            }
            lastBulkData = null;
            bulkReadsSinceRefresh.clear();
        }
    }
    public void failSafe() {}
    public void attemptFailSafeAndIgnoreErrors() {}
    public void enablePhoneCharging(boolean enable) {}
    public boolean isPhoneChargingEnabled() { return false; }
    public double getCurrent(CurrentUnit unit) { return unit.convert(current, CurrentUnit.AMPS); }
    public double getGpioBusCurrent(CurrentUnit unit) { return unit.convert(0.05, CurrentUnit.AMPS); }
    public double getI2cBusCurrent(CurrentUnit unit) { return unit.convert(0.05, CurrentUnit.AMPS); }
    public double getInputVoltage(VoltageUnit unit) { return unit.convert(inputVoltage, VoltageUnit.VOLTS); }
    public double getAuxiliaryVoltage(VoltageUnit unit) { return unit.convert(5.0, VoltageUnit.VOLTS); }
    public double getTemperature(TempUnit unit) { return unit.fromCelsius(temperatureC); }
    public LynxModuleImuType getImuType() { return imuType; }
    public void setDebug(DebugGroup group, DebugVerbosity verbosity) {}
    public <T> T acquireI2cLockWhile(com.qualcomm.hardware.lynx.Supplier<T> supplier) throws InterruptedException, com.qualcomm.robotcore.exception.RobotCoreException, LynxNackException { return supplier.get(); }
    public void acquireNetworkTransmissionLock(LynxMessage message) {}
    public void releaseNetworkTransmissionLock(LynxMessage message) {}
    public void sendCommand(LynxMessage command) {}
    public void retransmit(LynxMessage message) {}
    public void finishedWithMessage(LynxMessage message) {}
    public void pretendFinishExtantCommands() {}
    public void abandonUnfinishedCommands() {}
}
