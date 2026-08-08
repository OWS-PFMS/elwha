# Contributing to Elwha

Thanks for considering a contribution! Elwha is in early development (pre-1.0), so the contribution process is intentionally lightweight.

## Issue tracker

Bugs, feature requests, and design discussions: [GitHub Issues](https://github.com/OWS-PFMS/elwha/issues).

When filing an issue, include:

- Java + FlatLaf version
- A minimal reproducer (the playground apps are a good base — `ElwhaChipPlayground`, `ElwhaCardPlayground`)
- Screenshot or screen recording for visual bugs

## Development

### Build

```bash
mvn clean package
```

Produces `target/elwha-<version>.jar`, `elwha-<version>-sources.jar`, and `elwha-<version>-javadoc.jar`.

### Run a playground

```bash
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.chip.ElwhaChipPlayground"
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.card.playground.ElwhaCardPlayground"
```

### Tests

```bash
mvn verify
```

Runs the full suite — ~4,570 tests in two tiers, plus Spotless and Checkstyle. The default tier is
headless; a `@Tag("gui")` tier runs in its own forked JVM for the things headless cannot represent
(real focus ownership, `Robot` input, window realization). A behavior change should land with a
test. Read [`docs/development/testing.md`](docs/development/testing.md) first — its determinism
rules are what keeps the suite flake-free.

### Java version

**Build on JDK 21.** The library targets JDK 21 (`maven.compiler.release=21`), and while that
setting constrains *bytecode* regardless of which JVM runs Maven, the build itself is not
JDK-agnostic: Spotless' google-java-format calls javac internals and dies on JDK 25 with
`NoSuchMethodError: Log$DeferredDiagnosticHandler.getDiagnostics()`. It is a signature change, so
`--add-exports` cannot work around it. Plain `mvn compile` succeeds on any JDK — only the Spotless
step breaks, which makes the failure look unrelated to your change. All CI workflows pin temurin 21.

The repo ships an `.envrc`, so with [direnv](https://direnv.net) installed, entering the directory
pins `JAVA_HOME` for you (`direnv allow .` on first use) and leaving it reverts, letting a newer JDK
stay your global default. Otherwise:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn verify
```

## Pull request style

- One focused change per PR. Refactors, bug fixes, and new features should not be bundled.
- Commit messages follow Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`, `chore:`, `test:`).
- **Set a milestone when you open the PR.** The `@version` validation workflow reads it and hard-fails without one.
- For new public API, include javadoc and a usage example (in the javadoc or in the relevant playground).
- For visual changes, include a before/after screenshot in the PR description.

### Javadoc tags

Every public class / method should have:

- `@author` — credit
- `@version` — current library version when the entity was last touched
- `@since` — library version when the entity was introduced

The `@version` tag is bumped on every change; the `@since` tag is set once and never moves. (This matches the convention used in the parent OWS-Local-Search-GUI project.)

## Versioning

**1.0.0 is the API freeze.** From that release on, semver applies in full:

- **major** — breaking API change
- **minor** — new public API, no breaks
- **patch** — bug fix, no API change

Before 1.0.0 the rules were looser — a `0.x` minor bump could break API — which is why the
`[Unreleased]` history in `CHANGELOG.md` carries breaking entries that no published release ever
exposed. Every change, breaking or not, is documented in `CHANGELOG.md`.

## Release process

(Internal — for maintainers)

1. Update `version` in `pom.xml` and `CHANGELOG.md` `[Unreleased]` → `[X.Y.Z]`
2. Commit: `chore: release X.Y.Z`
3. Tag: `git tag -a vX.Y.Z -m "Release X.Y.Z"`
4. Push tag: `git push origin vX.Y.Z`
5. The GitHub Actions release workflow publishes to GitHub Packages on tag push.

[`docs/development/release-runbook.md`](docs/development/release-runbook.md) is the full procedure —
pre-flight checks, what the publish workflow validates, and what to verify afterwards. Use it rather
than these five lines when actually cutting a release.

## Code of conduct

Be kind, be specific, be patient. We're all working with Swing in 2026 — solidarity matters.
