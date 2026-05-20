package com.owspfm.elwha.showcase;

import com.owspfm.elwha.button.playground.ButtonPlaygroundPanels;
import com.owspfm.elwha.card.playground.CursorReferencePanel;
import com.owspfm.elwha.card.playground.ElwhaCardListShowcase;
import com.owspfm.elwha.card.playground.GalleryPanel;
import com.owspfm.elwha.card.playground.LiveConfigPanel;
import com.owspfm.elwha.card.playground.SnippetPanel;
import com.owspfm.elwha.chip.playground.ChipPlaygroundPanels;
import com.owspfm.elwha.iconbutton.playground.IconButtonPlaygroundPanels;
import com.owspfm.elwha.surface.playground.SurfacePlaygroundPanels;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.playground.FoundationsPanels;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JTree;
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
 * <p>Every panel is composed from the existing factored playground builders ({@code
 * ButtonPlaygroundPanels} and friends) so the Showcase and the standalone playgrounds never drift.
 * This is the Story-2 <em>shell</em> of epic #130: it reorganises what exists into the new
 * information architecture without refining the panels themselves. The locked design is {@code
 * docs/research/elwha-showcase-design.md}.
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

    statusLabel = new JLabel();
    bar.add(Box.createHorizontalStrut(16));
    bar.add(statusLabel);
    updateStatus();
    return bar;
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
    addLeaf(containers, "Button Group", scroll(ButtonPlaygroundPanels.buildTogglesPanel()));
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
    tabs.addTab("Workbench", scroll(ButtonPlaygroundPanels.buildLivePanel()));
    tabs.addTab(
        "Gallery",
        scroll(
            stack(
                ButtonPlaygroundPanels.buildVariantGalleryPanel(),
                ButtonPlaygroundPanels.buildSizesPanel())));
    return tabs;
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
    tabs.addTab("Workbench", scroll(SurfacePlaygroundPanels.buildLivePanel()));
    tabs.addTab("Gallery", scroll(SurfacePlaygroundPanels.buildMatrixPanel()));
    return tabs;
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
