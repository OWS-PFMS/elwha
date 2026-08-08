package com.owspfm.elwha.showcase;

import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.navrail.ElwhaNavigationRail;
import com.owspfm.elwha.overlay.AbstractElwhaOverlay;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.showcase.ElwhaShowcase.LeafEntry;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.textfield.ElwhaTextField;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Component;
import java.awt.Container;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 * Shared scaffolding for the Showcase suite: the built catalog, hierarchy search helpers, and the
 * two halves of the registry sweep.
 *
 * <p><strong>Why the catalog is built once.</strong> Populating it realises every leaf surface —
 * every workbench, every gallery, every playground panel the Showcase composes from — which is
 * seconds of work, so the suite shares one instance across classes. The trade is that a test must
 * never assert a leaf's <em>initial</em> state: another class may have driven a control on it
 * already. Tests that need a pristine scaffold build their own {@link ComponentWorkbench}.
 *
 * <p>The frame is never built. {@code ElwhaShowcase.buildAndShow} constructs a {@link
 * javax.swing.JFrame}, which throws under {@code java.awt.headless=true}; the catalog, the landing
 * cards, the leaf cards, and the navigation rail are all reachable without one.
 */
final class ShowcaseFixture {

  // Class references as they appear in a class file's constant pool — the internal form, with '/'
  // separators, of anything under the library's root package.
  private static final Pattern CLASS_REF =
      Pattern.compile("com/owspfm/elwha/[A-Za-z0-9$/]*[A-Za-z0-9$]");

  private static final String ROOT_PACKAGE = "com.owspfm.elwha.";

  // Harness suffixes: a story-time demo, smoke, guard, or diagnostic main is not a shipped
  // component, and neither is a standalone playground launcher.
  private static final List<String> HARNESS_SUFFIXES =
      List.of("Demo", "Smoke", "Guard", "Diag", "Playground");

  private static ElwhaShowcase built;
  private static Set<String> presented;
  private static List<String> shipped;

  private ShowcaseFixture() {}

  // ------------------------------------------------------------------ catalog

  /**
   * The shared Showcase — catalog populated, landing cards and leaf cards registered, navigation
   * rail installed.
   *
   * <p>Builds the deterministic theme itself rather than relying on {@link ThemeExtension}: a
   * {@code @ParameterizedTest} argument provider resolves before any {@code beforeEach} callback
   * runs, and a catalog built ahead of the theme would bake the pre-install fallback colors into
   * every leaf. Building on the dispatch thread has the same cause — argument providers run on the
   * calling thread, which is not the EDT.
   */
  static ElwhaShowcase showcase() {
    if (built == null) {
      onEdt(ShowcaseFixture::build);
    }
    return built;
  }

  private static void build() {
    MorphAnimator.setReducedMotion(true);
    ThemeExtension.install(Mode.LIGHT);
    final ElwhaShowcase showcase = new ElwhaShowcase();
    showcase.populateCatalog();
    showcase.populateLandingCards();
    showcase.populateLeafCards();
    showcase.buildShowcaseRail();
    built = showcase;
  }

  private static void onEdt(final Runnable task) {
    if (SwingUtilities.isEventDispatchThread()) {
      task.run();
      return;
    }
    try {
      SwingUtilities.invokeAndWait(task);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("interrupted building the Showcase catalog", e);
    } catch (final java.lang.reflect.InvocationTargetException e) {
      throw new IllegalStateException("the Showcase catalog failed to build", e.getCause());
    }
  }

  /** Every catalog leaf, in registration order. */
  static List<LeafEntry> leaves() {
    return new ArrayList<>(showcase().leaves.values());
  }

  /** Every leaf label, in registration order. */
  static List<String> leafLabels() {
    return new ArrayList<>(showcase().leaves.keySet());
  }

  /** The leaf registered under {@code label}. */
  static LeafEntry leaf(final String label) {
    return showcase().leaves.get(label);
  }

  /** The navigation rail the shared Showcase installed — the one {@code showCard} keeps in sync. */
  static ElwhaNavigationRail rail() {
    return showcase().rail;
  }

