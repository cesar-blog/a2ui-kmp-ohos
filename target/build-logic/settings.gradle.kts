// The build logic of both builds, target/a2ui-kmp and target/material3-a2ui-kmp, included by each
// (pluginManagement { includeBuild("../build-logic") }). The Kotlin and publishing plugins are
// compileOnly here: each build puts the version it uses on the classpath (its root plugins block,
// from ../../gradle/libs.versions.toml), so the same build logic runs with Kotlin 2.3 and 2.4.
dependencyResolutionManagement {
    repositories {
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
        gradlePluginPortal()
        mavenCentral()
        google()
    }
    // The repository's one version catalog.
    versionCatalogs { create("libs") { from(files("../../gradle/libs.versions.toml")) } }
}
rootProject.name = "build-logic"
