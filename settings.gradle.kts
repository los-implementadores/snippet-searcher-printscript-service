plugins {
    // If the JDK the toolchain asks for (21) isn't installed, Gradle downloads it.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "template"
