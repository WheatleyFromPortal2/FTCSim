package android.view;

import android.content.Context;

/** Desktop stand-in for android.view.View (a no-op widget). */
public class View {
    public static final int VISIBLE = 0, INVISIBLE = 4, GONE = 8;
    private final Context context;
    private int backgroundColor = 0;
    private int visibility = VISIBLE;
    public View(Context context) { this.context = context; }
    public Context getContext() { return context; }
    public void setBackgroundColor(int color) { backgroundColor = color; }
    public int getBackgroundColor() { return backgroundColor; }
    public boolean post(Runnable r) { if (r != null) r.run(); return true; }
    public boolean postDelayed(Runnable r, long delayMillis) { if (r != null) r.run(); return true; }
    public void setVisibility(int v) { visibility = v; }
    public int getVisibility() { return visibility; }
    public void invalidate() {}
    public void setEnabled(boolean e) {}
    public void setOnClickListener(Object l) {}
    public int getWidth() { return 0; }
    public int getHeight() { return 0; }
    public View findViewById(int id) { return this; }
    public void setAlpha(float a) {}
    public void setKeepScreenOn(boolean b) {}
}
