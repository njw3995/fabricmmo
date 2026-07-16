pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "fabricmmo"

include(
    "fabricmmo-api",
    "fabricmmo-core",
    "fabricmmo-client",
    "parity-tests",
    "tools:upstream-diff"
)