  /** The three area names, in rail order. */
  static List<String> areas() {
    return List.of(
        ElwhaShowcase.AREA_FOUNDATIONS,
        ElwhaShowcase.AREA_COMPONENTS,
        ElwhaShowcase.AREA_CONTAINERS);
  }

  // -------------------------------------------------------------- hierarchies

  /**
   * Sizes a component and lays out its whole subtree. {@code doLayout} places a container's own
   * children and stops there, so a grandchild keeps a zero rect and paints nothing — which would
   * make a construction smoke pass on a leaf that never actually rendered.
   */
  static void layOut(final Component component, final int width, final int height) {
    component.setSize(width, height);
    layOutTree(component);
  }

  private static void layOutTree(final Component component) {
    component.doLayout();
    if (component instanceof Container container) {
      for (final Component child : container.getComponents()) {
        layOutTree(child);
      }
    }
  }

  /** Every component in the subtree rooted at {@code root}, including {@code root} itself. */
  static List<Component> descendants(final Component root) {
    final List<Component> out = new ArrayList<>();
    collect(root, out);
    return out;
  }

  private static void collect(final Component component, final List<Component> out) {
    out.add(component);
    if (component instanceof Container container) {
      for (final Component child : container.getComponents()) {
        collect(child, out);
      }
    }
  }

  /** Every component of {@code type} in the subtree, in depth-first order. */
  static <T> List<T> findAll(final Component root, final Class<T> type) {
    final List<T> out = new ArrayList<>();
    for (final Component component : descendants(root)) {
      if (type.isInstance(component)) {
        out.add(type.cast(component));
      }
    }
    return out;
  }

  /** The first component of {@code type} in the subtree, or {@code null}. */
  static <T> T findFirst(final Component root, final Class<T> type) {
    final List<T> found = findAll(root, type);
    return found.isEmpty() ? null : found.get(0);
  }

  /**
   * The control captioned {@code label}, wherever that caption lives.
   *
   * <p>Two placements, because the dogfood sweep (#424) moved most captions off the row and into
   * the control. A {@link javax.swing.JSpinner} has no label of its own, so it still takes the row
   * label {@link WorkbenchControls#addControl} lays down beside it — rows are a label then its
   * control, added back to back, so the control is the next child along. An {@code
   * ElwhaSelectField} / {@code ElwhaCheckbox} / {@code ElwhaTextField} paints its own floating or
   * trailing label, so its row label is empty and the caption is read off the control itself.
   *
   * <p>Row labels are searched first: that placement is the one that pairs a caption with a
   * <em>different</em> component, so letting a control's own label shadow it could return the wrong
   * neighbour.
   */
  static Component controlLabelled(final Container column, final String label) {
    final Component[] children = column.getComponents();
    for (int i = 0; i < children.length - 1; i++) {
      if (children[i] instanceof JLabel row
          && label.equals(row.getText())
          && !isSectionHeader(row)) {
        return children[i + 1];
      }
    }
    for (final Component child : children) {
      if (label.equals(ownLabelOf(child))) {
        return child;
      }
    }
    return null;
  }

  /**
   * Whether a {@link JLabel} in a controls column is a section header rather than a row label. The
   * two are indistinguishable by type, and a section title can repeat a control's caption, so
   * {@link WorkbenchControls} marks its headers.
   */
  private static boolean isSectionHeader(final JLabel label) {
    return Boolean.TRUE.equals(label.getClientProperty(WorkbenchControls.SECTION_HEADER));
  }

  /**
   * The caption a control carries — its row label, or its own label for the controls that paint
   * one. {@code ""} when it has neither, as the unlabelled rows a trigger button takes. Used to
   * name a control in a failure message.
   */
  static String labelOf(final Component control) {
    final Container parent = control.getParent();
    if (parent != null) {
      final Component[] children = parent.getComponents();
      for (int i = 1; i < children.length; i++) {
        if (children[i] == control
            && children[i - 1] instanceof JLabel row
            && !isSectionHeader(row)
            && row.getText() != null
            && !row.getText().isEmpty()) {
          return row.getText();
        }
      }
    }
    return ownLabelOf(control);
  }

