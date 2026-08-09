# Release runbook

How to cut an Elwha release. Written for 1.0.0 — the first publish since `0.1.0` and the API
freeze — but the mechanics are the same for every tag after it.

The whole thing is meant to be a five-minute mechanical act. Everything that required judgement was
done ahead of it, in the PRs that landed on `main`. If you find yourself making a decision while
following this page, stop: the decision belongs in a PR, not in a release commit.

## Contents

1. [What the release actually is](#1-what-the-release-actually-is)
2. [Pre-flight](#2-pre-flight)
3. [The release commit](#3-the-release-commit)
4. [Tag and push](#4-tag-and-push)
5. [What the publish workflow checks](#5-what-the-publish-workflow-checks)
6. [Post-publish verification](#6-post-publish-verification)
7. [Close the milestone](#7-close-the-milestone)
8. [If something goes wrong](#8-if-something-goes-wrong)
9. [1.0.0-specific notes](#9-100-specific-notes)

---

## 1. What the release actually is

Publishing is tag-driven. Pushing a `v*` tag runs `.github/workflows/publish.yml`, which validates
the tag against the repository and then runs `mvn -B clean deploy` to GitHub Packages. There is no
manual upload step and no release branch — `main` is what ships.

Two commits' worth of work sit either side of the tag, and they are deliberately separate:

| | Where it lives | Who does it |
|---|---|---|
| Everything a reader of the release sees — CHANGELOG content, `CLAUDE.md`, `README.md`, docs | Ordinary PRs merged to `main` beforehand | Whoever does the work |
| `pom.xml` version + the `[Unreleased]` → `[X.Y.Z]` flip | **One commit, made directly on `main` at release time** | The maintainer cutting the release |

Keeping the version bump out of the content PRs is what makes this page short. A content PR that
also bumped `pom.xml` would be unmergeable the moment a second one appeared.

## 2. Pre-flight

Run through this before touching anything. Each line is a command, not a vibe.

**Everything is merged.**

```bash
gh pr list --state open --json number,title,milestone
```

No open PR should carry the milestone you are about to release. (Open PRs on a *later* milestone
are fine — they simply are not in this release.)

**The milestone is empty.**

```bash
gh api repos/OWS-PFMS/elwha/milestones --jq '.[] | "\(.title)\topen=\(.open_issues)\tclosed=\(.closed_issues)"'
```

The milestone being released should read `open=0`, with two allowed exceptions: the release chore
itself and the epic it closes. Both close *after* the tag publishes — see §7. Anything else still
open either ships in this release (finish it) or does not (move it to the next milestone).

**`main` is green and you are on it.**

```bash
git checkout main && git pull
gh run list --branch main --limit 5
```

All five required checks must be passing on the head commit: `build`, `Test (components +
Showcase)`, `Validate @version and @since tags`, `Validate formatting (Spotless)`, `Validate naming
(Checkstyle)`.

**The build passes locally on JDK 21.**

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn -B clean verify
```

JDK 21 is not advisory — Spotless' google-java-format calls javac internals and dies on newer JDKs
with a `NoSuchMethodError` that looks unrelated to your change. direnv handles this in an
interactive shell; a script or an agent must export `JAVA_HOME` explicitly. See *The build runs on
JDK 21* in `CLAUDE.md`.

**The Showcase launches and renders.**

```bash
mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.showcase.ElwhaShowcase"
```

Click through the three areas. Nothing in the test suite renders to a screen a human looks at, so
this is the only check that the storefront is not visually broken.

**`NOTICE` is accurate.** Every bundled third-party asset is attributed and every path in it
resolves. Licence text that contradicts `NOTICE` is a release blocker, not a follow-up.

**The CHANGELOG's `[Unreleased]` section is release-readable.** It should be organised by
category with breaking changes first — not a raw append-only log. Reorganising it is content work:
do it in a PR *before* the release commit, never inside it.

## 3. The release commit

One commit, made directly on `main`, containing exactly three edits.

`main` is protected with required status checks, but `enforce_admins` is off, so a repository admin
can push this commit straight to `main`. A non-admin maintainer cannot and will need to route it
through a PR — which is fine, just slower: the same three edits, milestoned `v1.0.0`, merged before
tagging.

**1. `pom.xml`** — the project version. There is no parent POM, so it is the **first** `<version>`
element in the file, directly under `<artifactId>elwha</artifactId>` (line 9 at the time of
writing); every later one belongs to a dependency or a plugin. Confirm before editing:

```bash
grep -n "<version>" pom.xml | head -3
```

Set it to the bare number, no `v` prefix: `1.0.0`.

**2. `CHANGELOG.md`** — rename the heading and add the date, then open a fresh empty
`[Unreleased]` above it:

```markdown
## [Unreleased]

## [1.0.0] — 2026-08-08
```

Update the link definitions at the bottom of the file:

```markdown
[Unreleased]: https://github.com/OWS-PFMS/elwha/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/OWS-PFMS/elwha/releases/tag/v1.0.0
[0.1.0]: https://github.com/OWS-PFMS/elwha/releases/tag/v0.1.0
```

The heading format matters: `publish.yml` greps for the literal string `## [1.0.0]`. The em-dash
and the date after it are free-form and are not matched.

**3. `CLAUDE.md`** — the *Version, carefully* paragraph in *What this repo is* opens by saying
`pom.xml` reads the last published release and is not a description of `main`. After the bump those
two agree, so rewrite the paragraph to say so and drop the four-unreleased-waves framing.

Then:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn -B clean verify          # the version bump must not break the build
git add pom.xml CHANGELOG.md CLAUDE.md
git commit -m "chore: release 1.0.0"
git push origin main
```

Push the commit and let `main`'s checks go green **before** tagging. A tag on a red commit publishes
a red artifact.

## 4. Tag and push

```bash
git tag -a v1.0.0 -m "Release 1.0.0 — API stability milestone"
git push origin v1.0.0
```

The tag is annotated (`-a`) and carries the `v` prefix; `pom.xml` does not. That mismatch is
intentional and the workflow strips the prefix before comparing.

Watch it:

```bash
gh run watch "$(gh run list --workflow publish.yml --limit 1 --json databaseId --jq '.[0].databaseId')"
```

## 5. What the publish workflow checks

`.github/workflows/publish.yml` triggers on `push` of any `v*` tag and runs three steps that can
fail, in order. Knowing which one failed tells you exactly what to fix.

| Step | What it does | Fails when |
|---|---|---|
| **Verify tag matches pom.xml version** | Strips `refs/tags/v` off the ref and string-compares against `${project.version}` | You tagged before committing the bump, or tagged the wrong number |
| **Verify CHANGELOG entry exists** | `grep -q "## \[${TAG_VERSION}\]" CHANGELOG.md` | You forgot the `[Unreleased]` → `[1.0.0]` flip, or the heading is malformed |
| **Build and publish** | `mvn -B clean deploy -DskipTests` on temurin 21, authenticating to GitHub Packages with `GITHUB_TOKEN` | The build itself breaks — which pre-flight should already have caught |

Tests are deliberately skipped in the deploy (#764): the required `Test` workflow already gated the
identical tree on `main`, and the gui tier needs display scaffolding this workflow doesn't carry —
the first 1.0.0 tag push failed on exactly that. A tag also runs the workflow file **at its own
ref**, so a publish-workflow fix only takes effect once the tag points at a commit containing it.

Note what it does **not** check: that `main` was green, that the milestone was empty, or that the
CHANGELOG says anything useful. Those are this page's job.

The deploy publishes three artifacts — the jar, `-sources`, and `-javadoc` — to
`https://maven.pkg.github.com/OWS-PFMS/elwha`.

## 6. Post-publish verification

**The GitHub Packages artifact.** It should list the new version, with all three jars. The Maven
package name is `groupId.artifactId`:

```bash
gh api "/orgs/OWS-PFMS/packages/maven/com.owspfm.elwha/versions" --jq '.[].name'
```

A `403 — You need at least read:packages scope` here is about **your token**, not the release. Re-run
with `gh auth refresh -s read:packages`, or just read the Packages tab in the browser.

Then confirm a consumer can actually resolve it. GitHub Packages requires authentication even for
public packages, so this needs a `~/.m2/settings.xml` server entry with a PAT carrying
`read:packages` — the same setup the consumer docs describe:

```bash
mvn dependency:get -DremoteRepositories=github::::https://maven.pkg.github.com/OWS-PFMS/elwha \
  -Dartifact=com.owspfm:elwha:1.0.0
```

**The Pages deploy.** `pages.yml` runs on every push to `main`, so the release commit triggers its
own deploy. The published javadoc should show the new version:

```bash
gh run list --workflow pages.yml --limit 1
```

Open the Pages URL and spot-check one class page — `ElwhaCard` or `ElwhaItemList` — for the bumped
`@version`.

**The consumer docs coordinate.** The install snippet in `README.md` and under `docs/consumer/`
carries a hardcoded `<version>`. Confirm it names the version you just published, not the previous
one. If a consumer-docs PR landed before the release with a forward-looking version, this is where
that lie becomes visible.

**A release on GitHub.** The workflow does not create one; the tag alone is enough for Maven. If
you want release notes on the Releases page, create it from the tag with the CHANGELOG section as
the body:

```bash
gh release create v1.0.0 --title "1.0.0" --notes-file <(sed -n '/## \[1.0.0\]/,/## \[0.1.0\]/p' CHANGELOG.md)
```

## 7. Close the milestone

Only now — after the artifact is real and resolvable.

```bash
gh issue close 97 --comment "Released 1.0.0; artifact published to GitHub Packages."
gh issue close 80 --comment "Card V3 complete and shipped in 1.0.0."
gh api -X PATCH repos/OWS-PFMS/elwha/milestones/<id> -f state=closed
```

Then move the corresponding cards to Done on
[Project #5](https://github.com/orgs/OWS-PFMS/projects/5).

## 8. If something goes wrong

**A validation step failed and nothing published.** Nothing is public yet, so this is cheap. Delete
the tag locally and remotely, fix the commit, tag again:

```bash
git tag -d v1.0.0
git push origin :refs/tags/v1.0.0
```

**The deploy failed halfway.** Re-run the workflow from the Actions UI. `mvn deploy` is idempotent
for a version that did not fully publish.

**The artifact published and is wrong.** Do not delete the published version and do not re-tag —
a consumer may already have resolved it, and re-publishing the same coordinate with different bytes
is the one genuinely unrecoverable mistake here. Cut a patch release instead: fix on `main`, bump
to `1.0.1`, and run this page again.

**A tag was pushed to the wrong commit.** Same rule. If it did not publish, delete and re-tag; if
it published, roll forward.

## 9. 1.0.0-specific notes

- **1.0.0 is the API freeze.** Every release after it is governed by semver: breaking changes need a
  major bump, minors are additive only, patches are fixes. The pre-1.0 licence to break API freely,
  and the "no backwards-compat shims" rule that went with it, both expire at this tag.
- **The `[Unreleased]` section is four waves deep.** `v0.2.0` was cancelled and `v0.3.0` / `v0.4.0`
  were planning waves that were never cut, so 1.0.0 carries everything since `0.1.0`. Its CHANGELOG
  section is correspondingly large — that is expected, not a sign something went wrong.
- **There is no consumer mid-migration to protect.** OWS never adopted a published V1 card; 1.0.0 is
  its initial adoption. That is why `card/v1` could be deleted outright rather than deprecated.
- **`docs/handoff/elwha-v1-roadmap-handoff.md` §11** is the definition of done that this release
  discharges. Read it once before starting if you want the full list in the form it was agreed.

## Related

- `CLAUDE.md` — *Release process* (the five-line summary), *Milestones*, *Version state & release*
- `docs/development/changelog-policy.md` — what earns a CHANGELOG entry and in which category
- `docs/development/versioning.md` — the `@version` / `@since` convention the release gate enforces
- `.github/workflows/publish.yml` — the workflow this page describes
