package ftcsim.hardware;

import java.util.Map;

/** Implemented by every simulated device so the UI can show and manipulate it. */
public interface SimDevice {
    String simName();
    /** Device type as listed in the robot configuration (e.g. "DcMotorEx"). */
    String simType();
    String simHub();
    int simPort();
    /** Adds live values (for display) to the map. */
    void fillView(Map<String, Object> view);
    /** Applies a value set from the UI (e.g. a sensor reading). Returns true if handled. */
    default boolean applyInput(String key, Object value) { return false; }
    /** Called when an OpMode is (re)initialised, like the hub's resetDeviceConfigurationForOpMode. */
    default void simResetForOpMode() {}

    static double asDouble(Object v, double def) {
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) { try { return Double.parseDouble((String) v); } catch (NumberFormatException e) { return def; } }
        return def;
    }
    static boolean asBool(Object v, boolean def) {
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof Number) return ((Number) v).doubleValue() != 0;
        if (v instanceof String) return Boolean.parseBoolean((String) v);
        return def;
    }
}
