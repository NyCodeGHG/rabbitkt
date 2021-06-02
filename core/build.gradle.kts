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
    `maven-publish`
}

kotlin {
    explicitApi()
}

dependencies {
    api(project(":annotations"))
    api(project(":plugins"))

    implementation("io.projectreactor.rabbitmq", "reactor-rabbitmq", Versions.`reactor-rabbitmq`)

    implementation("org.slf4j", "slf4j-api", Versions.`slf4j-api`)
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", Versions.`kotlinx-coroutines`)
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-reactor", Versions.`kotlinx-coroutines`)
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-core", Versions.`kotlinx-serialization`)

    // Test Dependencies
    testImplementation("org.junit.jupiter", "junit-jupiter-api", Versions.`junit-jupter`)
    testImplementation("org.junit.jupiter", "junit-jupiter-params", Versions.`junit-jupter`)
    testImplementation("io.strikt", "strikt-core", Versions.`strikt-core`)
    testImplementation("org.jetbrains.kotlinx", "kotlinx-coroutines-test", Versions.`kotlinx-coroutines`)
    testImplementation(platform("org.testcontainers:testcontainers-bom:${Versions.testcontainers}"))
    testImplementation("org.testcontainers", "testcontainers")
    testImplementation("org.testcontainers", "junit-jupiter")
    testImplementation("org.testcontainers", "rabbitmq")
    testImplementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", Versions.`kotlinx-serialization`)

    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", Versions.`junit-jupter`)
    testRuntimeOnly("ch.qos.logback", "logback-classic", "1.2.3")
}

apply(from = rootProject.file("publishing.gradle.kts"))