  /**
   * The label a control paints for itself, or {@code ""} for one that paints none. The three
   * labelled Elwha control families the Showcase rails are built from; they share no common
   * label-bearing supertype, so the read is per-type.
   */
  static String ownLabelOf(final Component control) {
    final String own;
    if (control instanceof ElwhaSelectField<?> select) {
      own = select.getLabel();
    } else if (control instanceof ElwhaCheckbox box) {
      own = box.getLabel();
    } else if (control instanceof ElwhaTextField field) {
      own = field.getLabel();
    } else {
      own = null;
    }
    return own == null ? "" : own;
  }

  // ----------------------------------------------------------- registry sweep

  /**
   * Every shipped component, discovered by scanning the compiled library rather than listed here.
   *
   * <p>A shipped component is a public, concrete, top-level {@code Elwha*} type that a consumer
   * mounts: either a {@link Component} or one of the layered-pane {@link AbstractElwhaOverlay}
   * hosts. Story-time harnesses are excluded by rule, not by name — the {@code showcase}, {@code
   * playground}, and {@code card.fixes} packages, plus the {@code *Demo} / {@code *Smoke} / {@code
   * *Guard} / {@code *Diag} / {@code *Playground} suffixes. Interfaces, enums, models, events, and
   * static utility classes are not components and carry no leaf.
   */
  static List<String> shippedComponents() {
    if (shipped == null) {
      final List<String> found = new ArrayList<>();
      for (final String binary : compiledClasses()) {
        if (isShippedComponent(binary)) {
          found.add(binary);
        }
      }
      found.sort(String::compareTo);
      shipped = found;
    }
    return shipped;
  }

  private static boolean isShippedComponent(final String binary) {
    if (binary.contains("$")) {
      return false;
    }
    final int split = binary.lastIndexOf('.');
    final String simple = binary.substring(split + 1);
    final String pkg = binary.substring(0, Math.max(0, split));
    if (!simple.startsWith("Elwha")) {
      return false;
    }
    if (pkg.equals("com.owspfm.elwha.showcase")
        || pkg.endsWith(".playground")
        || pkg.endsWith(".fixes")) {
      return false;
    }
    for (final String suffix : HARNESS_SUFFIXES) {
      if (simple.endsWith(suffix)) {
        return false;
      }
    }
    final Class<?> type = load(binary);
    if (type == null
        || type.isInterface()
        || type.isEnum()
        || !Modifier.isPublic(type.getModifiers())
        || Modifier.isAbstract(type.getModifiers())) {
      return false;
    }
    return Component.class.isAssignableFrom(type)
        || AbstractElwhaOverlay.class.isAssignableFrom(type);
  }

  /**
   * Every library class the Showcase presents — the union of two independent signals, so neither a
   * component built inside another component's slot nor one wired up only in a lambda is missed.
   *
   * <ul>
   *   <li><strong>Referenced.</strong> Class references in the constant pools of the {@code
   *       showcase} package and the playground panel builders it composes from. This is what the
   *       Showcase's own code names, whether or not the naming site ever runs.
   *   <li><strong>Realised.</strong> The runtime classes of every component in every built leaf's
   *       view hierarchy. This is what the catalog actually instantiates, including children a
   *       component creates for itself.
   * </ul>
   */
  static Set<String> presentedComponents() {
    if (presented == null) {
      final Set<String> found = new TreeSet<>(referencedByShowcase());
      for (final LeafEntry entry : leaves()) {
        for (final Component component : descendants(entry.surface)) {
          final String name = component.getClass().getName();
          if (name.startsWith(ROOT_PACKAGE)) {
            found.add(name);
          }
        }
      }
      presented = found;
    }
    return presented;
  }

