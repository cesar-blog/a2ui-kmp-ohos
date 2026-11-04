import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

val withNativeSelfTests = providers.gradleProperty("withNativeSelfTests").orElse("false").get().toBoolean()
require(!withNativeSelfTests || !providers.gradleProperty("enableOhosX64").orElse("false").get().toBoolean()) {
    "The compiled native self-test KLIBs target ohosArm64 only; disable enableOhosX64 for this profile."
}

kotlin {
    // This profile publishes and verifies OHOS artifacts only. Desktop sources remain
    // as references, without requesting variants absent from the fixed CPF release.
    val ohosTargets = mutableListOf(ohosArm64())
    if (providers.gradleProperty("enableOhosX64").orElse("false").get().toBoolean()) {
        ohosTargets.add(ohosX64())
    }
    ohosTargets.forEach { target ->
        target.binaries.sharedLib {
            baseName = "kn"
            export("org.jetbrains.compose.export:export:1.9.2-1.2.0")
            linkerOpts("-lz")
            if (providers.gradleProperty("rendererBackend").orElse("fusion-renderer").get() == "fusion-renderer") {
                linkerOpts(
                    "-lnative_drawing", "-limage_source", "-lpixelmap", "-lpixelmap_ndk.z",
                    "-lnative_window", "-lace_napi.z", "-lhilog_ndk.z", "-lhitrace_ndk.z",
                    "-luv", "-lunwind", "-licu",
                )
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("ai.littlepress.a2ui:compose-ui:1.0.0-alpha02-ohos.0.1.0")
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2-1.0.0")
        }
        val ohosMain by creating {
            dependsOn(commonMain.get())
            kotlin.srcDir(if (withNativeSelfTests) "src/nativeChecksMain/kotlin" else "src/nativeChecksDisabledMain/kotlin")
            dependencies {
                api("org.jetbrains.compose.export:export:1.9.2-1.2.0")
                if (withNativeSelfTests) {
                    // Reuse the already compiled test KLIBs; do not copy or reimplement tests.
                    val upstream = rootProject.projectDir.parentFile
                    val testKlibs = listOf(
                        upstream.resolve("target/a2ui-kmp/a2ui-compat/build/classes/kotlin/ohosArm64/test/klib/a2ui-compat_test"),
                        upstream.resolve("target/a2ui-kmp/compose/compose-runtime/build/classes/kotlin/ohosArm64/test/klib/compose-runtime_test"),
                    )
                    testKlibs.forEach { klib ->
                        require(klib.resolve("default/manifest").isFile) {
                            "Compile the OHOS test KLIB first: $klib"
                        }
                    }
                    implementation(files(testKlibs))
                    implementation(kotlin("test"))
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2-1.0.0")
                }
            }
        }
        ohosTargets.forEach { target ->
            getByName(target.name + "Main").dependsOn(ohosMain)
        }
    }
}

if (withNativeSelfTests) {
    gradle.taskGraph.whenReady {
        require(allTasks.none { it.name.contains("release", ignoreCase = true) }) {
            "withNativeSelfTests is a debug-only validation profile; disable it for release builds."
        }
    }
}

listOf("debug", "release").forEach { kind ->
    val capitalized = kind.replaceFirstChar { it.uppercase() }
    tasks.register<Copy>("publish${capitalized}BinariesToHarmonyApp") {
        group = "harmony"
        into(rootProject.file("harmonyApp"))
        kotlin.targets.withType<KotlinNativeTarget>().matching { it.name.startsWith("ohos") }.forEach { target ->
            dependsOn("link${capitalized}Shared${target.name.replaceFirstChar { it.uppercase() }}")
            val abi = if (target.name == "ohosArm64") "arm64-v8a" else "x86_64"
            from(layout.buildDirectory.file("bin/${target.name}/${kind}Shared/libkn.so")) {
                into("entry/libs/$abi")
            }
            from(layout.buildDirectory.file("bin/${target.name}/${kind}Shared/libkn_api.h")) {
                into("entry/src/main/cpp/include/$abi")
            }
        }
    }
}
