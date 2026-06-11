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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleState;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

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
 * the menu's {@code focusHome}), and picking an option still writes back. {@link
 * #setMultiSelect(boolean)} opts into the multi-select (Phase 3): the menu toggles any number of
 * options without closing and the field shows a summary of the selection. Editable and multi-select
 * are mutually exclusive in V1 — enabling one disables the other.
 *
 * <p>Phase 1 ships across stories #374–#378, Phase 2 across #391–#394, and Phase 3 across
 * #397–#400; this class began at the S1 skeleton (composition + menu round-trip + arrow toggle) and
 * is extended in place by the later stories (typed value API, keyboard + a11y, variant delegation,
 * Showcase, editable combo, multi-select).
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
  private ElwhaMenuItem noMatchesItem;
  private boolean expanded;
  private boolean readOnly;
  private boolean editable;
  private boolean freeTextAllowed;
  private boolean multiSelect;
  private final List<T> multiValues = new ArrayList<>();
  private final List<Consumer<List<T>>> multiSelectionListeners = new ArrayList<>();
  private int summaryLimit = 3;
  private String filterText = "";
  private String committedText = "";
  private boolean suppressFilter;

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
    installFilterListener();
    installCommitOnFocusLoss();
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
    if (multiSelect) {
      reorderMultiValues();
    }
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
   * Returns the currently selected option value, or {@code null} if nothing is selected. In a
   * {@linkplain #setMultiSelect multi-select} this is the <em>first</em> selected value in option
   * order (mirroring {@code JList.getSelectedValue}), or {@code null} when the selection is empty —
   * read the whole selection via {@link #getSelectedValues()}.
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
   * when the value actually changes. In a {@linkplain #setMultiSelect multi-select} this delegates
   * to {@link #setSelectedValues} with a one-element (or, for {@code null}, empty) collection — the
   * value becomes the entire selection.
   *
   * @param value the option to select, or {@code null} to clear
   */
  public void setSelectedValue(final T value) {
    if (multiSelect) {
      setSelectedValues(value == null ? List.of() : List.of(value));
      return;
    }
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
   * pick or {@link #setSelectedValue}). Not fired for a no-op set to the current value, and not
   * fired in {@linkplain #setMultiSelect multi-select} mode — register a {@linkplain
   * #addMultiSelectionChangeListener multi-selection-change listener} there.
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
    if (editable && multiSelect) {
      setMultiSelect(false);
    }
    this.editable = editable;
    if (expanded && menu != null) {
      menu.close();
    }
    this.menu = null;
    this.filterText = "";
    this.committedText = field.getText();
    field
        .getEditor()
        .getAccessibleContext()
        .setAccessibleDescription(
            editable ? "Editable combo box. Typing filters the options; Down opens them." : null);
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

  // ---- Multi-select (Phase 3, #397) ------------------------------------------
  // The opt-in lift of the single-selection constraint: the menu is built SelectionMode.MULTI
  // (toggling never closes; Esc / light-dismiss do) and the value model becomes an option-ordered
  // list — `multiValues` is canonical, kept pruned to the current options and in option order, with
  // `selectedValue` tracking its first element so the single-value getter stays meaningful.
  // Editable and multi are mutually exclusive in V1: enabling one disables the other.

  /**
   * Whether the select is a multi-select. Default {@code false}: the single select (or, when
   * {@linkplain #setEditable editable}, the combo).
   *
   * @return {@code true} when the menu toggles multiple options
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean isMultiSelect() {
    return multiSelect;
  }

  /**
   * Sets the multi-select mode. When {@code true}, the option menu toggles any number of options
   * and <em>stays open</em> while toggling (Esc, a press outside, or the arrow close it), the field
   * shows a summary of the selection, and the selection is read and written as an option-ordered
   * list via {@link #getSelectedValues()} / {@link #setSelectedValues(Collection)}.
   *
   * <p><strong>Mode flips preserve what they can.</strong> Turning multi-select on seeds the
   * selection with the current single value (when set); turning it off collapses the selection to
   * its first value in option order (the rest clear). Neither flip fires change listeners — the
   * {@linkplain #getSelectedValue() single value} is unchanged by either.
   *
   * <p><strong>Multi-select and the {@linkplain #setEditable editable} combo are mutually exclusive
   * in V1</strong> (a filterable multi-select is a documented deferral, not silently cut — design
   * doc §10): enabling multi-select on an editable combo first disables editable mode, and vice
   * versa.
   *
   * @param multiSelect {@code true} for the multi-select
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setMultiSelect(final boolean multiSelect) {
    if (this.multiSelect == multiSelect) {
      return;
    }
    if (multiSelect && editable) {
      setEditable(false);
    }
    if (expanded && menu != null) {
      menu.close();
    }
    this.menu = null;
    this.multiSelect = multiSelect;
    if (multiSelect) {
      multiValues.clear();
      if (selectedValue != null) {
        multiValues.add(selectedValue);
      }
      writeMultiSummary();
    } else {
      final T first = multiValues.isEmpty() ? null : multiValues.get(0);
      multiValues.clear();
      this.selectedValue = first;
      writeFieldText(first == null ? "" : display.apply(first));
      this.committedText = field.getText();
    }
  }

  /**
   * Returns the selected option values in <em>option order</em> (not toggle order). In the single
   * select this is an empty or one-element list mirroring {@link #getSelectedValue()}; in a
   * {@linkplain #setMultiSelect multi-select} it is the whole selection. Never {@code null}.
   *
   * @return an unmodifiable snapshot of the selected values, in option order
   * @version v0.4.0
   * @since v0.4.0
   */
  public List<T> getSelectedValues() {
    if (!multiSelect) {
      return selectedValue == null ? List.of() : List.of(selectedValue);
    }
    return List.copyOf(multiValues);
  }

  /**
   * Replaces the selection with the given values. Values not among the current {@linkplain
   * #setOptions options} are ignored and duplicates collapse; the resulting selection is kept in
   * option order regardless of the collection's order. {@code null} or an empty collection clears
   * the selection. In the single select (multi off) this leniently applies the <em>first</em>
   * recognized value as the single selection — one write path per mode, no exceptions.
   *
   * @param values the options to select, or {@code null} to clear
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setSelectedValues(final Collection<T> values) {
    if (!multiSelect) {
      if (values != null) {
        for (final T value : values) {
          if (options.contains(value)) {
            setSelectedValue(value);
            return;
          }
        }
      }
      setSelectedValue(null);
      return;
    }
    final List<T> next = new ArrayList<>();
    if (values != null) {
      for (final T option : options) {
        if (values.contains(option) && !next.contains(option)) {
          next.add(option);
        }
      }
    }
    final boolean changed = !next.equals(multiValues);
    multiValues.clear();
    multiValues.addAll(next);
    this.selectedValue = multiValues.isEmpty() ? null : multiValues.get(0);
    syncMultiMarks();
    writeMultiSummary();
    if (changed) {
      fireMultiSelectionChange();
    }
  }

  /**
   * Registers a listener notified with the current selection — an option-ordered, unmodifiable
   * snapshot — on every change while in {@linkplain #setMultiSelect multi-select} mode: each menu
   * toggle (the menu stays open, so a listener fires per toggle) and every effective {@link
   * #setSelectedValues} / {@link #setSelectedValue}. Not fired for a no-op set, outside multi mode,
   * or on a {@linkplain #setMultiSelect mode flip}. The single-value {@linkplain
   * #addSelectionChangeListener listeners} are the single-select counterpart — they do not fire in
   * multi mode.
   *
   * @param listener the change listener; {@code null} is ignored
   * @version v0.4.0
   * @since v0.4.0
   */
  public void addMultiSelectionChangeListener(final Consumer<List<T>> listener) {
    if (listener != null) {
      multiSelectionListeners.add(listener);
    }
  }

  /**
   * Removes a previously {@linkplain #addMultiSelectionChangeListener registered}
   * multi-selection-change listener.
   *
   * @param listener the listener to remove
   * @version v0.4.0
   * @since v0.4.0
   */
  public void removeMultiSelectionChangeListener(final Consumer<List<T>> listener) {
    multiSelectionListeners.remove(listener);
  }

  /**
   * The summary overflow threshold (default {@code 3}): the largest selection the field still
   * renders as joined display strings. Bigger selections render as the count form.
   *
   * @return the summary limit
   * @version v0.4.0
   * @since v0.4.0
   */
  public int getSummaryLimit() {
    return summaryLimit;
  }

  /**
   * Sets the summary overflow threshold. While the selection holds at most this many values, the
   * field shows their display strings joined in option order (e.g. {@code Earth, Mars}); past it,
   * the count form (e.g. {@code 5 selected}) — so a wide selection never blows out the field. A
   * negative limit clamps to {@code 0} (any non-empty selection shows the count form). The summary
   * re-renders immediately when the limit changes.
   *
   * @param summaryLimit the largest selection rendered as a join
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setSummaryLimit(final int summaryLimit) {
    this.summaryLimit = Math.max(0, summaryLimit);
    if (multiSelect) {
      writeMultiSummary();
    }
  }

  // The MULTI menu's onSelectionChange: the item's selected flag has already flipped — mirror it
  // into the value model and refresh the summary, leaving the menu open.
  private void multiToggle(final ElwhaMenuItem item) {
    final int index = items.indexOf(item);
    if (index < 0) {
      return;
    }
    final T value = options.get(index);
    if (item.isSelected()) {
      if (!multiValues.contains(value)) {
        multiValues.add(value);
      }
    } else {
      multiValues.remove(value);
    }
    reorderMultiValues();
    writeMultiSummary();
    fireMultiSelectionChange();
  }

  // Re-derives multiValues in option order, pruning values no longer among the options, and keeps
  // selectedValue on the first element — the model invariant every mutation funnels through.
  private void reorderMultiValues() {
    final List<T> ordered = new ArrayList<>();
    for (final T option : options) {
      if (multiValues.contains(option) && !ordered.contains(option)) {
        ordered.add(option);
      }
    }
    multiValues.clear();
    multiValues.addAll(ordered);
    this.selectedValue = multiValues.isEmpty() ? null : multiValues.get(0);
  }

  // The M3 summary display (S2, #398): joined display strings in option order up to the summary
  // limit, the count form past it, the empty selection resting the floating label.
  private void writeMultiSummary() {
    writeFieldText(summaryText());
  }

  private String summaryText() {
    if (multiValues.isEmpty()) {
      return "";
    }
    if (multiValues.size() > summaryLimit) {
      return multiValues.size() + " selected";
    }
    final StringBuilder sb = new StringBuilder();
    for (final T value : multiValues) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(display.apply(value));
    }
    return sb.toString();
  }

  private void fireMultiSelectionChange() {
    final List<T> snapshot = List.copyOf(multiValues);
    for (final Consumer<List<T>> listener : new ArrayList<>(multiSelectionListeners)) {
      listener.accept(snapshot);
    }
  }

  private void syncMultiMarks() {
    if (menu == null) {
      return;
    }
    for (int i = 0; i < items.size(); i++) {
      items.get(i).setSelected(multiValues.contains(options.get(i)));
    }
  }

  /**
   * Toggles the option at {@code index} through the same path a real MULTI menu pick takes — the
   * headless smoke's seam (the popup itself needs a window). A no-op outside multi-select mode or
   * range.
   *
   * @param index the option index to toggle
   */
  void toggleIndex(final int index) {
    if (!multiSelect || index < 0 || index >= options.size()) {
      return;
    }
    optionsMenu();
    final ElwhaMenuItem item = items.get(index);
    item.setSelected(!item.isSelected());
    multiToggle(item);
  }

  private void writeFieldText(final String text) {
    this.suppressFilter = true;
    try {
      field.setText(text);
    } finally {
      this.suppressFilter = false;
    }
  }

  // ---- Filter-as-you-type (Phase 2, #392) -----------------------------------
  // Live typing (not programmatic write-backs — those are suppressed) becomes the filter text;
  // the cached menu's items are shown/hidden in place via ElwhaMenu.setVisibleItems, so filtering
  // never rebuilds the menu and composes with the rebuild-on-options-change lifecycle. A pick or
  // setSelectedValue clears the filter, so the next open shows the full list again.

  private void installFilterListener() {
    field
        .getEditor()
        .getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void insertUpdate(final DocumentEvent e) {
                onEditorTextChanged();
              }

              @Override
              public void removeUpdate(final DocumentEvent e) {
                onEditorTextChanged();
              }

              @Override
              public void changedUpdate(final DocumentEvent e) {
                onEditorTextChanged();
              }
            });
  }

  private void onEditorTextChanged() {
    if (!editable || suppressFilter) {
      return;
    }
    this.filterText = field.getText();
    applyFilter();
  }

  /**
   * Pushes the current filter onto the cached menu: a case-insensitive match against each option's
   * display string (a prefix match and a substring match both qualify), the empty filter showing
   * everything. When nothing matches, the disabled "No matches" placeholder row is shown instead of
   * a stale list. No-op while the menu is unbuilt — {@link #rebuildMenu()} re-applies the filter
   * after building.
   */
  private void applyFilter() {
    if (menu == null || !editable) {
      return;
    }
    final List<ElwhaMenuItem> visible = new ArrayList<>();
    final String filter = filterText.toLowerCase(Locale.ROOT);
    for (int i = 0; i < items.size(); i++) {
      if (filter.isEmpty()
          || display.apply(options.get(i)).toLowerCase(Locale.ROOT).contains(filter)) {
        visible.add(items.get(i));
      }
    }
    if (visible.isEmpty() && noMatchesItem != null) {
      visible.add(noMatchesItem);
    }
    menu.setVisibleItems(visible);
    highlightPreferredMatch(visible, filter);
  }

  // The "prefix, then substring" priority surfaces as the highlight: the first prefix match gets
  // the active-option ring while the menu is open; absent one, the first visible match does.
  private void highlightPreferredMatch(final List<ElwhaMenuItem> visible, final String filter) {
    if (filter.isEmpty() || visible.isEmpty() || visible.get(0) == noMatchesItem) {
      return;
    }
    ElwhaMenuItem preferred = visible.get(0);
    for (int i = 0; i < items.size(); i++) {
      if (visible.contains(items.get(i))
          && display.apply(options.get(i)).toLowerCase(Locale.ROOT).startsWith(filter)) {
        preferred = items.get(i);
        break;
      }
    }
    menu.highlight(preferred);
  }

  // ---- Editable value model + commit semantics (Phase 2, #393) ---------------
  // Commit points: Enter (the menu highlight when one is committable, else the field text),
  // focus leaving the control, and a menu pick. Esc with the menu closed reverts to the last
  // committed value; with it open, Esc just closes (the menu's own binding). The committed value
  // is the single source of truth the field text resolves back to.

  /**
   * Whether free text — text that matches no option — may be committed in the {@linkplain
   * #setEditable editable} combo. Default {@code false}: the combo is constrained to its options.
   *
   * @return {@code true} when free text commits
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean isFreeTextAllowed() {
    return freeTextAllowed;
  }

  /**
   * Sets the free-text policy for the {@linkplain #setEditable editable} combo. When {@code false}
   * (the default, constrained), committing text that is not an option reverts the field to the last
   * committed value. When {@code true}, the free text is kept: the field shows it (read it via
   * {@link #getText()}), {@link #getSelectedValue()} becomes {@code null} (free text maps to no
   * option), the menu's {@code selected} marks clear, and the {@linkplain
   * #addSelectionChangeListener selection-change listeners} fire (with {@code null}) when that
   * cleared a previous selection. Text that matches an option case-insensitively always resolves to
   * the option itself (canonicalizing the display), under either policy.
   *
   * @param freeTextAllowed {@code true} to allow committing free text
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setFreeTextAllowed(final boolean freeTextAllowed) {
    this.freeTextAllowed = freeTextAllowed;
  }

  private void installCommitOnFocusLoss() {
    field
        .getEditor()
        .addFocusListener(
            new FocusAdapter() {
              @Override
              public void focusLost(final FocusEvent e) {
                // Neither a temporary loss (window deactivation — typing resumes on return) nor
                // a hop within the control (editor -> arrow) is a commit point.
                if (!editable
                    || e.isTemporary()
                    || (e.getOppositeComponent() != null
                        && SwingUtilities.isDescendingFrom(
                            e.getOppositeComponent(), ElwhaSelectField.this))) {
                  return;
                }
                resolveFieldText();
              }
            });
  }

  /**
   * Resolves the field's current text into a committed value — the Enter / focus-loss seam (also
   * the headless smoke's). Text matching an option case-insensitively selects that option
   * (canonicalizing the display); otherwise the {@linkplain #setFreeTextAllowed free-text policy}
   * decides between keeping the text and reverting to the last committed value.
   */
  void resolveFieldText() {
    if (!editable) {
      return;
    }
    final String text = field.getText();
    for (int i = 0; i < options.size(); i++) {
      if (display.apply(options.get(i)).equalsIgnoreCase(text)) {
        applySelection(i, options.get(i));
        return;
      }
    }
    if (freeTextAllowed) {
      commitFreeText(text);
    } else {
      revertToCommitted();
    }
  }

  /** Reverts the field text to the last committed value — the Esc seam (menu closed). */
  void revertToCommitted() {
    if (!editable) {
      return;
    }
    this.suppressFilter = true;
    try {
      field.setText(committedText);
    } finally {
      this.suppressFilter = false;
    }
    this.filterText = "";
    applyFilter();
  }

  private void commitFreeText(final String text) {
    final boolean changed = this.selectedValue != null;
    this.selectedValue = null;
    optionsMenu();
    for (final ElwhaMenuItem item : items) {
      item.setSelected(false);
    }
    this.committedText = text;
    this.filterText = "";
    applyFilter();
    if (changed) {
      fireSelectionChange();
    }
  }

  // Enter while the menu is open: commit the highlight when it is committable; the "No matches"
  // placeholder (or an empty highlight) falls back to the field-text commit.
  private void commitFromMenu() {
    final ElwhaMenu open = optionsMenu();
    final ElwhaMenuItem highlighted = open.getHighlightedItem();
    if (highlighted != null && highlighted.isEnabled()) {
      open.activateHighlighted();
      return;
    }
    open.close();
    resolveFieldText();
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
    updateEditorA11y(expand);
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

  // The aria-autocomplete approximation: the typeable editor itself announces the popup's
  // expanded/collapsed flips (Swing has no combobox role for a text component), mirroring the
  // arrow's state change. The option list keeps its MENU_ITEM roles + selected states.
  private void updateEditorA11y(final boolean expand) {
    if (!editable) {
      return;
    }
    field
        .getEditor()
        .getAccessibleContext()
        .firePropertyChange(
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
                if (editable && expanded) {
                  // Focus stays in the editor while the menu is open, so the menu's own bindings
                  // are inert — route the combobox navigation keys to it from here. Esc is left
                  // for the menu surface's WHEN_IN_FOCUSED_WINDOW dismiss binding.
                  switch (e.getKeyCode()) {
                    case KeyEvent.VK_DOWN:
                      optionsMenu().moveHighlight(1);
                      e.consume();
                      break;
                    case KeyEvent.VK_UP:
                      optionsMenu().moveHighlight(-1);
                      e.consume();
                      break;
                    case KeyEvent.VK_ENTER:
                      commitFromMenu();
                      e.consume();
                      break;
                    default:
                      break;
                  }
                  return;
                }
                if (expanded) {
                  return;
                }
                if (editable) {
                  // Enter commits the typed text, Esc reverts to the committed value, Down (incl.
                  // Alt+Down, the ARIA combobox open gesture) opens. Space stays a text key.
                  switch (e.getKeyCode()) {
                    case KeyEvent.VK_DOWN:
                      open();
                      e.consume();
                      break;
                    case KeyEvent.VK_ENTER:
                      resolveFieldText();
                      e.consume();
                      break;
                    case KeyEvent.VK_ESCAPE:
                      revertToCommitted();
                      e.consume();
                      break;
                    default:
                      break;
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
    writeFieldText(value == null ? "" : display.apply(value));
    this.committedText = field.getText();
    this.filterText = "";
    applyFilter();
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
            .selectionMode(multiSelect ? SelectionMode.MULTI : SelectionMode.SINGLE)
            .onSelectionChange(multiSelect ? this::multiToggle : this::commit)
            .onClose(cause -> handleClose());
    if (editable) {
      builder.focusHome(field.getEditor());
    }
    for (final T option : options) {
      final ElwhaMenuItem item = ElwhaMenuItem.of(display.apply(option));
      item.setSelected(multiSelect ? multiValues.contains(option) : option.equals(selectedValue));
      items.add(item);
      builder.addItem(item);
    }
    if (editable) {
      // The no-match state: a disabled placeholder row shown only when the filter empties the
      // list. Deliberately not in `items`, so it can never map to an option index.
      this.noMatchesItem = ElwhaMenuItem.of("No matches");
      noMatchesItem.setEnabled(false);
      noMatchesItem.setVisible(false);
      builder.addItem(noMatchesItem);
    } else {
      this.noMatchesItem = null;
    }
    this.menu = builder.build();
    applyFilter();
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
