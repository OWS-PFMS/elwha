package com.owspfm.ui.components.theme.playground;

import com.owspfm.ui.components.chip.ChipVariant;
import com.owspfm.ui.components.chip.FlatChip;
import com.owspfm.ui.components.theme.ColorRole;
import com.owspfm.ui.components.theme.Config;
import com.owspfm.ui.components.theme.FlatCompTheme;
import com.owspfm.ui.components.theme.MaterialPalettes;
import com.owspfm.ui.components.theme.Mode;
import com.owspfm.ui.components.theme.ShapeScale;
import com.owspfm.ui.components.theme.SpaceScale;
import com.owspfm.ui.components.theme.TypeRole;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

/**
 * Interactive visual harness for the FlatComp token foundation.
 *
 * <p>Three tabs — color-role swatches, the type scale, and a components gallery — let a maintainer
 * eyeball that the {@code FlatComp.*} tokens and the FlatLaf-native key mapping land correctly, and
 * that raw Swing widgets ({@code JButton}, {@code JTextField}, …) read as coherent next to a {@link
 * FlatChip}. The mode toggle re-installs the theme at runtime, exercising the binding rule
 * end-to-end. This is the visual validation surface for Epic #30 sub-story #34.
 *
 * <p>Run with: {@code mvn compile exec:java
 * -Dexec.mainClass="com.owspfm.ui.components.theme.playground.ThemePlayground"}
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public final class ThemePlayground {

  private final List<Runnable> tokenRefreshers = new ArrayList<>();
  private JLabel statusLabel;

  private ThemePlayground() {}

  /**
   * Launches the playground.
   *
   * @param args ignored
   * @version v0.1.0
   * @since v0.1.0
   */
  public static void main(String[] args) {
    FlatCompTheme.install(
        FlatCompTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.SYSTEM).build());
    SwingUtilities.invokeLater(() -> new ThemePlayground().buildAndShow());
  }

  private void buildAndShow() {
    JFrame frame = new JFrame("FlatComp Theme Playground");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    JPanel root = new JPanel(new java.awt.BorderLayout());
    root.add(buildControlBar(), java.awt.BorderLayout.NORTH);

    JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Color Roles", new JScrollPane(buildColorTab()));
    tabs.addTab("Type Scale", new JScrollPane(buildTypeTab()));
    tabs.addTab("Components", new JScrollPane(buildComponentsTab()));
    root.add(tabs, java.awt.BorderLayout.CENTER);

    frame.setContentPane(root);
    frame.setSize(960, 720);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private JComponent buildControlBar() {
    JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    bar.add(new JLabel("Mode:"));

    ButtonGroup group = new ButtonGroup();
    for (Mode mode : new Mode[] {Mode.LIGHT, Mode.DARK, Mode.SYSTEM}) {
      JToggleButton button = new JToggleButton(mode.name());
      button.addActionListener(event -> switchMode(mode));
      if (FlatCompTheme.current().mode() == mode) {
        button.setSelected(true);
      }
      group.add(button);
      bar.add(button);
    }

    statusLabel = new JLabel();
    updateStatus();
    bar.add(Box.createHorizontalStrut(16));
    bar.add(statusLabel);
    return bar;
  }

  private void switchMode(Mode mode) {
    Config next = FlatCompTheme.current().withMode(mode);
    FlatCompTheme.install(next);
    // install() dispatches the component-tree repaint; refresh explicitly-set token state too.
    SwingUtilities.invokeLater(
        () -> {
          tokenRefreshers.forEach(Runnable::run);
          updateStatus();
        });
  }

  private void updateStatus() {
    Mode requested = FlatCompTheme.current().mode();
    Mode resolved = requested.resolved();
    statusLabel.setText(
        "Theme: "
            + FlatCompTheme.current().theme().name()
            + "   ·   requested "
            + requested
            + " → resolved "
            + resolved);
  }

  // --- Color roles tab ---

  private JComponent buildColorTab() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    panel.add(
        sectionLabel("All 49 color roles — fill is the role, text is its on-pair (or onSurface)"));
    panel.add(Box.createVerticalStrut(8));

    JPanel grid = new JPanel(new GridLayout(0, 4, 8, 8));
    for (ColorRole role : ColorRole.values()) {
      grid.add(new Swatch(role));
    }
    grid.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    panel.add(grid);
    return panel;
  }

  // --- Type scale tab ---

  private JComponent buildTypeTab() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    panel.add(sectionLabel("The 12 type roles — bundled Inter (Regular / Medium)"));
    panel.add(Box.createVerticalStrut(12));

    for (TypeRole role : TypeRole.values()) {
      JLabel sample = new JLabel(role.name() + "  —  The quick brown fox (" + role.pt() + "pt)");
      sample.setAlignmentX(JComponent.LEFT_ALIGNMENT);
      Runnable refresh =
          () -> {
            sample.setFont(role.resolve());
            sample.setForeground(ColorRole.ON_SURFACE.resolve());
          };
      refresh.run();
      tokenRefreshers.add(refresh);
      panel.add(sample);
      panel.add(Box.createVerticalStrut(8));
    }
    return panel;
  }

  // --- Components tab ---

  private JComponent buildComponentsTab() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

    panel.add(sectionLabel("Raw Swing — inherits the theme via the FlatLaf-native key mapping"));
    panel.add(Box.createVerticalStrut(8));
    panel.add(buildRawSwingRow());
    panel.add(Box.createVerticalStrut(24));

    panel.add(
        sectionLabel("FlatChip (V1) — should read as coherent next to the raw widgets above"));
    panel.add(Box.createVerticalStrut(8));
    panel.add(buildChipRow());
    panel.add(Box.createVerticalStrut(24));

    panel.add(sectionLabel("Shape & spacing scales"));
    panel.add(Box.createVerticalStrut(8));
    panel.add(buildShapeSpacingRow());
    return panel;
  }

  private JComponent buildRawSwingRow() {
    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
    row.setAlignmentX(JComponent.LEFT_ALIGNMENT);

    JButton normal = new JButton("Button");
    JButton primary = new JButton("Default");
    primary.putClientProperty("JButton.buttonType", "default");
    JToggleButton toggle = new JToggleButton("Toggle");
    toggle.setSelected(true);
    JTextField field = new JTextField("Text field", 12);
    JCheckBox check = new JCheckBox("Checkbox", true);
    JComboBox<String> combo = new JComboBox<>(new String[] {"One", "Two", "Three"});
    JSlider slider = new JSlider(0, 100, 60);
    JProgressBar progress = new JProgressBar(0, 100);
    progress.setValue(45);

    DefaultListModel<String> listModel = new DefaultListModel<>();
    listModel.addElement("List item A");
    listModel.addElement("List item B");
    listModel.addElement("List item C");
    JList<String> list = new JList<>(listModel);
    list.setSelectedIndex(1);
    JScrollPane listScroll = new JScrollPane(list);
    listScroll.setPreferredSize(new Dimension(140, 80));

    row.add(normal);
    row.add(primary);
    row.add(toggle);
    row.add(field);
    row.add(check);
    row.add(combo);
    row.add(slider);
    row.add(progress);
    row.add(listScroll);
    return row;
  }

  private JComponent buildChipRow() {
    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
    row.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    row.add(new FlatChip("Filled").setVariant(ChipVariant.FILLED));
    row.add(new FlatChip("Outlined").setVariant(ChipVariant.OUTLINED));
    row.add(new FlatChip("Ghost").setVariant(ChipVariant.GHOST));
    row.add(new FlatChip("Warm accent").setVariant(ChipVariant.WARM_ACCENT));
    row.add(new FlatChip("Selected").setVariant(ChipVariant.FILLED).setSelected(true));
    return row;
  }

  private JComponent buildShapeSpacingRow() {
    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
    row.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    for (ShapeScale shape : ShapeScale.values()) {
      row.add(new ShapeChip(shape));
    }
    JPanel spacing = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    for (SpaceScale space : SpaceScale.values()) {
      JPanel bar = new JPanel();
      Runnable refresh =
          () -> {
            bar.setBackground(ColorRole.PRIMARY.resolve());
            bar.setPreferredSize(new Dimension(space.px(), 24));
          };
      refresh.run();
      tokenRefreshers.add(refresh);
      bar.setToolTipText(space.name() + " = " + space.px() + "px");
      spacing.add(bar);
      spacing.add(Box.createHorizontalStrut(8));
    }
    row.add(Box.createHorizontalStrut(16));
    row.add(spacing);
    return row;
  }

  private JLabel sectionLabel(String text) {
    JLabel label = new JLabel(text);
    label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    Runnable refresh =
        () -> {
          label.setFont(TypeRole.TITLE_SMALL.resolve());
          label.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
        };
    refresh.run();
    tokenRefreshers.add(refresh);
    return label;
  }

  // --- Custom-painted demo components — these resolve tokens live at paint time ---

  /** A single color-role swatch that paints itself from the role's live-resolved value. */
  private static final class Swatch extends JComponent {

    private final ColorRole role;

    Swatch(ColorRole role) {
      this.role = role;
      setPreferredSize(new Dimension(210, 56));
      setToolTipText(role.uiKey());
    }

    @Override
    protected void paintComponent(Graphics g) {
      Color fill = role.resolve();
      g.setColor(fill);
      g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
      g.setColor(ColorRole.OUTLINE_VARIANT.resolve());
      g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
      g.setColor(labelColor(fill));
      g.setFont(getFont().deriveFont(Font.PLAIN, 11f));
      g.drawString(role.name(), 10, 22);
      g.drawString(toHex(fill), 10, 40);
    }

    // Surface roles pair with a contrast-guaranteed on-role; the ON_* roles and the utility
    // colors (shadow / scrim / surfaceTint) have no on-pair — they are never a surface a
    // component paints text onto — so this swatch chart falls back to a luminance-picked
    // black/white. This is a display concern of the chart, not a gap in the token system.
    private Color labelColor(Color fill) {
      return role.on().map(ColorRole::resolve).orElseGet(() -> readableOn(fill));
    }

    private static Color readableOn(Color background) {
      double luminance =
          (0.299 * background.getRed()
                  + 0.587 * background.getGreen()
                  + 0.114 * background.getBlue())
              / 255.0;
      return luminance > 0.55 ? Color.BLACK : Color.WHITE;
    }

    private static String toHex(Color color) {
      return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
  }

  /** A rounded chip demonstrating one {@link ShapeScale} step at the live-resolved radius. */
  private static final class ShapeChip extends JComponent {

    private final ShapeScale shape;

    ShapeChip(ShapeScale shape) {
      this.shape = shape;
      setPreferredSize(new Dimension(72, 48));
      setToolTipText(shape.name());
    }

    @Override
    protected void paintComponent(Graphics g) {
      int radius = Math.min(shape.px(), Math.min(getWidth(), getHeight()));
      g.setColor(ColorRole.SECONDARY_CONTAINER.resolve());
      g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
      g.setColor(ColorRole.ON_SECONDARY_CONTAINER.resolve());
      g.setFont(getFont().deriveFont(Font.PLAIN, 11f));
      g.drawString(shape.key(), 8, getHeight() / 2 + 4);
    }
  }
}
