package com.owspfm.ui.components.pill;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

/**
 * Starter smoke-test for {@link FlatPill}: a 4-by-5 variant × state matrix.
 *
 * <p>Rows are the four {@link PillVariant} values; columns are the five visual states (default,
 * hover, pressed, focused, disabled). This is the minimal demo that ships with sub-story #235; the
 * full interactive playground (with orientations, drag, right-click, trailing-button, and the live
 * LAF-tweak panel) lives in {@code FlatPillPlayground} (sub-story #241).
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java \
 *     -Dexec.mainClass=com.owspfm.ui.components.pill.FlatPillDemo
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public final class FlatPillDemo {

  private FlatPillDemo() {
    // utility class — invoked via main()
  }

  /**
   * Launches the demo.
   *
   * @param args unused
   * @version v0.1.0
   * @since v0.1.0
   */
  public static void main(final String[] args) {
    SwingUtilities.invokeLater(FlatPillDemo::launch);
  }

  private static void launch() {
    FlatLightLaf.setup();

    final JFrame frame = new JFrame("FlatPill demo");
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JPanel matrix = new JPanel(new GridBagLayout());
    matrix.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(6, 6, 6, 6);
    gbc.anchor = GridBagConstraints.WEST;

    final String[] cols = {"default", "hover", "pressed", "focused", "disabled", "selected"};
    gbc.gridy = 0;
    gbc.gridx = 0;
    matrix.add(headerLabel("Variant"), gbc);
    for (int c = 0; c < cols.length; c++) {
      gbc.gridx = c + 1;
      matrix.add(headerLabel(cols[c]), gbc);
    }

    final PillVariant[] variants = PillVariant.values();
    for (int r = 0; r < variants.length; r++) {
      final PillVariant v = variants[r];
      gbc.gridy = r + 1;
      gbc.gridx = 0;
      matrix.add(headerLabel(v.name()), gbc);
      for (int c = 0; c < cols.length; c++) {
        gbc.gridx = c + 1;
        matrix.add(buildSample(v, cols[c]), gbc);
      }
    }

    frame.add(matrix, BorderLayout.CENTER);
    frame.add(buildFooter(), BorderLayout.SOUTH);

    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static JLabel headerLabel(final String theText) {
    final JLabel l = new JLabel(theText);
    l.putClientProperty("FlatLaf.styleClass", "small");
    l.setForeground(UIManager.getColor("Label.disabledForeground"));
    return l;
  }

  private static Component buildSample(final PillVariant theVariant, final String theState) {
    final FlatPill pill = new FlatPill(theVariant.name().toLowerCase().replace('_', ' '));
    pill.setVariant(theVariant);

    final boolean disabled = "disabled".equals(theState);
    final boolean selected = "selected".equals(theState);
    final boolean clickable = !disabled;

    pill.setInteractionMode(
        clickable ? PillInteractionMode.SELECTABLE : PillInteractionMode.STATIC);
    pill.setEnabled(!disabled);
    pill.setSelected(selected);

    // Static states (hover / pressed / focused) are simulated for the matrix view by overriding
    // the surface color directly — the matrix is a visual reference, not a live interaction.
    switch (theState) {
      case "hover" -> pill.setSurfaceColor(blendForState(theVariant, 0.18f));
      case "pressed" -> pill.setSurfaceColor(blendForState(theVariant, 0.28f));
      case "focused" -> {
        // We can't synthetically focus without owning the window — outline approximates it.
        pill.setBorderWidth(2);
      }
      default -> {
        // default / disabled / selected — leave as-is
      }
    }
    return pill;
  }

  private static Color blendForState(final PillVariant theVariant, final float theAmount) {
    Color base = UIManager.getColor("Panel.background");
    if (base == null) {
      base = new Color(245, 245, 245);
    }
    Color tint = UIManager.getColor("Label.foreground");
    if (tint == null) {
      tint = Color.DARK_GRAY;
    }
    if (theVariant == PillVariant.WARM_ACCENT) {
      base = new Color(248, 226, 165);
    }
    final int r = (int) (base.getRed() * (1 - theAmount) + tint.getRed() * theAmount);
    final int g = (int) (base.getGreen() * (1 - theAmount) + tint.getGreen() * theAmount);
    final int b = (int) (base.getBlue() * (1 - theAmount) + tint.getBlue() * theAmount);
    return new Color(r, g, b);
  }

  private static JPanel buildFooter() {
    final JPanel p = new JPanel(new BorderLayout());
    p.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));
    final FlatPill interactive =
        new FlatPill("click me")
            .setInteractionMode(PillInteractionMode.SELECTABLE)
            .setVariant(PillVariant.OUTLINED);
    interactive.setTrailingAction(
        new AbstractAction("×") {
          @Override
          public void actionPerformed(final java.awt.event.ActionEvent e) {
            System.out.println("trailing × clicked");
          }
        });
    interactive.addActionListener(
        e -> System.out.println("toggled -> " + interactive.isSelected()));
    p.add(new JLabel("Live interaction (selectable + trailing × button): "), BorderLayout.WEST);
    p.add(interactive, BorderLayout.CENTER);
    return p;
  }
}
