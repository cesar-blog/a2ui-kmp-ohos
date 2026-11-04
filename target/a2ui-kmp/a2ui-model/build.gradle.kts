plugins {
    kotlin("multiplatform")
    id("a2ui.publish")
}

a2uiPublishing {
    androidArtifact = "androidx.a2ui:a2ui-model"
}

kotlin {
    a2uiTargets(providers.gradleProperty("enableOhosX64").orElse("false").get().toBoolean())
    upstreamSources(this, "a2ui/a2ui-model")

    sourceSets {
        commonMain {
            dependencies {
                api(project(":a2ui-compat"))
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.androidx.collection)
                // Upstream gets these through androidx.core on Android; declared directly here.
                implementation(libs.androidx.annotation)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        findByName("desktopTest")?.apply {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.junit)
                implementation(libs.truth)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.guava.testlib)
                implementation(libs.mockito.core)
                implementation(libs.mockito.kotlin)
                implementation(libs.gson)
            }
        }
    }
}
