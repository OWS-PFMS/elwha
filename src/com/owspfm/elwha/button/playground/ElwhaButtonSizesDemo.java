package com.owspfm.elwha.button.playground;

import com.owspfm.elwha.button.ButtonShape;
import com.owspfm.elwha.button.ButtonSize;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * Story 4 (#117) scratch demo: visual smoketest for the {@link ButtonSize} axis — 5 size tiers × 2
 * shapes × FILLED + ELEVATED variants, with the 48 dp a11y target inflation visualized for XS / S.
 *
 * <p>Four sections stacked vertically:
 *
 * <ol>
 *   <li><strong>Size × shape matrix (FILLED)</strong> — 5 sizes × 2 shapes, with measurement
 *       captions underneath each cell showing container height / square corner / icon size / 48 dp
 *       inflation status. Visual dimensions match design doc Appendix A.
 *   <li><strong>Size × shape matrix (ELEVATED)</strong> — same grid for the ELEVATED variant so the
 *       shadow scaling across sizes is visible (and the smoketest from Story 3 follow-up #125 has a
 *       multi-size touch surface).
 *   <li><strong>With-icon row</strong> — one button per size, each with a leading icon sized to
 *       {@link ButtonSize#iconSizePx()}, verifying the icon-aware layout calculation across the
 *       five sizes.
 *   <li><strong>A11y target visualization</strong> — XS and S rendered inside a debug overlay panel
 *       (red dashed rectangle = layout's minimum target; solid = visible body) so the 32 / 40 dp
 *       body inside the 48 dp touch rect is visible. M and larger show the body and target
 *       coinciding (no inflation needed).
 * </ol>
 *
 * <p>Run: {@code mvn compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.button.playground.ElwhaButtonSizesDemo"}.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class ElwhaButtonSizesDemo {

  private static final ButtonSize[] ALL_SIZES = ButtonSize.values();

  private final JPanel root = new JPanel();
  private final JLabel statusLabel = new JLabel(" ");

  private ElwhaButtonSizesDemo() {}

  public static void main(final String[] args) {
    SwingUtilities.invokeLater(
        () -> {
          ElwhaTheme.install(
              ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
          new ElwhaButtonSizesDemo().show();
        });
  }

  private void show() {
    final JFrame frame = new JFrame("ElwhaButton — Story 4 Sizes Demo");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    frame.add(buildToolbar(), BorderLayout.NORTH);

    root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
    rebuild();
    final JScrollPane scroll = new JScrollPane(root);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    frame.add(scroll, BorderLayout.CENTER);

    statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
    statusLabel.setText(verifyMinimumSizeInflation());
    frame.add(statusLabel, BorderLayout.SOUTH);

    frame.setSize(1400, 1000);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  // -------------------------------------------------------------- toolbar

  private JPanel buildToolbar() {
    final JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
    bar.setBackground(ColorRole.SURFACE_CONTAINER_LOW.resolve());

    final JComboBox<Mode> modeBox = new JComboBox<>(new Mode[] {Mode.LIGHT, Mode.DARK});
    modeBox.setSelectedItem(Mode.LIGHT);
    modeBox.addActionListener(
        e -> {
          ElwhaTheme.install(ElwhaTheme.current().withMode((Mode) modeBox.getSelectedItem()));
          rebuild();
          SwingUtilities.invokeLater(() -> SwingUtilities.windowForComponent(bar).repaint());
        });

    bar.add(new JLabel("Mode:"));
    bar.add(modeBox);
    bar.add(Box.createHorizontalStrut(20));
    bar.add(new JLabel("All sizes × all shapes × FILLED / ELEVATED; with-icon row; a11y overlay"));
    return bar;
  }

  // ------------------------------------------------------------- sections

  private void rebuild() {
    root.removeAll();
    root.setBackground(ColorRole.SURFACE.resolve());
    root.add(buildSizeShapeMatrix("FILLED — size × shape", false));
    root.add(buildSizeShapeMatrix("ELEVATED — size × shape (shadow scales per size)", true));
    root.add(buildIconRow());
    root.add(buildA11yOverlay());
    root.revalidate();
    root.repaint();
  }

  private JPanel buildSizeShapeMatrix(final String title, final boolean elevated) {
    final JPanel section = section(title);
    final JPanel grid = new JPanel(new GridBagLayout());
    grid.setOpaque(false);
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(8, 12, 8, 12);
    gbc.anchor = GridBagConstraints.CENTER;

    gbc.gridy = 0;
    gbc.gridx = 0;
    grid.add(header(""), gbc);
    gbc.gridx = 1;
    grid.add(header("Round"), gbc);
    gbc.gridx = 2;
    grid.add(header("Square"), gbc);
    gbc.gridx = 3;
    grid.add(header("Spec"), gbc);

    int row = 1;
    for (ButtonSize size : ALL_SIZES) {
      gbc.gridy = row++;
      gbc.gridx = 0;
      grid.add(header(size.name()), gbc);

      gbc.gridx = 1;
      grid.add(makeBtn(size, ButtonShape.ROUND, elevated), gbc);
      gbc.gridx = 2;
      grid.add(makeBtn(size, ButtonShape.SQUARE, elevated), gbc);
      gbc.gridx = 3;
      grid.add(specLabel(size), gbc);
    }

    section.add(grid);
    return section;
  }

  private JPanel buildIconRow() {
    final JPanel section = section("With leading icon — one button per size");
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
    row.setOpaque(false);
    for (ButtonSize size : ALL_SIZES) {
      final ElwhaButton b =
          new ElwhaButton("Common button", MaterialIcons.delete(size.iconSizePx()))
              .setButtonSize(size);
      row.add(b);
    }
    section.add(row);
    return section;
  }

  private JPanel buildA11yOverlay() {
    final JPanel section =
        section(
            "48 dp touch target inflation — pink area = full click hit zone, body = visible chrome"
                + " (click anywhere in the pink to fire — counter updates live)");
    final JPanel grid = new JPanel(new GridBagLayout());
    grid.setOpaque(false);
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(8, 12, 8, 12);
    gbc.anchor = GridBagConstraints.CENTER;

    gbc.gridy = 0;
    gbc.gridx = 0;
    grid.add(header(""), gbc);
    gbc.gridx = 1;
    grid.add(header("Visible body"), gbc);
    gbc.gridx = 2;
    grid.add(header("Click zone (component bounds)"), gbc);
    gbc.gridx = 3;
    grid.add(header("Click counter"), gbc);
    gbc.gridx = 4;
    grid.add(header("Inflated?"), gbc);

    int row = 1;
    for (ButtonSize size : ALL_SIZES) {
      gbc.gridy = row++;
      gbc.gridx = 0;
      grid.add(header(size.name()), gbc);

      final ElwhaButton b = new ElwhaButton("Common button").setButtonSize(size);
      final JLabel counter = new JLabel("0");
      counter.setFont(TypeRole.LABEL_SMALL.resolve());
      counter.setForeground(ColorRole.ON_SURFACE.resolve());
      final int[] clicks = {0};
      b.addActionListener(
          e -> {
            clicks[0]++;
            counter.setText(String.valueOf(clicks[0]));
          });

      gbc.gridx = 1;
      grid.add(captionedPx(size.containerHeightPx()), gbc);
      gbc.gridx = 2;
      grid.add(new ClickZonePanel(b), gbc);
      gbc.gridx = 3;
      grid.add(counter, gbc);
      gbc.gridx = 4;
      grid.add(captionedInflation(size), gbc);
    }

    section.add(grid);
    return section;
  }

  // ------------------------------------------------------------- factories

  private ElwhaButton makeBtn(
      final ButtonSize size, final ButtonShape shape, final boolean elevated) {
    final ElwhaButton b = new ElwhaButton("Common button").setButtonSize(size).setShape(shape);
    if (elevated) {
      b.setVariant(com.owspfm.elwha.button.ButtonVariant.ELEVATED);
    }
    return b;
  }

  private JLabel specLabel(final ButtonSize size) {
    final String spec =
        String.format(
            Locale.ROOT,
            "<html>h=%d &nbsp;sq=%d<br>icon=%d &nbsp;min=%d</html>",
            size.containerHeightPx(),
            size.squareCornerPx(),
            size.iconSizePx(),
            size.minimumTargetPx());
    final JLabel l = new JLabel(spec);
    l.setFont(TypeRole.LABEL_SMALL.resolve());
    l.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
    return l;
  }

  private JLabel captionedPx(final int px) {
    final JLabel l = new JLabel(px + " px");
    l.setFont(TypeRole.LABEL_SMALL.resolve());
    l.setForeground(ColorRole.ON_SURFACE.resolve());
    return l;
  }

  private JLabel captionedInflation(final ButtonSize size) {
    final boolean inflated = size.minimumTargetPx() > size.containerHeightPx();
    final JLabel l =
        new JLabel(inflated ? "YES — body " + size.containerHeightPx() + " → target 48" : "no");
    l.setFont(TypeRole.LABEL_SMALL.resolve());
    l.setForeground((inflated ? ColorRole.PRIMARY : ColorRole.ON_SURFACE_VARIANT).resolve());
    return l;
  }

  // ------------------------------------------------------------- ui glue

  private JPanel section(final String title) {
    final JPanel s = new JPanel();
    s.setLayout(new BoxLayout(s, BoxLayout.Y_AXIS));
    s.setOpaque(false);
    s.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
    final JLabel l = new JLabel(title);
    l.setFont(TypeRole.TITLE_MEDIUM.resolve());
    l.setForeground(ColorRole.ON_SURFACE.resolve());
    l.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
    s.add(l);
    return s;
  }

  private JLabel header(final String text) {
    final JLabel l = new JLabel(text);
    l.setFont(TypeRole.LABEL_SMALL.resolve());
    l.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
    return l;
  }

  // ----------------------------------------------------------- a11y check

  private static String verifyMinimumSizeInflation() {
    final ElwhaButton xs = new ElwhaButton("Common button").setButtonSize(ButtonSize.XS);
    final ElwhaButton s = new ElwhaButton("Common button").setButtonSize(ButtonSize.S);
    final ElwhaButton m = new ElwhaButton("Common button").setButtonSize(ButtonSize.M);
    // After the click-area fix: preferred==min on cross axis, both ≥ 48 for XS/S.
    final boolean xsOk = xs.getPreferredSize().height == 48 && xs.getMinimumSize().height == 48;
    final boolean smOk = s.getPreferredSize().height == 48 && s.getMinimumSize().height == 48;
    final boolean mdOk = m.getPreferredSize().height == 56 && m.getMinimumSize().height == 56;
    if (xsOk && smOk && mdOk) {
      return "OK — XS/S inflate cross-axis to 48 (click area), visible body stays 32/40 centered."
          + " M reports 56 (no inflation needed).";
    }
    return String.format(
        Locale.ROOT,
        "FAILED — xsOk=%s (pref=%d min=%d) smOk=%s (pref=%d min=%d) mdOk=%s (pref=%d min=%d)",
        xsOk,
        xs.getPreferredSize().height,
        xs.getMinimumSize().height,
        smOk,
        s.getPreferredSize().height,
        s.getMinimumSize().height,
        mdOk,
        m.getPreferredSize().height,
        m.getMinimumSize().height);
  }

  /**
   * Wraps an {@link ElwhaButton} in a translucent pink background sized to the button's component
   * bounds (which now equal the inflated touch target on the cross axis). Visualizes the click hit
   * area: the pink region around the visible body is the a11y inflation padding, and clicks
   * anywhere in that region register on the button.
   */
  private static final class ClickZonePanel extends JPanel {

    ClickZonePanel(final ElwhaButton button) {
      setOpaque(true);
      setBackground(new Color(0xFF, 0xC0, 0xCB));
      setLayout(new GridBagLayout());
      final GridBagConstraints gbc = new GridBagConstraints();
      gbc.anchor = GridBagConstraints.CENTER;
      add(button, gbc);
    }
  }
}
