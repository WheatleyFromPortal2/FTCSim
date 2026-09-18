package ftcsim.physics;

/** Simple battery model: terminal voltage sags with total current draw. */
public final class Battery {
    public volatile double openCircuitVolts = 12.8;
    public volatile double internalResistanceOhms = 0.06;
    public volatile double quiescentCurrent = 0.5;   // hubs, sensors
    private volatile double volts = 12.8;
    private volatile double current;

    public double volts() { return volts; }
    public double current() { return current; }

    public void step(double loadCurrent) {
        current = loadCurrent + quiescentCurrent;
        volts = Math.max(6.0, openCircuitVolts - internalResistanceOhms * current);
    }
}
