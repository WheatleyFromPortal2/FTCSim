/*
 * The FTCSim application module.
 *
 * Besides the simulator itself this build script wires in the *team's* library
 * dependencies (Pedro Pathing, FTCLib, ...) by reading the dependency
 * declarations from the team repository's Gradle files, so the simulator's
 * classpath matches what the robot controller app would have.
 */
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.CompatibilityCheckDetails
import org.gradle.api.attributes.LibraryElements
import java.util.zip.ZipFile

plugins {
    application
    java
}

tasks.withType<JavaCompile> { options.encoding = "UTF-8"; options.release.set(17) }

val repoDir: File? = rootProject.extra["repoDir"] as File?
val ftcSdkVersion: String = rootProject.extra["ftcSdkVersion"] as String

// ---------------------------------------------------------------------------
// AAR support: Android library archives are turned into plain jars
// (classes.jar) so a JVM project can consume them.
// ---------------------------------------------------------------------------
val artifactType = Attribute.of("artifactType", String::class.java)

abstract class AarToJarTransform : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>
    override fun transform(outputs: TransformOutputs) {
        val aar = inputArtifact.get().asFile
        ZipFile(aar).use { zf ->
            val base = aar.name.removeSuffix(".aar")
            val classes = zf.getEntry("classes.jar")
            if (classes != null) {
                val out = outputs.file("$base-classes.jar")
                zf.getInputStream(classes).use { i -> out.outputStream().use { i.copyTo(it) } }
            }
            for (e in zf.entries()) {
                if (!e.isDirectory && e.name.startsWith("libs/") && e.name.endsWith(".jar")) {
                    val out = outputs.file("$base-" + e.name.substringAfterLast('/'))
                    zf.getInputStream(e).use { i -> out.outputStream().use { i.copyTo(it) } }
                }
            }
        }
    }
}

// Treat "aar" library elements as compatible with "jar" so variant matching succeeds.
class AarLibraryElementsCompatRule : AttributeCompatibilityRule<LibraryElements> {
    override fun execute(details: CompatibilityCheckDetails<LibraryElements>) {
        val consumer = details.consumerValue?.name
        val producer = details.producerValue?.name
        if (consumer == LibraryElements.JAR && producer == "aar") details.compatible()
    }
}

dependencies {
    attributesSchema {
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE) {
            compatibilityRules.add(AarLibraryElementsCompatRule::class.java)
        }
    }
    registerTransform(AarToJarTransform::class) {
        from.attribute(artifactType, "aar")
        to.attribute(artifactType, "jar")
    }
}

// ---------------------------------------------------------------------------
// Team dependencies parsed from the team repository.
// ---------------------------------------------------------------------------
val teamRuntime: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes.attribute(artifactType, "jar")
    // Provided by FTCSim itself / not usable on the desktop:
    exclude(group = "org.firstinspires.ftc")
    exclude(group = "androidx.appcompat")
    exclude(group = "androidx.core")
    exclude(group = "androidx.annotation")
    exclude(group = "androidx.lifecycle")
    exclude(group = "androidx.activity")
    exclude(group = "androidx.fragment")
    exclude(group = "com.android.support")
    exclude(group = "com.google.android")
    exclude(group = "com.bylazar")               // Panels: shimmed by FTCSim
    exclude(group = "com.bylazar.sloth")
    exclude(group = "com.acmerobotics.dashboard") // FTC Dashboard: shimmed by FTCSim
    exclude(group = "org.team11260")             // fast-load: APK hot-swap tool
    exclude(group = "dev.frozenmilk.sinister")   // Sloth: APK hot-swap tool
    exclude(group = "dev.frozenmilk")
    exclude(group = "org.openftc")               // EasyOpenCV / native OpenCV
}

data class GradleDep(val notation: String, val configuration: String)

/** Extracts `implementation 'g:a:v'` style declarations and maven repository URLs. */
fun parseGradleDeps(file: File): Pair<List<GradleDep>, List<String>> {
    if (!file.exists()) return Pair(emptyList(), emptyList())
    val text = file.readText()
    val deps = ArrayList<GradleDep>()
    val repos = ArrayList<String>()
    val depRe = Regex("""^\s*(implementation|api|compileOnly|runtimeOnly)\s*\(?\s*['"]([^'"]+)['"]""", RegexOption.MULTILINE)
    for (m in depRe.findAll(text)) {
        val notation = m.groupValues[2]
        if (notation.count { it == ':' } >= 2) deps.add(GradleDep(notation, m.groupValues[1]))
    }
    val repoRe = Regex("""url\s*=?\s*(?:uri\()?\s*['"]([^'"]+)['"]""")
    for (m in repoRe.findAll(text)) repos.add(m.groupValues[1])
    return Pair(deps, repos)
}

val teamDeps = ArrayList<GradleDep>()
val teamRepos = LinkedHashSet<String>()
if (repoDir != null) {
    val files = listOf(
        File(repoDir, "build.dependencies.gradle"),
        File(repoDir, "TeamCode/build.gradle"),
        File(repoDir, "TeamCode/build.gradle.kts"),
        File(repoDir, "build.gradle"),
        File(repoDir, "build.gradle.kts")
    )
    // Also honour a per-repo override file in FTCSim (never modifies the team repo)
    val override = rootProject.file("configs/${repoDir.name}.deps.gradle")
    for (f in files + override) {
        val (d, r) = parseGradleDeps(f)
        teamDeps.addAll(d)
        teamRepos.addAll(r)
    }
}

repositories {
    for (url in teamRepos) {
        if (url.contains("matthewo.tech")) continue
        maven { setUrl(url) }
    }
}

dependencies {
    implementation(project(":android-compat"))
    implementation(project(":ftc-sdk")) // generated R classes and resource strings
    implementation(project(path = ":ftc-sdk", configuration = "desktopSdk"))
    implementation("org.json:json:20240303")
    implementation("com.google.code.gson:gson:2.10.1")
    // Kotlin runtime for Kotlin-based team libraries (FTCLib, okhttp, ...)
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")

    for (d in teamDeps) {
        val g = d.notation.substringBefore(":")
        val isPresent = teamRuntime.excludeRules.any { it.group == g }
        if (!isPresent) {
            logger.lifecycle("FTCSim: team dependency $d")
            teamRuntime(d.notation)
        }
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}


application {
    mainClass.set("ftcsim.Main")
    applicationDefaultJvmArgs = listOf("-Xmx1g", "-Djava.awt.headless=true")
}

tasks.named<JavaExec>("run") {
    classpath += teamRuntime
    if (repoDir != null) systemProperty("ftcsim.repo", repoDir.absolutePath)
    systemProperty("ftcsim.home", rootProject.projectDir.absolutePath)
    standardInput = System.`in`
}

sourceSets.test { compileClasspath += teamRuntime; runtimeClasspath += teamRuntime }

tasks.test {
    useJUnitPlatform()
    classpath += teamRuntime
    systemProperty("ftcsim.home", rootProject.projectDir.absolutePath)
    if (repoDir != null) systemProperty("ftcsim.repo", repoDir.absolutePath)
}

/** Prints the full runtime classpath (used by the ftcsim launcher scripts). */
tasks.register("printClasspath") {
    dependsOn(tasks.named("classes"))
    doLast {
        val cp = sourceSets.main.get().runtimeClasspath + teamRuntime
        println("FTCSIM_CLASSPATH=" + cp.files.joinToString(File.pathSeparator))
    }
}
