# Elwha → 1.0.0 — takeover handoff

**Written:** 2026-08-06 · **Audience:** a fresh agent picking up the road to 1.0.0 cold · **Repo:** `OWS-PFMS/elwha`

Read `CLAUDE.md` first — it is the standing contract. **Caveat: the version on `main` is stale** (its package table lists 9 of 29 packages and its "Open epics" section names superseded consumer-repo epics as the 1.0.0 gates). The corrected version is on **PR #533**, unmerged at time of writing — see §10. Read it from that branch, not from `main`.

This doc is the *plan*: what remains, in what order, and what will bite you. It is a point-in-time snapshot; where it disagrees with a GitHub issue, the issue wins.

---

## 1. The one-paragraph situation

The component catalog is **done** — ~25 components across 29 packages, ~124k LOC, 554 source files. What stands between `main` and 1.0.0 is not more components. It is (a) one genuine refactor epic that was promised and never built, (b) a self-imposed quality gate that is still mostly *unscoped stubs*, and (c) the documentation and release mechanics of actually publishing. Nothing has shipped since `0.1.0`; **1.0.0 will be the first publish in three months and OWS's initial adoption of Elwha.**

The single biggest risk to a date is **#438** (regression test suite). It is a stub with one research story filed, it blocks two other epics natively, and nobody knows how big it is yet. Sizing it is the highest-value first move.

## 2. Version reality — read this before you touch anything

`pom.xml` says `0.1.0`. That is **not** a description of `main`.

