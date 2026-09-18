package ftcsim.physics;

/**
 * Emulation of the REV hub's velocity / position control loops. Output units are
 * the hub's 16-bit "duty" range (-32767..32767) so the coefficient conventions
 * teams are used to (F = 32767 / maxVelocity) hold.
 */
public final class PidfController {
    public double p, i, d, f;
    private double integral;
    private double lastError;
    private boolean hasLast;

    public PidfController(double p, double i, double d, double f) { set(p, i, d, f); }
    public void set(double p, double i, double d, double f) { this.p = p; this.i = i; this.d = d; this.f = f; }
    public void reset() { integral = 0; hasLast = false; lastError = 0; }

    /**
     * Effective control-loop rate used to scale the per-iteration I and D coefficients. 20 Hz makes the
     * usual "F = 32767/maxVel, P = 0.1F, I = 0.01F" guidance behave like it does on a real hub (a fast
     * approach with a few percent of overshoot).
     */
    public static final double HUB_LOOP_HZ = 20.0;
    /** Test hook: overrides the loop rate when non-zero. */
    public static volatile double HUB_LOOP_HZ_OVERRIDE = 0;
    private static double loopHz() { return HUB_LOOP_HZ_OVERRIDE > 0 ? HUB_LOOP_HZ_OVERRIDE : HUB_LOOP_HZ; }

    /** Returns duty in -1..1. */
    public double update(double target, double measured, double dt) {
        double error = target - measured;
        double deriv = hasLast && dt > 0 ? (error - lastError) / (dt * loopHz()) : 0;
        // integral zero-crossing reset: the error built up while spinning up must not carry past the setpoint
        if (hasLast && Math.signum(error) != Math.signum(lastError) && error != 0 && lastError != 0) integral = 0;
        lastError = error; hasLast = true;
        double unsaturated = f * target + p * error + i * integral + d * deriv;
        // anti-windup: do not keep integrating while the output is saturated in the direction of the error
        boolean saturated = Math.abs(unsaturated) >= 32767.0 && Math.signum(unsaturated) == Math.signum(error);
        if (!saturated && i != 0) {
            integral += error * dt * loopHz();
            double integralLimit = 0.25 * 32767.0 / Math.abs(i); // the I term alone never exceeds a quarter of full scale
            integral = Math.max(-integralLimit, Math.min(integralLimit, integral));
        }
        double out = f * target + p * error + i * integral + d * deriv;
        return Math.max(-1.0, Math.min(1.0, out / 32767.0));
    }
}
