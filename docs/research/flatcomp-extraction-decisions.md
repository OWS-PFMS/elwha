# FlatComp Extraction — Locked Decisions

**Deliverable for:** [issue #259](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/259) — strategic decisions for the FlatComp library extraction.

**Part of epic:** [#231 — Extract `com.owspfm.ui.components` into a standalone library](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/231)

**Locked date:** 2026-05-12

This document is the authoritative source of strategic decisions for the extraction. Sub-issues [#262](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/262)–[#265](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/265) consume these choices. Decisions are not to be re-debated during execution — any change requires reopening this document with rationale.

---

## Summary table

| # | Decision | Locked value |
|---|---|---|
| 1 | Repo strategy | **Separate GitHub repo: `OWS-PFMS/flatcomp`** |
| 2 | Library name | **`flatcomp`** |
| 3 | Group ID | **`com.owspfm`** |
| 4 | License | **Apache 2.0** |
| 5 | Build tool | **Maven** *(corrected 2026-05-12 — pre-flight discovered OWS-tool uses Maven, not Gradle)* |
| 6 | Publish target | **GitHub Packages** *(reaffirmed — OWS-tool uses AWS CodeArtifact privately, but Apache 2.0 lib intent points at the public-readable GH Packages)* |
| 7 | JDK target | **JDK 21** (migration to 25 planned, deferred) |
| 8 | Initial version | **0.1.0** (semver, 0.x = API not yet stable) |
| 9 | Git history preservation | **Yes**, via `git filter-repo --subdirectory-filter` |
| 10 | Issue tracker | New repo's own Issues; project #5 (org-level) attaches |

---

## 1. Repo strategy — separate GitHub repo

**Locked:** Create a new repo at `OWS-PFMS/flatcomp`.

**Why:** Clean dependency boundary. A separate repo physically prevents accidental re-coupling between the library and the OWS-tool app — there is no "just one import from `com.owspfm.cycle...`" temptation when the source code isn't even present. Independent semver and release cadence follow naturally.

**Trade-off accepted:** more GitHub admin overhead (branch protection, secrets, workflows, label set) per the bootstrap work in [#262](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/262). Worth it given the stated goal of reusing the library across multiple projects.

**Alternative considered and rejected:** Gradle sub-project inside `OWS-Local-Search-GUI`. Cheaper to set up, but coupling can sneak back in via shared classpath and shared build context. Rejected for the same reason a separate repo is preferred.

## 2. Library name — `flatcomp`

**Locked:** `flatcomp`.

**Why:** Short, descriptive, matches the internal vocabulary already used in this codebase ("FlatComp components"). Brandable enough to be the canonical name in `build.gradle` snippets.

**Alternatives considered:** `flatswing` (more generic, weaker brand identity), `owswing` (ties name to OWS — bad for reusability outside OWS).

## 3. Group ID — `com.owspfm`

**Locked:** `com.owspfm`.

**Why:** Matches existing Java package namespace (`com.owspfm.ui.components`), reuses the owned `owspfm.com` domain.

**Caveat:** If the lib is ever published to **Maven Central** (not just GitHub Packages), Maven Central requires verified ownership of the domain in the group ID. `com.owspfm` is fine because `owspfm.com` is owned. If publishing to Maven Central is ever pursued, no group ID change is needed.

## 4. License — Apache 2.0

**Locked:** Apache 2.0.

**Why:**

- **Permissive** — allows commercial use, sublicensing, modification, and distribution without forcing downstream code to be open-sourced. The OWS-tool app remains proprietary and commercially licensable while consuming this library.
- **Patent grant** — Apache 2.0 includes an explicit patent license from contributors, protecting both the library author and consumers. MIT does not include this protection.
- **Industry standard** — the dominant license for Swing/Java component libraries (FlatLaf itself is Apache 2.0). Familiar to any Java developer who might contribute.

**Compatibility with selling OWS commercially:** Apache 2.0 is explicitly designed to support exactly this pattern. The OWS-tool app can be sold under a commercial proprietary license while bundling Apache 2.0 libraries. The only obligations are: (1) include the Apache 2.0 `LICENSE` file in distributions, (2) include a `NOTICE` file if the library has one, (3) preserve copyright notices.

**Secondary consideration acknowledged:** Apache 2.0 means the *library itself* is open-source — competitors could theoretically build a competing analysis tool using the same Swing components. **Decision: accept this risk.** The competitive advantage of OWS-tool is in the cycle/MICMAC analysis domain logic and UX, not in the Swing primitive components. Open-sourcing the components also produces collateral benefits: third-party contributions, portfolio value, and credibility.

**Licenses considered and rejected:**
- **MIT** — equally permissive but lacks the patent grant; net slightly worse.
- **GPL / AGPL** — copyleft; would force OWS-tool itself to be GPL. Incompatible with commercial sale plans.
- **EPL-2.0** — weak copyleft (modifications to the lib must be open-sourced, but downstream consumers can be proprietary). Acceptable middle-ground but adds friction without clear benefit over Apache 2.0.
- **Proprietary / closed-source** — would maximize control but eliminate community goodwill, contributor potential, and portfolio value. The components are not the competitive advantage that would justify this.

## 5. Build tool — Maven

**Locked:** Maven (corrected from initial Gradle recommendation).

**Why:** Matches `OWS-Local-Search-GUI`'s existing build. The initial recommendation of Gradle was based on a faulty assumption that OWS-tool used Gradle; pre-flight inspection found `pom.xml` at the repo root, not `build.gradle`. Switching the lib to Maven removes the cognitive overhead of maintaining two different build tools across the lib and its primary consumer.

**Maven version:** 3.9+ (per OWS-tool's `BUILDING.md`).

**Note for future review:** Gradle remains a defensible choice for a lib (more expressive publishing block, easier Maven Central setup) — if at some future point the lib gains many external contributors or needs richer build-time customization, the move from Maven to Gradle is a one-shot conversion. For now, Maven matches the operator's existing skill set and the consumer's existing build.

## 6. Publish target — GitHub Packages

**Locked:** GitHub Packages (`maven.pkg.github.com/OWS-PFMS/flatcomp`).

**Why:**

- Free for public repos, free up to generous limits for private.
- Tightly integrated with the repo — no separate account setup, no domain verification.
- Authentication via GitHub tokens (already in use for CI).
- Easy upgrade path to Maven Central later if discoverability becomes a goal (decision is reversible).

**Alternative considered:** Maven Central. Better discoverability for general public consumers, but requires Sonatype account, domain verification, GPG signing, and staging-release workflow. **Overhead not justified** at this stage — the library has one known consumer (OWS-tool). Revisit if it gains external consumers.

**Pre-flight note (2026-05-12):** Inspection of OWS-tool's `pom.xml` found an existing AWS CodeArtifact distribution repo (`owspfm-616696950374.d.codeartifact.us-west-2.amazonaws.com`). The lib could share that infra for tooling consistency, but **CodeArtifact requires AWS credentials to read** — incompatible with the Apache 2.0 lib's intent of being broadly consumable. GitHub Packages stays as the locked choice; CodeArtifact remains in scope only for OWS-tool's own private artifacts.

## 7. JDK target — JDK 21

**Locked:** JDK 21 (`--release 21`, bytecode version 65).

**Why:** OWS-tool currently runs on JDK 21. Java bytecode is forward-compatible (older bytecode runs on newer JVMs) but **not backward-compatible** (newer bytecode does NOT run on older JVMs). Pinning the library at the lowest version among consumers (today: only JDK 21) ensures it works on every consumer regardless of their runtime version.

**Future migration to JDK 25:** OWS-tool is planned to migrate to JDK 25 at some future date. When that migration completes *and* no other JDK-21-pinned consumers remain, the library's `--release` flag can be bumped to 25 — a one-line `build.gradle` change. Until then, the lib stays at 21, restricted to Java 21 language and API features.

**Why not JDK 25 now:** A library compiled with `--release 25` produces bytecode version 69. Loading that into a JDK 21 runtime throws `UnsupportedClassVersionError` at class-load time. JDK 25 lib + JDK 21 consumer is structurally impossible without multi-release JARs (overkill for this lib).

## 8. Initial version — 0.1.0

**Locked:** First publish at `0.1.0`.

**Why:** Semantic versioning convention. `0.x.y` indicates "API not yet stable; breaking changes are permitted between minor versions." This is honest given that two breaking-change epics are queued post-extraction: [#251](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/251) (FlatPill → FlatChip rename) and [#252](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/252) (FlatList<T> selection-surface extension).

**Path to 1.0.0:** Bump to `1.0.0` only after (a) the rename is complete, (b) the selection surface is stabilized, (c) [#253](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/253) (FlatCard V2) is complete. Until then, expect `0.2.x`, `0.3.x`, etc. as those epics land.

## 9. Git history preservation — yes

**Locked:** Preserve the components' commit history into the new repo using `git filter-repo --subdirectory-filter src/com/owspfm/ui/components/`.

**Why:** The components have meaningful history — Epic [#230](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/230) alone produced 13 sub-issue PRs over the past month with detailed commit messages. Blame archaeology stays useful (clicking a line in `FlatPill.java` still leads back to the PR that introduced it). One-shot work, ~30 minutes of careful execution during sub-issue [#263](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/263).

**Method:** `git filter-repo` (modern, supported tool) over `git subtree split` (older, slower). Run on a fresh clone of `OWS-Local-Search-GUI` so the operation can't corrupt the working repo.

**Caveat:** History rewrite means new commit SHAs in the lib repo. References from the OWS-tool repo to specific commits in the lib's history will not resolve. Cross-reference by PR / issue URL instead. CHANGELOG entries pointing to the original PRs (e.g., `(#244)`) remain valuable as the canonical narrative source.

## 10. Issue tracker + project board

**Locked:**
- New repo `OWS-PFMS/flatcomp` gets its **own GitHub Issues**.
- Org-level project board **#5 (Material Flat Component Library)** attaches to the new repo as the canonical tracking surface going forward.
- Label set on the new repo mirrors `OWS-Local-Search-GUI`'s relevant labels: `technical-debt`, `developer-experience`, `enhancement`, `bug`, plus any FlatComp-specific labels that emerge during work.
- Sub-issues currently filed on `OWS-Local-Search-GUI` and parked on project #5 ([#251](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/251), [#252](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/252), [#253](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/253), and the 5 sub-issues of #251 — [#254](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/254)–[#258](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/258)) will be **transferred** to the new repo via `gh issue transfer` during sub-issue [#263](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/263) or [#264](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/264). Transfer preserves project board membership; numbers and issue types will need to be re-set on the new repo (a known cost, documented).

**Why:**
- An independent issue tracker keeps lib bugs/features from cluttering OWS-tool's queue, and vice versa.
- The org-level project board persists across the transfer — the board doesn't need to be rebuilt.

---

## Cross-references to other deliverables

- **Coupling audit** (sub-issue [#260](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/260)): `docs/research/flatcomp-coupling-audit.md` — found zero coupling sites; library is extraction-ready as-is. Sub-issue [#261](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/261) (break couplings) closed as **not required**.
- **Updated critical path** for the epic:
  `#259 + #260` ✅ → ~~#261~~ → `#262 (bootstrap repo)` → `#263 (migrate + first publish)` → `#264 (OWS-tool consumes)` → `#265 (CI + docs)`
