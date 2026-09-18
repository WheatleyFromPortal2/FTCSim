package android.graphics;

/** Minimal desktop stand-in for android.graphics.Bitmap backed by an int[] of ARGB pixels. */
public class Bitmap {
    public enum Config { ALPHA_8, RGB_565, ARGB_4444, ARGB_8888, RGBA_F16, HARDWARE }
    public enum CompressFormat { JPEG, PNG, WEBP }
    private final int width, height; private final int[] pixels; private final Config config;
    private Bitmap(int w, int h, Config c) { width = w; height = h; config = c; pixels = new int[Math.max(0, w * h)]; }
    public static Bitmap createBitmap(int width, int height, Config config) { return new Bitmap(width, height, config); }
    public static Bitmap createBitmap(Bitmap src) { Bitmap b = new Bitmap(src.width, src.height, src.config); System.arraycopy(src.pixels, 0, b.pixels, 0, src.pixels.length); return b; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Config getConfig() { return config; }
    public int getPixel(int x, int y) { return pixels[y * width + x]; }
    public void setPixel(int x, int y, int color) { pixels[y * width + x] = color; }
    public void getPixels(int[] dst, int offset, int stride, int x, int y, int w, int h) { for (int r = 0; r < h; r++) System.arraycopy(pixels, (y + r) * width + x, dst, offset + r * stride, w); }
    public void setPixels(int[] src, int offset, int stride, int x, int y, int w, int h) { for (int r = 0; r < h; r++) System.arraycopy(src, offset + r * stride, pixels, (y + r) * width + x, w); }
    public boolean isRecycled() { return false; }
    public void recycle() {}
    public boolean compress(CompressFormat format, int quality, java.io.OutputStream stream) { return false; }
    public int getByteCount() { return pixels.length * 4; }
    public void copyPixelsToBuffer(java.nio.Buffer dst) {}
    public void copyPixelsFromBuffer(java.nio.Buffer src) {}
}
