# rabbitkt

Wrapper around the reactive RabbitMQ Client for usage with Kotlin Coroutines.

This Library is in early development, so there is no release yet. You are able to try out snapshot versions.

**Build Status:**

| Branch | Status |
| ------ |:------ |
| [main](https://github.com/NyCodeGHG/rabbitkt/tree/main)   | [![Gradle CI](https://github.com/NyCodeGHG/rabbitkt/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/NyCodeGHG/rabbitkt/actions/workflows/ci.yml) |
| [dev](https://github.com/NyCodeGHG/rabbitkt/tree/dev) | [![Gradle CI](https://github.com/NyCodeGHG/rabbitkt/actions/workflows/ci.yml/badge.svg?branch=dev)](https://github.com/NyCodeGHG/rabbitkt/actions/workflows/ci.yml) |

## Build Systems

### Gradle

<details open>
<summary>Kotlin</summary>

```kotlin
repositories {
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation("de.nycode.rabbitkt:rabbitkt-core:1.0.0-SNAPSHOT")
}
```

</details>

<details>
<summary>Groovy</summary>

```groovy
repositories {
    maven {
        url "https://s01.oss.sonatype.org/content/repositories/snapshots/"
    }
}

dependencies {
    implementation 'de.nycode.rabbitkt:rabbitkt-core:1.0.0-SNAPSHOT'
}
```

</details>

### Maven

I recommend [Gradle](https://gradle.org) with the [Kotlin DSL](https://gradle.org/kotlin/), but if you really want to
use maven, here's your snippet.

```xml
<repositories>
    <repository>
        <id>s01-oss-snapshots</id>
        <name>S01 Sonatype Snapshots</name>
        <url>https://s01.oss.sonatype.org/content/repositories/snapshots/</url>
    </repository>
</repositories>
```

```xml
<dependencies>
    <dependency>
        <groupId>de.nycode.rabbitkt</groupId>
        <artifactId>rabbitkt-core</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

## Credits

Many parts of this library were heavily inspired by [kmongo](https://github.com/Litote/kmongo). Thank you for your
awesome work <3.
