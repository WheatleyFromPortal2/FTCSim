package com.qualcomm.robotcore.util;

import android.content.Context;
import com.qualcomm.robotcore.exception.RobotCoreException;
import ftcsim.log.SimLog;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * FTCSim replacement for the SDK's RobotLog. Same static API; messages go to
 * {@link SimLog} instead of logcat. Global error / warning messages are kept so
 * the simulator UI can show them like the Driver Station does.
 */
public class RobotLog {
    public static final String OPMODE_START_TAG = "******************** START - OPMODE %s ********************";
    public static final String OPMODE_STOP_TAG = "******************** STOP - OPMODE %s ********************";
    public static final String TAG = "RobotCore";

    private static final Object globalErrorLock = new Object();
    private static String globalErrorMessage = "";
    private static final Object globalWarningLock = new Object();
    private static String globalWarningMessage = "";
    private static final WeakHashMap<GlobalWarningSource, Integer> globalWarningSources = new WeakHashMap<>();
    private static double msTimeOffset = 0.0;
    private static boolean globalErrorMsgSticky = false;
    private static boolean globalWarningMsgSticky = false;
    private static String matchLogFilename = null;
    private static Calendar matchStartTime = null;

    private RobotLog() {}

    // ---- time synchronisation with the driver station (no-ops on the desktop) ----
    public static void processTimeSynch(long t0, long t1, long t2, long t3) {}
    public static void setMsTimeOffset(double offset) { msTimeOffset = offset; }
    public static long getRemoteTime() { return getRemoteTime(System.currentTimeMillis()); }
    public static long getRemoteTime(long localTime) { return (long) (localTime + msTimeOffset); }
    public static long getLocalTime(long remoteTime) { return (long) (remoteTime - msTimeOffset); }

    private static String fmt(String format, Object... args) {
        if (args == null || args.length == 0) return format;
        try { return String.format(format, args); } catch (RuntimeException e) { return format; }
    }

