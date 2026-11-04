plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("a2ui.publish")
}

a2uiPublishing {
    androidArtifact = "androidx.a2ui.compose:compose-ui-testing"
}

kotlin {
    a2uiTargets(providers.gradleProperty("enableOhosX64").orElse("false").get().toBoolean())
    upstreamSources(this, "a2ui/compose/compose-ui-testing")
    upstreamUiTestsOnDesktop(this, "a2ui/compose/compose-ui-testing")

    sourceSets {
        commonMain {
            dependencies {
                api(project(":a2ui-model"))
                api(project(":compose-runtime"))
                api(project(":compose-ui"))
                api(libs.compose.runtime)
                api(libs.compose.ui)
                api(libs.kotlinx.coroutines.test)
                implementation(project(":a2ui-engine"))
            }
        }
        findByName("desktopTest")?.apply {
            // runComposeUiTest is still experimental in Compose Multiplatform 1.12.
            languageSettings.optIn("androidx.compose.ui.test.ExperimentalTestApi")
            dependencies {
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui.test)
                implementation(desktopRuntime(libs.versions.composeMultiplatform.get()))
                implementation(kotlin("test"))
                implementation(libs.junit)
                implementation(libs.truth)
            }
        }
    }
}
