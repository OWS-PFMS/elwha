package com.owspfm.elwha.theme.playground;

import com.owspfm.elwha.chip.ChipVariant;
import com.owspfm.elwha.chip.ElwhaChip;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Config;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.SpaceScale;
import com.owspfm.elwha.theme.TypeRole;
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
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Interactive visual harness for the Elwha token foundation.
 *
 * <p>Three tabs — color-role swatches, the type scale, and a components gallery — let a maintainer
 * eyeball that the {@code Elwha.*} tokens and the FlatLaf-native key mapping land correctly, and
 * that raw Swing widgets ({@code JButton}, {@code JTextField}, …) read as coherent next to a {@link
 * ElwhaChip}. The mode toggle re-installs the theme at runtime, exercising the binding rule
 * end-to-end. This is the visual validation surface for Epic #30 sub-story #34.
 *
 * <p>Run with: {@code mvn compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.theme.playground.ThemePlayground"}
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public final class ThemePlayground {

  private final List<Runnable> tokenRefreshers = new ArrayList<>();
  private JLabel statusLabel;
  private JButton defaultButton;

  private ThemePlayground() {}

  /**
   * Launches the playground.
   *
   * @param args ignored
   * @version v0.1.0
   * @since v0.1.0
   */
  public static void main(String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.SYSTEM).build());
    SwingUtilities.invokeLater(() -> new ThemePlayground().buildAndShow());
  }

  private void buildAndShow() {
    JFrame frame = new JFrame("Elwha Theme Playground");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    JPanel root = new JPanel(new java.awt.BorderLayout());
    root.add(buildControlBar(), java.awt.BorderLayout.NORTH);

    JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Color Roles", new JScrollPane(buildColorTab()));
    tabs.addTab("Type Scale", new JScrollPane(buildTypeTab()));
    tabs.addTab("Components", new JScrollPane(buildComponentsTab()));
    root.add(tabs, java.awt.BorderLayout.CENTER);

    frame.setContentPane(root);
    if (defaultButton != null) {
      frame.getRootPane().setDefaultButton(defaultButton);
    }
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
      if (ElwhaTheme.current().mode() == mode) {
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
    Config next = ElwhaTheme.current().withMode(mode);
    ElwhaTheme.install(next);
    // install() dispatches the component-tree repaint; refresh explicitly-set token state too.
    SwingUtilities.invokeLater(
        () -> {
          tokenRefreshers.forEach(Runnable::run);
          updateStatus();
        });
  }

  private void updateStatus() {
    Mode requested = ElwhaTheme.current().mode();
    Mode resolved = requested.resolved();
    statusLabel.setText(
        "Theme: "
            + ElwhaTheme.current().theme().name()
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
        sectionLabel("ElwhaChip (V1) — should read as coherent next to the raw widgets above"));
    panel.add(Box.createVerticalStrut(8));
    panel.add(buildChipRow());
    panel.add(Box.createVerticalStrut(24));

    panel.add(sectionLabel("Shape & spacing scales"));
    panel.add(Box.createVerticalStrut(8));
    panel.add(buildShapeSpacingRow());
    return panel;
  }

  private JComponent buildRawSwingRow() {
    JPanel area = new JPanel();
    area.setLayout(new BoxLayout(area, BoxLayout.Y_AXIS));
    area.setAlignmentX(JComponent.LEFT_ALIGNMENT);

    area.add(rowBlock("Buttons", buildButtonsRow()));
    area.add(Box.createVerticalStrut(12));
    area.add(rowBlock("Icon buttons", buildIconButtonsRow()));
    area.add(Box.createVerticalStrut(12));
    area.add(rowBlock("Sized icons (SVG — crisp at any size)", buildSizedIconsRow()));
    area.add(Box.createVerticalStrut(12));
    area.add(rowBlock("Text", buildTextRow()));
    area.add(Box.createVerticalStrut(12));
    area.add(rowBlock("Selection", buildSelectionRow()));
    area.add(Box.createVerticalStrut(12));
    area.add(rowBlock("Range", buildRangeRow()));
    area.add(Box.createVerticalStrut(12));
    area.add(rowBlock("List & tree", buildListTreeRow()));
    return area;
  }

  private JComponent buildButtonsRow() {
    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
    JButton normal = new JButton("Button");
    // Becomes the frame's default button in buildAndShow() — the genuine FlatLaf mechanism for
    // the emphasis-button look, rather than a client property.
    defaultButton = new JButton("Default");
    JToggleButton toggle = new JToggleButton("Toggle");
    toggle.setSelected(true);
    row.add(normal);
    row.add(defaultButton);
    row.add(toggle);
    return row;
  }

  private JComponent buildIconButtonsRow() {
    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));

    JButton bordered = new JButton(MaterialIcons.edit());
    bordered.setToolTipText("Bordered icon button");

    JButton borderless = new JButton(MaterialIcons.delete());
    borderless.putClientProperty("JButton.buttonType", "borderless");
    borderless.setToolTipText("Borderless icon button (toolbar style)");

    JButton iconText = new JButton("Add", MaterialIcons.add());
    iconText.setToolTipText("Icon + text button");

    JToggleButton iconToggle = new JToggleButton(MaterialIcons.visibility());
    iconToggle.setSelected(true);
    iconToggle.setToolTipText("Icon toggle button");

    // Borderless click-toggle: a plain JButton whose icon swaps between an M3 fill-0 and
    // fill-1 pair on click. Uses the MaterialIcons no-arg form (DEFAULT_SIZE = 24, the M3
    // standard for icon buttons). setRequestFocusEnabled(false) keeps the click from moving
    // focus to the button — the idiomatic toolbar-button behavior, and the visible cue is
    // the icon swap alone (no lingering focus ring).
    JButton pinToggle = new JButton(MaterialIcons.pushPin());
    pinToggle.putClientProperty("JButton.buttonType", "borderless");
    pinToggle.setRequestFocusEnabled(false);
    pinToggle.setToolTipText("Click to toggle pin (icon swaps, focus does not move)");
    pinToggle.addActionListener(
        event -> {
          boolean pinned = Boolean.TRUE.equals(pinToggle.getClientProperty("pinned"));
          pinToggle.putClientProperty("pinned", !pinned);
          pinToggle.setIcon(!pinned ? MaterialIcons.pushPinFilled() : MaterialIcons.pushPin());
        });

    // Segmented icon-toggle group — the OWS app uses this pattern for view-mode pickers.
    JToggleButton viewGrid = new JToggleButton(MaterialIcons.gridView());
    JToggleButton viewTable = new JToggleButton(MaterialIcons.table());
    JToggleButton viewBackground = new JToggleButton(MaterialIcons.backgroundGridSmall());
    viewGrid.setToolTipText("Grid view");
    viewTable.setToolTipText("Table view");
    viewBackground.setToolTipText("Background view");
    ButtonGroup viewGroup = new ButtonGroup();
    viewGroup.add(viewGrid);
    viewGroup.add(viewTable);
    viewGroup.add(viewBackground);
    viewGrid.setSelected(true);
    JPanel segmented = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    segmented.add(viewGrid);
    segmented.add(viewTable);
    segmented.add(viewBackground);

    row.add(bordered);
    row.add(borderless);
    row.add(iconText);
    row.add(iconToggle);
    row.add(pinToggle);
    row.add(segmented);
    return row;
  }

  private JComponent buildSizedIconsRow() {
    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
    int[] sizes = {16, 20, 24, 32, 48};
    for (int size : sizes) {
      JPanel cell = new JPanel();
      cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
      JLabel iconLabel = new JLabel(MaterialIcons.favorite(size));
      iconLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
      JLabel caption = new JLabel(size + "px");
      caption.setAlignmentX(JComponent.CENTER_ALIGNMENT);
      Runnable refresh =
          () -> {
            caption.setFont(TypeRole.LABEL_SMALL.resolve());
            caption.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
          };
      refresh.run();
      tokenRefreshers.add(refresh);
      cell.add(iconLabel);
      cell.add(Box.createVerticalStrut(4));
      cell.add(caption);
      row.add(cell);
    }
    return row;
  }

  private JComponent buildTextRow() {
    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
    JTextField field = new JTextField("Text field", 14);
    JTextArea area = new JTextArea("Multi-line\ntext area\nfor longer input", 3, 18);
    area.setLineWrap(true);
    area.setWrapStyleWord(true);
    JScrollPane areaScroll = new JScrollPane(area);
    areaScroll.setPreferredSize(new Dimension(220, 64));
    row.add(field);
    row.add(areaScroll);
    return row;
  }

  private JComponent buildSelectionRow() {
    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));

    JCheckBox check = new JCheckBox("Checkbox", true);
    JRadioButton radioA = new JRadioButton("Radio A", true);
    JRadioButton radioB = new JRadioButton("Radio B");
    ButtonGroup radios = new ButtonGroup();
    radios.add(radioA);
    radios.add(radioB);
    JPanel selectionStack = new JPanel();
    selectionStack.setLayout(new BoxLayout(selectionStack, BoxLayout.Y_AXIS));
    selectionStack.add(check);
    selectionStack.add(radioA);
    selectionStack.add(radioB);

    JComboBox<String> combo = new JComboBox<>(new String[] {"One", "Two", "Three"});
    JSpinner spinner = new JSpinner(new SpinnerNumberModel(50, 0, 100, 1));

    row.add(selectionStack);
    row.add(combo);
    row.add(spinner);
    return row;
  }

  private JComponent buildRangeRow() {
    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
    JSlider slider = new JSlider(0, 100, 60);
    slider.setPreferredSize(new Dimension(200, slider.getPreferredSize().height));
    JProgressBar progress = new JProgressBar(0, 100);
    progress.setValue(45);
    progress.setPreferredSize(new Dimension(200, progress.getPreferredSize().height));
    row.add(slider);
    row.add(progress);
    return row;
  }

  private JComponent buildListTreeRow() {
    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));

    DefaultListModel<String> listModel = new DefaultListModel<>();
    listModel.addElement("List item A");
    listModel.addElement("List item B");
    listModel.addElement("List item C");
    listModel.addElement("List item D");
    JList<String> list = new JList<>(listModel);
    list.setSelectedIndex(1);
    JScrollPane listScroll = new JScrollPane(list);
    listScroll.setPreferredSize(new Dimension(160, 100));

    DefaultMutableTreeNode root = new DefaultMutableTreeNode("Project");
    DefaultMutableTreeNode defaults = new DefaultMutableTreeNode("Defaults");
    defaults.add(new DefaultMutableTreeNode("Default chart"));
    defaults.add(new DefaultMutableTreeNode("Default factors"));
    root.add(defaults);
    DefaultMutableTreeNode custom = new DefaultMutableTreeNode("Custom");
    custom.add(new DefaultMutableTreeNode("Scenario A"));
    custom.add(new DefaultMutableTreeNode("Scenario B"));
    root.add(custom);
    JTree tree = new JTree(root);
    tree.expandRow(0);
    tree.expandRow(1);
    tree.setSelectionRow(2);
    JScrollPane treeScroll = new JScrollPane(tree);
    treeScroll.setPreferredSize(new Dimension(200, 120));

    row.add(listScroll);
    row.add(treeScroll);
    return row;
  }

  private JComponent rowBlock(String caption, JComponent row) {
    JPanel block = new JPanel();
    block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
    block.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    JLabel cap = new JLabel(caption);
    cap.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    Runnable refresh =
        () -> {
          cap.setFont(TypeRole.LABEL_MEDIUM.resolve());
          cap.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
        };
    refresh.run();
    tokenRefreshers.add(refresh);
    block.add(cap);
    block.add(Box.createVerticalStrut(4));
    row.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    block.add(row);
    return block;
  }

  private JComponent buildChipRow() {
    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
    row.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    row.add(new ElwhaChip("Filled").setVariant(ChipVariant.FILLED));
    row.add(new ElwhaChip("Outlined").setVariant(ChipVariant.OUTLINED));
    row.add(new ElwhaChip("Ghost").setVariant(ChipVariant.GHOST));
    row.add(new ElwhaChip("Warm accent").setVariant(ChipVariant.WARM_ACCENT));
    row.add(new ElwhaChip("Selected").setVariant(ChipVariant.FILLED).setSelected(true));
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
