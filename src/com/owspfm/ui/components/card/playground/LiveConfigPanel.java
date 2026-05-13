package com.owspfm.ui.components.card.playground;

import com.owspfm.ui.components.card.CardInteractionMode;
import com.owspfm.ui.components.card.CardVariant;
import com.owspfm.ui.components.card.FlatCard;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;

/**
 * Live-config side of the playground: a focus card on the left and a stack of widgets on the right
 * that mutate the card's properties in real time.
 *
 * <p>Listeners registered via {@link #addConfigChangeListener(Consumer)} fire after every mutation
 * with the latest snapshot — used to keep the snippet panel in sync.
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public final class LiveConfigPanel extends JPanel {

  private final FlatCard focusCard;

  // Mutable config state ---------------------------------------------------
  private CardVariant variant = CardVariant.ELEVATED;
  private CardInteractionMode mode = CardInteractionMode.STATIC;
  private int elevation = 1;
  private int cornerRadius = 12;
  private int padding = 16;
  private int borderWidth = 1;
  private boolean collapsible;
  private boolean collapsed;
  private boolean disabled;
  private boolean showHeader = true;
  private boolean showMedia;
  private boolean showFooter = true;

  private final List<Consumer<Snapshot>> listeners = new ArrayList<>();
  private boolean updating;

  // Cached slot components (created once, swapped in/out by show* toggles only).
  private MediaPlate cachedMedia;
  private JComponent cachedFooter;
  // Track previously-applied slot visibility so we only rebuild the slot when it flips.
  private Boolean appliedShowHeader;
  private Boolean appliedShowMedia;
  private Boolean appliedShowFooter;

  /** Builds the live-config view with a default ELEVATED static card. */
  public LiveConfigPanel() {
    super(new BorderLayout(16, 0));
    setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

    focusCard = buildFocusCard();
    JPanel cardHolder = new JPanel(new BorderLayout());
    cardHolder.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    cardHolder.add(focusCard, BorderLayout.NORTH);
    cardHolder.setPreferredSize(new Dimension(360, 360));

    add(cardHolder, BorderLayout.CENTER);
    add(buildControls(), BorderLayout.EAST);

    applyAll();
  }

  // ----------------------------------------------------------------- focus

  private static JComponent buildFooter() {
    JPanel row = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
    row.setOpaque(false);
    row.add(new JButton("Primary"));
    row.add(new JButton("Cancel"));
    return row;
  }

  private FlatCard buildFocusCard() {
    JPanel body = new JPanel();
    body.setOpaque(false);
    body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
    body.add(new JLabel("Adjust the controls on the right."));
    body.add(new JLabel("This card mutates in real time."));

    return new FlatCard()
        .setHeader("Focus card", "Live-edited example")
        .setBody(body)
        .setFooter(new JButton("Primary"), new JButton("Cancel"))
        .setCollapsedSummary(new JLabel("Collapsed — click chevron to expand"));
  }

  // -------------------------------------------------------------- controls

  private JComponent buildControls() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1;
    gbc.insets = new Insets(4, 4, 4, 4);

    JComboBox<CardVariant> variantBox = new JComboBox<>(CardVariant.values());
    variantBox.setSelectedItem(variant);
    variantBox.addActionListener(
        e -> {
          variant = (CardVariant) variantBox.getSelectedItem();
          applyAll();
        });
    addLabeledRow(panel, gbc, "Variant", variantBox);

    JComboBox<CardInteractionMode> modeBox = new JComboBox<>(CardInteractionMode.values());
    modeBox.setSelectedItem(mode);
    modeBox.addActionListener(
        e -> {
          mode = (CardInteractionMode) modeBox.getSelectedItem();
          applyAll();
        });
    addLabeledRow(panel, gbc, "Interaction mode", modeBox);

    JSlider elevationSlider = newSlider(0, FlatCard.MAX_ELEVATION, elevation);
    elevationSlider.addChangeListener(
        e -> {
          elevation = elevationSlider.getValue();
          applyAll();
        });
    addLabeledRow(panel, gbc, "Elevation", elevationSlider);

    JSlider radiusSlider = newSlider(0, 36, cornerRadius);
    radiusSlider.addChangeListener(
        e -> {
          cornerRadius = radiusSlider.getValue();
          applyAll();
        });
    addLabeledRow(panel, gbc, "Corner radius", radiusSlider);

    JSlider paddingSlider = newSlider(0, 48, padding);
    paddingSlider.addChangeListener(
        e -> {
          padding = paddingSlider.getValue();
          applyAll();
        });
    addLabeledRow(panel, gbc, "Padding", paddingSlider);

    JSlider borderWidthSlider = newSlider(0, 6, borderWidth);
    borderWidthSlider.addChangeListener(
        e -> {
          borderWidth = borderWidthSlider.getValue();
          applyAll();
        });
    addLabeledRow(panel, gbc, "Border width", borderWidthSlider);

    JCheckBox collapsibleBox =
        newCheck(
            "Collapsible",
            collapsible,
            v -> {
              collapsible = v;
              applyAll();
            });
    JCheckBox collapsedBox =
        newCheck(
            "Collapsed",
            collapsed,
            v -> {
              collapsed = v;
              applyAll();
            });
    JCheckBox disabledBox =
        newCheck(
            "Disabled",
            disabled,
            v -> {
              disabled = v;
              applyAll();
            });
    addRow(panel, gbc, collapsibleBox);
    addRow(panel, gbc, collapsedBox);
    addRow(panel, gbc, disabledBox);

    JCheckBox showHeaderBox =
        newCheck(
            "Header",
            showHeader,
            v -> {
              showHeader = v;
              applyAll();
            });
    JCheckBox showMediaBox =
        newCheck(
            "Media",
            showMedia,
            v -> {
              showMedia = v;
              applyAll();
            });
    JCheckBox showFooterBox =
        newCheck(
            "Footer",
            showFooter,
            v -> {
              showFooter = v;
              applyAll();
            });
    addRow(panel, gbc, showHeaderBox);
    addRow(panel, gbc, showMediaBox);
    addRow(panel, gbc, showFooterBox);

    gbc.weighty = 1;
    panel.add(Box.createVerticalGlue(), gbc);

    panel.setPreferredSize(new Dimension(260, 1));

    JScrollPane scroll = new JScrollPane(panel);
    scroll.setBorder(null);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.setPreferredSize(new Dimension(280, 1));
    return scroll;
  }

  private static JSlider newSlider(final int min, final int max, final int value) {
    JSlider s = new JSlider(min, max, value);
    s.setMajorTickSpacing(Math.max(1, (max - min) / 4));
    s.setPaintTicks(true);
    s.setPaintLabels(true);
    return s;
  }

  private static JCheckBox newCheck(
      final String label, final boolean initial, final Consumer<Boolean> onChange) {
    JCheckBox c = new JCheckBox(label, initial);
    c.addActionListener(e -> onChange.accept(c.isSelected()));
    return c;
  }

  private static void addLabeledRow(
      final JPanel panel, final GridBagConstraints gbc, final String label, final JComponent comp) {
    JLabel lbl = new JLabel(label);
    lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
    addRow(panel, gbc, lbl);
    addRow(panel, gbc, comp);
  }

  private static void addRow(
      final JPanel panel, final GridBagConstraints gbc, final JComponent comp) {
    panel.add(comp, gbc);
    gbc.gridy++;
  }

  // ---------------------------------------------------------------- apply

  private void applyAll() {
    if (updating) {
      return;
    }
    updating = true;
    try {
      focusCard.setVariant(variant);
      focusCard.setInteractionMode(mode);
      focusCard.setElevation(elevation);
      focusCard.setCornerRadius(cornerRadius);
      focusCard.setPadding(padding);
      focusCard.setBorderWidth(borderWidth);
      focusCard.setCollapsible(collapsible);
      focusCard.setCollapsed(collapsed);
      focusCard.setEnabled(!disabled);

      if (appliedShowHeader == null || appliedShowHeader != showHeader) {
        if (showHeader) {
          focusCard.setHeader("Focus card", "Live-edited example");
        } else {
          focusCard.setHeader(null, null);
        }
        appliedShowHeader = showHeader;
      }
      if (appliedShowMedia == null || appliedShowMedia != showMedia) {
        if (showMedia) {
          if (cachedMedia == null) {
            cachedMedia = new MediaPlate();
          }
          focusCard.setMedia(cachedMedia);
        } else {
          focusCard.setMedia(null);
        }
        appliedShowMedia = showMedia;
      }
      if (appliedShowFooter == null || appliedShowFooter != showFooter) {
        if (showFooter) {
          if (cachedFooter == null) {
            cachedFooter = buildFooter();
          }
          focusCard.setFooter(cachedFooter);
        } else {
          focusCard.setFooter((JComponent) null);
        }
        appliedShowFooter = showFooter;
      }
    } finally {
      updating = false;
    }

    focusCard.revalidate();
    focusCard.repaint();
    notifyListeners();
  }

  // -------------------------------------------------------------- listeners

  /**
   * Registers a listener that is notified after every config change with the latest snapshot.
   *
   * @param listener consumer invoked with the snapshot; null is ignored
   * @version v0.1.0
   * @since v0.1.0
   */
  public void addConfigChangeListener(final Consumer<Snapshot> listener) {
    if (listener != null) {
      listeners.add(listener);
    }
  }

  /**
   * Returns an immutable snapshot of the current configuration suitable for snippet rendering.
   *
   * @return a defensive snapshot
   * @version v0.1.0
   * @since v0.1.0
   */
  public Snapshot snapshot() {
    return new Snapshot(
        variant,
        mode,
        elevation,
        cornerRadius,
        padding,
        borderWidth,
        collapsible,
        collapsed,
        disabled,
        showHeader,
        showMedia,
        showFooter);
  }

  private void notifyListeners() {
    Snapshot s = snapshot();
    for (Consumer<Snapshot> l : listeners) {
      l.accept(s);
    }
  }

  // ------------------------------------------------------------- helpers

  /** Decorative gradient plate used as media stand-in inside the live focus card. */
  private static final class MediaPlate extends JComponent {
    MediaPlate() {
      setPreferredSize(new Dimension(120, 60));
    }

    @Override
    protected void paintComponent(final java.awt.Graphics g) {
      java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
      g2.setRenderingHint(
          java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setPaint(
          new java.awt.GradientPaint(
              0, 0, new Color(120, 144, 200), getWidth(), getHeight(), new Color(200, 144, 192)));
      g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
      g2.dispose();
    }
  }

  /**
   * Immutable snapshot of the focus card's configuration. Safe to pass to snippet rendering or any
   * other listener.
   *
   * @param variant active variant
   * @param mode active interaction mode
   * @param elevation current elevation level
   * @param cornerRadius current corner radius
   * @param padding uniform content padding
   * @param borderWidth current border width
   * @param collapsible whether the card supports collapse/expand
   * @param collapsed current collapsed state
   * @param disabled whether the card is disabled
   * @param showHeader whether the header slot is populated
   * @param showMedia whether the media slot is populated
   * @param showFooter whether the footer slot is populated
   * @version v0.1.0
   * @since v0.1.0
   */
  public record Snapshot(
      CardVariant variant,
      CardInteractionMode mode,
      int elevation,
      int cornerRadius,
      int padding,
      int borderWidth,
      boolean collapsible,
      boolean collapsed,
      boolean disabled,
      boolean showHeader,
      boolean showMedia,
      boolean showFooter) {}
}
