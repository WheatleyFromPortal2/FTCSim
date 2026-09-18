package android.graphics;

public class RectF {
    public float left, top, right, bottom;
    public RectF() {}
    public RectF(float left, float top, float right, float bottom) { this.left = left; this.top = top; this.right = right; this.bottom = bottom; }
    public RectF(RectF r) { this(r.left, r.top, r.right, r.bottom); }
    public RectF(Rect r) { this(r.left, r.top, r.right, r.bottom); }
    public final float width() { return right - left; }
    public final float height() { return bottom - top; }
    public final float centerX() { return (left + right) * 0.5f; }
    public final float centerY() { return (top + bottom) * 0.5f; }
    public final boolean isEmpty() { return left >= right || top >= bottom; }
    public void set(float l, float t, float r, float b) { left = l; top = t; right = r; bottom = b; }
    public void set(RectF src) { set(src.left, src.top, src.right, src.bottom); }
    public void offset(float dx, float dy) { left += dx; top += dy; right += dx; bottom += dy; }
    public void inset(float dx, float dy) { left += dx; top += dy; right -= dx; bottom -= dy; }
    public boolean contains(float x, float y) { return left < right && top < bottom && x >= left && x < right && y >= top && y < bottom; }
    @Override public String toString() { return "RectF(" + left + ", " + top + ", " + right + ", " + bottom + ")"; }
}