| | |
|---|---|
| Last published release | `0.1.0`, 2026-05-12 — the only tag that exists |
| `v0.2.0` | **cancelled outright** (see #96's 2026-05-31 gate note) |
| `v0.3.0`, `v0.4.0` | planning waves, closed, never cut as releases |
| Next publish | **`1.0.0`** — there is no intermediate release planned |

Consequence worth internalizing: **there is no consumer mid-migration to protect.** OWS never adopted a published V1 card. That is why #96 can delete `card/v1` outright and why "no backwards-compat shims pre-1.0" is still in force.

## 3. Live milestones

Three, and the titles are load-bearing (see §4).

- **`v0.5.0`** — the current wave. 23 open. Everything being built now; the `@version` shipped files carry.
- **`v1.0.0`** — the freeze + first publish. 8 open.
- **`v1.1.0`** — post-1.0 parking. 3 open (#185 FAB Menu, #453 search app bar, #454 toolbar family). Deliberately deferred; **not** V1 scope. Don't pull from here without an operator call.

`v0.1.0`–`v0.4.0` are closed. **`v0.4.0` is unreachable, not merely stale** — `@version` never moves backwards and files on `main` already carry `v0.5.0`, so a PR milestoned `v0.4.0` fails the gate outright.

## 4. Constraints that will bite you

These are the ones that cost time when discovered late.

**Build on JDK 21 or your local run is meaningless.** All four CI workflows pin temurin 21. Spotless' google-java-format calls javac internals and dies on JDK 25 with a `NoSuchMethodError` — a *signature* change, so `--add-exports` cannot fix it. `.envrc` + direnv handles this interactively, but **direnv does not fire in non-interactive shells**, so as an agent you must `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` explicitly or wrap with `direnv exec .`.

**The `@version` gate is a workflow, not a Maven plugin.** `mvn verify` does **not** run it. Check it yourself before burning a CI build:

```bash
python3 scripts/update_javadoc_version.py --check --changed-only --expected v0.5.0
```

**The milestone title is passed verbatim as the expected `@version`.** `validate-versions.yml` reads `.milestone.title` and hands it straight to `--expected`, which string-compares against the tag. So every milestone must be a real version string — never `v1.x`, `next`, or `backlog`. This is why the parking lot is named `v1.1.0`.

**Bump `@version` precisely; never blanket-sed.** The gate's `check_file` only reads the *first* `@version` in a file, but the convention bumps every entity the PR touched. Added files → set both tags to the milestone. Modified files → bump the class tag plus only the tags this branch itself moved. Derive that from `git diff origin/main...HEAD -- <file> | grep '@version'`. A blanket sed silently falsifies entities the branch never touched.

**A CONFLICTING PR runs zero workflows.** If CI is silent, check `gh pr view --json mergeable` before debugging anything else. `CHANGELOG.md` and `ElwhaShowcase.java` are the recurring conflict sites.

**`Closes #N` is per-issue.** `Closes #A, #B, #C` closes only `#A`. Use one `Closes` per line, and verify every issue's state after a multi-issue merge.

## 5. Operating rules (from the operator — these carry forward)

- **Don't auto-merge.** After CI green, hand off. Merge only on explicit go.
- **Set the PR milestone at creation.** The version workflow hard-fails without one.
- **Every new issue goes on Project #5** as part of filing it: `gh project item-add 5 --owner OWS-PFMS --url <issue-url>`.
- **Clarifying questions as plain-text numbered lists in chat.** No UI pickers.
- **No interactive git** (`-i` flags).
- **Phase-level handoffs, not per-story.** For multi-story epics, work whole phases autonomously and hand off at phase boundaries. State the phase plan at every handoff so it is never a surprise.
- **Fresh demo class per story.** Never patch or extend a prior story's demo — per-story smoke needs a per-story artifact.
- **No invented scope cuts.** Complete the actual story contract; never write "deferred" or "no consumer needs this" as cover for an unsanctioned cut. Ask when ambiguous.
- **Surface decisions explicitly.** If you accept a quirk or defer something the operator flagged, say so out loud in the same response.
- **Diagnose before fixing.** When a fix doesn't work, instrument and capture evidence before retrying. Probes are cheap; guess-iteration is not.
- **Dogfood Elwha components** in Showcase/demo/playground code. Raw `J*` only where no Elwha equivalent exists.
- **Completeness before release.** The operator wants the library solid, not shipped early. Don't steer toward tagging unprompted.

## 6. The dependency graph

```
                    ┌──────────────────────────────────────────┐
  UNBLOCKED NOW ──▶ │ #529 Javadoc review        (v0.5.0)       │──┐
                    │   7 errors / 925 warnings / 580 API sites │  │
                    └──────────────────────────────────────────┘  │
                                                                  │
                    ┌──────────────────────────────────────────┐  │
  UNBLOCKED NOW ──▶ │ #439 test-framework research  (v0.5.0)   │  │
                    └───────────────────┬──────────────────────┘  │
                                        ▼                         │
                    ┌──────────────────────────────────────────┐  │
                    │ #438 regression suite       (v0.5.0)     │  │
                    │   ** the long pole — still unscoped **   │  │
                    └───────┬─────────────────────┬────────────┘  │
                            ▼                     ▼               │
                    ┌───────────────┐    ┌──────────────────┐     │
                    │ #440 lib      │    │ #441 Showcase    │     │
                    │      review   │    │      review      │◀─┐  │
                    └───────────────┘    └──────────────────┘  │  │
                                                     #424 ─────┘  │
                                                   (dogfood)      │
                                                                  │
  OPERATOR CALL ──▶ ┌──────────────┐                              │
                    │ #531 cursor  │                              │
                    │   license    │                              │
                    └──────┬───────┘                              │
                           ▼                                      │
  UNBLOCKED NOW ──▶ ┌──────────────┐   ┌──────┐   ┌──────────┐    │
                    │ #67 ItemList │──▶│ #96  │──▶│ #530     │◀───┘
                    │  #68→#69→#70 │   │ del  │   │ pub docs │
                    └──────────────┘   │ v1   │   └────┬─────┘
                                       └──────┘        ▼
                                                  ┌─────────┐
                                                  │   #97   │
                                                  │ release │
                                                  └─────────┘
```

## 7. Recommended order

**Start here — three things are unblocked right now:**

1. **#439 — test framework research.** *Do this first.* It is the sizing story for #438, which is the long pole and which natively blocks #440 and #441. Until it lands, no one can put a date on 1.0.0. Deliverable is `docs/research/elwha-test-suite-research.md` plus test-scoped `pom.xml` additions and a worked spike. Note the constraint that shapes everything: CI is headless Linux, dev is macOS, so `Robot` is unavailable and golden-image snapshots are font/AA-fragile. The existing `*Smoke` mains already prove that synthetic-event dispatch + pixel probes work headless — that is the incumbent to beat, not a blank slate.

2. **#529 — Javadoc review.** Fully parallel; the only quality epic **not** blocked by #438, because comment-only work cannot regress behavior. Phase 0 is triage + the enforcement-posture decision + one exemplar class doc as the house template. Baseline is already measured in the issue. Reproduce it with:
   ```bash
   export JAVA_HOME=$(/usr/libexec/java_home -v 21)
   mvn -q dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt
   find src/main/java -name '*.java' > /tmp/sources.txt
   javadoc -Xdoclint:all -Xmaxwarns 100000 -quiet -d /tmp/jd \
     -classpath "$(cat /tmp/cp.txt)" @/tmp/sources.txt
   ```
   **Decide the milestone before Phase 0, not after** — this pass re-tags `@version` across most of the library, and that is not cheaply redone.

3. **#68 — the `ElwhaItemList<T>` spec.** The last genuine architectural work before the freeze, and research-only, so it is safe to start while the above run. The epic body (#67) already locks the decisions: interface stays `ElwhaList<T>`, concrete class is `ElwhaItemList<T extends Component>`, and the governing principle is **`max(funcA, funcB)`** — where card and chip differ, the richer side wins, no feature loss. Don't re-litigate those.

**Then, in dependency order:**

4. **#531 decision** (operator-owned — see §8). Unblocks #96.
5. **#69 → #70** — build `ElwhaItemList<T>`, migrate playgrounds, delete the ~24 parallel `Card*`/`Chip*` classes.
6. **#438 stories**, as filed by #439's recommendation. Expect a per-family split.
7. **#440 / #441 / #424** fix waves — suite green after every batch; that is the entire point of the dependency.
8. **#96** — delete `card/v1` (gated on #531).
9. **#530** — the consumer publishing docs. Content pass wants the frozen API.
10. **#97** — release.

**Batchable anytime** (independent, small, several will be absorbed by #440's scan): #396, #442, #476, #478, #493 + #505 (same root cause — fix together), #506, #521, #525, #526. And #318 + #507 are seeded into #441; #321 folds into #424 or stays its own vehicle — #424's Phase 0 decides.

## 8. Open decisions the operator owns

Do not resolve these unilaterally.

1. **#531 — the cursor license.** `NOTICE` declares the bundled Capitaine cursors **CC BY-SA 4.0**; the shipped `LICENSE-capitaine.txt` says **LGPL-3.0**. `v0.1.0` already published with the wrong `NOTICE`. LGPL assets inside an Apache 2.0 jar needs a deliberate call: keep-and-declare-correctly, replace, or drop. **Do not quietly "fix" `NOTICE`** — that makes the current state look intentional.
2. **Does the quality gate really precede 1.0?** #438 + #440 as filed make 1.0.0 a function of an unscoped test-suite epic. If they can land in 1.1.x, the date moves in by months. There is a middle path — cut an interim `0.5.0` release so OWS gets something consumable while the suite matures — but the operator has been explicit about wanting completeness before release, so **do not push this unprompted.**
3. **#529's milestone**, per §7 item 2.
4. **Whether `CURSOR_SWAP` survives** in #67's `ReorderAffordance` enum. Interacts with #531 — dropping the LGPL cursors would settle it.

## 9. Known traps, already paid for

- **#96 has a live blocker.** `card/list/ElwhaCardList.java:4` imports `com.owspfm.elwha.card.v1.list.Cursors` — the V3 list depends on a V1 class. Deleting `card/v1` as written **breaks the shipped V3 list.**
- **`getMaximumSize() = getPreferredSize()` disrupts shadow** on halo-inclusive `getPreferredSize` leaves. Never do it (#199 / #200).
- **`cornerRadiusPx()` values are `RoundRectangle2D` arcWidth (diameter), not real radius.** `CornerRadii` treats them as real radius — mixing the two paths on a shadowed primitive breaks body/shadow silhouette agreement.
- **Containers that transform or snapshot live children in `paint()` must override `isPaintingOrigin()` → true**, or child ripple/caret animations freeze.
- **`AbstractAction.isEnabled()` shadowing** sat latent in seven components (#432). Outer-qualify.
- **A long-lived PR inherits the milestone of the era it merges in**, and the version gate makes that non-negotiable. Don't let branches sit.
- **Stretched bounds vs centered painted body** (#493 / #505) — bounds-anchored consumers and hit testing both address invisible edges. Expect this class of bug wherever a layout stretches an Elwha primitive.

## 10. In flight right now

**PR #533** — the maintainer-doc sweep (`CLAUDE.md` + superseding `elwha-v1-component-scope.md`), milestone `v0.5.0`, `Closes #532`. Head `284d909`, one commit ahead of `main`, content intact.

**It cannot go green: GitHub Actions was in a `major_outage` as of 2026-08-06 22:18 UTC.** No check suite was ever created for the commit — six nudges over ~36 minutes produced nothing. This is upstream, not a repo misconfiguration (workflows are `active`, no path filters, public repo so quota is moot). **First thing to do on pickup:** check whether the checks have since appeared. If not and Actions is operational again, one force-push of a fresh SHA will trigger `synchronize`.

Per the standing rule, **do not merge it** even when green — hand off.

## 11. Definition of done for 1.0.0

From #97, plus what has accrued since:

- [ ] #67 complete — one list implementation, legacy parallel classes deleted
- [ ] #531 resolved; #96 merged — `card/v1` gone, no `card.v1.*` references anywhere
- [ ] The v0.5.0 quality gate satisfied to whatever bar the operator sets (§8 item 2)
- [ ] #529 — javadoc builds clean at the agreed doclint level
- [ ] #530 — consumer docs accurate against the frozen API; the Quick start **compiles**
- [ ] `NOTICE` accurate, with paths that resolve
- [ ] CI green on `main`; `mvn verify` passes locally on JDK 21
- [ ] Showcase + playgrounds launch and render
- [ ] `pom.xml` → `1.0.0`; `CHANGELOG.md` `[Unreleased]` → `[1.0.0]` (it is ~239 lines deep — consolidation is real work)
- [ ] `CLAUDE.md` + `README.md` reflect stable status
- [ ] Tag `v1.0.0` pushed; publish workflow green; artifact visible on GitHub Packages
- [ ] Milestone `v1.0.0` closed; #80 and #67 closed; Project #5 reflects Done
