package android.graphics;

public class Rect {
    public int left, top, right, bottom;
    public Rect() {}
    public Rect(int left, int top, int right, int bottom) { this.left = left; this.top = top; this.right = right; this.bottom = bottom; }
    public Rect(Rect r) { this(r.left, r.top, r.right, r.bottom); }
    public final int width() { return right - left; }
    public final int height() { return bottom - top; }
    public final int centerX() { return (left + right) >> 1; }
    public final int centerY() { return (top + bottom) >> 1; }
    public final boolean isEmpty() { return left >= right || top >= bottom; }
    public void set(int l, int t, int r, int b) { left = l; top = t; right = r; bottom = b; }
    public void set(Rect src) { set(src.left, src.top, src.right, src.bottom); }
    public void setEmpty() { left = right = top = bottom = 0; }
    public void offset(int dx, int dy) { left += dx; top += dy; right += dx; bottom += dy; }
    public void offsetTo(int newLeft, int newTop) { right += newLeft - left; bottom += newTop - top; left = newLeft; top = newTop; }
    public void inset(int dx, int dy) { left += dx; top += dy; right -= dx; bottom -= dy; }
    public boolean contains(int x, int y) { return left < right && top < bottom && x >= left && x < right && y >= top && y < bottom; }
    public boolean contains(Rect r) { return left <= r.left && top <= r.top && right >= r.right && bottom >= r.bottom; }
    public boolean intersect(Rect r) {
        if (left < r.right && r.left < right && top < r.bottom && r.top < bottom) {
            left = Math.max(left, r.left); top = Math.max(top, r.top); right = Math.min(right, r.right); bottom = Math.min(bottom, r.bottom); return true;
        }
        return false;
    }
    public boolean intersects(int l, int t, int r, int b) { return left < r && l < right && top < b && t < bottom; }
    public void union(Rect r) { left = Math.min(left, r.left); top = Math.min(top, r.top); right = Math.max(right, r.right); bottom = Math.max(bottom, r.bottom); }
    @Override public boolean equals(Object o) { return o instanceof Rect && ((Rect) o).left == left && ((Rect) o).top == top && ((Rect) o).right == right && ((Rect) o).bottom == bottom; }
    @Override public int hashCode() { return ((left * 31 + top) * 31 + right) * 31 + bottom; }
    @Override public String toString() { return "Rect(" + left + ", " + top + " - " + right + ", " + bottom + ")"; }
    public String toShortString() { return "[" + left + "," + top + "][" + right + "," + bottom + "]"; }
}
