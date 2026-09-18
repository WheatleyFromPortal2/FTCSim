package android.util;

/** Desktop implementation of android.util.Size. */
public final class Size {
    private final int width, height;
    public Size(int width, int height) { this.width = width; this.height = height; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    @Override public boolean equals(Object o) { return o instanceof Size && ((Size) o).width == width && ((Size) o).height == height; }
    @Override public int hashCode() { return height ^ ((width << 16) | (width >>> 16)); }
    @Override public String toString() { return width + "x" + height; }
    public static Size parseSize(String s) {
        int i = s.indexOf('x'); if (i < 0) i = s.indexOf('*');
        if (i < 0) throw new NumberFormatException("Invalid Size: \"" + s + "\"");
        return new Size(Integer.parseInt(s.substring(0, i)), Integer.parseInt(s.substring(i + 1)));
    }
}
