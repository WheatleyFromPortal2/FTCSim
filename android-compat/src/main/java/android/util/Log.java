package android.util;

/**
 * Desktop implementation of android.util.Log. Messages are forwarded to a
 * pluggable {@link Sink} (FTCSim installs one that shows them in its UI) and,
 * by default, to standard output.
 */
public final class Log {
    public static final int VERBOSE = 2, DEBUG = 3, INFO = 4, WARN = 5, ERROR = 6, ASSERT = 7;

    /** Receives every log message. */
    public interface Sink { void log(int priority, String tag, String message, Throwable tr); }

    private static volatile Sink sink = (priority, tag, msg, tr) -> {
        String p = priority == VERBOSE ? "V" : priority == DEBUG ? "D" : priority == INFO ? "I" : priority == WARN ? "W" : priority == ERROR ? "E" : "A";
        System.out.println(p + "/" + tag + ": " + msg);
        if (tr != null) tr.printStackTrace(System.out);
    };

    private Log() {}
    public static void setSink(Sink s) { sink = s; }
    public static Sink getSink() { return sink; }

    private static int emit(int priority, String tag, String msg, Throwable tr) {
        Sink s = sink;
        if (s != null) s.log(priority, tag, msg, tr);
        return (msg == null ? 0 : msg.length());
    }
    public static int v(String tag, String msg) { return emit(VERBOSE, tag, msg, null); }
    public static int v(String tag, String msg, Throwable tr) { return emit(VERBOSE, tag, msg, tr); }
    public static int d(String tag, String msg) { return emit(DEBUG, tag, msg, null); }
    public static int d(String tag, String msg, Throwable tr) { return emit(DEBUG, tag, msg, tr); }
    public static int i(String tag, String msg) { return emit(INFO, tag, msg, null); }
    public static int i(String tag, String msg, Throwable tr) { return emit(INFO, tag, msg, tr); }
    public static int w(String tag, String msg) { return emit(WARN, tag, msg, null); }
    public static int w(String tag, String msg, Throwable tr) { return emit(WARN, tag, msg, tr); }
    public static int w(String tag, Throwable tr) { return emit(WARN, tag, "", tr); }
    public static int e(String tag, String msg) { return emit(ERROR, tag, msg, null); }
    public static int e(String tag, String msg, Throwable tr) { return emit(ERROR, tag, msg, tr); }
    public static int wtf(String tag, String msg) { return emit(ASSERT, tag, msg, null); }
    public static int wtf(String tag, Throwable tr) { return emit(ASSERT, tag, "", tr); }
    public static int wtf(String tag, String msg, Throwable tr) { return emit(ASSERT, tag, msg, tr); }
    public static int println(int priority, String tag, String msg) { return emit(priority, tag, msg, null); }
    public static boolean isLoggable(String tag, int level) { return true; }
    public static String getStackTraceString(Throwable tr) {
        if (tr == null) return "";
        java.io.StringWriter sw = new java.io.StringWriter();
        tr.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }
}
