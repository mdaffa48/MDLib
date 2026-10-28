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
        <version>2.4.5</version>
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
    implementation("com.muhammaddaffa:MDLib:2.4.5")
}
```

Command registration overrides conflicting command names and aliases. Other plugins remain reachable through their namespaced commands, and displaced names are restored when MDLib shuts down.

## Building and automatic versions

Build with Java 25 and the Maven wrapper:

```sh
sh mvnw verify
```

[Blocksmith Versioning](https://github.com/blocksmithstudio/versioning) calculates
`${revision}` from Git history. The immutable annotated `versioning/base/master`
tag anchors the existing `2.4.5` release; each subsequent first-parent commit on
`master` increments the third number automatically. Rebuilding the same commit
keeps its version. Do not edit the POM version or pass `-Drevision`.
The dependency examples above show the latest published release; a source commit
does not publish artifacts automatically.

Clone with full history and fetch tags. Before the first commit on a new branch,
initialize its baseline (replace `feature/example` with your branch name):

```sh
git switch -c feature/example
git tag -a versioning/base/feature/example -m '2.4.5'
git push -u origin feature/example refs/tags/versioning/base/feature/example
```

Feature builds receive branch and commit suffixes. Detached CI builds must set
`VERSIONING_BRANCH` to their source branch and fetch full history and baseline
tags. Local changes add `-dirty`; installing or deploying requires a clean tree.
Use the `install` or `deploy` lifecycle phase so published POMs contain the resolved
version. The tracked `.mvn/versioning-settings.xml` enables anonymous extension
resolution; publishing credentials stay in private Maven settings. If you override
settings with `--settings`, retain its active `blocksmith-versioning` profile.
