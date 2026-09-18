package android.graphics;

/** Minimal desktop stand-in for android.graphics.Paint (state holder only). */
public class Paint {
    public enum Style { FILL, STROKE, FILL_AND_STROKE }
    public enum Align { LEFT, CENTER, RIGHT }
    public enum Cap { BUTT, ROUND, SQUARE }
    public enum Join { MITER, ROUND, BEVEL }
    public static final int ANTI_ALIAS_FLAG = 1;
    private int color = 0xFF000000; private float strokeWidth = 0; private Style style = Style.FILL;
    private float textSize = 12; private Align align = Align.LEFT; private boolean antiAlias;
    public Paint() {}
    public Paint(int flags) { antiAlias = (flags & ANTI_ALIAS_FLAG) != 0; }
    public Paint(Paint p) { color = p.color; strokeWidth = p.strokeWidth; style = p.style; textSize = p.textSize; align = p.align; }
    public void setColor(int c) { color = c; }
    public int getColor() { return color; }
    public void setARGB(int a, int r, int g, int b) { color = Color.argb(a, r, g, b); }
    public void setAlpha(int a) { color = (color & 0x00FFFFFF) | (a << 24); }
    public int getAlpha() { return color >>> 24; }
    public void setStrokeWidth(float w) { strokeWidth = w; }
    public float getStrokeWidth() { return strokeWidth; }
    public void setStyle(Style s) { style = s; }
    public Style getStyle() { return style; }
    public void setTextSize(float s) { textSize = s; }
    public float getTextSize() { return textSize; }
    public void setTextAlign(Align a) { align = a; }
    public Align getTextAlign() { return align; }
    public void setAntiAlias(boolean aa) { antiAlias = aa; }
    public boolean isAntiAlias() { return antiAlias; }
    public void setStrokeCap(Cap c) {}
    public void setStrokeJoin(Join j) {}
    public void setTypeface(Object t) {}
    public float measureText(String text) { return text == null ? 0 : text.length() * textSize * 0.6f; }
    public void setShadowLayer(float r, float dx, float dy, int c) {}
    public void setDither(boolean d) {}
    public void setFilterBitmap(boolean f) {}
}
