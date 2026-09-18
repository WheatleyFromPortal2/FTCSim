package ftcsim.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Central log of the simulator. Everything the robot controller would write to
 * logcat (RobotLog, android.util.Log, System.out of the OpMode) ends up here so
 * the browser UI can show it.
 */
public final class SimLog {
    public enum Level { VERBOSE, DEBUG, INFO, WARN, ERROR }

    public static final class Entry {
        public final long seq;
        public final long timeMs;
        public final Level level;
        public final String tag;
        public final String message;
        Entry(long seq, long timeMs, Level level, String tag, String message) {
            this.seq = seq; this.timeMs = timeMs; this.level = level; this.tag = tag; this.message = message;
        }
        @Override public String toString() { return level.name().charAt(0) + "/" + tag + ": " + message; }
    }

    private static final int CAPACITY = 4000;
    private static final ArrayDeque<Entry> entries = new ArrayDeque<>();
    private static final List<Consumer<Entry>> listeners = new CopyOnWriteArrayList<>();
    private static long nextSeq = 1;
    private static volatile boolean echoToStdout = true;

    private SimLog() {}

    public static void setEchoToStdout(boolean echo) { echoToStdout = echo; }

    public static void log(Level level, String tag, String message, Throwable tr) {
        if (message == null) message = "";
        if (tr != null) {
            StringWriter sw = new StringWriter();
            tr.printStackTrace(new PrintWriter(sw));
            message = message.isEmpty() ? sw.toString() : message + "\n" + sw;
        }
        Entry e;
        synchronized (entries) {
            e = new Entry(nextSeq++, System.currentTimeMillis(), level, tag == null ? "" : tag, message);
            entries.addLast(e);
            while (entries.size() > CAPACITY) entries.pollFirst();
        }
        if (echoToStdout && level.ordinal() >= Level.DEBUG.ordinal()) {
            StdoutCapture.originalOut().println(e);
        }
        for (Consumer<Entry> l : listeners) {
            try { l.accept(e); } catch (RuntimeException ignored) {}
        }
    }

    public static void v(String tag, String msg) { log(Level.VERBOSE, tag, msg, null); }
    public static void d(String tag, String msg) { log(Level.DEBUG, tag, msg, null); }
    public static void i(String tag, String msg) { log(Level.INFO, tag, msg, null); }
    public static void w(String tag, String msg) { log(Level.WARN, tag, msg, null); }
    public static void e(String tag, String msg) { log(Level.ERROR, tag, msg, null); }
    public static void e(String tag, String msg, Throwable tr) { log(Level.ERROR, tag, msg, tr); }

    public static void addListener(Consumer<Entry> l) { listeners.add(l); }
    public static void removeListener(Consumer<Entry> l) { listeners.remove(l); }

    /** Returns entries with seq greater than the given value (oldest first). */
    public static List<Entry> since(long seq, int max) {
        List<Entry> out = new ArrayList<>();
        synchronized (entries) {
            for (Entry e : entries) {
                if (e.seq > seq) { out.add(e); if (out.size() >= max) break; }
            }
        }
        return out;
    }

    public static long latestSeq() { synchronized (entries) { return nextSeq - 1; } }

    public static void clear() { synchronized (entries) { entries.clear(); } }
}
