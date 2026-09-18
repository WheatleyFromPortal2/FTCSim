package org.firstinspires.ftc.robotcore.internal.system;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import com.qualcomm.robotcore.util.RobotLog;
import org.firstinspires.ftc.robotcore.external.Predicate;
import org.firstinspires.ftc.robotcore.internal.ui.UILocation;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * FTCSim replacement for the SDK's AppUtil. Provides the application-context
 * plumbing that SDK hardware classes rely on (e.g. resource strings for
 * getDeviceName()) without an Android runtime. UI related calls are logged.
 */
public class AppUtil {
    public static final String TAG = "AppUtil";
    public static final File ROOT_FOLDER = dataDir();
    public static final File FIRST_FOLDER = new File(ROOT_FOLDER, "FIRST");
    public static final File LOG_FOLDER = ROOT_FOLDER;
    public static final File MATCH_LOG_FOLDER = new File(FIRST_FOLDER, "matchlogs");
    public static final int MAX_MATCH_LOGS_TO_KEEP = 5;
    public static final File CONFIG_FILES_DIR = FIRST_FOLDER;
    public static final File BLOCK_OPMODES_DIR = new File(FIRST_FOLDER, "blocks");
    public static final String BLOCKS_BLK_EXT = ".blk";
    public static final String BLOCKS_JS_EXT = ".js";
    public static final File BLOCKS_SOUNDS_DIR = new File(BLOCK_OPMODES_DIR, "sounds");
    public static final File ROBOT_SETTINGS = new File(FIRST_FOLDER, "settings");
    public static final File ROBOT_DATA_DIR = new File(FIRST_FOLDER, "data");
    public static final File UPDATES_DIR = new File(FIRST_FOLDER, "updates");
    public static final File RC_APP_UPDATE_DIR = new File(UPDATES_DIR, "Robot Controller");
    public static final File LYNX_FIRMWARE_UPDATE_DIR = new File(UPDATES_DIR, "Expansion Hub Firmware");
    public static final File OTA_UPDATE_DIR = new File(UPDATES_DIR, "Control Hub OS");
    public static final File SOUNDS_DIR = new File(FIRST_FOLDER, "sounds");
    public static final File SOUNDS_CACHE = new File(SOUNDS_DIR, "cache");
    public static final File WEBCAM_CALIBRATIONS_DIR = new File(FIRST_FOLDER, "webcamcalibrations");
    public static final String PROGRESS_NAMESPACE = "progress";
    public static final String SHOW_PROGRESS_MSG = "showProgress";
    public static final String DISMISS_PROGRESS_MSG = "dismissProgress";

    private static final AppUtil instance = new AppUtil();
    private static Application application = new Application();
    private static Activity activity = new Activity();

    private static File dataDir() {
        String d = System.getProperty("ftcsim.data");
        File f = d != null ? new File(d) : new File(System.getProperty("user.home"), ".ftcsim");
        f.mkdirs();
        return f;
    }

    public static AppUtil getInstance() { return instance; }
    public static Application getDefContext() { return application; }
    public static void onApplicationStart(Application app) { application = app; }

