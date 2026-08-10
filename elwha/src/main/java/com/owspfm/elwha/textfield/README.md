# ElwhaTextField

A token-native M3 text-field primitive — a labeled, themed input with two chrome variants, a floating-label animation, leading/trailing icon slots, inline prefix/suffix affixes, supporting/error text, and a required-asterisk cue. Holds a single line by default and switches to multi-line auto-grow or a fixed-height text area.

`ElwhaTextField` styling resolves entirely from the [Elwha design tokens](../theme/) — no raw colors / insets / pixel values reach the public setter API. It mirrors the per-variant naming of the sibling [`ElwhaCard`](../card/README.md) / [`ElwhaChip`](../chip/README.md) primitives.

---

## Architecture — a decorator, not a subclass

Unlike `ElwhaButton extends JComponent` (where Elwha owns the whole paint), text input is genuinely hard to reimplement — caret, selection, IME/composition, copy/paste, undo, editing keys, Tab traversal, and the `AccessibleJTextComponent` surface all live in Swing's `JTextComponent`. So `ElwhaTextField extends JComponent` is a **decorator** over an embedded `JTextComponent` — a `javax.swing.JTextField` for the single-line mode, a `javax.swing.JTextArea` for the multi-line / text-area modes:

- the **wrapped editor** owns the editing surface and its accessibility — all free from Swing;
- **Elwha** owns the chrome paint (filled fill + active indicator, or outlined stroke with the label-notch), the floating label, the typed slots, the token mapping, and the one Swing accessibility gap — the error→"alert" announcement.

The editor is reachable via [`getEditor()`](ElwhaTextField.java) (typed `JTextComponent`) for attaching document/input listeners; the chrome stays Elwha-owned. A `setInputMode(...)` swap rebuilds the editor, so re-fetch it after changing modes.

---

## Quick start

```java
ElwhaTextField email = ElwhaTextField.outlined("Email");
email.setPlaceholder("you@example.com");
email.setSupportingText("We'll only use this to contact you");
email.setLeadingIcon(MaterialIcons.info());

// Visual-only error contract — run your own validation, then drive the chrome:
if (!isValid(email.getText())) {
  email.setError(true);
  email.setErrorText("Enter a valid email address");   // replaces the supporting line + auto error icon
}

// The editor is a real JTextComponent — listen to it directly:
email.getEditor().getDocument().addDocumentListener(myListener);
```

---

## Input modes (single-line · multi-line · text area)

`setInputMode(InputMode)` covers the M3 input-text trichotomy; the editor swaps in place, preserving the text and the enabled / read-only state, and the label / icon slots top-anchor in the multi-line modes while the chrome grows to the taller box. Both variants and the full error / disabled / read-only chrome carry over.

| Mode          | Editor                              | Behavior                                                                            |
| ------------- | ----------------------------------- | ----------------------------------------------------------------------------------- |
| `SINGLE_LINE` | `JTextField` (default)              | One line; text scrolls horizontally. Not for long responses.                        |
| `MULTI_LINE`  | wrapping `JTextArea`                | **Auto-grow** — the field grows with the content and shifts the layout below it down. |
| `TEXT_AREA`   | `JTextArea` in a `JScrollPane`      | **Fixed** at `setRows(int)` rows; scrolls its content internally. Signals long input is welcome. |

```java
ElwhaTextField notes = ElwhaTextField.outlined("Notes");
notes.setInputMode(ElwhaTextField.InputMode.TEXT_AREA);
notes.setRows(4);
```

The `JTextArea` restores default Tab traversal (Tab moves focus, Enter inserts a newline) so it behaves as a form field.

---

## Variants (chrome-only)

Both variants are current under M3 Expressive and expose **one identical API** — the choice is style alone (M3: "both variants provide the same functionality"). Don't intermix them within one form.

| Variant    | Chrome                                                                                  | When to use                                            |
| ---------- | --------------------------------------------------------------------------------------- | ------------------------------------------------------ |
| `FILLED`   | `surface-container-highest` fill, top-only rounded corners, a bottom **active indicator**. | Higher emphasis — a single prominent field.            |
| `OUTLINED` | Transparent container, full **outline** on all four corners; the floated label **notches** the top stroke. | Lower emphasis — many fields stacked in a form.        |

Per-variant factories `ElwhaTextField.filled(label)` / `outlined(label)` mirror `ElwhaCard` / `ElwhaChip`.

---

## Anatomy & slots

Laid out to the M3 redlines (56dp height, 16/12dp L-R without/with icons, 16dp icon↔text, 4dp supporting-top):

