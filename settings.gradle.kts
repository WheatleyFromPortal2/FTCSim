// Lets Gradle download a JDK when the machine has none that fits the toolchain declared in build.gradle.kts.
plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

rootProject.name = "FTCSim"

include("android-compat", "ftc-sdk", "sim")
