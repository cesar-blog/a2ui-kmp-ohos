plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("a2ui.publish")
}

a2uiPublishing {
    androidArtifact = "androidx.a2ui.compose:compose-ui"
}

kotlin {
    a2uiTargets(providers.gradleProperty("enableOhosX64").orElse("false").get().toBoolean())
    upstreamSources(this, "a2ui/compose/compose-ui")

    sourceSets {
        commonMain {
            dependencies {
                api(project(":a2ui-model"))
                api(project(":compose-runtime"))
                api(libs.compose.runtime)
                api(libs.compose.ui)
                implementation(project(":a2ui-engine"))
                implementation(libs.androidx.collection)
            }
        }
        findByName("desktopTest")?.apply {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.junit)
                implementation(libs.truth)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.test.parameter.injector)
            }
        }
    }
}
