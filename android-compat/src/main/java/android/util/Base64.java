package android.util;

import java.nio.charset.StandardCharsets;

/** Desktop implementation of android.util.Base64 on top of java.util.Base64. */
public final class Base64 {
    public static final int DEFAULT = 0, NO_PADDING = 1, NO_WRAP = 2, CRLF = 4, URL_SAFE = 8, NO_CLOSE = 16;
    private Base64() {}
    public static String encodeToString(byte[] input, int flags) { return new String(encode(input, flags), StandardCharsets.US_ASCII); }
    public static String encodeToString(byte[] input, int offset, int len, int flags) {
        byte[] b = new byte[len]; System.arraycopy(input, offset, b, 0, len); return encodeToString(b, flags);
    }
    public static byte[] encode(byte[] input, int flags) {
        java.util.Base64.Encoder enc = (flags & URL_SAFE) != 0 ? java.util.Base64.getUrlEncoder() : java.util.Base64.getEncoder();
        if ((flags & NO_PADDING) != 0) enc = enc.withoutPadding();
        byte[] out = enc.encode(input);
        if ((flags & NO_WRAP) == 0 && out.length > 76) {
            StringBuilder sb = new StringBuilder();
            String s = new String(out, StandardCharsets.US_ASCII);
            String sep = (flags & CRLF) != 0 ? "\r\n" : "\n";
            for (int i = 0; i < s.length(); i += 76) { sb.append(s, i, Math.min(s.length(), i + 76)).append(sep); }
            out = sb.toString().getBytes(StandardCharsets.US_ASCII);
        }
        return out;
    }
    public static byte[] decode(String str, int flags) { return decode(str.getBytes(StandardCharsets.US_ASCII), flags); }
    public static byte[] decode(byte[] input, int flags) {
        String s = new String(input, StandardCharsets.US_ASCII).replaceAll("[\\r\\n]", "");
        java.util.Base64.Decoder dec = (flags & URL_SAFE) != 0 ? java.util.Base64.getUrlDecoder() : java.util.Base64.getMimeDecoder();
        return dec.decode(s);
    }
    public static byte[] decode(byte[] input, int offset, int len, int flags) {
        byte[] b = new byte[len]; System.arraycopy(input, offset, b, 0, len); return decode(b, flags);
    }
}
