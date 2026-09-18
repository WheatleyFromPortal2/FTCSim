package ftcsim.vision;

/** Implemented by the simulator's VisionProcessor stand-ins; the simulated VisionPortal drives them instead of camera frames. */
public interface SimVisionProcessor {
    /** Produces the processor's results for one simulated frame. */
    void simFrame(CameraMount mount, int widthPx, int heightPx, long frameNanos);
    /** Short description for the UI (e.g. detected tag ids). */
    default String simSummary() { return ""; }
}
