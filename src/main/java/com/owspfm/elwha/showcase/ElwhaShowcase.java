package com.owspfm.elwha.showcase;

import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.badge.ElwhaBadgeAnchor;
import com.owspfm.elwha.badge.playground.BadgePlaygroundPanels;
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
import com.owspfm.elwha.dialog.ElwhaDialog;
import com.owspfm.elwha.dialog.ElwhaFullScreenDialog;
import com.owspfm.elwha.fab.ElwhaFab;
import com.owspfm.elwha.fab.ElwhaFabAnchor;
import com.owspfm.elwha.fab.playground.FabPlaygroundPanels;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.iconbutton.IconButtonGroup;
import com.owspfm.elwha.iconbutton.IconButtonInteractionMode;
import com.owspfm.elwha.iconbutton.IconButtonSize;
import com.owspfm.elwha.iconbutton.IconButtonVariant;
import com.owspfm.elwha.iconbutton.playground.IconButtonPlaygroundPanels;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.navrail.ElwhaNavRailDestination;
import com.owspfm.elwha.navrail.ElwhaNavigationRail;
import com.owspfm.elwha.navrail.playground.NavRailDestinationPlaygroundPanels;
import com.owspfm.elwha.navrail.playground.NavigationRailPlaygroundPanels;
import com.owspfm.elwha.surface.playground.SurfacePlaygroundPanels;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.CornerRadii;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.SpaceScale;
import com.owspfm.elwha.theme.Theme;
import com.owspfm.elwha.theme.playground.FoundationsPanels;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

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
 * @version v0.4.0
 * @since v0.3.0
 */
public final class ElwhaShowcase {

  private static final int RAIL_COLLAPSED_WIDTH = 96;
  private static final String HOME_KEY = "__landing_home";
  private static final String FOUNDATIONS_KEY = "__landing_foundations";
  private static final String COMPONENTS_KEY = "__landing_components";
  private static final String CONTAINERS_KEY = "__landing_containers";
  private static final String AREA_FOUNDATIONS = "Foundations";
  private static final String AREA_COMPONENTS = "Components";
  private static final String AREA_CONTAINERS = "Containers";
  // Maximum width of the landing-page card grid. Caps cards at ~320 dp each (960 / 3) so the
  // grid reads as a dashboard tile-row rather than stretching cards to the full frame width.
  private static final int MAX_GRID_WIDTH = 960;

  private final List<Runnable> tokenRefreshers = new ArrayList<>();
  private final JPanel content = new JPanel(new CardLayout());
  private final List<Theme> primaryThemes = MaterialPalettes.primary();
  private final List<Theme> secondaryThemes = MaterialPalettes.secondary();
  // Insertion-ordered so Home and area landings list leaves in the same order they appear in the
  // area sections (which is also the rail's primary-destination order).
  private final Map<String, LeafEntry> leaves = new LinkedHashMap<>();
  private JLabel statusLabel;
  private JComboBox<Theme> palettePicker;
  private Theme primarySelection;
  private Theme secondarySelection;
  private boolean secondaryTier;
  private boolean pickerAdjusting;
  private ElwhaNavigationRail rail;
  private ElwhaFabAnchor floatingFabAnchor;
  private ElwhaNavRailDestination foundationsPrim;
  private ElwhaNavRailDestination componentsPrim;
  private ElwhaNavRailDestination containersPrim;
  private ElwhaButtonGroup modeToggle;

  /**
   * Catalog entry for one Showcase leaf — what every landing-page card renders and what the
   * back-affordance returns to. Holds the leaf's display label, supporting copy, parent area, and
   * the realised JComponent surface that goes into the CardLayout.
   */
  private static final class LeafEntry {
    final String label;
    final String supporting;
    final String area;
    final JComponent surface;

    LeafEntry(
        final String label, final String supporting, final String area, final JComponent surface) {
      this.label = label;
      this.supporting = supporting;
      this.area = area;
      this.surface = surface;
    }
  }

  // Vertical-only scrollable page used for landing surfaces. Tracks the JScrollPane viewport
  // width (instead of the page's natural preferred width) so the embedded grid never pushes
  // horizontal scroll — the grid itself has a bounded max-width, and the BoxLayout column aligns
  // it to the leading edge while padding any remaining horizontal space.
  private static final class LandingPage extends JPanel implements Scrollable {
    LandingPage() {
      setOpaque(false);
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
      return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(
        final Rectangle visibleRect, final int orientation, final int direction) {
      return 16;
    }

    @Override
    public int getScrollableBlockIncrement(
        final Rectangle visibleRect, final int orientation, final int direction) {
      return 96;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
      return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
      return false;
    }
  }

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

    // The rail spans the full window height on the layered pane, so the header bar lives INSIDE
    // the content area (above the CardLayout content) rather than across the top of the frame.
    // Without this, the header would extend across the rail's leading column and the rail would
    // read as a sidebar pocket rather than a real-app shell. Content pane gets a 96-dp leading
    // inset matching the Collapsed rail width; the Expanded morph overlays into the inset area
    // without reflowing content. Mirrors the FAB Phase 5 layered-pane recipe (#206) at a
    // structural level.
    final JPanel contentWrapper = new JPanel(new BorderLayout());
    contentWrapper.setBorder(BorderFactory.createEmptyBorder(0, RAIL_COLLAPSED_WIDTH, 0, 0));
    contentWrapper.add(buildHeaderBar(), BorderLayout.NORTH);
    contentWrapper.add(content, BorderLayout.CENTER);

    final JPanel root = new JPanel(new BorderLayout());
    root.add(contentWrapper, BorderLayout.CENTER);

    // Populate the catalog of leaves + build all CardLayout cards (4 landings + 17 wrapped
    // leaves). Must run before the rail is built so the rail's primary action listeners can
    // resolve landing keys that already have cards registered.
    populateCatalog();
    populateLandingCards();
    populateLeafCards();

    rail = buildShowcaseRail();
    mountRailOnLayeredPane(frame, rail);

    floatingFabAnchor = buildFloatingFabAnchor(root);
    frame.setContentPane(floatingFabAnchor);
    frame.setSize(1320, 860);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
    // Initial card: the Foundations landing (rail's first primary is selected by default).
    showCard(FOUNDATIONS_KEY);
    // Park initial focus on the currently-selected mode segment in the header. The rail's first
    // destination would otherwise auto-claim focus on startup — visually that reads as a
    // navigation prompt before the user has done anything, when the friendlier landing point is
    // a header control the user can immediately act on with arrow keys.
    SwingUtilities.invokeLater(
        () -> {
          final int idx = modeToggle.getSelectedIndex();
          if (idx >= 0) {
            modeToggle.getButtonAt(idx).requestFocusInWindow();
          }
        });
  }

  // Mounts the ElwhaNavigationRail on the frame's JLayeredPane at PALETTE_LAYER, leading edge,
  // full layered-pane height (the header bar lives inside the content area, not above the rail —
  // see buildAndShow). Position is recomputed on layered-pane resize and on every variant morph
  // tick (during the Collapsed↔Expanded morph the rail's preferred width lerps, but the layered
  // pane uses absolute positioning so the parent never auto-relayouts — a 60 Hz Swing Timer keyed
  // to isMorphing() polls and re-setBounds until the morph settles).
  private void mountRailOnLayeredPane(final JFrame frame, final ElwhaNavigationRail target) {
    final JLayeredPane layeredPane = frame.getLayeredPane();
    layeredPane.add(target, JLayeredPane.PALETTE_LAYER);

    // Reserve trailing-edge clearance for the rail's elevation drop shadow — when elevation > 0,
    // the rail's ShadowPainter halo extends outward from the body silhouette. Bounds are widened
    // by trailingShadowReserve() so the halo lands cleanly on the layered pane behind the content
    // area, not clipping against the rail's right edge. The content area's leading inset (= rail
    // body width) is unchanged: the halo overlays the content's leading edge, which is exactly
    // M3's "rail elevated above content" read.
    final Runnable position =
        () -> {
          final Dimension pref = target.getPreferredSize();
          final int reserve = target.trailingShadowReserve();
          final int boundsW = pref.width + reserve;
          final boolean ltr = layeredPane.getComponentOrientation().isLeftToRight();
          final int x = ltr ? 0 : layeredPane.getWidth() - boundsW;
          target.setBounds(x, 0, boundsW, layeredPane.getHeight());
        };
    layeredPane.addComponentListener(
        new ComponentAdapter() {
          @Override
          public void componentResized(final ComponentEvent event) {
            position.run();
          }
        });

    final Timer morphTracker =
        new Timer(
            16,
            e -> {
              position.run();
              if (!target.isMorphing()) {
                ((Timer) e.getSource()).stop();
              }
            });
    morphTracker.setCoalesce(true);
    target.addPropertyChangeListener(
        ElwhaNavigationRail.PROPERTY_VARIANT, e -> morphTracker.restart());
    position.run();
  }

  // Wraps the Showcase content in an ElwhaFabAnchor (#205) so a real floating ElwhaFab sits
  // bottom-trailing above every tab — 16 dp off the visible-body edge, RTL-aware, re-pinned on
  // resize — all owned by the primitive instead of the hand-rolled JLayeredPane glue this method
  // used to carry. Click navigates to the FAB leaf; the floating instance is the Showcase's
  // self-demonstration of ElwhaFabAnchor. Co-exists with the navigation rail, which floats at the
  // leading edge of the frame's own layered pane (a different pane), so the two never contend.
  private ElwhaFabAnchor buildFloatingFabAnchor(final JComponent content) {
    final ElwhaFab floatingFab =
        ElwhaFab.extended(MaterialIcons.editFilled(ElwhaFab.Size.SMALL.iconPx()), "FAB Workbench");
    floatingFab.setToolTipText(
        "Floating ElwhaFab — click to open the FAB Workbench. Placed by ElwhaFabAnchor (#205).");
    floatingFab.addActionListener(event -> showCard("FAB"));
    return new ElwhaFabAnchor(content, floatingFab);
  }

  // Swap the CardLayout to the named card and re-sync the rail's selected primary to the area
  // that hosts this card. Programmatic setSelected on the rail does NOT fire its action listener,
  // so calling this from inside a rail action listener can't re-enter.
  private void showCard(final String key) {
    ((CardLayout) content.getLayout()).show(content, key);
    final ElwhaNavRailDestination targetPrim = primaryForKey(key);
    if (targetPrim != null && rail != null && rail.getSelected() != targetPrim) {
      rail.setSelected(targetPrim);
    }
    updateFabScrollSource();
  }

  // Point the floating FAB's SHRINK response at the newly-shown card's scroll region (#274). The
  // content is a CardLayout, so the scrollable region is the active card, not the anchor's content;
  // re-target on every card swap. Cycling the response through NONE first restores the FAB to
  // Extended so a freshly-shown card always starts un-shrunk (setScrollSource alone would carry the
  // prior card's shrunk state). Cards without a scroll pane leave the FAB Extended and inert.
  private void updateFabScrollSource() {
    if (floatingFabAnchor == null) {
      return;
    }
    final JScrollPane source = activeCardScrollPane();
    floatingFabAnchor.setScrollResponse(ElwhaFabAnchor.ScrollResponse.NONE);
    floatingFabAnchor.setScrollSource(source);
    if (source != null) {
      floatingFabAnchor.setScrollResponse(ElwhaFabAnchor.ScrollResponse.SHRINK);
    }
  }

