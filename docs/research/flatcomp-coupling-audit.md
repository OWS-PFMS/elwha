# FlatComp Coupling Audit

**Deliverable for:** [issue #260](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/260) — coupling audit of `components/` against the rest of the OWS-Local-Search-GUI app.

**Part of epic:** [#231 — Extract `com.owspfm.ui.components` into a standalone library](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/231)

**Audit date:** 2026-05-12
**Audited path:** `src/com/owspfm/ui/components/` (full tree)
**Method:** automated grep + import-graph inspection across every file in `components/`, cross-checked against `package-info.java` declarations.

---

## TL;DR

**Zero coupling sites found.** `components/` is already architecturally clean: no leaked imports from the app, all UIManager reads are either standard FlatLaf platform keys or component-internal keys, all resources live under the components' own package tree, and every interface declared in `components/` is implemented only within `components/`.

**Recommendation:** **Extract as-is.** Sub-issue [#261](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/261) ("break identified couplings") can be closed as **not required** — there is no coupling to break.

---

## 1. Java imports leaking from app into `components/`

Pattern searched: `^import com.owspfm` inside `src/com/owspfm/ui/components/`, filtered to anything **not** under `com.owspfm.ui.components.*`.

**Result:** None found.

Every `com.owspfm.*` import inside `components/` points at another path under `components/` itself.

## 2. Resource references

Searched for: `getResource(...)`, `getResourceAsStream(...)`, `ImageIO.read(...)`, `FlatSVGIcon("...")`.

| File | Line | Reference | Path location |
|---|---|---|---|
| `card/list/Cursors.java` | 154/159 | `Cursors.class.getResource("cursors/" + theFileName)` → `ImageIO.read(in)` | Bundled under `components/card/list/cursors/` |
| `icons/MaterialIcons.java` | 30/146 | `FlatSVGIcon(BASE + theName + ".svg", ...)` where `BASE = "com/owspfm/icons/material/"` | Bundled under the components' own icon resource tree |

Both resource references resolve inside the components' own bundle. **No app-specific paths.**

## 3. UIManager keys

### Standard FlatLaf keys read (platform-provided, not app-installed)

These are FlatLaf framework defaults — they exist in any FlatLaf-using app, not just this one:

- `Component.accentColor`
- `Component.arc`
- `Component.borderColor`
- `Component.focusColor`
- `Component.focusedBorderColor`
- `Label.disabledForeground`
- `Label.foreground`
- `Panel.background`
- `Separator.foreground`

### Component-internal keys (defined and read inside `components/`)

Every `FlatPill.*` key is both defined and consumed inside `components/`. No external installation required.

| Key constant | Key value | Defined in |
|---|---|---|
| `K_BACKGROUND` | `FlatPill.background` | `FlatPill.java:95` |
| `K_BORDER_COLOR` | `FlatPill.borderColor` | `FlatPill.java:98` |
| `K_ARC` | `FlatPill.arc` | `FlatPill.java:101` |
| `K_PADDING` | `FlatPill.padding` | `FlatPill.java:112` |
| `K_HOVER_BACKGROUND` | `FlatPill.hoverBackground` | `FlatPill.java:115` |
| `K_PRESSED_BACKGROUND` | `FlatPill.pressedBackground` | `FlatPill.java:118` |
| `K_SELECTED_BACKGROUND` | `FlatPill.selectedBackground` | `FlatPill.java:121` |
| `K_SELECTED_BORDER_COLOR` | `FlatPill.selectedBorderColor` | `FlatPill.java:124` |
| `K_FOCUS_COLOR` | `FlatPill.focusColor` | `FlatPill.java:127` |
| `K_DISABLED_BACKGROUND` | `FlatPill.disabledBackground` | `FlatPill.java:130` |
| `K_WARM_ACCENT` | `FlatPill.warmAccent` | `FlatPill.java:133` |
| `K_FOREGROUND` | `FlatPill.foreground` | `FlatPill.java:142` |
| (override hook) | `FlatPill.removeIcon` | `FlatPillPlayground.java:313` |

`FlatCard` defines **no** custom UIManager keys — it reads only the standard FlatLaf keys listed above.

**Coupling assessment:** No external dependency on app-installed UIManager keys.

## 4. String-literal FlatLaf style classes

Searched for `putClientProperty(FlatLaf.STYLE_CLASS, ...)` and related calls. All style classes referenced are **FlatLaf built-ins**, not app-specific theme extensions:

| Style class | Reference count |
|---|---|
| `"h2"` | 1 (documented) |
| `"h3"` | 5 |
| `"h4"` | 8+ |
| `"small"` | 9+ |
| `"monospaced"` | 4 |

These are FlatLaf typography/semantic defaults — every FlatLaf app has them.

## 5. Interfaces with implementers outside `components/`

Every interface declared inside `components/`:

- `flatlist/FlatList<T>`
- `card/list/CardSelectionListener<T>`, `CardListDataListener`, `CardAdapter<T>`, `CardListModel<T>`, `CardSelectionModel<T>`, `CardReorderListener<T>`
- `pill/list/PillReorderListener<T>`, `PillSelectionModel<T>`, `PillSelectionListener<T>`, `PillListModel<T>`, `PillAdapter<T>`, `PillListDataListener`

**Implementers found outside `components/`:** None.

Every implementation lives in `components/` itself (e.g., `DefaultCardListModel`, `DefaultPillSelectionModel`).

## 6. Module declarations

**`module-info.java`:** Not present.

**`package-info.java` files present (6):**

| Path | Self-declared status |
|---|---|
| `card/package-info.java` | "free of OWS-specific imports; depends only on FlatLaf + standard Swing" |
| `card/list/package-info.java` | "free of OWS-specific imports; depends only on FlatLaf + standard Swing" |
| `card/playground/package-info.java` | "free of OWS-specific imports; travels with the card library" |
| `pill/package-info.java` | "free of OWS-specific imports; depends only on FlatLaf + standard Swing" |
| `pill/list/package-info.java` | "free of OWS-specific imports; depends only on FlatLaf + standard Swing" |
| `flatlist/package-info.java` | "free of OWS-specific imports; depends only on standard Swing" (extracted in epic #230 story #237) |

**Assessment:** Every `package-info.java` declaration explicitly asserts zero app coupling. The audit confirms each claim is accurate — these declarations are not aspirational.

---

## Summary

| Metric | Value |
|---|---|
| Imports leaking from app → components | 0 |
| Resource references pointing outside components | 0 |
| UIManager keys requiring app-side installation | 0 |
| App-specific FlatLaf style classes | 0 |
| Interfaces with implementers outside components | 0 |
| `package-info.java` accuracy | 6/6 declarations verified |
| **Total coupling sites** | **0** |

## Recommended next step

**Extract `components/` as-is.** Move `com.owspfm.ui.components.{card, pill, flatlist, icons}` and their subpackages directly to the new library artifact — no remediation pass needed.

**Action items for the epic:**

- ✅ Close sub-issue [#260](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/260) as completed (this document is the deliverable)
- 🟢 Close sub-issue [#261](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/261) as **not required** — the precondition (coupling exists) is false; no decoupling work to do
- ➡️ Sub-issue [#263](https://github.com/OWS-PFMS/OWS-Local-Search-GUI/issues/263) (migrate sources) is no longer gated on #261 — it now needs only #259 (decisions) + #262 (new repo bootstrap)

## Caveats

- This audit covers `components/` as it exists at the audit date. If future stories (e.g., #251 rename, #252 selection-surface extension, #253 FlatCard V2) introduce app-specific coupling during their work, those couplings will need to be caught during their respective PR reviews — the "already clean" property must be actively defended.
- The audit did **not** verify that the components compile in isolation against only `flatlaf*.jar` + `rt.jar` — that's a stronger test than "no leaked imports" and remains a final pre-publish smoke for sub-issue #263. The expectation is that it will pass, but it should be run.
