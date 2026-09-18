/*
 * FTCSim root build.
 *
 * The team repository to simulate is selected with (first match wins):
 *   -Prepo=/path/to/your/FtcRobotController-repo
 *   environment variable FTCSIM_REPO
 *   ftcsim.properties (key "repo") in this directory
 * The FTC SDK version is read from that repo's build.dependencies.gradle.
 */
import org.gradle.jvm.toolchain.JavaLanguageVersion
import java.util.Properties

val propsFile = rootProject.file("ftcsim.properties")
val localProps = Properties().apply { if (propsFile.exists()) propsFile.inputStream().use { load(it) } }

fun findRepoDir(): File? {
    val candidates = listOf(
        (project.findProperty("repo") as String?),
        System.getenv("FTCSIM_REPO"),
        localProps.getProperty("repo")
    )
    for (c in candidates) {
        if (c != null && c.isNotBlank()) {
            val f = if (File(c).isAbsolute) File(c) else rootProject.file(c)
            return f.canonicalFile
        }
    }
    return null
}

val repoDir: File? = findRepoDir()
extra["repoDir"] = repoDir

// Detect the FTC SDK version used by the team repo (defaults to 11.0.0).
fun detectSdkVersion(dir: File?): String {
    if (dir == null) return "11.0.0"
    val candidates = listOf(File(dir, "build.dependencies.gradle"), File(dir, "TeamCode/build.gradle"), File(dir, "build.gradle"))
    val re = Regex("""org\.firstinspires\.ftc:RobotCore:([0-9][0-9A-Za-z.\-]*)""")
    for (f in candidates) {
        if (f.exists()) {
            val m = re.find(f.readText())
            if (m != null) return m.groupValues[1]
        }
    }
    return "11.0.0"
}
val ftcSdkVersion = detectSdkVersion(repoDir)
extra["ftcSdkVersion"] = ftcSdkVersion

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

/*
 * Java toolchain. The simulator needs a JDK 17 or newer (the SDK classes are Java 17 and the team code
 * is compiled in-process with javac). When Gradle itself runs on such a JDK that one is used; when it
 * was started from an older Java or from a JRE without a compiler (a system where `java` is a Java 8
 * JRE, say), a JDK 21 is picked from the installed JVMs or downloaded by the foojay resolver plugin.
 * -Pftcsim.jdk=<version> forces a specific toolchain version.
 */
val forcedJdk = (project.findProperty("ftcsim.jdk") as String?)?.toIntOrNull()
val currentJvmIsUsable = JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17) && javax.tools.ToolProvider.getSystemJavaCompiler() != null
val toolchainVersion: Int? = forcedJdk ?: if (currentJvmIsUsable) null else 21
extra["toolchainVersion"] = toolchainVersion

subprojects {
    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            if (toolchainVersion != null) toolchain { languageVersion.set(JavaLanguageVersion.of(toolchainVersion)) }
        }
    }
}

gradle.projectsEvaluated {
    if (repoDir != null) logger.lifecycle("FTCSim: team repo = $repoDir (FTC SDK $ftcSdkVersion)")
    else logger.lifecycle("FTCSim: no team repo configured (use -Prepo=/path/to/repo); FTC SDK $ftcSdkVersion")
    if (toolchainVersion != null) logger.lifecycle("FTCSim: Gradle runs on Java ${JavaVersion.current()} (${System.getProperty("java.home")}); compiling and running with a JDK $toolchainVersion toolchain")
}
