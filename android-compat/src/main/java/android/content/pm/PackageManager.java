package android.content.pm;

/** Minimal stand-in for android.content.pm.PackageManager. */
public class PackageManager {
    public static final int GET_META_DATA = 128;

    public static class NameNotFoundException extends Exception {
        public NameNotFoundException() { super(); }
        public NameNotFoundException(String name) { super(name); }
    }

    public PackageInfo getPackageInfo(String packageName, int flags) throws NameNotFoundException {
        PackageInfo info = new PackageInfo();
        info.packageName = packageName;
        return info;
    }

    public boolean hasSystemFeature(String name) { return false; }
}