    // ---- assert-level (mapped to error) ----
    public static void a(String format, Object... args) { internalLog(SimLog.Level.ERROR, TAG, fmt(format, args)); }
    public static void a(String message) { internalLog(SimLog.Level.ERROR, TAG, message); }
    public static void aa(String tag, String format, Object... args) { internalLog(SimLog.Level.ERROR, tag, fmt(format, args)); }
    public static void aa(String tag, String message) { internalLog(SimLog.Level.ERROR, tag, message); }
    public static void aa(String tag, Throwable t, String format, Object... args) { internalLog(SimLog.Level.ERROR, tag, t, fmt(format, args)); }
    public static void aa(String tag, Throwable t, String message) { internalLog(SimLog.Level.ERROR, tag, t, message); }
    // ---- verbose ----
    public static void v(String format, Object... args) { internalLog(SimLog.Level.VERBOSE, TAG, fmt(format, args)); }
    public static void v(String message) { internalLog(SimLog.Level.VERBOSE, TAG, message); }
    public static void vv(String tag, String format, Object... args) { internalLog(SimLog.Level.VERBOSE, tag, fmt(format, args)); }
    public static void vv(String tag, String message) { internalLog(SimLog.Level.VERBOSE, tag, message); }
    public static void vv(String tag, Throwable t, String format, Object... args) { internalLog(SimLog.Level.VERBOSE, tag, t, fmt(format, args)); }
    public static void vv(String tag, Throwable t, String message) { internalLog(SimLog.Level.VERBOSE, tag, t, message); }
    // ---- debug ----
    public static void d(String format, Object... args) { internalLog(SimLog.Level.DEBUG, TAG, fmt(format, args)); }
    public static void d(String message) { internalLog(SimLog.Level.DEBUG, TAG, message); }
    public static void dd(String tag, String format, Object... args) { internalLog(SimLog.Level.DEBUG, tag, fmt(format, args)); }
    public static void dd(String tag, String message) { internalLog(SimLog.Level.DEBUG, tag, message); }
    public static void dd(String tag, Throwable t, String format, Object... args) { internalLog(SimLog.Level.DEBUG, tag, t, fmt(format, args)); }
    public static void dd(String tag, Throwable t, String message) { internalLog(SimLog.Level.DEBUG, tag, t, message); }
    // ---- info ----
    public static void i(String format, Object... args) { internalLog(SimLog.Level.INFO, TAG, fmt(format, args)); }
    public static void i(String message) { internalLog(SimLog.Level.INFO, TAG, message); }
    public static void ii(String tag, String format, Object... args) { internalLog(SimLog.Level.INFO, tag, fmt(format, args)); }
    public static void ii(String tag, String message) { internalLog(SimLog.Level.INFO, tag, message); }
    public static void ii(String tag, Throwable t, String format, Object... args) { internalLog(SimLog.Level.INFO, tag, t, fmt(format, args)); }
    public static void ii(String tag, Throwable t, String message) { internalLog(SimLog.Level.INFO, tag, t, message); }
    // ---- warning ----
    public static void w(String format, Object... args) { internalLog(SimLog.Level.WARN, TAG, fmt(format, args)); }
    public static void w(String message) { internalLog(SimLog.Level.WARN, TAG, message); }
    public static void ww(String tag, String format, Object... args) { internalLog(SimLog.Level.WARN, tag, fmt(format, args)); }
    public static void ww(String tag, String message) { internalLog(SimLog.Level.WARN, tag, message); }
    public static void ww(String tag, Throwable t, String format, Object... args) { internalLog(SimLog.Level.WARN, tag, t, fmt(format, args)); }
    public static void ww(String tag, Throwable t, String message) { internalLog(SimLog.Level.WARN, tag, t, message); }
    // ---- error ----
    public static void e(String format, Object... args) { internalLog(SimLog.Level.ERROR, TAG, fmt(format, args)); }
    public static void e(String message) { internalLog(SimLog.Level.ERROR, TAG, message); }
    public static void ee(String tag, String format, Object... args) { internalLog(SimLog.Level.ERROR, tag, fmt(format, args)); }
    public static void ee(String tag, String message) { internalLog(SimLog.Level.ERROR, tag, message); }
    public static void ee(String tag, Throwable t, String format, Object... args) { internalLog(SimLog.Level.ERROR, tag, t, fmt(format, args)); }
    public static void ee(String tag, Throwable t, String message) { internalLog(SimLog.Level.ERROR, tag, t, message); }

    /** Android priority ints (2..7) as used by the real RobotLog.internalLog. */
    public static void internalLog(int priority, String tag, String message) { SimLog.log(levelOf(priority), tag, message, null); }
    public static void internalLog(int priority, String tag, Throwable t, String message) { SimLog.log(levelOf(priority), tag, message, t); }
    private static void internalLog(SimLog.Level level, String tag, String message) { SimLog.log(level, tag, message, null); }
    /** Called with the throwable when the SDK's ThreadPool reports an exception it swallowed (an Error in an OpMode thread). */
    public static volatile java.util.function.Consumer<Throwable> threadPoolErrorHook;
    private static void internalLog(SimLog.Level level, String tag, Throwable t, String message) {
        SimLog.log(level, tag, message, t);
        java.util.function.Consumer<Throwable> hook = threadPoolErrorHook;
        if (hook != null && t != null && message != null && message.startsWith("exception thrown in thread pool")) {
            try { hook.accept(t); } catch (RuntimeException ignored) {}
        }
    }
    private static SimLog.Level levelOf(int priority) {
        switch (priority) {
            case 2: return SimLog.Level.VERBOSE;
            case 3: return SimLog.Level.DEBUG;
            case 4: return SimLog.Level.INFO;
            case 5: return SimLog.Level.WARN;
            default: return SimLog.Level.ERROR;
        }
    }

