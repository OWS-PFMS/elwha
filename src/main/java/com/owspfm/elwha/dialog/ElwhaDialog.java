package com.owspfm.elwha.dialog;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.surface.ElwhaSurface;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Easing;
import com.owspfm.elwha.theme.MorphAnimator;
import com.owspfm.elwha.theme.ShadowPainter;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.SpaceScale;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.function.Consumer;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * The Material 3 <strong>Basic Dialog</strong> primitive — a token-themed modal surface with typed
 * anatomy slots (icon → headline → supporting text → content → actions) shown as an in-window
 * overlay on the host frame's {@link JLayeredPane}. Spec lives in {@code
 * docs/research/elwha-dialog-design.md}; tracks M3 Expressive post-May-2025.
 *
 * <p><strong>Why not {@code JOptionPane}.</strong> {@code ElwhaButton extends JComponent}, so
 * {@link JRootPane#setDefaultButton(javax.swing.JButton)} rejects it and {@code JOptionPane} is
 * fully closed to Elwha actions. {@code ElwhaDialog} is the custom path that formalizes the
 * hand-rolled About-dialog chrome from #252 — including the Esc-to-dismiss and Enter-to-confirm key
 * wiring done by hand (the About dialog already proved the Esc half).
 *
 * <p><strong>Modality mechanism (design doc §2, locked Phase 1 S1).</strong> Rather than a separate
 * top-level {@code JDialog}, the dialog is an overlay installed on {@code
 * SwingUtilities.getRootPane(parent).getLayeredPane()} at {@link JLayeredPane#MODAL_LAYER}: a
 * full-bounds scrim layer that consumes all input not landing on the dialog surface, plus the
 * centered surface on top. Only this path can paint the M3 scrim over the app content and run the
 * scale-in entrance (Phase 2) in the same window; a {@code JDialog} can do neither. Input blocking
 * is the scrim-consumes-events pattern plus the focus trap — the same approach every web dialog
 * uses. Reuses the {@code ElwhaFabAnchor} (#205) / {@code ElwhaBadgeAnchor} layered-pane glue.
 *
 * <p><strong>Lifecycle.</strong> {@link #show(Component)} resolves the host frame from any
 * component in the tree (mirroring {@code JOptionPane.showXxx(parentComponent, ...)}), attaches the
 * overlay, and returns immediately — the dialog is non-blocking and reports its outcome through
 * {@link Builder#onClose(Consumer)} with a {@link DismissCause}. {@link #dismiss()} closes it
 * programmatically.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.3.0
 * @since v0.3.0
 */
public final class ElwhaDialog {

  /** M3 dialog container elevation — Level 3 (design doc §6). */
  static final int ELEVATION = 3;

  /** Entrance / exit duration — M3 medium2 (300 ms), design doc §13. */
  static final int MOTION_MS = MorphAnimator.MEDIUM2_MS;

  /** Container scale at the start of the entrance (and end of the exit); grows to 1.0 (§13). */
  static final float SCALE_FROM = 0.8f;

  private final Icon icon;
  private final String headline;
  private final String supportingText;
  private final JComponent content;
  private final ElwhaButton confirmAction;
  private final ElwhaButton alternateAction;
  private final ElwhaButton cancelAction;
  private final boolean dismissibleByScrim;
  private final boolean dismissibleByEsc;
  private final Consumer<DismissCause> onClose;

  // Live overlay state — non-null only while shown.
  private JLayeredPane layeredPane;
  private Scrim scrim;
  private DialogSurface surface;
  private JScrollPane contentScroll;
  private JComponent scrollDivider;
  private ComponentListener relayoutListener;
  private PropertyChangeListener focusTrap;
  private Window hostWindow;
  private ComponentOrientation orientation = ComponentOrientation.LEFT_TO_RIGHT;
  private Component focusOwnerBeforeShow;
  private MorphAnimator entrance;
  private float motionProgress = 1f;
  private DismissCause exitCause;
  private boolean closing;

  private ElwhaDialog(final Builder b) {
    this.icon = b.icon;
    this.headline = b.headline;
    this.supportingText = b.supportingText;
    this.content = b.content;
    this.confirmAction = b.confirmAction;
    this.alternateAction = b.alternateAction;
    this.cancelAction = b.cancelAction;
    this.dismissibleByScrim = b.dismissibleByScrim;
    this.dismissibleByEsc = b.dismissibleByEsc;
    this.onClose = b.onClose;
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

  /**
   * Shows the dialog as a modal overlay on the host frame resolved from {@code parent}. Returns
   * immediately; the outcome is reported through {@link Builder#onClose(Consumer)}.
   *
   * @param parent any component in the target window's tree; used to resolve the host root pane and
   *     to restore focus on close
   * @throws NullPointerException if {@code parent} is {@code null}
   * @throws IllegalStateException if {@code parent} is not yet in a realized window
   * @version v0.3.0
   * @since v0.3.0
   */
  public void show(final Component parent) {
    Objects.requireNonNull(parent, "parent");
    final JRootPane root = SwingUtilities.getRootPane(parent);
    if (root == null) {
      throw new IllegalStateException("parent is not in a realized window with a root pane");
    }
    this.layeredPane = root.getLayeredPane();
    this.hostWindow = SwingUtilities.getWindowAncestor(root);
    this.orientation = parent.getComponentOrientation();
    this.focusOwnerBeforeShow =
        KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();

    this.motionProgress = 0f;
    this.closing = false;
    this.scrim = new Scrim();
    this.surface = new DialogSurface();
    buildSurfaceContent();
    installKeyBindings();
    surface.applyComponentOrientation(orientation);
    if (headline != null) {
      surface.getAccessibleContext().setAccessibleName(headline);
    }

    layeredPane.add(scrim, JLayeredPane.MODAL_LAYER);
    layeredPane.add(surface, JLayeredPane.MODAL_LAYER);
    layeredPane.moveToFront(surface);

    relayoutListener =
        new ComponentAdapter() {
          @Override
          public void componentResized(final ComponentEvent e) {
            relayout();
          }
        };
    layeredPane.addComponentListener(relayoutListener);
    installFocusTrap();

    relayout();

    // Entrance: scrim fade + container scale-in + fade (§13), eased emphasized-decelerate. One
    // animator hosted on the surface drives both layers; reduced motion snaps to the end state.
    entrance = new MorphAnimator(surface, MOTION_MS);
    entrance.addProgressListener(this::onMotionTick);
    entrance.snapTo(0f);
    entrance.start();

    layeredPane.revalidate();
    layeredPane.repaint();

    SwingUtilities.invokeLater(this::focusInitial);
  }

  /**
   * Closes the dialog programmatically (cancel-equivalent), reporting {@link
   * DismissCause#PROGRAMMATIC} to {@link Builder#onClose(Consumer)}. A no-op if not currently
   * shown.
   *
   * @version v0.3.0
   * @since v0.3.0
   */
  public void dismiss() {
    dismiss(DismissCause.PROGRAMMATIC);
  }

  // Begins closing: runs the exit motion, then tears the overlay down when it reaches 0 (reduced
  // motion snaps + fires the listener synchronously, so this path still completes). Re-entry
  // guarded
  // so an action listener that also calls dismiss() — or a scrim click mid-exit — can't
  // double-fire.
  void dismiss(final DismissCause cause) {
    if (closing || layeredPane == null) {
      return;
    }
    closing = true;
    exitCause = cause;
    if (entrance != null) {
      entrance.reverse();
    } else {
      performTeardown(cause);
    }
  }

  // Per-tick motion update: ease the linear progress, push it to the scrim (the surface is the
  // animator's own repaint host), and finish teardown once the exit has fully collapsed.
  private void onMotionTick() {
    if (entrance == null) {
      return;
    }
    motionProgress = Easing.EMPHASIZED_DECELERATE.ease(entrance.progress());
    if (scrim != null) {
      scrim.repaint();
    }
    if (closing && entrance.target() == 0f && entrance.progress() == 0f) {
      performTeardown(exitCause);
    }
  }

  // Detaches the overlay, restores focus, and fires onClose. Idempotent — the exit-motion
  // completion
  // tick can land on progress 0 more than once.
  private void performTeardown(final DismissCause cause) {
    if (layeredPane == null) {
      return;
    }
    if (entrance != null) {
      entrance.stop();
    }
    if (focusTrap != null) {
      KeyboardFocusManager.getCurrentKeyboardFocusManager()
          .removePropertyChangeListener("focusOwner", focusTrap);
    }
    layeredPane.removeComponentListener(relayoutListener);
    layeredPane.remove(scrim);
    layeredPane.remove(surface);
    layeredPane.revalidate();
    layeredPane.repaint();

    final Component toRestore = focusOwnerBeforeShow;
    final JLayeredPane closed = layeredPane;
    layeredPane = null;
    scrim = null;
    surface = null;
    contentScroll = null;
    scrollDivider = null;
    relayoutListener = null;
    focusTrap = null;
    entrance = null;
    hostWindow = null;
    focusOwnerBeforeShow = null;
    closing = false;

    if (toRestore != null) {
      SwingUtilities.invokeLater(toRestore::requestFocusInWindow);
    } else {
      closed.repaint();
    }

    if (onClose != null) {
      onClose.accept(cause);
    }
  }

  private void buildSurfaceContent() {
    populateSurface(surface, availableContentWidth());
  }

  // Builds the 24px-padded body — icon/headline/supporting header pinned NORTH, optional content
  // slot CENTER, action row SOUTH — into the given surface. The icon-present centering rule (§7) is
  // the single layout conditional. Shared by show() and renderPreview().
  private void populateSurface(final DialogSurface target, final int contentWidth) {
    final boolean centered = icon != null;

    final JPanel body = new JPanel(new BorderLayout());
    body.setOpaque(false);
    body.setBorder(
        BorderFactory.createEmptyBorder(
            SpaceScale.XL.px(), SpaceScale.XL.px(), SpaceScale.XL.px(), SpaceScale.XL.px()));

    body.add(buildHeader(centered, contentWidth), BorderLayout.NORTH);

    if (content != null) {
      body.add(buildContentScroll(), BorderLayout.CENTER);
    }

    final JComponent south = buildSouth();
    if (south != null) {
      body.add(south, BorderLayout.SOUTH);
    }

    target.add(body, BorderLayout.CENTER);
  }

  /**
   * Renders the dialog's surface — container + slots + action row — as a standalone, non-modal
   * component. For embedding a <em>static preview</em> (a gallery card, documentation) where {@link
   * #show(Component)} would be wrong: there is no modal overlay, scrim, focus management, or
   * entrance motion, and the surface is laid out at its natural size within the 280–560px band.
   * Each call returns a fresh component. Not a substitute for {@link #show(Component)} — that
   * presents the dialog for real.
   *
   * @return a non-modal render of the dialog surface
   * @version v0.3.0
   * @since v0.3.0
   */
  public JComponent renderPreview() {
    final DialogSurface preview = new DialogSurface();
    populateSurface(preview, availableContentWidth());
    return preview;
  }

  // The optional content slot, wrapped so it scrolls (vertically only) when taller than the space
  // the host frame leaves the dialog — the headline/icon (NORTH) and action row (SOUTH) stay pinned
  // while only this CENTER region scrolls (M3 scrollable-content behavior).
  private JComponent buildContentScroll() {
    content.setOpaque(false);
    final JScrollPane scroll =
        new JScrollPane(
            content,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.setOpaque(false);
    scroll.getViewport().setOpaque(false);
    scroll.setBorder(BorderFactory.createEmptyBorder(SpaceScale.LG.px(), 0, 0, 0));
    this.contentScroll = scroll;
    return scroll;
  }

  // The bottom region: the action row, preceded by a 1px scroll divider (M3 affordance) shown only
  // when the content slot is actually scrolling. No divider without a scrolling content slot.
  private JComponent buildSouth() {
    final JComponent actions = buildActionRow();
    if (actions == null) {
      return null;
    }
    if (content == null) {
      return actions;
    }
    final JPanel south = new JPanel(new BorderLayout());
    south.setOpaque(false);
    final ScrollDivider divider = new ScrollDivider();
    divider.setVisible(false);
    this.scrollDivider = divider;
    south.add(divider, BorderLayout.NORTH);
    south.add(actions, BorderLayout.CENTER);
    contentScroll.getViewport().addChangeListener(e -> updateScrollDivider());
    return south;
  }

  // Divider shows iff the content's natural height exceeds the viewport — i.e. it's scrolling.
  private void updateScrollDivider() {
    if (scrollDivider == null || contentScroll == null) {
      return;
    }
    final boolean scrolls =
        contentScroll.getViewport().getViewSize().height
            > contentScroll.getViewport().getExtentSize().height;
    scrollDivider.setVisible(scrolls);
  }

  // The trailing-justified action row (§5). M3 order is cancel (leading) → alternate → confirm
  // (trailing); a trailing FlowLayout plus that add order parks the confirming action at the
  // trailing edge regardless of which roles are present. 8px between buttons, 24px above the row.
  // Returns null when no action role is set.
  private JComponent buildActionRow() {
    if (confirmAction == null && alternateAction == null && cancelAction == null) {
      return null;
    }
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.TRAILING, SpaceScale.SM.px(), 0));
    row.setOpaque(false);
    row.setBorder(BorderFactory.createEmptyBorder(SpaceScale.XL.px(), 0, 0, 0));
    addAction(row, cancelAction, DismissCause.CANCEL);
    addAction(row, alternateAction, DismissCause.ALTERNATE);
    addAction(row, confirmAction, DismissCause.CONFIRM);
    return row;
  }

  // Adds one action button to the row with the dialog's close-after-fire listener. The consumer's
  // own listener was registered before the button reached the builder, so it runs first; this
  // trailing listener then closes the dialog with the role's cause (§9).
  private void addAction(final JPanel row, final ElwhaButton button, final DismissCause cause) {
    if (button == null) {
      return;
    }
    button.addActionListener(e -> dismiss(cause));
    row.add(button);
  }

  // The width (inside the shadow reserve + 24px padding) the dialog's body will actually get,
  // derived from the realized host width and the 280-560px clamp. Supporting text wraps to this so
  // it can't overflow a body the host has clamped narrower than the 560px max and clip
  // horizontally.
  private int availableContentWidth() {
    final Insets shadow = ShadowPainter.shadowInsets(ELEVATION);
    final int shadowH = shadow.left + shadow.right;
    final int maxBodyTotal = DialogSurface.MAX_BODY_WIDTH + shadowH;
    final int avail = layeredPane != null ? layeredPane.getWidth() : 0;
    final int bodyTotal =
        avail > 0 ? Math.min(avail - 2 * SpaceScale.XL.px(), maxBodyTotal) : maxBodyTotal;
    final int bodyWidth = Math.max(DialogSurface.MIN_BODY_WIDTH, bodyTotal - shadowH);
    return Math.max(0, bodyWidth - 2 * SpaceScale.XL.px());
  }

  // The icon → headline → supporting-text stack. Vertical box; every child shares one horizontal
  // alignment (center when an icon is present, leading otherwise) so the column reads as a unit.
  private JComponent buildHeader(final boolean centered, final int contentWidth) {
    final float leading =
        orientation.isLeftToRight() ? Component.LEFT_ALIGNMENT : Component.RIGHT_ALIGNMENT;
    final float alignX = centered ? Component.CENTER_ALIGNMENT : leading;
    final JPanel header = new JPanel();
    header.setOpaque(false);
    header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
    header.setAlignmentX(Component.LEFT_ALIGNMENT);

    if (icon != null) {
      final JLabel iconLabel = new JLabel(icon);
      iconLabel.setAlignmentX(alignX);
      header.add(iconLabel);
      header.add(Box.createVerticalStrut(SpaceScale.LG.px()));
    }

    if (headline != null) {
      final JLabel headlineLabel = new JLabel(headline);
      headlineLabel.setFont(TypeRole.HEADLINE_SMALL.resolve());
      headlineLabel.setForeground(ColorRole.ON_SURFACE.resolve());
      headlineLabel.setHorizontalAlignment(
          centered ? SwingConstants.CENTER : SwingConstants.LEADING);
      headlineLabel.setAlignmentX(alignX);
      header.add(headlineLabel);
    }

    if (supportingText != null) {
      if (headline != null) {
        header.add(Box.createVerticalStrut(SpaceScale.LG.px()));
      }
      final JLabel supporting = new JLabel(wrapHtml(supportingText, centered, contentWidth));
      supporting.setFont(TypeRole.BODY_MEDIUM.resolve());
      supporting.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
      supporting.setAlignmentX(alignX);
      header.add(supporting);
    }

    return header;
  }

  // Wraps supporting prose so it word-wraps at the dialog's actual content width rather than
  // forcing
  // the dialog wide on one line or overflowing a host-clamped narrow body. {@code contentWidth} is
  // the real available column from availableContentWidth().
  private static String wrapHtml(
      final String text, final boolean centered, final int contentWidth) {
    final String align = centered ? "text-align:center;" : "";
    return "<html><div style='" + align + "width:" + contentWidth + "px'>" + text + "</div></html>";
  }

  // Esc → cancel semantics (§5/§9): fires the cancel action when present (so its consumer listener
  // runs and the close cause is CANCEL), else closes with DismissCause.ESC. Enter → the confirming
  // action — the hand-wired twin of Esc this epic exists to formalize, since ElwhaButton isn't a
  // JButton and can't be a root-pane default button.
  private void installKeyBindings() {
    final InputMap im = surface.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    final ActionMap am = surface.getActionMap();

    if (dismissibleByEsc) {
      im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "elwha-dialog-cancel");
      am.put(
          "elwha-dialog-cancel",
          action(
              () -> {
                if (cancelAction != null) {
                  cancelAction.doClick();
                } else {
                  dismiss(DismissCause.ESC);
                }
              }));
    }
    if (confirmAction != null) {
      im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "elwha-dialog-confirm");
      am.put("elwha-dialog-confirm", action(confirmAction::doClick));
    }
  }

  private static AbstractAction action(final Runnable body) {
    return new AbstractAction() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        body.run();
      }
    };
  }

  // Keyboard focus trap (§10): while shown, if focus escapes the dialog surface to the now-inert
  // scrimmed background (still inside the host window), pull it back. Focus moving to a different
  // window is left alone — the host window simply lost activation.
  private void installFocusTrap() {
    focusTrap =
        evt -> {
          final Object next = evt.getNewValue();
          if (!(next instanceof Component) || surface == null) {
            return;
          }
          final Component owner = (Component) next;
          if (SwingUtilities.isDescendingFrom(owner, surface)) {
            return;
          }
          if (SwingUtilities.getWindowAncestor(owner) != hostWindow) {
            return;
          }
          SwingUtilities.invokeLater(this::focusInitial);
        };
    KeyboardFocusManager.getCurrentKeyboardFocusManager()
        .addPropertyChangeListener("focusOwner", focusTrap);
  }

  // Initial / recovered focus target (§10): the confirming action, else the first focusable
  // descendant (a content field), else the surface itself — never the inert background.
  private void focusInitial() {
    if (surface == null) {
      return;
    }
    if (confirmAction != null && confirmAction.requestFocusInWindow()) {
      return;
    }
    final Component first = firstFocusable(surface);
    if (first != null) {
      first.requestFocusInWindow();
    } else {
      surface.requestFocusInWindow();
    }
  }

  // Depth-first search for the first focus-accepting descendant, skipping the surface itself.
  private static Component firstFocusable(final Container root) {
    for (final Component child : root.getComponents()) {
      if (child.isFocusable()
          && child.isEnabled()
          && child.isVisible()
          && child.isDisplayable()
          && !(child instanceof JPanel)) {
        return child;
      }
      if (child instanceof Container) {
        final Component nested = firstFocusable((Container) child);
        if (nested != null) {
          return nested;
        }
      }
    }
    return null;
  }

  // Sizes + centers the surface inside the layered pane (respecting the M3 24px side / 80px
  // top-bottom insets) and stretches the scrim over the full layered pane. Re-run on host resize.
  private void relayout() {
    if (layeredPane == null) {
      return;
    }
    final int lpW = layeredPane.getWidth();
    final int lpH = layeredPane.getHeight();
    scrim.setBounds(0, 0, lpW, lpH);

    final Dimension pref = surface.getPreferredSize();
    final int maxW = Math.max(0, lpW - 2 * SpaceScale.XL.px());
    // The 80px top/bottom margin is aesthetic breathing room on a roomy window, but on a short
    // window it must yield so the dialog can use the available height instead of collapsing its
    // (scrollable) content to nothing. Shrink it proportionally below the threshold.
    final int verticalInset = Math.min(MIN_VERTICAL_INSET, lpH / 10);
    final int maxH = Math.max(0, lpH - 2 * verticalInset);
    final int w = Math.min(pref.width, maxW);
    final int h = Math.min(pref.height, maxH);
    surface.setBounds((lpW - w) / 2, (lpH - h) / 2, w, h);
    surface.revalidate();
    SwingUtilities.invokeLater(this::updateScrollDivider);
  }

  /**
   * Minimum top/bottom margin between the dialog body and the frame edge (MDC {@code Dialog.md}).
   */
  private static final int MIN_VERTICAL_INSET = 80;

  /** Why the dialog closed — reported to {@link Builder#onClose(Consumer)}. */
  public enum DismissCause {
    /** A confirming action fired. */
    CONFIRM,
    /** A cancelling action fired, or Esc was pressed with cancel semantics. */
    CANCEL,
    /** An alternate action fired. */
    ALTERNATE,
    /** The scrim was clicked while scrim-dismiss was enabled. */
    SCRIM,
    /** The Escape key was pressed while Esc-dismiss was enabled. */
    ESC,
    /** {@link ElwhaDialog#dismiss()} was called. */
    PROGRAMMATIC
  }

  /**
   * Fluent builder for {@link ElwhaDialog}. The anatomy parts are optional slots; a basic alert and
   * a destructive confirm are the same class with different slots filled.
   *
   * @author Charles Bryan (cfb3@uw.edu)
   * @version v0.3.0
   * @since v0.3.0
   */
  public static final class Builder {
    private Icon icon;
    private String headline;
    private String supportingText;
    private JComponent content;
    private ElwhaButton confirmAction;
    private ElwhaButton alternateAction;
    private ElwhaButton cancelAction;
    private boolean dismissibleByScrim = true;
    private boolean dismissibleByEsc = true;
    private Consumer<DismissCause> onClose;

    private Builder() {}

    /**
     * Sets the optional leading icon. When present, the icon, headline, and supporting text are
     * center-aligned (M3 rule); absent, they are start-aligned.
     *
     * @param icon the icon, or {@code null} for none
     * @return {@code this}
     * @version v0.3.0
     * @since v0.3.0
     */
    public Builder icon(final Icon icon) {
      this.icon = icon;
      return this;
    }

    /**
     * Sets the headline — the dialog's accessible name. Effectively required.
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
     * Sets the optional supporting text shown below the headline.
     *
     * @param text the supporting prose, or {@code null} for none
     * @return {@code this}
     * @version v0.3.0
     * @since v0.3.0
     */
    public Builder supportingText(final String text) {
      this.supportingText = text;
      return this;
    }

    /**
     * Sets the optional arbitrary content component (a form, a list). Scrolls when taller than the
     * available frame height.
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
     * Sets the confirming action — the trailing-most (rightmost in LTR) button, and the target of
     * the Enter key. Firing it (click or Enter) runs the consumer's own listener, then closes the
     * dialog with {@link DismissCause#CONFIRM}. Pass any {@code ElwhaButton}; M3's default is a
     * text button, but a filled / tonal one for emphasis is allowed.
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
     * Sets the optional alternate action — the middle button, between cancel and confirm. Firing it
     * closes the dialog with {@link DismissCause#ALTERNATE}.
     *
     * @param button the alternate action, or {@code null} for none
     * @return {@code this}
     * @version v0.3.0
     * @since v0.3.0
     */
    public Builder alternateAction(final ElwhaButton button) {
      this.alternateAction = button;
      return this;
    }

    /**
     * Sets the cancelling action — the leading-most (leftmost in LTR) button. Firing it (or Esc,
     * when this action is present) runs the consumer's own listener, then closes the dialog with
     * {@link DismissCause#CANCEL}.
     *
     * @param button the cancel action, or {@code null} for none
     * @return {@code this}
     * @version v0.3.0
     * @since v0.3.0
     */
    public Builder cancelAction(final ElwhaButton button) {
      this.cancelAction = button;
      return this;
    }

    /**
     * Whether clicking the scrim dismisses the dialog. Default {@code true}; set {@code false} for
     * destructive / required-decision dialogs.
     *
     * @param v whether scrim-click dismisses
     * @return {@code this}
     * @version v0.3.0
     * @since v0.3.0
     */
    public Builder dismissibleByScrim(final boolean v) {
      this.dismissibleByScrim = v;
      return this;
    }

    /**
     * Whether the Escape key dismisses the dialog. Default {@code true}.
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
     * @return a new {@link ElwhaDialog}
     * @version v0.3.0
     * @since v0.3.0
     */
    public ElwhaDialog build() {
      return new ElwhaDialog(this);
    }
  }

  // A 1px OUTLINE_VARIANT line separating scrolled content from the pinned action row.
  private static final class ScrollDivider extends JComponent {
    ScrollDivider() {
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

  // The dialog body: an ElwhaSurface (managed shadow reserve + rounded-corner child clip) painting
  // the SURFACE_CONTAINER_HIGH / 28px / Level-3 container. Width is clamped to the M3 280-560px
  // band; height is content-driven and capped by the host frame at layout time.
  private final class DialogSurface extends ElwhaSurface {
    private static final int MIN_BODY_WIDTH = 280;
    private static final int MAX_BODY_WIDTH = 560;

    DialogSurface() {
      setLayout(new BorderLayout());
      setSurfaceRole(ColorRole.SURFACE_CONTAINER_HIGH);
      setShape(ShapeScale.XL);
      setBorderRole(null);
      setElevation(ELEVATION);
      setFocusable(true);
    }

    // Entrance/exit motion (§13): scale the whole surface (background + children) about its center
    // from SCALE_FROM → 1.0 and fade 0 → 1, both keyed to the eased motionProgress. At full
    // progress
    // it's a plain live paint — no transform or buffer cost on the steady state.
    //
    // During the tween the surface is rasterized once at full size into a device-resolution buffer,
    // and only that bitmap is scaled. Scaling the *live* component instead re-rasterizes its text
    // at
    // the animated scale every frame, so glyph advances snap to the pixel grid differently per
    // frame
    // and the words visibly shuffle left/right; rendering once at a stable scale removes that.
    @Override
    public void paint(final Graphics g) {
      final float p = Math.max(0f, Math.min(1f, motionProgress));
      if (p >= 1f) {
        super.paint(g);
        return;
      }
      final int w = getWidth();
      final int h = getHeight();
      if (w <= 0 || h <= 0) {
        return;
      }
      final AffineTransform tx = ((Graphics2D) g).getTransform();
      final double sx = tx.getScaleX() > 0 ? tx.getScaleX() : 1.0;
      final double sy = tx.getScaleY() > 0 ? tx.getScaleY() : 1.0;
      final int deviceW = Math.max(1, (int) Math.ceil(w * sx));
      final int deviceH = Math.max(1, (int) Math.ceil(h * sy));
      final BufferedImage buffer = new BufferedImage(deviceW, deviceH, BufferedImage.TYPE_INT_ARGB);
      final Graphics2D bg = buffer.createGraphics();
      try {
        bg.scale(sx, sy);
        super.paint(bg);
      } finally {
        bg.dispose();
      }

      final Graphics2D g2 = (Graphics2D) g.create();
      try {
        final float scale = SCALE_FROM + (1f - SCALE_FROM) * p;
        final int cx = w / 2;
        final int cy = h / 2;
        g2.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.translate(cx, cy);
        g2.scale(scale, scale);
        g2.translate(-cx, -cy);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, p));
        g2.drawImage(buffer, 0, 0, w, h, null);
      } finally {
        g2.dispose();
      }
    }

    @Override
    public Dimension getPreferredSize() {
      final Dimension pref = super.getPreferredSize();
      final Insets s = getInsets();
      final int shadowH = s.left + s.right;
      final int bodyW = pref.width - shadowH;
      final int clampedBody = Math.max(MIN_BODY_WIDTH, Math.min(MAX_BODY_WIDTH, bodyW));
      return new Dimension(clampedBody + shadowH, pref.height);
    }

    @Override
    public Dimension getMinimumSize() {
      return getPreferredSize();
    }

    // Reports AccessibleRole.DIALOG so assistive tech announces this as a dialog (§10); the
    // accessible name is set to the headline by the enclosing ElwhaDialog at show time.
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

  // The full-bounds scrim. Paints SCRIM @ 32% beneath the surface and consumes every mouse event
  // that reaches it (i.e. not over the surface) — the input-blocking half of overlay modality. A
  // click dismisses the dialog when scrim-dismiss is enabled.
  private final class Scrim extends JComponent {
    private static final float SCRIM_ALPHA = 0.32f;

    Scrim() {
      setOpaque(false);
      // Attaching listeners makes the scrim the mouse target over its bounds, so events never reach
      // the now-inert content pane beneath it.
      final MouseAdapter consumer =
          new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
              if (dismissibleByScrim) {
                dismiss(DismissCause.SCRIM);
              }
            }
          };
      addMouseListener(consumer);
      addMouseMotionListener(consumer);
      addMouseWheelListener(e -> {});
    }

    @Override
    protected void paintComponent(final Graphics g) {
      final Graphics2D g2 = (Graphics2D) g.create();
      try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        final float p = Math.max(0f, Math.min(1f, motionProgress));
        final Color scrim = ColorRole.SCRIM.resolve();
        g2.setColor(
            new Color(
                scrim.getRed(),
                scrim.getGreen(),
                scrim.getBlue(),
                Math.round(255 * SCRIM_ALPHA * p)));
        g2.fillRect(0, 0, getWidth(), getHeight());
      } finally {
        g2.dispose();
      }
    }
  }
}
