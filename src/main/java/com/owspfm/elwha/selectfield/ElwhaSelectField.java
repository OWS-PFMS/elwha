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
 * <p><strong>V1 is the non-editable (pure) select.</strong> The embedded field is read-only — the
 * field and the trailing arrow are the open/close affordance, and picking a menu item is the only
 * way to set the value. The editable filter-as-you-type combo (Phase 2) and multi-select (Phase 3)
 * are later phases.
 *
 * <p>Phase 1 ships across stories #374–#378; this class begins at the S1 skeleton (composition +
 * menu round-trip + arrow toggle) and is extended in place by the later stories (typed value API,
 * keyboard + a11y, variant delegation, Showcase).
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
    if (options.isEmpty()) {
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
