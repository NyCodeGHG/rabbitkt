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
import de.nycode.rabbitkt.gradle.*

apply(plugin = "org.gradle.maven-publish")
apply(plugin = "org.gradle.signing")

val sonatypeUsername = System.getenv("SONATYPE_USER") ?: findProperty("sonatypeUsername")?.toString()
val sonatypePassword = System.getenv("SONATYPE_PASSWORD") ?: findProperty("sonatypePassword")?.toString()

val isSnapshot = version.toString().endsWith("SNAPSHOT")

val dokkaJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    val dokkaHtml = project.tasks.findByName("dokkaHtml") ?: return@registering
    dependsOn(dokkaHtml)
    from(dokkaHtml)
}

val configurePublishing: PublishingExtension.() -> Unit = {
    repositories {
        maven {
            name = "oss"
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (isSnapshot) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = sonatypeUsername
                password = sonatypePassword
            }
        }
    }
    val branch = getGitBranch()

    publications {
        if (branch != "main" && !isSnapshot) {
            return@publications
        }
        create<MavenPublication>(project.name.toString()) {
            println("Creating Publication for ${project.name}")
            from(project.components["kotlin"])
            groupId = project.group.toString()
            artifact(dokkaJar)
            configureVersion(project, branch)
            pom {
                configureMavenCentralMetadata(project)
            }
            signPublicationIfKeyPresent(project)
        }
    }
}

extensions.configure("publishing", configurePublishing)

val Project.publishing: PublishingExtension
    get() =
        (this as ExtensionAware).extensions.getByName("publishing") as PublishingExtension
