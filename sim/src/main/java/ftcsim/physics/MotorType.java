package ftcsim.physics;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Electro-mechanical description of a DC gearmotor as seen at its output shaft
 * (the shaft the encoder counts), at the nominal 12 V.
 */
public final class MotorType {
    public final String name;
    public final double maxRpm;          // free speed at 12 V
    public final double ticksPerRev;     // encoder ticks per output shaft revolution
    public final double stallTorqueNm;   // at 12 V
    public final double stallCurrentA;
    public final double noLoadCurrentA;
    public final double gearing;
    /**
     * Motor orientation as declared by the SDK's motor type ({@code Rotation.CCW} for goBILDA and REV HD Hex,
     * {@code CW} for TETRIX, NeveRest 20, UltraPlanetary...). The SDK inverts the controller-level power and
     * encoder sign for CCW motors so that {@code Direction.FORWARD} always means the same physical sense.
     */
    public final boolean ccw;

    public MotorType(String name, double maxRpm, double ticksPerRev, double stallTorqueNm, double stallCurrentA, double noLoadCurrentA, double gearing) {
        this(name, maxRpm, ticksPerRev, stallTorqueNm, stallCurrentA, noLoadCurrentA, gearing, true);
    }

    public MotorType(String name, double maxRpm, double ticksPerRev, double stallTorqueNm, double stallCurrentA, double noLoadCurrentA, double gearing, boolean ccw) {
        this.name = name; this.maxRpm = maxRpm; this.ticksPerRev = ticksPerRev; this.stallTorqueNm = stallTorqueNm;
        this.stallCurrentA = stallCurrentA; this.noLoadCurrentA = noLoadCurrentA; this.gearing = gearing; this.ccw = ccw;
    }

    /** Sign relating controller-level (raw) power/ticks to the SDK's FORWARD sense: -1 for CCW motor types. */
    public int orientationSign() { return ccw ? -1 : 1; }

    public double freeSpeedRadPerSec() { return Units.rpmToRadPerSec(maxRpm); }
    public double maxTicksPerSecond() { return maxRpm / 60.0 * ticksPerRev; }

    private static final Map<String, MotorType> LIBRARY = new LinkedHashMap<>();
    private static void add(MotorType t) { LIBRARY.put(t.name.toLowerCase(), t); }
    static {
        // goBILDA 5203/5202 Yellow Jacket series (spec sheet values)
        add(new MotorType("goBILDA_6000", 6000, 28, Units.kgCmToNm(1.47), 9.2, 0.25, 1));
        add(new MotorType("goBILDA_1620", 1620, 103.8, Units.kgCmToNm(5.4), 9.2, 0.25, 3.7));
        add(new MotorType("goBILDA_1150", 1150, 145.1, Units.kgCmToNm(7.9), 9.2, 0.25, 5.2));
        add(new MotorType("goBILDA_435", 435, 384.5, Units.kgCmToNm(18.7), 9.2, 0.25, 13.7));
        add(new MotorType("goBILDA_312", 312, 537.7, Units.kgCmToNm(24.3), 9.2, 0.25, 19.2));
        add(new MotorType("goBILDA_223", 223, 751.8, Units.kgCmToNm(38.0), 9.2, 0.25, 26.9));
        add(new MotorType("goBILDA_117", 117, 1425.1, Units.kgCmToNm(68.4), 9.2, 0.25, 50.9));
        add(new MotorType("goBILDA_84", 84, 1993.6, Units.kgCmToNm(93.6), 9.2, 0.25, 71.2));
        add(new MotorType("goBILDA_60", 60, 2786.2, Units.kgCmToNm(133), 9.2, 0.25, 99.5));
        add(new MotorType("goBILDA_43", 43, 3895.9, Units.kgCmToNm(185), 9.2, 0.25, 139.1));
        add(new MotorType("goBILDA_30", 30, 5281.1, Units.kgCmToNm(250), 9.2, 0.25, 188.7));
        // REV
        add(new MotorType("REV_HD_HEX_BARE", 6000, 28, 0.105, 9.2, 0.3, 1));
        add(new MotorType("REV_HD_HEX_20", 300, 560, 2.1, 9.2, 0.3, 20));
        add(new MotorType("REV_HD_HEX_40", 150, 1120, 4.2, 9.2, 0.3, 40));
        add(new MotorType("REV_CORE_HEX", 125, 288, 3.2, 4.4, 0.2, 72));
        add(new MotorType("REV_UltraPlanetary_20", 300, 560, 2.1, 9.2, 0.3, 20, false));
        // AndyMark
        add(new MotorType("NeveRest_3.7", 1780, 103.6, 0.35, 9.8, 0.4, 3.7));
        add(new MotorType("NeveRest_20", 340, 537.6, 1.83, 9.8, 0.4, 20, false));
        add(new MotorType("NeveRest_40", 160, 1120, 3.66, 9.8, 0.4, 40));
        add(new MotorType("NeveRest_60", 105, 1680, 5.5, 9.8, 0.4, 60));
        // TETRIX
        add(new MotorType("TETRIX_TorqueNADO", 100, 1440, 6.0, 8.2, 0.5, 60, false));
        add(new MotorType("TETRIX_DC", 152, 1440, 2.5, 5.0, 0.3, 52, false));
        // Matrix
        add(new MotorType("Matrix_12V", 210, 1478.4, 3.0, 5.0, 0.3, 52, false));
    }

    public static MotorType byName(String name) {
        if (name == null) return LIBRARY.get("gobilda_312");
        MotorType t = LIBRARY.get(name.toLowerCase());
        if (t == null) t = LIBRARY.get(name.toLowerCase().replace("-", "_"));
        return t;
    }
    public static Map<String, MotorType> library() { return LIBRARY; }
    public static MotorType defaultType() { return LIBRARY.get("gobilda_312"); }

    /** Custom motor type from raw parameters. */
    public static MotorType custom(double maxRpm, double ticksPerRev, double stallTorqueNm, double stallCurrentA) {
        return custom(maxRpm, ticksPerRev, stallTorqueNm, stallCurrentA, true);
    }

    public static MotorType custom(double maxRpm, double ticksPerRev, double stallTorqueNm, double stallCurrentA, boolean ccw) {
        return new MotorType("custom", maxRpm, ticksPerRev, stallTorqueNm, stallCurrentA, 0.25, 1, ccw);
    }
}
