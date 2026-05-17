# Card API survey across M3 / M3-adjacent libraries

A comparison of how five widely-used libraries expose their Card APIs.
Input for the Elwha Card V2-or-V3 design decision: which API model best
matches Elwha's imperative-Swing model + M3-spec-compliance goals?

Companion to:

- [`m3-card-spec-findings.md`](m3-card-spec-findings.md) — raw M3 spec walkthrough
- [`m3-card-spec-organized.md`](m3-card-spec-organized.md) — topical M3 reference

## TL;DR — the API philosophy spectrum

| Library | Approach | Card API surface |
| ------- | -------- | ---------------- |
| **MaterialCardView** (Android View) | Thin chrome + checked-icon only | ~30 setters, all govern the *container* — no typed content slots |
| **Compose Material3** (Kotlin) | Single content lambda | ~6 params + `content: ColumnScope.() -> Unit` — no slot params |
| **Flutter `Card`** (Dart) | Thinnest possible + `ListTile` for content | 11 named params + 1 `child` — slot vocabulary lives on `ListTile` |
| **shadcn/ui** (React) | Sub-component composition | 7 `Card*` near-identity `<div>` wrappers, zero semantic props |
| **MUI Joy UI** (React) | Hybrid: rich root + sub-components | 10 props on root + 4 typed sub-components (`CardContent`/`CardCover`/`CardOverflow`/`CardActions`) |

Two cleanly opposite ends — **MaterialCardView and Compose** are
chrome-only, defer all content to children. **Joy UI** is the most
opinionated, with both root props AND typed sub-components. **Flutter
and shadcn** sit in between, each delegating slot vocabulary to a
sibling primitive (`ListTile` / `CardHeader+CardTitle+...`).

**The one M3-spec-faithful Card primitive (`androidx.compose.material3.Card`) is
the thinnest of all five.** That's a load-bearing data point.

## Per-library detail

### 1. MaterialCardView (Android, View-based, imperative)

**Closest analog to Elwha's setter-based Swing model.**

- **Chassis API:** ~30 setters across shape, stroke, fill, ripple,
  elevation, content padding, foreground tint.
- **First-class content slots:** *one* — the checked icon (selection
  overlay). Position via `setCheckedIconGravity(TOP_START | TOP_END |
  BOTTOM_START | BOTTOM_END)`.
- **No headline / subhead / supporting / media / actions / leading-icon
  setters.** The Card.md doc says explicitly: *"all the optional
  elements of a card's content (with the exception of the checked
  icon) are implemented through the use of other views/components."*
- **Variant selection:** XML `style="@style/Widget.Material3.CardView.{Elevated|Filled|Outlined}"`
  or 3-arg constructor with `defStyleAttr`. **No `setVariant(...)` enum
  exists** — variant is baked at inflation time.
- **Actionability model:** standard Android — `setClickable(true)` +
  `setOnClickListener(...)`. No `setActionable` heuristic, no presence
  detection. Ripple via `setRippleColor(ColorStateList)`.
- **State model:** `setChecked(boolean)` and `setDragged(boolean)` are
  explicit client-driven booleans. Javadoc warns the client is
  responsible for long-press-to-select wiring.
- **API shape:** thin chrome wrapper, almost aggressively composable.
  Card is a styled `FrameLayout` that knows how to look M3 and how to
  be selectable/draggable; anything inside is somebody else's problem.

