package android.os;

/** Desktop implementation of Android's SystemClock backed by System.nanoTime(). */
public final class SystemClock {
    private static final long START_NANOS = System.nanoTime();
    private SystemClock() {}
    public static long uptimeMillis() { return (System.nanoTime() - START_NANOS) / 1_000_000L; }
    public static long elapsedRealtime() { return uptimeMillis(); }
    public static long elapsedRealtimeNanos() { return System.nanoTime() - START_NANOS; }
    public static long currentThreadTimeMillis() { return uptimeMillis(); }
    public static long currentTimeMillis() { return System.currentTimeMillis(); }
    public static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
