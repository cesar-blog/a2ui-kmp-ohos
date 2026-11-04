plugins {
    kotlin("multiplatform")
    id("a2ui.publish")
}

a2uiPublishing {
    androidArtifact = "androidx.a2ui:a2ui-engine"
}

kotlin {
    a2uiTargets(providers.gradleProperty("enableOhosX64").orElse("false").get().toBoolean())
    upstreamSources(this, "a2ui/a2ui-engine")

    sourceSets {
        commonMain {
            dependencies {
                api(project(":a2ui-model"))
                api(libs.kotlinx.coroutines.core)
                implementation(libs.androidx.collection)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        findByName("desktopTest")?.apply {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.junit)
                implementation(libs.truth)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.guava.testlib)
            }
        }
    }
}
