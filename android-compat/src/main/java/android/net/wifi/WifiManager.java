package android.net.wifi;

/** Desktop stand-in for android.net.wifi.WifiManager: there is no Wi-Fi radio to lock or query. */
public class WifiManager {
    public static final int WIFI_MODE_FULL = 1;
    public static final int WIFI_MODE_SCAN_ONLY = 2;
    public static final int WIFI_MODE_FULL_HIGH_PERF = 3;
    public static final int WIFI_MODE_FULL_LOW_LATENCY = 4;
    public static final int WIFI_STATE_ENABLED = 3;

    public class WifiLock {
        private final String tag;
        private boolean refCounted = true;
        private int count;
        WifiLock(String tag) { this.tag = tag; }
        public void acquire() { count++; }
        public void release() { if (count > 0) count--; }
        public boolean isHeld() { return count > 0; }
        public void setReferenceCounted(boolean refCounted) { this.refCounted = refCounted; }
        @Override public String toString() { return "WifiLock{" + tag + " held=" + isHeld() + " refCounted=" + refCounted + "}"; }
    }

    public WifiLock createWifiLock(int lockType, String tag) { return new WifiLock(tag); }
    public WifiLock createWifiLock(String tag) { return new WifiLock(tag); }
    public boolean isWifiEnabled() { return true; }
    public boolean setWifiEnabled(boolean enabled) { return true; }
    public int getWifiState() { return WIFI_STATE_ENABLED; }
    public WifiInfo getConnectionInfo() { return new WifiInfo(); }
    public boolean is5GHzBandSupported() { return true; }
    public boolean isP2pSupported() { return false; }
}
