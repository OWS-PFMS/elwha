# FlatCard

A standalone, reusable FlatLaf-styled card primitive for Swing. Mirrors the slot-based
shape of modern web/React card components (header, media, body, footer) and exposes the
variant taxonomy from Material 3 / shadcn (`ELEVATED`, `OUTLINED`, `FILLED`).

This package has **no dependencies on application code** — only FlatLaf and standard
Swing — so the directory can be lifted into a separate Maven module when extracted as a
library.

## Quick start

```java
FlatCard card = new FlatCard()
    .setVariant(CardVariant.ELEVATED)
    .setHeader("Recent activity", "Last 30 days")
    .setBody(new JLabel("12 cycles found across 4 factors."))
    .setFooter(new JButton("Open"), new JButton("Dismiss"))
    .setInteractionMode(CardInteractionMode.HOVERABLE);
```

Sensible defaults: `ELEVATED` variant, `STATIC` interaction, elevation `1`, padding `16`,
corner radius from FlatLaf's `Component.arc` key.

## API reference

### Variants — `CardVariant`

| Variant    | Background           | Border        | Shadow |
|------------|----------------------|---------------|--------|
| `ELEVATED` | Card / Panel surface | none          | yes    |
| `OUTLINED` | Card / Panel surface | hairline      | no     |
| `FILLED`   | Tinted surface       | none          | no     |

### Interaction modes — `CardInteractionMode`

| Mode         | Cursor | Focusable | Fires `ActionEvent` | Holds selection |
|--------------|--------|-----------|---------------------|-----------------|
| `STATIC`     | default | no       | no                  | no              |
| `HOVERABLE`  | hand    | no       | no                  | no              |
| `CLICKABLE`  | hand    | yes      | yes (click + Space/Enter) | no       |
| `SELECTABLE` | hand    | yes      | yes (toggle)        | yes             |

### Slots

```java
card.setHeader(title);
card.setHeader(title, subtitle);
card.setHeader(title, subtitle, leadingIcon);
card.setLeadingIcon(icon);
card.setTrailingActions(button1, button2);

card.setMedia(component);
card.setBody(component);

card.setFooter(component);                         // single component
card.setFooter(button1, button2);                  // right-aligned actions row
```

### Surface properties

```java
card.setElevation(0..5);       // ELEVATED variant only — drop-shadow depth
card.setCornerRadius(int);     // null falls back to FlatLaf Component.arc
card.setCornerRadius(null);
card.setPadding(int);          // uniform spacing on all four sides
card.setPadding(insets);       // per-side override
card.setBorderWidth(int);
card.setBorderColor(color);    // null = derived from theme
```

### Collapsible / expandable

```java
card.setCollapsible(true)
    .setCollapsed(true)
    .setCollapsedSummary(new JLabel("3 items hidden"))
    .setAnimateCollapse(true);

card.addPropertyChangeListener(FlatCard.PROPERTY_COLLAPSED, evt -> ...);
```

When `collapsible` is enabled, the header gains a chevron (`▾` / `▸`); clicking the
header (or pressing `Space`/`Enter` when focused) toggles the state. The optional
`collapsedSummary` slot is shown only while collapsed.

### Selection (toggle)

```java
card.setInteractionMode(CardInteractionMode.SELECTABLE);
card.addActionListener(evt -> System.out.println("selected=" + card.isSelected()));
card.addPropertyChangeListener(FlatCard.PROPERTY_SELECTED, evt -> ...);
```

Selected cards display an accent-colored border in addition to the variant's normal
treatment.

### Theme awareness

Colors and the corner radius are read from `UIManager` keys — the card repaints
correctly when FlatLaf themes are switched. No caller code is required.

## Variant gallery & live editor

Run the playground for an interactive tour:

```
mvn -q exec:java \
  -Dexec.mainClass=com.owspfm.ui.components.card.playground.FlatCardPlayground
```

The playground shows every variant + every mode + collapsed/expanded states side-by-side
on the left, and a live-editing focus card with code-snippet output on the right.

A minimal smoke-test demo (`FlatCardDemo`) is also included in this package for use as a
developer-only entry point.

## Extracting to a separate library

The package was designed for a clean lift-and-shift:

1. Move the `com/owspfm/ui/components/card` directory into a new Maven module's
   `src/main/java/`.
2. Move the `playground` sub-package into the new module's test or examples source root.
3. Rename the package root if you prefer a non-OWS namespace (e.g.,
   `dev.charlesbryan.flatcard`); the only references are intra-package.
4. Add `com.formdev:flatlaf` as the only runtime dependency.

There are no Singleton, static factory, or app-scope dependencies to unwind — every
collaborator is constructed directly by the caller.
