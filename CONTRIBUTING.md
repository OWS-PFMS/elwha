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

### Java version

The library targets **JDK 21** (`maven.compiler.release=21`). You can build with a newer JDK (e.g., JDK 25), but bytecode is constrained to Java 21 so the library remains consumable by JDK 21 consumers.

## Pull request style

- One focused change per PR. Refactors, bug fixes, and new features should not be bundled.
- Commit messages follow Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`, `chore:`, `test:`).
- For new public API, include javadoc and a usage example (in the javadoc or in the relevant playground).
- For visual changes, include a before/after screenshot in the PR description.

### Javadoc tags

Every public class / method should have:

- `@author` — credit
- `@version` — current library version when the entity was last touched
- `@since` — library version when the entity was introduced

The `@version` tag is bumped on every change; the `@since` tag is set once and never moves. (This matches the convention used in the parent OWS-Local-Search-GUI project.)

## Versioning

Pre-1.0:

- **0.x.y** — API may change between minor versions
- Breaking changes are explicitly documented in `CHANGELOG.md`

Post-1.0:

- **major** — breaking API change
- **minor** — new public API, no breaks
- **patch** — bug fix, no API change

## Release process

(Internal — for maintainers)

1. Update `version` in `pom.xml` and `CHANGELOG.md` `[Unreleased]` → `[X.Y.Z]`
2. Commit: `chore: release X.Y.Z`
3. Tag: `git tag -a vX.Y.Z -m "Release X.Y.Z"`
4. Push tag: `git push origin vX.Y.Z`
5. The GitHub Actions release workflow publishes to GitHub Packages on tag push.

## Code of conduct

Be kind, be specific, be patient. We're all working with Swing in 2026 — solidarity matters.