    // ---- files ----
    public File getRelativePath(File root, File child) { return root.toPath().relativize(child.toPath()).toFile(); }
    public void ensureDirectoryExists(File dir) { ensureDirectoryExists(dir, true); }
    public void ensureDirectoryExists(File dir, boolean removeOthers) { dir.mkdirs(); }
    public void deleteChildren(File dir) { File[] c = dir.listFiles(); if (c != null) for (File f : c) delete(f); }
    public void delete(File f) { if (f.isDirectory()) deleteChildren(f); f.delete(); }
    public List<File> filesUnder(File root) { return filesUnder(root, (Predicate<File>) f -> true); }
    public List<File> filesUnder(File root, Predicate<File> predicate) {
        List<File> out = new ArrayList<>();
        File[] children = root.listFiles();
        if (children != null) for (File f : children) { if (f.isDirectory()) out.addAll(filesUnder(f, predicate)); else if (predicate.test(f)) out.add(f); }
        return out;
    }
    public List<File> filesUnder(File root, String extension) { return filesUnder(root, (Predicate<File>) f -> f.getName().endsWith(extension)); }
    public List<File> filesIn(File root) { return filesIn(root, (Predicate<File>) f -> true); }
    public List<File> filesIn(File root, Predicate<File> predicate) {
        List<File> out = new ArrayList<>();
        File[] children = root.listFiles();
        if (children != null) for (File f : children) if (predicate.test(f)) out.add(f);
        return out;
    }
    public List<File> filesIn(File root, String extension) { return filesIn(root, (Predicate<File>) f -> f.getName().endsWith(extension)); }
    public File getSettingsFile(String name) { ROBOT_SETTINGS.mkdirs(); return new File(ROBOT_SETTINGS, name); }
    public void copyFile(File src, File dst) throws IOException { Files.copy(src.toPath(), dst.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
    public void copyStream(InputStream in, File dst) throws IOException { try (OutputStream out = new FileOutputStream(dst)) { copyStream(in, out); } }
    public void copyStream(File src, OutputStream out) throws IOException { try (InputStream in = new FileInputStream(src)) { copyStream(in, out); } }
    public void copyStream(InputStream in, OutputStream out) throws IOException { byte[] b = new byte[8192]; int n; while ((n = in.read(b)) > 0) out.write(b, 0, n); }
    public File createTempFile(String prefix, String suffix, File dir) throws IOException { return File.createTempFile(prefix, suffix, dir); }
    public File createTempDirectory(String prefix, String suffix, File dir) throws IOException { return Files.createTempDirectory(dir == null ? null : dir.toPath(), prefix).toFile(); }
    public String getUsbFileSystemRoot() { return null; }
    public String getNonNullUsbFileSystemRoot() { return ""; }
    public static String computeMd5(File file) throws java.security.NoSuchAlgorithmException, IOException {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
        md.update(Files.readAllBytes(file.toPath()));
        StringBuilder sb = new StringBuilder(); for (byte b : md.digest()) sb.append(String.format("%02x", b)); return sb.toString();
    }

    // ---- application ----
    public void restartApp(int status) { RobotLog.ww(TAG, "restartApp(%d) requested; ignored by FTCSim", status); }
    public void exitApplication(int status) { RobotLog.ww(TAG, "exitApplication(%d) requested; ignored by FTCSim", status); }
    public void exitApplication() { exitApplication(0); }
    public Application getApplication() { return application; }
    public String getApplicationId() { return "com.qualcomm.ftcrobotcontroller"; }
    public boolean isRobotController() { return true; }
    public boolean isDriverStation() { return false; }
    public String getAppName() { return "FTCSim Robot Controller"; }
    public String getRemoteAppName() { return "FTCSim Driver Station"; }
    public static String getSdkVersionString(int major, int minor, int point) { return major + "." + minor + "." + point; }
    public static String getSdkVersionString() { return System.getProperty("ftcsim.sdkVersion", "11.0.0"); }
    public static int getColor(int id) { return 0xFF000000; }

    // ---- UI (no-ops / inline execution) ----
    public void synchronousRunOnUiThread(Runnable r) { r.run(); }
    public void synchronousRunOnUiThread(Activity a, Runnable r) { r.run(); }
    public void runOnUiThread(Runnable r) { r.run(); }
    public void runOnUiThread(Activity a, Runnable r) { r.run(); }
    public void showWaitCursor(String message, Runnable runnable) { runnable.run(); }
    public void showWaitCursor(String message, Runnable runnable, Runnable runPostOnUIThread) { runnable.run(); if (runPostOnUIThread != null) runPostOnUIThread.run(); }
    public void showProgress(UILocation uiLocation, String message, double fractionComplete) { RobotLog.ii(TAG, "progress: %s (%.0f%%)", message, fractionComplete * 100); }
    public void showProgress(UILocation uiLocation, String message, double fractionComplete, int max) { showProgress(uiLocation, message, fractionComplete); }
    public void dismissProgress(UILocation uiLocation) {}
    public DialogContext showAlertDialog(UILocation uiLocation, String title, String message) { RobotLog.ii(TAG, "alert: %s: %s", title, message); return new DialogContext(); }
    public DialogContext showDialog(DialogParams params) { RobotLog.ii(TAG, "dialog: %s: %s", params.title, params.message); return new DialogContext(); }
    public void dismissAllDialogs(UILocation uiLocation) {}
    public void showToast(UILocation uiLocation, String msg) { RobotLog.ii(TAG, "toast: %s", msg); }
    public void showToast(UILocation uiLocation, String msg, int duration) { showToast(uiLocation, msg); }
    public void showToast(UILocation uiLocation, Context context, String msg) { showToast(uiLocation, msg); }
    public void showToast(UILocation uiLocation, Activity activity, Context context, String msg) { showToast(uiLocation, msg); }
    public void showToast(UILocation uiLocation, Activity activity, Context context, String msg, int duration) { showToast(uiLocation, msg); }
    public Activity getActivity() { return activity; }
    public Context getModalContext() { return activity; }
    public Activity getRootActivity() { return activity; }
    public void setBluetoothEnabled(boolean enabled) {}

    // ---- time ----
    public long getWallClockTime() { return System.currentTimeMillis(); }
    public void setWallClockTime(long millis) {}
    public void setWallClockIfCurrentlyInsane(long millis, String reason) {}
    public void setTimeZone(String timeZone) {}
    public boolean isSaneWallClockTime(long millis) { return millis > 1_500_000_000_000L; }

    // ---- diagnostics ----
    public String findCaller(String message, int frameIndex) {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        int i = Math.min(st.length - 1, frameIndex + 2);
        return message + " [" + st[i] + "]";
    }
    public RuntimeException unreachable() { return unreachable("internal error: unreachable"); }
    public RuntimeException unreachable(Throwable t) { return new RuntimeException("internal error: unreachable", t); }
    public RuntimeException unreachable(String msg) { return new RuntimeException(msg); }
    public RuntimeException unreachable(String msg, Throwable t) { return new RuntimeException(msg, t); }
    public RuntimeException failFast(String tag, String format, Object... args) { String m = String.format(format, args); RobotLog.ee(tag, m); return new RuntimeException(m); }
    public RuntimeException failFast(String tag, String message) { RobotLog.ee(tag, message); return new RuntimeException(message); }
    public RuntimeException failFast(String tag, Throwable t, String format, Object... args) { String m = String.format(format, args); RobotLog.ee(tag, t, m); return new RuntimeException(m, t); }
    public RuntimeException failFast(String tag, Throwable t, String message) { RobotLog.ee(tag, t, message); return new RuntimeException(message, t); }

    /** Stand-in for the SDK's dialog handle. */
    public static class DialogContext {
        public boolean isDismissed() { return true; }
        public void dismissDialog() {}
    }
    /** Stand-in for the SDK's dialog parameters. */
    public static class DialogParams {
        public UILocation uiLocation;
        public String title;
        public String message;
        public Activity activity;
        public DialogParams(UILocation uiLocation, String title, String message) { this.uiLocation = uiLocation; this.title = title; this.message = message; }
    }
    public interface UsbFileSystemRootListener { void onUsbFileSystemRootChanged(String usbFileSystemRoot); }
    public void addUsbfsListener(UsbFileSystemRootListener l) {}
    public void removeUsbfsListener(UsbFileSystemRootListener l) {}
}
