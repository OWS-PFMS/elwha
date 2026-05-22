package com.owspfm.elwha.showcase;

import com.owspfm.elwha.button.ButtonInteractionMode;
import com.owspfm.elwha.button.ButtonShape;
import com.owspfm.elwha.button.ButtonSize;
import com.owspfm.elwha.button.ButtonVariant;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.button.playground.ButtonPlaygroundPanels;
import com.owspfm.elwha.buttongroup.ButtonGroupColorStyle;
import com.owspfm.elwha.buttongroup.ButtonGroupVariant;
import com.owspfm.elwha.buttongroup.ElwhaButtonGroup;
import com.owspfm.elwha.buttongroup.ResizeMode;
import com.owspfm.elwha.buttongroup.SelectionMode;
import com.owspfm.elwha.card.CardVariant;
import com.owspfm.elwha.card.CollapseRule;
import com.owspfm.elwha.card.ElwhaCard;
import com.owspfm.elwha.card.ElwhaCardActions;
import com.owspfm.elwha.card.ElwhaCardChevron;
import com.owspfm.elwha.card.ElwhaCardDivider;
import com.owspfm.elwha.card.ElwhaCardExpandLink;
import com.owspfm.elwha.card.ElwhaCardHeader;
import com.owspfm.elwha.card.ElwhaCardLeadingIcon;
import com.owspfm.elwha.card.ElwhaCardMedia;
import com.owspfm.elwha.card.ElwhaCardSupportingText;
import com.owspfm.elwha.card.ElwhaCardThumbnail;
import com.owspfm.elwha.card.ExpansionOverflow;
import com.owspfm.elwha.card.ThumbnailShape;
import com.owspfm.elwha.card.playground.CursorReferencePanel;
import com.owspfm.elwha.card.playground.GalleryPanel;
import com.owspfm.elwha.chip.ChipInteractionMode;
import com.owspfm.elwha.chip.ChipVariant;
import com.owspfm.elwha.chip.ElwhaChip;
import com.owspfm.elwha.chip.playground.ChipPlaygroundPanels;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.iconbutton.IconButtonGroup;
import com.owspfm.elwha.iconbutton.IconButtonInteractionMode;
import com.owspfm.elwha.iconbutton.IconButtonSize;
import com.owspfm.elwha.iconbutton.IconButtonVariant;
import com.owspfm.elwha.iconbutton.playground.IconButtonPlaygroundPanels;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.surface.playground.SurfacePlaygroundPanels;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.SpaceScale;
import com.owspfm.elwha.theme.Theme;
import com.owspfm.elwha.theme.playground.FoundationsPanels;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
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
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
 *   <li><strong>Components</strong> — Button, Chip, Icon Button, Button Group, Card, and Surface,
 *       each a single inner tabbed pane of {@code Workbench} (interactive) and {@code Gallery}
 *       (matrix) views.
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
  private final List<Theme> primaryThemes = MaterialPalettes.primary();
  private final List<Theme> secondaryThemes = MaterialPalettes.secondary();
  private JLabel statusLabel;
  private JComboBox<Theme> palettePicker;
  private Theme primarySelection;
  private Theme secondarySelection;
  private boolean secondaryTier;
  private boolean pickerAdjusting;

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
    // Park initial focus on the nav tree, not the header button group — otherwise a focused mode
    // segment and the selected mode segment read as two selections at once.
    SwingUtilities.invokeLater(nav::requestFocusInWindow);
  }

  // --- header bar ---

  private JComponent buildHeaderBar() {
    final JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));

    bar.add(new JLabel("Mode:"));
    bar.add(buildModeToggle());

    bar.add(Box.createHorizontalStrut(16));
    bar.add(new JLabel("Tier:"));
    bar.add(buildTierToggle());

    bar.add(Box.createHorizontalStrut(16));
    bar.add(new JLabel("Palette:"));
    bar.add(buildPalettePicker());

    statusLabel = new JLabel();
    bar.add(Box.createHorizontalStrut(16));
    bar.add(statusLabel);
    updateStatus();
    return bar;
  }

  // The light / dark / system mode toggle — an ElwhaButtonGroup connected group in REQUIRED mode.
  // The Showcase dogfooding the library's own M3 segmented-control component is the operator-named
  // OWS use case behind epic #170; the standalone ElwhaButtonGroup Workbench is under Components.
  private JComponent buildModeToggle() {
    final Mode[] modes = {Mode.LIGHT, Mode.DARK, Mode.SYSTEM};
    final String[] iconNames = {"light_mode", "dark_mode", "brightness_auto"};
    final String[] labels = {"Light", "Dark", "System"};
    final ElwhaButtonGroup toggle =
        ElwhaButtonGroup.connected()
            .setSelectionMode(SelectionMode.REQUIRED)
            .setButtonSize(ButtonSize.S)
            .setResizeMode(ResizeMode.FIXED)
            .setColorStyle(ButtonGroupColorStyle.TONAL);
    for (int i = 0; i < modes.length; i++) {
      final MaterialIcons.IconPair pair =
          MaterialIcons.pair(iconNames[i], IconButtonSize.S.iconPx());
      final ElwhaIconButton segment = new ElwhaIconButton(pair.resting());
      // Outline at rest, filled when selected — the M3 toggle-icon swap.
      segment.setIcons(pair.resting(), pair.filled());
      // Icon-only segments carry their label as tooltip + accessible name.
      segment.setToolTipText(labels[i]);
      segment.setName(labels[i]);
      toggle.add(segment);
    }
    final Mode current = ElwhaTheme.current().mode();
    for (int i = 0; i < modes.length; i++) {
      if (modes[i] == current) {
        toggle.setSelectedIndex(i);
      }
    }
    // Listener attached after seeding so the initial selection does not re-install the theme.
    toggle.addSelectionListener(group -> switchMode(modes[group.getSelectedIndex()]));
    return toggle;
  }

  // The primary / secondary palette-tier toggle — likewise an ElwhaButtonGroup connected group.
  private JComponent buildTierToggle() {
    final ElwhaButtonGroup toggle =
        ElwhaButtonGroup.connected()
            .setSelectionMode(SelectionMode.REQUIRED)
            .setButtonSize(ButtonSize.XS)
            .setResizeMode(ResizeMode.FIXED)
            .setColorStyle(ButtonGroupColorStyle.TONAL)
            .add(new ElwhaButton("Primary"))
            .add(new ElwhaButton("Secondary"));
    toggle.setSelectedIndex(secondaryTier ? 1 : 0);
    toggle.addSelectionListener(group -> switchTier(group.getSelectedIndex() == 1));
    return toggle;
  }

  // The picker shows one tier at a time; each tier is directory-derived and spectrally ordered by
  // MaterialPalettes. A new Elwha-format palette JSON dropped into the tier's resource subdirectory
  // appears here with no code change.
  private JComponent buildPalettePicker() {
    palettePicker = new JComboBox<>();
    palettePicker.setRenderer(
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

    // The app installs MaterialPalettes.baseline() at startup — a primary-tier theme.
    primarySelection = matchOrFirst(primaryThemes, ElwhaTheme.current().theme().name());
    secondarySelection = secondaryThemes.get(0);
    populatePicker(primaryThemes, primarySelection);

    palettePicker.addActionListener(
        event -> {
          if (!pickerAdjusting && palettePicker.getSelectedItem() instanceof Theme theme) {
            if (secondaryTier) {
              secondarySelection = theme;
            } else {
              primarySelection = theme;
            }
            switchTheme(theme);
          }
        });
    return palettePicker;
  }

  // Repopulates the picker with one tier's themes without firing a redundant re-install — the
  // model swap and selection seed run under the pickerAdjusting guard.
  private void populatePicker(final List<Theme> themes, final Theme selection) {
    pickerAdjusting = true;
    palettePicker.setModel(new DefaultComboBoxModel<>(themes.toArray(new Theme[0])));
    palettePicker.setSelectedItem(selection);
    pickerAdjusting = false;
  }

  private void switchTier(final boolean secondary) {
    if (secondary == secondaryTier) {
      return;
    }
    secondaryTier = secondary;
    final Theme selection = secondary ? secondarySelection : primarySelection;
    populatePicker(secondary ? secondaryThemes : primaryThemes, selection);
    switchTheme(selection);
  }

  private static Theme matchOrFirst(final List<Theme> themes, final String name) {
    for (final Theme theme : themes) {
      if (theme.name().equals(name)) {
        return theme;
      }
    }
    return themes.get(0);
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
    addLeaf(components, "Button Group", buildButtonGroupComponent());
    addLeaf(components, "Card", buildCardComponent());
    addLeaf(components, "Surface", buildSurfaceComponent());
    root.add(components);

    final DefaultMutableTreeNode containers = new DefaultMutableTreeNode("Containers");
    addLeaf(containers, "Chip List", new ChipListContainer().component());
    addLeaf(containers, "Card List", new CardListContainer().component());
    addLeaf(containers, "Button Group (mutex)", buildButtonGroupContainer());
    addLeaf(containers, "Icon Button Group (mutex)", buildIconButtonGroupContainer());
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

  private static JComponent buildChipComponent() {
    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Workbench", buildChipWorkbench());
    tabs.addTab(
        "Gallery",
        scroll(
            gallerySection(
                "Variants, modes & states", ChipPlaygroundPanels.buildVariantGalleryMatrix())));
    return tabs;
  }

  private static JComponent buildChipWorkbench() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    final JTextField textField = new JTextField("Chip", 14);
    final JComboBox<ChipVariant> variantBox = new JComboBox<>(ChipVariant.values());
    final JComboBox<ChipInteractionMode> modeBox = new JComboBox<>(ChipInteractionMode.values());
    modeBox.setSelectedItem(ChipInteractionMode.SELECTABLE);
    final JComboBox<ChipSurfaceRole> surfaceBox = new JComboBox<>(ChipSurfaceRole.values());
    final JComboBox<ShapeScale> shapeBox = new JComboBox<>(ShapeScale.values());
    final JComboBox<SpaceScale> padHBox = new JComboBox<>(SpaceScale.values());
    padHBox.setSelectedItem(SpaceScale.MD);
    final JComboBox<SpaceScale> padVBox = new JComboBox<>(SpaceScale.values());
    padVBox.setSelectedItem(SpaceScale.XS);
    final JSpinner borderWidth = new JSpinner(new SpinnerNumberModel(1, 0, 4, 1));
    final JComboBox<LeadingSlot> leadingSlotBox = new JComboBox<>(LeadingSlot.values());
    final JCheckBox leadingAffordanceActiveBox = new JCheckBox("Affordance active");
    leadingAffordanceActiveBox.setEnabled(false);
    final JComboBox<TrailingSlot> trailingSlotBox = new JComboBox<>(TrailingSlot.values());
    final JCheckBox trailingAffordanceActiveBox = new JCheckBox("Affordance active");
    trailingAffordanceActiveBox.setEnabled(false);
    final JCheckBox selectedBox = new JCheckBox("Selected");
    final JCheckBox enabledBox = new JCheckBox("Enabled", true);

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Chip");
    controls.addControl("Text", textField);
    controls.addControl("Variant", variantBox);
    controls.addControl("Interaction mode", modeBox);
    controls.addSection("Appearance");
    controls.addControl("Surface role override", surfaceBox);
    controls.addControl("Shape", shapeBox);
    controls.addControl("Padding — horizontal", padHBox);
    controls.addControl("Padding — vertical", padVBox);
    controls.addControl("Border width", borderWidth);
    controls.addControl("Leading slot", leadingSlotBox);
    controls.addControl("", leadingAffordanceActiveBox);
    controls.addControl("Trailing slot", trailingSlotBox);
    controls.addControl("", trailingAffordanceActiveBox);
    controls.addSection("State");
    controls.addControl("", selectedBox);
    controls.addControl("", enabledBox);

    final Runnable apply =
        () -> {
          final String text = textField.getText();
          final ChipVariant variant = (ChipVariant) variantBox.getSelectedItem();
          final ChipInteractionMode mode = (ChipInteractionMode) modeBox.getSelectedItem();
          final ChipSurfaceRole surface = (ChipSurfaceRole) surfaceBox.getSelectedItem();
          final ShapeScale shape = (ShapeScale) shapeBox.getSelectedItem();
          final SpaceScale padH = (SpaceScale) padHBox.getSelectedItem();
          final SpaceScale padV = (SpaceScale) padVBox.getSelectedItem();
          final int width = (Integer) borderWidth.getValue();
          final LeadingSlot leadingSlot = (LeadingSlot) leadingSlotBox.getSelectedItem();
          leadingAffordanceActiveBox.setEnabled(leadingSlot == LeadingSlot.AFFORDANCE);
          final boolean leadingAffordanceActive = leadingAffordanceActiveBox.isSelected();
          final TrailingSlot trailingSlot = (TrailingSlot) trailingSlotBox.getSelectedItem();
          trailingAffordanceActiveBox.setEnabled(trailingSlot == TrailingSlot.AFFORDANCE);
          final boolean trailingAffordanceActive = trailingAffordanceActiveBox.isSelected();
          // GHOST does not render a selected state (issue #50) — reflect that in the control.
          final boolean ghost = variant == ChipVariant.GHOST;
          selectedBox.setEnabled(!ghost);
          selectedBox.setToolTipText(
              ghost ? "GHOST does not render a selected state (issue #50)." : null);
          final boolean selected = selectedBox.isSelected();
          final boolean enabled = enabledBox.isSelected();

          final ElwhaChip chip = new ElwhaChip(text);
          chip.setVariant(variant)
              .setInteractionMode(mode)
              .setShape(shape)
              .setPadding(padH, padV)
              .setBorderWidth(width);
          if (surface.role != null) {
            chip.setSurfaceRole(surface.role);
          }
          if (leadingSlot == LeadingSlot.ICON) {
            chip.setLeadingIcon(MaterialIcons.star(14));
          } else if (leadingSlot == LeadingSlot.AFFORDANCE) {
            final MaterialIcons.IconPair star = MaterialIcons.pair("star", 14);
            chip.setLeadingAffordance(
                star.resting(),
                star.filled(),
                leadingAffordanceActive,
                false,
                "Toggle",
                leadingAffordanceActiveBox::doClick);
          }
          if (trailingSlot == TrailingSlot.ICON) {
            // Display-only indicator — the M3 filter-chip dropdown caret; the chip body owns the
            // click, the caret has no hit target of its own.
            chip.setTrailingIndicator(MaterialIcons.expandMore(14));
          } else if (trailingSlot == TrailingSlot.BUTTON) {
            // No-op handler — the single-instance Workbench stage has nothing to remove; a real
            // consumer supplies onRemove. The "Button" label keeps the click affordance honest.
            chip.setTrailingIcon(MaterialIcons.delete(14), "Remove", () -> {});
          } else if (trailingSlot == TrailingSlot.AFFORDANCE) {
            final MaterialIcons.IconPair favorite = MaterialIcons.pair("favorite", 14);
            chip.setTrailingAffordance(
                favorite.resting(),
                favorite.filled(),
                trailingAffordanceActive,
                false,
                "Toggle",
                trailingAffordanceActiveBox::doClick);
          }
          chip.setSelected(selected);
          chip.setEnabled(enabled);
          workbench.setStage(chip);
          workbench.setCode(
              renderChipCode(
                  text,
                  variant,
                  mode,
                  surface,
                  shape,
                  padH,
                  padV,
                  width,
                  leadingSlot,
                  leadingAffordanceActive,
                  trailingSlot,
                  trailingAffordanceActive,
                  selected,
                  enabled));
        };

    textField.getDocument().addDocumentListener(new SimpleDocumentListener(apply));
    variantBox.addActionListener(event -> apply.run());
    modeBox.addActionListener(event -> apply.run());
    surfaceBox.addActionListener(event -> apply.run());
    shapeBox.addActionListener(event -> apply.run());
    padHBox.addActionListener(event -> apply.run());
    padVBox.addActionListener(event -> apply.run());
    borderWidth.addChangeListener(event -> apply.run());
    leadingSlotBox.addActionListener(event -> apply.run());
    leadingAffordanceActiveBox.addActionListener(event -> apply.run());
    trailingSlotBox.addActionListener(event -> apply.run());
    trailingAffordanceActiveBox.addActionListener(event -> apply.run());
    selectedBox.addActionListener(event -> apply.run());
    enabledBox.addActionListener(event -> apply.run());
    apply.run();
    return workbench;
  }

  private static String renderChipCode(
      final String text,
      final ChipVariant variant,
      final ChipInteractionMode mode,
      final ChipSurfaceRole surface,
      final ShapeScale shape,
      final SpaceScale padH,
      final SpaceScale padV,
      final int width,
      final LeadingSlot leadingSlot,
      final boolean leadingAffordanceActive,
      final TrailingSlot trailingSlot,
      final boolean trailingAffordanceActive,
      final boolean selected,
      final boolean enabled) {
    final StringBuilder code = new StringBuilder(320);
    code.append("ElwhaChip chip = new ElwhaChip(\"").append(text).append("\");\n");
    code.append("chip.setVariant(ChipVariant.").append(variant).append(")\n");
    code.append("    .setInteractionMode(ChipInteractionMode.").append(mode).append(")\n");
    code.append("    .setShape(ShapeScale.").append(shape).append(")\n");
    code.append("    .setPadding(SpaceScale.")
        .append(padH)
        .append(", SpaceScale.")
        .append(padV)
        .append(")");
    if (width != 1) {
      code.append("\n    .setBorderWidth(").append(width).append(")");
    }
    if (surface.role != null) {
      code.append("\n    .setSurfaceRole(ColorRole.").append(surface.role).append(")");
    }
    code.append(";");
    if (leadingSlot == LeadingSlot.ICON) {
      code.append("\nchip.setLeadingIcon(MaterialIcons.star(14));");
    } else if (leadingSlot == LeadingSlot.AFFORDANCE) {
      code.append("\nMaterialIcons.IconPair star = MaterialIcons.pair(\"star\", 14);");
      code.append("\nchip.setLeadingAffordance(\n")
          .append("    star.resting(), star.filled(), ")
          .append(leadingAffordanceActive)
          .append(", false, \"Toggle\", onClick);");
    }
    if (trailingSlot == TrailingSlot.ICON) {
      code.append("\nchip.setTrailingIndicator(MaterialIcons.expandMore(14));");
    } else if (trailingSlot == TrailingSlot.BUTTON) {
      code.append("\nchip.setTrailingIcon(MaterialIcons.delete(14), \"Remove\", onRemove);");
    } else if (trailingSlot == TrailingSlot.AFFORDANCE) {
      code.append("\nMaterialIcons.IconPair favorite = MaterialIcons.pair(\"favorite\", 14);");
      code.append("\nchip.setTrailingAffordance(\n")
          .append("    favorite.resting(), favorite.filled(), ")
          .append(trailingAffordanceActive)
          .append(", false, \"Toggle\", onClick);");
    }
    if (selected) {
      code.append("\nchip.setSelected(true);");
    }
    if (!enabled) {
      code.append("\nchip.setEnabled(false);");
    }
    return code.toString();
  }

  /**
   * Wraps a nullable surface-role override as a combo-box entry — {@code VARIANT_DEFAULT} → null.
   */
  private enum ChipSurfaceRole {
    VARIANT_DEFAULT(null),
    PRIMARY(ColorRole.PRIMARY),
    PRIMARY_CONTAINER(ColorRole.PRIMARY_CONTAINER),
    SECONDARY(ColorRole.SECONDARY),
    SECONDARY_CONTAINER(ColorRole.SECONDARY_CONTAINER),
    TERTIARY(ColorRole.TERTIARY),
    TERTIARY_CONTAINER(ColorRole.TERTIARY_CONTAINER),
    SURFACE_VARIANT(ColorRole.SURFACE_VARIANT),
    ERROR_CONTAINER(ColorRole.ERROR_CONTAINER);

    private final ColorRole role;

    ChipSurfaceRole(final ColorRole role) {
      this.role = role;
    }
  }

  /** The Chip Workbench's leading-slot option — empty, a static icon, or a two-state affordance. */
  private enum LeadingSlot {
    NONE,
    ICON,
    AFFORDANCE
  }

  /**
   * The Chip Workbench's trailing-slot option — empty, a display-only indicator icon (the M3
   * filter-chip dropdown-caret pattern), a single-state action button (the M3 input-chip remove
   * pattern), or a two-state affordance.
   */
  private enum TrailingSlot {
    NONE,
    ICON,
    BUTTON,
    AFFORDANCE
  }

  /** A {@link DocumentListener} that runs one callback on any text-field change. */
  private static final class SimpleDocumentListener implements DocumentListener {
    private final Runnable onChange;

    SimpleDocumentListener(final Runnable onChange) {
      this.onChange = onChange;
    }

    @Override
    public void insertUpdate(final DocumentEvent event) {
      onChange.run();
    }

    @Override
    public void removeUpdate(final DocumentEvent event) {
      onChange.run();
    }

    @Override
    public void changedUpdate(final DocumentEvent event) {
      onChange.run();
    }
  }

  private static JComponent buildIconButtonComponent() {
    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Workbench", buildIconButtonWorkbench());
    tabs.addTab(
        "Gallery",
        scroll(
            stack(
                gallerySection(
                    "Variants & states", IconButtonPlaygroundPanels.buildVariantGalleryPanel()),
                gallerySection("Sizes", IconButtonPlaygroundPanels.buildSizesPanel()))));
    return tabs;
  }

  private static JComponent buildIconButtonWorkbench() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    final JComboBox<IconChoice> iconBox = new JComboBox<>(IconChoice.values());
    iconBox.setSelectedItem(IconChoice.FAVORITE);
    final JComboBox<IconButtonVariant> variantBox = new JComboBox<>(IconButtonVariant.values());
    variantBox.setSelectedItem(IconButtonVariant.FILLED_TONAL);
    final JComboBox<IconButtonInteractionMode> modeBox =
        new JComboBox<>(IconButtonInteractionMode.values());
    modeBox.setSelectedItem(IconButtonInteractionMode.SELECTABLE);
    final JComboBox<IconButtonSize> sizeBox = new JComboBox<>(IconButtonSize.values());
    sizeBox.setSelectedItem(IconButtonSize.M);
    final JComboBox<ShapeScale> shapeBox = new JComboBox<>(ShapeScale.values());
    shapeBox.setSelectedItem(ShapeScale.FULL);
    final JComboBox<IconButtonSurfaceRole> surfaceBox =
        new JComboBox<>(IconButtonSurfaceRole.values());
    final JSpinner borderWidth = new JSpinner(new SpinnerNumberModel(1, 0, 4, 1));
    final JCheckBox selectedBox = new JCheckBox("Selected");
    final JCheckBox enabledBox = new JCheckBox("Enabled", true);

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Icon Button");
    controls.addControl("Icon", iconBox);
    controls.addControl("Variant", variantBox);
    controls.addControl("Interaction mode", modeBox);
    controls.addControl("Size", sizeBox);
    controls.addControl("Shape", shapeBox);
    controls.addSection("Appearance");
    controls.addControl("Surface role override", surfaceBox);
    controls.addControl("Border width", borderWidth);
    controls.addSection("State");
    controls.addControl("", selectedBox);
    controls.addControl("", enabledBox);

    final Runnable apply =
        () -> {
          final IconChoice icon = (IconChoice) iconBox.getSelectedItem();
          final IconButtonVariant variant = (IconButtonVariant) variantBox.getSelectedItem();
          final IconButtonInteractionMode mode =
              (IconButtonInteractionMode) modeBox.getSelectedItem();
          final IconButtonSize size = (IconButtonSize) sizeBox.getSelectedItem();
          final ShapeScale shape = (ShapeScale) shapeBox.getSelectedItem();
          final IconButtonSurfaceRole surface =
              (IconButtonSurfaceRole) surfaceBox.getSelectedItem();
          final int width = (Integer) borderWidth.getValue();
          final boolean selectable = mode == IconButtonInteractionMode.SELECTABLE;
          selectedBox.setEnabled(selectable);
          final boolean selected = selectedBox.isSelected();
          final boolean enabled = enabledBox.isSelected();

          final MaterialIcons.IconPair pair = icon.pair(size.iconPx());
          final ElwhaIconButton button = new ElwhaIconButton(pair.resting());
          button
              .setVariant(variant)
              .setInteractionMode(mode)
              .setButtonSize(size)
              .setShape(shape)
              .setBorderWidth(width);
          if (surface.role != null) {
            button.setSurfaceRole(surface.role);
          }
          if (selectable) {
            button.setIcons(pair.resting(), pair.filled());
            button.setSelected(selected);
          }
          button.setEnabled(enabled);
          workbench.setStage(button);
          workbench.setCode(
              renderIconButtonCode(
                  icon, variant, mode, size, shape, surface, width, selected, enabled));
        };
    iconBox.addActionListener(event -> apply.run());
    variantBox.addActionListener(event -> apply.run());
    modeBox.addActionListener(event -> apply.run());
    sizeBox.addActionListener(event -> apply.run());
    shapeBox.addActionListener(event -> apply.run());
    surfaceBox.addActionListener(event -> apply.run());
    borderWidth.addChangeListener(event -> apply.run());
    selectedBox.addActionListener(event -> apply.run());
    enabledBox.addActionListener(event -> apply.run());
    apply.run();
    return workbench;
  }

  private static String renderIconButtonCode(
      final IconChoice icon,
      final IconButtonVariant variant,
      final IconButtonInteractionMode mode,
      final IconButtonSize size,
      final ShapeScale shape,
      final IconButtonSurfaceRole surface,
      final int width,
      final boolean selected,
      final boolean enabled) {
    final StringBuilder code = new StringBuilder(320);
    code.append("MaterialIcons.IconPair icon = MaterialIcons.pair(\"")
        .append(icon.baseName)
        .append("\", ")
        .append(size.iconPx())
        .append(");\n");
    code.append("ElwhaIconButton button = new ElwhaIconButton(icon.resting());\n");
    code.append("button.setVariant(IconButtonVariant.").append(variant).append(")\n");
    code.append("    .setInteractionMode(IconButtonInteractionMode.").append(mode).append(")\n");
    code.append("    .setButtonSize(IconButtonSize.").append(size).append(")\n");
    code.append("    .setShape(ShapeScale.").append(shape).append(")");
    if (width != 1) {
      code.append("\n    .setBorderWidth(").append(width).append(")");
    }
    if (surface.role != null) {
      code.append("\n    .setSurfaceRole(ColorRole.").append(surface.role).append(")");
    }
    code.append(";");
    if (mode == IconButtonInteractionMode.SELECTABLE) {
      code.append("\nbutton.setIcons(icon.resting(), icon.filled());");
      code.append("\nbutton.setSelected(").append(selected).append(");");
    }
    if (!enabled) {
      code.append("\nbutton.setEnabled(false);");
    }
    return code.toString();
  }

  /**
   * Wraps a nullable surface-role override as a combo-box entry — {@code VARIANT_DEFAULT} → null.
   */
  private enum IconButtonSurfaceRole {
    VARIANT_DEFAULT(null),
    PRIMARY(ColorRole.PRIMARY),
    PRIMARY_CONTAINER(ColorRole.PRIMARY_CONTAINER),
    SECONDARY_CONTAINER(ColorRole.SECONDARY_CONTAINER),
    TERTIARY_CONTAINER(ColorRole.TERTIARY_CONTAINER),
    SURFACE(ColorRole.SURFACE),
    SURFACE_CONTAINER_HIGHEST(ColorRole.SURFACE_CONTAINER_HIGHEST),
    ERROR_CONTAINER(ColorRole.ERROR_CONTAINER);

    private final ColorRole role;

    IconButtonSurfaceRole(final ColorRole role) {
      this.role = role;
    }
  }

  /** Bundled outline / fill icon pairs offered by the Icon Button Workbench's icon picker. */
  private enum IconChoice {
    FAVORITE("favorite"),
    STAR("star"),
    PUSH_PIN("push_pin"),
    ANCHOR("anchor"),
    VISIBILITY("visibility"),
    INFO("info"),
    HELP("help"),
    DELETE("delete"),
    EDIT("edit");

    private final String baseName;

    IconChoice(final String baseName) {
      this.baseName = baseName;
    }

    MaterialIcons.IconPair pair(final int size) {
      return MaterialIcons.pair(baseName, size);
    }
  }

  // The Icon Button group demo on the shared ContainerWorkbench scaffold.
  private static JComponent buildIconButtonGroupContainer() {
    final ContainerWorkbench workbench = new ContainerWorkbench();

    final JComboBox<IconButtonVariant> variantBox = new JComboBox<>(IconButtonVariant.values());
    variantBox.setSelectedItem(IconButtonVariant.FILLED_TONAL);
    final JCheckBox mandatoryBox = new JCheckBox("Mandatory", true);

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Icon Button group");
    controls.addControl("Variant", variantBox);
    controls.addControl("", mandatoryBox);

    final Runnable rebuild =
        () -> {
          final IconButtonVariant variant = (IconButtonVariant) variantBox.getSelectedItem();
          final boolean mandatory = mandatoryBox.isSelected();
          final JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
          final IconButtonGroup group = new IconButtonGroup(mandatory);
          for (final String[] entry :
              new String[][] {
                {"favorite", "Favorite"},
                {"star", "Star"},
                {"info", "Info"},
                {"help", "Help"}
              }) {
            final MaterialIcons.IconPair pair = MaterialIcons.pair(entry[0], 24);
            final ElwhaIconButton button =
                new ElwhaIconButton(pair.resting())
                    .setVariant(variant)
                    .setInteractionMode(IconButtonInteractionMode.SELECTABLE);
            button.setIcons(pair.resting(), pair.filled());
            final String label = entry[1];
            button.addSelectionChangeListener(
                event -> {
                  if (Boolean.TRUE.equals(event.getNewValue())) {
                    workbench.logEvent("selected: " + label);
                  }
                });
            group.add(button);
            row.add(button);
          }
          workbench.setContainer(row);
        };
    variantBox.addActionListener(event -> rebuild.run());
    mandatoryBox.addActionListener(event -> rebuild.run());
    rebuild.run();
    return workbench;
  }

  // --- Button Group component: ElwhaButtonGroup Workbench + Gallery ---

  /** Segment labels offered by the Button Group Workbench, in order. */
  private static final String[] SEGMENT_LABELS = {"Day", "Week", "Month", "Year", "All"};

  /** Material icon base names for the Button Group Workbench's icon segments, in order. */
  private static final String[] SEGMENT_ICONS = {"favorite", "star", "info", "help", "visibility"};

  /** The Button Group Workbench's segment-content option. */
  private enum SegmentContent {
    LABELS,
    ICONS,
    MIXED,
    TEXT_AND_ICONS
  }

  private static JComponent buildButtonGroupComponent() {
    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Workbench", buildButtonGroupWorkbench());
    tabs.addTab(
        "Gallery",
        scroll(
            stack(
                gallerySection("Standard variant", buildStandardGallerySection()),
                gallerySection("Connected — colour styles", buildColorStyleGallerySection()),
                gallerySection("Connected — size axis", buildSizeGallerySection()),
                gallerySection("Connected — selection modes", buildSelectionGallerySection()))));
    return tabs;
  }

  private static JComponent buildButtonGroupWorkbench() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    final JComboBox<ButtonGroupVariant> variantBox = new JComboBox<>(ButtonGroupVariant.values());
    variantBox.setSelectedItem(ButtonGroupVariant.CONNECTED);
    final JComboBox<SelectionMode> selectionBox = new JComboBox<>(SelectionMode.values());
    final JComboBox<ButtonSize> sizeBox = new JComboBox<>(ButtonSize.values());
    sizeBox.setSelectedItem(ButtonSize.S);
    final JComboBox<ButtonShape> shapeBox = new JComboBox<>(ButtonShape.values());
    final JComboBox<ResizeMode> resizeBox = new JComboBox<>(ResizeMode.values());
    final JComboBox<ButtonGroupColorStyle> colorBox =
        new JComboBox<>(ButtonGroupColorStyle.values());
    colorBox.setSelectedItem(ButtonGroupColorStyle.TONAL);
    final JComboBox<SegmentContent> contentBox = new JComboBox<>(SegmentContent.values());
    final JSpinner countSpinner = new JSpinner(new SpinnerNumberModel(3, 2, 5, 1));
    final JSpinner maxWidthSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 600, 20));
    final JCheckBox enabledBox = new JCheckBox("Enabled", true);

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Button group");
    controls.addControl("Variant", variantBox);
    controls.addControl("Selection mode", selectionBox);
    controls.addControl("Size", sizeBox);
    controls.addControl("Default shape", shapeBox);
    controls.addControl("Resize mode", resizeBox);
    controls.addControl("Connected max width", maxWidthSpinner);
    controls.addSection("Segments");
    controls.addControl("Content", contentBox);
    controls.addControl("Count", countSpinner);
    controls.addControl("Connected colour style", colorBox);
    controls.addSection("State");
    controls.addControl("", enabledBox);

    final Runnable apply =
        () -> {
          final ButtonGroupVariant variant = (ButtonGroupVariant) variantBox.getSelectedItem();
          final SelectionMode selection = (SelectionMode) selectionBox.getSelectedItem();
          final ButtonSize size = (ButtonSize) sizeBox.getSelectedItem();
          final ButtonShape shape = (ButtonShape) shapeBox.getSelectedItem();
          final ResizeMode resize = (ResizeMode) resizeBox.getSelectedItem();
          final ButtonGroupColorStyle color = (ButtonGroupColorStyle) colorBox.getSelectedItem();
          final SegmentContent content = (SegmentContent) contentBox.getSelectedItem();
          final int count = (Integer) countSpinner.getValue();
          final int maxWidth = (Integer) maxWidthSpinner.getValue();
          final boolean enabled = enabledBox.isSelected();

          // The colour style, resize mode, and max-width clamp only act on the connected variant.
          final boolean connected = variant == ButtonGroupVariant.CONNECTED;
          colorBox.setEnabled(connected);
          resizeBox.setEnabled(connected);
          maxWidthSpinner.setEnabled(connected);

          final ElwhaButtonGroup group =
              new ElwhaButtonGroup(variant)
                  .setSelectionMode(selection)
                  .setButtonSize(size)
                  .setShape(shape)
                  .setResizeMode(resize)
                  .setColorStyle(color)
                  .setMaxWidth(maxWidth);
          for (int i = 0; i < count; i++) {
            addGroupSegment(group, i, content, size);
          }
          // Seed a selection so the selected-shape inversion is visible at rest (a REQUIRED group
          // has already seeded itself).
          if (selection != SelectionMode.REQUIRED && group.getSelectedIndex() < 0) {
            group.setSelectedIndex(0);
          }
          group.setEnabled(enabled);

          workbench.setStage(stageForGroup(group, variant, resize, maxWidth));
          workbench.setCode(
              renderButtonGroupCode(
                  variant, selection, size, shape, resize, color, content, count, maxWidth,
                  enabled));
        };
    variantBox.addActionListener(event -> apply.run());
    selectionBox.addActionListener(event -> apply.run());
    sizeBox.addActionListener(event -> apply.run());
    shapeBox.addActionListener(event -> apply.run());
    resizeBox.addActionListener(event -> apply.run());
    colorBox.addActionListener(event -> apply.run());
    contentBox.addActionListener(event -> apply.run());
    countSpinner.addChangeListener(event -> apply.run());
    maxWidthSpinner.addChangeListener(event -> apply.run());
    enabledBox.addActionListener(event -> apply.run());
    apply.run();
    return workbench;
  }

  // Adds segment i to the group: a label button, an icon button, a label-and-icon button, or — in
  // MIXED mode — alternating label / icon. Icon segments carry an accessible name, since the glyph
  // alone cannot.
  private static void addGroupSegment(
      final ElwhaButtonGroup group,
      final int index,
      final SegmentContent content,
      final ButtonSize size) {
    switch (content) {
      case LABELS -> group.add(new ElwhaButton(SEGMENT_LABELS[index]));
      case ICONS -> group.add(buildIconSegment(index, size));
      case MIXED -> {
        if (index % 2 == 1) {
          group.add(buildIconSegment(index, size));
        } else {
          group.add(new ElwhaButton(SEGMENT_LABELS[index]));
        }
      }
      case TEXT_AND_ICONS ->
          group.add(
              new ElwhaButton(
                  SEGMENT_LABELS[index],
                  MaterialIcons.get(SEGMENT_ICONS[index], size.iconSizePx())));
    }
  }

  private static ElwhaIconButton buildIconSegment(final int index, final ButtonSize size) {
    final int iconPx = IconButtonSize.values()[size.ordinal()].iconPx();
    final MaterialIcons.IconPair pair = MaterialIcons.pair(SEGMENT_ICONS[index], iconPx);
    final ElwhaIconButton button = new ElwhaIconButton(pair.resting());
    button.setIcons(pair.resting(), pair.filled());
    button.setName(SEGMENT_ICONS[index]);
    return button;
  }

  // A connected FLEXIBLE group only shows its fill behavior when its parent gives it width — on the
  // centered workbench stage it would otherwise hug. Wrap it so it stretches; every other
  // combination hugs and mounts directly.
  private static JComponent stageForGroup(
      final ElwhaButtonGroup group,
      final ButtonGroupVariant variant,
      final ResizeMode resize,
      final int maxWidth) {
    if (variant != ButtonGroupVariant.CONNECTED || resize != ResizeMode.FLEXIBLE) {
      return group;
    }
    final JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.setOpaque(false);
    wrapper.add(group, BorderLayout.CENTER);
    wrapper.setPreferredSize(
        new Dimension(maxWidth > 0 ? maxWidth : 480, group.getPreferredSize().height));
    return wrapper;
  }

  private static String renderButtonGroupCode(
      final ButtonGroupVariant variant,
      final SelectionMode selection,
      final ButtonSize size,
      final ButtonShape shape,
      final ResizeMode resize,
      final ButtonGroupColorStyle color,
      final SegmentContent content,
      final int count,
      final int maxWidth,
      final boolean enabled) {
    final boolean connected = variant == ButtonGroupVariant.CONNECTED;
    final StringBuilder code = new StringBuilder(384);
    code.append("ElwhaButtonGroup group = ElwhaButtonGroup.")
        .append(connected ? "connected()" : "standard()")
        .append("\n    .setSelectionMode(SelectionMode.")
        .append(selection)
        .append(")\n    .setButtonSize(ButtonSize.")
        .append(size)
        .append(")\n    .setShape(ButtonShape.")
        .append(shape)
        .append(")");
    if (connected) {
      code.append("\n    .setResizeMode(ResizeMode.").append(resize).append(")");
      code.append("\n    .setColorStyle(ButtonGroupColorStyle.").append(color).append(")");
      if (maxWidth > 0) {
        code.append("\n    .setMaxWidth(").append(maxWidth).append(")");
      }
    }
    code.append(";");
    for (int i = 0; i < count; i++) {
      final boolean icon =
          content == SegmentContent.ICONS || (content == SegmentContent.MIXED && i % 2 == 1);
      if (content == SegmentContent.TEXT_AND_ICONS) {
        code.append("\ngroup.add(new ElwhaButton(\"")
            .append(SEGMENT_LABELS[i])
            .append("\", MaterialIcons.")
            .append(SEGMENT_ICONS[i])
            .append("()));");
      } else if (icon) {
        code.append("\ngroup.add(new ElwhaIconButton(MaterialIcons.")
            .append(SEGMENT_ICONS[i])
            .append("()));");
      } else {
        code.append("\ngroup.add(new ElwhaButton(\"").append(SEGMENT_LABELS[i]).append("\"));");
      }
    }
    if (selection != SelectionMode.REQUIRED) {
      code.append("\ngroup.setSelectedIndex(0);");
    }
    if (!enabled) {
      code.append("\ngroup.setEnabled(false);");
    }
    return code.toString();
  }

  // --- Button Group gallery sections ---

  private static JComponent buildStandardGallerySection() {
    return flowRow(
        captioned(
            "SINGLE select",
            sampleGroup(ButtonGroupVariant.STANDARD, SelectionMode.SINGLE, ButtonSize.S, 3, 1)));
  }

  private static JComponent buildColorStyleGallerySection() {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 12));
    for (final ButtonGroupColorStyle style : ButtonGroupColorStyle.values()) {
      final ElwhaButtonGroup group =
          new ElwhaButtonGroup(ButtonGroupVariant.CONNECTED)
              .setSelectionMode(SelectionMode.SINGLE)
              .setButtonSize(ButtonSize.S)
              .setColorStyle(style);
      for (int i = 0; i < 3; i++) {
        group.add(new ElwhaButton(SEGMENT_LABELS[i]));
      }
      group.setSelectedIndex(1);
      row.add(captioned(style.name(), group));
    }
    return row;
  }

  private static JComponent buildSizeGallerySection() {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 12));
    for (final ButtonSize size : new ButtonSize[] {ButtonSize.XS, ButtonSize.S, ButtonSize.M}) {
      row.add(
          captioned(
              size.name(),
              sampleGroup(ButtonGroupVariant.CONNECTED, SelectionMode.SINGLE, size, 3, 0)));
    }
    return row;
  }

  private static JComponent buildSelectionGallerySection() {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 12));
    for (final SelectionMode mode : SelectionMode.values()) {
      row.add(
          captioned(
              mode.name(), sampleGroup(ButtonGroupVariant.CONNECTED, mode, ButtonSize.S, 3, 1)));
    }
    return row;
  }

  // Builds a live sample group of label segments with one segment seeded selected.
  private static ElwhaButtonGroup sampleGroup(
      final ButtonGroupVariant variant,
      final SelectionMode mode,
      final ButtonSize size,
      final int count,
      final int seededIndex) {
    final ElwhaButtonGroup group =
        new ElwhaButtonGroup(variant).setSelectionMode(mode).setButtonSize(size);
    for (int i = 0; i < count; i++) {
      group.add(new ElwhaButton(SEGMENT_LABELS[i]));
    }
    if (mode != SelectionMode.REQUIRED) {
      group.setSelectedIndex(seededIndex);
    }
    return group;
  }

  private static JComponent flowRow(final JComponent... items) {
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 12));
    for (final JComponent item : items) {
      row.add(item);
    }
    return row;
  }

  // Stacks a small centered caption beneath a sample component.
  private static JComponent captioned(final String caption, final JComponent body) {
    final JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    body.setAlignmentX(Component.CENTER_ALIGNMENT);
    final JLabel label = new JLabel(caption);
    label.setAlignmentX(Component.CENTER_ALIGNMENT);
    label.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
    panel.add(body);
    panel.add(label);
    return panel;
  }

  private static JComponent buildSurfaceComponent() {
    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Workbench", buildSurfaceWorkbench());
    tabs.addTab(
        "Gallery",
        scroll(
            gallerySection("ColorRole × ShapeScale", SurfacePlaygroundPanels.buildMatrixPanel())));
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
    final ComponentWorkbench workbench = new ComponentWorkbench();

    final JComboBox<CardVariant> variantBox = new JComboBox<>(CardVariant.values());
    final JSpinner elevationBox =
        new JSpinner(new SpinnerNumberModel(1, 0, ElwhaCard.MAX_ELEVATION, 1));
    final JComboBox<SpaceScale> padHBox = new JComboBox<>(SpaceScale.values());
    padHBox.setSelectedItem(SpaceScale.LG);
    final JComboBox<SpaceScale> padVBox = new JComboBox<>(SpaceScale.values());
    padVBox.setSelectedItem(SpaceScale.MD);
    final JComboBox<CardMediaSlot> mediaBox = new JComboBox<>(CardMediaSlot.values());
    final JCheckBox headerBox = new JCheckBox("Header (title + subtitle)", true);
    final JComboBox<CardHeaderLeading> headerLeadingBox =
        new JComboBox<>(CardHeaderLeading.values());
    final JCheckBox bodyBox = new JCheckBox("Supporting text", true);
    final JCheckBox dividerBox = new JCheckBox("Divider");
    final JCheckBox actionsBox = new JCheckBox("Actions row");
    final JCheckBox actionableBox = new JCheckBox("Actionable");
    final JCheckBox selectableBox = new JCheckBox("Selectable");
    final JCheckBox selectedBox = new JCheckBox("Selected");
    final JCheckBox collapsibleBox = new JCheckBox("Collapsible");
    final JCheckBox collapsedBox = new JCheckBox("Collapsed");
    final JCheckBox animateBox = new JCheckBox("Animate collapse", true);
    final JComboBox<ExpansionOverflow> overflowBox = new JComboBox<>(ExpansionOverflow.values());
    final JCheckBox enabledBox = new JCheckBox("Enabled", true);

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Card");
    controls.addControl("Variant", variantBox);
    controls.addControl("Elevation", elevationBox);
    controls.addControl("Padding — horizontal", padHBox);
    controls.addControl("Padding — vertical", padVBox);
    controls.addSection("Slots");
    controls.addControl("Media", mediaBox);
    controls.addControl("", headerBox);
    controls.addControl("Header leading", headerLeadingBox);
    controls.addControl("", bodyBox);
    controls.addControl("", dividerBox);
    controls.addControl("", actionsBox);
    controls.addSection("Interaction");
    controls.addControl("", actionableBox);
    controls.addControl("", selectableBox);
    controls.addControl("", selectedBox);
    controls.addControl("", collapsibleBox);
    controls.addControl("", collapsedBox);
    controls.addControl("", animateBox);
    controls.addControl("Expansion overflow", overflowBox);
    controls.addSection("State");
    controls.addControl("", enabledBox);

    final Runnable apply =
        () -> {
          headerLeadingBox.setEnabled(headerBox.isSelected());
          selectedBox.setEnabled(selectableBox.isSelected());
          collapsedBox.setEnabled(collapsibleBox.isSelected());
          animateBox.setEnabled(collapsibleBox.isSelected());
          final CardConfig cfg =
              new CardConfig(
                  (CardVariant) variantBox.getSelectedItem(),
                  (Integer) elevationBox.getValue(),
                  (SpaceScale) padHBox.getSelectedItem(),
                  (SpaceScale) padVBox.getSelectedItem(),
                  (CardMediaSlot) mediaBox.getSelectedItem(),
                  headerBox.isSelected(),
                  (CardHeaderLeading) headerLeadingBox.getSelectedItem(),
                  bodyBox.isSelected(),
                  dividerBox.isSelected(),
                  actionsBox.isSelected(),
                  actionableBox.isSelected(),
                  selectableBox.isSelected(),
                  selectedBox.isSelected(),
                  collapsibleBox.isSelected(),
                  collapsedBox.isSelected(),
                  animateBox.isSelected(),
                  (ExpansionOverflow) overflowBox.getSelectedItem(),
                  enabledBox.isSelected());
          workbench.setStage(buildCard(cfg));
          workbench.setCode(renderCardCode(cfg));
        };
    variantBox.addActionListener(event -> apply.run());
    elevationBox.addChangeListener(event -> apply.run());
    padHBox.addActionListener(event -> apply.run());
    padVBox.addActionListener(event -> apply.run());
    mediaBox.addActionListener(event -> apply.run());
    headerBox.addActionListener(event -> apply.run());
    headerLeadingBox.addActionListener(event -> apply.run());
    bodyBox.addActionListener(event -> apply.run());
    dividerBox.addActionListener(event -> apply.run());
    actionsBox.addActionListener(event -> apply.run());
    actionableBox.addActionListener(event -> apply.run());
    selectableBox.addActionListener(event -> apply.run());
    selectedBox.addActionListener(event -> apply.run());
    collapsibleBox.addActionListener(event -> apply.run());
    collapsedBox.addActionListener(event -> apply.run());
    animateBox.addActionListener(event -> apply.run());
    overflowBox.addActionListener(event -> apply.run());
    enabledBox.addActionListener(event -> apply.run());
    apply.run();
    return workbench;
  }

  private static ElwhaCard buildCard(final CardConfig cfg) {
    final ElwhaCard card =
        switch (cfg.variant()) {
          case FILLED -> ElwhaCard.filledCard();
          case OUTLINED -> ElwhaCard.outlinedCard();
          default -> ElwhaCard.elevatedCard();
        };
    card.setElevation(cfg.elevation());
    card.setPadding(cfg.padH(), cfg.padV());

    if (cfg.media() == CardMediaSlot.IMAGE) {
      card.add(ElwhaCardMedia.image(demoImage()));
    } else if (cfg.media() == CardMediaSlot.RENDERED) {
      card.add(ElwhaCardMedia.painter(ElwhaShowcase::paintDemoMedia));
    }

    ElwhaCardHeader header = null;
    if (cfg.header()) {
      header = new ElwhaCardHeader().setTitle("Card title").setSubtitle("Supporting subtitle");
      if (cfg.headerLeading() == CardHeaderLeading.ICON) {
        header.setLeading(new ElwhaCardLeadingIcon(MaterialIcons.star(24)));
      } else if (cfg.headerLeading() == CardHeaderLeading.AVATAR) {
        header.setLeading(new ElwhaCardThumbnail(demoImage()).setShape(ThumbnailShape.CIRCULAR));
      }
      card.add(header);
    }
    if (cfg.body()) {
      card.add(
          new ElwhaCardSupportingText(
              "Supporting text carries the card's detail and wraps to the card width."));
    }
    if (cfg.divider()) {
      card.add(new ElwhaCardDivider());
    }
    if (cfg.actions()) {
      card.add(
          new ElwhaCardActions()
              .addTrailing(ElwhaButton.textButton("Dismiss"))
              .addTrailing(ElwhaButton.filledButton("Confirm")));
    }

    card.setActionable(cfg.actionable());
    card.setSelectable(cfg.selectable());
    card.setExpansionOverflow(cfg.overflow());
    card.setAnimateCollapse(cfg.animate());
    if (cfg.collapsible()) {
      card.setCollapsible(true);
      if (header != null) {
        header.addTrailing(new ElwhaCardChevron(card));
        card.setCollapseConstraint(header, CollapseRule.ALWAYS_VISIBLE);
      } else {
        card.add(new ElwhaCardExpandLink(card, "Show more", "Show less"));
      }
      card.setCollapsed(cfg.collapsed());
    }
    if (cfg.selectable()) {
      card.setSelected(cfg.selected());
    }
    card.setEnabled(cfg.enabled());
    return card;
  }

  private static String renderCardCode(final CardConfig cfg) {
    final String factory =
        switch (cfg.variant()) {
          case FILLED -> "filledCard";
          case OUTLINED -> "outlinedCard";
          default -> "elevatedCard";
        };
    final StringBuilder code = new StringBuilder(640);
    code.append("ElwhaCard card = ElwhaCard.").append(factory).append("();\n");
    code.append("card.setElevation(").append(cfg.elevation()).append(");\n");
    code.append("card.setPadding(SpaceScale.")
        .append(cfg.padH())
        .append(", SpaceScale.")
        .append(cfg.padV())
        .append(");\n");
    if (cfg.media() == CardMediaSlot.IMAGE) {
      code.append("card.add(ElwhaCardMedia.image(image));\n");
    } else if (cfg.media() == CardMediaSlot.RENDERED) {
      code.append("card.add(ElwhaCardMedia.painter((g, w, h) -> g.fillRect(0, 0, w, h)));\n");
    }
    if (cfg.header()) {
      code.append("ElwhaCardHeader header = new ElwhaCardHeader()\n");
      code.append("    .setTitle(\"Card title\")\n");
      code.append("    .setSubtitle(\"Supporting subtitle\");\n");
      if (cfg.headerLeading() == CardHeaderLeading.ICON) {
        code.append("header.setLeading(new ElwhaCardLeadingIcon(MaterialIcons.star(24)));\n");
      } else if (cfg.headerLeading() == CardHeaderLeading.AVATAR) {
        code.append(
            "header.setLeading("
                + "new ElwhaCardThumbnail(image).setShape(ThumbnailShape.CIRCULAR));\n");
      }
      code.append("card.add(header);\n");
    }
    if (cfg.body()) {
      code.append("card.add(new ElwhaCardSupportingText(\"Supporting text…\"));\n");
    }
    if (cfg.divider()) {
      code.append("card.add(new ElwhaCardDivider());\n");
    }
    if (cfg.actions()) {
      code.append("card.add(new ElwhaCardActions()\n");
      code.append("    .addTrailing(ElwhaButton.textButton(\"Dismiss\"))\n");
      code.append("    .addTrailing(ElwhaButton.filledButton(\"Confirm\")));\n");
    }
    if (cfg.actionable()) {
      code.append("card.setActionable(true);\n");
    }
    if (cfg.selectable()) {
      code.append("card.setSelectable(true);\n");
    }
    if (cfg.overflow() != ExpansionOverflow.GROW) {
      code.append("card.setExpansionOverflow(ExpansionOverflow.")
          .append(cfg.overflow())
          .append(");\n");
    }
    if (cfg.collapsible()) {
      code.append("card.setCollapsible(true);\n");
      if (cfg.header()) {
        code.append("header.addTrailing(new ElwhaCardChevron(card));\n");
      } else {
        code.append("card.add(new ElwhaCardExpandLink(card, \"Show more\", \"Show less\"));\n");
      }
      if (!cfg.animate()) {
        code.append("card.setAnimateCollapse(false);\n");
      }
      if (cfg.collapsed()) {
        code.append("card.setCollapsed(true);\n");
      }
    }
    if (cfg.selectable() && cfg.selected()) {
      code.append("card.setSelected(true);\n");
    }
    if (!cfg.enabled()) {
      code.append("card.setEnabled(false);\n");
    }
    if (code.length() > 0 && code.charAt(code.length() - 1) == '\n') {
      code.setLength(code.length() - 1);
    }
    return code.toString();
  }

  // A demo raster for the Card Workbench's image-media and avatar-thumbnail slots — the library
  // ships no sample imagery, so the workbench paints its own.
  private static Image demoImage() {
    final BufferedImage image = new BufferedImage(480, 270, BufferedImage.TYPE_INT_RGB);
    final Graphics2D g = image.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setPaint(new GradientPaint(0, 0, new Color(0x33568C), 480, 270, new Color(0x8C5A8C)));
    g.fillRect(0, 0, 480, 270);
    g.dispose();
    return image;
  }

  private static void paintDemoMedia(final Graphics2D g, final int width, final int height) {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setPaint(new GradientPaint(0, 0, new Color(0x2E7D6B), width, height, new Color(0x7FB8A8)));
    g.fillRect(0, 0, width, height);
  }

  /** The Card Workbench's media-slot option. */
  private enum CardMediaSlot {
    NONE,
    IMAGE,
    RENDERED
  }

  /** The Card Workbench's header leading-slot option. */
  private enum CardHeaderLeading {
    NONE,
    ICON,
    AVATAR
  }

  /** The full Card Workbench configuration — read from the controls, consumed by builder + code. */
  private record CardConfig(
      CardVariant variant,
      int elevation,
      SpaceScale padH,
      SpaceScale padV,
      CardMediaSlot media,
      boolean header,
      CardHeaderLeading headerLeading,
      boolean body,
      boolean divider,
      boolean actions,
      boolean actionable,
      boolean selectable,
      boolean selected,
      boolean collapsible,
      boolean collapsed,
      boolean animate,
      ExpansionOverflow overflow,
      boolean enabled) {}

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
