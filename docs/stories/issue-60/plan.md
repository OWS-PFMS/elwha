# Issue #60 — Implementation Plan

**Title:** chore: migrate to standard Maven source layout (`src/main/java`)
**Milestone:** v0.2.0
**Branch:** `feature/issue-60-maven-source-layout`
**Type:** chore (no consumer-visible API/behavior change)

---

## Summary

Move the 134-file Java source tree from the non-standard `src/com/owspfm/...`
to the Maven-standard `src/main/java/com/owspfm/...`, drop the
`<sourceDirectory>` override, and update the build-tool path references and
docs that point at the old location. Purely a filesystem move — **no package
renames, no API changes, no component-behavior changes.** Resources at
`src/main/resources/` are already standard and stay put.

### Clarifications captured (from the operator, 2026-05-19)

1. **Blocker cleared.** Even if an OWS-Local-Search-GUI build path read Elwha's
   sources directly, OWS work is paused until this lib publishes — and a
   Flat→Elwha-scale OWS breaking change is already in flight, so now is the
   right moment for another. Proceed regardless.
2. The Spotless + Checkstyle pom path updates are folded into this PR.
3. **Fix all docs.** After the move, scan the *entire* repo for old
   `src/com` references. Old references are permitted **only** in
   older/completed design docs (the `docs/research/*` extraction records, which
   describe OWS-tool's layout, not Elwha's).

---

## Scope correction (recon finding — the issue understates this)

The issue describes a "single mechanical commit." Recon found the old path is
referenced in **four** build-critical places, not one:

| Location | Current value | Action | Severity if missed |
|---|---|---|---|
| `pom.xml:75` | `<sourceDirectory>src</sourceDirectory>` | Remove | Build can't find sources |
| `pom.xml:~118` | Spotless `<include>src/com/owspfm/**/*.java</include>` | Retarget to `src/main/java/com/owspfm/**/*.java` | **Silent** — glob matches 0 files, `spotless:check` passes vacuously, formatting enforcement dies |
| `pom.xml:~148` | Checkstyle `<sourceDirectory>${project.basedir}/src</sourceDirectory>` | Retarget to `${project.basedir}/src/main/java` | Checkstyle scope drifts |
| `scripts/update_javadoc_version.py` `changed_java_files()` | `--diff-filter=ARC` classifies renames as "added" | Rewrite to exclude pure renames | **Hard CI failure** — see Risk 1 |

`src/main/resources/` is already standard. `.github/workflows/*.yml` have **no**
hard-coded `src/` paths (grep-confirmed) — they only invoke `mvn`.

---

## Risks & Mitigations

### Risk 1 (CRITICAL) — `validate-versions` CI fails on 134 "added" files

`scripts/update_javadoc_version.py::changed_java_files()` builds the
"added" list with `git diff --name-only --diff-filter=ARC` — the `R` admits
**renames**. A `git mv` with no content edit is a 100%-similarity rename, so
**all 134 moved files land in the `added` list.** `check_changed_only()`
(lines 295–315) then requires every "added" file to have `@version == v0.2.0`
**and** `@since == v0.2.0`. The moved files legitimately carry older `@since`
values, and `@since` is immutable by doctrine — so the check would emit ~134
violations and the `Validate @version and @since tags` required check (branch
protection on `main`) would hard-fail. Acceptance criterion "CI workflows
green" cannot pass without addressing this.

**Mitigation — required script fix (Phase 1).** A pure rename carries zero
content change; its Javadoc tags are unchanged and still valid. Rewrite
`changed_java_files()` to classify via `git diff --name-status`:

* `R100` (pure rename) → **excluded** (no content change)
* `R<100` (rename + edit) → new path treated as **modified**
* `A` / `C` → **added**
* `M` → **modified**
* `D` → excluded

