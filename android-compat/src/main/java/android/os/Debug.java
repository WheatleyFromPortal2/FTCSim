package android.os;

public final class Debug {
    private Debug() {}
    public static boolean isDebuggerConnected() { return false; }
    public static boolean waitingForDebugger() { return false; }
    public static void waitForDebugger() {}
    public static long getNativeHeapAllocatedSize() { return 0; }
}
