# Issue #60 — Verification Checklist

Single source of truth for the `/story:verify 60` pass that gates
`/story:close 60`. Every item must pass with the stated evidence.

---

## A. Source tree moved

- [ ] **A1.** `src/com` no longer exists.
  *Evidence:* `test ! -d src/com && echo OK` prints `OK`.
- [ ] **A2.** All 134 Java files now live under `src/main/java/com/owspfm/...`.
  *Evidence:* `find src/main/java/com -name '*.java' | wc -l` prints `134`.
- [ ] **A3.** The move was a pure rename — no `.java` content was edited.
  *Evidence:* `git diff --name-status -M origin/main...HEAD -- 'src/**/*.java'`
  shows every entry as `R100` (134 lines); zero `M`, `A`, or `R<100`.
- [ ] **A4.** `src/main/resources/` is untouched (still standard, not moved).
  *Evidence:* `ls src/main` shows exactly `java` and `resources`; resources
  tree unchanged in `git status`.
  *Ties to:* acceptance criterion "All sources moved" + "Out of scope: resources".

## B. pom.xml

- [ ] **B1.** `<sourceDirectory>` override is gone from the `<build>` block.
  *Evidence:* `grep -n 'sourceDirectory' pom.xml` shows no `<build>`-level
  `<sourceDirectory>` (only the Checkstyle `<sourceDirectories>` entry remains).
  *Ties to:* acceptance criterion "`<sourceDirectory>` removed from pom.xml".
- [ ] **B2.** Spotless `<include>` points at the new path.
  *Evidence:* `grep -n 'src/main/java/com/owspfm' pom.xml` shows the Spotless
  include; `grep 'src/com/owspfm' pom.xml` returns nothing.
- [ ] **B3.** Checkstyle `<sourceDirectory>` points at `src/main/java`.
  *Evidence:* the Checkstyle plugin config reads
  `${project.basedir}/src/main/java`.
- [ ] **B4.** Spotless still actually enforces formatting (not vacuously
  passing). *Evidence:* `mvn spotless:check` reports a non-zero file count
  scanned, or an intentional bad-format probe is caught then reverted.

## C. Build & static checks

- [ ] **C1.** `mvn clean verify` exits 0 (compile + Spotless + Checkstyle all
  green from the new layout). *Evidence:* terminal shows `BUILD SUCCESS`.
  *Ties to:* acceptance criterion "`mvn clean verify` green".
- [ ] **C2.** The built `target/elwha-*.jar` + sources jar + javadoc jar are
  produced. *Evidence:* `ls target/elwha-*.jar`.

## D. Playgrounds

- [ ] **D1–D5.** Each of the 5 playgrounds launches via the standard
  `mvn compile exec:java -Dexec.mainClass="..."` invocation and opens its
  window without exception:
  - [ ] `com.owspfm.elwha.theme.playground.ThemePlayground`
  - [ ] `com.owspfm.elwha.card.playground.ElwhaCardPlayground`
  - [ ] `com.owspfm.elwha.chip.ElwhaChipPlayground`
  - [ ] `com.owspfm.elwha.surface.playground.ElwhaSurfacePlayground`
  - [ ] `com.owspfm.elwha.iconbutton.playground.ElwhaIconButtonPlayground`
  *Evidence:* each run reaches the Swing window with no stack trace on stdout/err.
  *Ties to:* acceptance criterion "All five playgrounds launch".

## E. Blame preservation

- [ ] **E1.** `git log --follow` traverses the rename for a migrated file.
  *Evidence:* `git log --follow --oneline src/main/java/com/owspfm/elwha/chip/ElwhaChip.java`
  lists commits dated **before** the migration commit.
  *Ties to:* acceptance criterion "`git log --follow` works ... blame preserved".

## F. Version-validator fix

- [ ] **F1.** `changed_java_files()` in `scripts/update_javadoc_version.py` is
  rename-aware: pure `R100` renames are excluded from both returned lists;
  `R<100` → modified; `A`/`C` → added; `M` → modified; `D` excluded.
  *Evidence:* code review of the function.
- [ ] **F2.** The script still parses / runs.
  *Evidence:* `python3 -c "import ast; ast.parse(open('scripts/update_javadoc_version.py').read())"`
  exits 0; the F3 invocation runs without a Python traceback.
- [ ] **F3.** Local validator dry-run reports **0 violations** for the migration
  branch. *Evidence:* `git fetch origin main` then
  `python3 scripts/update_javadoc_version.py --check --changed-only
  --expected v0.2.0 --base-ref origin/main` prints success / 0 violations.
  *Rationale:* without the Phase 1 fix this would report ~134 violations.

## G. CI on the PR

- [ ] **G1.** `build` check green.
- [ ] **G2.** `Validate @version and @since tags` check green (proves F1–F3
  hold under CI, no `src/com` assumptions in the version tooling).
- [ ] **G3.** `Validate formatting (Spotless)` check green.
- [ ] **G4.** `Validate naming (Checkstyle)` check green.
  *Ties to:* acceptance criterion "CI workflows green on the migration PR".

## H. Docs & CLAUDE.md

- [ ] **H1.** CLAUDE.md `## Source layout` section is rewritten to its final
  standard-layout state — no "non-standard" framing, no "#60 tracks the
  migration" sentence; states sources at `src/main/java/com/owspfm/...`.
  *Ties to:* acceptance criterion "CLAUDE.md `## Source layout` section updated".
- [ ] **H2.** `docs/migration/elwha-card-v1-to-v3.md`,
  `docs/development/versioning-playbook.md`, and
  `docs/handoff/elwha-card-v3-phase2-status.md` no longer reference the old
  `src/com/owspfm/elwha/...` path.
- [ ] **H3.** CHANGELOG handled per `docs/development/changelog-policy.md`
  (entry present if policy requires one; absence is justified by the policy if
  not).
- [ ] **H4.** Repo-wide sweep: `grep -rn 'src/com' . --exclude-dir=.git` —
  every remaining hit is under `docs/research/*` (OWS-tool historical) or
  `docs/stories/issue-60/*` (this story's own docs). No other file references
  the old path.
  *Ties to:* operator clarification 3 ("scan the ENTIRE repo").

## I. Process hygiene

- [ ] **I1.** Branch is `feature/issue-60-maven-source-layout`.
- [ ] **I2.** Commit message follows Conventional Commits (`chore(#60): ...`)
  and the body contains `Closes #60`.
- [ ] **I3.** PR has milestone **v0.2.0** set.
- [ ] **I4.** PR not merged — handed off for explicit operator go.
- [ ] **I5.** No package renames, no public-API changes, no component-behavior
  changes (out-of-scope guard). *Evidence:* the only non-doc/non-build diff is
  R100 renames.
