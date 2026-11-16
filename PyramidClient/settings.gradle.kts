pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }
rootProject.name = "Pyramid Client"
include(":app")

dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}
