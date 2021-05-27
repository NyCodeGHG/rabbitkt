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

apply(plugin = "org.jetbrains.dokka")
apply(plugin = "org.gradle.maven-publish")
apply(plugin = "org.gradle.signing")

val sonatypeUsername = System.getenv("SONATYPE_USER")
val sonatypePassword = System.getenv("SONATYPE_PASSWORD")

val dokkaJar by tasks.registering(Jar::class) {
    dependsOn("dokkaHtml")
    archiveClassifier.set("javadoc")
    from(tasks.getByName("dokkaHtml"))
}

val configurePublishing: PublishingExtension.() -> Unit = {
    repositories {
        maven {
            name = "oss"
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = sonatypeUsername
                password = sonatypePassword
            }
        }
    }
    val branch = getGitBranch()

    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
            groupId = project.group.toString()
            artifactId = project.name.toString()
            val projectVersion = project.version.toString()
            val branch = getGitBranch()
            if (projectVersion.endsWith("SNAPSHOT") && branch != "dev") {
                version = "$branch-$projectVersion"
            } else {
                version = projectVersion
            }
            artifact(dokkaJar)
            pom {
                name.set(project.name)
                description.set(project.description)
                url.set("https://github.com/NyCodeGHG/rabbitkt")

                licenses {
                    license {
                        name.set("Apache-2.0 License")
                        url.set("https://github.com/NyCodeGHG/rabbitkt/blob/main/LICENSE")
                    }
                }

                issueManagement {
                    system.set("GitHub")
                    url.set("https://github.com/NyCodeGHG/rabbitkt/issues")
                }

                scm {
                    connection.set("https://github.com/NyCodeGHG/rabbitkt.git")
                    url.set("https://github.com/NyCodeGHG/rabbitkt")
                }

                developers {
                    developer {
                        name.set("NyCode")
                        email.set("nico@nycode.de")
                        url.set("https://nycode.de")
                        timezone.set("Europe/Berlin")
                    }
                }
            }
        }
    }
}

val configureSigning: SigningExtension.() -> Unit = {
    val signingKey = System.getenv("SIGNING_KEY")
    val signingPassword = System.getenv("SIGNING_PASSWORD")
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(
            String(java.util.Base64.getDecoder().decode(signingKey.toByteArray())),
            signingPassword
        )
    }

    publishing.publications.withType<MavenPublication> {
        sign(this)
    }
}

extensions.configure("signing", configureSigning)
extensions.configure("publishing", configurePublishing)

val Project.publishing: PublishingExtension
    get() =
        (this as ExtensionAware).extensions.getByName("publishing") as PublishingExtension
