import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    base
    id("fabric-loom") version "1.9.2" apply false
}

allprojects {
    group = providers.gradleProperty("maven_group").get()
    version = providers.gradleProperty("fabricmmo_version").get()

    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://libraries.minecraft.net/")
    }
}

subprojects {
    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(21))
            withSourcesJar()
        }

        tasks.withType<JavaCompile>().configureEach {
            options.release.set(21)
            options.encoding = "UTF-8"
            options.compilerArgs.addAll(listOf("-Xlint:all,-processing", "-Werror"))
        }

        dependencies.add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            testLogging {
                events = setOf(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
                exceptionFormat = TestExceptionFormat.FULL
            }
        }
    }
}
