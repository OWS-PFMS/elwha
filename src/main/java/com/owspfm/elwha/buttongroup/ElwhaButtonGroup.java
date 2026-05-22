package com.owspfm.elwha.buttongroup;

import com.owspfm.elwha.button.ButtonInteractionMode;
import com.owspfm.elwha.button.ButtonShape;
import com.owspfm.elwha.button.ButtonSize;
import com.owspfm.elwha.button.ButtonVariant;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.iconbutton.IconButtonInteractionMode;
import com.owspfm.elwha.iconbutton.IconButtonSize;
import com.owspfm.elwha.iconbutton.IconButtonVariant;
import com.owspfm.elwha.theme.CornerRadii;
import com.owspfm.elwha.theme.ShapeScale;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JComponent;

/**
 * The M3 Expressive <strong>Button group</strong> — an invisible visual + layout + selection
 * container that composes {@link ElwhaButton} and {@link ElwhaIconButton} segments.
 *
 * <p><strong>Variants.</strong> {@link ButtonGroupVariant#STANDARD} lays the segments out with a
 * size-dependent visible gap and lets each render its own rounded shape — a toolbar / action
 * cluster. {@link ButtonGroupVariant#CONNECTED} butts the segments edge-to-edge with a 2&nbsp;dp
 * inner padding, gives them a shared corner treatment (rounded outer corners, nearly-square inner
 * corners), grows them to fill the container width, and stamps one colour style across all of them
 * — the selection control that replaces M3's deprecated Segmented Button.
 *
 * <p><strong>Selection.</strong> The group owns the selection model for all three M3 modes ({@link
 * SelectionMode#SINGLE} / {@link SelectionMode#MULTI} / {@link SelectionMode#REQUIRED}) — every
 * segment is coerced to its primitive's selectable interaction mode on add, and the group enforces
 * the mode and emits one unified {@link SelectionListener} event. A {@code REQUIRED} group seeds
 * its first segment selected so the "exactly one" invariant holds from the start.
 *
 * <p><strong>Selected-segment shape.</strong> The selected / unselected segment shape is fixed per
 * variant — there is no shape configuration. A {@link ButtonGroupVariant#STANDARD} group's selected
 * segment renders square while its unselected segments stay round; a {@link
 * ButtonGroupVariant#CONNECTED} group's selected segment pops to a uniform round pill out of its
 * pill-ended bar. The transient press width / shape morph is a separate polish epic and is not
 * rendered statically here.
 *
 * <p><strong>Naming.</strong> Not to be confused with {@link com.owspfm.elwha.button.ButtonGroup} /
 * {@link com.owspfm.elwha.iconbutton.IconButtonGroup}, which are pure selection-mutex helpers with
 * no layout or paint. {@code ElwhaButtonGroup} is the visual container.
 *
 * <p><strong>Quick start:</strong>
 *
 * <pre>{@code
 * ElwhaButtonGroup viewMode = ElwhaButtonGroup.connected()
 *     .setSelectionMode(SelectionMode.REQUIRED)
 *     .setButtonSize(ButtonSize.S)
 *     .add("List", "Grid", "Compact");
 * viewMode.addSelectionListener(group ->
 *     System.out.println("selected: " + group.getSelectedIndex()));
 * }</pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ElwhaButtonGroup extends JComponent {

  /** The inner padding between butted segments of a connected group, in pixels (M3 §11). */
  private static final int CONNECTED_GAP_PX = 2;

  private final List<Segment> segments = new ArrayList<>();
  private final List<SelectionListener> selectionListeners = new ArrayList<>();
  private final PropertyChangeListener segmentSelectionListener = this::onSegmentSelectionChanged;

  private ButtonGroupVariant variant = ButtonGroupVariant.STANDARD;
  private SelectionMode selectionMode = SelectionMode.SINGLE;
  private ButtonSize buttonSize = ButtonSize.S;
  private ResizeMode resizeMode = ResizeMode.FLEXIBLE;
  private ButtonGroupColorStyle colorStyle = ButtonGroupColorStyle.TONAL;
  private int maxWidthPx;

  private boolean adjusting;
  private List<Integer> lastNotifiedSelection = List.of();

  // ----------------------------------------------------------------- ctors

  /**
   * Creates an empty {@link ButtonGroupVariant#STANDARD} group. Supply segments with {@link
   * #add(ElwhaButton)} / {@link #add(ElwhaIconButton)}.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaButtonGroup() {
    this(ButtonGroupVariant.STANDARD);
  }

  /**
   * Creates an empty group of the given variant.
   *
   * @param variant the group variant; {@code null} falls back to {@link
   *     ButtonGroupVariant#STANDARD}
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaButtonGroup(final ButtonGroupVariant variant) {
    this.variant = variant != null ? variant : ButtonGroupVariant.STANDARD;
    setOpaque(false);
    // §13 — the container is not focusable and carries no accessible name; each segment is its own
    // tab stop, which Swing's default focus traversal already provides.
    setFocusable(false);
  }

  // ---------------------------------------------------------- factory presets

  /**
   * Creates an empty {@link ButtonGroupVariant#STANDARD} group — the gapped toolbar cluster.
   *
   * @return a configured standard group
   * @version v0.3.0
   * @since v0.3.0
   */
  public static ElwhaButtonGroup standard() {
    return new ElwhaButtonGroup(ButtonGroupVariant.STANDARD);
  }

  /**
   * Creates an empty {@link ButtonGroupVariant#CONNECTED} group — the butted selection control.
   *
   * @return a configured connected group
   * @version v0.3.0
   * @since v0.3.0
   */
  public static ElwhaButtonGroup connected() {
    return new ElwhaButtonGroup(ButtonGroupVariant.CONNECTED);
  }

  // -------------------------------------------------------------- segments

  /**
   * Adds a text button as the next segment. The button is coerced to {@link
   * ButtonInteractionMode#SELECTABLE} and re-sized to the group size; a connected group also stamps
   * its colour style onto it.
   *
   * @param button the button to add
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if {@code button} is {@code null}
   * @throws IllegalArgumentException if {@code button} is a {@link ButtonVariant#TEXT} button — M3
   *     excludes text buttons from button groups
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaButtonGroup add(final ElwhaButton button) {
    if (button == null) {
      throw new NullPointerException("button");
    }
    if (button.getVariant() == ButtonVariant.TEXT) {
      throw new IllegalArgumentException(
          "An ElwhaButtonGroup segment cannot be a TEXT-variant button — M3 excludes text buttons"
              + " from button groups. Use FILLED, FILLED_TONAL, or OUTLINED.");
    }
    addSegment(new ButtonSegment(button));
    return this;
  }

  /**
   * Adds an icon button as the next segment. The button is coerced to {@link
   * IconButtonInteractionMode#SELECTABLE} and re-sized to the group size; a connected group also
   * stamps its colour style onto it. Click-to-focus is enabled so a clicked segment takes focus, as
   * a segmented control expects.
   *
   * @param button the icon button to add
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if {@code button} is {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaButtonGroup add(final ElwhaIconButton button) {
    if (button == null) {
      throw new NullPointerException("button");
    }
    button.setRequestFocusEnabled(true);
    addSegment(new IconButtonSegment(button));
    return this;
  }

  /**
   * Adds one text-button segment per label — the convenience for the common case where the caller
   * supplies only segment text and lets the group own size, shape, and selection. Each label
   * becomes a {@link ButtonVariant#FILLED_TONAL} {@link ElwhaButton}; a {@link
   * ButtonGroupVariant#CONNECTED} group then stamps its own colour style over each, and a {@link
   * ButtonGroupVariant#STANDARD} group keeps the tonal default — use {@link #add(ElwhaButton)} for
   * a standard group whose segments mix colour styles. Each built button is coerced and re-sized
   * exactly as {@link #add(ElwhaButton)}.
   *
   * @param labels the segment labels, added in order
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if {@code labels} or any element is {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaButtonGroup add(final String... labels) {
    if (labels == null) {
      throw new NullPointerException("labels");
    }
    for (final String label : labels) {
      if (label == null) {
        throw new NullPointerException("label");
      }
      add(ElwhaButton.filledTonalButton(label));
    }
    return this;
  }

  /**
   * Adds a single text-button segment carrying a label and a leading icon — the {@link
   * #add(String...)} convenience extended with an icon. The button is a {@link
   * ButtonVariant#FILLED_TONAL} {@link ElwhaButton}; a {@link ButtonGroupVariant#CONNECTED} group
   * then stamps its own colour style over it.
   *
   * @param label the segment label
   * @param icon the leading icon; may be {@code null}
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if {@code label} is {@code null}
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaButtonGroup add(final String label, final Icon icon) {
    if (label == null) {
      throw new NullPointerException("label");
    }
    return add(new ElwhaButton(label, icon).setVariant(ButtonVariant.FILLED_TONAL));
  }

  private void addSegment(final Segment segment) {
    segments.add(segment);
    segment.makeSelectable();
    segment.addSelectionListener(segmentSelectionListener);
    super.add(segment.component());
    // A REQUIRED group must always carry exactly one selection — seed the first segment so the
    // invariant holds before the user has clicked anything.
    if (selectionMode == SelectionMode.REQUIRED && !anySelected()) {
      adjusting = true;
      try {
        segment.setSelected(true);
      } finally {
        adjusting = false;
      }
    }
    refreshSegments();
    fireSelectionChanged();
  }

  /**
   * Removes every segment from the group.
   *
   * @return {@code this} for fluent chaining
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaButtonGroup clear() {
    for (final Segment segment : segments) {
      segment.removeSelectionListener(segmentSelectionListener);
      super.remove(segment.component());
    }
    segments.clear();
    refreshSegments();
    fireSelectionChanged();
    return this;
  }

  /**
   * Returns the number of segments in the group.
   *
   * @return the segment count
   * @version v0.3.0
   * @since v0.3.0
   */
  public int getButtonCount() {
    return segments.size();
  }

  /**
   * Returns the segment component at the given index — an {@link ElwhaButton} or {@link
   * ElwhaIconButton}.
   *
   * @param index the zero-based segment index
   * @return the segment component
   * @throws IndexOutOfBoundsException if {@code index} is out of range
   * @version v0.3.0
   * @since v0.3.0
   */
  public JComponent getButtonAt(final int index) {
    return segments.get(index).component();
  }

  // -------------------------------------------------------------- variant

  /**
   * Sets the group variant and re-applies the layout + per-segment treatment.
   *
   * @param variant the new variant; ignored if {@code null}
   * @return {@code this} for fluent chaining
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaButtonGroup setVariant(final ButtonGroupVariant variant) {
    if (variant == null || variant == this.variant) {
      return this;
    }
    this.variant = variant;
    refreshSegments();
    return this;
  }

  /**
   * Returns the active group variant.
   *
   * @return the active variant (never {@code null})
   * @version v0.3.0
   * @since v0.3.0
   */
  public ButtonGroupVariant getVariant() {
    return variant;
  }

  // ---------------------------------------------------------- selection mode

  /**
   * Sets the selection mode. Switching to {@link SelectionMode#REQUIRED} with no current selection
   * seeds the first segment. Switching to {@link SelectionMode#SINGLE} or {@link
   * SelectionMode#REQUIRED} while more than one segment is selected collapses the selection to the
   * first selected segment.
   *
   * @param mode the new selection mode; ignored if {@code null}
   * @return {@code this} for fluent chaining
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaButtonGroup setSelectionMode(final SelectionMode mode) {
    if (mode == null || mode == this.selectionMode) {
      return this;
    }
    this.selectionMode = mode;
    if (mode != SelectionMode.MULTI) {
      collapseToSingleSelection();
    }
    if (mode == SelectionMode.REQUIRED && !anySelected() && !segments.isEmpty()) {
      adjusting = true;
      try {
        segments.get(0).setSelected(true);
      } finally {
        adjusting = false;
      }
    }
    refreshSegments();
    fireSelectionChanged();
    return this;
  }

  /**
   * Returns the active selection mode.
   *
   * @return the active selection mode (never {@code null})
   * @version v0.3.0
   * @since v0.3.0
   */
  public SelectionMode getSelectionMode() {
    return selectionMode;
  }

  // -------------------------------------------------------------- sizing

  /**
   * Sets the group-wide M3 size tier, applied uniformly to every segment.
   *
   * @param size the new size; ignored if {@code null}
   * @return {@code this} for fluent chaining
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaButtonGroup setButtonSize(final ButtonSize size) {
    if (size == null || size == this.buttonSize) {
      return this;
    }
    this.buttonSize = size;
    refreshSegments();
    return this;
  }

  /**
   * Returns the group-wide size tier.
   *
   * @return the active size (never {@code null})
   * @version v0.3.0
   * @since v0.3.0
   */
  public ButtonSize getButtonSize() {
    return buttonSize;
  }

  /**
   * Sets the resize mode. Only the {@link ButtonGroupVariant#CONNECTED} variant is affected — a
   * connected {@link ResizeMode#FLEXIBLE} group fills its container width, a {@link
   * ResizeMode#FIXED} one hugs its content. A standard group always hugs.
   *
   * @param mode the new resize mode; ignored if {@code null}
   * @return {@code this} for fluent chaining
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaButtonGroup setResizeMode(final ResizeMode mode) {
    if (mode == null || mode == this.resizeMode) {
      return this;
    }
    this.resizeMode = mode;
    revalidate();
    repaint();
    return this;
  }

  /**
   * Returns the active resize mode.
   *
   * @return the active resize mode (never {@code null})
   * @version v0.3.0
   * @since v0.3.0
   */
  public ResizeMode getResizeMode() {
    return resizeMode;
  }

  /**
   * Sets the colour style applied to every segment of a {@link ButtonGroupVariant#CONNECTED} group.
   * Ignored by the {@link ButtonGroupVariant#STANDARD} variant, where segments keep their own
   * colour styles.
   *
   * @param style the new colour style; ignored if {@code null}
   * @return {@code this} for fluent chaining
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaButtonGroup setColorStyle(final ButtonGroupColorStyle style) {
    if (style == null || style == this.colorStyle) {
      return this;
    }
    this.colorStyle = style;
    refreshSegments();
    return this;
  }

  /**
   * Returns the connected-variant colour style.
   *
   * @return the active colour style (never {@code null})
   * @version v0.3.0
   * @since v0.3.0
   */
  public ButtonGroupColorStyle getColorStyle() {
    return colorStyle;
  }

  /**
   * Sets the maximum width in pixels a {@link ButtonGroupVariant#CONNECTED} {@link
   * ResizeMode#FLEXIBLE} group grows to. {@code 0} (the default) leaves the width unclamped.
   *
   * @param px the maximum width, clamped to {@code >= 0}; {@code 0} for unbounded
   * @return {@code this} for fluent chaining
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaButtonGroup setMaxWidth(final int px) {
    this.maxWidthPx = Math.max(0, px);
    revalidate();
    repaint();
    return this;
  }

  /**
   * Returns the connected flexible-width clamp in pixels, or {@code 0} when unbounded.
   *
   * @return the maximum width, or {@code 0}
   * @version v0.3.0
   * @since v0.3.0
   */
  public int getMaxWidth() {
    return maxWidthPx;
  }

  // ------------------------------------------------------------ selection

  /**
   * Selects or deselects the segment at the given index, applying the active {@link SelectionMode}
   * — in {@link SelectionMode#SINGLE} / {@link SelectionMode#REQUIRED} selecting a segment
   * deselects the others, and in {@code REQUIRED} a deselect that would empty the group is refused.
   *
   * @param index the zero-based segment index
   * @param selected the desired selected state
   * @return {@code this} for fluent chaining
   * @throws IndexOutOfBoundsException if {@code index} is out of range
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaButtonGroup setSelected(final int index, final boolean selected) {
    segments.get(index).setSelected(selected);
    return this;
  }

  /**
   * Selects the segment at the given index — convenience for {@link #setSelected(int, boolean)
   * setSelected(index, true)}.
   *
   * @param index the zero-based segment index
   * @return {@code this} for fluent chaining
   * @throws IndexOutOfBoundsException if {@code index} is out of range
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaButtonGroup setSelectedIndex(final int index) {
    return setSelected(index, true);
  }

  /**
   * Returns the index of the first selected segment, or {@code -1} if none is selected.
   *
   * @return the first selected index, or {@code -1}
   * @version v0.3.0
   * @since v0.3.0
   */
  public int getSelectedIndex() {
    for (int i = 0; i < segments.size(); i++) {
      if (segments.get(i).isSelected()) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Returns the indices of every selected segment, ascending — a single-element list for {@link
   * SelectionMode#SINGLE} / {@link SelectionMode#REQUIRED}, any number for {@link
   * SelectionMode#MULTI}, empty when nothing is selected.
   *
   * @return an unmodifiable ascending list of selected indices
   * @version v0.3.0
   * @since v0.3.0
   */
  public List<Integer> getSelectedIndices() {
    final List<Integer> selected = new ArrayList<>();
    for (int i = 0; i < segments.size(); i++) {
      if (segments.get(i).isSelected()) {
        selected.add(i);
      }
    }
    return Collections.unmodifiableList(selected);
  }

  /**
   * Clears the selection — deselects every segment.
   *
   * @return {@code this} for fluent chaining
   * @throws IllegalStateException if the group is in {@link SelectionMode#REQUIRED} mode, which
   *     forbids an empty selection
   * @version v0.3.0
   * @since v0.3.0
   */
  public ElwhaButtonGroup clearSelection() {
    if (selectionMode == SelectionMode.REQUIRED) {
      throw new IllegalStateException(
          "Cannot clear the selection of a REQUIRED ElwhaButtonGroup — exactly one segment must"
              + " remain selected.");
    }
    adjusting = true;
    try {
      for (final Segment segment : segments) {
        segment.setSelected(false);
      }
    } finally {
      adjusting = false;
    }
    refreshSegments();
    fireSelectionChanged();
    return this;
  }

  /**
   * Registers a listener notified once after every selection change, whatever its cause — a user
   * click, a keyboard activation, or a programmatic {@link #setSelected(int, boolean)}.
   *
   * @param listener the listener; {@code null} is ignored
   * @version v0.3.0
   * @since v0.3.0
   */
  public void addSelectionListener(final SelectionListener listener) {
    if (listener != null) {
      selectionListeners.add(listener);
    }
  }

  /**
   * Removes a previously registered selection listener.
   *
   * @param listener the listener to remove
   * @version v0.3.0
   * @since v0.3.0
   */
  public void removeSelectionListener(final SelectionListener listener) {
    selectionListeners.remove(listener);
  }

  // ----------------------------------------------------------- internals

  private void onSegmentSelectionChanged(final PropertyChangeEvent event) {
    if (adjusting) {
      return;
    }
    final boolean nowSelected = Boolean.TRUE.equals(event.getNewValue());
    final Segment source = findSegment((JComponent) event.getSource());
    if (source == null) {
      return;
    }
    adjusting = true;
    try {
      if (nowSelected) {
        if (selectionMode != SelectionMode.MULTI) {
          for (final Segment other : segments) {
            if (other != source && other.isSelected()) {
              other.setSelected(false);
            }
          }
        }
      } else if (selectionMode == SelectionMode.REQUIRED && !anySelected()) {
        // Refuse the deselect — a REQUIRED group keeps exactly one segment selected.
        source.setSelected(true);
      }
    } finally {
      adjusting = false;
    }
    refreshSegments();
    fireSelectionChanged();
  }

  private Segment findSegment(final JComponent component) {
    for (final Segment segment : segments) {
      if (segment.component() == component) {
        return segment;
      }
    }
    return null;
  }

  private boolean anySelected() {
    for (final Segment segment : segments) {
      if (segment.isSelected()) {
        return true;
      }
    }
    return false;
  }

  // Collapses a multi-selection down to the first selected segment — used when switching out of
  // MULTI mode into SINGLE / REQUIRED.
  private void collapseToSingleSelection() {
    boolean kept = false;
    adjusting = true;
    try {
      for (final Segment segment : segments) {
        if (segment.isSelected()) {
          if (kept) {
            segment.setSelected(false);
          } else {
            kept = true;
          }
        }
      }
    } finally {
      adjusting = false;
    }
  }

  private void fireSelectionChanged() {
    final List<Integer> current = getSelectedIndices();
    if (current.equals(lastNotifiedSelection)) {
      return;
    }
    lastNotifiedSelection = current;
    for (final SelectionListener listener : new ArrayList<>(selectionListeners)) {
      listener.selectionChanged(this);
    }
  }

  // Re-applies the group-wide size, colour style, and shape / corner treatment to every segment.
  // Idempotent — the primitives' setters early-return when nothing changed — so it is cheap to
  // call after any configuration or selection change.
  private void refreshSegments() {
    for (final Segment segment : segments) {
      segment.applySize(buttonSize);
      if (variant == ButtonGroupVariant.CONNECTED) {
        segment.applyColorStyle(colorStyle);
      }
    }
    final int rowHeight = rowHeightPx();
    final int count = segments.size();
    for (int i = 0; i < count; i++) {
      final Segment segment = segments.get(i);
      if (variant == ButtonGroupVariant.CONNECTED) {
        segment.applyCornerRadii(connectedRadii(i, count, rowHeight, segment.isSelected()));
      } else {
        // Standard: the selected segment renders square, unselected segments round (M3 §10). The
        // transient press width/shape morph is the deferred animation epic, not rendered here.
        segment.applyCornerRadii(null);
        segment.applyShape(segment.isSelected() ? ButtonShape.SQUARE : ButtonShape.ROUND);
      }
    }
    revalidate();
    repaint();
  }

  // The per-corner radii for a connected segment. A SELECTED segment pops as a uniform pill,
  // whatever its position. An UNSELECTED segment takes the segmented-bar treatment: the group's
  // outer end-caps are always fully rounded (pill), butted inner corners stay small.
  private CornerRadii connectedRadii(
      final int index, final int count, final int rowHeight, final boolean selected) {
    final int inner = connectedCornerRadiusPx(buttonSize);
    final int pill = rowHeight / 2;
    if (selected) {
      return CornerRadii.uniform(pill);
    }
    if (count == 1) {
      return CornerRadii.uniform(pill);
    }
    if (index == 0) {
      return CornerRadii.of(pill, inner, inner, pill);
    }
    if (index == count - 1) {
      return CornerRadii.of(inner, pill, pill, inner);
    }
    return CornerRadii.uniform(inner);
  }

  private int rowHeightPx() {
    int height = 0;
    for (final Segment segment : segments) {
      height = Math.max(height, segment.preferredSize().height);
    }
    return height;
  }

  // The widest segment's content-hug preferred width, the floor every connected segment shares.
  private int connectedContentWidthPx() {
    int width = 0;
    for (final Segment segment : segments) {
      width = Math.max(width, segment.preferredSize().width);
    }
    return Math.max(width, connectedMinSegmentWidthPx(buttonSize));
  }

  private int connectedSegmentWidthPx(final int availableWidth) {
    final int count = segments.size();
    final int floor = connectedContentWidthPx();
    if (resizeMode == ResizeMode.FIXED) {
      return floor;
    }
    int available = availableWidth;
    if (maxWidthPx > 0) {
      available = Math.min(available, maxWidthPx);
    }
    final int share = (available - CONNECTED_GAP_PX * (count - 1)) / count;
    return Math.max(share, floor);
  }

  // --- M3 measurement tables (design doc §11) ---

  private static int standardGapPx(final ButtonSize size) {
    return switch (size) {
      case XS -> 18;
      case S -> 12;
      default -> 8;
    };
  }

  private static int connectedCornerRadiusPx(final ButtonSize size) {
    return switch (size) {
      case XS -> 4;
      case S -> 8;
      case M -> 8;
      case L -> 16;
      case XL -> 20;
    };
  }

  private static int connectedMinSegmentWidthPx(final ButtonSize size) {
    // §13 — XS / S connected segments carry a 48 dp minimum width as an accessibility floor.
    return size == ButtonSize.XS || size == ButtonSize.S ? 48 : 0;
  }

  // ----------------------------------------------------------- layout

  @Override
  public void doLayout() {
    final int count = segments.size();
    if (count == 0) {
      return;
    }
    final int rowHeight = rowHeightPx();
    int x = 0;
    if (variant == ButtonGroupVariant.STANDARD) {
      final int gap = standardGapPx(buttonSize);
      for (final Segment segment : segments) {
        final int width = segment.preferredSize().width;
        segment.component().setBounds(x, 0, width, rowHeight);
        x += width + gap;
      }
    } else {
      final int width = connectedSegmentWidthPx(getWidth());
      for (final Segment segment : segments) {
        segment.component().setBounds(x, 0, width, rowHeight);
        x += width + CONNECTED_GAP_PX;
      }
    }
  }

  @Override
  public Dimension getPreferredSize() {
    final int count = segments.size();
    if (count == 0) {
      return new Dimension(0, 0);
    }
    final int rowHeight = rowHeightPx();
    if (variant == ButtonGroupVariant.STANDARD) {
      int width = standardGapPx(buttonSize) * (count - 1);
      for (final Segment segment : segments) {
        width += segment.preferredSize().width;
      }
      return new Dimension(width, rowHeight);
    }
    final int segmentWidth = connectedContentWidthPx();
    return new Dimension(segmentWidth * count + CONNECTED_GAP_PX * (count - 1), rowHeight);
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public Dimension getMaximumSize() {
    if (variant == ButtonGroupVariant.CONNECTED && resizeMode == ResizeMode.FLEXIBLE) {
      final int width = maxWidthPx > 0 ? maxWidthPx : Integer.MAX_VALUE;
      return new Dimension(width, getPreferredSize().height);
    }
    return getPreferredSize();
  }

  /**
   * Enables or disables the group, cascading the state to every segment so a disabled group reads
   * as a disabled control end-to-end.
   *
   * @param enabled whether the group and its segments are enabled
   * @version v0.3.0
   * @since v0.3.0
   */
  @Override
  public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    for (final Segment segment : segments) {
      segment.component().setEnabled(enabled);
    }
  }

  // ----------------------------------------------------------- listener type

  /**
   * Notified once after every {@link ElwhaButtonGroup} selection change. Query the group for the
   * new selection via {@link #getSelectedIndices()} or {@link #getSelectedIndex()}.
   *
   * @author Charles Bryan
   * @version v0.3.0
   * @since v0.3.0
   */
  @FunctionalInterface
  public interface SelectionListener {

    /**
     * Called after the group's selection changes.
     *
     * @param group the group whose selection changed
     * @version v0.3.0
     * @since v0.3.0
     */
    void selectionChanged(ElwhaButtonGroup group);
  }

  // ----------------------------------------------------------- segment adapter

  // Normalises the two unrelated primitive types (ElwhaButton / ElwhaIconButton — both extend
  // JComponent directly, with no shared supertype) behind one interface the group drives, so the
  // group never branches on the concrete type and a group may freely mix label and icon segments.
  private abstract static class Segment {

    abstract JComponent component();

    abstract void applySize(ButtonSize size);

    abstract void applyColorStyle(ButtonGroupColorStyle style);

    abstract void applyShape(ButtonShape shape);

    abstract void applyCornerRadii(CornerRadii radii);

    abstract void makeSelectable();

    abstract boolean isSelected();

    abstract void setSelected(boolean selected);

    abstract void addSelectionListener(PropertyChangeListener listener);

    abstract void removeSelectionListener(PropertyChangeListener listener);

    abstract Dimension preferredSize();
  }

  private static final class ButtonSegment extends Segment {

    private final ElwhaButton button;

    ButtonSegment(final ElwhaButton button) {
      this.button = button;
    }

    @Override
    JComponent component() {
      return button;
    }

    @Override
    void applySize(final ButtonSize size) {
      button.setButtonSize(size);
    }

    @Override
    void applyColorStyle(final ButtonGroupColorStyle style) {
      button.setVariant(
          switch (style) {
            case FILLED -> ButtonVariant.FILLED;
            case TONAL -> ButtonVariant.FILLED_TONAL;
            case OUTLINED -> ButtonVariant.OUTLINED;
          });
    }

    @Override
    void applyShape(final ButtonShape shape) {
      button.setShape(shape);
    }

    @Override
    void applyCornerRadii(final CornerRadii radii) {
      button.setCornerRadii(radii);
    }

    @Override
    void makeSelectable() {
      button.setInteractionMode(ButtonInteractionMode.SELECTABLE);
    }

    @Override
    boolean isSelected() {
      return button.isSelected();
    }

    @Override
    void setSelected(final boolean selected) {
      button.setSelected(selected);
    }

    @Override
    void addSelectionListener(final PropertyChangeListener listener) {
      button.addSelectionChangeListener(listener);
    }

    @Override
    void removeSelectionListener(final PropertyChangeListener listener) {
      button.removePropertyChangeListener(ElwhaButton.PROPERTY_SELECTED, listener);
    }

    @Override
    Dimension preferredSize() {
      return button.getPreferredSize();
    }
  }

  private static final class IconButtonSegment extends Segment {

    private final ElwhaIconButton button;

    IconButtonSegment(final ElwhaIconButton button) {
      this.button = button;
    }

    @Override
    JComponent component() {
      return button;
    }

    @Override
    void applySize(final ButtonSize size) {
      button.setButtonSize(IconButtonSize.values()[size.ordinal()]);
    }

    @Override
    void applyColorStyle(final ButtonGroupColorStyle style) {
      button.setVariant(
          switch (style) {
            case FILLED -> IconButtonVariant.FILLED;
            case TONAL -> IconButtonVariant.FILLED_TONAL;
            case OUTLINED -> IconButtonVariant.OUTLINED;
          });
    }

    @Override
    void applyShape(final ButtonShape shape) {
      button.setShape(shape == ButtonShape.ROUND ? ShapeScale.FULL : ShapeScale.SM);
    }

    @Override
    void applyCornerRadii(final CornerRadii radii) {
      button.setCornerRadii(radii);
    }

    @Override
    void makeSelectable() {
      button.setInteractionMode(IconButtonInteractionMode.SELECTABLE);
    }

    @Override
    boolean isSelected() {
      return button.isSelected();
    }

    @Override
    void setSelected(final boolean selected) {
      button.setSelected(selected);
    }

    @Override
    void addSelectionListener(final PropertyChangeListener listener) {
      button.addSelectionChangeListener(listener);
    }

    @Override
    void removeSelectionListener(final PropertyChangeListener listener) {
      button.removePropertyChangeListener(ElwhaIconButton.PROPERTY_SELECTED, listener);
    }

    @Override
    Dimension preferredSize() {
      return button.getPreferredSize();
    }
  }
}