  private JScrollPane activeCardScrollPane() {
    for (final Component card : content.getComponents()) {
      if (card.isVisible()) {
        return findScrollPane(card);
      }
    }
    return null;
  }

  private static JScrollPane findScrollPane(final Component component) {
    if (component instanceof JScrollPane scrollPane) {
      return scrollPane;
    }
    if (component instanceof Container container) {
      for (final Component child : container.getComponents()) {
        final JScrollPane found = findScrollPane(child);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private ElwhaNavRailDestination primaryForKey(final String key) {
    if (FOUNDATIONS_KEY.equals(key)) {
      return foundationsPrim;
    }
    if (COMPONENTS_KEY.equals(key)) {
      return componentsPrim;
    }
    if (CONTAINERS_KEY.equals(key)) {
      return containersPrim;
    }
    final LeafEntry entry = leaves.get(key);
    if (entry == null) {
      return null;
    }
    return switch (entry.area) {
      case AREA_FOUNDATIONS -> foundationsPrim;
      case AREA_COMPONENTS -> componentsPrim;
      case AREA_CONTAINERS -> containersPrim;
      default -> null;
    };
  }

  // --- header bar ---

  private JComponent buildHeaderBar() {
    final JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));

    // No "Mode:" label — the sun / moon / auto icons are self-evident.
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
    modeToggle = toggle;
    return toggle;
  }

  // The primary / secondary palette-tier toggle — an ElwhaButtonGroup connected group of text +
  // icon buttons; the leading icon swaps outline → filled on selection via ElwhaButton.setIcons.
  private JComponent buildTierToggle() {
    final int iconPx = ButtonSize.XS.iconSizePx();
    final ElwhaButton primary =
        new ElwhaButton("Primary")
            .setIcons(MaterialIcons.palette(iconPx), MaterialIcons.paletteFilled(iconPx));
    final ElwhaButton secondary =
        new ElwhaButton("Secondary")
            .setIcons(MaterialIcons.colorize(iconPx), MaterialIcons.colorizeFilled(iconPx));
    final ElwhaButtonGroup toggle =
        ElwhaButtonGroup.connected()
            .setSelectionMode(SelectionMode.REQUIRED)
            .setButtonSize(ButtonSize.XS)
            .setResizeMode(ResizeMode.FIXED)
            .setColorStyle(ButtonGroupColorStyle.TONAL)
            .add(primary)
            .add(secondary);
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

  // --- navigation rail + content cards ---

  // Catalog of every Showcase leaf — label, supporting blurb, area, and the realised surface
  // component. The order here is the order Home + the area landings render their cards in, and
  // is also the order the Components/Containers area destinations are traversed from. Foundations
  // appears first per the rail's first-primary-selected-by-default contract — see buildAndShow.
  private void populateCatalog() {
    register(
        new LeafEntry(
            "Color Roles",
            "Material 3 color roles, swatched and live-wired to the active palette.",
            AREA_FOUNDATIONS,
            scroll(FoundationsPanels.buildColorRoles(tokenRefreshers))));
    register(
        new LeafEntry(
            "Type Scale",
            "The full M3 typography scale rendered in the bundled Inter type-face.",
            AREA_FOUNDATIONS,
            scroll(FoundationsPanels.buildTypeScale(tokenRefreshers))));
    register(
        new LeafEntry(
            "Icons",
            "Every bundled Material Symbol, themed and labelled with its MaterialIcons factory"
                + " call.",
            AREA_FOUNDATIONS,
            scroll(FoundationsPanels.buildIconGallery(tokenRefreshers))));
    register(
        new LeafEntry(
            "Swing Comps",
            "Raw Swing primitives — sanity-check the FlatLaf theme under Elwha tokens.",
            AREA_FOUNDATIONS,
            scroll(FoundationsPanels.buildSwingComps(tokenRefreshers))));

    register(
        new LeafEntry(
            "Button",
            "The M3 Expressive button — five variants, four sizes, the morph kit, all knobs.",
            AREA_COMPONENTS,
            buildButtonComponent()));
    register(
        new LeafEntry(
            "Chip",
            "Assist / filter / input / suggestion chips with trailing-slot variants.",
            AREA_COMPONENTS,
            buildChipComponent()));
    register(
        new LeafEntry(
            "Icon Button",
            "Icon-only buttons with the same variant + interaction-mode contract as Button.",
            AREA_COMPONENTS,
            buildIconButtonComponent()));
    register(
        new LeafEntry(
            "FAB",
            "Floating Action Button — Standard ↔ Extended morph, three sizes, four colors.",
            AREA_COMPONENTS,
            buildFabComponent()));
    register(
        new LeafEntry(
            "Badge",
            "M3 Badge primitive + anchor — dot, count, and label forms on any icon-bearing host.",
            AREA_COMPONENTS,
            buildBadgeComponent()));
    register(
        new LeafEntry(
            "Nav Rail Destination",
            "The rail's destination atom — Collapsed / Expanded layouts and badge slot.",
            AREA_COMPONENTS,
            buildNavRailDestinationComponent()));
    register(
        new LeafEntry(
            "Navigation Rail",
            "The full rail container — chrome slots, sections, Collapsed ↔ Expanded morph.",
            AREA_COMPONENTS,
            buildNavigationRailComponent()));
    register(
        new LeafEntry(
            "Button Group",
            "M3 segmented + connected button groups with single / required / multi selection.",
            AREA_COMPONENTS,
            buildButtonGroupComponent()));
    register(
        new LeafEntry(
            "Card",
            "ElwhaCard variants and composition primitives (header, body, actions, media).",
            AREA_COMPONENTS,
            buildCardComponent()));
    register(
        new LeafEntry(
            "Surface",
            "The token-driven surface primitive underpinning every chrome component.",
            AREA_COMPONENTS,
            buildSurfaceComponent()));
    register(
        new LeafEntry(
            "Dialog",
            "M3 Basic Dialog — modal overlay with icon / headline / supporting / content /"
                + " actions.",
            AREA_COMPONENTS,
            buildDialogComponent()));

    register(
        new LeafEntry(
            "Chip List",
            "ElwhaChipList — orientation, selection model, drag-reorder, tab-strip semantics.",
            AREA_CONTAINERS,
            new ChipListContainer().component()));
    register(
        new LeafEntry(
            "Card List",
            "ElwhaCardList — vertical and horizontal card containers with selection + reorder.",
            AREA_CONTAINERS,
            new CardListContainer().component()));
    register(
        new LeafEntry(
            "Button Group (mutex)",
            "Mutually-exclusive button-group demo — radio-style selection across N segments.",
            AREA_CONTAINERS,
            buildButtonGroupContainer()));
    register(
        new LeafEntry(
            "Icon Button Group (mutex)",
            "Icon-only mutex group — toolbar-style segmented control with single selection.",
            AREA_CONTAINERS,
            buildIconButtonGroupContainer()));
  }

  private void register(final LeafEntry entry) {
    leaves.put(entry.label, entry);
  }

  // The four landing cards, populated into the CardLayout content panel. Each is a grid of
  // ElwhaCards — title + supporting text + an actionable click that routes to the leaf surface.
  // Home is the master index (all 17 leaves, grouped by area heading); each area landing covers
  // just its own leaves. ElwhaCard's actionable mode is the entire raison-d'être here: the cards
  // are the navigation surface, not decoration.
  private void populateLandingCards() {
    content.add(buildHomeLanding(), HOME_KEY);
    content.add(buildAreaLanding(AREA_FOUNDATIONS), FOUNDATIONS_KEY);
    content.add(buildAreaLanding(AREA_COMPONENTS), COMPONENTS_KEY);
    content.add(buildAreaLanding(AREA_CONTAINERS), CONTAINERS_KEY);
  }

  // Wraps each leaf surface with a leading-edge "← {area}" back affordance and registers it in
  // the CardLayout under the leaf label. The back row is a thin BorderLayout NORTH strip; the
  // leaf surface fills CENTER unchanged.
  private void populateLeafCards() {
    for (final LeafEntry entry : leaves.values()) {
      content.add(withBackToLanding(entry.area, entry.surface), entry.label);
    }
  }

  private JComponent buildHomeLanding() {
    final LandingPage page = new LandingPage();
    page.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    page.add(landingTitle("Welcome", "Every Elwha surface on one page. Pick anywhere to dive in."));
    page.add(Box.createVerticalStrut(16));
    for (final String area : List.of(AREA_FOUNDATIONS, AREA_COMPONENTS, AREA_CONTAINERS)) {
      page.add(sectionHeading(area));
      page.add(Box.createVerticalStrut(8));
      page.add(landingGrid(leavesIn(area)));
      page.add(Box.createVerticalStrut(20));
    }
    return scroll(page);
  }

  private JComponent buildAreaLanding(final String area) {
    final LandingPage page = new LandingPage();
    page.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    page.add(landingTitle(area, areaBlurb(area)));
    page.add(Box.createVerticalStrut(16));
    page.add(landingGrid(leavesIn(area)));
    return scroll(page);
  }

  private static String areaBlurb(final String area) {
    return switch (area) {
      case AREA_FOUNDATIONS ->
          "The design-token foundation — color, type, and the raw-Swing baseline.";
      case AREA_COMPONENTS ->
          "The single-instance surfaces — every Elwha primitive in its own Workbench + Gallery.";
      case AREA_CONTAINERS ->
          "The multi-instance surfaces — lists, groups, and the composition demos.";
      default -> "";
    };
  }

  private List<LeafEntry> leavesIn(final String area) {
    final List<LeafEntry> out = new ArrayList<>();
    for (final LeafEntry entry : leaves.values()) {
      if (entry.area.equals(area)) {
        out.add(entry);
      }
    }
    // Alpha-sort within each area so landing-page cards read left-to-right, top-down by label.
    out.sort(Comparator.comparing(e -> e.label));
    return out;
  }

  // A 3-column GridLayout grid of leaf cards, capped at MAX_GRID_WIDTH so individual cards stay
  // at a dashboard-tile measure (~320 dp each) instead of stretching to the frame width.
  // GridLayout divides its container width evenly across columns — so capping container.maxSize
  // is enough; the BoxLayout column hosting the grid respects the max and aligns the grid
  // leading-edge. ElwhaCardSupportingText is HTML-auto-wrapping so the cards' supporting copy
  // reflows naturally at the chosen card width.
  private JComponent landingGrid(final List<LeafEntry> entries) {
    final JPanel grid = new JPanel(new GridLayout(0, 3, 12, 12));
    grid.setOpaque(false);
    grid.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    grid.setMaximumSize(new Dimension(MAX_GRID_WIDTH, Integer.MAX_VALUE));
    for (final LeafEntry entry : entries) {
      grid.add(leafCard(entry));
    }
    return grid;
  }

  private ElwhaCard leafCard(final LeafEntry entry) {
    final ElwhaCard card = ElwhaCard.elevatedCard().setActionable(true);
    card.add(new ElwhaCardHeader().setTitle(entry.label));
    card.add(new ElwhaCardDivider());
    card.add(new ElwhaCardSupportingText(entry.supporting));
    card.setToolTipText("Open " + entry.label);
    card.addActionListener(e -> showCard(entry.label));
    return card;
  }

  private static JComponent landingTitle(final String title, final String subtitle) {
    final JPanel head = new JPanel();
    head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
    head.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    head.setOpaque(false);
    final JLabel titleLabel = new JLabel(title);
    titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 26f));
    titleLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    head.add(titleLabel);
    final JLabel subtitleLabel = new JLabel(subtitle);
    subtitleLabel.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
    subtitleLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
    subtitleLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    head.add(subtitleLabel);
    return head;
  }

