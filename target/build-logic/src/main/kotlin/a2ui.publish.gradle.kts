// Local OHOS fork publishing. No credentials or remote publication are required.
// Retains the upstream module extension so the original build files stay readable.
apply(plugin = "maven-publish")
group = "ai.littlepress.a2ui"
val versions = the<VersionCatalogsExtension>().named("libs")
version = versions.findVersion("a2uiKmp").get().requiredVersion
extensions.create<A2uiPublishingExtension>("a2uiPublishing")
configure<PublishingExtension> {
    repositories {
        maven {
            name = "local"
            url = rootDir.resolve("../../build/repo").canonicalFile.toURI()
        }
    }
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("A2UI OHOS: ${project.name}")
            description.set("OHOS adaptation for CPF KMP 2.2.21-1.2.0 and CMP 1.9.2-1.2.0")
            url.set("https://github.com/BinaryTape/a2ui-kmp")
            licenses { license { name.set("Apache License, Version 2.0"); url.set("https://www.apache.org/licenses/LICENSE-2.0") } }
        }
    }
}
