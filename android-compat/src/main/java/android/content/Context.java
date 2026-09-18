package android.content;

import android.content.res.Resources;
import java.io.File;

/**
 * Desktop stand-in for android.content.Context. FTCSim installs a single
 * application context; resource lookups return neutral values so sample code
 * that touches the Android UI does not crash.
 */
public class Context {
    public static final String ACTIVITY_SERVICE = "activity";
    public static final String AUDIO_SERVICE = "audio";
    public static final String WIFI_SERVICE = "wifi";
    public static final String WIFI_P2P_SERVICE = "wifip2p";
    public static final String USB_SERVICE = "usb";
    public static final String POWER_SERVICE = "power";
    public static final String VIBRATOR_SERVICE = "vibrator";
    public static final String NOTIFICATION_SERVICE = "notification";
    public static final String CONNECTIVITY_SERVICE = "connectivity";
    public static final String INPUT_SERVICE = "input";
    public static final String LAYOUT_INFLATER_SERVICE = "layout_inflater";
    public static final int MODE_PRIVATE = 0;

    private final Resources resources = new Resources();
    private static File filesDir = new File(System.getProperty("java.io.tmpdir"), "ftcsim-files");

    public static void setFilesDir(File dir) { filesDir = dir; }

    public Resources getResources() { return resources; }
    public String getString(int resId) { return resources.getString(resId); }
    public String getString(int resId, Object... formatArgs) { return resources.getString(resId, formatArgs); }
    public CharSequence getText(int resId) { return getString(resId); }
    public String getPackageName() { return "com.qualcomm.ftcrobotcontroller"; }
    public Context getApplicationContext() { return this; }
    private static final android.net.wifi.WifiManager WIFI = new android.net.wifi.WifiManager();
    private static final android.content.pm.PackageManager PACKAGE_MANAGER = new android.content.pm.PackageManager();
    public Object getSystemService(String name) { return WIFI_SERVICE.equals(name) ? WIFI : null; }
    public android.content.pm.PackageManager getPackageManager() { return PACKAGE_MANAGER; }
    public SharedPreferences getSharedPreferences(String name, int mode) { return android.preference.PreferenceManager.getSharedPreferences(name); }
    public File getFilesDir() { filesDir.mkdirs(); return filesDir; }
    public File getCacheDir() { File f = new File(filesDir, "cache"); f.mkdirs(); return f; }
    public File getExternalFilesDir(String type) { File f = type == null ? filesDir : new File(filesDir, type); f.mkdirs(); return f; }
    public File getDir(String name, int mode) { File f = new File(filesDir, name); f.mkdirs(); return f; }
    public ClassLoader getClassLoader() { return Context.class.getClassLoader(); }
    public boolean bindService(Object intent, Object conn, int flags) { return false; }
    public void startActivity(Object intent) {}
    public void sendBroadcast(Object intent) {}
}