  private static JComponent sectionHeading(final String text) {
    final JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(Font.BOLD, 16f));
    label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    label.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
    return label;
  }

  // Wraps a leaf surface with a leading "← {area}" back-button strip. Routes the click back to
  // the leaf's parent landing — substitute for a breadcrumbs primitive we don't have yet. The
  // back row is intentionally a thin FlowLayout strip so the leaf's existing chrome (the
  // Workbench / Gallery JTabbedPane on most leaves) sits visually adjacent to it.
  private JComponent withBackToLanding(final String area, final JComponent leafSurface) {
    final JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.setOpaque(false);

    final JPanel backRow = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 6));
    backRow.setOpaque(false);
    final ElwhaButton back = ElwhaButton.textButton("← " + area);
    back.setToolTipText("Back to " + area);
    back.addActionListener(e -> showCard(landingKeyFor(area)));
    backRow.add(back);

    wrapper.add(backRow, BorderLayout.NORTH);
    wrapper.add(leafSurface, BorderLayout.CENTER);
    return wrapper;
  }

  private static String landingKeyFor(final String area) {
    return switch (area) {
      case AREA_FOUNDATIONS -> FOUNDATIONS_KEY;
      case AREA_COMPONENTS -> COMPONENTS_KEY;
      case AREA_CONTAINERS -> CONTAINERS_KEY;
      default -> HOME_KEY;
    };
  }

  // Builds the Showcase's navigation rail — 3 primary destinations (one per area) wired to area
  // landings, plus an extended FAB linking to the Home master-index landing. Both the rail-FAB
  // and the bottom-trailing floating FAB share the layered pane; together they demonstrate two
  // distinct M3 FAB placement modes (chrome slot + true floating anchor) on a single frame.
  private ElwhaNavigationRail buildShowcaseRail() {
    final ElwhaNavigationRail target = ElwhaNavigationRail.collapsed();
    target.getAccessibleContext().setAccessibleName("Showcase navigation rail");
    target.setSurfaceFilled(true);
    target.setElevation(1);
    target.setMenuButton(new ElwhaIconButton(MaterialIcons.menu()));

    final ElwhaFab homeFab =
        ElwhaFab.extended(MaterialIcons.home(ElwhaFab.Size.SMALL.iconPx()), "Home");
    homeFab.setToolTipText("Open the Home landing — index of every Showcase surface.");
    homeFab.addActionListener(e -> showCard(HOME_KEY));
    target.setFab(homeFab);

    foundationsPrim = ElwhaNavRailDestination.of(MaterialIcons.symbol("palette"), AREA_FOUNDATIONS);
    componentsPrim = ElwhaNavRailDestination.of(MaterialIcons.symbol("widgets"), AREA_COMPONENTS);
    containersPrim = ElwhaNavRailDestination.of(MaterialIcons.symbol("layers"), AREA_CONTAINERS);

    target.setPrimary(List.of(foundationsPrim, componentsPrim, containersPrim));

    // Action listeners (not selection listeners) so re-clicking the already-selected primary
    // while inside a leaf returns to that area's landing — the single-step "back to area" trick
    // without a breadcrumbs component.
    foundationsPrim.addActionListener(e -> showCard(FOUNDATIONS_KEY));
    componentsPrim.addActionListener(e -> showCard(COMPONENTS_KEY));
    containersPrim.addActionListener(e -> showCard(CONTAINERS_KEY));

    // Trailing-action slot — the rail's bottom-anchored utility row. The Showcase dogfoods BOTH M3
    // dialog types here as live chrome: a (?) help button opens a Basic ElwhaDialog (how-to), and
    // the (i) About button opens an ElwhaFullScreenDialog — so the rail's chrome contract and both
    // dialog primitives have a live demo on the library's own canonical playground.
    final ElwhaIconButton helpButton =
        new ElwhaIconButton(MaterialIcons.help(IconButtonSize.M.iconPx()));
    helpButton.setToolTipText("How to use the Showcase");
    helpButton.addActionListener(e -> openHelpDialog());
    final ElwhaIconButton aboutButton =
        new ElwhaIconButton(MaterialIcons.info(IconButtonSize.M.iconPx()));
    aboutButton.setToolTipText("About Elwha and the Showcase");
    aboutButton.addActionListener(e -> openAboutDialog());
    target.setTrailingActions(List.of(helpButton, aboutButton));

    return target;
  }

  // Opens the About surface using ElwhaFullScreenDialog (#271) — dogfooding the M3 full-screen
  // dialog as live Showcase chrome. Full-screen has headline + content slots only (no
  // supportingText), so the Basic dialog's tagline folds into the first content paragraph; the
  // leading ✕ / Esc dismiss it (no separate Close action needed). contentMaxWidth(640) keeps the
  // longer-form About content readable without it sprawling on a wide desktop frame (#291).
  private void openAboutDialog() {
    final JPanel aboutBody = new JPanel();
    aboutBody.setOpaque(false);
    aboutBody.setLayout(new BoxLayout(aboutBody, BoxLayout.Y_AXIS));

    aboutBody.add(
        aboutParagraph(
            "<html><body style='width:600px'>A Swing component library built on FlatLaf. Elwha"
                + " provides Material 3 Expressive components for desktop Java &mdash; buttons,"
                + " chips, cards, FABs, badges, button groups, a navigation rail, and the token"
                + " foundation underneath them. Apache 2.0, JDK 21.</body></html>"));

    aboutBody.add(Box.createVerticalStrut(16));

    final JLabel sectionShowcase = new JLabel("The Elwha Showcase");
    sectionShowcase.setFont(sectionShowcase.getFont().deriveFont(Font.BOLD, 14f));
    sectionShowcase.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    aboutBody.add(sectionShowcase);

    aboutBody.add(
        aboutParagraph(
            "<html><body style='width:600px'>This app is the unified, curated playground for the"
                + " whole component set. Foundations covers the design tokens; Components is a"
                + " Workbench + Gallery per primitive; Containers covers the multi-instance"
                + " surfaces. Switch palette and light/dark/system from the header bar to see the"
                + " whole library re-theme live.</body></html>"));

    aboutBody.add(Box.createVerticalStrut(16));

    final JLabel sectionLinks = new JLabel("Links");
    sectionLinks.setFont(sectionLinks.getFont().deriveFont(Font.BOLD, 14f));
    sectionLinks.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    aboutBody.add(sectionLinks);

    aboutBody.add(aboutParagraph("<html>Repository: github.com/OWS-PFMS/elwha</html>"));
    aboutBody.add(aboutParagraph("<html>License: Apache License 2.0</html>"));

    ElwhaFullScreenDialog.builder()
        .headline("About Elwha")
        .content(aboutBody)
        .contentMaxWidth(640)
        .showDivider(true)
        .build()
        .show(rail);
  }

  // Opens a "how to use the Showcase" Basic ElwhaDialog (#254) — the other half of the rail's
  // dialog dogfood: a short, quick-decision help surface where the Basic dialog (not full-screen)
  // is the right M3 fit.
  private void openHelpDialog() {
    final JPanel helpBody = new JPanel();
    helpBody.setOpaque(false);
    helpBody.setLayout(new BoxLayout(helpBody, BoxLayout.Y_AXIS));

    helpBody.add(
        aboutParagraph(
            "<html><body style='width:380px'>The navigation rail on the left has three areas:"
                + " <b>Foundations</b> (design tokens), <b>Components</b> (a Workbench + Gallery"
                + " per primitive), and <b>Containers</b> (multi-instance surfaces). Click an area"
                + " to open its landing page, then click a card to open that"
                + " component.</body></html>"));
    helpBody.add(Box.createVerticalStrut(8));
    helpBody.add(
        aboutParagraph(
            "<html><body style='width:380px'>Each component leaf has two tabs: <b>Workbench</b>"
                + " &mdash; configure and exercise the live component &mdash; and <b>Gallery</b>"
                + " &mdash; a static matrix of variants &amp; states.</body></html>"));
    helpBody.add(Box.createVerticalStrut(8));
    helpBody.add(
        aboutParagraph(
            "<html><body style='width:380px'>The header bar switches palette and light / dark /"
                + " system mode &mdash; the whole library re-themes live. Re-click the current area"
                + " in the rail to return to its landing; the <b>(i)</b> button opens About.</body>"
                + "</html>"));

    ElwhaDialog.builder()
        .headline("How to use the Showcase")
        .content(helpBody)
        .confirmAction(ElwhaButton.textButton("Got it"))
        .build()
        .show(rail);
  }

  private static JLabel aboutParagraph(final String html) {
    final JLabel label = new JLabel(html);
    label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    label.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
    return label;
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
    final JCheckBox cornerRadiiBox = new JCheckBox("Per-corner radii override");
    final JSpinner topLeftSpinner = new JSpinner(new SpinnerNumberModel(12, 0, 60, 2));
    final JSpinner topRightSpinner = new JSpinner(new SpinnerNumberModel(12, 0, 60, 2));
    final JSpinner bottomRightSpinner = new JSpinner(new SpinnerNumberModel(12, 0, 60, 2));
    final JSpinner bottomLeftSpinner = new JSpinner(new SpinnerNumberModel(12, 0, 60, 2));
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
    controls.addSection("Corner radii");
    controls.addControl("", cornerRadiiBox);
    controls.addControl("Top-left", topLeftSpinner);
    controls.addControl("Top-right", topRightSpinner);
    controls.addControl("Bottom-right", bottomRightSpinner);
    controls.addControl("Bottom-left", bottomLeftSpinner);
    controls.addSection("State");
    controls.addControl("", selectedBox);
    controls.addControl("", enabledBox);
    installMorphControls(controls);

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

          // The per-corner override replaces the Shape-derived corner radius — disable the Shape
          // control while it is active so the override reads as the single corner-geometry source.
          final boolean perCorner = cornerRadiiBox.isSelected();
          shapeBox.setEnabled(!perCorner);
          topLeftSpinner.setEnabled(perCorner);
          topRightSpinner.setEnabled(perCorner);
          bottomRightSpinner.setEnabled(perCorner);
          bottomLeftSpinner.setEnabled(perCorner);
          final CornerRadii cornerRadii =
              perCorner
                  ? CornerRadii.of(
                      (Integer) topLeftSpinner.getValue(),
                      (Integer) topRightSpinner.getValue(),
                      (Integer) bottomRightSpinner.getValue(),
                      (Integer) bottomLeftSpinner.getValue())
                  : null;

          final ElwhaButton button = new ElwhaButton("Common button");
          button.setVariant(variant).setButtonSize(size).setShape(shape).setBorderWidth(width);
          if (icon) {
            button.setIcons(
                MaterialIcons.delete(size.iconSizePx()),
                MaterialIcons.deleteFilled(size.iconSizePx()));
          }
          if (mode == ButtonInteractionMode.SELECTABLE) {
            button.setInteractionMode(ButtonInteractionMode.SELECTABLE);
            button.setSelected(selected);
          }
          if (surface.role != null) {
            button.setSurfaceRole(surface.role);
          }
          button.setCornerRadii(cornerRadii);
          button.setEnabled(enabled);
          workbench.setStage(button);
          workbench.setCode(
              renderButtonCode(
                  variant,
                  mode,
                  size,
                  shape,
                  surface,
                  width,
                  icon,
                  selected,
                  enabled,
                  cornerRadii));
        };
    variantBox.addActionListener(event -> apply.run());
    modeBox.addActionListener(event -> apply.run());
    sizeBox.addActionListener(event -> apply.run());
    shapeBox.addActionListener(event -> apply.run());
    surfaceBox.addActionListener(event -> apply.run());
    borderWidth.addChangeListener(event -> apply.run());
    iconBox.addActionListener(event -> apply.run());
    cornerRadiiBox.addActionListener(event -> apply.run());
    topLeftSpinner.addChangeListener(event -> apply.run());
    topRightSpinner.addChangeListener(event -> apply.run());
    bottomRightSpinner.addChangeListener(event -> apply.run());
    bottomLeftSpinner.addChangeListener(event -> apply.run());
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
      final boolean enabled,
      final CornerRadii cornerRadii) {
    final StringBuilder code = new StringBuilder(320);
    code.append("ElwhaButton button = new ElwhaButton(\"Common button\");\n");
    code.append("button.setVariant(ButtonVariant.").append(variant).append(")\n");
    code.append("    .setButtonSize(ButtonSize.").append(size).append(")");
    // The Shape-derived corner radius is moot while a per-corner override is installed.
    if (cornerRadii == null) {
      code.append("\n    .setShape(ButtonShape.").append(shape).append(")");
    }
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
    if (icon) {
      code.append("\nbutton.setIcons(MaterialIcons.delete(), MaterialIcons.deleteFilled());");
    }
    if (cornerRadii != null) {
      code.append("\nbutton.setCornerRadii(CornerRadii.of(")
          .append(cornerRadii.topLeftPx())
          .append(", ")
          .append(cornerRadii.topRightPx())
          .append(", ")
          .append(cornerRadii.bottomRightPx())
          .append(", ")
          .append(cornerRadii.bottomLeftPx())
          .append("));");
    }
    if (!enabled) {
      code.append("\nbutton.setEnabled(false);");
    }
    return code.toString();
  }

  // #176 Phase 5 — shared "Animation" control group used by the Button and Button Group
  // workbenches per design doc §13. Surfaces the global reduced-motion toggle (drives
  // MorphAnimator.setReducedMotion, the same one ElwhaTheme.config(...).reducedMotion(...) wires)
  // and the workbench-only duration multiplier so an operator can slow morphs down for
  // observation. Listeners write directly to MorphAnimator globals — no stage rebuild needed
  // because the morphs read these on every tick.
  private static void installMorphControls(final WorkbenchControls controls) {
    final JCheckBox reducedMotionBox = new JCheckBox("Reduced motion");
    reducedMotionBox.setSelected(MorphAnimator.isReducedMotion());
    reducedMotionBox.addActionListener(
        e -> MorphAnimator.setReducedMotion(reducedMotionBox.isSelected()));

    final JComboBox<MorphSpeed> speedBox = new JComboBox<>(MorphSpeed.values());
    speedBox.setSelectedItem(MorphSpeed.NORMAL);
    speedBox.addActionListener(
        e ->
            MorphAnimator.setDurationMultiplier(
                ((MorphSpeed) speedBox.getSelectedItem()).multiplier));

    controls.addSection("Animation");
    controls.addControl("", reducedMotionBox);
    controls.addControl("Speed", speedBox);
  }

  // Workbench-only speed presets — 1× is the §3 spec, 2× / 5× let the operator watch a single
  // morph cycle at a slow enough rate to read the curve. Not a consumer API.
  private enum MorphSpeed {
    NORMAL("1× — natural", 1f),
    HALF("2× — slow", 2f),
    FIFTH("5× — very slow", 5f);

    final String label;
    final float multiplier;

    MorphSpeed(final String label, final float multiplier) {
      this.label = label;
      this.multiplier = multiplier;
    }

    @Override
    public String toString() {
      return label;
    }
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

  // ------------------------------------------------------------- FAB

  private static JComponent buildFabComponent() {
    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Workbench", buildFabWorkbench());
    tabs.addTab(
        "Gallery",
        scroll(
            stack(
                gallerySection("Variants & states", FabPlaygroundPanels.buildVariantGalleryPanel()),
                gallerySection("Sizes & forms", FabPlaygroundPanels.buildSizesPanel()))));
    return tabs;
  }

  private static JComponent buildFabWorkbench() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    final JComboBox<FabForm> formBox = new JComboBox<>(FabForm.values());
    formBox.setSelectedItem(FabForm.STANDARD);
    final JComboBox<ElwhaFab.Size> sizeBox = new JComboBox<>(ElwhaFab.Size.values());
    sizeBox.setSelectedItem(ElwhaFab.Size.SMALL);
    final JComboBox<ElwhaFab.Color> colorBox = new JComboBox<>(ElwhaFab.Color.values());
    colorBox.setSelectedItem(ElwhaFab.Color.PRIMARY_CONTAINER);
    final JComboBox<FabIconChoice> iconBox = new JComboBox<>(FabIconChoice.values());
    iconBox.setSelectedItem(FabIconChoice.ADD);
    final JTextField labelField = new JTextField("Compose", 14);
    final JCheckBox hoveredBox = new JCheckBox("Hovered");
    final JCheckBox pressedBox = new JCheckBox("Pressed");
    final JCheckBox enabledBox = new JCheckBox("Enabled", true);
    final JButton morphButton = new JButton("Toggle Standard ↔ Extended");

    // Holder for the live FAB so the morph button can drive morphTo(...) on whatever stage
    // instance the apply runnable last mounted, even though that runnable rebuilds on every
    // control change.
    final java.util.concurrent.atomic.AtomicReference<ElwhaFab> liveFab =
        new java.util.concurrent.atomic.AtomicReference<>();

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("FAB");
    controls.addControl("Form", formBox);
    controls.addControl("Size", sizeBox);
    controls.addControl("Color", colorBox);
    controls.addSection("Content");
    controls.addControl("Icon", iconBox);
    controls.addControl("Label text", labelField);
    controls.addSection("State");
    controls.addControl("", hoveredBox);
    controls.addControl("", pressedBox);
    controls.addControl("", enabledBox);
    controls.addSection("Morph");
    controls.addControl("", morphButton);

    final Runnable apply =
        () -> {
          final FabForm form = (FabForm) formBox.getSelectedItem();
          final ElwhaFab.Size size = (ElwhaFab.Size) sizeBox.getSelectedItem();
          final ElwhaFab.Color color = (ElwhaFab.Color) colorBox.getSelectedItem();
          final FabIconChoice iconChoice = (FabIconChoice) iconBox.getSelectedItem();
          final String label = labelField.getText() == null ? "" : labelField.getText();
          final boolean hovered = hoveredBox.isSelected();
          final boolean pressed = pressedBox.isSelected();
          final boolean enabled = enabledBox.isSelected();

          // Label text is only meaningful for the Extended forms — disable the field on Standard
          // so the operator sees that the value is currently ignored, but don't lose its content.
          labelField.setEnabled(form != FabForm.STANDARD);
          // The text-only Extended form ignores the icon picker.
          iconBox.setEnabled(form != FabForm.EXTENDED_TEXT);

          final ElwhaFab fab;
          final String safeLabel = label.isEmpty() ? "Compose" : label;
          switch (form) {
            case STANDARD -> fab = ElwhaFab.standard(iconChoice.icon(size.iconPx()));
            case EXTENDED_TEXT -> fab = ElwhaFab.extended(safeLabel);
            case EXTENDED_ICON_TEXT ->
                fab = ElwhaFab.extended(iconChoice.icon(size.iconPx()), safeLabel);
            default -> fab = ElwhaFab.standard(iconChoice.icon(size.iconPx()));
          }
          fab.setFabSize(size).setColor(color);
          fab.setHovered(hovered);
          fab.setPressed(pressed);
          fab.setEnabled(enabled);
          workbench.setStage(fab);
          workbench.setCode(renderFabCode(form, size, color, iconChoice, safeLabel, enabled));
          liveFab.set(fab);
          // Morph is only bidirectional on instances built via extended(Icon, String) per design
          // doc §9.3 — Standard (no text) and text-only Extended (no icon) lack the content
          // required for the opposite form, so morphTo throws there. Reflect that at the control
          // level rather than letting the click crash.
          final boolean morphable = form == FabForm.EXTENDED_ICON_TEXT;
          morphButton.setEnabled(morphable && enabled);
          morphButton.setToolTipText(
              morphable
                  ? "Click to animate the §9.1 Standard ↔ Extended morph on this instance."
                  : "Morph requires an extended(Icon, String) FAB — pick the icon + text form.");
        };
    morphButton.addActionListener(
        event -> {
          final ElwhaFab fab = liveFab.get();
          if (fab == null) {
            return;
          }
          fab.morphTo(
              fab.getForm() == ElwhaFab.Form.EXTENDED
                  ? ElwhaFab.Form.STANDARD
                  : ElwhaFab.Form.EXTENDED);
        });
    formBox.addActionListener(event -> apply.run());
    sizeBox.addActionListener(event -> apply.run());
    colorBox.addActionListener(event -> apply.run());
    iconBox.addActionListener(event -> apply.run());
    labelField.addActionListener(event -> apply.run());
    labelField
        .getDocument()
        .addDocumentListener(
            new javax.swing.event.DocumentListener() {
              @Override
              public void insertUpdate(final javax.swing.event.DocumentEvent e) {
                apply.run();
              }

              @Override
              public void removeUpdate(final javax.swing.event.DocumentEvent e) {
                apply.run();
              }

              @Override
              public void changedUpdate(final javax.swing.event.DocumentEvent e) {
                apply.run();
              }
            });
    hoveredBox.addActionListener(event -> apply.run());
    pressedBox.addActionListener(event -> apply.run());
    enabledBox.addActionListener(event -> apply.run());
    apply.run();
    return workbench;
  }

  private static String renderFabCode(
      final FabForm form,
      final ElwhaFab.Size size,
      final ElwhaFab.Color color,
      final FabIconChoice iconChoice,
      final String label,
      final boolean enabled) {
    final StringBuilder code = new StringBuilder(240);
    switch (form) {
      case STANDARD ->
          code.append("ElwhaFab fab = ElwhaFab.standard(MaterialIcons.")
              .append(iconChoice.codeMethodName())
              .append("(")
              .append(size.iconPx())
              .append("));");
      case EXTENDED_TEXT ->
          code.append("ElwhaFab fab = ElwhaFab.extended(\"").append(label).append("\");");
      case EXTENDED_ICON_TEXT ->
          code.append("ElwhaFab fab = ElwhaFab.extended(MaterialIcons.")
              .append(iconChoice.codeMethodName())
              .append("(")
              .append(size.iconPx())
              .append("), \"")
              .append(label)
              .append("\");");
      default -> {
        /* unreachable */
      }
    }
    if (size != ElwhaFab.Size.SMALL) {
      code.append("\nfab.setFabSize(ElwhaFab.Size.").append(size).append(");");
    }
    if (color != ElwhaFab.Color.PRIMARY_CONTAINER) {
      code.append("\nfab.setColor(ElwhaFab.Color.").append(color).append(");");
    }
    if (!enabled) {
      code.append("\nfab.setEnabled(false);");
    }
    return code.toString();
  }

  /** Form choices offered by the FAB Workbench's form picker. */
  private enum FabForm {
    STANDARD("Standard (icon only)"),
    EXTENDED_TEXT("Extended (text only)"),
    EXTENDED_ICON_TEXT("Extended (icon + text)");

    private final String displayName;

    FabForm(final String displayName) {
      this.displayName = displayName;
    }

    @Override
    public String toString() {
      return displayName;
    }
  }

  /**
   * Single-icon choices offered by the FAB Workbench's icon picker — FAB has no toggle state. M3
   * shows FAB icons at FILL=1 (the bolder filled variant) in its component docs, so the picker
   * resolves to the filled glyph where one is bundled. {@code add} (the M3 archetypal FAB icon) is
   * a geometric mark with no enclosed area, so {@code MaterialIcons} doesn't ship a separate filled
   * variant — there's no visible difference at FILL=1 to bundle.
   */
  private enum FabIconChoice {
    ADD("add"),
    EDIT("edit"),
    FAVORITE("favorite"),
    STAR("star"),
    DELETE("delete");

    private final String methodName;

    FabIconChoice(final String methodName) {
      this.methodName = methodName;
    }

    javax.swing.Icon icon(final int size) {
      return switch (methodName) {
        case "add" -> MaterialIcons.add(size);
        case "edit" -> MaterialIcons.editFilled(size);
        case "favorite" -> MaterialIcons.favoriteFilled(size);
        case "star" -> MaterialIcons.starFilled(size);
        case "delete" -> MaterialIcons.deleteFilled(size);
        default -> MaterialIcons.add(size);
      };
    }

    /**
     * The {@code MaterialIcons} method name that {@link #icon(int)} actually calls — used by the
     * code-view renderer so the equivalent-Java snippet matches the live FAB exactly.
     */
    String codeMethodName() {
      return switch (methodName) {
        case "add" -> "add";
        case "edit" -> "editFilled";
        case "favorite" -> "favoriteFilled";
        case "star" -> "starFilled";
        case "delete" -> "deleteFilled";
        default -> "add";
      };
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
    controls.addControl("Resize mode", resizeBox);
    controls.addControl("Connected max width", maxWidthSpinner);
    controls.addSection("Segments");
    controls.addControl("Content", contentBox);
    controls.addControl("Count", countSpinner);
    controls.addControl("Connected colour style", colorBox);
    controls.addSection("State");
    controls.addControl("", enabledBox);
    installMorphControls(controls);

    final Runnable apply =
        () -> {
          final ButtonGroupVariant variant = (ButtonGroupVariant) variantBox.getSelectedItem();
          final SelectionMode selection = (SelectionMode) selectionBox.getSelectedItem();
          final ButtonSize size = (ButtonSize) sizeBox.getSelectedItem();
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
                  variant, selection, size, resize, color, content, count, maxWidth, enabled));
        };
    variantBox.addActionListener(event -> apply.run());
    selectionBox.addActionListener(event -> apply.run());
    sizeBox.addActionListener(event -> apply.run());
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
      case TEXT_AND_ICONS -> {
        final MaterialIcons.IconPair pair =
            MaterialIcons.pair(SEGMENT_ICONS[index], size.iconSizePx());
        group.add(new ElwhaButton(SEGMENT_LABELS[index]).setIcons(pair.resting(), pair.filled()));
      }
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
  // combination hugs and mounts directly. The wrapper width is the larger of the demo target and
  // the group's own preferred width — at L / XL sizes the per-segment content floor can exceed the
  // 480 px demo target, and a wrapper sized to the demo target would clip the segments.
  private static JComponent stageForGroup(
      final ElwhaButtonGroup group,
      final ButtonGroupVariant variant,
      final ResizeMode resize,
      final int maxWidth) {
    // A STANDARD group has the §6 width-ripple; mount it with the live per-segment borrow readouts
    // (#184). CONNECTED has no width-ripple, so it gets the bare group / flex wrapper below.
    if (variant == ButtonGroupVariant.STANDARD) {
      return new ButtonGroupBorrowReadout(group);
    }
    if (variant != ButtonGroupVariant.CONNECTED || resize != ResizeMode.FLEXIBLE) {
      return group;
    }
    final JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.setOpaque(false);
    wrapper.add(group, BorderLayout.CENTER);
    final Dimension groupPref = group.getPreferredSize();
    final int demoTarget = maxWidth > 0 ? maxWidth : 480;
    wrapper.setPreferredSize(
        new Dimension(Math.max(demoTarget, groupPref.width), groupPref.height));
    return wrapper;
  }

  private static String renderButtonGroupCode(
      final ButtonGroupVariant variant,
      final SelectionMode selection,
      final ButtonSize size,
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
            .append("\")\n    .setIcons(MaterialIcons.")
            .append(SEGMENT_ICONS[i])
            .append("(), MaterialIcons.")
            .append(SEGMENT_ICONS[i])
            .append("Filled()));");
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

  // ------------------------------------------------------------- Badge

  private static JComponent buildBadgeComponent() {
    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Workbench", buildBadgeWorkbench());
    tabs.addTab(
        "Gallery",
        scroll(
            stack(
                gallerySection("Variants", BadgePlaygroundPanels.buildVariantsPanel()),
                gallerySection("Content range", BadgePlaygroundPanels.buildContentRangePanel()),
                gallerySection("Orientation", BadgePlaygroundPanels.buildOrientationPanel()),
                gallerySection(
                    "Trailing edge — Favorites 84",
                    BadgePlaygroundPanels.buildTrailingEdgePanel()))));
    return tabs;
  }

  private static final String BADGE_DEFAULT_CONTENT = "3";

  // Builds the Workbench's badge host for the chosen anchor mode: an ElwhaIconButton for
  // ICON_CORNER
  // (an IconBearing host), or a fixed-width "favorites" icon + label row for TRAILING_EDGE (the M3
  // "Favorites 84" composition). Both carry the "Favorites" accessible name and report name changes
  // to the inspector so the a11y splice stays visible regardless of mode.
  private static JComponent buildBadgeHost(
      final boolean trailingEdge, final boolean rtl, final JLabel a11yInspector) {
    final java.awt.ComponentOrientation orientation =
        rtl
            ? java.awt.ComponentOrientation.RIGHT_TO_LEFT
            : java.awt.ComponentOrientation.LEFT_TO_RIGHT;
    final JComponent host;
    if (trailingEdge) {
      final JLabel content =
          new JLabel(
              "Favorites", MaterialIcons.favoriteFilled(), javax.swing.SwingConstants.LEADING);
      content.setIconTextGap(8);
      content.setComponentOrientation(orientation);
      final JPanel row = new JPanel(new BorderLayout());
      row.setOpaque(false);
      row.add(content, BorderLayout.LINE_START);
      row.setPreferredSize(new Dimension(220, 44));
      host = row;
    } else {
      host = new ElwhaIconButton(MaterialIcons.favoriteFilled());
    }
    host.setComponentOrientation(orientation);
    host.getAccessibleContext().setAccessibleName("Favorites");
    host.getAccessibleContext()
        .addPropertyChangeListener(
            event -> {
              if (javax.accessibility.AccessibleContext.ACCESSIBLE_NAME_PROPERTY.equals(
                  event.getPropertyName())) {
                final Object next = event.getNewValue();
                a11yInspector.setText(next == null ? "(null)" : "\"" + next + "\"");
              }
            });
    return host;
  }

  private static JComponent buildBadgeWorkbench() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    // Variant selector: a two-segment ElwhaButtonGroup (mandatory single-select) instead of a
    // combo. No clean MaterialIcons mapping for "small dot" vs "large pill" — text-only segments
    // are clearer than overloading an unrelated icon. Order matches the ElwhaBadge.Variant enum.
    final ElwhaButtonGroup variantGroup =
        new ElwhaButtonGroup(ButtonGroupVariant.CONNECTED)
            .setColorStyle(ButtonGroupColorStyle.FILLED)
            .setSelectionMode(SelectionMode.REQUIRED)
            .add("Small", "Large");
    variantGroup.setSelectedIndex(1);
    final JTextField contentField = new JTextField(BADGE_DEFAULT_CONTENT, 8);

    // Lib-native stepper buttons: ElwhaIconButton at XS (24 dp) using the M3 icon set —
    // remove / add / cached (refresh). setFocusable(false) keeps focus on the content field so
    // each click doesn't shift focus to the next button (Swing's default click-takes-focus
    // behavior was disorienting on a stepper that lives next to its input).
    final int stepperIconPx = IconButtonSize.XS.iconPx();
    final ElwhaIconButton decrementButton =
        new ElwhaIconButton(MaterialIcons.remove(stepperIconPx)).setButtonSize(IconButtonSize.XS);
    final ElwhaIconButton incrementButton =
        new ElwhaIconButton(MaterialIcons.add(stepperIconPx)).setButtonSize(IconButtonSize.XS);
    final ElwhaIconButton contentResetButton =
        new ElwhaIconButton(MaterialIcons.cached(stepperIconPx)).setButtonSize(IconButtonSize.XS);
    decrementButton.setToolTipText("Decrement count (only enabled when content is a numeric > 0)");
    incrementButton.setToolTipText("Increment count (only enabled when content is numeric)");
    contentResetButton.setToolTipText(
        "Reset content to default (\"" + BADGE_DEFAULT_CONTENT + "\")");
    decrementButton.setFocusable(false);
    incrementButton.setFocusable(false);
    contentResetButton.setFocusable(false);
    final JPanel stepperRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    stepperRow.add(decrementButton);
    stepperRow.add(incrementButton);
    stepperRow.add(contentResetButton);

    final JComboBox<ColorRole> containerColorBox = new JComboBox<>(ColorRole.values());
    containerColorBox.setSelectedItem(ColorRole.ERROR);
    final JComboBox<ColorRole> labelColorBox = new JComboBox<>(ColorRole.values());
    labelColorBox.setSelectedItem(ColorRole.ON_ERROR);
    final JLabel colorGuidance =
        new JLabel(
            "<html><i>M3 strongly prefers Error / On&nbsp;error.</i></html>",
            MaterialIcons.info(14),
            javax.swing.SwingConstants.LEADING);
    colorGuidance.setToolTipText(
        "<html>M3 doesn't strictly forbid other color roles, but every Badge example in the spec"
            + " uses Error / On&nbsp;error for visibility against navigation surfaces.<br>"
            + "Pick a different pair only when the consumer has a clear contrast story and"
            + " accessible defaults — see design doc §6.</html>");

    final JTextField a11yOverrideField = new JTextField("", 16);
    // Outlined ElwhaButton for the a11y-override clear action — matches the lib's M3 styling
    // rather than the default Swing JButton chrome.
    final ElwhaButton clearOverrideButton = ElwhaButton.outlinedButton("Reset");
    final JCheckBox rtlBox = new JCheckBox("RTL");
    final JComboBox<ElwhaBadgeAnchor.AnchorMode> anchorModeBox =
        new JComboBox<>(ElwhaBadgeAnchor.AnchorMode.values());
    final JLabel a11yInspector = new JLabel("(detached)");

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Badge");
    controls.addControl("Variant", variantGroup);
    controls.addControl("Content", contentField);
    controls.addControl("", stepperRow);
    controls.addSection("Color");
    controls.addControl("", colorGuidance);
    controls.addControl("Container", containerColorBox);
    controls.addControl("Label", labelColorBox);
    controls.addSection("Accessibility");
    controls.addControl("Override", a11yOverrideField);
    controls.addControl("", clearOverrideButton);
    controls.addControl("host.name:", a11yInspector);
    controls.addSection("Layout");
    controls.addControl("Anchor", anchorModeBox);
    controls.addControl("", rtlBox);

    final java.util.concurrent.atomic.AtomicReference<ElwhaBadgeAnchor.Attachment> liveAttachment =
        new java.util.concurrent.atomic.AtomicReference<>();

    final Runnable apply =
        () -> {
          final ElwhaBadgeAnchor.Attachment prior = liveAttachment.getAndSet(null);
          if (prior != null) {
            ElwhaBadgeAnchor.detach(prior);
          }

          final ElwhaBadge.Variant variant =
              variantGroup.getSelectedIndex() == 0
                  ? ElwhaBadge.Variant.SMALL
                  : ElwhaBadge.Variant.LARGE;
          final ColorRole containerColor = (ColorRole) containerColorBox.getSelectedItem();
          final ColorRole labelColor = (ColorRole) labelColorBox.getSelectedItem();
          final String contentText = contentField.getText() == null ? "" : contentField.getText();
          final String overrideText =
              a11yOverrideField.getText() == null ? "" : a11yOverrideField.getText();

          final boolean isLarge = variant == ElwhaBadge.Variant.LARGE;
          contentField.setEnabled(isLarge);
          labelColorBox.setEnabled(isLarge);
          // Numeric stepper buttons only make sense on integer content. Reset always available
          // on Large since it just rewrites the field to the seed. Decrement gates additionally
          // on > 0 so users can't drive the count negative — setContent(int) would throw anyway.
          final boolean numericContent = isAllAsciiDigits(contentText);
          final int parsedCount = numericContent ? Integer.parseInt(contentText) : -1;
          decrementButton.setEnabled(isLarge && numericContent && parsedCount > 0);
          incrementButton.setEnabled(isLarge && numericContent);
          contentResetButton.setEnabled(isLarge);

          final boolean trailingEdge =
              anchorModeBox.getSelectedItem() == ElwhaBadgeAnchor.AnchorMode.TRAILING_EDGE;
          final JComponent host = buildBadgeHost(trailingEdge, rtlBox.isSelected(), a11yInspector);

          final ElwhaBadge badge;
          final String resolvedContent = contentText.isEmpty() ? "3" : contentText;
          if (variant == ElwhaBadge.Variant.SMALL) {
            badge = ElwhaBadge.small();
          } else {
            badge = ElwhaBadge.large(resolvedContent);
            badge.withLabelColor(labelColor);
          }
          badge.withContainerColor(containerColor);
          if (!overrideText.isEmpty()) {
            badge.withAccessibilityText(overrideText);
          }

          workbench.setStage(host);
          liveAttachment.set(
              trailingEdge
                  ? ElwhaBadgeAnchor.attachTrailingEdge(host, badge)
                  : ElwhaBadgeAnchor.attach((com.owspfm.elwha.badge.IconBearing) host, badge));
          a11yInspector.setText("\"" + host.getAccessibleContext().getAccessibleName() + "\"");
          workbench.setCode(
              renderBadgeCode(variant, resolvedContent, containerColor, labelColor, overrideText));
        };

    clearOverrideButton.addActionListener(
        event -> {
          a11yOverrideField.setText("");
          apply.run();
        });
    decrementButton.addActionListener(
        event -> {
          final String text = contentField.getText();
          if (!isAllAsciiDigits(text)) {
            return;
          }
          final int next = Math.max(0, Integer.parseInt(text) - 1);
          contentField.setText(Integer.toString(next));
        });
    incrementButton.addActionListener(
        event -> {
          final String text = contentField.getText();
          if (!isAllAsciiDigits(text)) {
            return;
          }
          // Cap raw value at 1000 so increment-from-999 lands on the M3 "999+" overflow without
          // letting the field accumulate arbitrarily large numbers the badge will collapse anyway.
          final int next = Math.min(1000, Integer.parseInt(text) + 1);
          contentField.setText(Integer.toString(next));
        });
    contentResetButton.addActionListener(event -> contentField.setText(BADGE_DEFAULT_CONTENT));
    variantGroup.addSelectionListener(group -> apply.run());
    containerColorBox.addActionListener(event -> apply.run());
    labelColorBox.addActionListener(event -> apply.run());
    rtlBox.addActionListener(event -> apply.run());
    anchorModeBox.addActionListener(event -> apply.run());

    final javax.swing.event.DocumentListener docListener =
        new javax.swing.event.DocumentListener() {
          @Override
          public void insertUpdate(final javax.swing.event.DocumentEvent e) {
            apply.run();
          }

          @Override
          public void removeUpdate(final javax.swing.event.DocumentEvent e) {
            apply.run();
          }

          @Override
          public void changedUpdate(final javax.swing.event.DocumentEvent e) {
            apply.run();
          }
        };
    contentField.getDocument().addDocumentListener(docListener);
    a11yOverrideField.getDocument().addDocumentListener(docListener);

    apply.run();
    return workbench;
  }

  private static String renderBadgeCode(
      final ElwhaBadge.Variant variant,
      final String content,
      final ColorRole containerColor,
      final ColorRole labelColor,
      final String accessibilityOverride) {
    final StringBuilder code = new StringBuilder(240);
    code.append("ElwhaBadge badge = ElwhaBadge.");
    if (variant == ElwhaBadge.Variant.SMALL) {
      code.append("small()");
    } else {
      code.append("large(\"").append(content).append("\")");
    }
    if (containerColor != ColorRole.ERROR) {
      code.append("\n    .withContainerColor(ColorRole.").append(containerColor.name()).append(")");
    }
    if (variant == ElwhaBadge.Variant.LARGE && labelColor != ColorRole.ON_ERROR) {
      code.append("\n    .withLabelColor(ColorRole.").append(labelColor.name()).append(")");
    }
    if (!accessibilityOverride.isEmpty()) {
      code.append("\n    .withAccessibilityText(\"").append(accessibilityOverride).append("\")");
    }
    code.append(";\nElwhaBadgeAnchor.attach(host, badge);");
    return code.toString();
  }

  private static boolean isAllAsciiDigits(final String s) {
    if (s == null || s.isEmpty()) {
      return false;
    }
    for (int i = 0; i < s.length(); i++) {
      final char c = s.charAt(i);
      if (c < '0' || c > '9') {
        return false;
      }
    }
    return true;
  }

  // ------------------------------------------------------------- Nav rail destination

  private static JComponent buildNavRailDestinationComponent() {
    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Workbench", buildNavRailDestinationWorkbench());
    tabs.addTab(
        "Gallery",
        scroll(
            stack(
                gallerySection("Variants", NavRailDestinationPlaygroundPanels.buildVariantsPanel()),
                gallerySection(
                    "Factory axis", NavRailDestinationPlaygroundPanels.buildFactoryAxisPanel()))));
    return tabs;
  }

  private static JComponent buildNavRailDestinationWorkbench() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    final java.util.List<com.owspfm.elwha.navrail.ElwhaNavRailDestination> destinations =
        new java.util.ArrayList<>();
    final String[][] entries = {
      {"widgets", "Home"},
      {"favorite", "Liked"},
      {"visibility", "Watched"},
      {"layers", "Stacks"},
      {"star", "Starred"}
    };
    final JPanel row = new JPanel(new java.awt.GridLayout(1, entries.length, 0, 0));
    row.setOpaque(false);
    row.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    for (final String[] entry : entries) {
      final com.owspfm.elwha.navrail.ElwhaNavRailDestination d =
          com.owspfm.elwha.navrail.ElwhaNavRailDestination.of(
              MaterialIcons.symbol(entry[0]), entry[1]);
      d.addActionListener(
          e -> {
            for (final com.owspfm.elwha.navrail.ElwhaNavRailDestination other : destinations) {
              other.setSelected(other == d);
            }
          });
      destinations.add(d);
      row.add(d);
    }
    destinations.get(0).setSelected(true);

    final JComboBox<String> badgeBox =
        new JComboBox<>(new String[] {"None", "Small (dot)", "Large · 3", "Large · 999+"});
    badgeBox.setSelectedIndex(0);
    final JComboBox<String> targetBox = new JComboBox<>();
    for (final String[] entry : entries) {
      targetBox.addItem(entry[1]);
    }

    final ElwhaButton applyBadge = ElwhaButton.outlinedButton("Apply badge");
    final JLabel badgeStatus = new JLabel(" ");
    applyBadge.addActionListener(
        e -> {
          final com.owspfm.elwha.navrail.ElwhaNavRailDestination target =
              destinations.get(targetBox.getSelectedIndex());
          target.setBadge(badgeFor((String) badgeBox.getSelectedItem()));
          badgeStatus.setText(badgeBox.getSelectedItem() + " → " + target.getLabel());
        });

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Selection");
    controls.addControl(
        "", new JLabel("Click any destination to select it (tab-strip semantics)."));
    controls.addSection("Badge");
    controls.addControl("Target", targetBox);
    controls.addControl("Variant", badgeBox);
    controls.addControl("", applyBadge);
    controls.addControl("Last:", badgeStatus);

    workbench.setStage(row);
    workbench.setCode(
        "ElwhaNavRailDestination home =\n"
            + "    ElwhaNavRailDestination.of(MaterialIcons.symbol(\"widgets\"), \"Home\");\n"
            + "home.setSelected(true);\n"
            + "home.setBadge(ElwhaBadge.large(3));");
    return workbench;
  }

  // ------------------------------------------------------------- Navigation Rail (Phase 2)

  private static JComponent buildNavigationRailComponent() {
    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Workbench", buildNavigationRailWorkbench());
    tabs.addTab(
        "Gallery",
        scroll(
            stack(
                gallerySection("Variants", NavigationRailPlaygroundPanels.buildVariantsPanel()),
                gallerySection(
                    "Expanded variant", NavigationRailPlaygroundPanels.buildExpandedPanel()),
                gallerySection(
                    "Surface knobs", NavigationRailPlaygroundPanels.buildSurfacePanel()))));
    return tabs;
  }

  private static JComponent buildNavigationRailWorkbench() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    final com.owspfm.elwha.navrail.ElwhaNavigationRail rail =
        com.owspfm.elwha.navrail.ElwhaNavigationRail.collapsed();
    rail.getAccessibleContext().setAccessibleName("Showcase Navigation Rail");

    final String[][] entries = {
      {"widgets", "Home"},
      {"favorite", "Liked"},
      {"visibility", "Watched"},
      {"layers", "Stacks"},
      {"star", "Starred"}
    };
    final java.util.List<com.owspfm.elwha.navrail.ElwhaNavRailDestination> dests =
        new java.util.ArrayList<>();
    for (final String[] entry : entries) {
      dests.add(
          com.owspfm.elwha.navrail.ElwhaNavRailDestination.of(
              MaterialIcons.symbol(entry[0]), entry[1]));
    }
    dests.get(1).setBadge(com.owspfm.elwha.badge.ElwhaBadge.small());
    rail.setPrimary(dests);
    rail.setSurfaceFilled(true);
    rail.setMenuButton(new com.owspfm.elwha.iconbutton.ElwhaIconButton(MaterialIcons.menu()));

    // Custom Workbench stage for Nav Rail: the ComponentWorkbench default "single comp centered on
    // a surface" pattern doesn't fit chrome components — the rail IS the surface, so placing it
    // on another surface reads as "chrome inside chrome" decoration. Instead build a fake-app
    // frame: rail docks on the leading edge, a content panel with selected-label + log fills the
    // rest. Matches the standalone playground's Interactive tab so the rail-as-chrome semantics
    // are visible at a glance.
    final javax.swing.JTextArea railLog = new javax.swing.JTextArea(6, 30);
    railLog.setEditable(false);
    railLog.setLineWrap(true);
    railLog.setWrapStyleWord(true);
    final javax.swing.JScrollPane logScroll = new javax.swing.JScrollPane(railLog);
    logScroll.setBorder(BorderFactory.createTitledBorder("Selection + variant log"));

    final JLabel selectedLabel = new JLabel("Selected: Home");
    selectedLabel.setFont(selectedLabel.getFont().deriveFont(Font.BOLD, 18f));
    selectedLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

    final JPanel contentArea = new JPanel(new BorderLayout());
    contentArea.setOpaque(false);
    contentArea.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    contentArea.add(selectedLabel, BorderLayout.NORTH);
    contentArea.add(logScroll, BorderLayout.CENTER);

    final JPanel fakeAppFrame =
        new JPanel(new BorderLayout()) {
          @Override
          public Dimension getPreferredSize() {
            // Stretch the fake app frame to a tall preferred size so the ComponentWorkbench's
            // stage surface grows to accommodate it (sizeStageSurface picks max(chosen, content)).
            // Without this the rail's natural preferred height (chrome + few destinations ≈
            // 380 dp) leaves the rail marooned in the middle of a much larger empty surface;
            // 720 dp is a comfortable laptop-friendly window-frame height.
            return new Dimension(720, 720);
          }
        };
    fakeAppFrame.setBackground(com.owspfm.elwha.theme.ColorRole.SURFACE_CONTAINER_LOW.resolve());
    fakeAppFrame.setBorder(
        BorderFactory.createLineBorder(
            com.owspfm.elwha.theme.ColorRole.OUTLINE_VARIANT.resolve(), 1));
    fakeAppFrame.add(rail, BorderLayout.WEST);
    fakeAppFrame.add(contentArea, BorderLayout.CENTER);

    final JPanel railRow = new JPanel(new BorderLayout());
    railRow.setOpaque(false);
    railRow.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    railRow.add(fakeAppFrame, BorderLayout.CENTER);

    rail.addSelectionListener(
        (prev, cur) -> {
          final String label = cur == null ? "(none)" : cur.getLabel();
          selectedLabel.setText("Selected: " + label);
          railLog.append(
              "Selection: " + (prev == null ? "(none)" : prev.getLabel()) + " → " + label + "\n");
          railLog.setCaretPosition(railLog.getDocument().getLength());
        });
    rail.addPropertyChangeListener(
        com.owspfm.elwha.navrail.ElwhaNavigationRail.PROPERTY_VARIANT,
        e -> {
          railLog.append("Variant → " + e.getNewValue() + "\n");
          railLog.setCaretPosition(railLog.getDocument().getLength());
        });

    final JCheckBox surfaceFilled = new JCheckBox("Surface filled", true);
    surfaceFilled.addActionListener(e -> rail.setSurfaceFilled(surfaceFilled.isSelected()));
    final JCheckBox dividerBox = new JCheckBox("Divider");
    dividerBox.addActionListener(e -> rail.setDivider(dividerBox.isSelected()));
    final JCheckBox elevationBox = new JCheckBox("Elevation 1");
    elevationBox.addActionListener(e -> rail.setElevation(elevationBox.isSelected() ? 1 : 0));

    final JCheckBox menuBox = new JCheckBox("Menu button", true);
    menuBox.addActionListener(
        e ->
            rail.setMenuButton(
                menuBox.isSelected()
                    ? new com.owspfm.elwha.iconbutton.ElwhaIconButton(MaterialIcons.menu())
                    : null));
    final JCheckBox fabBox = new JCheckBox("FAB");
    fabBox.addActionListener(
        e ->
            rail.setFab(
                fabBox.isSelected()
                    ? com.owspfm.elwha.fab.ElwhaFab.extended(MaterialIcons.edit(), "Compose")
                    : null));
    final JCheckBox trailingBox = new JCheckBox("Trailing actions");
    trailingBox.addActionListener(
        e -> {
          if (trailingBox.isSelected()) {
            final java.util.List<com.owspfm.elwha.iconbutton.ElwhaIconButton> actions =
                new java.util.ArrayList<>();
            actions.add(new com.owspfm.elwha.iconbutton.ElwhaIconButton(MaterialIcons.help()));
            actions.add(new com.owspfm.elwha.iconbutton.ElwhaIconButton(MaterialIcons.info()));
            rail.setTrailingActions(actions);
          } else {
            rail.setTrailingActions(null);
          }
        });

    // --- Phase 3: Variant + Expanded layout controls ---
    final javax.swing.JRadioButton collapsedBtn = new javax.swing.JRadioButton("Collapsed", true);
    final javax.swing.JRadioButton expandedBtn = new javax.swing.JRadioButton("Expanded");
    final javax.swing.ButtonGroup variantGrp = new javax.swing.ButtonGroup();
    variantGrp.add(collapsedBtn);
    variantGrp.add(expandedBtn);
    collapsedBtn.addActionListener(
        e -> rail.morphTo(com.owspfm.elwha.navrail.ElwhaNavigationRail.Variant.COLLAPSED));
    expandedBtn.addActionListener(
        e -> rail.morphTo(com.owspfm.elwha.navrail.ElwhaNavigationRail.Variant.EXPANDED));
    rail.addPropertyChangeListener(
        com.owspfm.elwha.navrail.ElwhaNavigationRail.PROPERTY_VARIANT,
        e -> {
          final com.owspfm.elwha.navrail.ElwhaNavigationRail.Variant v =
              (com.owspfm.elwha.navrail.ElwhaNavigationRail.Variant) e.getNewValue();
          if (v == com.owspfm.elwha.navrail.ElwhaNavigationRail.Variant.EXPANDED) {
            expandedBtn.setSelected(true);
          } else {
            collapsedBtn.setSelected(true);
          }
        });

    final javax.swing.JSlider widthSlider = new javax.swing.JSlider(220, 360, 256);
    widthSlider.setMajorTickSpacing(70);
    widthSlider.setPaintTicks(true);
    final JLabel widthLabel = new JLabel("256 px");
    widthSlider.addChangeListener(
        e -> {
          final int v = widthSlider.getValue();
          rail.setExpandedWidth(v);
          widthLabel.setText(v + " px");
        });

    final JCheckBox sectionsBox = new JCheckBox("Sections");
    sectionsBox.addActionListener(
        e -> {
          if (sectionsBox.isSelected()) {
            rail.clearSections();
            final java.util.List<com.owspfm.elwha.navrail.ElwhaNavRailDestination> tools =
                new java.util.ArrayList<>();
            tools.add(
                com.owspfm.elwha.navrail.ElwhaNavRailDestination.of(
                    MaterialIcons.symbol("dark_mode"), "Theme"));
            tools.add(
                com.owspfm.elwha.navrail.ElwhaNavRailDestination.of(
                    MaterialIcons.symbol("help"), "Help"));
            rail.addSection("Tools", tools);
            final java.util.List<com.owspfm.elwha.navrail.ElwhaNavRailDestination> other =
                new java.util.ArrayList<>();
            other.add(
                com.owspfm.elwha.navrail.ElwhaNavRailDestination.of(
                    MaterialIcons.symbol("info"), "About"));
            other.add(
                com.owspfm.elwha.navrail.ElwhaNavRailDestination.of(
                    MaterialIcons.symbol("star"), "Sponsor"));
            rail.addSection("Other", other);
          } else {
            rail.clearSections();
          }
        });

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Variant");
    controls.addControl("", collapsedBtn);
    controls.addControl("", expandedBtn);
    controls.addSection("Layout");
    controls.addControl("Expanded width:", widthSlider);
    controls.addControl("", widthLabel);
    controls.addControl("", sectionsBox);
    controls.addSection("Surface");
    controls.addControl("", surfaceFilled);
    controls.addControl("", dividerBox);
    controls.addControl("", elevationBox);
    controls.addSection("Chrome");
    controls.addControl("", menuBox);
    controls.addControl("", fabBox);
    controls.addControl("", trailingBox);
    controls.addSection("Selection");
    controls.addControl("", new JLabel("Click any destination to select."));
    controls.addControl("Current:", selectedLabel);

    workbench.setStage(railRow);
    workbench.setCode(
        "ElwhaNavigationRail rail = ElwhaNavigationRail.collapsed();\n"
            + "rail.setMenuButton(new ElwhaIconButton(MaterialIcons.menu()));\n"
            + "rail.setFab(ElwhaFab.extended(MaterialIcons.edit(), \"Compose\"));\n"
            + "rail.setPrimary(List.of(home, liked, watched, stacks, starred));\n"
            + "rail.addSection(\"Tools\", List.of(settings, help));\n"
            + "rail.morphTo(Variant.EXPANDED);  // 350ms morph\n"
            + "rail.addSelectionListener((prev, cur) -> selectTab(cur));");
    return workbench;
  }

  private static com.owspfm.elwha.badge.ElwhaBadge badgeFor(final String label) {
    if (label == null) {
      return null;
    }
    return switch (label) {
      case "Small (dot)" -> com.owspfm.elwha.badge.ElwhaBadge.small();
      case "Large · 3" -> com.owspfm.elwha.badge.ElwhaBadge.large(3);
      case "Large · 999+" -> com.owspfm.elwha.badge.ElwhaBadge.large("999+");
      default -> null;
    };
  }

  // --- helpers ---

  // --- Dialog leaf: trigger-button Workbench + static-snapshot Gallery (design doc §15) ---
  //
  // Dialogs are modal overlays, not embeddable surfaces, so the Dialog leaf breaks the standard
  // ComponentWorkbench pattern: the Workbench is a control panel of trigger buttons that open live
  // ElwhaDialogs on this Showcase frame (which doubles as the real-world overlay-on-frame smoke
  // test), and the Gallery shows static non-modal renderPreview() snapshots rather than a live
  // matrix that would stack dialogs onto the frame.

  private static JComponent buildDialogComponent() {
    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Workbench", buildDialogWorkbench());
    tabs.addTab(
        "Gallery",
        scroll(
            stack(
                gallerySection("Basic", buildDialogGallery()),
                gallerySection("Full-screen", buildFullScreenDialogGallery()))));
    return tabs;
  }

  private enum DialogVariant {
    BASIC,
    WITH_ICON,
    DESTRUCTIVE,
    SCROLLABLE
  }

  private static JComponent buildDialogWorkbench() {
    final JComboBox<Integer> actionCount = new JComboBox<>(new Integer[] {1, 2, 3});
    actionCount.setSelectedItem(2);
    final JCheckBox scrimDismiss = new JCheckBox("scrim-dismissible", true);
    final JCheckBox escDismiss = new JCheckBox("Esc-dismissible", true);
    final JCheckBox reducedMotion = new JCheckBox("reduced motion");
    final JCheckBox fsConfirm = new JCheckBox("FS: confirm action", true);
    final JCheckBox fsDivider = new JCheckBox("FS: divider", true);
    final JLabel status = new JLabel("Configure, then open a dialog — it opens on this frame.");

    final ElwhaButton basic = ElwhaButton.filledButton("Open basic dialog");
    final ElwhaButton withIcon = ElwhaButton.filledTonalButton("Open dialog with icon");
    final ElwhaButton destructive = ElwhaButton.filledTonalButton("Open destructive confirm");
    final ElwhaButton scrollable = ElwhaButton.filledTonalButton("Open scrollable-content dialog");
    final ElwhaButton fullScreen = ElwhaButton.filledTonalButton("Open full-screen dialog");

    basic.addActionListener(
        e ->
            openWorkbenchDialog(
                basic,
                DialogVariant.BASIC,
                actionCount,
                scrimDismiss,
                escDismiss,
                reducedMotion,
                status));
    withIcon.addActionListener(
        e ->
            openWorkbenchDialog(
                withIcon,
                DialogVariant.WITH_ICON,
                actionCount,
                scrimDismiss,
                escDismiss,
                reducedMotion,
                status));
    destructive.addActionListener(
        e ->
            openWorkbenchDialog(
                destructive,
                DialogVariant.DESTRUCTIVE,
                actionCount,
                scrimDismiss,
                escDismiss,
                reducedMotion,
                status));
    scrollable.addActionListener(
        e ->
            openWorkbenchDialog(
                scrollable,
                DialogVariant.SCROLLABLE,
                actionCount,
                scrimDismiss,
                escDismiss,
                reducedMotion,
                status));
    fullScreen.addActionListener(
        e ->
            openWorkbenchFullScreenDialog(
                fullScreen, fsConfirm, fsDivider, escDismiss, reducedMotion, status));

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 16, 8));
    controls.add(new JLabel("Actions:"));
    controls.add(actionCount);
    controls.add(scrimDismiss);
    controls.add(escDismiss);
    controls.add(reducedMotion);
    controls.add(fsConfirm);
    controls.add(fsDivider);

    final JPanel triggers = new JPanel(new GridLayout(0, 1, 0, 12));
    triggers.setBorder(BorderFactory.createEmptyBorder(24, 64, 16, 64));
    triggers.add(basic);
    triggers.add(withIcon);
    triggers.add(destructive);
    triggers.add(scrollable);
    triggers.add(fullScreen);
    triggers.add(status);

    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(controls, BorderLayout.NORTH);
    panel.add(triggers, BorderLayout.CENTER);
    return panel;
  }

  // Builds a dialog per the chosen variant + the shared controls and shows it on the frame the
  // trigger lives in. A destructive dialog forces scrim/Esc dismissal off regardless of the toggles
  // — M3's "dialogs requiring a decision aren't dismissible without an explicit choice".
  private static void openWorkbenchDialog(
      final Component parent,
      final DialogVariant variant,
      final JComboBox<Integer> actionCount,
      final JCheckBox scrimDismiss,
      final JCheckBox escDismiss,
      final JCheckBox reducedMotion,
      final JLabel status) {
    MorphAnimator.setReducedMotion(reducedMotion.isSelected());

    final ElwhaDialog.Builder builder = ElwhaDialog.builder();
    switch (variant) {
      case BASIC ->
          builder
              .headline("Sync your library?")
              .supportingText("This downloads the latest catalogue to all of your devices.");
      case WITH_ICON ->
          builder
              .icon(MaterialIcons.symbol("cached").unselected(28))
              .headline("Refresh data?")
              .supportingText("We'll pull the newest records from the server.");
      case DESTRUCTIVE ->
          builder
              .icon(MaterialIcons.symbol("delete").unselected(28))
              .headline("Delete this collection?")
              .supportingText("This permanently removes the collection and everything in it.");
      case SCROLLABLE -> builder.headline("Terms of service").content(buildScrollableContent());
    }

    final boolean destructive = variant == DialogVariant.DESTRUCTIVE;
    final int count = (Integer) actionCount.getSelectedItem();
    builder.confirmAction(ElwhaButton.filledButton(destructive ? "Delete" : "Confirm"));
    if (count >= 3) {
      builder.alternateAction(ElwhaButton.textButton("Not now"));
    }
    if (count >= 2) {
      builder.cancelAction(ElwhaButton.textButton("Cancel"));
    }

    builder
        .dismissibleByScrim(!destructive && scrimDismiss.isSelected())
        .dismissibleByEsc(!destructive && escDismiss.isSelected())
        .onClose(cause -> status.setText("Last close: " + cause.name()))
        .build()
        .show(parent);
  }

  private static JComponent buildScrollableContent() {
    final JPanel body = new JPanel();
    body.setOpaque(false);
    body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
    for (int i = 1; i <= 16; i++) {
      final JLabel line =
          new JLabel("§ " + i + ". A clause the reader scrolls past to reach the actions.");
      line.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
      line.setAlignmentX(Component.LEFT_ALIGNMENT);
      body.add(line);
      body.add(Box.createVerticalStrut(8));
    }
    return body;
  }

  // Opens a live ElwhaFullScreenDialog on the trigger's frame — the real overlay-on-frame smoke
  // test
  // for the M3 full-screen variant (epic #271). A small sample form stands in for the longer-form-
  // input use case the full-screen dialog targets. Esc + reduced-motion reuse the shared toggles.
  private static void openWorkbenchFullScreenDialog(
      final Component parent,
      final JCheckBox fsConfirm,
      final JCheckBox fsDivider,
      final JCheckBox escDismiss,
      final JCheckBox reducedMotion,
      final JLabel status) {
    MorphAnimator.setReducedMotion(reducedMotion.isSelected());
    final ElwhaFullScreenDialog.Builder builder =
        ElwhaFullScreenDialog.builder()
            .headline("New event")
            .content(buildFullScreenSampleForm())
            .showDivider(fsDivider.isSelected())
            .dismissibleByEsc(escDismiss.isSelected())
            .onClose(cause -> status.setText("Last close: " + cause.name()));
    if (fsConfirm.isSelected()) {
      builder.confirmAction(ElwhaButton.textButton("Save"));
    }
    builder.build().show(parent);
  }

  // A few labeled fields standing in for a "create event"-style form. Field widths come from the
  // 560-column when shown live (the content tracks the viewport width); the column count gives the
  // static gallery preview a sensible natural width.
  private static JComponent buildFullScreenSampleForm() {
    final JPanel form = new JPanel();
    form.setOpaque(false);
    form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
    for (final String label : new String[] {"Title", "Location", "Start", "End", "Notes"}) {
      final JLabel caption = new JLabel(label);
      caption.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
      caption.setAlignmentX(Component.LEFT_ALIGNMENT);
      final JTextField field = new JTextField(18);
      field.setAlignmentX(Component.LEFT_ALIGNMENT);
      field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
      form.add(caption);
      form.add(Box.createVerticalStrut(4));
      form.add(field);
      form.add(Box.createVerticalStrut(16));
    }
    return form;
  }

  // Static, non-modal snapshots via ElwhaDialog.renderPreview() — real rendered surfaces (their
  // buttons are live but inert: clicking calls dismiss(), a no-op with no overlay attached),
  // stacked
  // vertically so each variant reads as its own dialog card without competing for one row's width.
  private static JComponent buildDialogGallery() {
    final JPanel column = new JPanel();
    column.setOpaque(false);
    column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
    column.setBorder(BorderFactory.createEmptyBorder(8, 20, 24, 20));
    addPreview(
        column,
        ElwhaDialog.builder()
            .headline("Discard draft?")
            .supportingText("Your changes haven't been saved and will be lost.")
            .confirmAction(ElwhaButton.filledButton("Discard"))
            .cancelAction(ElwhaButton.textButton("Cancel"))
            .build()
            .renderPreview());
    addPreview(
        column,
        ElwhaDialog.builder()
            .icon(MaterialIcons.symbol("delete").unselected(28))
            .headline("Delete item?")
            .supportingText("This action can't be undone.")
            .confirmAction(ElwhaButton.filledButton("Delete"))
            .cancelAction(ElwhaButton.textButton("Cancel"))
            .build()
            .renderPreview());
    addPreview(
        column,
        ElwhaDialog.builder()
            .headline("Leave without saving?")
            .confirmAction(ElwhaButton.filledButton("Leave"))
            .alternateAction(ElwhaButton.textButton("Save"))
            .cancelAction(ElwhaButton.textButton("Cancel"))
            .build()
            .renderPreview());
    return column;
  }

  // Static, non-modal full-screen-dialog snapshot via renderPreview() — the surface rendered at its
  // natural (560-column) preferred size rather than filling a frame, so it reads as a preview card
  // alongside the Basic snapshots (a live full-screen dialog would cover the whole frame). Buttons
  // are live but inert (dismiss() no-ops with no overlay attached).
  private static JComponent buildFullScreenDialogGallery() {
    final JPanel column = new JPanel();
    column.setOpaque(false);
    column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
    column.setBorder(BorderFactory.createEmptyBorder(8, 20, 24, 20));
    addPreview(
        column,
        ElwhaFullScreenDialog.builder()
            .headline("New event")
            .content(buildFullScreenSampleForm())
            .confirmAction(ElwhaButton.textButton("Save"))
            .showDivider(true)
            .build()
            .renderPreview());
    return column;
  }

  // Adds one preview to the vertical gallery column, wrapped in a left-aligned FlowLayout row so
  // the
  // surface keeps its natural width (BoxLayout would stretch it) and left-anchors, with a gap
  // below.
  private static void addPreview(final JPanel column, final JComponent preview) {
    final JPanel rowWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    rowWrap.setOpaque(false);
    rowWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
    rowWrap.add(preview);
    column.add(rowWrap);
    column.add(Box.createVerticalStrut(24));
  }

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
}