    public static void logExceptionHeader(Exception e, String format, Object... args) { logExceptionHeader(TAG, e, format, args); }
    public static void logExceptionHeader(String tag, Exception e, String format, Object... args) {
        String message = fmt(format, args);
        StackTraceElement top = getStackTop(e);
        ee(tag, "exception %s(%s): %s [%s]", e.getClass().getSimpleName(), e.getMessage(), message, top == null ? "" : top.toString());
    }
    private static StackTraceElement getStackTop(Exception e) {
        StackTraceElement[] frames = e.getStackTrace();
        return frames != null && frames.length > 0 ? frames[0] : null;
    }
    public static void logStacktrace(Throwable e) { logStackTrace(e); }
    public static void logStackTrace(Throwable e) { logStackTrace(TAG, e); }
    public static void logStackTrace(Thread thread, String format, Object... args) {
        String message = fmt(format, args);
        ee(TAG, "thread id=%d tid=%d name=\"%s\" %s", thread.getId(), thread.getId(), thread.getName(), message);
        logStackFrames(thread.getStackTrace());
    }
    public static void logStackTrace(Thread thread, StackTraceElement[] stackTrace) {
        ee(TAG, "thread id=%d tid=%d name=\"%s\"", thread.getId(), thread.getId(), thread.getName());
        logStackFrames(stackTrace);
    }
    public static void logStackTrace(String tag, Throwable e) { SimLog.log(SimLog.Level.ERROR, tag, e.toString(), e); }
    private static void logStackFrames(StackTraceElement[] stackTrace) {
        for (StackTraceElement frame : stackTrace) ee(TAG, "    at %s", frame.toString());
    }

    public static void logAndThrow(String errMsg) throws RobotCoreException { e(errMsg); throw new RobotCoreException(errMsg); }

    // ---- global error / warning messages (shown on the Driver Station) ----
    public static boolean setGlobalErrorMsg(String message) {
        synchronized (globalErrorLock) {
            if (globalErrorMessage.isEmpty()) { globalErrorMessage = message == null ? "" : message; return true; }
            return false;
        }
    }
    public static void setGlobalErrorMsg(String format, Object... args) { setGlobalErrorMsg(fmt(format, args)); }
    public static void addGlobalWarningMessage(String message) {
        synchronized (globalWarningLock) {
            if (message == null || message.isEmpty()) return;
            if (globalWarningMessage.isEmpty()) globalWarningMessage = message;
            else if (!globalWarningMessage.contains(message)) globalWarningMessage = globalWarningMessage + "; " + message;
        }
        ww(TAG, "global warning: %s", message);
    }
    public static void addGlobalWarningMessage(String format, Object... args) { addGlobalWarningMessage(fmt(format, args)); }
    public static void registerGlobalWarningSource(GlobalWarningSource source) { synchronized (globalWarningLock) { globalWarningSources.put(source, 1); } }
    public static void unregisterGlobalWarningSource(GlobalWarningSource source) { synchronized (globalWarningLock) { globalWarningSources.remove(source); } }
    public static void setGlobalErrorMsg(RobotCoreException e, String message) { setGlobalErrorMsg(message + ": " + e.getMessage()); }
    public static void setGlobalErrorMsgAndThrow(RobotCoreException e, String message) throws RobotCoreException { setGlobalErrorMsg(e, message); throw e; }
    public static void setGlobalErrorMsg(RuntimeException e, String message) { setGlobalErrorMsg(String.format("%s: %s: %s", message, e.getClass().getSimpleName(), e.getMessage())); }
    public static void setGlobalErrorMsgAndThrow(RuntimeException e, String message) throws RobotCoreException { setGlobalErrorMsg(e, message); throw e; }
    public static String getGlobalErrorMsg() { synchronized (globalErrorLock) { return globalErrorMessage; } }
    public static void setGlobalErrorMsgSticky(boolean sticky) { globalErrorMsgSticky = sticky; }
    public static GlobalWarningMessage getGlobalWarningMessage() {
        List<String> messages = new ArrayList<>();
        synchronized (globalWarningLock) {
            if (!globalWarningMessage.isEmpty()) messages.add(globalWarningMessage);
            for (GlobalWarningSource source : globalWarningSources.keySet()) {
                String s = source.getGlobalWarning();
                if (s != null && !s.isEmpty()) messages.add(s);
            }
        }
        return new GlobalWarningMessage(combineGlobalWarnings(messages), globalWarningMsgSticky);
    }
    public static void setGlobalWarningMsgSticky(boolean sticky) { globalWarningMsgSticky = sticky; }
    public static String combineGlobalWarnings(List<String> warnings) {
        StringBuilder sb = new StringBuilder();
        for (String w : warnings) { if (w == null || w.isEmpty()) continue; if (sb.length() > 0) sb.append("; "); sb.append(w); }
        return sb.toString();
    }
    public static boolean hasGlobalErrorMsg() { return !getGlobalErrorMsg().isEmpty(); }
    public static boolean hasGlobalWarningMsg() { return !getGlobalWarningMessage().message.isEmpty(); }
    public static void clearGlobalErrorMsg() { synchronized (globalErrorLock) { if (!globalErrorMsgSticky) globalErrorMessage = ""; } }
    public static void forceClearGlobalErrorMsg() { synchronized (globalErrorLock) { globalErrorMessage = ""; } }
    public static void clearGlobalWarningMsg() {
        synchronized (globalWarningLock) {
            if (!globalWarningMsgSticky) globalWarningMessage = "";
            for (GlobalWarningSource source : globalWarningSources.keySet()) source.clearGlobalWarning();
        }
    }

