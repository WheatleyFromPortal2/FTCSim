package android.os;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Desktop stand-in for the hidden android.os.SystemProperties class the SDK
 * reaches through reflection. FTCSim answers like a REV Control Hub.
 */
public final class SystemProperties {
    private static final Map<String, String> props = new ConcurrentHashMap<>();
    static {
        props.put("ro.serialno", "FTCSIM0001");
        props.put("ro.product.model", "FTCSim");
        props.put("ro.product.manufacturer", "FTCSim");
        props.put("ro.build.version.sdk", "28");
        props.put("persist.ftcandroid.serialasusb", "true");
        props.put("ro.ftcandroid.serialasusb", "true");
        props.put("ro.ftcandroid.controlhub", "true");
        props.put("persist.ftcandroid.controlhub", "true");
    }
    private SystemProperties() {}
    public static String get(String key) { String v = props.get(key); return v == null ? "" : v; }
    public static String get(String key, String def) { String v = props.get(key); return v == null ? def : v; }
    public static boolean getBoolean(String key, boolean def) {
        String v = props.get(key);
        if (v == null) return def;
        return v.equals("1") || v.equalsIgnoreCase("y") || v.equalsIgnoreCase("yes") || v.equalsIgnoreCase("true") || v.equalsIgnoreCase("on");
    }
    public static int getInt(String key, int def) { try { return Integer.parseInt(get(key, String.valueOf(def))); } catch (NumberFormatException e) { return def; } }
    public static long getLong(String key, long def) { try { return Long.parseLong(get(key, String.valueOf(def))); } catch (NumberFormatException e) { return def; } }
    public static void set(String key, String val) { if (val == null) props.remove(key); else props.put(key, val); }
}
