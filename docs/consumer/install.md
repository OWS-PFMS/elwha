# Install and authenticate

Elwha publishes to **GitHub Packages**, not Maven Central. GitHub Packages requires an
authenticated request for *every* download, including from public repositories — so unlike a
Central dependency, adding the coordinates alone is not enough. Budget five minutes for the
token step the first time.

## Requirements

| | |
|---|---|
| **JDK** | 21 or later. Elwha compiles to bytecode 65 (`maven.compiler.release=21`), so a JDK 17 consumer cannot load it. |
| **UI toolkit** | Swing. No JavaFX, no Compose, no AWT-only fallback. |
| **Transitive dependencies** | `com.formdev:flatlaf`, `flatlaf-extras`, `flatlaf-intellij-themes` (all 3.2.5), plus `com.github.weisj:jsvg` at runtime for SVG icon rendering. All resolve automatically. |

Elwha depends on **Swing and FlatLaf only** — see [Dependency stance](stability.md#dependency-stance).

## 1. Add the repository and the dependency

```xml
<repositories>
  <repository>
    <id>github-elwha</id>
    <url>https://maven.pkg.github.com/OWS-PFMS/elwha</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.owspfm</groupId>
    <artifactId>elwha</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
```

The `<repository>` `<id>` is what ties this block to your credentials in the next step. If you
change it here, change it there too.

## 2. Create a personal access token

GitHub Packages authenticates Maven reads with a personal access token, not your password.

1. Go to **Settings → Developer settings → Personal access tokens** on GitHub.
2. Create a token with the **`read:packages`** scope. That single scope is sufficient to consume
   Elwha; `write:packages` and `repo` are not needed and should not be granted.
3. Copy the token. GitHub shows it once.

Either a classic token or a fine-grained token works. A fine-grained token needs read access to
packages for the `OWS-PFMS` organization.

## 3. Add the credentials to `settings.xml`

In `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>github-elwha</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_PAT_WITH_READ_PACKAGES</password>
    </server>
  </servers>
</settings>
```

The `<id>` must match the `<repository>` `<id>` from step 1 exactly. This is the single most
common cause of a first-time failure.

Do not commit the token. `~/.m2/settings.xml` lives outside your project for exactly this reason;
if your build must read it from an environment variable instead, use
`<password>${env.GITHUB_TOKEN}</password>` and export the variable at build time.

## 4. Verify

```bash
mvn -q dependency:get -Dartifact=com.owspfm:elwha:1.0.0
```

A silent exit means the artifact resolved. Then continue to the [Quick start](quick-start.md).

## Troubleshooting

**`401 Unauthorized` fetching `elwha-1.0.0.pom`.** Maven is sending no credentials or wrong ones.
Check, in order: the `<server>` `<id>` matches the `<repository>` `<id>`; the username is your
GitHub login, not your email; the token has not expired. Run with `-X` and look for
`Using authentication for server github-elwha` — if that line is absent, the ids do not match.

**`403 Forbidden`.** The credentials were accepted but the token lacks `read:packages`, or a
fine-grained token was scoped to the wrong organization. Regenerate with the right scope.

**`Could not find artifact com.owspfm:elwha:1.0.0`.** Maven never reached GitHub Packages. Confirm
the `<repositories>` block is in the POM that is actually building — in a multi-module project it
belongs in the parent, or in each module that declares the dependency.

**It worked yesterday and now returns 401.** Classic tokens expire. Regenerate and update
`settings.xml`.

**Gradle.** Elwha is not published with Gradle metadata, but the Maven POM resolves normally:

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/OWS-PFMS/elwha")
        credentials {
            username = providers.gradleProperty("gpr.user").get()
            password = providers.gradleProperty("gpr.token").get()
        }
    }
}

dependencies {
    implementation("com.owspfm:elwha:1.0.0")
}
```
