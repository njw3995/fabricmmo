plugins { application }

application {
    mainClass.set("io.github.njw3995.fabricmmo.tools.upstreamdiff.UpstreamDiffMain")
}

tasks.named<JavaExec>("run") {
    workingDir(rootProject.projectDir)
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