    public static class GlobalWarningMessage {
        public final String message;
        public final boolean sticky;
        public GlobalWarningMessage(String message, boolean sticky) { this.message = message; this.sticky = sticky; }
    }

    // ---- logcat-to-disk and match logging: not applicable on the desktop ----
    public static void onApplicationStart() {}
    protected static synchronized void writeLogcatToDisk(Context context, int kbSize) {}
    public static void startMatchLogging(Context context, String eventName, int matchNumber) throws RobotCoreException {
        matchLogFilename = getMatchLogFilename(context, eventName, matchNumber);
        matchStartTime = Calendar.getInstance();
        ii(TAG, "Match logging started: %s", matchLogFilename);
    }
    public static synchronized void stopMatchLogging() { matchLogFilename = null; }
    public static String getLogFilename() { return new File(logDir(), "robotControllerLog.txt").getAbsolutePath(); }
    public static String getLogFilename(Context context) { return getLogFilename(); }
    protected static void pruneMatchLogsIfNecessary() {}
    public static String getMatchLogFilename(Context context, String eventName, int matchNumber) {
        return new File(logDir(), String.format("Match-%d-%s.txt", matchNumber, eventName == null ? "" : eventName.replaceAll("[^A-Za-z0-9]", "_"))).getAbsolutePath();
    }
    public static List<File> getExtantLogFiles(Context context) { return new ArrayList<>(); }
    public static synchronized void cancelWriteLogcatToDisk() {}
    public static void logAppInfo() { ii(TAG, "FTCSim desktop robot controller"); }
    public static void logDeviceInfo() { ii(TAG, "Device: FTCSim (%s %s, Java %s)", System.getProperty("os.name"), System.getProperty("os.arch"), System.getProperty("java.version")); }
    public static void logBytes(String tag, String caption, byte[] data, int cb) { logBytes(tag, caption, data, 0, cb); }
    public static void logBytes(String tag, String caption, byte[] data, int ibStart, int cb) {
        StringBuilder sb = new StringBuilder();
        for (int i = ibStart; i < ibStart + cb && i < data.length; i++) sb.append(String.format("%02x ", data[i] & 0xff));
        vv(tag, "%s: %s", caption, sb.toString().trim());
    }
    private static File logDir() {
        File f = new File(System.getProperty("ftcsim.data", System.getProperty("java.io.tmpdir")), "logs");
        f.mkdirs();
        return f;
    }
}
