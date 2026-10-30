Choose exactly one platform module for your plugin:

| Platform | Artifact |
| --- | --- |
| Spigot | `mdlib-spigot` |
| Velocity | `mdlib-velocity` |
| BungeeCord | `mdlib-bungeecord` |

The examples below use `mdlib-spigot`. Replace it with the artifact for your platform. Each platform module includes `mdlib-common` as a transitive dependency.

## Maven

```xml
<repositories>
    <repository>
        <id>blocksmith-releases</id>
        <url>https://repo.blocksmith.studio/releases</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.muhammaddaffa</groupId>
        <artifactId>mdlib-spigot</artifactId>
        <version>3.0.1</version>
    </dependency>
</dependencies>
```

## Gradle

```groovy
repositories {
    maven {
        url = uri("https://repo.blocksmith.studio/releases")
    }
}

dependencies {
    implementation("com.muhammaddaffa:mdlib-spigot:3.0.1")
}
```

Shade the selected module and its transitive dependencies into your plugin. If your shading configuration filters artifacts, include `mdlib-common` too. Relocate `com.muhammaddaffa.mdlib` to a package owned by your plugin. Existing imports for `mdlib-spigot` stay unchanged.
