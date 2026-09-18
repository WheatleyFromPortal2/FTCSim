package android.os;

public final class Looper {
    private static final Looper MAIN = new Looper();
    private Looper() {}
    public static Looper getMainLooper() { return MAIN; }
    public static Looper myLooper() { return MAIN; }
    public static void prepare() {}
    public static void loop() {}
    public Thread getThread() { return Thread.currentThread(); }
    public boolean isCurrentThread() { return true; }
}
