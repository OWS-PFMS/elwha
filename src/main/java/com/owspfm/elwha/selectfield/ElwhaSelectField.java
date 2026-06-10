package com.owspfm.elwha.selectfield;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.iconbutton.IconButtonSize;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.menu.ElwhaMenu;
import com.owspfm.elwha.menu.ElwhaMenuItem;
import com.owspfm.elwha.menu.SelectionMode;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleState;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * The M3 <strong>exposed dropdown menu</strong> (select field): a text field with a trailing
 * dropdown-arrow that opens an anchored menu of typed options; choosing one writes its display text
 * back into the field. Generic over the option type {@code T}.
 *
 * <p><strong>Pure composition, not inheritance.</strong> An {@code ElwhaSelectField} <em>owns</em>
 * a read-only {@link ElwhaTextField} (chrome, label, slots, states, a11y) and builds an {@link
 * ElwhaMenu} for the option list — it paints nothing of its own. It is deliberately <em>not</em> a
 * subclass of {@link ElwhaTextField} (which would inherit a typing surface a pure select must
 * suppress) and <em>not</em> a {@code selectMode} flag on the field (which would bloat the field's
 * API with menu/option concerns). The architecture is locked by the epic #331 S1 spike; see {@code
 * docs/research/elwha-selectfield-design.md} §2.
 *
 * <p><strong>The default is the non-editable (pure) select.</strong> The embedded field is
 * read-only — the field and the trailing arrow are the open/close affordance, and picking a menu
 * item is the only way to set the value. {@link #setEditable(boolean)} opts into the M3
 * <em>editable</em> exposed dropdown (Phase 2): the embedded field becomes typeable while the menu
 * keeps anchoring to the field, keyboard focus stays in the editor (the ARIA combobox pattern, via
 * the menu's {@code focusHome}), and picking an option still writes back. Multi-select (Phase 3) is
 * a later phase.
 *
 * <p>Phase 1 ships across stories #374–#378 and Phase 2 across #391–#394; this class began at the
 * S1 skeleton (composition + menu round-trip + arrow toggle) and is extended in place by the later
 * stories (typed value API, keyboard + a11y, variant delegation, Showcase, editable combo).
 *
 * @param <T> the option value type
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public class ElwhaSelectField<T> extends JComponent {

  private static final int ARROW_PX = 20;
  private static final int ROTATE_MS = 180;

  private final ElwhaTextField field;
  private final ElwhaIconButton arrow;
  private final RotatableIcon arrowIcon;
  private final MorphAnimator arrowAnim;
  private final List<ElwhaMenuItem> items = new ArrayList<>();
  private final List<Consumer<T>> selectionListeners = new ArrayList<>();

  private List<T> options = List.of();
  private Function<T, String> display = String::valueOf;
  private T selectedValue;

  private ElwhaMenu menu;
  private boolean expanded;
  private boolean readOnly;
  private boolean editable;

  /**
   * Creates a {@link ElwhaTextField.Variant#FILLED} select field with the given floating label.
   *
   * @param label the field's floating label
   */
  public ElwhaSelectField(final String label) {
    this(ElwhaTextField.Variant.FILLED, label);
  }

  /**
   * Creates a select field of the given variant with the given floating label.
   *
   * @param variant the field variant (a {@code null} is treated as {@link
   *     ElwhaTextField.Variant#FILLED})
   * @param label the field's floating label
   */
  public ElwhaSelectField(final ElwhaTextField.Variant variant, final String label) {
    this.field =
        new ElwhaTextField(
            variant == null ? ElwhaTextField.Variant.FILLED : variant, label == null ? "" : label);
    this.field.setReadOnly(true);
    this.arrowIcon = new RotatableIcon(MaterialIcons.arrowDropDown(ARROW_PX));
    this.arrow = new ElwhaIconButton(arrowIcon);
    this.arrow.setButtonSize(IconButtonSize.S);
    this.field.setTrailingIconButton(arrow);
    this.arrowAnim = new MorphAnimator(arrow, ROTATE_MS);
    this.arrowAnim.addProgressListener(
        () -> {
          arrowIcon.setAngle(arrowAnim.progress() * 180f);
          arrow.repaint();
        });

    setLayout(new BorderLayout());
    setOpaque(false);
    add(field, BorderLayout.CENTER);

    arrow.addActionListener(e -> toggle());
    updateArrowA11y(false);
    installFieldMouse();
    installFieldKeyboard();
  }

  /**
   * Creates a {@link ElwhaTextField.Variant#FILLED} select field with the given label.
   *
   * @param <T> the option value type
   * @param label the field's floating label
   * @return a new filled select field
   */
  public static <T> ElwhaSelectField<T> filled(final String label) {
    return new ElwhaSelectField<>(ElwhaTextField.Variant.FILLED, label);
  }

  /**
   * Creates a {@link ElwhaTextField.Variant#OUTLINED} select field with the given label.
   *
   * @param <T> the option value type
   * @param label the field's floating label
   * @return a new outlined select field
   */
  public static <T> ElwhaSelectField<T> outlined(final String label) {
    return new ElwhaSelectField<>(ElwhaTextField.Variant.OUTLINED, label);
  }

  /**
   * Sets the option list. The menu is rebuilt lazily on the next open (the {@code
   * rebuild-on-options-change} lifecycle locked by the S1 spike), so changing options is cheap and
   * preserves the {@code selected} mark cheaply between opens.
   *
   * @param options the options, or {@code null} for none
   */
  public void setOptions(final List<T> options) {
    this.options = options == null ? List.of() : List.copyOf(options);
    this.menu = null;
  }

  /**
   * Sets the function mapping an option value to its display string (default {@code
   * String::valueOf}). The menu is rebuilt lazily on the next open.
   *
   * @param displayFunction the value→label renderer, or {@code null} to reset to {@code
   *     String::valueOf}
   */
  public void setDisplayFunction(final Function<T, String> displayFunction) {
    this.display = displayFunction == null ? String::valueOf : displayFunction;
    this.menu = null;
  }

  /**
   * Returns the currently selected option value, or {@code null} if nothing is selected.
   *
   * @return the selected value, or {@code null}
   */
  public T getSelectedValue() {
    return selectedValue;
  }

  /**
   * Selects the given option value programmatically: writes its display text into the field and
   * marks the matching menu item {@code selected} (single-select — the others clear). Passing
   * {@code null} clears the selection (empty field, the floating label rests). A value that is not
   * among the current {@linkplain #setOptions options} is ignored — a select is constrained to its
   * options. Fires the {@linkplain #addSelectionChangeListener selection-change listeners} only
   * when the value actually changes.
   *
   * @param value the option to select, or {@code null} to clear
   */
  public void setSelectedValue(final T value) {
    if (value == null) {
      applySelection(-1, null);
      return;
    }
    final int index = options.indexOf(value);
    if (index >= 0) {
      applySelection(index, value);
    }
  }

  /**
   * Registers a listener notified with the new value whenever the selection changes (via a menu
   * pick or {@link #setSelectedValue}). Not fired for a no-op set to the current value.
   *
   * @param listener the change listener; {@code null} is ignored
   */
  public void addSelectionChangeListener(final Consumer<T> listener) {
    if (listener != null) {
      selectionListeners.add(listener);
    }
  }

  /**
   * Removes a previously {@linkplain #addSelectionChangeListener registered} selection-change
   * listener.
   *
   * @param listener the listener to remove
   */
  public void removeSelectionChangeListener(final Consumer<T> listener) {
    selectionListeners.remove(listener);
  }

  // ---- Editable mode (Phase 2, #391) ----------------------------------------
  // The M3 editable exposed dropdown: the embedded editor becomes typeable while the menu keeps
  // anchoring to the field. Keyboard focus stays in the editor the whole time the menu is open —
  // the menu is built with focusHome(editor), so opening never steals focus and presses/focus in
  // the editor never light-dismiss. The arrow still toggles; typing or Down/Alt+Down opens.

  /**
   * Whether the select is editable — the M3 editable exposed dropdown (combobox). Default {@code
   * false}: the pure select, whose field is read-only and whose body press toggles the menu.
   *
   * @return {@code true} when the embedded field is typeable
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean isEditable() {
    return editable;
  }

  /**
   * Sets the editable mode. When {@code true}, the embedded field becomes typeable (free text is
   * accepted), a press in the field places the caret instead of toggling the menu, and the menu —
   * opened by the trailing arrow, typing, or Down/Alt+Down — leaves keyboard focus in the editor
   * (the ARIA combobox pattern). When {@code false} (the default), the field is read-only and the
   * whole field body is the open/close affordance. Flipping the mode closes an open menu and
   * rebuilds it lazily on the next open.
   *
   * @param editable {@code true} for the editable combo
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setEditable(final boolean editable) {
    if (this.editable == editable) {
      return;
    }
    this.editable = editable;
    if (expanded && menu != null) {
      menu.close();
    }
    this.menu = null;
    syncEmbeddedReadOnly();
  }

  /**
   * Returns the field's current text. In the pure select this is always the selected option's
   * display string (or empty); in the {@linkplain #setEditable editable} combo it may be free text
   * the user has typed that maps to no option.
   *
   * @return the field text
   * @version v0.4.0
   * @since v0.4.0
   */
  public String getText() {
    return field.getText();
  }

  // The embedded editor is typeable only in editable mode, and never while the select itself is
  // read-only (read-only shows the value but does not allow changing it in either mode).
  private void syncEmbeddedReadOnly() {
    field.setReadOnly(!editable || readOnly);
  }

  // ---- Variant, state, and slot delegation (S4) -----------------------------
  // The embedded field carries the variant (per the filled()/outlined() factories) and every state;
  // the select field delegates to it. The trailing slot is OWNED by the select field (the arrow) —
  // there is intentionally no setTrailingIcon passthrough, so a consumer cannot displace the arrow.

  /**
   * Returns the floating label.
   *
   * @return the label text
   */
  public String getLabel() {
    return field.getLabel();
  }

  /**
   * Sets the floating label (delegates to the embedded field).
   *
   * @param label the label text
   */
  public void setLabel(final String label) {
    field.setLabel(label);
  }

  /**
   * Sets the leading icon (delegates to the embedded field).
   *
   * @param icon the leading icon, or {@code null} to clear
   */
  public void setLeadingIcon(final Icon icon) {
    field.setLeadingIcon(icon);
  }

  /**
   * Returns the supporting text shown below the field.
   *
   * @return the supporting text
   */
  public String getSupportingText() {
    return field.getSupportingText();
  }

  /**
   * Sets the supporting text shown below the field (delegates to the embedded field).
   *
   * @param supportingText the supporting text
   */
  public void setSupportingText(final String supportingText) {
    field.setSupportingText(supportingText);
  }

  /**
   * Returns the placeholder shown when no option is selected.
   *
   * @return the placeholder text
   */
  public String getPlaceholder() {
    return field.getPlaceholder();
  }

  /**
   * Sets the placeholder shown when no option is selected (delegates to the embedded field).
   *
   * @param placeholder the placeholder text
   */
  public void setPlaceholder(final String placeholder) {
    field.setPlaceholder(placeholder);
  }

  /**
   * Whether the control is in the error state.
   *
   * @return {@code true} if errored
   */
  public boolean isError() {
    return field.isError();
  }

  /**
   * Sets the error state (delegates to the embedded field — error chrome + supporting-row swap).
   *
   * @param error {@code true} for the error state
   */
  public void setError(final boolean error) {
    field.setError(error);
  }

  /**
   * Returns the error text.
   *
   * @return the error text
   */
  public String getErrorText() {
    return field.getErrorText();
  }

  /**
   * Sets the error text shown in the supporting row while errored (delegates to the embedded
   * field).
   *
   * @param errorText the error text
   */
  public void setErrorText(final String errorText) {
    field.setErrorText(errorText);
  }

  /**
   * Whether the select is read-only — its value is shown but cannot be changed (the menu will not
   * open). Unlike {@linkplain #setEnabled(boolean) disabling}, the chrome stays normal (not
   * dimmed). The embedded editor is non-typeable in either case (V1 is a pure select).
   *
   * @return {@code true} if read-only
   */
  public boolean isReadOnly() {
    return readOnly;
  }

  /**
   * Sets the read-only state. A read-only select shows its value but cannot be opened to change it.
   *
   * @param readOnly {@code true} for read-only
   */
  public void setReadOnly(final boolean readOnly) {
    this.readOnly = readOnly;
    syncEmbeddedReadOnly();
  }

  @Override
  public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    field.setEnabled(enabled);
    arrow.setEnabled(enabled);
  }

  /**
   * Whether the option menu is currently open (the combobox <em>expanded</em> state).
   *
   * @return {@code true} while the menu is showing
   */
  public boolean isExpanded() {
    return expanded;
  }

  @Override
  public Dimension getPreferredSize() {
    return field.getPreferredSize();
  }

  private void installFieldMouse() {
    final MouseAdapter toggleOnPress =
        new MouseAdapter() {
          @Override
          public void mousePressed(final MouseEvent e) {
            if (editable) {
              // Editable combo: the editor owns presses (caret placement); a press on the field
              // chrome just moves focus into the editor. Only the arrow toggles by pointer.
              if (e.getComponent() == field) {
                field.getEditor().requestFocusInWindow();
              }
              return;
            }
            toggle();
          }
        };
    field.addMouseListener(toggleOnPress);
    field.getEditor().addMouseListener(toggleOnPress);
  }

  private void toggle() {
    // While the menu is open, a press on the trigger reaches the overlay's outside-press listener
    // first and begins dismissing it (onClose flips expanded back). Guarding here keeps that same
    // click from immediately reopening the menu — the click reads as a close, not a re-toggle.
    if (expanded) {
      return;
    }
    open();
  }

  private void open() {
    if (options.isEmpty() || readOnly || !isEnabled()) {
      return;
    }
    applyExpandedState(true);
    optionsMenu().open(field);
  }

  private void handleClose() {
    applyExpandedState(false);
  }

  /**
   * Drives the expanded/collapsed visuals + a11y: rotates the arrow (down ↔ up, reduced-motion →
   * instant via the shared {@link MorphAnimator}) and flips the arrow's accessible name + fires the
   * state-change announcement. The menu's own open/close is the caller's concern.
   */
  void applyExpandedState(final boolean expand) {
    this.expanded = expand;
    if (expand) {
      arrowAnim.start();
    } else {
      arrowAnim.reverse();
    }
    updateArrowA11y(expand);
  }

  /** The arrow's current rotation in degrees — the smoke's reduced-motion-instant probe. */
  float arrowAngle() {
    return arrowIcon.angle();
  }

  private void updateArrowA11y(final boolean expand) {
    final AccessibleContext ctx = arrow.getAccessibleContext();
    ctx.setAccessibleName(expand ? "Close options" : "Open options");
    ctx.firePropertyChange(
        AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
        expand ? AccessibleState.COLLAPSED : AccessibleState.EXPANDED,
        expand ? AccessibleState.EXPANDED : AccessibleState.COLLAPSED);
  }

  // Field-focused, menu-closed keyboard: Down / Up / Enter / Space / Alt+Down open the menu (which
  // then owns navigation, Esc-close, and focus-return); a printable key opens and forwards itself
  // to
  // the menu's type-ahead. Once open, focus is on the menu surface, so these never double-fire.
  private void installFieldKeyboard() {
    field
        .getEditor()
        .addKeyListener(
            new KeyAdapter() {
              @Override
              public void keyPressed(final KeyEvent e) {
                if (expanded) {
                  return;
                }
                if (editable) {
                  // Enter/Space are text keys in a typeable editor; only Down (incl. Alt+Down,
                  // the ARIA combobox open gesture) opens.
                  if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    open();
                    e.consume();
                  }
                  return;
                }
                switch (e.getKeyCode()) {
                  case KeyEvent.VK_DOWN:
                  case KeyEvent.VK_UP:
                  case KeyEvent.VK_ENTER:
                  case KeyEvent.VK_SPACE:
                    open();
                    e.consume();
                    break;
                  default:
                    break;
                }
              }

              @Override
              public void keyTyped(final KeyEvent e) {
                if (expanded) {
                  return;
                }
                final char c = e.getKeyChar();
                if (!Character.isLetterOrDigit(c)) {
                  return;
                }
                if (editable) {
                  // Typing begins the combo interaction: open the menu but let the character land
                  // in the editor (no consume, no type-ahead — focus stays in the editor).
                  open();
                  return;
                }
                open();
                forwardTypeAhead(c);
                e.consume();
              }
            });
  }

  /** Re-delivers the opening keystroke to the now-focused menu surface so type-ahead matches it. */
  private void forwardTypeAhead(final char c) {
    SwingUtilities.invokeLater(
        () -> {
          final Component owner =
              KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
          if (owner != null) {
            owner.dispatchEvent(
                new KeyEvent(owner, KeyEvent.KEY_TYPED, 0L, 0, KeyEvent.VK_UNDEFINED, c));
          }
        });
  }

  /**
   * The single selection seam: every selection route (a menu pick, {@link #setSelectedValue}, the
   * smoke's {@link #selectIndex}) funnels here. Updates the menu's {@code selected} marks
   * (single-select — clears the others), writes the display text to the field, and fires the change
   * listeners only when the value actually changed.
   */
  private void applySelection(final int index, final T value) {
    final boolean changed = !Objects.equals(this.selectedValue, value);
    this.selectedValue = value;
    optionsMenu();
    for (int i = 0; i < items.size(); i++) {
      items.get(i).setSelected(i == index);
    }
    field.setText(value == null ? "" : display.apply(value));
    if (changed) {
      fireSelectionChange();
    }
  }

  private void fireSelectionChange() {
    for (final Consumer<T> listener : new ArrayList<>(selectionListeners)) {
      listener.accept(selectedValue);
    }
  }

  /** Write-back for a real menu pick — routed through the shared selection seam. */
  private void commit(final ElwhaMenuItem item) {
    final int index = items.indexOf(item);
    if (index >= 0) {
      applySelection(index, options.get(index));
    }
  }

  /**
   * Selects the option at {@code index}, the shared selection seam the headless smoke drives in
   * lieu of a real popup pick (the public {@link #setSelectedValue} is the consumer-facing route).
   *
   * @param index the option index, ignored if out of range
   */
  void selectIndex(final int index) {
    if (index < 0 || index >= options.size()) {
      return;
    }
    applySelection(index, options.get(index));
  }

  /**
   * The built (and cached) option menu, rebuilt on demand after an options/display change. Visible
   * for the headless smoke, which reads its items/marks while driving selection through {@link
   * #selectIndex} (the popup itself needs a window).
   */
  ElwhaMenu optionsMenu() {
    if (menu == null) {
      rebuildMenu();
    }
    return menu;
  }

  private void rebuildMenu() {
    items.clear();
    final ElwhaMenu.Builder builder =
        ElwhaMenu.builder()
            .selectionMode(SelectionMode.SINGLE)
            .onSelectionChange(this::commit)
            .onClose(cause -> handleClose());
    if (editable) {
      builder.focusHome(field.getEditor());
    }
    for (final T option : options) {
      final ElwhaMenuItem item = ElwhaMenuItem.of(display.apply(option));
      item.setSelected(option.equals(selectedValue));
      items.add(item);
      builder.addItem(item);
    }
    this.menu = builder.build();
  }

  /**
   * An {@link Icon} that paints a delegate rotated about its centre — the arrow's down ↔ up flip,
   * driven by the {@link MorphAnimator}. The delegate is the themed dropdown-arrow glyph, so a 180°
   * rotation reads as {@code arrow_drop_up}; reduced-motion lands at 0° / 180° instantly.
   */
  private static final class RotatableIcon implements Icon {

    private final Icon delegate;
    private float angleDeg;

    RotatableIcon(final Icon delegate) {
      this.delegate = delegate;
    }

    void setAngle(final float degrees) {
      this.angleDeg = degrees;
    }

    float angle() {
      return angleDeg;
    }

    @Override
    public int getIconWidth() {
      return delegate.getIconWidth();
    }

    @Override
    public int getIconHeight() {
      return delegate.getIconHeight();
    }

    @Override
    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
      final Graphics2D g2 = (Graphics2D) g.create();
      try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        final double cx = x + delegate.getIconWidth() / 2.0;
        final double cy = y + delegate.getIconHeight() / 2.0;
        g2.rotate(Math.toRadians(angleDeg), cx, cy);
        delegate.paintIcon(c, g2, x, y);
      } finally {
        g2.dispose();
      }
    }
  }
}
