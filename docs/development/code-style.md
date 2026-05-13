# Code Style Guide

FlatComp follows the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html), enforced automatically on every PR.

## What's enforced

| Concern | Tool | Config | Where |
|---|---|---|---|
| Formatting (indent, line length, brace style, import order, line-wrap reflow) | Spotless + `googleJavaFormat` | bundled `GOOGLE` style | `pom.xml` |
| Naming (members, parameters, locals, statics, constants, classes, methods, types, packages) | Checkstyle | `config/checkstyle/checkstyle.xml` | project-local |
| Project-specific ban on `my*` / `the*` identifier prefixes | Checkstyle | same | enforced on top of Google naming patterns |

The two tools are scoped to **non-overlapping concerns** — Spotless owns formatting, Checkstyle owns naming. Don't duplicate rules between them.

## Running the checkers locally

```bash
mvn spotless:check       # check formatting (read-only)
mvn spotless:apply       # fix formatting in place
mvn checkstyle:check     # check naming (read-only)
mvn verify               # run everything: compile + spotless:check + checkstyle:check + tests + package
```

### JDK version requirement

**Spotless + `googleJavaFormat` require JDK 21 to run.** Newer JDKs (22+) hit a binary-API drift in `com.sun.tools.javac.util.Log` (`DeferredDiagnosticHandler.getDiagnostics()` return type changed from `Queue` to `List`). Tracked upstream at [diffplug/spotless#2468](https://github.com/diffplug/spotless/issues/2468). The library itself targets JDK 21 (`maven.compiler.release=21`) regardless of runtime, so this only affects which JDK runs `mvn spotless:*`.

Quick fix if you're on a newer JDK:

```bash
export JAVA_HOME=/path/to/jdk-21
mvn spotless:apply
```

Checkstyle is JDK-version-agnostic — runs on whatever JDK launched Maven.

## The `my*` / `the*` prefix ban

Earlier FlatComp code (inherited from the OWS-tool source it was extracted from) used `my*` for instance fields and `the*` for parameters/locals. Google Java Style doesn't address these specifically — they're syntactically `lowerCamelCase` and would pass vanilla `google_checks.xml`. The ban is a project-specific addition layered on top.

Don't introduce identifiers that start with `my` or `the` followed by an uppercase letter. The Checkstyle config will reject them.

```java
// ❌
private int myCount;
public void setCount(int theCount) { myCount = theCount; }

// ✅
private int count;
public void setCount(int count) { this.count = count; }
```

When a parameter name shadows a field, qualify the field with `this.`. Don't reintroduce the `my` / `the` workaround.

## CI gate

The [`Validate Style` workflow](../../.github/workflows/validate-style.yml) runs on every PR to `main` (and every push to `main`) and hard-fails on any violation. Two jobs:

- **`Validate formatting (Spotless)`** — runs `mvn spotless:check`.
- **`Validate naming (Checkstyle)`** — runs `mvn checkstyle:check`.

Both are required for merge per `main`'s branch-protection rules.

## Related

- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) — the authoritative source for everything Spotless enforces.
- [`versioning.md`](versioning.md) — `@version` / `@since` tagging conventions.
- [`changelog-policy.md`](changelog-policy.md) — when to log a change in `CHANGELOG.md`.
- [`CLAUDE.md`](../../CLAUDE.md) — repository conventions overview.
