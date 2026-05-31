package com.owspfm.elwha.theme.playground;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.SpaceScale;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Reusable builders for the Elwha design-token <em>Foundations</em> surfaces — the color-role
 * swatches, the type scale, and the raw-Swing component gallery that inherits the theme through the
 * FlatLaf-native key mapping.
 *
 * <p>Factored out so {@code The Elwha Showcase} ({@link com.owspfm.elwha.showcase.ElwhaShowcase})
 * can compose the same Foundations panels that {@code ThemePlayground} presents — mirroring the
 * factored-builder pattern already established for the components by {@code ChipPlaygroundPanels},
 * {@code ButtonPlaygroundPanels}, and friends.
 *
 * <p>Each builder takes a {@code List<Runnable>} into which it registers <em>token refreshers</em>:
 * labels and panels carrying explicitly-set fonts or colors do not re-resolve on a theme re-install
 * the way UI-resource-backed widgets do, so the host runs every registered refresher after a mode
 * switch.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.3.0
 */
public final class FoundationsPanels {

  private FoundationsPanels() {}

  /**
   * Builds the Color Roles panel: all 49 {@link ColorRole} swatches in a grid, each painting itself
   * from the role's live-resolved value.
   *
   * @param refreshers registry the builder adds its token refreshers to
   * @return the color-roles panel
   * @version v0.3.0
   * @since v0.3.0
   */
  public static JComponent buildColorRoles(final List<Runnable> refreshers) {
    final JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    panel.add(
        sectionLabel(
            "All 49 color roles — fill is the role, text is its on-pair (or onSurface)",
            refreshers));
    panel.add(Box.createVerticalStrut(8));

    final JPanel grid = new JPanel(new GridLayout(0, 4, 8, 8));
    for (final ColorRole role : ColorRole.values()) {
      grid.add(new Swatch(role));
    }
    grid.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    panel.add(grid);
    return panel;
  }

  /**
   * Uniform render size for every cell in the icon gallery — bigger than the 24-dp default so the
   * glyphs read clearly in the grid.
   */
  private static final int GALLERY_ICON_SIZE_PX = 28;

  /**
   * Builds the Icons panel: every glyph factory exposed by {@link MaterialIcons}, rendered at a
   * uniform size and labelled with its factory-method name (the call to copy). The roster is
   * discovered reflectively from {@code MaterialIcons}' zero-arg {@link FlatSVGIcon} factories, so
   * a newly-bundled glyph surfaces here with no edit to this builder, and it is sorted so each
   * {@code *Filled} variant sits immediately after its outline counterpart. Each cell builds a
   * fresh icon instance via the sized factory overload — no shared {@code ColorFilter} is mutated
   * (cf. #197) — so every glyph re-themes correctly when the Showcase mode toggle flips
   * light&nbsp;↔&nbsp;dark.
   *
   * @param refreshers registry the builder adds its token refreshers to
   * @return the icons gallery panel
   * @version v0.4.0
   * @since v0.4.0
   */
  public static JComponent buildIconGallery(final List<Runnable> refreshers) {
    final JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    panel.add(
        sectionLabel(
            "Every bundled Material Symbol — labelled with its MaterialIcons factory call",
            refreshers));
    panel.add(Box.createVerticalStrut(12));

    final JPanel grid = new JPanel(new GridLayout(0, 6, 12, 12));
    grid.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    for (final String name : iconFactoryNames()) {
      grid.add(iconCell(name, refreshers));
    }
    panel.add(grid);
    return panel;
  }

  private static JComponent iconCell(final String name, final List<Runnable> refreshers) {
    final JPanel cell = new JPanel();
    cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
    cell.setToolTipText("MaterialIcons." + name + "()");

    final JLabel icon = new JLabel(galleryIcon(name));
    icon.setAlignmentX(JComponent.CENTER_ALIGNMENT);

    final JLabel caption = new JLabel(name);
    caption.setAlignmentX(JComponent.CENTER_ALIGNMENT);
    caption.setHorizontalAlignment(SwingConstants.CENTER);

    final Runnable refresh =
        () -> {
          caption.setFont(TypeRole.LABEL_SMALL.resolve());
          caption.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
          icon.repaint();
        };
    refresh.run();
    refreshers.add(refresh);

    cell.add(icon);
    cell.add(Box.createVerticalStrut(6));
    cell.add(caption);
    return cell;
  }

  // The icon-factory roster is discovered reflectively: every public static zero-arg method on
  // MaterialIcons returning a FlatSVGIcon is a glyph factory (pushPin, delete, edit, ...). Sorted
  // by
  // name so each *Filled variant sits immediately after its outline counterpart in the grid.
  private static List<String> iconFactoryNames() {
    final List<String> names = new ArrayList<>();
    for (final Method method : MaterialIcons.class.getMethods()) {
      if (Modifier.isStatic(method.getModifiers())
          && method.getParameterCount() == 0
          && method.getReturnType() == FlatSVGIcon.class) {
        names.add(method.getName());
      }
    }
    names.sort(Comparator.naturalOrder());
    return names;
  }

