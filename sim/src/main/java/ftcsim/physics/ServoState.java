package ftcsim.physics;

import com.qualcomm.robotcore.hardware.PwmControl;

/** Simulation state of one servo port (positional or continuous rotation). */
public final class ServoState {
    public final int port;
    public final String name;
    public volatile boolean continuous;
    /** Raw commanded position 0..1 as sent to the hub (CR servos: 0.5 = stopped). */
    public volatile double commanded = 0.5;
    public volatile boolean pwmEnabled = false;   // the hub only drives a servo once a position was written
    public volatile boolean everCommanded = false;
    public volatile PwmControl.PwmRange pwmRange = PwmControl.PwmRange.defaultRange;
    /** Seconds to travel the full 0..1 range (positional). */
    public volatile double secondsFullRange = 0.6;
    /** Max speed in output revolutions per second (continuous). */
    public volatile double maxRevPerSec = 1.5;
    public volatile double idleCurrent = 0.05, movingCurrent = 0.5;

    private double position = 0.5;   // actual (positional, 0..1)
    private double angle;            // accumulated output angle (continuous), rad
    private double omega;
    private volatile double current;

    public ServoState(int port, String name) { this.port = port; this.name = name; }

    public double position() { return position; }
    public double angle() { return angle; }
    public double omega() { return omega; }
    public double current() { return current; }
    public void setPosition(double p) { position = p; }

    /** Pulse width in microseconds currently commanded (for LED drivers such as the Blinkin). */
    public double pulseWidthUs() {
        PwmControl.PwmRange r = pwmRange;
        return r.usPulseLower + commanded * (r.usPulseUpper - r.usPulseLower);
    }

    public void step(double dt) {
        if (!pwmEnabled || !everCommanded) { current = 0; omega = 0; return; }
        if (continuous) {
            double power = (commanded - 0.5) * 2.0;
            if (Math.abs(power) < 0.02) power = 0;
            omega = power * maxRevPerSec * 2 * Math.PI;
            angle += omega * dt;
            current = idleCurrent + Math.abs(power) * movingCurrent;
        } else {
            double rate = 1.0 / Math.max(1e-3, secondsFullRange);
            double delta = commanded - position;
            double stepMax = rate * dt;
            double move = Math.max(-stepMax, Math.min(stepMax, delta));
            position += move;
            omega = move / dt;
            current = idleCurrent + (Math.abs(delta) > 1e-3 ? movingCurrent : 0);
        }
    }

    public void resetForOpMode() {
        pwmRange = PwmControl.PwmRange.defaultRange;
    }
}
