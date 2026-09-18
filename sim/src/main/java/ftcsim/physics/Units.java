package ftcsim.physics;

public final class Units {
    private Units() {}
    public static final double INCH = 0.0254;
    public static double inToM(double in) { return in * INCH; }
    public static double mToIn(double m) { return m / INCH; }
    public static double mmToM(double mm) { return mm / 1000.0; }
    public static double rpmToRadPerSec(double rpm) { return rpm * 2 * Math.PI / 60.0; }
    public static double kgCmToNm(double kgcm) { return kgcm * 0.0980665; }
}
