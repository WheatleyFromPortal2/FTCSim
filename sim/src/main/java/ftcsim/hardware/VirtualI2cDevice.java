package ftcsim.hardware;

import com.qualcomm.robotcore.hardware.*;

/**
 * A do-nothing I2C bus client handed to the SDK's real I2C driver classes so
 * their constructors work. All register reads return zero; the simulated
 * subclasses of the drivers override the public API instead of talking I2C.
 */
public class VirtualI2cDevice implements I2cDeviceSynch, I2cDeviceSynchReadHistory {
    private final String name;
    private I2cAddr addr = I2cAddr.zero();
    private boolean engaged;
    private String userName;
    private HealthStatus health = HealthStatus.HEALTHY;
    private ReadWindow readWindow;
    private int heartbeatInterval = 500;
    private HeartbeatAction heartbeatAction;
    private boolean logging;
    private String loggingTag = "I2C";

    public VirtualI2cDevice(String name) { this.name = name; }

    @Override public byte read8() { return 0; }
    @Override public byte read8(int ireg) { return 0; }
    @Override public byte[] read(int creg) { return new byte[Math.max(0, creg)]; }
    @Override public byte[] read(int ireg, int creg) { return new byte[Math.max(0, creg)]; }
    @Override public TimestampedData readTimeStamped(int creg) { return stamped(new byte[Math.max(0, creg)]); }
    @Override public TimestampedData readTimeStamped(int ireg, int creg) { return stamped(new byte[Math.max(0, creg)]); }
    private static TimestampedData stamped(byte[] d) { TimestampedData t = new TimestampedData(); t.data = d; t.nanoTime = System.nanoTime(); return t; }
    @Override public void write8(int bVal) {}
    @Override public void write8(int ireg, int bVal) {}
    @Override public void write(byte[] data) {}
    @Override public void write(int ireg, byte[] data) {}
    @Override public void write8(int bVal, I2cWaitControl waitControl) {}
    @Override public void write8(int ireg, int bVal, I2cWaitControl waitControl) {}
    @Override public void write(byte[] data, I2cWaitControl waitControl) {}
    @Override public void write(int ireg, byte[] data, I2cWaitControl waitControl) {}
    @Override public void waitForWriteCompletions(I2cWaitControl waitControl) {}
    @Override public void enableWriteCoalescing(boolean enable) {}
    @Override public boolean isWriteCoalescingEnabled() { return false; }
    @Override public boolean isArmed() { return true; }
    @Override public void setI2cAddr(I2cAddr i2cAddr) { addr = i2cAddr; }
    @Override public I2cAddr getI2cAddr() { return addr; }
    @Override public void setLogging(boolean enabled) { logging = enabled; }
    @Override public boolean getLogging() { return logging; }
    @Override public void setLoggingTag(String loggingTag) { this.loggingTag = loggingTag; }
    @Override public String getLoggingTag() { return loggingTag; }
    @Override public void setReadWindow(ReadWindow window) { readWindow = window; }
    @Override public ReadWindow getReadWindow() { return readWindow; }
    @Override public void ensureReadWindow(ReadWindow windowNeeded, ReadWindow windowToSet) { readWindow = windowToSet; }
    @Override public TimestampedData readTimeStamped(int ireg, int creg, ReadWindow readWindowNeeded, ReadWindow readWindowSet) { return readTimeStamped(ireg, creg); }
    @Override public void setHeartbeatInterval(int ms) { heartbeatInterval = ms; }
    @Override public int getHeartbeatInterval() { return heartbeatInterval; }
    @Override public void setHeartbeatAction(HeartbeatAction action) { heartbeatAction = action; }
    @Override public HeartbeatAction getHeartbeatAction() { return heartbeatAction; }
    @Override public void disengage() { engaged = false; }
    @Override public void engage() { engaged = true; }
    @Override public boolean isEngaged() { return engaged; }
    @Override public void setHealthStatus(HealthStatus status) { health = status; }
    @Override public HealthStatus getHealthStatus() { return health; }
    @Override public void setI2cAddress(I2cAddr newAddress) { addr = newAddress; }
    @Override public I2cAddr getI2cAddress() { return addr; }
    @Override public void setUserConfiguredName(String n) { userName = n; }
    @Override public String getUserConfiguredName() { return userName; }
    @Override public Manufacturer getManufacturer() { return Manufacturer.Lynx; }
    @Override public String getDeviceName() { return "FTCSim virtual I2C device"; }
    @Override public String getConnectionInfo() { return "virtual I2C " + name; }
    @Override public int getVersion() { return 1; }
    @Override public void resetDeviceConfigurationForOpMode() {}
    @Override public void close() {}
    @Override public void setHistoryQueueCapacity(int capacity) {}
    @Override public int getHistoryQueueCapacity() { return 0; }
    @Override public java.util.concurrent.BlockingQueue<TimestampedI2cData> getHistoryQueue() { return new java.util.concurrent.LinkedBlockingQueue<>(); }
}
