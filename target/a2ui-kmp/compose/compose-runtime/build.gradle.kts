plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("a2ui.publish")
}

a2uiPublishing {
    androidArtifact = "androidx.a2ui.compose:compose-runtime"
}

kotlin {
    a2uiTargets(providers.gradleProperty("enableOhosX64").orElse("false").get().toBoolean())
    upstreamSources(this, "a2ui/compose/compose-runtime")

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        commonMain {
            dependencies {
                api(project(":a2ui-model"))
                api(libs.androidx.collection)
                api(libs.compose.runtime)
                implementation(project(":a2ui-engine"))
                implementation(libs.androidx.annotation)
            }
        }
        findByName("desktopTest")?.apply {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.junit)
                implementation(libs.truth)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}
