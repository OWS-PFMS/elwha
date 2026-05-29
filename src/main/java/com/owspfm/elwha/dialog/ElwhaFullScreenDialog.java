package com.owspfm.elwha.dialog;

import com.owspfm.elwha.surface.ElwhaSurface;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.SpaceScale;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

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

  private final String headline;
  private final JComponent content;

  private ElwhaFullScreenDialog(final Builder b) {
    super(b.dismissibleByEsc, b.onClose);
    this.headline = b.headline;
    this.content = b.content;
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

  // Esc → close with cancel semantics (§9). Enter → confirm is wired in S3 alongside the app bar.
  @Override
  protected void installKeyBindings() {
    if (!isDismissibleByEsc()) {
      return;
    }
    final InputMap im = surface.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    final ActionMap am = surface.getActionMap();
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "elwha-fsd-cancel");
    am.put("elwha-fsd-cancel", action(() -> dismiss(DismissCause.CANCEL)));
  }

  // Fill the layered pane edge-to-edge (the backdrop, when present, is already stretched by the
  // base; full-screen has none).
  @Override
  protected void layoutSurface(final int paneWidth, final int paneHeight) {
    surface.setBounds(0, 0, paneWidth, paneHeight);
    surface.revalidate();
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

  // The centered max-560 column: headline pinned to the top, content below. The column (incl. its
  // 24px padding) is centered horizontally within the frame-filling surface; on a frame narrower
  // than the column max it spans the full width.
  private JComponent buildColumnHost() {
    final JPanel host = new JPanel(new CenteredColumnLayout(CONTENT_COLUMN_PX));
    host.setOpaque(false);

    final JPanel column = new JPanel(new BorderLayout());
    column.setOpaque(false);
    column.setBorder(
        BorderFactory.createEmptyBorder(
            SpaceScale.XL.px(), SpaceScale.XL.px(), SpaceScale.XL.px(), SpaceScale.XL.px()));

    if (headline != null) {
      final JLabel headlineLabel = new JLabel(headline);
      headlineLabel.setFont(TypeRole.TITLE_LARGE.resolve());
      headlineLabel.setForeground(ColorRole.ON_SURFACE.resolve());
      column.add(headlineLabel, BorderLayout.NORTH);
    }

    if (content != null) {
      content.setOpaque(false);
      final JPanel contentHolder = new JPanel(new BorderLayout());
      contentHolder.setOpaque(false);
      contentHolder.setBorder(BorderFactory.createEmptyBorder(SpaceScale.LG.px(), 0, 0, 0));
      contentHolder.add(content, BorderLayout.CENTER);
      column.add(contentHolder, BorderLayout.CENTER);
    }

    host.add(column);
    return host;
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
   * Fluent builder for {@link ElwhaFullScreenDialog}. The top app bar (close affordance + optional
   * confirm action) and edge-to-edge scrolling content are layered on in later Phase-1 stories; the
   * skeleton fills the frame with a headline + content column.
   *
   * @author Charles Bryan (cfb3@uw.edu)
   * @version v0.3.0
   * @since v0.3.0
   */
  public static final class Builder {
    private String headline;
    private JComponent content;
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