  // Builds a fresh, themed icon at the uniform gallery size by invoking the factory's sized (int)
  // overload reflectively — a per-cell instance, so no shared ColorFilter is mutated (cf. #197).
  private static Icon galleryIcon(final String name) {
    try {
      return (Icon)
          MaterialIcons.class.getMethod(name, int.class).invoke(null, GALLERY_ICON_SIZE_PX);
    } catch (final ReflectiveOperationException e) {
      throw new IllegalStateException("No sized overload for MaterialIcons." + name, e);
    }
  }

  /**
   * Builds the Type Scale panel: every {@link TypeRole} rendered as a sample line in the bundled
   * Inter face at the role's point size.
   *
   * @param refreshers registry the builder adds its token refreshers to
   * @return the type-scale panel
   * @version v0.3.0
   * @since v0.3.0
   */
  public static JComponent buildTypeScale(final List<Runnable> refreshers) {
    final JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    panel.add(sectionLabel("The 12 type roles — bundled Inter (Regular / Medium)", refreshers));
    panel.add(Box.createVerticalStrut(12));

    for (final TypeRole role : TypeRole.values()) {
      final JLabel sample =
          new JLabel(role.name() + "  —  The quick brown fox (" + role.pt() + "pt)");
      sample.setAlignmentX(JComponent.LEFT_ALIGNMENT);
      final Runnable refresh =
          () -> {
            sample.setFont(role.resolve());
            sample.setForeground(ColorRole.ON_SURFACE.resolve());
          };
      refresh.run();
      refreshers.add(refresh);
      panel.add(sample);
      panel.add(Box.createVerticalStrut(8));
    }
    return panel;
  }

  /**
   * Builds the Swing Comps panel: raw Swing widgets that inherit the theme through the
   * FlatLaf-native key mapping, plus the shape and spacing scales.
   *
   * @param refreshers registry the builder adds its token refreshers to
   * @return the swing-comps panel
   * @version v0.3.0
   * @since v0.3.0
   */
  public static JComponent buildSwingComps(final List<Runnable> refreshers) {
    final JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

    panel.add(
        sectionLabel(
            "Raw Swing — inherits the theme via the FlatLaf-native key mapping", refreshers));
    panel.add(Box.createVerticalStrut(8));
    panel.add(buildRawSwingRow(refreshers));
    panel.add(Box.createVerticalStrut(24));

    panel.add(sectionLabel("Shape & spacing scales", refreshers));
    panel.add(Box.createVerticalStrut(8));
    panel.add(buildShapeSpacingRow(refreshers));
    return panel;
  }

  private static JComponent buildRawSwingRow(final List<Runnable> refreshers) {
    final JPanel area = new JPanel();
    area.setLayout(new BoxLayout(area, BoxLayout.Y_AXIS));
    area.setAlignmentX(JComponent.LEFT_ALIGNMENT);

    area.add(rowBlock("Buttons", buildButtonsRow(), refreshers));
    area.add(Box.createVerticalStrut(12));
    area.add(rowBlock("Icon buttons", buildIconButtonsRow(), refreshers));
    area.add(Box.createVerticalStrut(12));
    area.add(
        rowBlock(
            "Sized icons (SVG — crisp at any size)", buildSizedIconsRow(refreshers), refreshers));
    area.add(Box.createVerticalStrut(12));
    area.add(rowBlock("Text", buildTextRow(), refreshers));
    area.add(Box.createVerticalStrut(12));
    area.add(rowBlock("Selection", buildSelectionRow(), refreshers));
    area.add(Box.createVerticalStrut(12));
    area.add(rowBlock("Range", buildRangeRow(), refreshers));
    area.add(Box.createVerticalStrut(12));
    area.add(rowBlock("List & tree", buildListTreeRow(), refreshers));
    return area;
  }

  private static JComponent buildButtonsRow() {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
    final JButton normal = new JButton("Button");
    final JButton emphasis = new JButton("Default");
    final JToggleButton toggle = new JToggleButton("Toggle");
    toggle.setSelected(true);
    row.add(normal);
    row.add(emphasis);
    row.add(toggle);
    return row;
  }

