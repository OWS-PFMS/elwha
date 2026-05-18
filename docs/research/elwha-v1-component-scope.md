# Elwha v1 Component Scope — Decision Doc

**Status:** DRAFT — DECISIONS NEEDED. This doc proposes the **complete component catalog** for v1.0.0 and freezes it. Components not on the v1 list ship in v1.x+ when OWS need surfaces. Operator review of §6 settles the cut.

**Drafted:** 2026-05-14

**Author:** Charles Bryan (`cfb3@uw.edu`), via design conversation with Claude.

**Parents:**
- [`elwha-design-direction.md`](elwha-design-direction.md) — §9 "Component scope — what to build vs. what to lean on Swing for" is the rule this doc applies.
- [`elwha-token-taxonomy.md`](elwha-token-taxonomy.md) — locked token surface every v1 component consumes.

---

## TL;DR

1. **Why this doc:** component candidates are being discovered conversationally (Surface → Icon Button → Segmented Button → …). Past 2–3 that's accretion, and accretion delays v1.0. Decide the v1 cut as a **single deliberate batch** instead of one-at-a-time.
2. **The cut should be small.** Design-direction §9: "build only when raw Swing + tokens can't express the need, *and* there is real OWS reuse evidence." Both halves must hold. "Could be useful" doesn't qualify.
3. **Recommended v1 components** (in addition to what's already filed): see §4. Roughly 1–3 more on top of Chip / Card / Surface / IconButton.
4. **Everything else moves to v1.1+** with a clear "build when OWS needs it" trigger — no speculative builds.

---

## 1. Why this doc — the accretion problem

The token foundation has unlocked enthusiasm: with tokens in place, *any* component becomes cheap to build, which makes every M3 component a candidate. M3 has 30+ components. If half of them feel reasonable to add, v1.0 grows from "ship the design system + the components OWS demonstrably needs" into "re-implement MUI on Swing."

The design direction explicitly anticipates this:

> §9: "Build a component only when raw Swing + tokens can't express what you need."
> §13: "Extract when duplication is *real* and *painful*, not for hypothetical reuse."

Past Chip/Card/Surface/IconButton, every additional v1 component spends maintainer time that could instead ship v1.0 to OWS. **The default answer is "later."**

## 2. The framework — two tests, both required

A component earns its way into v1 only if it passes **both** tests:

**Test A — Doctrine (§9):** Raw Swing + tokens cannot express the visual/behavior, *or* the boilerplate to re-express it per consumer is meaningfully duplicative. UIManager-styled standard widgets are not enough.

**Test B — Reuse (§13):** OWS uses this pattern in **multiple places today**, or has a known imminent need. "Could be useful someday" fails this test.

Test A is roughly a property of M3 + Swing. Test B is a property of OWS. The operator owns Test B — anywhere this doc says "(operator: confirm)" I'm guessing at OWS's actual usage.

## 3. The catalog

Already in or queued — **no decision needed:**

| Component | Status |
|---|---|
| `ElwhaCard` + variants | Shipped V1, V2 epic #253 deferred |
| `ElwhaChip` + variants | Shipped V1, V2 rebuild in #31 |
| Card list / Chip list | Shipped; selection/drag unification in #252 |
| `MaterialIcons` helper | Shipped |
| `ElwhaSurface` | Filed #43 |
| `ElwhaIconButton` | Filed #45 |

Decision required for the rest. Recommendation column reads as:
- **v1 build** — meets both tests, build before 1.0
- **v1 raw Swing + tokens** — Swing covers it, just needs theme keys mapped (#34's appendix)
- **v1.x+** — defer until OWS need is concrete
- **Never** — Swing/FlatLaf already solves this; no design-system value in wrapping it

### 3.1 Buttons & button-like

| Component | Swing baseline | M3 gap | OWS signal | Recommendation |
|---|---|---|---|---|
| Text Button | `JButton` + FlatLaf | minor | OWS rarely uses text buttons | **v1 raw Swing + tokens** |
| Icon Button | `JButton` + icon | per-instance variants, toggle icon swap | "almost exclusively" | **already filed (#45)** |
| Segmented Button | `ButtonGroup` + `JToggleButton` | positional borders, connected look | "multiple groups" | **v1 build** *(see §4)* |
| FAB / Extended FAB | none | full M3 spec | desktop apps rarely use FABs (operator: confirm) | **v1.x+** |
| Switch | `JCheckBox` + FlatLaf switch styling | minor | (operator: confirm) | **v1 raw Swing + tokens** unless FlatLaf falls short |
| Toggle Button (standalone) | `JToggleButton` + tokens | minor | covered by IconButton+SELECTABLE for the icon case | **v1 raw Swing + tokens** for text case |

### 3.2 Inputs

| Component | Swing baseline | M3 gap | OWS signal | Recommendation |
|---|---|---|---|---|
| Text Field | `JTextField` + FlatLaf | label / supporting-text / leading-icon slots | (operator: confirm — OWS forms?) | **v1 raw Swing + tokens** unless slot needs are real |
| Text Area | `JTextArea` | minor | likely fine | **v1 raw Swing + tokens** |
| Select / Combo Box | `JComboBox` + FlatLaf | minor | likely fine | **v1 raw Swing + tokens** |
| Checkbox | `JCheckBox` + FlatLaf | minor | fine | **v1 raw Swing + tokens** |
| Radio | `JRadioButton` + FlatLaf | minor | fine | **v1 raw Swing + tokens** |
| Slider | `JSlider` + FlatLaf | minor | fine | **v1 raw Swing + tokens** |
| Date Picker | nothing usable | full Material spec; complex | (operator: confirm) | **v1.x+** unless concrete OWS need |
| Time Picker | nothing usable | full Material spec | (operator: confirm) | **v1.x+** |
| Color Picker | `JColorChooser` | M3 has no spec'd color picker | (operator: confirm) | **never** (raw Swing) |

### 3.3 Surfaces & containers

| Component | Swing baseline | M3 gap | OWS signal | Recommendation |
|---|---|---|---|---|
| Surface / Paper | rounded `JPanel` boilerplate | rounded token-resolved paint | foundation for Card V2 | **already filed (#43)** |
| Card | exists V1; V2 in #253 | escape-hatch retirement | yes, heavy | **V2 covered by #253 (deferred)** |
| Bottom Sheet | nothing | full M3 spec | desktop UI rarely uses (operator: confirm) | **never / v1.x+** |
| Side Sheet | nothing | full M3 spec | possibly useful for OWS panels (operator: confirm) | **v1.x+** |
| Drawer / Nav Drawer | nothing | full M3 spec | (operator: confirm) | **v1.x+** |
| Dialog | `JDialog` + tokens | structured patterns (alert / confirm / full-screen) | (operator: confirm) | **v1 raw Swing + tokens**; a `Dialogs` factory if patterns repeat |

### 3.4 Communication / feedback

| Component | Swing baseline | M3 gap | OWS signal | Recommendation |
|---|---|---|---|---|
| Snackbar / Toast | nothing | full spec; transient overlay | (operator: confirm — OWS shows success/error somewhere?) | **v1 build candidate** *(see §4 — pending operator confirm)* |
| Banner / Inline Alert | nothing | role-colored inline message (`errorContainer` / `tertiaryContainer`) | (operator: confirm) | **v1 build candidate** *(see §4)* |
| Badge | nothing | small dot/count overlay on another widget | (operator: confirm) | **v1.x+** unless OWS has notification counts today |
| Tooltip | `JToolTip` + FlatLaf | role-colored (`inverseSurface`) per M3 | minor | **v1 raw Swing + tokens** (key-mapped via #34) |
| Progress (linear) | `JProgressBar` + FlatLaf | minor | OWS search loading states (operator: confirm) | **v1 raw Swing + tokens** |
| Progress (circular indeterminate) | nothing usable | iconic M3 spinner | OWS likely needs (operator: confirm) | **v1 build candidate** *(see §4)* |

### 3.5 Display

| Component | Swing baseline | M3 gap | OWS signal | Recommendation |
|---|---|---|---|---|
| Avatar | nothing | circular image / initials | OWS shows users / entities? (operator: confirm) | **v1.x+** unless concrete need |
| Divider | `JSeparator` + tokens | none real | yes | **v1 raw Swing + tokens** |
| Image List | nothing nice | full M3 spec | (operator: confirm) | **v1.x+ / never** |
| Table | `JTable` + tokens | structured row variants | OWS has data-heavy panels (per CLAUDE.md) | **v1 raw Swing + tokens** |
| Tree | `JTree` + tokens | minor | fine | **v1 raw Swing + tokens** |
| List | `JList` (and Elwha chip/card lists) | n/a — already covered | covered | **already shipped** |

### 3.6 Navigation

| Component | Swing baseline | M3 gap | OWS signal | Recommendation |
|---|---|---|---|---|
| Tabs (primary M3) | `JTabbedPane` + tokens; ChipList `SINGLE_MANDATORY` already does tab-strip semantics | indicator + state-layer treatment | already partially covered by chip-list | **v1 raw Swing + tokens** unless ChipList gap surfaces |
| Top App Bar | menubar idiom | different desktop paradigm | desktop has menubar | **never** (different idiom) |
| Bottom Nav Bar | nothing | mobile pattern | n/a for desktop | **never** |
| Navigation Rail | nothing | could fit desktop side-nav | (operator: confirm) | **v1.x+** |
| Breadcrumbs | nothing | full M3 spec | (operator: confirm) | **v1.x+** |
| Pagination | nothing | full M3 spec | (operator: confirm) | **v1.x+** |
| Menu / Popup Menu | `JMenu` / `JPopupMenu` + FlatLaf | minor | fine | **v1 raw Swing + tokens** |

## 4. Recommended v1 build cut

On top of what's already filed (#31 Chip V2, #43 Surface, #45 IconButton), my recommendation is **at most three more**, all gated on operator confirming the OWS reuse signal:

| Candidate | Why it earns v1 | Operator confirm |
|---|---|---|
| **Segmented Button** | Test A passes (positional borders impossible via raw `JToggleButton` styling). Test B: "multiple ZButton groups." | How many groups, and how visually inconsistent does the JToggleButton-row approach feel today? If it's 5+ groups and visibly ad-hoc, build. If 2 and tolerable, defer. |
| **Snackbar / Toast** | Test A passes (nothing in Swing). Test B: success / error feedback is universal in UIs. | Does OWS show transient success/error messages today, and how (status bar? dialog? nothing)? If "nothing" or "dialog when it should be toast," build. |
| **Circular Progress (indeterminate)** | Test A passes (Swing has nothing). Test B: search-loading state. | Does OWS show a loading indicator during local search? If yes (or wants to), build. |

**Capped at three** by design. Each costs maintainer cycles between now and v1.0; each one we *don't* build ships v1.0 sooner. If the operator can't confirm a Test B signal for one of these, it drops to v1.x+.

**Banner / Inline Alert** is right at the cut line. If OWS does inline error messaging in forms today, it should be on the list; if errors are surfaced via dialogs / snackbars, it's deferrable.

## 5. What's deferred and why

Everything in §3 marked `v1.x+`. Build them when **two** things are true: (a) OWS has a concrete screen that needs it (not "could use it"); (b) the cost of *not* building it — i.e., the duplication / hand-rolled boilerplate that piles up in OWS — exceeds the cost of building it in Elwha.

Specifically not-yet:
- **Date / Time pickers** — complex; defer until a real OWS form needs them
- **Badge** — useful but cheap to defer
- **Avatar** — depends on OWS showing users / entities
- **FAB / Bottom Sheet / Drawer / Nav Rail / Breadcrumbs / Pagination** — desktop-app fit varies; revisit per real need
- **Snackbar / Inline Alert / Circular Progress** — *unless* operator confirms in §4, defer

## 6. Decisions — operator review

| # | Question | Default if undecided |
|---|---|---|
| 1 | Does the framework (§2 two-test rule) hold? | Yes — applies §9 + §13 mechanically |
| 2 | Add **Segmented Button** to v1? | No (defer) unless OWS has 5+ groups visibly inconsistent today |
| 3 | Add **Snackbar / Toast** to v1? | No (defer) unless OWS does (or wants to) show transient feedback today |
| 4 | Add **Circular Progress** to v1? | No (defer) unless OWS needs a loading indicator for search today |
| 5 | Add **Banner / Inline Alert** to v1? | No (defer) unless OWS has inline-form-error messaging today |
| 6 | Is there a component **not in §3** OWS needs? | Surface it now — easier to add to v1 here than discover later |
| 7 | Anything in §3 marked `v1 raw Swing + tokens` that actually needs a wrapper? | None unless flagged |

Once 1–7 are answered, this doc becomes **LOCKED v1 component scope** and any new component candidate has to wait for v1.1+ — no more conversational accretion.

## 7. v1.0.0 stop-line, recap

With this doc locked, the v1.0.0 roadmap is:

```
✓ #30 token foundation                          (merged)
→ #42 rename FlatComp → Elwha + package flatten (in flight on refactor/elwha-rename)
→ #31 Chip V2 rebuild (creates SurfacePainter)
→ #43 ElwhaSurface
→ #45 ElwhaIconButton
→ [whatever §4 candidates the operator confirms]
→ #253 Card V2
→ #252 FlatList<T> selection/drag unification
→ v1.0.0
```

Nothing else.
