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
package de.nycode.rabbitkt.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension


infix fun <T> Property<T>.by(value: T) = set(value)

fun MavenPom.configureMavenCentralMetadata(project: Project) {
    name by project.name
    description by ""
    url by "https://github.com/NyCodeGHG/rabbitkt"

    licenses {
        license {
            name by "Apache-2.0 License"
            url by "https://github.com/NyCodeGHG/rabbitkt/blob/main/LICENSE"
        }
    }

    issueManagement {
        system by "GitHub"
        url by "https://github.com/NyCodeGHG/rabbitkt/issues"
    }

    scm {
        connection by "https://github.com/NyCodeGHG/rabbitkt.git"
        url by "https://github.com/NyCodeGHG/rabbitkt"
    }

    developers {
        developer {
            name by "NyCode"
            email by "nico@nycode.de"
            url by "https://nycode.de"
            timezone by "Europe/Berlin"
        }
    }
}

fun MavenPublication.configureVersion(project: Project, branch: String?) {
    artifactId = "rabbitkt-${project.name}"
    val projectVersion = project.version.toString()
    if (projectVersion.endsWith("SNAPSHOT") && branch != "dev") {
        version = "$branch-$projectVersion"
    } else {
        version = projectVersion
    }
}

fun MavenPublication.signPublicationIfKeyPresent(project: Project) {
    val signingKey =
        System.getenv("SIGNING_KEY") ?: project.findProperty("signingKey")?.toString()
    val signingPassword = System.getenv("SIGNING_PASSWORD") ?: project.findProperty("signingPassword")?.toString()
    if (signingKey != null && signingPassword != null) {
        project.extensions.configure<SigningExtension>("signing") {
            useInMemoryPgpKeys(
                String(java.util.Base64.getDecoder().decode(signingKey.toByteArray())),
                signingPassword
            )
            sign(this@signPublicationIfKeyPresent)
        }
    }
}

