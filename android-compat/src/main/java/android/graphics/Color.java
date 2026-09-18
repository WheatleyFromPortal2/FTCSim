package android.graphics;

/** Desktop implementation of android.graphics.Color (ARGB int color utilities). */
public class Color {
    public static final int BLACK = 0xFF000000, DKGRAY = 0xFF444444, GRAY = 0xFF888888, LTGRAY = 0xFFCCCCCC,
        WHITE = 0xFFFFFFFF, RED = 0xFFFF0000, GREEN = 0xFF00FF00, BLUE = 0xFF0000FF, YELLOW = 0xFFFFFF00,
        CYAN = 0xFF00FFFF, MAGENTA = 0xFFFF00FF, TRANSPARENT = 0;

    public static int alpha(int color) { return color >>> 24; }
    public static int red(int color) { return (color >> 16) & 0xFF; }
    public static int green(int color) { return (color >> 8) & 0xFF; }
    public static int blue(int color) { return color & 0xFF; }
    public static int rgb(int red, int green, int blue) { return 0xFF000000 | (red << 16) | (green << 8) | blue; }
    public static int rgb(float red, float green, float blue) { return rgb(Math.round(red * 255f), Math.round(green * 255f), Math.round(blue * 255f)); }
    public static int argb(int alpha, int red, int green, int blue) { return (alpha << 24) | (red << 16) | (green << 8) | blue; }
    public static int argb(float alpha, float red, float green, float blue) { return argb(Math.round(alpha * 255f), Math.round(red * 255f), Math.round(green * 255f), Math.round(blue * 255f)); }
    public static float luminance(int color) {
        double r = red(color) / 255.0, g = green(color) / 255.0, b = blue(color) / 255.0;
        r = r < 0.03928 ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4);
        g = g < 0.03928 ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4);
        b = b < 0.03928 ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4);
        return (float) (0.2126 * r + 0.7152 * g + 0.0722 * b);
    }
    public static int parseColor(String colorString) {
        if (colorString.charAt(0) == '#') {
            long color = Long.parseLong(colorString.substring(1), 16);
            if (colorString.length() == 7) color |= 0x00000000FF000000L;
            else if (colorString.length() != 9) throw new IllegalArgumentException("Unknown color");
            return (int) color;
        }
        switch (colorString.toLowerCase()) {
            case "black": return BLACK; case "darkgray": return DKGRAY; case "gray": return GRAY; case "lightgray": return LTGRAY;
            case "white": return WHITE; case "red": return RED; case "green": return GREEN; case "blue": return BLUE;
            case "yellow": return YELLOW; case "cyan": return CYAN; case "magenta": return MAGENTA;
            case "aqua": return 0xFF00FFFF; case "fuchsia": return 0xFFFF00FF; case "darkgrey": return DKGRAY;
            case "grey": return GRAY; case "lightgrey": return LTGRAY; case "lime": return 0xFF00FF00;
            case "maroon": return 0xFF800000; case "navy": return 0xFF000080; case "olive": return 0xFF808000;
            case "purple": return 0xFF800080; case "silver": return 0xFFC0C0C0; case "teal": return 0xFF008080;
            default: throw new IllegalArgumentException("Unknown color");
        }
    }
    public static void RGBToHSV(int red, int green, int blue, float[] hsv) {
        if (hsv.length < 3) throw new RuntimeException("3 components required for hsv");
        float r = red / 255f, g = green / 255f, b = blue / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float h;
        if (delta == 0) h = 0;
        else if (max == r) h = 60f * (((g - b) / delta) % 6f);
        else if (max == g) h = 60f * (((b - r) / delta) + 2f);
        else h = 60f * (((r - g) / delta) + 4f);
        if (h < 0) h += 360f;
        hsv[0] = h; hsv[1] = max == 0 ? 0 : delta / max; hsv[2] = max;
    }
    public static void colorToHSV(int color, float[] hsv) { RGBToHSV(red(color), green(color), blue(color), hsv); }
    public static int HSVToColor(float[] hsv) { return HSVToColor(0xFF, hsv); }
    public static int HSVToColor(int alpha, float[] hsv) {
        if (hsv.length < 3) throw new RuntimeException("3 components required for hsv");
        float h = hsv[0], s = hsv[1], v = hsv[2];
        float c = v * s, x = c * (1 - Math.abs((h / 60f) % 2 - 1)), m = v - c;
        float r, g, b;
        if (h < 60) { r = c; g = x; b = 0; } else if (h < 120) { r = x; g = c; b = 0; } else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; } else if (h < 300) { r = x; g = 0; b = c; } else { r = c; g = 0; b = x; }
        return argb(alpha, Math.round((r + m) * 255), Math.round((g + m) * 255), Math.round((b + m) * 255));
    }
    public static int toArgb(long color) { return (int) (color >> 32); }
    public static float alpha(long color) { return alpha(toArgb(color)) / 255f; }
}
