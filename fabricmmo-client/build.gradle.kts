plugins {
    `java-library`
    id("fabric-loom")
}

loom {
    runs {
        named("client") {
            programArg("--username")
            programArg("Player338")
        }
    }
}

dependencies {
    implementation(project(":fabricmmo-api"))
    implementation(project(path = ":fabricmmo-core", configuration = "namedElements"))

    minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
    mappings("net.fabricmc:yarn:${providers.gradleProperty("yarn_mappings").get()}:v2")
    modImplementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
    modRuntimeOnly("me.lucko:fabric-permissions-api:${providers.gradleProperty("fabric_permissions_api_version").get()}")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}
