plugins {
    java
    checkstyle
    pmd
    jacoco
    id("org.springframework.boot") version "4.1.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "7.2.1"
}

group = "org.snippetsearcher"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

// New Relic agent: resolved by Gradle but kept off the classpath. The Dockerfile attaches it with
// -javaagent (see copyNewRelic).
val newRelic: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-restclient-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    newRelic("com.newrelic.agent.java:newrelic-agent:9.5.0")
}

checkstyle {
    toolVersion = "10.17.0"
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    // A warning fails the build (and the pre-commit hook): if it could be ignored, nobody would look.
    maxWarnings = 0
}

pmd {
    toolVersion = "7.3.0"
    isConsoleOutput = true
    ruleSetFiles = files("${rootProject.projectDir}/config/pmd/pmd-rules.xml")
    ruleSets = listOf()
}

spotless {
    java {
        googleJavaFormat("1.22.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// Fixed name so the Dockerfile doesn't depend on the project name or version.
tasks.bootJar {
    archiveFileName.set("app.jar")
}

// Puts the agent and its config side by side in build/newrelic: the agent reads newrelic.yml from
// the directory of newrelic.jar.
val copyNewRelic by tasks.registering(Sync::class) {
    description = "Copies the New Relic agent and its config to build/newrelic."
    group = "build"
    from(newRelic) {
        rename { "newrelic.jar" }
    }
    from("newrelic") {
        include("newrelic.yml")
    }
    into(layout.buildDirectory.dir("newrelic"))
}

// Points git at the versioned hooks in hooks/ (core.hooksPath). Runs before any compilation, so
// nobody has to remember to install them, and a change to a hook reaches everyone on the next pull.
val installGitHooks by tasks.registering(Exec::class) {
    description = "Configures git to use the hooks in hooks/ (core.hooksPath)."
    group = "build setup"
    val gitDir = layout.projectDirectory.file(".git").asFile
    val currentHooksPath =
        providers
            .exec {
                commandLine("git", "config", "--get", "core.hooksPath")
                isIgnoreExitValue = true
            }
            .standardOutput
            .asText
            .map { it.trim() }
    onlyIf("it is a git clone") { gitDir.exists() }
    // An existing core.hooksPath (a global hook manager, for example) is left alone: that person's
    // hooks would silently stop running in this repo.
    onlyIf("no core.hooksPath is configured") {
        val current = currentHooksPath.get()
        if (current.isNotEmpty() && current != "hooks") {
            logger.warn(
                "git already uses core.hooksPath=$current, so the hooks in hooks/ are not configured. " +
                    "To use them in this repo: git config core.hooksPath hooks",
            )
        }
        current.isEmpty()
    }
    commandLine("git", "config", "core.hooksPath", "hooks")
}

tasks.compileJava {
    dependsOn(installGitHooks)
}
