/*
 *    Copyright 2021 NyCode
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    dokka
}

apply(from = "../publishing.gradle.kts")

kotlin {
    explicitApi()
}

dependencies {
    implementation("io.projectreactor.rabbitmq", "reactor-rabbitmq", "1.5.2")

    implementation("org.slf4j", "slf4j-api", "1.7.30")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.5.0")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-reactor", "1.5.0")

    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-core", "1.2.1")

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

    testImplementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", "1.2.1")

    testRuntimeOnly("ch.qos.logback", "logback-classic", "1.2.3")
}
