package ftcsim.physics;

/**
 * Linear brushed DC motor model:
 *   torque  = stallTorque * (V / Vnom) - (stallTorque / freeSpeed) * omega
 *   current = stallCurrent * |V/Vnom - omega/freeSpeed| + noLoadCurrent
 * evaluated at the output shaft of the gearmotor.
 */
public final class MotorModel {
    public static final double V_NOM = 12.0;
    public final MotorType type;
    public MotorModel(MotorType type) { this.type = type; }

    /** Torque (N·m) produced at the output shaft for an applied voltage and shaft speed (rad/s). */
    public double torque(double volts, double omega) {
        double kv = type.stallTorqueNm / type.freeSpeedRadPerSec();
        return type.stallTorqueNm * (volts / V_NOM) - kv * omega;
    }

    /** Braking torque (N·m) when the windings are shorted (BRAKE zero power behaviour). */
    public double brakeTorque(double omega) {
        double kv = type.stallTorqueNm / type.freeSpeedRadPerSec();
        return -kv * omega;
    }

    /** Current draw (A) from the battery for an applied voltage and shaft speed. */
    public double current(double volts, double omega) {
        if (volts == 0) return 0;
        double i = type.stallCurrentA * Math.abs(volts / V_NOM - omega / type.freeSpeedRadPerSec()) + type.noLoadCurrentA;
        return Math.min(i, type.stallCurrentA * 1.2);
    }
}
