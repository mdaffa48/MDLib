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
        <artifactId>MDLib</artifactId>
        <version>2.4.8</version>
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
    implementation("com.muhammaddaffa:MDLib:2.4.8")
}
```
