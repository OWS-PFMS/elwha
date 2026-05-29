package com.owspfm.elwha.dialog;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.surface.ElwhaSurface;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.SpaceScale;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * The Material 3 <strong>Full-screen Dialog</strong> primitive — a modal overlay that fills the
 * host frame edge-to-edge for longer-form input flows (multi-field forms, "create event"-style
 * tasks) or narrow frames where a centered {@link ElwhaDialog Basic Dialog} would be cramped. Spec
 * lives in {@code docs/research/elwha-fullscreen-dialog-design.md}; tracks M3 Expressive.
 *
 * <p><strong>Sibling, not a variant.</strong> The anatomy diverges from the Basic Dialog (a top app
 * bar instead of a centered headline, no scrim, 0dp corners, edge-to-edge content), so this is a
 * separate class. The two share the overlay-host lifecycle through the package-private {@link
 * AbstractElwhaDialog} base (host resolution, {@link javax.swing.JLayeredPane#MODAL_LAYER} attach,
 * dismiss/teardown, focus trap + restore, relayout, motion plumbing); this class supplies the
 * full-screen anatomy.
 *
 * <p><strong>Container.</strong> The surface is {@link ColorRole#SURFACE} (the base page surface —
 * it <em>replaces</em> the page rather than floating above it), {@link ShapeScale#NONE} (0dp, flush
 * to the frame), no elevation shadow. It fills the layered pane, so there is no scrim: the surface
 * physically covers all app content, and the {@code AbstractElwhaDialog} focus trap keeps keyboard
 * focus inside. The content sits in a max-{@value #CONTENT_COLUMN_PX}dp column centered
 * horizontally (M3 spec) with {@link SpaceScale#XL} (24px) padding, so the dialog reads correctly
 * at any frame width.
 *
 * <p><strong>Top app bar.</strong> A {@value #APP_BAR_PX}px-tall bar pinned at the top of the
 * column carries the leading close affordance (an {@link ElwhaIconButton} with the {@link
 * MaterialIcons#close()} glyph, dismissing with {@link DismissCause#CANCEL}), the start-aligned
 * headline ({@link TypeRole#TITLE_LARGE}), and an optional trailing confirm text button (dismissing
 * with {@link DismissCause#CONFIRM} after the consumer's listener runs). Esc → close, Enter →
 * confirm — the same hand-wiring the Basic Dialog uses since {@code ElwhaButton} is not a {@code
 * JButton}. An optional 1px {@link ColorRole#OUTLINE_VARIANT} divider sits under the bar.
 *
 * <p><strong>Lifecycle.</strong> {@link #show(java.awt.Component)} resolves the host frame from any
 * component in the tree, attaches the overlay, and returns immediately; the outcome is reported
 * through {@link Builder#onClose(Consumer)} with a {@link DismissCause}. {@link #dismiss()} closes
 * it programmatically.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ElwhaFullScreenDialog extends AbstractElwhaDialog {

  /** Max width of the centered content column (M3 full-screen spec table, §4 F4). */
  static final int CONTENT_COLUMN_PX = 560;

  /** Top app bar height (M3 full-screen spec table, §4 F6). */
  static final int APP_BAR_PX = 56;

  private final String headline;
  private final JComponent content;
  private final ElwhaButton confirmAction;
  private final boolean showDivider;

  // Live overlay state — non-null only while shown.
  private JScrollPane contentScroll;
  private DividerLine scrollDivider;
  private ElwhaIconButton closeButton;

  private ElwhaFullScreenDialog(final Builder b) {
    super(b.dismissibleByEsc, b.onClose);
    this.headline = b.headline;
    this.content = b.content;
    this.confirmAction = b.confirmAction;
    this.showDivider = b.showDivider;
    if (confirmAction != null) {
      // The consumer's own listener was registered before the button reached the builder, so it
      // runs first; this trailing listener then closes the dialog with CONFIRM (§5/§9). Wired once
      // here (not per show) so repeated shows don't stack the close listener.
      confirmAction.addActionListener(e -> dismiss(DismissCause.CONFIRM));
    }
  }

  /**
   * Starts a new fluent builder.
   *
   * @return a fresh {@link Builder}
   * @version v0.3.0
   * @since v0.3.0
   */
  public static Builder builder() {
    return new Builder();
  }

  // ----------------------------------------------------- anatomy hooks

  @Override
  protected JComponent createSurface() {
    final FullScreenSurface s = new FullScreenSurface();
    s.add(buildColumnHost(), BorderLayout.CENTER);
    return s;
  }

  // No scrim — a full-screen dialog fills the frame; the surface physically covers app content.
  @Override
  protected JComponent createBackdrop() {
    return null;
  }

  @Override
  protected String accessibleName() {
    return headline;
  }

  // Esc → close with cancel semantics (§9); Enter → the confirming action (§5). The hand-wired twin
  // of Esc this epic shares with the Basic Dialog, since ElwhaButton isn't a JButton.
  @Override
  protected void installKeyBindings() {
    final InputMap im = surface.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    final ActionMap am = surface.getActionMap();
    if (isDismissibleByEsc()) {
      im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "elwha-fsd-cancel");
      am.put("elwha-fsd-cancel", action(() -> dismiss(DismissCause.CANCEL)));
    }
    if (confirmAction != null) {
      im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "elwha-fsd-confirm");
      am.put("elwha-fsd-confirm", action(confirmAction::doClick));
    }
  }

  // Fill the layered pane edge-to-edge (the backdrop, when present, is already stretched by the
  // base; full-screen has none).
  @Override
  protected void layoutSurface(final int paneWidth, final int paneHeight) {
    surface.setBounds(0, 0, paneWidth, paneHeight);
    surface.revalidate();
    SwingUtilities.invokeLater(this::updateScrollDivider);
  }

  // A full-screen dialog is an input flow, so focus belongs in the form (§9): the first focusable
  // content field, else the close affordance, else (via the base) the surface itself — never the
  // inert background.
  @Override
  protected Component initialFocusTarget() {
    if (content != null) {
      if (content.isFocusable() && content.isEnabled() && !(content instanceof JPanel)) {
        return content;
      }
      final Component field = firstFocusable(content);
      if (field != null) {
        return field;
      }
    }
    return closeButton;
  }

  @Override
  protected void clearTransientState() {
    contentScroll = null;
    scrollDivider = null;
    closeButton = null;
  }

  /**
   * Renders the dialog's surface as a standalone, non-modal component for a <em>static preview</em>
   * (a gallery card, documentation) where {@link #show(java.awt.Component)} would be wrong: there
   * is no modal overlay, focus management, or entrance motion. Each call returns a fresh component.
   *
   * @return a non-modal render of the dialog surface
   * @version v0.3.0
   * @since v0.3.0
   */
  public JComponent renderPreview() {
    final FullScreenSurface preview = new FullScreenSurface();
    preview.add(buildColumnHost(), BorderLayout.CENTER);
    return preview;
  }

  // ----------------------------------------------------- column build

  // The centered max-560 column: top app bar (+ optional divider) pinned, content below. The column
  // (incl. its 24px padding) is centered horizontally within the frame-filling surface; on a frame
  // narrower than the column max it spans the full width.
  private JComponent buildColumnHost() {
    final JPanel host = new JPanel(new CenteredColumnLayout(CONTENT_COLUMN_PX));
    host.setOpaque(false);

    final JPanel column = new JPanel(new BorderLayout());
    column.setOpaque(false);
    column.setBorder(
        BorderFactory.createEmptyBorder(
            SpaceScale.XL.px(), SpaceScale.XL.px(), SpaceScale.XL.px(), SpaceScale.XL.px()));

    // The divider is always mounted under the bar; its visibility is showDivider OR'd with "content
    // is scrolling" (M3 scroll affordance). Invisible → BorderLayout reserves no space for it.
    final DividerLine divider = new DividerLine();
    divider.setVisible(showDivider);
    this.scrollDivider = divider;

    final JPanel top = new JPanel(new BorderLayout());
    top.setOpaque(false);
    top.add(buildAppBar(), BorderLayout.CENTER);
    top.add(divider, BorderLayout.SOUTH);
    column.add(top, BorderLayout.NORTH);

    if (content != null) {
      content.setOpaque(false);
      // Edge-to-edge content within the column; scrolls vertically when taller than the frame
      // leaves room (the app bar stays pinned). No horizontal scrollbar — content must reflow.
      final JScrollPane scroll =
          new JScrollPane(
              content,
              JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
              JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      scroll.setOpaque(false);
      scroll.getViewport().setOpaque(false);
      scroll.setBorder(BorderFactory.createEmptyBorder(SpaceScale.LG.px(), 0, 0, 0));
      scroll.getViewport().addChangeListener(e -> updateScrollDivider());
      this.contentScroll = scroll;
      column.add(scroll, BorderLayout.CENTER);
    }

    host.add(column);
    return host;
  }

  // Divider visible when the consumer requested it OR the content is taller than its viewport (i.e.
  // it's scrolling) — the M3 scroll affordance, mirroring the Basic Dialog's scroll divider.
  private void updateScrollDivider() {
    if (scrollDivider == null) {
      return;
    }
    final boolean scrolls =
        contentScroll != null
            && contentScroll.getViewport().getViewSize().height
                > contentScroll.getViewport().getExtentSize().height;
    scrollDivider.setVisible(showDivider || scrolls);
  }

  // The top app bar (§5): leading close affordance → start-aligned headline → trailing confirm.
  // LINE_START / LINE_END (not WEST / EAST) so the edges mirror for RTL (§10) once the base applies
  // the component orientation; the headline's gap-from-close flips to the trailing side to match.
  private JComponent buildAppBar() {
    final AppBar bar = new AppBar();

    final ElwhaIconButton close = ElwhaIconButton.standardIconButton(MaterialIcons.close());
    close.setToolTipText("Close");
    close.addActionListener(e -> dismiss(DismissCause.CANCEL));
    this.closeButton = close;
    bar.add(verticalCenter(close), BorderLayout.LINE_START);

    if (headline != null) {
      final JLabel title = new JLabel(headline);
      title.setFont(TypeRole.TITLE_LARGE.resolve());
      title.setForeground(ColorRole.ON_SURFACE.resolve());
      final int gap = SpaceScale.SM.px();
      title.setBorder(
          orientation.isLeftToRight()
              ? BorderFactory.createEmptyBorder(0, gap, 0, 0)
              : BorderFactory.createEmptyBorder(0, 0, 0, gap));
      bar.add(title, BorderLayout.CENTER);
    }

    if (confirmAction != null) {
      bar.add(verticalCenter(confirmAction), BorderLayout.LINE_END);
    }
    return bar;
  }

  // Wraps a fixed-size control so BorderLayout's full-height region stretch doesn't deform it — the
  // GridBag centers the control vertically (and horizontally) at its preferred size.
  private static JPanel verticalCenter(final JComponent c) {
    final JPanel wrap = new JPanel(new GridBagLayout());
    wrap.setOpaque(false);
    wrap.add(c);
    return wrap;
  }

  // The frame-filling container: SURFACE / 0dp / no shadow. Focusable so the base focus trap has a
  // last-resort target when the dialog carries no focusable content.
  private static final class FullScreenSurface extends ElwhaSurface {
    FullScreenSurface() {
      setLayout(new BorderLayout());
      setSurfaceRole(ColorRole.SURFACE);
      setShape(ShapeScale.NONE);
      setBorderRole(null);
      setElevation(0);
      setFocusable(true);
    }

    // Reports AccessibleRole.DIALOG so assistive tech announces this as a dialog (§9); the
    // accessible name is set to the headline by the base at show time.
    @Override
    public AccessibleContext getAccessibleContext() {
      if (accessibleContext == null) {
        accessibleContext =
            new AccessibleJComponent() {
              @Override
              public AccessibleRole getAccessibleRole() {
                return AccessibleRole.DIALOG;
              }
            };
      }
      return accessibleContext;
    }
  }

  // The app bar panel — forces the M3 56px height regardless of its (shorter) children.
  private static final class AppBar extends JPanel {
    AppBar() {
      super(new BorderLayout());
      setOpaque(false);
    }

    @Override
    public Dimension getPreferredSize() {
      final Dimension d = super.getPreferredSize();
      return new Dimension(d.width, Math.max(APP_BAR_PX, d.height));
    }
  }

  // A 1px OUTLINE_VARIANT line under the app bar (and, in a later story, above scrolled content).
  private static final class DividerLine extends JComponent {
    DividerLine() {
      setOpaque(false);
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(0, 1);
    }

    @Override
    protected void paintComponent(final Graphics g) {
      g.setColor(ColorRole.OUTLINE_VARIANT.resolve());
      g.fillRect(0, 0, getWidth(), 1);
    }
  }

  // Lays out a single child as a top-anchored column: width clamped to maxColumnPx (or the full
  // parent width when narrower), centered horizontally, full parent height.
  private static final class CenteredColumnLayout implements LayoutManager {
    private final int maxColumnPx;

    CenteredColumnLayout(final int maxColumnPx) {
      this.maxColumnPx = maxColumnPx;
    }

    @Override
    public void addLayoutComponent(final String name, final java.awt.Component comp) {}

    @Override
    public void removeLayoutComponent(final java.awt.Component comp) {}

    @Override
    public Dimension preferredLayoutSize(final Container parent) {
      if (parent.getComponentCount() == 0) {
        return new Dimension(0, 0);
      }
      final Dimension childPref = parent.getComponent(0).getPreferredSize();
      return new Dimension(Math.min(maxColumnPx, childPref.width), childPref.height);
    }

    @Override
    public Dimension minimumLayoutSize(final Container parent) {
      return new Dimension(0, 0);
    }

    @Override
    public void layoutContainer(final Container parent) {
      if (parent.getComponentCount() == 0) {
        return;
      }
      final int w = parent.getWidth();
      final int h = parent.getHeight();
      final int columnW = Math.min(maxColumnPx, w);
      final int x = (w - columnW) / 2;
      parent.getComponent(0).setBounds(x, 0, columnW, h);
    }
  }

  /**
   * Fluent builder for {@link ElwhaFullScreenDialog}. Edge-to-edge scrolling content and a11y / RTL
   * refinements are layered on in later Phase-1 stories; this builds the frame-filling dialog with
   * a top app bar (close → headline → optional confirm) over a content column.
   *
   * @author Charles Bryan (cfb3@uw.edu)
   * @version v0.3.0
   * @since v0.3.0
   */
  public static final class Builder {
    private String headline;
    private JComponent content;
    private ElwhaButton confirmAction;
    private boolean showDivider;
    private boolean dismissibleByEsc = true;
    private Consumer<DismissCause> onClose;

    private Builder() {}

    /**
     * Sets the headline — the app-bar title and the dialog's accessible name. Effectively required.
     *
     * @param text the headline text
     * @return {@code this}
     * @version v0.3.0
     * @since v0.3.0
     */
    public Builder headline(final String text) {
      this.headline = text;
      return this;
    }

    /**
     * Sets the primary content component — typically a form. The dialog hosts it transparently and
     * does not recolor it. (Edge-to-edge scrolling when the content is taller than the frame is
     * added in a later Phase-1 story.)
     *
     * @param content the content component, or {@code null} for none
     * @return {@code this}
     * @version v0.3.0
     * @since v0.3.0
     */
    public Builder content(final JComponent content) {
      this.content = content;
      return this;
    }

    /**
     * Sets the optional confirming action — the trailing app-bar text button (M3 default; e.g.
     * "Save"), and the target of the Enter key. Firing it runs the consumer's own listener, then
     * closes the dialog with {@link DismissCause#CONFIRM}. When absent, the app bar is close +
     * headline only.
     *
     * @param button the confirming action, or {@code null} for none
     * @return {@code this}
     * @version v0.3.0
     * @since v0.3.0
     */
    public Builder confirmAction(final ElwhaButton button) {
      this.confirmAction = button;
      return this;
    }

    /**
     * Whether a 1px divider is drawn under the top app bar. Default {@code false}.
     *
     * @param v whether to show the app-bar divider
     * @return {@code this}
     * @version v0.3.0
     * @since v0.3.0
     */
    public Builder showDivider(final boolean v) {
      this.showDivider = v;
      return this;
    }

    /**
     * Whether the Escape key dismisses the dialog (cancel semantics). Default {@code true}.
     *
     * @param v whether Esc dismisses
     * @return {@code this}
     * @version v0.3.0
     * @since v0.3.0
     */
    public Builder dismissibleByEsc(final boolean v) {
      this.dismissibleByEsc = v;
      return this;
    }

    /**
     * Sets the close hook, fired after the dialog closes with the cause.
     *
     * @param cb the callback, or {@code null} for none
     * @return {@code this}
     * @version v0.3.0
     * @since v0.3.0
     */
    public Builder onClose(final Consumer<DismissCause> cb) {
      this.onClose = cb;
      return this;
    }

    /**
     * Builds the dialog.
     *
     * @return a new {@link ElwhaFullScreenDialog}
     * @version v0.3.0
     * @since v0.3.0
     */
    public ElwhaFullScreenDialog build() {
      return new ElwhaFullScreenDialog(this);
    }
  }
}
