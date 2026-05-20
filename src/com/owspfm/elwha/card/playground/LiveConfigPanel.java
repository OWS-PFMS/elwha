package com.owspfm.elwha.card.playground;

import com.owspfm.elwha.card.CardVariant;
import com.owspfm.elwha.card.CollapseRule;
import com.owspfm.elwha.card.ElwhaCard;
import com.owspfm.elwha.card.ElwhaCardChevron;
import com.owspfm.elwha.card.ElwhaCardHeader;
import com.owspfm.elwha.card.ElwhaCardSupportingText;
import com.owspfm.elwha.card.ExpansionOverflow;
import java.awt.BorderLayout;
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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Live-config side of the V3 playground: a focus card on the left, controls on the right that
 * mutate it in real time.
 *
 * <p>Listeners registered via {@link #addConfigChangeListener(Consumer)} fire after every mutation
 * with the latest snapshot — used to keep the {@link SnippetPanel} in sync.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class LiveConfigPanel extends JPanel {

  private final ElwhaCard focusCard;
  private final ElwhaCardHeader focusHeader;
  private final ElwhaCardChevron focusChevron;
  private final ElwhaCardSupportingText focusBody;

  private CardVariant variant = CardVariant.ELEVATED;
  private ExpansionOverflow overflow = ExpansionOverflow.GROW;
  private boolean actionable;
  private boolean selectable;
  private boolean collapsible;
  private boolean collapsed;
  private boolean disabled;

  private final List<Consumer<Snapshot>> listeners = new ArrayList<>();

  /** Builds the live-config view with a default Elevated static card. */
  public LiveConfigPanel() {
    super(new BorderLayout(16, 0));
    setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

    focusCard = ElwhaCard.elevatedCard();
    focusHeader = new ElwhaCardHeader().setTitle("Focus card").setSubtitle("Live-edited example");
    focusChevron = new ElwhaCardChevron(focusCard);
    focusBody =
        new ElwhaCardSupportingText(
            "Adjust the controls on the right; the card mutates in real time.");
    focusCard.add(focusHeader);
    focusCard.add(focusBody);

    final JPanel cardHolder = new JPanel(new BorderLayout());
    cardHolder.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    cardHolder.add(focusCard, BorderLayout.NORTH);
    cardHolder.setPreferredSize(new Dimension(380, 380));

    add(cardHolder, BorderLayout.CENTER);
    add(buildControls(), BorderLayout.EAST);

    applyAll();
  }

  private JComponent buildControls() {
    final JPanel panel = new JPanel(new GridBagLayout());
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1;
    gbc.insets = new Insets(4, 4, 4, 4);

    final JComboBox<CardVariant> variantBox = new JComboBox<>(CardVariant.values());
    variantBox.setSelectedItem(variant);
    variantBox.addActionListener(
        e -> {
          variant = (CardVariant) variantBox.getSelectedItem();
          applyAll();
        });
    addLabeledRow(panel, gbc, "Variant", variantBox);

    final JComboBox<ExpansionOverflow> overflowBox = new JComboBox<>(ExpansionOverflow.values());
    overflowBox.setSelectedItem(overflow);
    overflowBox.addActionListener(
        e -> {
          overflow = (ExpansionOverflow) overflowBox.getSelectedItem();
          applyAll();
        });
    addLabeledRow(panel, gbc, "Expansion overflow", overflowBox);

    addRow(
        panel,
        gbc,
        newCheck(
            "Actionable",
            actionable,
            v -> {
              actionable = v;
              applyAll();
            }));
    addRow(
        panel,
        gbc,
        newCheck(
            "Selectable",
            selectable,
            v -> {
              selectable = v;
              applyAll();
            }));
    addRow(
        panel,
        gbc,
        newCheck(
            "Collapsible",
            collapsible,
            v -> {
              collapsible = v;
              applyAll();
            }));
    addRow(
        panel,
        gbc,
        newCheck(
            "Collapsed",
            collapsed,
            v -> {
              collapsed = v;
              applyAll();
            }));
    addRow(
        panel,
        gbc,
        newCheck(
            "Disabled",
            disabled,
            v -> {
              disabled = v;
              applyAll();
            }));

    gbc.weighty = 1;
    panel.add(Box.createVerticalGlue(), gbc);
    panel.setPreferredSize(new Dimension(260, 1));

    final JScrollPane scroll = new JScrollPane(panel);
    scroll.setBorder(null);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.setPreferredSize(new Dimension(280, 1));
    return scroll;
  }

  private static JCheckBox newCheck(
      final String label, final boolean initial, final Consumer<Boolean> onChange) {
    final JCheckBox c = new JCheckBox(label, initial);
    c.addActionListener(e -> onChange.accept(c.isSelected()));
    return c;
  }

  private static void addLabeledRow(
      final JPanel p, final GridBagConstraints gbc, final String label, final JComponent comp) {
    final JLabel l = new JLabel(label);
    l.setAlignmentX(Component.LEFT_ALIGNMENT);
    addRow(p, gbc, l);
    addRow(p, gbc, comp);
  }

  private static void addRow(final JPanel p, final GridBagConstraints gbc, final JComponent comp) {
    p.add(comp, gbc);
    gbc.gridy++;
  }

  private void applyAll() {
    focusCard.setVariant(variant);
    focusCard.setExpansionOverflow(overflow);
    focusCard.setActionable(actionable);
    focusCard.setSelectable(selectable);
    focusCard.setCollapsible(collapsible);
    focusCard.setEnabled(!disabled);

    // Wire / unwire the chevron in the header trailing slot as collapsible toggles.
    if (collapsible) {
      if (focusHeader.getTrailingItems().isEmpty()) {
        focusHeader.addTrailing(focusChevron);
      }
      focusCard.setCollapseConstraint(focusHeader, CollapseRule.ALWAYS_VISIBLE);
    } else {
      focusHeader.clearTrailing();
    }
    focusCard.setCollapsed(collapsible && collapsed);

    focusCard.revalidate();
    focusCard.repaint();
    notifyListeners();
  }

  /**
   * Registers a listener notified after every config change with the latest snapshot.
   *
   * @param listener consumer invoked with the snapshot; {@code null} is ignored
   * @version v0.2.0
   * @since v0.2.0
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
   * @version v0.2.0
   * @since v0.2.0
   */
  public Snapshot snapshot() {
    return new Snapshot(
        variant, overflow, actionable, selectable, collapsible, collapsed, disabled);
  }

  private void notifyListeners() {
    final Snapshot s = snapshot();
    for (final Consumer<Snapshot> l : listeners) {
      l.accept(s);
    }
  }

  /**
   * Immutable snapshot of the focus card's configuration.
   *
   * @param variant active variant
   * @param overflow active expansion overflow strategy
   * @param actionable whether the card is actionable
   * @param selectable whether the card is selectable
   * @param collapsible whether the card supports collapse
   * @param collapsed current collapsed state
   * @param disabled whether the card is disabled
   * @version v0.2.0
   * @since v0.2.0
   */
  public record Snapshot(
      CardVariant variant,
      ExpansionOverflow overflow,
      boolean actionable,
      boolean selectable,
      boolean collapsible,
      boolean collapsed,
      boolean disabled) {}
}
