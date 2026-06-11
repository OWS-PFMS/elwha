package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.tabs.ElwhaTab;
import com.owspfm.elwha.tabs.ElwhaTabs;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * The Elwha <strong>color picker</strong> — an inline composite that lets people pick or define a
 * color through a closed set of {@link PickerMode modes} (design doc {@code
 * elwha-color-picker-design.md}). M3 defines no color picker; this component applies the picker
 * grammar M3's date and time pickers share — supporting text over a large headline that renders the
 * pending selection, a divider, the picking surface, and a tabbed mode switch — to color,
 * cross-checked against the JColorChooser / WinUI / macOS / Chrome picker lineage (research doc §X).
 *
 * <p><strong>Modes are closed.</strong> {@link #setModes} accepts any non-empty, duplicate-free
 * subset of {@link PickerMode} in any order — the first becomes the active tab, and a single mode
 * hides the tab bar entirely. There is no pluggable panel API: client code picks from the defined
 * options only.
 *
 * <p><strong>Selection model.</strong> {@link #getColor()} is never {@code null}. Every edit —
 * swatch click, spectrum drag, slider drag, hex entry, {@link #setColor} — funnels through one
 * commit path; {@link ChangeListener}s registered via {@link #addChangeListener} fire on every
 * accepted commit, and {@link #isAdjusting()} answers {@code true} while a drag is in flight
 * (the {@code BoundedRangeModel} convention {@code ElwhaSlider} also follows). Switching modes
 * never mutates the color.
 *
 * <p>For the modal form with pending-until-OK semantics, see {@code ElwhaColorPickerDialog}.
 *
 * <pre>{@code
 * ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0xFF7043));
 * picker.addChangeListener(e -> preview.setBackground(picker.getColor()));
 * }</pre>
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public class ElwhaColorPicker extends JComponent {

  /** Most-recently-used colors retained for the SWATCHES pane's recent row. */
  static final int RECENT_CAPACITY = 10;

  private static final int PREFERRED_WIDTH = 328;

  private final java.util.List<Color> recent = new java.util.ArrayList<>();

  private final ColorPickerHeader header;
  private final ElwhaTabs tabs;
  private final CardLayout paneCards;
  private final JPanel paneHost;
  private final Map<PickerMode, ColorPickerPane> panes = new EnumMap<>(PickerMode.class);

  private List<PickerMode> modes =
      List.of(PickerMode.SWATCHES, PickerMode.SPECTRUM, PickerMode.SLIDERS);
  private Color color;
  private boolean adjusting;
  private boolean committing;
  private boolean rebuilding;
  private boolean alphaEnabled;
  private String supportingText = "Select color";

  /**
   * Creates a picker with all three modes and an initial color of white (the JColorChooser
   * default).
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaColorPicker() {
    this(Color.WHITE);
  }

  /**
   * Creates a picker with all three modes, staged on the given color.
   *
   * @param initialColor the starting color
   * @throws IllegalArgumentException if {@code initialColor} is {@code null}
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaColorPicker(final Color initialColor) {
    if (initialColor == null) {
      throw new IllegalArgumentException("initialColor must not be null");
    }
    this.color = normalize(initialColor);
    this.header = new ColorPickerHeader(this);
    this.tabs = ElwhaTabs.primary();
    this.paneCards = new CardLayout();
    this.paneHost = new JPanel(paneCards);
    paneHost.setOpaque(false);
    setLayout(new BorderLayout());
    add(header, BorderLayout.NORTH);
    final JPanel body = new JPanel(new BorderLayout());
    body.setOpaque(false);
    body.add(tabs, BorderLayout.NORTH);
    body.add(paneHost, BorderLayout.CENTER);
    add(body, BorderLayout.CENTER);
    tabs.addChangeListener(
        e -> {
          if (!rebuilding) {
            showActivePane();
          }
        });
    rebuildModes();
  }

  /**
   * Returns the picker's current color. Never {@code null}.
   *
   * @return the current color
   * @version v0.5.0
   * @since v0.5.0
   */
  public Color getColor() {
    return color;
  }

  /**
   * Sets the current color programmatically — a non-adjusting commit: every pane resyncs and
   * {@code ChangeListener}s fire if the color actually changed.
   *
   * @param color the new color
   * @throws IllegalArgumentException if {@code color} is {@code null}
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setColor(final Color color) {
    if (color == null) {
      throw new IllegalArgumentException("color must not be null");
    }
    commitInternal(null, normalize(color), false);
  }

  /**
   * Answers whether the current color is mid-gesture — a drag on a spectrum thumb or slider handle.
   * Listeners wanting only settled values should skip events while this answers {@code true}.
   *
   * @return {@code true} while a drag is in flight
   * @version v0.5.0
   * @since v0.5.0
   */
  public boolean isAdjusting() {
    return adjusting;
  }

  /**
   * Registers a listener fired on every accepted color commit (and on the final commit that ends a
   * drag). Read {@link #getColor()} and {@link #isAdjusting()} from the event.
   *
   * @param listener the listener to add
   * @version v0.5.0
   * @since v0.5.0
   */
  public void addChangeListener(final ChangeListener listener) {
    listenerList.add(ChangeListener.class, listener);
  }

  /**
   * Removes a previously registered change listener.
   *
   * @param listener the listener to remove
   * @version v0.5.0
   * @since v0.5.0
   */
  public void removeChangeListener(final ChangeListener listener) {
    listenerList.remove(ChangeListener.class, listener);
  }

  /**
   * Restricts (or reorders) the picker's modes. The first mode becomes active; a single mode hides
   * the tab bar. The current color is preserved.
   *
   * @param modes the modes to offer, in tab order
   * @throws IllegalArgumentException if {@code modes} is null, empty, or contains nulls or
   *     duplicates
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setModes(final PickerMode... modes) {
    if (modes == null || modes.length == 0) {
      throw new IllegalArgumentException("at least one mode is required");
    }
    final List<PickerMode> next = Arrays.asList(modes);
    if (next.contains(null)) {
      throw new IllegalArgumentException("modes must not contain null");
    }
    if (next.stream().distinct().count() != next.size()) {
      throw new IllegalArgumentException("modes must not repeat");
    }
    this.modes = List.copyOf(next);
    rebuildModes();
  }

  /**
   * Returns the offered modes in tab order.
   *
   * @return an immutable mode list
   * @version v0.5.0
   * @since v0.5.0
   */
  public List<PickerMode> getModes() {
    return modes;
  }

  /**
   * Returns the active mode.
   *
   * @return the mode whose pane is showing
   * @version v0.5.0
   * @since v0.5.0
   */
  public PickerMode getMode() {
    return modes.get(Math.max(0, tabs.getActiveTabIndex()));
  }

  /**
   * Activates a mode's tab and pane. Never mutates the color.
   *
   * @param mode the mode to activate
   * @throws IllegalArgumentException if {@code mode} is not among {@link #getModes()}
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setMode(final PickerMode mode) {
    final int index = modes.indexOf(mode);
    if (index < 0) {
      throw new IllegalArgumentException("mode " + mode + " is not offered — see setModes");
    }
    tabs.setActiveTabIndex(index);
  }

  /**
   * Sets the supporting text naming the task (the M3 header's first line). {@code null} hides the
   * line — the form `ElwhaColorPickerDialog` uses, since the dialog headline already names the
   * task.
   *
   * @param supportingText the text, or {@code null} to hide
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setSupportingText(final String supportingText) {
    this.supportingText = supportingText;
    header.revalidate();
    header.repaint();
  }

  /**
   * Returns the supporting text, or {@code null} when hidden.
   *
   * @return the supporting text
   * @version v0.5.0
   * @since v0.5.0
   */
  public String getSupportingText() {
    return supportingText;
  }

  /**
   * Opts the picker into the alpha channel (WinUI's {@code IsAlphaEnabled} precedent, design §9):
   * an alpha track joins the SPECTRUM and SLIDERS panes, the hex grammar grows to
   * {@code #RRGGBBAA}, and the preview backs translucency with the transparency checkerboard.
   * Turning alpha off strips the current color to opaque (a change commit if it carried alpha).
   * Mode panes are rebuilt; the recent row survives.
   *
   * @param alphaEnabled whether colors carry alpha
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setAlphaEnabled(final boolean alphaEnabled) {
    if (this.alphaEnabled == alphaEnabled) {
      return;
    }
    this.alphaEnabled = alphaEnabled;
    if (!alphaEnabled && color.getAlpha() != 255) {
      commitInternal(
          null, new Color(color.getRed(), color.getGreen(), color.getBlue()), false);
    }
    rebuildModes();
    header.revalidate();
    header.repaint();
  }

  /**
   * Answers whether the alpha channel is enabled.
   *
   * @return {@code true} when colors carry alpha
   * @version v0.5.0
   * @since v0.5.0
   */
  public boolean isAlphaEnabled() {
    return alphaEnabled;
  }

  /**
   * Enables or disables the whole picker — tabs, panes, and header rendering follow.
   *
   * @param enabled the new enabled state
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    tabs.setEnabled(enabled);
    for (final ColorPickerPane pane : panes.values()) {
      pane.setEnabled(enabled);
    }
    repaint();
  }

  @Override
  public Dimension getPreferredSize() {
    final Dimension d = super.getPreferredSize();
    d.width = Math.max(d.width, PREFERRED_WIDTH);
    return d;
  }

  void commitFromPane(final ColorPickerPane source, final Color next, final boolean adjusting) {
    commitInternal(source, normalize(next), adjusting);
  }

  ColorPickerPane paneFor(final PickerMode mode) {
    return panes.get(mode);
  }

  java.util.List<Color> recentColors() {
    return java.util.Collections.unmodifiableList(recent);
  }

  TypeRole headlineTypeRole() {
    return alphaEnabled ? TypeRole.HEADLINE_MEDIUM : TypeRole.HEADLINE_LARGE;
  }

  String formatCurrentHex() {
    return ColorHex.format(color, alphaEnabled);
  }

  private Color normalize(final Color candidate) {
    return alphaEnabled || candidate.getAlpha() == 255
        ? candidate
        : new Color(candidate.getRed(), candidate.getGreen(), candidate.getBlue());
  }

  private void commitInternal(
      final ColorPickerPane source, final Color next, final boolean nowAdjusting) {
    if (committing) {
      return;
    }
    if (next.equals(color) && nowAdjusting == adjusting) {
      return;
    }
    committing = true;
    try {
      this.color = next;
      this.adjusting = nowAdjusting;
      if (!nowAdjusting) {
        recent.removeIf(entry -> entry.equals(next));
        recent.add(0, next);
        while (recent.size() > RECENT_CAPACITY) {
          recent.remove(recent.size() - 1);
        }
      }
      for (final ColorPickerPane pane : panes.values()) {
        if (pane != source) {
          pane.syncFromPicker(next);
        }
      }
      header.repaint();
      fireColorChanged();
    } finally {
      committing = false;
    }
  }

  private void fireColorChanged() {
    final ChangeEvent event = new ChangeEvent(this);
    for (final ChangeListener listener : listenerList.getListeners(ChangeListener.class)) {
      listener.stateChanged(event);
    }
  }

  private void rebuildModes() {
    rebuilding = true;
    try {
      while (tabs.getTabCount() > 0) {
        tabs.removeTab(tabs.getTabAt(0));
      }
      paneHost.removeAll();
      panes.clear();
      for (final PickerMode mode : modes) {
        final ElwhaTab tab = ElwhaTab.of(MaterialIcons.symbol(mode.iconName()), mode.label());
        tab.setInlineIcon(true);
        tabs.addTab(tab);
        final ColorPickerPane pane = createPane(mode);
        pane.setEnabled(isEnabled());
        pane.syncFromPicker(color);
        panes.put(mode, pane);
        paneHost.add(pane, mode.name());
      }
      tabs.setVisible(modes.size() > 1);
      tabs.setActiveTabIndex(0);
    } finally {
      rebuilding = false;
    }
    showActivePane();
    revalidate();
    repaint();
  }

  private void showActivePane() {
    paneCards.show(paneHost, getMode().name());
  }

  /**
   * Returns the picker's accessible context — role {@code COLOR_CHOOSER}, named by the supporting
   * text, describing the current color's hex (design doc §10).
   *
   * @return the accessible context
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public javax.accessibility.AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new AccessibleElwhaColorPicker();
    }
    return accessibleContext;
  }

  /** Accessible support for the picker composite. */
  protected class AccessibleElwhaColorPicker extends AccessibleJComponent {

    /**
     * Creates the accessible context.
     *
     * @version v0.5.0
     * @since v0.5.0
     */
    protected AccessibleElwhaColorPicker() {}

    @Override
    public javax.accessibility.AccessibleRole getAccessibleRole() {
      return javax.accessibility.AccessibleRole.COLOR_CHOOSER;
    }

    @Override
    public String getAccessibleName() {
      final String explicit = super.getAccessibleName();
      if (explicit != null) {
        return explicit;
      }
      return supportingText != null ? supportingText : "Select color";
    }

    @Override
    public String getAccessibleDescription() {
      final String explicit = super.getAccessibleDescription();
      return explicit != null ? explicit : "Current color " + formatCurrentHex();
    }
  }

  private ColorPickerPane createPane(final PickerMode mode) {
    return switch (mode) {
      case SWATCHES -> new SwatchesPane(this);
      case SPECTRUM -> new SpectrumPane(this);
      case SLIDERS -> new SlidersPane(this);
    };
  }
}