- **Floating label** — centered in the empty field, scales (`BODY_LARGE`→`BODY_SMALL`) and rises to the top on focus/populate (reduced-motion snaps). Optional — a label-less field centers its input (the adjacent-label pattern).
- **Placeholder** — `setPlaceholder(...)`; shown in `on-surface-variant` while the editor is empty and the label is out of the way.
- **Leading / trailing icon** — `setLeadingIcon(Icon)` / `setTrailingIcon(Icon)` (`MaterialIcons`, 24dp). An interactive trailing affordance (clear / show-password) is an `ElwhaIconButton` via `setTrailingIconButton(...)`, which brings free Button accessibility. Leading/trailing mirror under `ComponentOrientation` (RTL).
- **Prefix / suffix** — `setPrefixText(...)` / `setSuffixText(...)`; inline fixed affixes in `on-surface-variant`.
- **Supporting text** — `setSupportingText(...)` (`BODY_SMALL`, `on-surface-variant`); its row height is always reserved so an error swap never shifts layout.
- **Character counter** — `setMaxLength(int)` shows a live `used/total` counter (`BODY_SMALL`) right-aligned in that same reserved row. See below.
- **Required asterisk** — `setRequired(true)` appends `*` to the label and the accessible name; `setNoAsterisk(true)` suppresses the glyph while keeping the required a11y cue.

---

## Character counter & supporting visibility

`setMaxLength(int)` (`-1` = none, the default) drives a live `used/total` character counter (for example `5/20`), right-aligned in the always-reserved supporting row — showing or hiding it never shifts layout.

The counter is **display only**: in line with the visual-only validation doctrine it does **not** truncate input. When the count exceeds the limit the counter turns the `error` color as the over-limit cue, but the editor still accepts the characters — enforce a hard cap on your own `Document` if you need one. The live count feeds the accessible description (`"character count, N of M characters entered"`).

`setSupportingTextVisibility(SupportingTextVisibility)` controls when the advisory supporting text **and** the counter show:

| Mode       | Behavior                                                                                     |
| ---------- | -------------------------------------------------------------------------------------------- |
| `ALWAYS`   | Supporting text and counter are always visible. The default.                                 |
| `ON_FOCUS` | They appear only while the field is focused; on blur the reserved row is blank-but-sized.     |

Higher-priority content always shows regardless of the mode: **error text** and an **over-limit counter** are never hidden by `ON_FOCUS`.

```java
ElwhaTextField bio = ElwhaTextField.outlined("Bio");
bio.setMaxLength(120);
bio.setSupportingTextVisibility(ElwhaTextField.SupportingTextVisibility.ON_FOCUS);
```

---

## States & color rules

`enabled` · `hover` · `focus` · `error` · `disabled` · `read-only`.

- Filled active indicator: resting `on-surface-variant` (1dp) → hover `on-surface` → focus `primary` (**3dp**, Expressive).
- Outlined outline: resting `outline` → focus `primary`; hover deepens the stroke (no container fill).
- **Error beats focus** — the indicator/outline, label, supporting text, trailing icon, and caret go `error`; the **input text stays `on-surface`** and the **leading icon stays neutral**. Error + hover deepens via the hover layer over error.
- **Disabled** = `on-surface` @ 0.04 container / 0.38 content (`setEnabled(false)`).
- **Read-only** = `setReadOnly(true)` (`setEditable(false)`) — normal, non-dimmed chrome; the text stays selectable/copyable.

---

## Error & accessibility

The error contract is **visual-only** — Elwha ships no validation engine. Consumers run their own logic and call `setError(boolean)` / `setErrorText(String)`. `setErrorText` replaces the supporting line, auto-shows the non-color **error icon** in the trailing slot (when the consumer set none), and fires the accessibility alert.

Accessibility is overwhelmingly satisfied by the wrapped editor (role `Textbox`, caret, selection, editing keys, Tab). Elwha wires the rest: `accessibleName = label (+ "*" + supporting text)`, `accessibleDescription = supporting text`, and the **error→"alert"** announcement (supporting text first, then error) via an `AccessibleContext` description-property change on the editor — the one native-Swing gap this primitive engineers.

---

## Scope

V1 is **complete**: Phase 1 shipped the single-line field, Phase 2 (`S6`) added **multi-line + text-area**, and Phase 3 (`S7`) added the **character counter + supporting-text visibility mode**. Select / exposed-dropdown ([#331](https://github.com/OWS-PFMS/elwha/issues/331)), search, and formatted/numeric fields are documented follow-ups. Decisions: [`docs/research/elwha-textfield-design.md`](../../../../../../../docs/research/elwha-textfield-design.md).
