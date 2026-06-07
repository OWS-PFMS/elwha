package com.owspfm.elwha.selectfield;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.iconbutton.IconButtonSize;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.menu.ElwhaMenu;
import com.owspfm.elwha.menu.ElwhaMenuItem;
import com.owspfm.elwha.menu.SelectionMode;
import com.owspfm.elwha.textfield.ElwhaTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.swing.JComponent;

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

  private final ElwhaTextField field;
  private final ElwhaIconButton arrow;
  private final List<ElwhaMenuItem> items = new ArrayList<>();

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
    this.arrow = new ElwhaIconButton(MaterialIcons.arrowDropDown(ARROW_PX));
    this.arrow.setButtonSize(IconButtonSize.S);
    this.field.setTrailingIconButton(arrow);

    setLayout(new BorderLayout());
    setOpaque(false);
    add(field, BorderLayout.CENTER);

    arrow.addActionListener(e -> toggle());
    installFieldMouse();
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
    expanded = true;
    arrow.setIcon(MaterialIcons.arrowDropUp(ARROW_PX));
    optionsMenu().open(field);
  }

  private void handleClose() {
    expanded = false;
    arrow.setIcon(MaterialIcons.arrowDropDown(ARROW_PX));
  }

  private void applyValue(final T value) {
    this.selectedValue = value;
    field.setText(value == null ? "" : display.apply(value));
  }

  /** Write-back for a real menu pick — the menu has already updated its {@code selected} marks. */
  private void commit(final ElwhaMenuItem item) {
    final int index = items.indexOf(item);
    if (index >= 0) {
      applyValue(options.get(index));
    }
  }

  /**
   * Selects the option at {@code index} programmatically: updates the menu's {@code selected} marks
   * (single-select — clears the others) and writes the value back to the field. The shared internal
   * selection seam the headless smoke drives in lieu of a real popup pick; the public typed-value
   * API ({@code setSelectedValue}) lands in S2 (#375) on top of it.
   *
   * @param index the option index, ignored if out of range
   */
  void selectIndex(final int index) {
    if (index < 0 || index >= options.size()) {
      return;
    }
    optionsMenu();
    for (int i = 0; i < items.size(); i++) {
      items.get(i).setSelected(i == index);
    }
    applyValue(options.get(index));
  }

  /**
   * The built (and cached) option menu, rebuilt on demand after an options/display change. Visible
   * for the headless smoke, which drives selection via {@link ElwhaMenuItem#activate(int)} without
   * a window.
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
}
