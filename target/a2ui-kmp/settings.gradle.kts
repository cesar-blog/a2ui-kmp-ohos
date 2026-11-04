// The KMP build of five of the AndroidX A2UI packages, from ../../source/a2ui as synced from AOSP
// for desktop and iOS. The modules mirror upstream's layout (compose/compose-ui is
// source/a2ui/compose/compose-ui). Each compiles the upstream sources plus its overlay-generated/ and
// overlay-manual/ copies, which replace the upstream file at the same relative path, and src/, which
// is code of ours only. material3-a2ui needs newer Kotlin and Compose: ../material3-a2ui-kmp.
pluginManagement {
    // The build logic both builds share: the a2ui.publish plugin and the module helpers.
    includeBuild("../build-logic")
    repositories {
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
    }
    // The repository's one version catalog.
    versionCatalogs { create("libs") { from(files("../../gradle/libs.versions.toml")) } }
}
rootProject.name = "a2ui-kmp"
include(":a2ui-compat")
include(":a2ui-model")
include(":a2ui-engine")
for (module in listOf("compose-runtime", "compose-ui", "compose-ui-testing")) {
    include(":$module")
    project(":$module").projectDir = file("compose/$module")
}
