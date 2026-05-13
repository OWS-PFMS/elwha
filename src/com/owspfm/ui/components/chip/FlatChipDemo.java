package com.owspfm.ui.components.chip;

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
 * Starter smoke-test for {@link FlatChip}: a 4-by-5 variant × state matrix.
 *
 * <p>Rows are the four {@link ChipVariant} values; columns are the five visual states (default,
 * hover, pressed, focused, disabled). This is the minimal demo that ships with sub-story #235; the
 * full interactive playground (with orientations, drag, right-click, trailing-button, and the live
 * LAF-tweak panel) lives in {@code FlatChipPlayground} (sub-story #241).
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java \
 *     -Dexec.mainClass=com.owspfm.ui.components.chip.FlatChipDemo
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public final class FlatChipDemo {

  private FlatChipDemo() {
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
    SwingUtilities.invokeLater(FlatChipDemo::launch);
  }

  private static void launch() {
    FlatLightLaf.setup();

    final JFrame frame = new JFrame("FlatChip demo");
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

    final ChipVariant[] variants = ChipVariant.values();
    for (int r = 0; r < variants.length; r++) {
      final ChipVariant v = variants[r];
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

  private static JLabel headerLabel(final String text) {
    final JLabel l = new JLabel(text);
    l.putClientProperty("FlatLaf.styleClass", "small");
    l.setForeground(UIManager.getColor("Label.disabledForeground"));
    return l;
  }

  private static Component buildSample(final ChipVariant variant, final String state) {
    final FlatChip chip = new FlatChip(variant.name().toLowerCase().replace('_', ' '));
    chip.setVariant(variant);

    final boolean disabled = "disabled".equals(state);
    final boolean selected = "selected".equals(state);
    final boolean clickable = !disabled;

    chip.setInteractionMode(
        clickable ? ChipInteractionMode.SELECTABLE : ChipInteractionMode.STATIC);
    chip.setEnabled(!disabled);
    chip.setSelected(selected);

    // Static states (hover / pressed / focused) are simulated for the matrix view by overriding
    // the surface color directly — the matrix is a visual reference, not a live interaction.
    switch (state) {
      case "hover" -> chip.setSurfaceColor(blendForState(variant, 0.18f));
      case "pressed" -> chip.setSurfaceColor(blendForState(variant, 0.28f));
      case "focused" -> {
        // We can't synthetically focus without owning the window — outline approximates it.
        chip.setBorderWidth(2);
      }
      default -> {
        // default / disabled / selected — leave as-is
      }
    }
    return chip;
  }

  private static Color blendForState(final ChipVariant variant, final float amount) {
    Color base = UIManager.getColor("Panel.background");
    if (base == null) {
      base = new Color(245, 245, 245);
    }
    Color tint = UIManager.getColor("Label.foreground");
    if (tint == null) {
      tint = Color.DARK_GRAY;
    }
    if (variant == ChipVariant.WARM_ACCENT) {
      base = new Color(248, 226, 165);
    }
    final int r = (int) (base.getRed() * (1 - amount) + tint.getRed() * amount);
    final int g = (int) (base.getGreen() * (1 - amount) + tint.getGreen() * amount);
    final int b = (int) (base.getBlue() * (1 - amount) + tint.getBlue() * amount);
    return new Color(r, g, b);
  }

  private static JPanel buildFooter() {
    final JPanel p = new JPanel(new BorderLayout());
    p.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));
    final FlatChip interactive =
        new FlatChip("click me")
            .setInteractionMode(ChipInteractionMode.SELECTABLE)
            .setVariant(ChipVariant.OUTLINED);
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
