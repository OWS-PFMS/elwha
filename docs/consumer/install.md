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
| **Transitive dependencies** | `com.formdev:flatlaf` and `flatlaf-extras` (both 3.2.5), plus `com.github.weisj:jsvg` at runtime for SVG icon rendering. All resolve automatically. |

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
    <version>1.1.0</version>
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
mvn -q dependency:get -Dartifact=com.owspfm:elwha:1.1.0
```

A silent exit means the artifact resolved. Then continue to the [Quick start](quick-start.md).

The API reference is browsable without any of the above, at
**[ows-pfms.github.io/elwha](https://ows-pfms.github.io/elwha/)**. It is also published as a
`javadoc` classifier on the artifact — `com.owspfm:elwha:1.1.0:javadoc` — which most IDEs will
attach automatically once the dependency resolves.

## The other published artifacts

From 1.1.0 on the repository publishes three coordinates. The library is `com.owspfm:elwha`, and
it is the only coordinate your build ever names — the other two exist for specific jobs:

**`com.owspfm:elwha-parent`** is the reactor parent pom that the `elwha` pom references. Maven
resolves it automatically through the same repository and credentials as the jar; you never
declare it. It matters only if you *mirror* the repository into a repository manager (Nexus,
Artifactory, …): mirror all three coordinates, or the library pom will fail to resolve its parent.

**`com.owspfm:elwha` with the `tests` classifier** is an optional jar carrying the shared test
fixtures (the `com.owspfm.elwha.testkit` package — theme-installing JUnit extensions, EDT
interception, pixel probes), published from 1.1.0 on for consumers who want to reuse them in their
own suites. Test scope only:

```xml
<dependency>
  <groupId>com.owspfm</groupId>
  <artifactId>elwha</artifactId>
  <version>1.1.0</version>
  <type>test-jar</type>
  <scope>test</scope>
</dependency>
```

The testkit asserts through **JUnit Jupiter and AssertJ**, and a classifier jar carries no
dependency metadata of its own — without both on your test classpath, the first fixture use fails
with `NoClassDefFoundError: org/assertj/core/api/Assertions` (or the JUnit equivalent). Declare
them alongside it, at or near the versions Elwha builds against:

```xml
<dependency>
  <groupId>org.junit.jupiter</groupId>
  <artifactId>junit-jupiter</artifactId>
  <version>6.1.2</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.assertj</groupId>
  <artifactId>assertj-core</artifactId>
  <version>3.27.7</version>
  <scope>test</scope>
</dependency>
```

A suite already on JUnit 5/6 + AssertJ needs nothing extra — these are the stack the testkit was
extracted from, not additions to it.

**`com.owspfm:elwha-showcase`** is the demo storefront — an application, not a library, and not
covered by the [stability policy](stability.md#the-elwha-showcase-artifact). To evaluate Elwha you
do not need it from Packages at all: download the self-contained
`elwha-showcase-<version>-app.jar` from the
[releases page](https://github.com/OWS-PFMS/elwha/releases) (attached to every release from 1.1.0
on) and `java -jar` it — no token required.

## Troubleshooting

**`401 Unauthorized` fetching `elwha-1.1.0.pom`.** Maven is sending no credentials or wrong ones.
Check, in order: the `<server>` `<id>` matches the `<repository>` `<id>`; the username is your
GitHub login, not your email; the token has not expired. Run with `-X` and look for
`Using authentication for server github-elwha` — if that line is absent, the ids do not match.

**`403 Forbidden`.** The credentials were accepted but the token lacks `read:packages`, or a
fine-grained token was scoped to the wrong organization. Regenerate with the right scope.

**`Could not find artifact com.owspfm:elwha:1.1.0`.** Maven never reached GitHub Packages. Confirm
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
    implementation("com.owspfm:elwha:1.1.0")
}
```
