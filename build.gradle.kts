plugins {
    kotlin("jvm") version "1.5.10" apply false
    kotlin("plugin.serialization") version "1.5.10" apply false
    dokka version "1.4.32"
}

group = "de.nycode.rabbitkt"
version = "1.0.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }
}

val projectJvmTarget = 11

subprojects {
    group = rootProject.group
    version = rootProject.version
    tasks {
        withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions {
                jvmTarget = if (projectJvmTarget <= 8) "1.$projectJvmTarget" else projectJvmTarget.toString()
                freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
            }
        }
        withType<Test> {
            useJUnitPlatform()
        }
        withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
            dokkaSourceSets {
                configureEach {
                    jdkVersion.set(projectJvmTarget)
                    platform.set(org.jetbrains.dokka.Platform.jvm)

                    sourceLink {
                        localDirectory.set(file("src/main/kotlin"))
                        remoteUrl.set(uri("https://github.com/NyCodeGHG/rabbitkt/tree/main/${project.name}/src/main/kotlin").toURL())
                        remoteLineSuffix.set("#L")
                    }
                }
            }
        }
    }
}