Sources: `MaterialCardView.java`, `attrs.xml`, `styles.xml`, `Card.md`
from
[`material-components/material-components-android`](https://github.com/material-components/material-components-android/blob/master/lib/java/com/google/android/material/card/).

### 2. Compose Material3 (Kotlin, declarative)

**The Google-canonical M3 Card reference.**

Three top-level composables — `Card`, `ElevatedCard`, `OutlinedCard`
(no `FilledCard` — plain `Card` *is* the filled variant). Each has
two overloads: non-clickable and clickable.

```kotlin
Card(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
)

Card(
    onClick: () -> Unit,            // selects the clickable overload
    modifier: Modifier = ...,
    enabled: Boolean = true,
    shape, colors, elevation, border, interactionSource,
    content: @Composable ColumnScope.() -> Unit,
)
```

- **First-class slots:** zero. The entire body is one `content` lambda
  wrapped in a `Column` internally. No headline / subhead /
  supportingText / media / actions / icon parameters.
- **Composition vocabulary lives on `ListItem`** (also in `material3`),
  which has `headlineContent`, `supportingContent`, `leadingContent`,
  `trailingContent` slots. Canonical pattern:
  `Card { ListItem(headlineContent = {...}, ...) }`.
- **Variant selection:** distinct top-level functions (`Card` /
  `ElevatedCard` / `OutlinedCard`), not an enum. Preserves type-distinct
  defaults via `CardDefaults.{shape, elevatedShape, outlinedShape}`.
- **Actionability model:** overload-based — `onClick` parameter
  presence selects the clickable form. No `isActionable` flag, no
  shared overload. **Library does not enforce** the M3 "no actions on
  actionable surface" doctrine — it's a guideline, not a type
  constraint.
- **Customization shape:** three opaque value types — `CardColors`,
  `CardElevation`, `BorderStroke` — produced by `CardDefaults` factory
  functions. No per-property setters anywhere.
- **API surface size:** roughly **one-tenth** of a setter-heavy
  imperative card.

Sources: [`Card.kt` in `androidx-main`](https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/Card.kt).

### 3. Flutter `Card` (Dart, declarative)

**The thinnest possible M3 Card primitive, paired with `ListTile` for
content vocabulary.**

Three constructors with identical 11-param signatures — `Card(...)`,
`Card.filled(...)`, `Card.outlined(...)`. Params:

| Param | Default |
| ----- | ------- |
| `key`, `color`, `shadowColor`, `surfaceTintColor`, `elevation`, `shape`, `margin`, `clipBehavior` | all null → theme |
| `borderOnForeground` | `true` |
| `semanticContainer` | `true` |
| `child` | `Widget?` |

- **No padding param** — consumer wraps `child` in `Padding`.
- **No `onTap`** — wrap with `InkWell` or `GestureDetector`.
- **`ListTile` is the canonical composition primitive** — Card's
  dartdoc explicitly links to it. `ListTile(leading:, title:,
  subtitle:, trailing:, onTap:)` provides the structured slots Card
  refuses. Idiomatic actionable card with content:
  `Card(child: ListTile(leading: ..., title: ..., subtitle: ...,
  trailing: ..., onTap: () => ...))`.
- **Variant selection:** named constructors (`Card()` / `Card.filled()`
  / `Card.outlined()`), not enum, not setter. Each variant a distinct
  call-site choice.
- **No "no actions on actionable surface" enforcement** — Flutter API
  docs don't restate the M3 web doctrine; both `InkWell`-on-Card and
  `onTap`-on-`ListTile` are equally available.
- **API shape:** cleanest separation of concerns — surface
  (`Card`) / content (`ListTile`) / interaction (`InkWell`) live in
  three different widgets. Cost: consumers must learn the
  `ListTile`-equivalent to get an idiomatic card.

Sources: [`Card-class.html`](https://api.flutter.dev/flutter/material/Card-class.html),
[`ListTile-class.html`](https://api.flutter.dev/flutter/material/ListTile-class.html),
[`card.dart`](https://github.com/flutter/flutter/blob/master/packages/flutter/lib/src/material/card.dart).

### 4. shadcn/ui Card (React + Tailwind)

**Sub-component composition with transparent passthrough.**

Seven near-identity `<div>` wrappers, all with the signature
`function CardX({ className, ...props }: React.ComponentProps<"div">)`.
Zero semantic props on any of them.

| Component | Default Tailwind classes / purpose |
| --------- | ---------------------------------- |
| `Card` | Root. `flex flex-col gap-6 rounded-xl border bg-card py-6 shadow-sm` |
| `CardHeader` | Top region. CSS grid; auto-promotes to 2 columns when a `CardAction` descendant is detected |
| `CardTitle` | Title. `leading-none font-semibold`. **Rendered as `<div>`, not `<h*>`** — a11y miss |
| `CardDescription` | Subtitle. `text-sm text-muted-foreground` |
| `CardAction` | Top-right slot positioned in `CardHeader` (col-start-2). Note: singular |
| `CardContent` | Body. `px-6` only |
| `CardFooter` | Bottom. `flex items-center px-6` |

- **Variant model:** none. Single visual treatment (`rounded-xl border +
  bg-card + shadow-sm` — effectively "Outlined + faint elevation").
  Consumers do `<Card className="border-0 shadow-lg">` etc.
- **No media slot, no CardActions plural** — consumers drop `<img>` /
  `<Button>` directly inside `CardContent` / `CardFooter`.
- **Actionability:** zero. No `onClick`, `role`, `tabIndex`, focus ring.
  Consumer wraps in `<Link>` / `<button>` or spreads props.
- **Each rendered element carries `data-slot="card-*"`** — enables
  parent-driven styling via Tailwind `has-data-[slot=...]` selectors
  (the header's action auto-promotion uses this).
- **API shape:** transparent JSX containers; the entire library is
  ~85 lines of TSX. No M3 variant taxonomy, no actionability story, no
  semantic HTML guarantees. The benefit: a `className` change away from
  any design system.

Sources: [`card.tsx`](https://github.com/shadcn-ui/ui/blob/main/apps/v4/registry/new-york-v4/ui/card.tsx),
[docs page](https://ui.shadcn.com/docs/components/card).

### 5. MUI Joy UI Card family (React)

**Hybrid: rich root props + typed sub-components.** The most opinionated
of the five.

> Caveat: the agent that researched Joy UI couldn't reach the live docs
> (WebFetch tool unavailable in its session) and fell back to training
> knowledge for the v5 stable API. Prop names should be spot-checked
> against the live MUI docs before locking design decisions.

**Sub-component inventory:**

| Component | Purpose |
| --------- | ------- |
| `Card` | Root. Owns variant/color/size/orientation/invertedColors |
| `CardContent` | Flex column wrapper, `flex: 1` |
| `CardCover` | Absolutely-positioned full-bleed background (image, video, gradient) |
| `CardOverflow` | Bleeds children past Card padding to edges |
| `CardActions` | Bottom-anchored action row with own `orientation` + `buttonFlex` |

Card-adjacent primitives used in canonical recipes: `AspectRatio`,
`Typography level="..."`, `Link overlay`.

**Root `<Card>` props:**

| Prop | Values | Default |
| ---- | ------ | ------- |
| `variant` | `solid`, `soft`, `outlined`, `plain` | `outlined` |
| `color` | `primary`, `neutral`, `danger`, `success`, `warning` | `neutral` |
| `size` | `sm`, `md`, `lg` | `md` |
| `orientation` | `vertical`, `horizontal` | `vertical` |
| `invertedColors` | `boolean` | `false` |
| `component`, `slots`, `slotProps`, `sx`, `children` | — | — |

**Notable choices:**

- **No "Elevated" variant by name.** Joy treats elevation as a token
  (`shadow.sm/md/lg/xl`) applied via `sx`, not a Card variant. Variant
  ↔ M3 mapping is approximate: `outlined`→Outlined, `soft`→Filled,
  `solid` and `plain` have no M3 Card analog.
- **`orientation` is first-class on the root** — one of the only libs
  in this survey that surfaces horizontal/vertical as an API parameter
  (matches M3 spec's "same content, two orientations" frame).
- **`CardCover` and `CardOverflow` are layout primitives** — full-bleed
  background and edge-bleeding respectively. M3-relevant for the
  hero-card + headline-overlay pattern.
- **Actionability model:** **child-driven, not Card-driven**. Joy
  recommends `<Link overlay>` on a child `Typography` — expands the
  link's hit-target to the whole positioned ancestor (the Card) without
  nesting interactives inside a `<button>` (which would be invalid
  HTML). No `<Card onClick>` story.
- **`invertedColors`** — descendant palette flip for `solid` Cards
  (recognizes that filled-color cards need to recolor inner children).

Sources: [Joy Card guide](https://mui.com/joy-ui/react-card/) + the
per-component API pages.

## Cross-cutting observations

### Observation 1 — The M3-canonical API is *thin*

Both Google-shipped M3 implementations (MaterialCardView, Compose
Material3) expose **zero typed content slots**. They're chrome
wrappers. The only first-class content concept in MaterialCardView is
the checked-icon overlay; in Compose it's literally nothing.

This is a strong signal. If we look at "what does M3 itself ship,"
the answer is: a styled container with state + variant + actionability
hooks, and nothing inside. The slot vocabulary (headline, subhead,
supporting, media, actions) is **doctrine**, not API.

### Observation 2 — Slot vocabulary lives on a companion primitive

Across the survey, libraries that DO surface slot vocabulary tend to
put it on a sibling primitive, not the Card itself:

- **Flutter:** `ListTile` with `leading` / `title` / `subtitle` /
  `trailing`
- **Compose:** `ListItem` with `headlineContent` / `supportingContent`
  / `leadingContent` / `trailingContent` (canonical
  `Card { ListItem(...) }` pattern)
- **shadcn:** seven sub-`Card*` components
- **Joy UI:** four sub-`Card*` components plus `Typography level`

Only Joy puts opinions on the *root* (variant/color/size/orientation).
Everyone else puts opinions on the *contents* (in a separate
primitive) or nowhere at all.

### Observation 3 — Variant selection is split

| Mechanism | Libraries |
| --------- | --------- |
| Distinct top-level functions / constructors | Compose (`ElevatedCard`/`OutlinedCard`), Flutter (`Card`/`.filled()`/`.outlined()`) |
| XML style / theme attr | MaterialCardView |
| Root prop (`variant`) | Joy UI |
| `className` override | shadcn |
| **Enum setter (`setVariant(...)`)** | **None of these** |

Notable: **none of the five libs uses the `setVariant(enum)` pattern
Elwha currently uses.** The closest analog is Joy UI's `variant`
prop, but it's a declarative prop (recomposes on change), not an
imperative setter on a mutable object.

### Observation 4 — Actionability is never a typed slot

Every library handles actionability differently:

- **MaterialCardView:** `setClickable(true)` + `setOnClickListener`
- **Compose:** overload selection via `onClick` parameter presence
- **Flutter:** wrap with `InkWell` (Card-level) or use `ListTile.onTap`
- **shadcn:** consumer wraps in `<Link>` / `<button>`
- **Joy UI:** child `<Link overlay>` expands hit-target

**No library uses a `setActionable(boolean)` flag.** And none of them
*enforce* the M3 "don't put actions on an actionable surface"
doctrine in the type system — it's universally a guideline.

### Observation 5 — Horizontal orientation is rare

Of the five, only **Joy UI** ships horizontal orientation as a
first-class root prop. The M3 spec sanctions both orientations
(spec frame: "Example of the same card with two different
orientations"), but most libs leave horizontal layouts to the
consumer's child composition.

### Observation 6 — Media as a typed slot is rare

Of the five, only **Joy UI** has a dedicated media primitive
(`CardCover`). MaterialCardView, Compose Card, Flutter Card, and
shadcn all leave media to consumer composition (`<img>` / `Image`
widget / nested `ImageView`). MUI v5 (not Joy) does have `CardMedia`,
but it's a separate component, not a Card prop.

### Observation 7 — The two thinnest libs are the M3-canonical ones

There's a pattern: **the closer a library is to "the Google M3
reference," the thinner the Card API.** Compose (Google) and Flutter
(Google) are the thinnest. MaterialCardView (Google, legacy) is
chrome-heavy but still has no typed content slots. The thickest API
(Joy UI) is the one explicitly *not* claiming M3 conformance — it
explicitly says it's "M3-inspired" rather than "M3-compliant."

## What this tells us about Elwha's design fork

The operator's reframe (M3 spec gives "core six + sanctioned additive
composition," not "this exact recipe ABC") is **directly supported by
this survey**. The two Google-shipped M3 cards both encode that
reframe at the API level — they expose zero typed content slots and
delegate all composition to children.

Elwha V2 currently sits closest to **Joy UI** on the spectrum: typed
slots on the root (`setHeadline`, `setSubhead`, `setSupportingText`,
`setMedia`, `setActions`, `setLeadingIcon`, `setLeadingActions`,
`setTrailingActions`, disclosure axis). That puts us in the most
opinionated quadrant — *more* opinionated than Material's own
imperative reference (MaterialCardView), and far more opinionated
than the declarative M3 reference (Compose Material3).

Three plausible directions surface:

**Direction A — Stay Joy-shaped (refine V2).**
Keep typed slots on the root; add the missing ones the M3 walkthrough
surfaced (leading thumbnail, action-row leading/trailing segments,
orientation, action alignment, expansion overflow). Document the
"core six + additive" doctrine explicitly in the README so consumers
understand what's API vs what's pattern.

**Direction B — Drop to MaterialCardView shape (rewrite as V3).**
Strip typed slots entirely. Card becomes a chrome wrapper:
variant / shape / elevation / state / checked-icon / actionability /
ripple, plus `add(Component)` for everything else. Consumers compose
header rows, media, action rows themselves. *Requires* a companion
primitive (an "ElwhaCardTile" equivalent to `ListTile` /
`ListItem`) that holds the slot vocabulary — otherwise consumers
re-invent it.

**Direction C — Hybrid (rewrite as V3 with split surface).**
A thin `ElwhaCard` chrome primitive (Direction B) plus an
`ElwhaCardLayout` composition primitive that holds the slot
vocabulary (analogous to Flutter's `Card + ListTile`). Most cards
in OWS use the layout; advanced consumers use raw `ElwhaCard +
add()`. Matches the M3-canonical separation of "surface vs content
vocabulary."

Direction A is the lowest disruption — it's the refinement path the
current `m3-card-spec-organized.md` §6 follow-ups already enumerate
(35 items). Directions B and C are rewrites.

**No recommendation yet.** This survey is meant to support — not
decide — the next iterative conversation.

## Open questions to surface in that conversation

1. **How important is API discoverability vs API thinness for Elwha
   consumers?** Joy-shaped wins on discoverability (you can autocomplete
   `setHeadline`); chrome-shaped wins on thinness. The current OWS
   consumer is a single internal team — does discoverability matter
   less for one consumer than it would for a public library?

2. **Is a companion composition primitive viable?** Directions B and C
   only work if consumers will adopt an `ElwhaCardTile`-equivalent.
   That's a second component to design, document, and ship.

3. **What's the cost of breaking V2 now?** The current V2 stack is
   ~7 stacked PRs in active flight (#71 merged, #72-#77 open). A
   rewrite means abandoning that and starting V3.

4. **Does the M3 actionability doctrine ("no actions on an actionable
   surface") actually matter for OWS?** Both `ElwhaCardList<T>` row
   selection and the V2 disclosure chevron currently violate it. The
   organized doc flags this as a 1.0 blocker on a11y grounds — is that
   right?

5. **Is horizontal orientation a real OWS need or a "M3 says it
   exists" need?** If no concrete OWS use case, defer.
