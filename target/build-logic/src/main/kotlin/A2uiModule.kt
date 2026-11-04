// Helpers every module uses, in target/a2ui-kmp and target/material3-a2ui-kmp. Part of the
// build-logic included build; the Kotlin plugin it calls is the version of the build that applies it.
import java.io.File
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

/** The targets and compiler settings every module uses, in target/a2ui-kmp and target/material3-a2ui-kmp. */
fun KotlinMultiplatformExtension.a2uiTargets(enableOhosX64: Boolean = false) {
    ohosArm64()
    if (enableOhosX64) ohosX64()
    val ohosShared = sourceSets.maybeCreate("ohosMain")
    ohosShared.dependsOn(sourceSets.getByName("commonMain"))
    sourceSets.getByName("ohosArm64Main").dependsOn(ohosShared)
    sourceSets.findByName("ohosX64Main")?.dependsOn(ohosShared)
    val ohosTests = sourceSets.maybeCreate("ohosTest")
    ohosTests.dependsOn(sourceSets.getByName("commonTest"))
    sourceSets.getByName("ohosArm64Test").dependsOn(ohosTests)
    sourceSets.findByName("ohosX64Test")?.dependsOn(ohosTests)
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

/**
 * Builds commonMain from the upstream module at [upstreamPath] (relative to source/).
 * Files in overlay-generated/ or overlay-manual/ replace upstream files at the same relative path.
 * scripts/generate-overlays.py writes generated overlays; overlay-manual.json records manual ones.
 * src/ supplies additional platform code. The upstream unit tests run on desktop.
 */
fun Project.upstreamSources(kotlin: KotlinMultiplatformExtension, upstreamPath: String) {
    val upstream = upstreamRoot().resolve(upstreamPath).resolve("src").canonicalFile
    val main = upstream.resolve("main/kotlin")
    val generated = projectDir.resolve("overlay-generated")
    val manual = projectDir.resolve("overlay-manual")
    val overlaid = relativeFiles(generated) + relativeFiles(manual)
    kotlin.sourceSets.getByName("commonMain").kotlin.apply {
        srcDir(main)
        srcDir(generated)
        srcDir(manual)
        exclude { it.file.startsWith(main) && it.relativePath.pathString in overlaid }
    }
    kotlin.sourceSets.findByName("desktopTest")?.apply {
        this.kotlin.srcDir(upstream.resolve("test/kotlin"))
        resources.srcDir(upstream.resolve("test/resources"))
    }
}

/** source/: AndroidX's sources as synced. Both builds (target/a2ui-kmp, target/material3-a2ui-kmp) are two levels down. */
private fun Project.upstreamRoot(): File = rootDir.resolve("../../source")

private fun relativeFiles(dir: File): Set<String> =
    dir.walkTopDown().filter { it.isFile }.map { it.relativeTo(dir).invariantSeparatorsPath }.toSet()

/**
 * Runs the upstream module's instrumented tests (src/androidTest) on desktop, unchanged, with Compose
 * Multiplatform's runComposeUiTest: adds them and the stand-ins for the Android test APIs they use
 * (android-test-stand-ins) to desktopTest. The module adds the test dependencies.
 */
fun Project.upstreamUiTestsOnDesktop(kotlin: KotlinMultiplatformExtension, upstreamPath: String) {
    val tests = upstreamRoot().resolve(upstreamPath).resolve("src/androidTest/kotlin").canonicalFile
    kotlin.sourceSets.findByName("desktopTest")?.kotlin?.apply {
        srcDir(tests)
        // tests/android-test-stand-ins, shared by both builds.
        srcDir(rootDir.resolve("../../tests/android-test-stand-ins/kotlin").canonicalFile)
    }
}

/** The Compose Multiplatform desktop artifact with the Skia runtime for this machine. */
fun desktopRuntime(version: String): String {
    val os = System.getProperty("os.name").lowercase()
    val arch = if (System.getProperty("os.arch") in setOf("aarch64", "arm64")) "arm64" else "x64"
    val platform = when {
        "mac" in os -> "macos"
        "win" in os -> "windows"
        else -> "linux"
    }
    return "org.jetbrains.compose.desktop:desktop-jvm-$platform-$arch:$version"
}
