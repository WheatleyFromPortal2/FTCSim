package android.text;

public final class TextUtils {
    private TextUtils() {}
    public static boolean isEmpty(CharSequence s) { return s == null || s.length() == 0; }
    public static String join(CharSequence delimiter, Iterable<?> tokens) {
        StringBuilder sb = new StringBuilder(); boolean first = true;
        for (Object t : tokens) { if (!first) sb.append(delimiter); first = false; sb.append(t); }
        return sb.toString();
    }
    public static String join(CharSequence delimiter, Object[] tokens) { return join(delimiter, java.util.Arrays.asList(tokens)); }
    public static boolean equals(CharSequence a, CharSequence b) { return a == null ? b == null : b != null && a.toString().contentEquals(b); }
    public static boolean isDigitsOnly(CharSequence s) { for (int i = 0; i < s.length(); i++) if (!Character.isDigit(s.charAt(i))) return false; return true; }
}
