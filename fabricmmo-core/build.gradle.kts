plugins {
    `java-library`
    id("fabric-loom")
}

dependencies {
    api(project(":fabricmmo-api"))
    include(project(":fabricmmo-api"))

    minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
    mappings("net.fabricmc:yarn:${providers.gradleProperty("yarn_mappings").get()}:v2")
    modImplementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")

    val permissionsApi = "me.lucko:fabric-permissions-api:${providers.gradleProperty("fabric_permissions_api_version").get()}"
    modImplementation(permissionsApi)
    include(permissionsApi)

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

val modVersion = providers.gradleProperty("fabricmmo_version").get()

tasks.processResources {
    inputs.property("version", modVersion)
    filesMatching("fabric.mod.json") {
        expand("version" to modVersion)
    }
}
