package ftcsim.teamcode;

import com.qualcomm.robotcore.util.RobotLog;
import ftcsim.runner.OpModeEntry;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Loads (compiles, scans, reloads) the team's OpModes. Each build gets a fresh
 * class loader so static state in team code resets on reload, like
 * reinstalling the app on the Robot Controller.
 */
public final class TeamCode {
    public static final String TAG = "TeamCode";

    public static final class Build {
        public final ClassLoader classLoader;
        public final List<OpModeEntry> opModes;
        public final List<Class<?>> allClasses;
        public final TeamCompiler.Result compileResult;
        public final long builtAt = System.currentTimeMillis();
        public final List<String> loadErrors;
        Build(ClassLoader cl, List<OpModeEntry> opModes, List<Class<?>> allClasses, TeamCompiler.Result r, List<String> loadErrors) { this.classLoader = cl; this.opModes = opModes; this.allClasses = allClasses; this.compileResult = r; this.loadErrors = loadErrors; }
    }

    private final TeamRepo repo;
    private final File buildDir;
    private final String classpath;
    private final boolean includeSamples;
    private final boolean usePrebuilt;
    private volatile Build current;
    private volatile boolean building;
    private final List<Consumer<Build>> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean sourcesChanged;
    private Thread watcher;

    public TeamCode(TeamRepo repo, File buildDir, String classpath, boolean includeSamples, boolean usePrebuilt) {
        this.repo = repo; this.buildDir = buildDir; this.classpath = classpath; this.includeSamples = includeSamples; this.usePrebuilt = usePrebuilt;
    }

    public TeamRepo repo() { return repo; }
    public Build current() { return current; }
    public boolean isBuilding() { return building; }
    public boolean sourcesChanged() { return sourcesChanged; }
    public void addListener(Consumer<Build> l) { listeners.add(l); }

    public List<File> sourceFiles() {
        List<File> files = new ArrayList<>();
        for (File root : repo.sourceRoots) files.addAll(TeamRepo.collect(root, ".java"));
        if (includeSamples && repo.samplesRoot.isDirectory()) files.addAll(TeamRepo.collect(repo.samplesRoot, ".java"));
        return files;
    }

    /** Compiles and loads the team code; never throws. */
    public synchronized Build build() {
        building = true;
        try {
            sourcesChanged = false;
            List<File> classDirs = new ArrayList<>();
            TeamCompiler.Result result;
            if (usePrebuilt && !repo.prebuiltClassDirs.isEmpty()) {
                classDirs.addAll(repo.prebuiltClassDirs);
                result = new TeamCompiler.Result(true, Collections.emptyList(), 0, 0, "Using prebuilt classes from " + repo.prebuiltClassDirs);
                RobotLog.ii(TAG, "Using prebuilt classes: %s", repo.prebuiltClassDirs);
            } else {
                List<File> sources = sourceFiles();
                if (repo.hasKotlin) RobotLog.ww(TAG, "Kotlin sources found; FTCSim compiles Java only. Build the project in Android Studio and start FTCSim with --prebuilt to use the compiled Kotlin classes.");
                File out = new File(buildDir, "classes");
                TeamCompiler compiler = new TeamCompiler(out, classpath);
                RobotLog.ii(TAG, "Compiling %d Java files from %s", sources.size(), repo.sourceRoots);
                result = compiler.compile(sources);
                for (TeamCompiler.Diagnostic d : result.diagnostics) {
                    if ("ERROR".equals(d.kind)) RobotLog.ee(TAG, "%s:%d: %s", d.file, d.line, d.message);
                }
                RobotLog.ii(TAG, result.summary);
                if (!result.success) {
                    Build b = new Build(current == null ? TeamCode.class.getClassLoader() : current.classLoader, current == null ? Collections.emptyList() : current.opModes, current == null ? Collections.emptyList() : current.allClasses, result, Collections.emptyList());
                    notifyListeners(b);
                    return b;
                }
                classDirs.add(out);
            }
            URL[] urls = new URL[classDirs.size()];
            for (int i = 0; i < urls.length; i++) { try { urls[i] = classDirs.get(i).toURI().toURL(); } catch (MalformedURLException e) { throw new RuntimeException(e); } }
            ClassLoader cl = new URLClassLoader("teamcode", urls, TeamCode.class.getClassLoader());
            List<OpModeEntry> entries = new ArrayList<>();
            List<Class<?>> classes = new ArrayList<>();
            List<String> loadErrors = new ArrayList<>();
            for (File dir : classDirs) {
                for (String name : TeamCompiler.classNames(dir)) {
                    try {
                        Class<?> c = Class.forName(name, false, cl);
                        classes.add(c);
                        OpModeEntry e = OpModeEntry.from(c);
                        if (e != null) entries.add(e);
                    } catch (Throwable t) {
                        String msg = name + ": " + t;
                        loadErrors.add(msg);
                        RobotLog.ww(TAG, "Could not load class %s", msg);
                    }
                }
            }
            Collections.sort(entries);
            int enabled = 0; for (OpModeEntry e : entries) if (!e.disabled) enabled++;
            RobotLog.ii(TAG, "Found %d OpModes (%d enabled)", entries.size(), enabled);
            Build b = new Build(cl, entries, classes, result, loadErrors);
            current = b;
            notifyListeners(b);
            return b;
        } finally {
            building = false;
        }
    }

    private void notifyListeners(Build b) { for (Consumer<Build> l : listeners) { try { l.accept(b); } catch (RuntimeException ignored) {} } }

    /** Watches the source roots and flags changes (the UI offers a reload). */
    public synchronized void startWatching() {
        if (watcher != null) return;
        Thread t = new Thread(() -> {
            try (WatchService ws = FileSystems.getDefault().newWatchService()) {
                List<File> roots = new ArrayList<>(repo.sourceRoots);
                if (includeSamples && repo.samplesRoot.isDirectory()) roots.add(repo.samplesRoot);
                for (File root : roots) registerAll(root.toPath(), ws);
                while (!Thread.currentThread().isInterrupted()) {
                    WatchKey key = ws.take();
                    for (WatchEvent<?> ev : key.pollEvents()) {
                        Path p = (Path) ev.context();
                        if (p != null && (p.toString().endsWith(".java") || p.toString().endsWith(".kt"))) sourcesChanged = true;
                        if (ev.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                            Path dir = ((Path) key.watchable()).resolve(p);
                            if (Files.isDirectory(dir)) registerAll(dir, ws);
                        }
                    }
                    key.reset();
                }
            } catch (IOException | InterruptedException | RuntimeException e) {
                RobotLog.ww(TAG, "source watcher stopped: %s", e.toString());
            }
        }, "ftcsim-source-watcher");
        t.setDaemon(true);
        watcher = t;
        t.start();
    }

    private static void registerAll(Path root, WatchService ws) throws IOException {
        Files.walk(root).filter(Files::isDirectory).forEach(d -> {
            try { d.register(ws, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE); } catch (IOException ignored) {}
        });
    }
}
