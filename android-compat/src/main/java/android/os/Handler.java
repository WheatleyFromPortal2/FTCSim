package android.os;

/** Desktop stand-in for android.os.Handler that runs work on a shared timer thread. */
public class Handler {
    private static final java.util.Timer TIMER = new java.util.Timer("ftcsim-android-handler", true);
    public interface Callback { boolean handleMessage(Object msg); }
    public Handler() {}
    public Handler(Looper looper) {}
    public Handler(Looper looper, Callback cb) {}
    public boolean post(Runnable r) { if (r != null) r.run(); return true; }
    public boolean postDelayed(Runnable r, long delayMillis) {
        if (r == null) return false;
        TIMER.schedule(new java.util.TimerTask() { @Override public void run() { r.run(); } }, Math.max(0, delayMillis));
        return true;
    }
    public void removeCallbacks(Runnable r) {}
    public void removeCallbacksAndMessages(Object token) {}
    public Looper getLooper() { return Looper.getMainLooper(); }
}
