# FlatPill → FlatChip Rename Inventory

**Deliverable for:** [issue #23](https://github.com/OWS-PFMS/flatcomp/issues/23) — pre-flight inventory of FlatPill → FlatChip rename touchpoints.

**Part of epic:** [#27](https://github.com/OWS-PFMS/flatcomp/issues/27) — Rename `FlatPill` → `FlatChip` to align with Material chip taxonomy.

**Inventory date:** 2026-05-13
**Inventoried path:** Repo-wide (`src/`, `docs/`, top-level docs, `pom.xml`).
**Method:** `rg` patterns (`\bPill\b`, `pill\.`, `"[Pp]ill"`, `"FlatPill\.`, `K_`), cross-checked against directory enumeration of `src/com/owspfm/ui/components/pill/`.

---

## TL;DR

The rename has **clean boundaries**:

- **21 Java files** under `src/com/owspfm/ui/components/pill/` (5 in the `pill/` package, 14 in `pill/list/`, plus 2 `package-info.java`) — all need to move.
- **3 javadoc cross-references** in *non-pill* source files (`flatlist/`, `icons/`, `card/list/FlatCardList.java`) that mention `FlatPill` / `FlatPillList` in prose only.
- **14 UIManager string keys** under the `"FlatPill."` namespace (13 declared as `K_*` constants in `FlatPill.java`, 1 ad-hoc `"FlatPill.removeIcon"` used only in `FlatPillPlayground`).
- **1 client-property key** `"FlatPill.style"` (`STYLE_PROPERTY` constant).
- **6 prose / config files** outside `src/` mention pill (`README.md`, `CHANGELOG.md`, `CLAUDE.md`, `CONTRIBUTING.md`, `docs/research/flatcomp-coupling-audit.md`, plus 3 dev-docs that mention pill incidentally and need no change).
- **Zero** reflective lookups, classpath scans, ServiceLoader files, `META-INF`, `.properties`, or other tricky cases. No FlatLaf `styleClass` strings referencing pill (the `FlatLaf.styleClass` usages in playground/demo are generic FlatLaf class names like `"small"` / `"h4"`).
- **One incidental "pill" usage to leave alone:** `card/playground/CycleCardExample.java` defines a local `factorPill(...)` helper and `drawPill(...)` chart-rendering method, plus `PILL_PAD_X` / `PILL_ARC` constants. These describe a *visual shape* in a card-related chart demo and have no relationship to the `FlatPill` class — they stay unchanged.

The rename is mechanical and 1:1 (`pill` → `chip`). No FlatLaf reflection or runtime-string-built lookups would silently break.

---

## 1. Files in the `pill/` package — to move and rename

### 1.1 `src/com/owspfm/ui/components/pill/`

| File                          | Declared class                       | Public API? | Renames to                     |
|---|---|---|---|
| `FlatPill.java`               | `public class FlatPill`              | Yes         | `FlatChip.java` / `FlatChip`   |
| `FlatPillDemo.java`           | `public final class FlatPillDemo`    | Yes         | `FlatChipDemo.java` / `FlatChipDemo` |
| `FlatPillPlayground.java`     | `public final class FlatPillPlayground` | Yes      | `FlatChipPlayground.java` / `FlatChipPlayground` |
| `PillVariant.java`            | `public enum PillVariant`            | Yes         | `ChipVariant.java` / `ChipVariant` |
| `PillInteractionMode.java`    | `public enum PillInteractionMode`    | Yes         | `ChipInteractionMode.java` / `ChipInteractionMode` |
| `package-info.java`           | (package-info)                       | n/a         | move to `chip/package-info.java`, body updated |
| `README.md`                   | (docs)                               | n/a         | move to `chip/README.md`, body rewritten — handled in #26 |

**Inner classes inside `FlatPill.java`:**

| Inner class                  | Visibility | Renames to                |
|---|---|---|
| `AccessibleFlatPill`         | `protected` | `AccessibleFlatChip`      |
| `PillTextLabel`              | `private`   | `ChipTextLabel`           |

Also note local helper classes inside `FlatPill.java` like `LeadingButton` and `TrailingIconButton` — they are not named after "pill" and **do not** need renaming.

### 1.2 `src/com/owspfm/ui/components/pill/list/`

| File                                  | Declared class                                         | Renames to                  |
|---|---|---|
| `FlatPillList.java`                   | `public class FlatPillList<T>`                         | `FlatChipList`              |
| `PillListModel.java`                  | `public interface PillListModel<T>`                    | `ChipListModel`             |
| `DefaultPillListModel.java`           | `public class DefaultPillListModel<T>`                 | `DefaultChipListModel`      |
| `PillSelectionModel.java`             | `public interface PillSelectionModel<T>`               | `ChipSelectionModel`        |
| `DefaultPillSelectionModel.java`      | `public class DefaultPillSelectionModel<T>`            | `DefaultChipSelectionModel` |
| `PillSelectionMode.java`              | `public enum PillSelectionMode`                        | `ChipSelectionMode`         |
| `PillSelectionEvent.java`             | `public class PillSelectionEvent<T>`                   | `ChipSelectionEvent`        |
| `PillSelectionListener.java`          | `public interface PillSelectionListener<T>`            | `ChipSelectionListener`     |
| `PillAdapter.java`                    | `public interface PillAdapter<T>`                      | `ChipAdapter`               |
| `PillReorderEvent.java`               | `public class PillReorderEvent<T>`                     | `ChipReorderEvent`          |
| `PillReorderListener.java`            | `public interface PillReorderListener<T>`              | `ChipReorderListener`       |
| `PillListDataEvent.java`              | `public class PillListDataEvent`                       | `ChipListDataEvent`         |
| `PillListDataListener.java`           | `public interface PillListDataListener`                | `ChipListDataListener`      |
| `package-info.java`                   | (package-info)                                         | move to `chip/list/package-info.java`, body updated |

**Inner classes inside `FlatPillList.java`:**

| Inner class                  | Visibility | Renames to                |
|---|---|---|
| `AccessibleFlatPillList`     | `protected` | `AccessibleFlatChipList`  |

`FlatPillList` also contains log-message strings like `"FlatPillList: switching to ANCHORED..."` (lines 464, 468, 576, 696, 1155, 1827) — these are human-readable log prefixes that should be updated to `"FlatChipList: ..."`. Searchable via `rg '"FlatPillList:'`.

---

## 2. `import` statements

All imports of `com.owspfm.ui.components.pill.*` are **internal to the pill package** today:

| File                              | Imports                                                        |
|---|---|
| `FlatPillPlayground.java`         | `pill.list.DefaultPillListModel`, `pill.list.FlatPillList`, `pill.list.PillSelectionMode` |
| `pill/list/PillAdapter.java`      | `pill.FlatPill`                                                |
| `pill/list/FlatPillList.java`     | `pill.FlatPill`, `pill.PillInteractionMode`                    |

No source file *outside* the `pill/` directory imports anything from `pill.*` — confirmed via `rg "^import.*\.pill\." -g 'src/**'` returning only the three files above.

This is consistent with the original coupling audit's finding that `FlatPill` is fully consumed from within its own package and via the cross-cutting `FlatList<T>` interface only.

---

## 3. UIManager keys (`FlatPill.*` namespace)

### 3.1 Declared `K_*` constants in `FlatPill.java`

| Constant                  | Current value                       | New value                       |
|---|---|---|
| `STYLE_PROPERTY`          | `"FlatPill.style"`                  | `"FlatChip.style"`              |
| `K_BACKGROUND`            | `"FlatPill.background"`             | `"FlatChip.background"`         |
| `K_BORDER_COLOR`          | `"FlatPill.borderColor"`            | `"FlatChip.borderColor"`        |
| `K_ARC`                   | `"FlatPill.arc"`                    | `"FlatChip.arc"`                |
| `K_PADDING`               | `"FlatPill.padding"`                | `"FlatChip.padding"`            |
| `K_HOVER_BACKGROUND`      | `"FlatPill.hoverBackground"`        | `"FlatChip.hoverBackground"`    |
| `K_PRESSED_BACKGROUND`    | `"FlatPill.pressedBackground"`      | `"FlatChip.pressedBackground"`  |
| `K_SELECTED_BACKGROUND`   | `"FlatPill.selectedBackground"`     | `"FlatChip.selectedBackground"` |
| `K_SELECTED_BORDER_COLOR` | `"FlatPill.selectedBorderColor"`    | `"FlatChip.selectedBorderColor"`|
| `K_FOCUS_COLOR`           | `"FlatPill.focusColor"`             | `"FlatChip.focusColor"`         |
| `K_DISABLED_BACKGROUND`   | `"FlatPill.disabledBackground"`     | `"FlatChip.disabledBackground"` |
| `K_WARM_ACCENT`           | `"FlatPill.warmAccent"`             | `"FlatChip.warmAccent"`         |
| `K_FOREGROUND`            | `"FlatPill.foreground"`             | `"FlatChip.foreground"`         |

Java constant names (`K_*`, `STYLE_PROPERTY`) stay — only the string values move.

### 3.2 Ad-hoc UIManager key reads (no `K_*` constant)

| Location                                       | Key                       | Notes                                                          |
|---|---|---|
| `FlatPillPlayground.java:316` (comment)        | `"FlatPill.removeIcon"`   | Documentation comment — update string and inline literals      |
| `FlatPillPlayground.java:318` (live read)      | `"FlatPill.removeIcon"`   | `UIManager.get("FlatPill.removeIcon") instanceof javax.swing.Icon` — the literal needs to become `"FlatChip.removeIcon"` |

### 3.3 Where `K_*` constants are read

`rg "FlatPill\.K_" src/` returns only their declaration site. They are *publicly declared* but referenced internally only by string-equivalent (via `UIManager.get(key)`) inside `FlatPill.java`'s rendering code. After the value rename, the constants will refer to the new `"FlatChip.foo"` strings and consumers using `FlatPill.K_BACKGROUND` (post-rename, `FlatChip.K_BACKGROUND`) will see the new value automatically.

### 3.4 Where the literal strings appear in docs

| File                            | Lines                            |
|---|---|
| `README.md`                     | 106–108 (the snippet shows `UIManager.put("FlatPill.arc", 20)` etc.) |
| `src/com/owspfm/ui/components/pill/README.md` | 108 (`"FlatPill.style"` client property example) |
| `docs/research/flatcomp-coupling-audit.md`    | 74 (audit table cell referencing `FlatPill.removeIcon`) |

The `docs/research/flatcomp-coupling-audit.md` reference is a **historical audit artifact**; recommendation is to leave it intact (with a one-line note that the lib subsequently renamed to FlatChip) rather than rewriting history. Decision deferred to the docs PR (#26).

---

## 4. FlatLaf style classes / `putClientProperty` strings

`rg 'putClientProperty.*[Pp]ill|"styleClass".*[Pp]ill'` returns **zero hits**. All `FlatLaf.styleClass` client-property usages in `FlatPillDemo` and `FlatPillPlayground` use generic FlatLaf class names (`"small"`, `"h4"`) — none are pill-namespaced. **Nothing to rename here.**

---

## 5. Javadoc cross-references in non-pill source files

| File                                                    | Line | Reference                                              |
|---|---|---|
| `src/com/owspfm/ui/components/flatlist/package-info.java` | 4    | `com.owspfm.ui.components.pill.list.FlatPillList`     |
| `src/com/owspfm/ui/components/flatlist/FlatList.java`     | 10   | `{@code FlatCardList}, {@code FlatPillList}, ...`     |
| `src/com/owspfm/ui/components/flatlist/FlatList.java`     | 32   | `{@code FlatPill} vs {@code FlatCard}, {@code PillSelectionMode}` |
| `src/com/owspfm/ui/components/flatlist/FlatListOrientation.java` | 5  | `{@code FlatPillList}, and future siblings`           |
| `src/com/owspfm/ui/components/flatlist/FlatListOrientation.java` | 18 | `{@code FlatPillList} adds the wider set in #238`     |
| `src/com/owspfm/ui/components/icons/MaterialIcons.java`   | 27   | `matches the leading-icon footprint on FlatPill`      |
| `src/com/owspfm/ui/components/card/list/FlatCardList.java` | 1384 | `back-port of FlatPillList's HorizontalLayout`        |
| `src/com/owspfm/ui/components/card/list/FlatCardList.java` | 1445 | `back-port of FlatPillList's WrapLayout`              |

All eight references are documentation only — no compilation impact. Update in the atomic rename PR (#24) to keep terminology consistent in javadoc, even though these files don't move.

---

## 6. README / CHANGELOG / top-level docs

| File                                                  | Pill mentions                            | Action                                      |
|---|---|---|
| `README.md`                                           | 4 places (component list, playground path, exec:java example, UIManager snippet) | Update to `FlatChip` / `FlatChipPlayground` / `"FlatChip.*"` |
| `CHANGELOG.md`                                        | 2 places (entries under `[0.1.0]` for `FlatPill` and `FlatPillPlayground`)        | Leave `[0.1.0]` entries as-is (historical) — add `### Changed` entry under `[Unreleased]` describing the rename. Handled in #26 |
| `CONTRIBUTING.md`                                     | 2 places (mention of `FlatPillPlayground` and `mvn exec:java -Dexec.mainClass=...FlatPillPlayground`) | Update to FlatChip |
| `CLAUDE.md`                                           | Many places (component table, playground commands, conventions reference)         | Update to FlatChip — handled in #26 |
| `src/com/owspfm/ui/components/pill/README.md`         | Throughout                                                                        | Move to `chip/README.md`, fully rewritten — handled in #26 |
| `docs/research/flatcomp-extraction-decisions.md`      | Mentions FlatPill as a member of the extracted set                                | **Leave alone** — historical research artifact frozen at extraction date 2026-05-12 |
| `docs/research/flatcomp-coupling-audit.md`            | Cites `FlatPill.removeIcon` audit cell                                            | **Leave alone** — historical audit artifact; the rename note belongs in the new inventory (this file) and the CHANGELOG |
| `docs/development/versioning.md`, `versioning-playbook.md`, `changelog-policy.md` | Casual mentions in examples | **Leave alone** unless examples become misleading; revisit if any do |

---

## 7. Identifier renames inside pill source

In addition to type names, the following local-identifier patterns appear and should be renamed in the atomic PR (#24):

| Pattern (current)                  | Replace with                | Locations (approx.) |
|---|---|---|
| Local variable `pill`              | `chip`                      | `FlatPillDemo.java` (~7), `FlatPillPlayground.java` (~12) |
| Method param `pill`                | `chip`                      | Anywhere it appears in pill source — verify post-rename |
| Field/variable name containing `pill` | substitute `chip`        | Across `FlatPill.java` / `FlatPillList.java`              |
| Log-message string `"FlatPillList: ..."` | `"FlatChipList: ..."` | `FlatPillList.java` 6 locations: 464, 468, 576, 696, 1155, 1827 |
| Local helper / inner method names containing `pill`/`Pill` | substitute | Verify post-IDE-rename — should be subsumed by IntelliJ rename refactor |

No `my*` / `the*` prefixed identifiers should be left over from the older codebase (already cleaned up in epic #5). The Checkstyle ban on those prefixes guards against regression.

---

## 8. Consumer-side call sites (out of scope for this repo)

Tracked in **OWS-PFMS/OWS-Local-Search-GUI#258**:

- That repo's `#243` (FactorPill → FlatPill migration) and `#244` (InnerViewTabStrip → FlatPill migration) are *future work* that hasn't started.
- Recommendation captured in `#258`: when those issues start, they target `FlatChip` directly (not `FlatPill` → `FlatChip` migration). The simplest resolution is to update `#243` / `#244` bodies in the consumer repo to read "FlatChip" before they kick off.

No code change in this repo. No active consumer to migrate.

---

## 9. Tricky cases checked — all clear

| Concern                                                   | Result          |
|---|---|
| Reflection (`Class.forName`, `getClass().getName`, etc.)  | One usage in `card/playground/CursorReferencePanel.java` — references `com.owspfm.ui.components.card.list.Cursors`, not pill. **Clear.** |
| Classpath / ServiceLoader / `META-INF/services`           | None present in repo. **Clear.** |
| `.properties` files                                       | None present. **Clear.** |
| String-built class names                                  | None found. **Clear.** |
| FlatLaf `styleClass` strings referencing pill             | None — all `FlatLaf.styleClass` values are generic (`"small"`, `"h4"`). **Clear.** |
| Bundled resources under `pill/`                           | None — `src/main/resources/com/owspfm/ui/components/` has only `card/list/cursors/`, no pill assets. **Clear.** |
| `pom.xml` `mainClass` references                          | None hard-coded — `exec:java` mainClass passed at the CLI. Docs that include those CLI examples are listed in §6 above. **Clear.** |
| GitHub workflows referencing `FlatPill`                   | `rg "FlatPill" .github/` returns no hits. **Clear.** |

---

## 10. Suggested execution order for the atomic rename PR (#24)

1. IDE-assisted package rename `pill/` → `chip/` (IntelliJ "Move package" handles directory + `package` declarations + imports).
2. IDE-assisted class renames for every entry in §1.1 and §1.2 (with "Search in comments and strings" enabled to catch javadoc `{@link}` and prose).
3. Inner-class renames (`PillTextLabel`, `AccessibleFlatPill`, `AccessibleFlatPillList`).
4. UIManager key string-value renames (§3) — separate commit if useful for diff readability, but lands in the same PR.
5. Log-message string renames in `FlatPillList.java` (`"FlatPillList: ..."` → `"FlatChipList: ..."`).
6. Cross-reference doc updates in `flatlist/`, `card/list/`, `icons/` (§5).
7. `mvn verify` clean — Spotless + Checkstyle + build.
8. Both playgrounds run (`FlatChipPlayground`, `FlatCardListShowcase`).
9. Run `rg "[Pp]ill" --no-ignore -g '!target/' -g '!.git/'` after the rename — every remaining hit should be intentional (history mentions, the chart-shape `PILL_ARC` constants in `CycleCardExample.java`, the `[0.1.0]` CHANGELOG entries).

Docs sweep (top-level README, CHANGELOG `[Unreleased]` entry, CLAUDE.md, CONTRIBUTING.md) lands in the same PR if bundled per the parent epic's atomic-PR recommendation, or in #26 if split — see parent epic for the PR plan.