  private static JComponent buildIconButtonsRow() {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));

    final JButton bordered = new JButton(MaterialIcons.edit());
    bordered.setToolTipText("Bordered icon button");

    final JButton borderless = new JButton(MaterialIcons.delete());
    borderless.putClientProperty("JButton.buttonType", "borderless");
    borderless.setToolTipText("Borderless icon button (toolbar style)");

    final JButton iconText = new JButton("Add", MaterialIcons.add());
    iconText.setToolTipText("Icon + text button");

    final JToggleButton iconToggle = new JToggleButton(MaterialIcons.visibility());
    iconToggle.setSelected(true);
    iconToggle.setToolTipText("Icon toggle button");

    final JButton pinToggle = new JButton(MaterialIcons.pushPin());
    pinToggle.putClientProperty("JButton.buttonType", "borderless");
    pinToggle.setRequestFocusEnabled(false);
    pinToggle.setToolTipText("Click to toggle pin (icon swaps, focus does not move)");
    pinToggle.addActionListener(
        event -> {
          final boolean pinned = Boolean.TRUE.equals(pinToggle.getClientProperty("pinned"));
          pinToggle.putClientProperty("pinned", !pinned);
          pinToggle.setIcon(!pinned ? MaterialIcons.pushPinFilled() : MaterialIcons.pushPin());
        });

    final JToggleButton viewGrid = new JToggleButton(MaterialIcons.gridView());
    final JToggleButton viewTable = new JToggleButton(MaterialIcons.table());
    final JToggleButton viewBackground = new JToggleButton(MaterialIcons.backgroundGridSmall());
    viewGrid.setToolTipText("Grid view");
    viewTable.setToolTipText("Table view");
    viewBackground.setToolTipText("Background view");
    final ButtonGroup viewGroup = new ButtonGroup();
    viewGroup.add(viewGrid);
    viewGroup.add(viewTable);
    viewGroup.add(viewBackground);
    viewGrid.setSelected(true);
    final JPanel segmented = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
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

  private static JComponent buildSizedIconsRow(final List<Runnable> refreshers) {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
    final int[] sizes = {16, 20, 24, 32, 48};
    for (final int size : sizes) {
      final JPanel cell = new JPanel();
      cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
      final JLabel iconLabel = new JLabel(MaterialIcons.favorite(size));
      iconLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
      final JLabel caption = new JLabel(size + "px");
      caption.setAlignmentX(JComponent.CENTER_ALIGNMENT);
      final Runnable refresh =
          () -> {
            caption.setFont(TypeRole.LABEL_SMALL.resolve());
            caption.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
          };
      refresh.run();
      refreshers.add(refresh);
      cell.add(iconLabel);
      cell.add(Box.createVerticalStrut(4));
      cell.add(caption);
      row.add(cell);
    }
    return row;
  }

  private static JComponent buildTextRow() {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
    final JTextField field = new JTextField("Text field", 14);
    final JTextArea area = new JTextArea("Multi-line\ntext area\nfor longer input", 3, 18);
    area.setLineWrap(true);
    area.setWrapStyleWord(true);
    final JScrollPane areaScroll = new JScrollPane(area);
    areaScroll.setPreferredSize(new Dimension(220, 64));
    row.add(field);
    row.add(areaScroll);
    return row;
  }

  private static JComponent buildSelectionRow() {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));

    final JCheckBox check = new JCheckBox("Checkbox", true);
    final JRadioButton radioA = new JRadioButton("Radio A", true);
    final JRadioButton radioB = new JRadioButton("Radio B");
    final ButtonGroup radios = new ButtonGroup();
    radios.add(radioA);
    radios.add(radioB);
    final JPanel selectionStack = new JPanel();
    selectionStack.setLayout(new BoxLayout(selectionStack, BoxLayout.Y_AXIS));
    selectionStack.add(check);
    selectionStack.add(radioA);
    selectionStack.add(radioB);

    final JComboBox<String> combo = new JComboBox<>(new String[] {"One", "Two", "Three"});
    final JSpinner spinner = new JSpinner(new SpinnerNumberModel(50, 0, 100, 1));

    row.add(selectionStack);
    row.add(combo);
    row.add(spinner);
    return row;
  }

  private static JComponent buildRangeRow() {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
    final JSlider slider = new JSlider(0, 100, 60);
    slider.setPreferredSize(new Dimension(200, slider.getPreferredSize().height));
    final JProgressBar progress = new JProgressBar(0, 100);
    progress.setValue(45);
    progress.setPreferredSize(new Dimension(200, progress.getPreferredSize().height));
    row.add(slider);
    row.add(progress);
    return row;
  }

  private static JComponent buildListTreeRow() {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));

    final DefaultListModel<String> listModel = new DefaultListModel<>();
    listModel.addElement("List item A");
    listModel.addElement("List item B");
    listModel.addElement("List item C");
    listModel.addElement("List item D");
    final JList<String> list = new JList<>(listModel);
    list.setSelectedIndex(1);
    final JScrollPane listScroll = new JScrollPane(list);
    listScroll.setPreferredSize(new Dimension(160, 100));

    final DefaultMutableTreeNode root = new DefaultMutableTreeNode("Project");
    final DefaultMutableTreeNode defaults = new DefaultMutableTreeNode("Defaults");
    defaults.add(new DefaultMutableTreeNode("Default chart"));
    defaults.add(new DefaultMutableTreeNode("Default factors"));
    root.add(defaults);
    final DefaultMutableTreeNode custom = new DefaultMutableTreeNode("Custom");
    custom.add(new DefaultMutableTreeNode("Scenario A"));
    custom.add(new DefaultMutableTreeNode("Scenario B"));
    root.add(custom);
    final JTree tree = new JTree(root);
    tree.expandRow(0);
    tree.expandRow(1);
    tree.setSelectionRow(2);
    final JScrollPane treeScroll = new JScrollPane(tree);
    treeScroll.setPreferredSize(new Dimension(200, 120));

    row.add(listScroll);
    row.add(treeScroll);
    return row;
  }

  private static JComponent rowBlock(
      final String caption, final JComponent row, final List<Runnable> refreshers) {
    final JPanel block = new JPanel();
    block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
    block.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    final JLabel cap = new JLabel(caption);
    cap.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    final Runnable refresh =
        () -> {
          cap.setFont(TypeRole.LABEL_MEDIUM.resolve());
          cap.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
        };
    refresh.run();
    refreshers.add(refresh);
    block.add(cap);
    block.add(Box.createVerticalStrut(4));
    row.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    block.add(row);
    return block;
  }

  private static JComponent buildShapeSpacingRow(final List<Runnable> refreshers) {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
    row.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    for (final ShapeScale shape : ShapeScale.values()) {
      row.add(new ShapeChip(shape));
    }
    final JPanel spacing = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    for (final SpaceScale space : SpaceScale.values()) {
      final JPanel bar = new JPanel();
      final Runnable refresh =
          () -> {
            bar.setBackground(ColorRole.PRIMARY.resolve());
            bar.setPreferredSize(new Dimension(space.px(), 24));
          };
      refresh.run();
      refreshers.add(refresh);
      bar.setToolTipText(space.name() + " = " + space.px() + "px");
      spacing.add(bar);
      spacing.add(Box.createHorizontalStrut(8));
    }
    row.add(Box.createHorizontalStrut(16));
    row.add(spacing);
    return row;
  }

  private static JLabel sectionLabel(final String text, final List<Runnable> refreshers) {
    final JLabel label = new JLabel(text);
    label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    final Runnable refresh =
        () -> {
          label.setFont(TypeRole.TITLE_SMALL.resolve());
          label.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
        };
    refresh.run();
    refreshers.add(refresh);
    return label;
  }

  /** A single color-role swatch that paints itself from the role's live-resolved value. */
  private static final class Swatch extends JComponent {

    private final ColorRole role;

    Swatch(final ColorRole role) {
      this.role = role;
      setPreferredSize(new Dimension(210, 56));
      setToolTipText(role.uiKey());
    }

    @Override
    protected void paintComponent(final Graphics g) {
      final Color fill = role.resolve();
      g.setColor(fill);
      g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
      g.setColor(ColorRole.OUTLINE_VARIANT.resolve());
      g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
      g.setColor(labelColor(fill));
      g.setFont(getFont().deriveFont(Font.PLAIN, 11f));
      g.drawString(role.name(), 10, 22);
      g.drawString(toHex(fill), 10, 40);
    }

    private Color labelColor(final Color fill) {
      return role.on().map(ColorRole::resolve).orElseGet(() -> readableOn(fill));
    }

    private static Color readableOn(final Color background) {
      final double luminance =
          (0.299 * background.getRed()
                  + 0.587 * background.getGreen()
                  + 0.114 * background.getBlue())
              / 255.0;
      return luminance > 0.55 ? Color.BLACK : Color.WHITE;
    }

    private static String toHex(final Color color) {
      return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
  }

  /** A rounded chip demonstrating one {@link ShapeScale} step at the live-resolved radius. */
  private static final class ShapeChip extends JComponent {

    private final ShapeScale shape;

    ShapeChip(final ShapeScale shape) {
      this.shape = shape;
      setPreferredSize(new Dimension(72, 48));
      setToolTipText(shape.name());
    }

    @Override
    protected void paintComponent(final Graphics g) {
      final int radius = Math.min(shape.px(), Math.min(getWidth(), getHeight()));
      g.setColor(ColorRole.SECONDARY_CONTAINER.resolve());
      g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
      g.setColor(ColorRole.ON_SECONDARY_CONTAINER.resolve());
      g.setFont(getFont().deriveFont(Font.PLAIN, 11f));
      g.drawString(shape.key(), 8, getHeight() / 2 + 4);
    }
  }
}