  /**
   * The shipped components {@code presented} does not account for — the sweep's verdict. Kept as a
   * pure function over both sets so it can be exercised against a name nothing presents, proving
   * the sweep still reports a gap rather than passing everything.
   */
  static List<String> notPresented(
      final Collection<String> components, final Set<String> presentedClasses) {
    final List<String> gaps = new ArrayList<>();
    for (final String component : components) {
      if (!presentedClasses.contains(component)) {
        gaps.add(component);
      }
    }
    return gaps;
  }

  // Class references reachable from the showcase package, expanded through playground panel
  // builders only. Stopping the walk at the first component class is deliberate: expanding through
  // components too would mark a component "presented" merely because another component uses one
  // internally, and the sweep would stop failing for a component that has no leaf.
  private static Set<String> referencedByShowcase() {
    final Set<String> zone = new TreeSet<>();
    final Deque<String> pending = new ArrayDeque<>();
    for (final String binary : compiledClasses()) {
      if (binary.startsWith("com.owspfm.elwha.showcase.")) {
        zone.add(binary);
        pending.add(binary);
      }
    }

    final Set<String> refs = new TreeSet<>();
    while (!pending.isEmpty()) {
      final String binary = pending.poll();
      for (final String ref : classRefsIn(binary)) {
        refs.add(ref);
        final boolean panelBuilder =
            ref.contains(".playground.") || ref.startsWith("com.owspfm.elwha.showcase.");
        if (panelBuilder && zone.add(ref)) {
          pending.add(ref);
        }
      }
    }
    return refs;
  }

  private static Set<String> classRefsIn(final String binary) {
    final Path file = classesRoot().resolve(binary.replace('.', '/') + ".class");
    if (!Files.exists(file)) {
      return Set.of();
    }
    final String bytes;
    try {
      bytes = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    final Set<String> refs = new TreeSet<>();
    final Matcher matcher = CLASS_REF.matcher(bytes);
    while (matcher.find()) {
      refs.add(matcher.group().replace('/', '.'));
    }
    return refs;
  }

  private static List<String> compiledClasses() {
    final Path root = classesRoot().resolve("com/owspfm/elwha");
    final List<String> out = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(root)) {
      walk.filter(path -> path.toString().endsWith(".class"))
          .forEach(
              path ->
                  out.add(
                      ROOT_PACKAGE
                          + root.relativize(path)
                              .toString()
                              .replace(".class", "")
                              .replace('/', '.')));
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    out.sort(String::compareTo);
    return out;
  }

  // The compiled-output root the library was loaded from — class files resolve under it by binary
  // name, and the library's own tree hangs off com/owspfm/elwha.
  private static Path classesRoot() {
    try {
      return Path.of(
          ElwhaShowcase.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    } catch (final URISyntaxException e) {
      throw new IllegalStateException("cannot locate the compiled library", e);
    }
  }

  private static Class<?> load(final String binary) {
    try {
      return Class.forName(binary, false, ShowcaseFixture.class.getClassLoader());
    } catch (final ClassNotFoundException | LinkageError e) {
      return null;
    }
  }

  /** The realised surface registered under {@code label}. */
  static JComponent surfaceOf(final String label) {
    return leaf(label).surface;
  }

  /**
   * The one card the content area is currently showing. {@link java.awt.CardLayout} exposes no
   * "current card" accessor, so the visible child is the reading — and the invariant that exactly
   * one child is visible is itself worth asserting.
   */
  static Component visibleCard() {
    final List<Component> showing = new ArrayList<>();
    for (final Component card : showcase().content.getComponents()) {
      if (card.isVisible()) {
        showing.add(card);
      }
    }
    return showing.size() == 1 ? showing.get(0) : null;
  }

  /** Every card currently visible in the content area — one, unless something is wrong. */
  static List<Component> visibleCards() {
    final List<Component> showing = new ArrayList<>();
    for (final Component card : showcase().content.getComponents()) {
      if (card.isVisible()) {
        showing.add(card);
      }
    }
    return showing;
  }

  /** Shows {@code key} and returns the card that became visible. */
  static Component show(final String key) {
    showcase().showCard(key);
    return visibleCard();
  }
}
