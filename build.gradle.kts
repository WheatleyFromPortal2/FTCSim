/*
 * FTCSim root build.
 *
 * The team repository to simulate is selected with (first match wins):
 *   -Prepo=/path/to/your/FtcRobotController-repo
 *   environment variable FTCSIM_REPO
 *   ftcsim.properties (key "repo") in this directory
 * The FTC SDK version is read from that repo's build.dependencies.gradle.
 */
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

gradle.projectsEvaluated {
    if (repoDir != null) logger.lifecycle("FTCSim: team repo = $repoDir (FTC SDK $ftcSdkVersion)")
    else logger.lifecycle("FTCSim: no team repo configured (use -Prepo=/path/to/repo); FTC SDK $ftcSdkVersion")
}
