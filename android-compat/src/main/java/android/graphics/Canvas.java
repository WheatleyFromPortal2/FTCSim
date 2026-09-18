package android.graphics;

/** Minimal desktop stand-in for android.graphics.Canvas (all drawing is a no-op). */
public class Canvas {
    private int width, height;
    public Canvas() {}
    public Canvas(Bitmap b) { if (b != null) { width = b.getWidth(); height = b.getHeight(); } }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public void drawLine(float x1, float y1, float x2, float y2, Paint p) {}
    public void drawLines(float[] pts, Paint p) {}
    public void drawCircle(float cx, float cy, float r, Paint p) {}
    public void drawRect(float l, float t, float r, float b, Paint p) {}
    public void drawRect(Rect r, Paint p) {}
    public void drawRect(RectF r, Paint p) {}
    public void drawText(String text, float x, float y, Paint p) {}
    public void drawPoint(float x, float y, Paint p) {}
    public void drawPoints(float[] pts, Paint p) {}
    public void drawColor(int color) {}
    public void drawBitmap(Bitmap b, float left, float top, Paint p) {}
    public void drawBitmap(Bitmap b, Rect src, Rect dst, Paint p) {}
    public void drawOval(RectF r, Paint p) {}
    public void drawPath(Object path, Paint p) {}
    public int save() { return 0; }
    public void restore() {}
    public void translate(float dx, float dy) {}
    public void rotate(float deg) {}
    public void rotate(float deg, float px, float py) {}
    public void scale(float sx, float sy) {}
    public boolean clipRect(Rect r) { return true; }
}
