pluginManagement {
    repositories {
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        exclusiveContent {
            forRepository {
                maven {
                    name = "LocalA2ui"
                    url = uri(providers.gradleProperty("a2uiRepo").orElse("../build/repo").get())
                }
            }
            filter { includeGroup("ai.littlepress.a2ui") }
        }
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
        mavenCentral()
        google()
    }
}

rootProject.name = "kai_a2ui_ohos"
include(":composeApp")
