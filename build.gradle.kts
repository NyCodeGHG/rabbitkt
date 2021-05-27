plugins {
    kotlin("jvm") version "1.5.10"
    dokka version "1.4.32"
    `maven-publish`
    signing
}

group = "de.nycode"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.projectreactor.rabbitmq", "reactor-rabbitmq", "1.5.2")

    implementation("org.slf4j", "slf4j-api", "1.7.30")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.5.0")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-reactor", "1.5.0")

    // Test Dependencies
    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.7.2")
    testImplementation("org.junit.jupiter", "junit-jupiter-params", "5.7.2")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.7.2")

    testImplementation("io.strikt", "strikt-core", "0.31.0")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.jetbrains.kotlinx", "kotlinx-coroutines-test", "1.5.0")

    testImplementation(platform("org.testcontainers:testcontainers-bom:1.15.3"))
    testImplementation("org.testcontainers", "testcontainers")
    testImplementation("org.testcontainers", "junit-jupiter")
    testImplementation("org.testcontainers", "rabbitmq")
}

kotlin {
    explicitApi()
}

val projectJvmTarget = 11

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = if (projectJvmTarget <= 8) "1.$projectJvmTarget" else projectJvmTarget.toString()
            freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
        }
    }
    test {
        useJUnitPlatform()
    }
    dokkaHtml {
        moduleName.set("RabbitKt")
        dokkaSourceSets {
            configureEach {
                jdkVersion.set(projectJvmTarget)
                platform.set(org.jetbrains.dokka.Platform.jvm)

                sourceLink {
                    localDirectory.set(file("src/main/kotlin"))
                    remoteUrl.set(uri("https://github.com/NyCodeGHG/rabbitkt/tree/main/src/main/kotlin").toURL())
                    remoteLineSuffix.set("#L")
                }
            }
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(projectJvmTarget))
    }
}

apply(from = "publishing.gradle.kts")
