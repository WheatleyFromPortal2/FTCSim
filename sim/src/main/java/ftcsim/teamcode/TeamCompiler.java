package ftcsim.teamcode;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Compiles the team's Java sources in-process with the JDK compiler. */
public final class TeamCompiler {
    public static final class Diagnostic {
        public final String kind, file, message; public final long line, column;
        Diagnostic(String kind, String file, long line, long column, String message) { this.kind = kind; this.file = file; this.line = line; this.column = column; this.message = message; }
        @Override public String toString() { return kind + " " + file + ":" + line + ": " + message; }
    }
    public static final class Result {
        public final boolean success; public final List<Diagnostic> diagnostics; public final int fileCount; public final long millis; public final String summary;
        Result(boolean success, List<Diagnostic> diagnostics, int fileCount, long millis, String summary) { this.success = success; this.diagnostics = diagnostics; this.fileCount = fileCount; this.millis = millis; this.summary = summary; }
    }

    private final File outputDir;
    private final String classpath;

    public TeamCompiler(File outputDir, String classpath) { this.outputDir = outputDir; this.classpath = classpath; }

    public Result compile(List<File> sources) {
        long t0 = System.currentTimeMillis();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return new Result(false, Collections.singletonList(new Diagnostic("ERROR", "", 0, 0, "No Java compiler available: run FTCSim with a JDK (not a JRE)")), 0, 0, "no compiler");
        }
        if (sources.isEmpty()) return new Result(true, Collections.emptyList(), 0, 0, "no sources");
        try {
            if (outputDir.exists()) deleteRecursively(outputDir);
            outputDir.mkdirs();
        } catch (RuntimeException e) { /* ignore */ }
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        StandardJavaFileManager fm = compiler.getStandardFileManager(collector, null, StandardCharsets.UTF_8);
        List<String> options = new ArrayList<>(Arrays.asList(
            "-d", outputDir.getAbsolutePath(),
            "-classpath", classpath,
            "-encoding", "UTF-8",
            "-proc:none",
            "-Xlint:none",
            "-g",
            "-parameters",
            "-implicit:none"
        ));
        Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromFiles(sources);
        JavaCompiler.CompilationTask task = compiler.getTask(null, fm, collector, options, null, units);
        boolean ok;
        try { ok = Boolean.TRUE.equals(task.call()); } catch (RuntimeException e) { ok = false; collector.report(null); }
        List<Diagnostic> diags = new ArrayList<>();
        int errors = 0;
        for (javax.tools.Diagnostic<? extends JavaFileObject> d : collector.getDiagnostics()) {
            if (d == null) continue;
            String file = d.getSource() == null ? "" : d.getSource().getName();
            String kind = d.getKind().name();
            if (d.getKind() == javax.tools.Diagnostic.Kind.ERROR) errors++;
            if (d.getKind() == javax.tools.Diagnostic.Kind.NOTE || d.getKind() == javax.tools.Diagnostic.Kind.OTHER) continue;
            diags.add(new Diagnostic(kind, file, d.getLineNumber(), d.getColumnNumber(), d.getMessage(Locale.getDefault())));
        }
        try { fm.close(); } catch (IOException ignored) {}
        long ms = System.currentTimeMillis() - t0;
        String summary = ok ? String.format("Compiled %d files in %d ms", sources.size(), ms) : String.format("Compilation failed: %d error(s)", errors);
        return new Result(ok, diags, sources.size(), ms, summary);
    }

    private static void deleteRecursively(File f) {
        File[] children = f.listFiles();
        if (children != null) for (File c : children) deleteRecursively(c);
        f.delete();
    }

    public File outputDir() { return outputDir; }

    /** Lists the binary class names found under a class directory. */
    public static List<String> classNames(File dir) {
        List<String> out = new ArrayList<>();
        int prefix = dir.getAbsolutePath().length() + 1;
        for (File f : TeamRepo.collect(dir, ".class")) {
            String rel = f.getAbsolutePath().substring(prefix).replace(File.separatorChar, '/');
            out.add(rel.substring(0, rel.length() - ".class".length()).replace('/', '.'));
        }
        return out;
    }

    public static String readText(File f) {
        try { return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8); } catch (IOException e) { return ""; }
    }
}
