package ftcsim.log;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Redirects System.out / System.err into the simulator log (line buffered),
 * the way logcat captures an app's stdout on the robot controller.
 */
public final class StdoutCapture {
    private static PrintStream originalOut = System.out;
    private static PrintStream originalErr = System.err;
    private static boolean installed;

    private StdoutCapture() {}

    public static PrintStream originalOut() { return originalOut; }

    public static synchronized void install() {
        if (installed) return;
        installed = true;
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(new LineSink(SimLog.Level.INFO, "System.out"), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new LineSink(SimLog.Level.ERROR, "System.err"), true, StandardCharsets.UTF_8));
    }

    private static final class LineSink extends OutputStream {
        private final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        private final SimLog.Level level;
        private final String tag;
        LineSink(SimLog.Level level, String tag) { this.level = level; this.tag = tag; }
        @Override public synchronized void write(int b) {
            if (b == '\n') flushLine(); else if (b != '\r') buf.write(b);
        }
        @Override public synchronized void write(byte[] b, int off, int len) {
            for (int i = off; i < off + len; i++) write(b[i]);
        }
        @Override public synchronized void flush() { }
        private void flushLine() {
            String line = buf.toString(StandardCharsets.UTF_8);
            buf.reset();
            SimLog.log(level, tag, line, null);
        }
    }
}
