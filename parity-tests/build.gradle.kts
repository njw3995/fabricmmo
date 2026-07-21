import org.gradle.api.file.DuplicatesStrategy
import org.gradle.jvm.tasks.Jar

plugins { java }

val externalAddon = sourceSets.create("externalAddon") {
    java.srcDir("fixtures/external-addon/src/main/java")
    resources.srcDir("fixtures/external-addon/src/main/resources")
}

val manualProbe = sourceSets.create("manualProbe") {
    java.srcDir("fixtures/manual-probe/src/main/java")
}

dependencies {
    add(externalAddon.implementationConfigurationName, project(":fabricmmo-api"))
    add(manualProbe.implementationConfigurationName, project(":fabricmmo-api"))

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

fun registerManualProbeJar(taskName: String, archiveName: String, mode: String) =
    tasks.register<Jar>(taskName) {
        group = "build"
        description = "Builds the $mode Taming/Alchemy manual API probe."
        dependsOn(manualProbe.classesTaskName)
        archiveFileName.set("$archiveName.jar")
        from(manualProbe.output)
        from("fixtures/manual-probe/$mode")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

val manualProbeObserveJar = registerManualProbeJar(
    "manualProbeObserveJar",
    "fabricmmo-taming-alchemy-probe-observe",
    "observe")
val manualProbeCancelJar = registerManualProbeJar(
    "manualProbeCancelJar",
    "fabricmmo-taming-alchemy-probe-cancel",
    "cancel")
val manualProbeMutateJar = registerManualProbeJar(
    "manualProbeMutateJar",
    "fabricmmo-taming-alchemy-probe-mutate",
    "mutate")

tasks.named("assemble") {
    dependsOn(manualProbeObserveJar, manualProbeCancelJar, manualProbeMutateJar)
}
