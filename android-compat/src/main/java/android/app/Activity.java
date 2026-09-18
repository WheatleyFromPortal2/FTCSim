package android.app;

import android.content.Context;
import android.view.View;

/** Desktop stand-in for android.app.Activity. All UI calls are harmless no-ops. */
public class Activity extends Context {
    private final View contentView = new View(this);
    public View findViewById(int id) { return contentView; }
    public View getWindow() { return contentView; }
    public void runOnUiThread(Runnable r) { if (r != null) r.run(); }
    public void setContentView(int layoutResID) {}
    public void finish() {}
    public boolean isFinishing() { return false; }
    public boolean isDestroyed() { return false; }
    public Object getIntent() { return null; }
    public void setTitle(CharSequence title) {}
    public void setRequestedOrientation(int o) {}
}
