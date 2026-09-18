package android.os;

/** Desktop stand-in for android.os.Process (thread priorities are ignored). */
public final class Process {
    public static final int THREAD_PRIORITY_DEFAULT = 0, THREAD_PRIORITY_LOWEST = 19, THREAD_PRIORITY_BACKGROUND = 10,
        THREAD_PRIORITY_FOREGROUND = -2, THREAD_PRIORITY_DISPLAY = -4, THREAD_PRIORITY_URGENT_DISPLAY = -8,
        THREAD_PRIORITY_AUDIO = -16, THREAD_PRIORITY_URGENT_AUDIO = -19, THREAD_PRIORITY_MORE_FAVORABLE = -1, THREAD_PRIORITY_LESS_FAVORABLE = 1;
    private Process() {}
    public static int myPid() { return (int) ProcessHandle.current().pid(); }
    public static int myTid() { return (int) Thread.currentThread().getId(); }
    public static int myUid() { return 10000; }
    public static void setThreadPriority(int priority) {}
    public static void setThreadPriority(int tid, int priority) {}
    public static int getThreadPriority(int tid) { return THREAD_PRIORITY_DEFAULT; }
    public static void killProcess(int pid) {}
    public static long getElapsedCpuTime() { return System.nanoTime() / 1_000_000L; }
    public static boolean is64Bit() { return true; }
}
