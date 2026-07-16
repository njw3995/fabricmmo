plugins { java }

val externalAddon = sourceSets.create("externalAddon") {
    java.srcDir("fixtures/external-addon/src/main/java")
    resources.srcDir("fixtures/external-addon/src/main/resources")
}

dependencies {
    add(externalAddon.implementationConfigurationName, project(":fabricmmo-api"))

    testImplementation(project(":fabricmmo-api"))
    testImplementation(project(":fabricmmo-core"))
    testImplementation(externalAddon.output)
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.named("compileTestJava") {
    dependsOn(externalAddon.classesTaskName)
}

tasks.named<Test>("test") {
    dependsOn(externalAddon.classesTaskName)
}
