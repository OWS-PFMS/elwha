package com.owspfm.elwha.showcase;

import com.owspfm.elwha.button.ButtonInteractionMode;
import com.owspfm.elwha.button.ButtonShape;
import com.owspfm.elwha.button.ButtonSize;
import com.owspfm.elwha.button.ButtonVariant;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.button.playground.ButtonPlaygroundPanels;
import com.owspfm.elwha.card.playground.CursorReferencePanel;
import com.owspfm.elwha.card.playground.ElwhaCardListShowcase;
import com.owspfm.elwha.card.playground.GalleryPanel;
import com.owspfm.elwha.card.playground.LiveConfigPanel;
import com.owspfm.elwha.card.playground.SnippetPanel;
import com.owspfm.elwha.chip.playground.ChipPlaygroundPanels;
import com.owspfm.elwha.iconbutton.playground.IconButtonPlaygroundPanels;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.surface.playground.SurfacePlaygroundPanels;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.Theme;
import com.owspfm.elwha.theme.playground.FoundationsPanels;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;

/**
 * The Elwha Showcase — the unified, curated playground for the whole Elwha component set.
 *
 * <p>A left <em>sidebar nav</em> selects one surface at a time into a card-swapped content area; a
 * header bar carries the light / dark / system mode toggle. The nav is organised into three
 * sections:
 *
 * <ul>
 *   <li><strong>Foundations</strong> — the design tokens: color roles, type scale, and the
 *       raw-Swing gallery (see {@link FoundationsPanels}).
 *   <li><strong>Components</strong> — Button, Chip, Icon Button, Card, and Surface, each a single
 *       inner tabbed pane of {@code Workbench} (interactive) and {@code Gallery} (matrix) views.
 *   <li><strong>Containers</strong> — the multi-instance surfaces: Chip List, Card List, and the
 *       Button / Icon Button group demos.
 * </ul>
 *
 * <p>Most panels are composed from the existing factored playground builders ({@code
 * ButtonPlaygroundPanels} and friends) so the Showcase and the standalone playgrounds never drift;
 * component Workbenches are progressively migrated onto the shared {@link ComponentWorkbench}
 * scaffold. The locked design is {@code docs/research/elwha-showcase-design.md}.
 *
 * <p>Run with: {@code mvn compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.showcase.ElwhaShowcase"}
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ElwhaShowcase {

  private final List<Runnable> tokenRefreshers = new ArrayList<>();
  private final JPanel content = new JPanel(new CardLayout());
  private JLabel statusLabel;

  private ElwhaShowcase() {}

  /**
   * Launches the Showcase.
   *
   * @param args ignored
   * @version v0.3.0
   * @since v0.3.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.SYSTEM).build());
    SwingUtilities.invokeLater(() -> new ElwhaShowcase().buildAndShow());
  }

  private void buildAndShow() {
    final JFrame frame = new JFrame("The Elwha Showcase");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    final JPanel root = new JPanel(new BorderLayout());
    root.add(buildHeaderBar(), BorderLayout.NORTH);

    final JTree nav = buildNav();
    final JScrollPane navScroll = new JScrollPane(nav);
    navScroll.setPreferredSize(new Dimension(236, 0));
    navScroll.setBorder(
        BorderFactory.createMatteBorder(0, 0, 0, 1, UIManager.getColor("Component.borderColor")));
    root.add(navScroll, BorderLayout.WEST);
    root.add(content, BorderLayout.CENTER);

    frame.setContentPane(root);
    frame.setSize(1320, 860);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  // --- header bar ---

  private JComponent buildHeaderBar() {
    final JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    bar.add(new JLabel("Mode:"));

    final ButtonGroup group = new ButtonGroup();
    for (final Mode mode : new Mode[] {Mode.LIGHT, Mode.DARK, Mode.SYSTEM}) {
      final JToggleButton button = new JToggleButton(mode.name());
      button.addActionListener(event -> switchMode(mode));
      if (ElwhaTheme.current().mode() == mode) {
        button.setSelected(true);
      }
      group.add(button);
      bar.add(button);
    }

    bar.add(Box.createHorizontalStrut(16));
    bar.add(new JLabel("Palette:"));
    bar.add(buildPalettePicker());

    statusLabel = new JLabel();
    bar.add(Box.createHorizontalStrut(16));
    bar.add(statusLabel);
    updateStatus();
    return bar;
  }

  // The picker is populated from MaterialPalettes.bundled() — directory-derived, so a new palette
  // JSON dropped into the resources palettes/ directory appears here with no code change.
  private JComponent buildPalettePicker() {
    final List<Theme> themes = MaterialPalettes.bundled();
    final JComboBox<Theme> picker = new JComboBox<>(themes.toArray(new Theme[0]));
    picker.setRenderer(
        new DefaultListCellRenderer() {
          @Override
          public Component getListCellRendererComponent(
              final JList<?> list,
              final Object value,
              final int index,
              final boolean isSelected,
              final boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Theme theme) {
              setText(theme.name());
            }
            return this;
          }
        });

    // Select the entry matching the installed theme before wiring the listener, so seeding the
    // initial selection does not fire a redundant re-install.
    final String installed = ElwhaTheme.current().theme().name();
    for (final Theme theme : themes) {
      if (theme.name().equals(installed)) {
        picker.setSelectedItem(theme);
        break;
      }
    }
    picker.addActionListener(
        event -> {
          if (picker.getSelectedItem() instanceof Theme theme) {
            switchTheme(theme);
          }
        });
    return picker;
  }

  private void switchMode(final Mode mode) {
    ElwhaTheme.install(ElwhaTheme.current().withMode(mode));
    // install() dispatches the component-tree repaint; refresh explicitly-set token state too.
    SwingUtilities.invokeLater(
        () -> {
          tokenRefreshers.forEach(Runnable::run);
          updateStatus();
        });
  }

  private void switchTheme(final Theme theme) {
    ElwhaTheme.install(ElwhaTheme.current().withTheme(theme));
    // install() dispatches the component-tree repaint; refresh explicitly-set token state too.
    SwingUtilities.invokeLater(
        () -> {
          tokenRefreshers.forEach(Runnable::run);
          updateStatus();
        });
  }

  private void updateStatus() {
    final Mode requested = ElwhaTheme.current().mode();
    statusLabel.setText(
        "Theme: "
            + ElwhaTheme.current().theme().name()
            + "   ·   requested "
            + requested
            + " → resolved "
            + requested.resolved());
  }

  // --- sidebar nav + content cards ---

  private JTree buildNav() {
    final DefaultMutableTreeNode root = new DefaultMutableTreeNode("Showcase");

    final DefaultMutableTreeNode foundations = new DefaultMutableTreeNode("Foundations");
    addLeaf(foundations, "Color Roles", scroll(FoundationsPanels.buildColorRoles(tokenRefreshers)));
    addLeaf(foundations, "Type Scale", scroll(FoundationsPanels.buildTypeScale(tokenRefreshers)));
    addLeaf(foundations, "Swing Comps", scroll(FoundationsPanels.buildSwingComps(tokenRefreshers)));
    root.add(foundations);

    final DefaultMutableTreeNode components = new DefaultMutableTreeNode("Components");
    addLeaf(components, "Button", buildButtonComponent());
    addLeaf(components, "Chip", buildChipComponent());
    addLeaf(components, "Icon Button", buildIconButtonComponent());
    addLeaf(components, "Card", buildCardComponent());
    addLeaf(components, "Surface", buildSurfaceComponent());
    root.add(components);

    final DefaultMutableTreeNode containers = new DefaultMutableTreeNode("Containers");
    addLeaf(containers, "Chip List", ChipPlaygroundPanels.buildLiveListPanel());
    addLeaf(containers, "Card List", new ElwhaCardListShowcase());
    addLeaf(containers, "Button Group", buildButtonGroupContainer());
    addLeaf(
        containers,
        "Icon Button Group",
        scroll(IconButtonPlaygroundPanels.buildToggleExamplesPanel()));
    root.add(containers);

    final JTree tree = new JTree(root);
    tree.setRootVisible(false);
    tree.setShowsRootHandles(true);
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    tree.setCellRenderer(navRenderer());
    tree.setRowHeight(28);
    tree.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    for (int i = 0; i < tree.getRowCount(); i++) {
      tree.expandRow(i);
    }
    tree.addTreeSelectionListener(
        event -> {
          final DefaultMutableTreeNode node =
              (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
          if (node != null && node.isLeaf()) {
            ((CardLayout) content.getLayout()).show(content, node.getUserObject().toString());
          }
        });
    // Open on the first Foundations leaf (row 0 = Foundations section, row 1 = Color Roles).
    tree.setSelectionRow(1);
    return tree;
  }

  private void addLeaf(
      final DefaultMutableTreeNode section, final String label, final Component panel) {
    section.add(new DefaultMutableTreeNode(label));
    content.add(panel, label);
  }

  // --- component surfaces: Workbench (interactive) + Gallery (matrix) ---

  private static JComponent buildButtonComponent() {
    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Workbench", buildButtonWorkbench());
    tabs.addTab(
        "Gallery",
        scroll(
            stack(
                gallerySection(
                    "Variants & states", ButtonPlaygroundPanels.buildVariantGalleryPanel()),
                gallerySection("Sizes", ButtonPlaygroundPanels.buildSizesPanel()))));
    return tabs;
  }

  private static JComponent buildButtonWorkbench() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    final JComboBox<ButtonVariant> variantBox = new JComboBox<>(ButtonVariant.values());
    variantBox.setSelectedItem(ButtonVariant.FILLED);
    final JComboBox<ButtonInteractionMode> modeBox =
        new JComboBox<>(ButtonInteractionMode.values());
    final JComboBox<ButtonSize> sizeBox = new JComboBox<>(ButtonSize.values());
    sizeBox.setSelectedItem(ButtonSize.S);
    final JComboBox<ButtonShape> shapeBox = new JComboBox<>(ButtonShape.values());
    final JComboBox<ButtonSurfaceRole> surfaceBox = new JComboBox<>(ButtonSurfaceRole.values());
    final JSpinner borderWidth = new JSpinner(new SpinnerNumberModel(1, 0, 4, 1));
    final JCheckBox iconBox = new JCheckBox("Leading icon");
    final JCheckBox selectedBox = new JCheckBox("Selected");
    final JCheckBox enabledBox = new JCheckBox("Enabled", true);

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Button");
    controls.addControl("Variant", variantBox);
    controls.addControl("Interaction mode", modeBox);
    controls.addControl("Size", sizeBox);
    controls.addControl("Shape", shapeBox);
    controls.addSection("Appearance");
    controls.addControl("Surface role override", surfaceBox);
    controls.addControl("Border width", borderWidth);
    controls.addControl("", iconBox);
    controls.addSection("State");
    controls.addControl("", selectedBox);
    controls.addControl("", enabledBox);

    final Runnable apply =
        () -> {
          final ButtonVariant variant = (ButtonVariant) variantBox.getSelectedItem();
          ButtonInteractionMode mode = (ButtonInteractionMode) modeBox.getSelectedItem();
          // SELECTABLE + TEXT is illegal — guard the pairing so the demo never throws.
          if (mode == ButtonInteractionMode.SELECTABLE && variant == ButtonVariant.TEXT) {
            mode = ButtonInteractionMode.CLICKABLE;
            modeBox.setSelectedItem(ButtonInteractionMode.CLICKABLE);
          }
          final ButtonSize size = (ButtonSize) sizeBox.getSelectedItem();
          final ButtonShape shape = (ButtonShape) shapeBox.getSelectedItem();
          final ButtonSurfaceRole surface = (ButtonSurfaceRole) surfaceBox.getSelectedItem();
          final int width = (Integer) borderWidth.getValue();
          final boolean icon = iconBox.isSelected();
          final boolean selected = selectedBox.isSelected();
          final boolean enabled = enabledBox.isSelected();

          final ElwhaButton button =
              icon
                  ? new ElwhaButton("Common button", MaterialIcons.delete(size.iconSizePx()))
                  : new ElwhaButton("Common button");
          button.setVariant(variant).setButtonSize(size).setShape(shape).setBorderWidth(width);
          if (mode == ButtonInteractionMode.SELECTABLE) {
            button.setInteractionMode(ButtonInteractionMode.SELECTABLE);
            button.setSelected(selected);
          }
          if (surface.role != null) {
            button.setSurfaceRole(surface.role);
          }
          button.setEnabled(enabled);
          workbench.setStage(button);
          workbench.setCode(
              renderButtonCode(
                  variant, mode, size, shape, surface, width, icon, selected, enabled));
        };
    variantBox.addActionListener(event -> apply.run());
    modeBox.addActionListener(event -> apply.run());
    sizeBox.addActionListener(event -> apply.run());
    shapeBox.addActionListener(event -> apply.run());
    surfaceBox.addActionListener(event -> apply.run());
    borderWidth.addChangeListener(event -> apply.run());
    iconBox.addActionListener(event -> apply.run());
    selectedBox.addActionListener(event -> apply.run());
    enabledBox.addActionListener(event -> apply.run());
    apply.run();
    return workbench;
  }

  private static String renderButtonCode(
      final ButtonVariant variant,
      final ButtonInteractionMode mode,
      final ButtonSize size,
      final ButtonShape shape,
      final ButtonSurfaceRole surface,
      final int width,
      final boolean icon,
      final boolean selected,
      final boolean enabled) {
    final StringBuilder code = new StringBuilder(256);
    if (icon) {
      code.append("new ElwhaButton(\"Common button\",\n");
      code.append("    MaterialIcons.delete(").append(size.iconSizePx()).append("))\n");
    } else {
      code.append("new ElwhaButton(\"Common button\")\n");
    }
    code.append("    .setVariant(ButtonVariant.").append(variant).append(")\n");
    code.append("    .setButtonSize(ButtonSize.").append(size).append(")\n");
    code.append("    .setShape(ButtonShape.").append(shape).append(")");
    if (width != 1) {
      code.append("\n    .setBorderWidth(").append(width).append(")");
    }
    if (surface.role != null) {
      code.append("\n    .setSurfaceRole(ColorRole.").append(surface.role).append(")");
    }
    if (mode == ButtonInteractionMode.SELECTABLE) {
      code.append("\n    .setInteractionMode(ButtonInteractionMode.SELECTABLE)");
      code.append("\n    .setSelected(").append(selected).append(")");
    }
    code.append(";");
    if (!enabled) {
      code.append("\n// button.setEnabled(false);");
    }
    return code.toString();
  }

  /**
   * Wraps a nullable surface-role override as a combo-box entry — {@code VARIANT_DEFAULT} → null.
   */
  private enum ButtonSurfaceRole {
    VARIANT_DEFAULT(null),
    PRIMARY(ColorRole.PRIMARY),
    PRIMARY_CONTAINER(ColorRole.PRIMARY_CONTAINER),
    SECONDARY_CONTAINER(ColorRole.SECONDARY_CONTAINER),
    TERTIARY_CONTAINER(ColorRole.TERTIARY_CONTAINER),
    SURFACE_CONTAINER_HIGHEST(ColorRole.SURFACE_CONTAINER_HIGHEST),
    ERROR_CONTAINER(ColorRole.ERROR_CONTAINER);

    private final ColorRole role;

    ButtonSurfaceRole(final ColorRole role) {
      this.role = role;
    }
  }

  // Story-5: the Button group demo on the shared ContainerWorkbench scaffold.
  private static JComponent buildButtonGroupContainer() {
    final ContainerWorkbench workbench = new ContainerWorkbench();

    final JComboBox<ButtonVariant> variantBox =
        new JComboBox<>(
            new ButtonVariant[] {
              ButtonVariant.ELEVATED,
              ButtonVariant.FILLED,
              ButtonVariant.FILLED_TONAL,
              ButtonVariant.OUTLINED
            });
    variantBox.setSelectedItem(ButtonVariant.FILLED);
    final JCheckBox mandatoryBox = new JCheckBox("Mandatory", true);

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Button group");
    controls.addControl("Variant", variantBox);
    controls.addControl("", mandatoryBox);

    final Runnable rebuild =
        () -> {
          final ButtonVariant variant = (ButtonVariant) variantBox.getSelectedItem();
          final boolean mandatory = mandatoryBox.isSelected();
          final JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
          final com.owspfm.elwha.button.ButtonGroup group =
              new com.owspfm.elwha.button.ButtonGroup().setMandatory(mandatory);
          ElwhaButton first = null;
          for (final String label : new String[] {"List", "Grid", "Compact"}) {
            final ElwhaButton item =
                new ElwhaButton(label)
                    .setVariant(variant)
                    .setInteractionMode(ButtonInteractionMode.SELECTABLE);
            group.add(item);
            if (first == null) {
              first = item;
            }
            row.add(item);
          }
          if (mandatory && first != null) {
            group.setSelected(first);
          }
          group.addSelectionChangeListener(
              evt -> {
                final ElwhaButton picked = (ElwhaButton) evt.getNewValue();
                workbench.logEvent("selected: " + (picked == null ? "(none)" : picked.getText()));
              });
          workbench.setContainer(row);
        };
    variantBox.addActionListener(event -> rebuild.run());
    mandatoryBox.addActionListener(event -> rebuild.run());
    rebuild.run();
    return workbench;
  }

  // Chip has no single-instance Workbench yet — that is net-new work for Story 6 of epic #130.
  private static JComponent buildChipComponent() {
    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Gallery", scroll(ChipPlaygroundPanels.buildVariantGallery()));
    return tabs;
  }

  private static JComponent buildIconButtonComponent() {
    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Workbench", scroll(IconButtonPlaygroundPanels.buildLivePanel()));
    tabs.addTab(
        "Gallery",
        scroll(
            stack(
                IconButtonPlaygroundPanels.buildVariantGalleryPanel(),
                IconButtonPlaygroundPanels.buildSizesPanel())));
    return tabs;
  }

  private static JComponent buildSurfaceComponent() {
    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Workbench", buildSurfaceWorkbench());
    tabs.addTab("Gallery", scroll(SurfacePlaygroundPanels.buildMatrixPanel()));
    return tabs;
  }

  // The Surface Workbench's Component segment configures the demonstrated ElwhaSurface; the
  // scaffold then sits it on its own configurable stage surface — surface-on-surface.
  private static JComponent buildSurfaceWorkbench() {
    final ComponentWorkbench workbench = new ComponentWorkbench();
    final SurfaceControlPanel demo = new SurfaceControlPanel(workbench.controls(), false);
    workbench.setStage(demo.surface());
    demo.addChangeListener(() -> workbench.setCode(demo.code()));
    workbench.setCode(demo.code());
    return workbench;
  }

  private static JComponent buildCardComponent() {
    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Workbench", buildCardWorkbench());
    tabs.addTab("Gallery", new GalleryPanel());
    tabs.addTab("Cursors", new CursorReferencePanel());
    return tabs;
  }

  private static JComponent buildCardWorkbench() {
    final LiveConfigPanel live = new LiveConfigPanel();
    final SnippetPanel snippet = new SnippetPanel();
    snippet.update(live.snapshot());
    live.addConfigChangeListener(snippet::update);
    snippet.setPreferredSize(new Dimension(620, 220));

    final JPanel wrap = new JPanel(new BorderLayout());
    wrap.add(live, BorderLayout.CENTER);
    wrap.add(snippet, BorderLayout.SOUTH);
    return wrap;
  }

  // --- helpers ---

  private static JComponent stack(final JComponent... parts) {
    final JPanel column = new JPanel();
    column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
    for (final JComponent part : parts) {
      part.setAlignmentX(JComponent.LEFT_ALIGNMENT);
      column.add(part);
    }
    return column;
  }

  // Wraps a gallery matrix with a bold section heading so a multi-matrix Gallery tab reads as
  // distinct sections rather than two floating grids.
  private static JComponent gallerySection(final String title, final JComponent body) {
    final JPanel section = new JPanel(new BorderLayout());
    section.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    final JLabel heading = new JLabel(title);
    heading.setFont(heading.getFont().deriveFont(Font.BOLD));
    heading.setBorder(BorderFactory.createEmptyBorder(16, 20, 0, 20));
    section.add(heading, BorderLayout.NORTH);
    section.add(body, BorderLayout.CENTER);
    return section;
  }

  private static JScrollPane scroll(final Component view) {
    final JScrollPane pane = new JScrollPane(view);
    pane.setBorder(BorderFactory.createEmptyBorder());
    pane.getVerticalScrollBar().setUnitIncrement(16);
    return pane;
  }

  private static DefaultTreeCellRenderer navRenderer() {
    final DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
    renderer.setLeafIcon(null);
    renderer.setOpenIcon(null);
    renderer.setClosedIcon(null);
    return renderer;
  }
}