This makes "changed" mean "content actually changed" — correct in general, and
future-proofs later file moves (e.g. Card V2 #253). The script is a `.py` file,
not validated by itself, so editing it does not recurse into the validator.

Reference implementation:

```python
def changed_java_files(base_ref: str) -> Tuple[List[Path], List[Path]]:
    """Return ``(modified_files, added_files)`` under src/ or test/, *.java only.

    Uses ``base_ref...HEAD`` (triple-dot) so we diff against the merge-base —
    matching PR diff semantics. Classification is rename-aware:

      * ``A`` / ``C``            -> added
      * ``M``                    -> modified
      * ``R100`` (pure rename)   -> excluded (content unchanged, tags valid)
      * ``R<100`` (rename+edit)  -> modified (new path)
      * ``D``                    -> excluded

    Excluding pure renames keeps a filesystem move (e.g. the #60 src/main/java
    migration) from spuriously demanding @version/@since bumps on files whose
    content never changed.
    """
    diff_range = f"{base_ref}...HEAD"
    raw = _git(
        "diff", "--name-status", "--find-renames", diff_range,
        "--", "src/", "test/",
    )
    modified: List[Path] = []
    added: List[Path] = []
    for line in raw.splitlines():
        if not line:
            continue
        parts = line.split("\t")
        code = parts[0][0]
        if code == "R":
            similarity = int(parts[0][1:]) if parts[0][1:].isdigit() else 0
            new_path = parts[2]
            if similarity < 100 and new_path.endswith(".java"):
                modified.append(Path(new_path))
        elif code in ("A", "C"):
            path = parts[-1]
            if path.endswith(".java"):
                added.append(Path(path))
        elif code == "M":
            path = parts[1]
            if path.endswith(".java"):
                modified.append(Path(path))
        # D -> excluded
    return modified, added
```

Keep the function's return type and call sites unchanged. Confirm `main()`'s
`--changed-only` path dispatches through `changed_java_files()`.

### Risk 2 — `git mv` fails because `src/main/java` does not exist

`src/main/` exists (holds `resources/`) but `src/main/java/` does not. `git mv`
errors if the destination's parent is absent. **Mitigation:** `mkdir -p
src/main/java` before the `git mv`.

### Risk 3 — blame not preserved

Acceptance requires `git log --follow` to work post-move. A `git mv` with
byte-identical content yields R100, which git's rename detection follows.
**Mitigation:** do not edit any moved `.java` file content in this PR; verify
with `git log --follow` in Phase 5.

### Risk 4 — IDE phantom diagnostics during the move

jdt.ls may show stale "cannot be resolved" errors mid-move. Not a build
problem. **Mitigation:** trust `mvn clean verify`, not the IDE; restart the
Java language server if needed.

---

## Phases & Detailed Steps

### Phase 1 — Fix the version validator (do this FIRST)

1. Edit `scripts/update_javadoc_version.py` — replace `changed_java_files()`
   with the rename-aware version above. (~2 min)
2. Confirm `main()` `--changed-only` dispatches through it. (~2 min)
3. Sanity-check Python parses: `python3 -c "import ast;
   ast.parse(open('scripts/update_javadoc_version.py').read())"`. (~1 min)

### Phase 2 — Move the source tree

4. `mkdir -p src/main/java` (~1 min)
5. `git mv src/com src/main/java/com` (~1 min)
6. Verify: `git status --short | grep -c '^R'` reports 134; `find
   src/main/java/com -name '*.java' | wc -l` reports 134; `test ! -d src/com`;
   `ls src/main` shows `java` + `resources`. (~2 min)

### Phase 3 — Update pom.xml

7. Remove `<sourceDirectory>src</sourceDirectory>` (line 75). (~1 min)
8. Spotless include (~line 118): `src/com/owspfm/**/*.java` →
   `src/main/java/com/owspfm/**/*.java`. (~1 min)
9. Checkstyle `<sourceDirectory>` (~line 148): `${project.basedir}/src` →
   `${project.basedir}/src/main/java`. (~1 min)

### Phase 4 — Docs

10. Rewrite CLAUDE.md `## Source layout` section to its final state: drop the
    "non-standard" framing and the "#60 tracks the migration" sentence; state
    briefly that sources live at the standard `src/main/java/com/owspfm/...`,
    resources at `src/main/resources/`. Keep the package table. (~10 min)
11. Update real current-path refs:
    - `docs/migration/elwha-card-v1-to-v3.md` — `src/com/owspfm/elwha/card/v1/`
      → `src/main/java/com/owspfm/elwha/card/v1/`.
    - `docs/development/versioning-playbook.md` — the two illustrative
      example-output lines (`src/com/owspfm/elwha/badge/ElwhaBadge.java`,
      `src/com/owspfm/elwha/card/ElwhaCard.java`) → `src/main/java/...`.
    - `docs/handoff/elwha-card-v3-phase2-status.md` — `src/com/.../ElwhaCardV3Demo.java`
      path prefix → `src/main/java/...` (path-prefix consistency only; the
      referenced file was deleted earlier — that staleness is out of scope).
12. CHANGELOG: read `docs/development/changelog-policy.md`; add an entry under
    `[Unreleased]` only if the policy covers internal/no-consumer-impact build
    chores. A layout move does not affect the published JAR, so default
    expectation is *no* entry — but defer to the policy text. (~5 min)

### Phase 5 — Build & smoke verification

13. `mvn clean verify` — must be green (compiles from `src/main/java`, Spotless
    + Checkstyle both pass). (~3 min)
14. Launch each of the 5 playgrounds, confirm the window opens, close it:
    ```
    mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.theme.playground.ThemePlayground"
    mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.card.playground.ElwhaCardPlayground"
    mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.chip.ElwhaChipPlayground"
    mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.surface.playground.ElwhaSurfacePlayground"
    mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.iconbutton.playground.ElwhaIconButtonPlayground"
    ```
    (~10 min)
15. `git log --follow src/main/java/com/owspfm/elwha/chip/ElwhaChip.java` —
    must show commits from before the move (blame preserved). (~1 min)
16. Local validator dry-run (proves Phase 1 fix + that CI will be green):
    `git fetch origin main` then
    `python3 scripts/update_javadoc_version.py --check --changed-only
    --expected v0.2.0 --base-ref origin/main` — must report **0 violations**.
    (~2 min)

### Phase 6 — Repo-wide stale-reference sweep

17. `grep -rn 'src/com' . --exclude-dir=.git` — every remaining hit must be
    either under `docs/research/*` (OWS-tool historical, describes
    `src/com/owspfm/ui/components/` — leave) or inside `docs/stories/issue-60/*`
    (this story's own docs, which legitimately mention the old path while
    describing the migration — leave). Fix anything else. (~5 min)

### Phase 7 — Commit & PR

18. Stage all changes; single commit:
    `chore(#60): migrate to standard Maven source layout (src/main/java)`,
    body explains the move + the validator-rename fix + `Closes #60`. (~3 min)
19. Push; open PR with **milestone v0.2.0 set at creation**. Preserve the
    `Closes #60` keyword. Do **not** merge — hand off after CI green. (~3 min)

---

## Files to Modify / Create

**Modified:**
- `scripts/update_javadoc_version.py` — rename-aware `changed_java_files()`.
- `pom.xml` — remove `<sourceDirectory>`; retarget Spotless include + Checkstyle dir.
- `CLAUDE.md` — `## Source layout` section → final standard-layout state.
- `docs/migration/elwha-card-v1-to-v3.md` — path ref.
- `docs/development/versioning-playbook.md` — example-output path refs.
- `docs/handoff/elwha-card-v3-phase2-status.md` — path-prefix ref.
- `CHANGELOG.md` — per changelog-policy (likely no entry; confirm).

**Moved (134 `.java` files, content unchanged):**
- `src/com/owspfm/...` → `src/main/java/com/owspfm/...`

**Created:**
- `docs/stories/issue-60/plan.md` (this file), `docs/stories/issue-60/verify.md`.

---

## Testing Strategy

No JUnit suite exists. Verification is: (a) `mvn clean verify` green — exercises
compile + Spotless + Checkstyle from the new layout; (b) all 5 playgrounds
launch via the standard `mvn compile exec:java`; (c) `git log --follow` proves
blame preservation; (d) the local validator dry-run proves the Phase 1 fix and
predicts green CI; (e) CI green on the PR is the final gate.

---

## TODO list

- [ ] Phase 1 — rename-aware `changed_java_files()` in `update_javadoc_version.py`
- [ ] Phase 2 — `git mv src/com src/main/java/com` (134 files)
- [ ] Phase 3 — pom.xml: remove `<sourceDirectory>`, retarget Spotless + Checkstyle
- [ ] Phase 4 — CLAUDE.md `## Source layout` final state + doc path refs + CHANGELOG-per-policy
- [ ] Phase 5 — `mvn clean verify` green, 5 playgrounds launch, `git log --follow`, validator dry-run = 0 violations
- [ ] Phase 6 — repo-wide `src/com` sweep clean (only `docs/research/*` + `docs/stories/issue-60/*` allowed)
- [ ] Phase 7 — single `chore(#60)` commit + PR with milestone v0.2.0; hand off after CI green
