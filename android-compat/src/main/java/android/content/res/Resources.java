package android.content.res;

public class Resources {
    /** Optional hook so the simulator can resolve real resource strings. */
    public interface StringResolver { String resolve(int resId); }
    private static volatile StringResolver stringResolver;
    public static void setStringResolver(StringResolver r) { stringResolver = r; }

    public int getIdentifier(String name, String defType, String defPackage) { return 0; }
    public String getString(int resId) {
        StringResolver r = stringResolver;
        String s = r != null ? r.resolve(resId) : null;
        return s != null ? s : "";
    }
    public String getString(int resId, Object... formatArgs) {
        String s = getString(resId);
        try { return String.format(s, formatArgs); } catch (RuntimeException e) { return s; }
    }
    public CharSequence getText(int resId) { return getString(resId); }
    public int getColor(int resId) { return 0xFF000000; }
    public int getInteger(int resId) { return 0; }
    public boolean getBoolean(int resId) { return false; }
    public float getDimension(int resId) { return 0f; }
    public String[] getStringArray(int resId) { return new String[0]; }
    public static class NotFoundException extends RuntimeException {
        public NotFoundException() {}
        public NotFoundException(String msg) { super(msg); }
    }
}
