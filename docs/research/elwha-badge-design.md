# ElwhaBadge — Design Decisions

**Status:** LOCKED for the Phase 1 build. This doc fixes the `ElwhaBadge` API, variant axis, content rules, color treatment, anchor geometry, RTL mirroring, and the push-model accessibility wiring for the badge epic.

**Drafted:** 2026-05-26

**Author:** Charles Bryan (`cfb3@uw.edu`).

**Parents:**
- [`elwha-design-direction.md`](elwha-design-direction.md) §9 — doctrine bar (raw Swing + tokens can't express).
- [`elwha-fab-design.md`](elwha-fab-design.md) §15 + lesson — the FAB epic added its placement primitive (`ElwhaFabAnchor`, #205) after the fact. This epic builds the placement primitive (`ElwhaBadgeAnchor`) from day one. Badges are *always* anchored to a host; no standalone use case justifies the anchor-bolt-on path.
- [`elwha-v1-component-scope.md`](elwha-v1-component-scope.md) — catalogs the badge primitive.
- [M3 Badge (Expressive)](https://m3.material.io/components/badges/overview) — the Expressive variant of the spec.

**Epic:** TBD — filed after design-doc approval.

**Blocks:** Navigation Rail epic (#159) — Rail items want badges on item icons.

---

## TL;DR

1. **What it is:** a single `ElwhaBadge` primitive with two variants (Small / Large), plus an `ElwhaBadgeAnchor` placement primitive that attaches a badge to a host icon-bearing component at the M3 upper-trailing position and wires accessibility into the host's accessible name.
2. **Why one class, not two:** Small and Large share color treatment, host attachment, accessibility model, and RTL behavior. Small is a degenerate Large with no label sub-part. Per-variant factories (`small()`, `large(String)`) enforce content rules at the API.
3. **Why build the anchor primitive from day one:** badges have no standalone use case — every M3 Badge example anchors to an icon inside a navigation destination. Building the placement primitive after the fact (per the FAB epic #205 retroactive lesson) just defers the same work and ships a less-correct V0.3.0 surface.
4. **Posture:** tracks M3 Expressive. Color mapping = `Error` / `On error` from the existing `ColorRole` facade — no new theme tokens.
5. **Form enforcement:** per-variant static factories — `ElwhaBadge.small()` takes no content; `ElwhaBadge.large(String content)` requires content. The 4-char cap (including a `+`) is silently truncated in the model; documented as a layout invariant per M3.
6. **A11y push model:** badge owns its `accessibilityText()`; anchor splices it into the host's `accessibleName` on attach and on badge-content change via a property listener. The badge itself is non-focusable and reports no independent accessible action — AT users address badge announcements by selecting the host destination, exactly per M3's use-case requirement.
7. **RTL:** auto via `Component.getComponentOrientation()`. Upper-trailing flips to upper-leading; anchor math is the only place that branches.
8. **Out of scope:** trailing-edge composition placement (e.g., the M3 "Favorites 84" pattern); badge interaction states (badges are decorations, not buttons); animated value transitions; localization beyond the per-badge `withAccessibilityText(...)` override.

---

## §0. Posture: M3 Expressive

Per [`elwha-design-direction.md`](elwha-design-direction.md) Elwha tracks M3 Expressive. The badge spec is largely unchanged between baseline M3 and Expressive — same two variants, same anchor geometry, same color treatment. Expressive-specific notes:

- **Anchor target convention** — M3 Expressive emphasizes anchoring badges to the icon's bounding box, not the whole host component. This drives the anchor primitive's contract: hosts expose an *icon bounding box*, not just their own bounds.
- **Active rail item interaction with badge** — in M3 Expressive Nav Rail, the active indicator pill sits behind the icon and the badge sits over the icon. Badge anchor math is unchanged across active/inactive states; the active-indicator pill is the rail item's concern.

§10 (acc) and §11 (RTL) are CODE rules per M3.

---

## §1. Scope decisions — Elwha adaptation

- ✅ **Two variants** — Small (6×6 dp dot) and Large (16×H dp pill, content auto-sized).
- ✅ **Anchor primitive** — `ElwhaBadgeAnchor` ships with the epic. Provides icon-top-trailing placement, RTL-mirroring, and push-model a11y wiring.
- ✅ **Default color = Error / On-error** — the only M3 color mapping; consumers can override but the default is enforced.
- ✅ **4-char content cap including `+`** — silently truncated; documented as a layout invariant.
- ❌ **Navigation Bar context** — M3 documents badges in both Navigation Bar (mobile) and Navigation Rail (desktop) contexts. Elwha is a desktop library; nav bars are out of scope. Rail context is the canonical placement reference.
- ❌ **Trailing-edge composition placement** (M3 "Favorites 84" pattern, where a Large badge sits at the trailing edge of a label+icon composition rather than on the icon corner) — deferred. Rail items don't trigger this case; consumer can compose manually if needed pre-1.0.
- ❌ **Badge interaction states** — badges are decorations; no hover / focus / press / ripple. The host has those.
- ❌ **Animated value transitions** (count incrementing, dot↔pill morph) — not in this epic; can layer on `MorphAnimator` later.
- ❌ **Localization of default announcement strings** — hardcoded English in V0.3.0. Per-badge `withAccessibilityText(...)` override available. Follow-up issue tracks `UIManager` keys.

---

## §2. Component model — one component, not two

**Decision:** Single `ElwhaBadge` class covers both Small and Large variants.

M3 documents them as two variants, but they share:
- Color treatment (`Error` container, `On error` label for Large)
- Anchor target (host icon's upper-trailing corner)
- Anchor offset *geometry* (offset values differ; geometry rule is identical)
- A11y posture (announced via host's accessible name; not independently focusable)
- RTL behavior (mirror to upper-leading)

Small is "Large minus a label" — the dot is a degenerate pill. The variant flag exists primarily to drive the size + anchor-offset lookup.

**Rationale for one class:**
- Mirrors `ElwhaFab` (one class, two forms via factories).
- Anchor primitive treats both variants uniformly — single attach/detach contract.
- Cross-variant theming applies once.

---

## §3. Content rules

Source: M3 "Configuration" + "Container" cards.

| Variant | Content |
|---|---|
| **Small** | none |
| **Large** | required; 1–4 characters including any `+` suffix |

**Concretely:**
- Small badge: no content; conveys boolean presence ("has unread"). M3 default announcement = "New notification."
- Large badge: short label or count. M3 convention is `1–999` numeric values with `999+` for overflow.

**Enforcement** via per-variant static factories:

```java
ElwhaBadge.small()                       // no content possible
ElwhaBadge.large(String content)         // content required (non-null, non-empty)
ElwhaBadge.large(int count)              // non-negative count; >999 → "999+"
```

Null content on `large(String)` produces `NullPointerException`; empty content and negative `count` produce `IllegalArgumentException`. Empty-Large is just a worse Small; the integer overload rejects negatives because the badge has no meaningful display for them.

**Coercion rule:** content is coerced to fit the 4-character display cap at construction time. The original argument is not retained.

- **Pure-numeric content > 999** collapses to the M3 `"999+"` sentinel. Applies to both `large(String)` (when the string is all-digit) and `large(int)`. So `large("1234")`, `large("999999")`, and `large(50000)` all store `"999+"`.
- **Non-numeric content longer than 3 characters** keeps its first 3 characters and gains a `"+"` suffix. So `large("Message")` stores `"Mes+"`, `large("BETA")` stores `"BET+"`. The `"+"` consistently signals "this is truncated" the same way M3's numeric overflow does, rather than silently chopping characters.
- **Shorter content** (≤ 3 characters, or numeric ≤ 999) passes through verbatim. So `large("OK")` stores `"OK"`, `large("999")` stores `"999"`, `large("99+")` stores `"99+"`.

Rationale: the cap is a layout invariant (a wider pill would break the anchor geometry); the numeric collapse implements the M3 contract that counts cap at `999+`; and propagating the `"+"` signal to non-numeric overflow keeps the truncation visible rather than silently lying about content length.

---

## §4. Size axis (M3 Expressive)

Both variants use fixed token-driven dimensions. Large's width is content-driven up to the max-char limit.

| Variant | Container H | Container W | Corner radius |
|---|---|---|---|
| **Small** | 6 dp | 6 dp | 3 dp (full round) |
| **Large** (1 digit) | 16 dp | 16 dp | 8 dp (full round) |
| **Large** (max chars, e.g. `999+` in Roboto) | 16 dp | ≈ 34 dp | 8 dp |

Per the M3 measurements table. The `34 dp` value is the M3 reference dimension for `"999+"` at the spec'd typography — a *measurement*, not an enforced cap. Our implementation sizes the container to `max(16, textWidth + 8)` so wider glyphs at consumer-substituted fonts grow the pill to fit rather than clipping; the 4-character content cap is what bounds growth.

### §4.1 Large width is dynamic

Width grows **trailing-ward** from the anchor's pinned bottom-leading corner as content widens — rightward in LTR, leftward in RTL. Single-digit content is square (16×16); 4-character content reaches approximately 16×34 at the M3 reference typography. Anchor placement stays pinned (per §5.2); only the trailing edge moves.

### §4.2 Interior padding (Large)

Padding between badge edge and text container: **4 dp** on each side. Label text fits the remaining height (8 dp) with the M3 Label Small font role.

**Typography token:** Label text uses the M3 Label Small role (`TypeRole.LABEL_SMALL`) mapped onto Inter Regular per `Typography.defaults()`. Exact size resolved against the existing token at Phase 1 implementation.

---

## §5. Anchor geometry

The anchor primitive places the badge inside the icon's bounding box at the **upper-trailing edge**, with the badge's bottom-leading corner offset from the icon's top-trailing corner by a variant-dependent amount.

### §5.1 Offset values

| Variant | Offset (icon top-trailing → badge bottom-leading) |
|---|---|
| Small | 6 × 6 dp |
| Large | 14 × 12 dp |

Per the M3 measurements table. "H × W" — first value is vertical (badge bottom-leading is *that many dp below* the icon top-trailing corner along the Y axis); second is horizontal.

### §5.2 Pin behavior

The badge's **bottom-leading corner** is the pinned anchor point. As Large badge content grows wider, the **trailing edge** of the badge moves outward — rightward in LTR, leftward in RTL; the pinned leading corner doesn't move. Verified against the M3 spec visual: a `"999+"` badge anchored to an icon's upper-trailing corner extends *past* the icon's trailing edge as content widens, rather than fanning back across the icon body. The M3 "Large badge container … width expands, but keeps the same placement" rule refers to the pinned corner, not the trailing edge.

### §5.3 Icon bounding box

The anchor primitive needs to know the host's *icon* bounding box, not the host's component bounds. For hosts whose icon position is implicit (e.g., a Nav Rail item with icon-above-label layout), the host exposes an `Rectangle getIconBounds()`-like contract via a small `IconBearing` interface.

```java
package com.owspfm.elwha.badge;

public interface IconBearing {
  /** Icon bounding box in this component's coordinate space. */
  Rectangle getIconBounds();
}
```

Conforming hosts: `ElwhaIconButton`, `ElwhaButton` (when configured with an icon), eventually `ElwhaNavRail.Item`. Non-conforming hosts can still anchor a badge by passing an explicit `Rectangle` at attach time.

### §5.4 No on-top-of-icon mode

M3 "Don't" example: don't place the badge centered over the icon. The anchor primitive's geometry makes that unrepresentable — there's no `setPosition(Position)` escape hatch. Position is computed from the icon's top-trailing corner, period.

---

## §6. Color axis

One color mapping, two roles. Small uses only the container role (no label sub-part); Large uses both.

| Anatomy part | Color role |
|---|---|
| Small badge dot | `ERROR` |
| Large badge container | `ERROR` |
| Large badge label | `ON_ERROR` |

**No new theme tokens.** Both roles exist on the `ColorRole` facade today (verified in `ColorRole.java`).

**Default mapping is enforced.** Consumers can override via explicit per-badge methods (`withContainerColor(ColorRole)` / `withLabelColor(ColorRole)`) but the default is `ERROR` / `ON_ERROR` and that requires no consumer action.

**Contrast rule (DOCS):** M3 requires ≥3:1 contrast between container and label when consumers use a custom color pair. Not enforced at runtime — too expensive, trust the consumer. Documented in Javadoc.

**Dark mode** is automatic via the theme infrastructure.

**M3 documentation note:** the M3 color callouts page lists "On error" for the container and "Error" for the label on the Nav Rail side — inverted from the conventional fill-on-fill pairing. The clean Anatomy page resolves this: the inverted callout is pointing at the rail's *active indicator pill* (a separate component owned by the rail item), not at the badge container. Treat the conventional mapping above as authoritative.

---

## §7. Anatomy

### §7.1 Small badge

1. **Container** — solid filled circle, 6 dp diameter.

(No sub-parts. The dot is the whole component.)

### §7.2 Large badge

1. **Container** — 16 dp tall rounded rectangle, full-round corners (8 dp radius), width content-driven.
2. **Label** — 1–4 characters, M3 Label Small role, centered vertically and horizontally inside the 4 dp interior padding.

---

## §8. API design (LOCKED)

```java
package com.owspfm.elwha.badge;

public final class ElwhaBadge extends JComponent {

  /** Two variants — Small (dot) and Large (pill with content). */
  public enum Variant { SMALL, LARGE }

  // Small form — no content
  public static ElwhaBadge small()                          { ... }

  // Large form — content required (truncated to 4 chars)
  public static ElwhaBadge large(String content)            { ... }

  // Variant accessor
  public Variant getVariant()                               { ... }

  // Large-only content accessor; null for Small
  public String getContent()                                { ... }
  public ElwhaBadge setContent(String content)              { ... }  // Large only; ISE on Small

  // Color overrides (default Error / On error)
  public ElwhaBadge withContainerColor(ColorRole role)      { ... }
  public ElwhaBadge withLabelColor(ColorRole role)          { ... }
  public ColorRole getContainerColor()                      { ... }
  public ColorRole getLabelColor()                          { ... }

  // Accessibility text override (default = M3-prescribed announcement)
  public ElwhaBadge withAccessibilityText(String text)      { ... }
  public String getAccessibilityText()                      { ... }
}
```

### §8.1 Default values

| Property | Default |
|---|---|
| `Variant` | determined by factory |
| `containerColor` | `ColorRole.ERROR` |
| `labelColor` | `ColorRole.ON_ERROR` (Large only; null on Small) |
| `accessibilityText` | Small: `"New notification"`; Large: `"{content} new notifications"` |
| Focusable | `false` (set in constructor) |

### §8.2 Convention adherence

Follows [`docs/development/component-api-conventions.md`](../development/component-api-conventions.md):
- Per-variant static factories (`small` / `large`).
- Fluent setters return `ElwhaBadge`.
- Getter naming `getX()` only.
- Extends `JComponent` (not `AbstractButton`) — badges are decorations, not actions; no need for click/Space/Enter wiring.

### §8.3 Non-focusable invariant

`setFocusable(false)` is set in the constructor and not exposed for override. Re-enabling focus would break the M3 a11y use-case requirement that users address badge announcements via the host destination.

---

## §9. Anchor primitive — `ElwhaBadgeAnchor`

The anchor primitive attaches a badge to a host icon-bearing component. It owns: placement geometry (§5), RTL mirroring (§11), and push-model a11y wiring (§10).

### §9.1 API shape

```java
package com.owspfm.elwha.badge;

public final class ElwhaBadgeAnchor {

  /** Attach a badge to a host that conforms to IconBearing. */
  public static Attachment attach(IconBearing host, ElwhaBadge badge);

  /** Attach a badge with an explicit icon bounding box (non-conforming host). */
  public static Attachment attach(JComponent host, Rectangle iconBounds, ElwhaBadge badge);

  /** Detach the badge from its host. Returns the badge to its untethered state. */
  public static void detach(Attachment attachment);

  /** Opaque handle returned by attach(); pass to detach(). */
  public static final class Attachment { /* package-private fields */ }
}
```

### §9.2 Attachment behavior

On `attach()`, the anchor:

1. Adds the badge to the host's containment hierarchy (host's parent layered pane if the host has one; otherwise installs an internal overlay pane).
2. Computes initial bounds from `host.getIconBounds()` + variant offset + RTL flip.
3. Registers a `ComponentListener` on the host to recompute bounds on resize.
4. Registers a `PropertyChangeListener` on the badge to recompute on variant or content change (Large width changes with content).
5. Splices the badge's `accessibilityText()` into the host's `accessibleName` (push model — see §10).

On `detach()`, all listeners are removed; the badge is removed from the parent; the host's accessibleName is restored to its pre-attach value.

### §9.3 Why one badge per anchor

The anchor primitive supports **one badge per host**. M3 doesn't document multi-badge scenarios; the upper-trailing position has a single slot. Re-attaching a badge to a host that already has one detaches the prior badge first (`IllegalStateException` is overkill; the natural semantics is replace).

### §9.4 Lesson from FAB epic #205

The FAB epic built its placement primitive (`ElwhaFabAnchor`) retroactively in #205, after the FAB itself shipped. The Showcase §15 recipe documents the four pieces every floating-FAB consumer had to assemble manually (layered pane, explicit bounds, ComponentListener, RTL flip). Badges have the same problem shape (placement glue + RTL + listener) — so the anchor primitive ships with the badge from day one, not after.

---

## §10. Accessibility

### §10.1 Push model

The badge owns its accessibility text. The anchor primitive splices that text into the host's `accessibleName` and listens for badge changes to keep the spliced name in sync.

**On attach:**

```
host.accessibleName  =  hostBaseName + " " + badge.getAccessibilityText()
```

**On badge content/variant change:**

The anchor's PropertyChangeListener recomputes and updates `host.accessibleName`.

**On detach:**

`host.accessibleName` reverts to `hostBaseName` (the pre-attach value, captured at attach time).

### §10.2 Why push, not pull

Alternative (pull model): host queries the badge during its own `getAccessibleContext()` build. Considered and rejected:

- Requires every host (Nav Rail item, eventual Tab, etc.) to know about badges.
- Breaks the host-agnostic anchor goal — anchor + host coupling, not anchor + JComponent.
- More moving parts: badge changes have to propagate via the host's own listener chain.

The push model keeps the anchor primitive as the single integration seam. Mirrors how `ElwhaFabAnchor` (#205) handles placement glue.

### §10.3 Default announcement strings

| Variant | Default `accessibilityText()` |
|---|---|
| Small | `"New notification"` |
| Large (content `c`) | `"{c} new notifications"` (e.g., `"3 new notifications"`, `"999+ new notifications"`) |

**Localization:** hardcoded English in V0.3.0. Per-badge `withAccessibilityText(...)` overrides for consumers that need non-English. Follow-up issue tracks routing through `UIManager` keys (e.g., `"ElwhaBadge.smallBadgeAnnouncement"`).

### §10.4 Read order

Per M3: badge text reads **after** the host destination name.

The push-model splicing (`hostBaseName + " " + badge.getAccessibilityText()`) enforces this — host name first, badge text appended.

### §10.5 Badge is not focusable

`setFocusable(false)` is set in the badge's constructor (§8.3). The badge:
- Does not appear in tab order.
- Has no independent click / Space / Enter action.
- Is announced only via the host's accessible name.

Per M3: "People should be able to address badge announcements by selecting corresponding navigation destinations." Selecting the host is the only path; this is enforced by construction.

### §10.6 AccessibleContext role

Badge overrides `getAccessibleContext()` to return an `Accessible` with `AccessibleRole.LABEL` and an `AccessibleRelation.LABEL_FOR` link to the host. Most ATs will discover the badge text via the host's name (the primary path) but the `LABEL_FOR` relation is the redundant secondary path.

### §10.7 Other a11y rules — DOCS

| Rule | Disposition |
|---|---|
| ≥3:1 contrast between container and label (when consumers override) | DOCS — runtime validation overkill, trust consumer |
| Hide badge once destination selected | DOCS — consumer state-management decision; badge has no notion of "selected" |
| Don't obscure focus indicators of host or neighbors | DOCS — composition concern |

---

## §11. RTL mirroring

**CODE rule** (M3 "Do" example: change badge position for right-to-left languages).

| Orientation | Anchor corner |
|---|---|
| LTR | Icon top-**trailing** (right) |
| RTL | Icon top-**leading** (left) |

**Implementation rule:** `ElwhaBadgeAnchor` queries `host.getComponentOrientation().isLeftToRight()` at every bounds computation. The X-axis offset flips sign; Y is unchanged. The badge's pinned corner switches from bottom-leading (LTR) to bottom-trailing (RTL).

Standard Swing `ComponentOrientation` propagates from parent containers; consumers who set `setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT)` on a parent container automatically get the mirrored badge placement.

---

## §12. Guidelines reference

Captured from the M3 spec slides during this design pass. The full table lives in Appendix B. Summary:

| Category | Count | Disposition |
|---|---|---|
| CODE (locked or enforced) | 8 | §3 content rules; §4 sizes; §5 anchor; §6 color; §10 a11y; §11 RTL |
| CODE-IMPLICIT (falls out of design) | 3 | §3 truncation; §5.4 no-center-on-icon; §8.3 non-focusable |
| DOCS (Javadoc / design-doc only) | 6 | Hide-on-selected; contrast on overrides; trailing-edge composition; sibling-collision; semantic choice; etc. |
| OUT OF SCOPE | 2 | Trailing-edge composition mode; nav-bar context |

Most guidelines that aren't enforced are composition-time decisions a leaf component can't make for the consumer.

---

## §13. Story breakdown (Phases 1–2)

Each story is a sub-issue of the epic, added to org Project #5 per [[feedback_issues_to_project_board]]. Each story produces a fresh demo class per [[feedback_fresh_demo_per_story]]. PRs land at phase boundaries per [[feedback_phase_handoff_cadence]], not per story.

### Phase 1 — Core primitive + anchor + a11y

- **S1.** `ElwhaBadge` skeleton + `Variant` enum + `small()` / `large(String)` factories + container rendering for both variants in default color. Demo: small + large + max-char visual matrix.
- **S2.** Large content rendering — label typography, 4-char cap with silent truncation, dynamic width with pinned bottom-leading corner. Demo: 1 / 12 / 999 / 999+ / overflow (truncation behavior).
- **S3.** `ElwhaBadgeAnchor` + `IconBearing` interface + attach/detach + ComponentListener + bounds computation. Hosts: `ElwhaIconButton` (existing) gets `IconBearing` conformance. Demo: badge anchored to `ElwhaIconButton` with live size + content controls.
- **S4.** RTL mirroring via `ComponentOrientation`. Demo: LTR/RTL toggle on the S3 demo.
- **S5.** A11y push-model wiring — `accessibilityText()` default strings + splice into host accessibleName + PropertyChangeListener + restore on detach + `LABEL_FOR` relation. Demo: a11y-text inspector panel + manual override demo.

**Phase 1 PR:** badge + anchor + a11y visually and behaviorally complete.

### Phase 2 — Showcase integration

- **S6.** Showcase Workbench page for `ElwhaBadge` — interactive controls (variant, content, color override, accessibility text override), live anchor preview against an `ElwhaIconButton` host. Demo: workbench is the demo.
- **S7.** Showcase Gallery card + sidebar entry. Mirrors the FAB Phase 4 (#204) Gallery+sidebar pattern.

**Phase 2 PR:** Showcase integration complete.

**Real Nav Rail integration** lands with the Nav Rail epic itself, not this one.

**Milestone:** v0.3.0 across both phases.

---

## §14. Future work (deferred / out of scope)

Filed for posterity; not in this epic.

| Item | Disposition | Reference |
|---|---|---|
| Trailing-edge composition placement (M3 "Favorites 84" pattern) | DEFERRED | Anchor primitive's second mode — file when first consumer needs it. |
| Localization of default announcement strings via `UIManager` keys | DEFERRED | Follow-up issue after V0.3.0 ships. |
| Animated value transitions (count increment, dot↔pill morph) | DEFERRED | Can layer on `MorphAnimator` later. |
| `ElwhaNavRail.Item` `IconBearing` conformance | NAV RAIL EPIC | Tracked in the Nav Rail epic; not this one. |
| Multi-badge per host | OUT OF SCOPE | M3 doesn't document; the upper-trailing slot is singular. |
| Navigation Bar context | OUT OF SCOPE | Mobile pattern; Elwha is desktop-only. |

---

## Appendix A — Decision history

Decisions captured during the spec pass on 2026-05-26.

| Decision | Resolution |
|---|---|
| **One component vs two** | One `ElwhaBadge` covers Small + Large via per-variant factories. |
| **Build anchor primitive day one** | Yes. Avoids FAB epic's retroactive #205 work; badges have no standalone use case. |
| **Variant axis as the API split** | `small()` / `large(String)` factories enforce content rules at the API. |
| **4-char cap behavior** | Overflow-aware coercion: pure-digit content > 999 collapses to `"999+"` (the M3 overflow sentinel); non-numeric content longer than 3 characters keeps its first 3 plus a `"+"` suffix (`"Message"` → `"Mes+"`). Shorter content passes through verbatim. `large(int)` overload added for the common count case. The `"+"` signal is consistent across numeric and non-numeric overflow rather than silently chopping characters. |
| **Empty Large content** | Rejected with `IllegalArgumentException` — empty Large is a worse Small. |
| **Default color = Error / On-error** | Per M3 default color mapping. No new theme tokens. |
| **Inverted-looking M3 color callout reconciled** | Rail-page callout points at the active-indicator pill, not the badge container. Conventional fill-on-fill mapping is authoritative. |
| **A11y push model (anchor mutates host name)** | Keeps anchor as the single integration seam; hosts stay badge-agnostic. |
| **Default announcement strings hardcoded English** | OWS-tooling is English-first. Follow-up issue tracks `UIManager` keys. |
| **Per-badge `withAccessibilityText(...)` override** | Escape hatch for consumers needing custom text or non-English. |
| **Non-focusable invariant** | Set in constructor; not overridable. M3 a11y requires users address badges via host. |
| **RTL via `ComponentOrientation`** | Auto-mirror in anchor; no consumer flag. |
| **Trailing-edge composition mode deferred** | Rail doesn't need it; defer until first consumer asks. |
| **Nav-bar context out of scope** | Desktop library; mobile patterns excluded. |
| **`IconBearing` interface for host conformance** | Hosts expose `getIconBounds()`; non-conforming hosts can pass explicit `Rectangle`. |
| **One badge per host** | Re-attach replaces; no multi-badge slot. |

---

## Appendix B — M3 guideline reference table

Captured 2026-05-26 from the M3 Badge slides. Each row maps an M3 guideline to its Elwha disposition.

| # | Source | Verdict | Notes |
|---|---|---|---|
| G1 | Can contain labels or numbers | DOCS | Semantic choice; consumer decides. |
| G2 | Two variants: small and large | CODE | §1 / §3 / §8. |
| G3 | Anchor inside icon bounding box, upper-trailing | CODE | §5. |
| G4 | Limit content to 4 characters including a `+` | CODE | §3 truncation. |
| G5 | Keep the default color mapping | CODE (default) + DOCS (override allowed) | §6. |
| G6 | Small = unread notification (no text) | CODE-IMPLICIT | `small()` factory takes no content. |
| G7 | Large = label text communicating count | CODE-IMPLICIT | `large(String)` requires content. |
| G8 | Most commonly used within nav rail / app bars / tabs | DOCS | Anchor is host-agnostic; document common hosts. |
| G9 | Hide badge once destination selected (nav-bar context) | DOCS | Consumer state; badge has no "selected" concept. |
| G10 | Anchored inside icon bounding box (large badges expand width, keep placement) | CODE | §5.2 pin behavior. |
| G11 | Use color intended to stand out (default Error) | CODE (default) | §6. |
| G12 | Do: change position for RTL languages | CODE | §11. |
| G13 | Don't: change position arbitrarily or place badge over icon center | CODE-IMPLICIT | §5.4 no escape hatch. |
| G14 | Do: use default badge color | CODE (default) | §6. |
| G15 | Don't: custom color roles unless necessary; if custom ≥3:1 contrast | DOCS | §6 contrast rule. |
| G16 | Label large badges with counts or status | DOCS | Semantic. |
| G17 | Max 4 characters including a `+` | CODE | §3 truncation. |
| G18 | Do: use recommended max char count so labels don't extend beyond container | CODE-IMPLICIT | §3 cap. |
| G19 | Do: truncate badge labels as needed | CODE-IMPLICIT | §3 truncation. |
| G20 | Don't: let badge get cut off or collide with another element | DOCS | Composition concern; depends on parent layout. |
| G21 | Do: large badge for count info when no visual collisions (e.g., nav rail) | DOCS | Consumer composition. |
| G22 | Use small badge when spaces are tightly constrained | DOCS | Consumer composition. |
| G23 | When icon-with-badge is followed by trailing text/element, place Large badge at trailing edge | DEFERRED | Trailing-edge composition mode — see §1 / §14. |
| G24 | A11y — AT users must understand badge info | CODE | §10 push model. |
| G25 | A11y — users address announcements via host destination | CODE | §10.5 non-focusable. |
| G26 | A11y — label read **after** host destination name | CODE | §10.4 splice order. |
| G27 | A11y — numerical badges announce the number | CODE (default) | §10.3. |
| G28 | A11y — non-counting (Small) badges announce "New notification" | CODE (default) | §10.3. |
| G29 | A11y — visual indicators ≥3:1 contrast | DOCS | §10.7. |
