package com.owspfm.elwha.sidesheet;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.CornerRadii;
import com.owspfm.elwha.theme.Easing;
import com.owspfm.elwha.theme.MorphAnimator;
import com.owspfm.elwha.theme.ShapeScale;
import com.owspfm.elwha.theme.SpaceScale;
import com.owspfm.elwha.theme.SurfacePainter;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The Material 3 <strong>side sheet</strong> primitive — a surface carrying supplementary content
 * anchored to the side of the window, with typed anatomy slots: a header (optional back affordance
 * → headline → optional close affordance), a consumer-supplied content slot, and an optional action
 * footer (divider + {@link ElwhaButton} actions). Spec capture lives in {@code
 * docs/research/elwha-side-sheet-research.md}; decisions in {@code elwha-side-sheet-design.md}.
 *
 * <p><strong>One component, two presentation paths.</strong> A {@link SheetType#STANDARD} sheet is
 * ordinary page furniture: embed it in your layout (typically {@code BorderLayout.LINE_END}) where
 * it coexists with the main UI. A {@link SheetType#MODAL} sheet is presented over a scrim with
 * {@code showModal(parent)} and dismissed by Esc / scrim click / its affordances. The chrome —
 * container color, corner shape, edge divider — derives from the type ({@code SURFACE} / square /
 * divider vs {@code SURFACE_CONTAINER_LOW} / 16px content-facing corners / scrim); there are no raw
 * chrome setters, and neither type paints a drop shadow (the spec renders show the modal sheet flat
 * over its scrim).
 *
 * <p><strong>Edge anchoring.</strong> {@link SheetEdge#TRAILING} (the M3 default) or {@link
 * SheetEdge#LEADING}, resolved against the component orientation at paint/layout time — the
 * resolved edge picks which corners round, which edge wears the standard divider, and which side
 * the modal presentation docks and slides from. For a standard sheet the edge drives chrome only;
 * placing the component on the matching side of the layout remains the consumer's job.
 *
 * <p><strong>Footer actions are consumer-owned.</strong> Unlike the dialog's action row, footer
 * actions do not auto-dismiss — a filter sheet's "Apply" may legitimately keep the sheet open. Wire
 * your own listeners; call {@code close()} / {@code dismiss()} yourself when an action should also
 * dismiss (and consider {@link ElwhaButton#setRippleEnabled(boolean) suppressing that button's
 * ripple} so it can't freeze on the modal exit snapshot).
 *
 * <p><strong>Guidelines (M3).</strong> Side sheets carry supporting content and tasks related to
 * the main view — filters, detail panes, settings, secondary lists. They are not primary navigation
 * (that's the navigation rail), not interruptive confirmation (the dialog), and not transient
 * command lists (the menu). Keep headlines short; prefer one sheet at a time per edge.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaSideSheet extends JComponent {

  /** M3 docked side sheet container width ({@code md.comp.sheet.side.docked.container.width}). */
  public static final int SHEET_WIDTH_PX = 256;

  /** M3 detached side sheet margin ({@code m3_side_sheet_margin_detached}). */
  public static final int DETACHED_MARGIN_PX = SpaceScale.LG.px();

  private SheetType sheetType;
  private SheetEdge sheetEdge = SheetEdge.TRAILING;
  private SheetPosture sheetPosture = SheetPosture.DOCKED;
  private int sheetWidth = SHEET_WIDTH_PX;

  private String headline;
  private Runnable onBack;
  private boolean closeAffordanceVisible = true;
  private boolean backAffordanceVisible;
  private boolean edgeDividerVisible = true;
  private boolean footerDividerVisible = true;
  private JComponent content;
  private final List<ElwhaButton> actions = new ArrayList<>();

  private boolean open = true;
  private float openProgress = 1f;
  private MorphAnimator openAnimator;

  private boolean dismissibleByEsc = true;
  private boolean dismissibleByScrim = true;
  private Consumer<SheetDismissCause> onClose;
  private SideSheetOverlay overlay;

  private final JPanel body = new JPanel(new BorderLayout());
  private final JPanel header = new JPanel(new BorderLayout(SpaceScale.MD.px(), 0));
  private final JLabel headlineLabel = new HeadlineLabel();
  private final ElwhaIconButton backButton;
  private final ElwhaIconButton closeButton;
  private final JPanel backWrap = centerWrap();
  private final JPanel closeWrap = centerWrap();
  private final JPanel contentHolder = new JPanel(new BorderLayout());
  private final JPanel footer = new JPanel(new BorderLayout());
  private final FooterDivider footerDivider = new FooterDivider();
  private final JPanel actionsRow =
      new JPanel(new WrapFlowLayout(FlowLayout.LEADING, SpaceScale.MD.px(), SpaceScale.SM.px()));

  /**
   * Creates a {@link SheetType#STANDARD} sheet with the given headline — the convenience
   * constructor.
   *
   * @param headline the header headline; also the sheet's accessible name
   * @version v0.5.0
   * @since v0.5.0
   */
  public ElwhaSideSheet(final String headline) {
    this(SheetType.STANDARD, headline);
  }

  private ElwhaSideSheet(final SheetType type, final String headline) {
    this.sheetType = Objects.requireNonNull(type, "type");
    this.headline = headline;

    setOpaque(false);

    backButton = ElwhaIconButton.standardIconButton(MaterialIcons.arrowBack());
    backButton.setToolTipText("Back");
    backButton.getAccessibleContext().setAccessibleName("Back");
    // Dismissing through this affordance snapshots the surface for the modal exit slide; a live
    // press ripple would freeze mid-stroke on that snapshot (#288), so it stays suppressed.
    backButton.setRippleEnabled(false);
    backButton.addActionListener(e -> onBackActivated());

    closeButton = ElwhaIconButton.standardIconButton(MaterialIcons.close());
    closeButton.setToolTipText("Close");
    closeButton.getAccessibleContext().setAccessibleName("Close");
    closeButton.setRippleEnabled(false);
    closeButton.addActionListener(e -> onCloseActivated());

    backWrap.add(backButton);
    closeWrap.add(closeButton);

    header.setOpaque(false);
    header.add(backWrap, BorderLayout.LINE_START);
    header.add(headlineLabel, BorderLayout.CENTER);
    header.add(closeWrap, BorderLayout.LINE_END);

    contentHolder.setOpaque(false);

    actionsRow.setOpaque(false);
    // The flow layout contributes its own hgap/vgap at the row edges, so the border carries the
    // remainder of the 16/24/24 redline paddings (border + gap = the design values).
    actionsRow.setBorder(
        BorderFactory.createEmptyBorder(
            SpaceScale.SM.px(), SpaceScale.MD.px(), SpaceScale.LG.px(), SpaceScale.MD.px()));
    footer.setOpaque(false);
    footer.add(footerDivider, BorderLayout.NORTH);
    footer.add(actionsRow, BorderLayout.CENTER);

    body.setOpaque(false);
    body.add(header, BorderLayout.NORTH);
    body.add(contentHolder, BorderLayout.CENTER);
    body.add(footer, BorderLayout.SOUTH);
    add(body);

    headlineLabel.setText(headline);
    getAccessibleContext().setAccessibleName(headline);
    refreshChrome();
  }

  /**
   * Creates a {@link SheetType#STANDARD} sheet — the docked, embeddable type.
   *
   * @param headline the header headline; also the sheet's accessible name
   * @return a new standard sheet
   * @version v0.5.0
   * @since v0.5.0
   */
  public static ElwhaSideSheet standardSheet(final String headline) {
    return new ElwhaSideSheet(SheetType.STANDARD, headline);
  }

  /**
   * Creates a {@link SheetType#MODAL} sheet — the scrim-presented type.
   *
   * @param headline the header headline; also the sheet's accessible name
   * @return a new modal sheet
   * @version v0.5.0
   * @since v0.5.0
   */
  public static ElwhaSideSheet modalSheet(final String headline) {
    return new ElwhaSideSheet(SheetType.MODAL, headline);
  }

  // ------------------------------------------------------------ chrome axes

  /**
   * Sets the sheet type, re-deriving the chrome (container color, elevation, corners, dividers).
   *
   * @param type the sheet type
   * @throws NullPointerException if {@code type} is {@code null}
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setSheetType(final SheetType type) {
    final SheetType next = Objects.requireNonNull(type, "type");
    if (next == this.sheetType) {
      return;
    }
    this.sheetType = next;
    revalidate();
    repaint();
  }

  /**
   * @return the sheet type
   * @version v0.5.0
   * @since v0.5.0
   */
  public SheetType getSheetType() {
    return sheetType;
  }

  /**
   * Sets the logical window edge this sheet anchors to. Drives the chrome (corner asymmetry,
   * divider side) and the modal docking/slide side; for a standard sheet, matching layout placement
   * stays the consumer's job.
   *
   * @param edge the anchor edge
   * @throws NullPointerException if {@code edge} is {@code null}
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setSheetEdge(final SheetEdge edge) {
    final SheetEdge next = Objects.requireNonNull(edge, "edge");
    if (next == this.sheetEdge) {
      return;
    }
    this.sheetEdge = next;
    repaint();
  }

  /**
   * @return the logical anchor edge
   * @version v0.5.0
   * @since v0.5.0
   */
  public SheetEdge getSheetEdge() {
    return sheetEdge;
  }

  /**
   * Sets the posture — {@link SheetPosture#DOCKED} (flush to the edge) or {@link
   * SheetPosture#DETACHED} (floating, inset by a {@value #DETACHED_MARGIN_PX}px margin on all sides
   * with all four corners rounded and no edge divider). Default {@link SheetPosture#DOCKED} — a
   * docked sheet is exactly the V1 sheet. Applies live: an embedded sheet reflows its host, and a
   * currently-shown modal presentation re-docks at the new footprint.
   *
   * @param posture the posture
   * @throws NullPointerException if {@code posture} is {@code null}
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setSheetPosture(final SheetPosture posture) {
    final SheetPosture next = Objects.requireNonNull(posture, "posture");
    if (next == this.sheetPosture) {
      return;
    }
    this.sheetPosture = next;
    refreshChrome();
    revalidate();
    repaint();
    if (isModalShowing()) {
      overlay.relayoutHost();
    }
  }

  /**
   * @return the posture
   * @version v0.5.0
   * @since v0.5.0
   */
  public SheetPosture getSheetPosture() {
    return sheetPosture;
  }

  /**
   * Sets the sheet's open width in pixels. The M3 docked width is {@link #SHEET_WIDTH_PX} (256);
   * the spec caps modal sheets at 400 — documented, not enforced (desktop detail panes legitimately
   * run wider). A modal presentation additionally clamps to the host window's width. Applies live:
   * an embedded sheet reflows its host layout, and a currently-shown modal presentation re-docks at
   * the new width.
   *
   * @param px the open width in pixels (clamped to {@code >= 0})
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setSheetWidth(final int px) {
    final int next = Math.max(0, px);
    if (next == this.sheetWidth) {
      return;
    }
    this.sheetWidth = next;
    revalidate();
    repaint();
    if (isModalShowing()) {
      overlay.relayoutHost();
    }
  }

  /**
   * @return the open width in pixels
   * @version v0.5.0
   * @since v0.5.0
   */
  public int getSheetWidth() {
    return sheetWidth;
  }

  // ------------------------------------------------------------ anatomy slots

  /**
   * Sets the headline — the header's title and the sheet's accessible name. Rendered {@code
   * TITLE_LARGE} / {@code ON_SURFACE}, single line, ellipsized when the sheet is narrower than the
   * text.
   *
   * @param headline the headline text, or {@code null} for none
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setHeadline(final String headline) {
    this.headline = headline;
    headlineLabel.setText(headline);
    getAccessibleContext().setAccessibleName(headline);
    revalidate();
    repaint();
  }

  /**
   * @return the headline text, or {@code null}
   * @version v0.5.0
   * @since v0.5.0
   */
  public String getHeadline() {
    return headline;
  }

  /**
   * Sets the content slot — the consumer-supplied component shown between the header and the
   * footer, inset by the sheet's 24px content padding. The sheet hosts it transparently and does
   * not recolor it. Content that scrolls is consumer-owned: wrap it in a {@link
   * javax.swing.JScrollPane} yourself.
   *
   * @param content the content component, or {@code null} for none
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setContent(final JComponent content) {
    if (this.content != null) {
      contentHolder.remove(this.content);
    }
    this.content = content;
    if (content != null) {
      content.setOpaque(false);
      contentHolder.add(content, BorderLayout.CENTER);
    }
    revalidate();
    repaint();
  }

  /**
   * @return the content component, or {@code null}
   * @version v0.5.0
   * @since v0.5.0
   */
  public JComponent getContent() {
    return content;
  }

  /**
   * Sets the footer actions, replacing any previous set. The footer renders them leading-aligned
   * (confirming action first, per the M3 sheet renders) above the sheet's bottom edge, preceded by
   * the footer divider when {@link #isFooterDividerVisible()}. Passing no buttons removes the
   * footer entirely. Actions do <strong>not</strong> auto-dismiss — see the class doc.
   *
   * @param actions the footer action buttons, leading-first; empty for no footer
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setActions(final ElwhaButton... actions) {
    this.actions.clear();
    actionsRow.removeAll();
    if (actions != null) {
      for (final ElwhaButton action : actions) {
        if (action != null) {
          this.actions.add(action);
          actionsRow.add(action);
        }
      }
    }
    refreshChrome();
    revalidate();
    repaint();
  }

  /**
   * @return the footer action buttons, leading-first; empty when the footer is absent
   * @version v0.5.0
   * @since v0.5.0
   */
  public List<ElwhaButton> getActions() {
    return List.copyOf(actions);
  }

  /**
   * Shows or hides the header's trailing close icon button. Visible by default.
   *
   * @param visible whether the close affordance shows
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setCloseAffordanceVisible(final boolean visible) {
    if (visible == this.closeAffordanceVisible) {
      return;
    }
    this.closeAffordanceVisible = visible;
    refreshChrome();
    revalidate();
    repaint();
  }

  /**
   * @return whether the close affordance shows
   * @version v0.5.0
   * @since v0.5.0
   */
  public boolean isCloseAffordanceVisible() {
    return closeAffordanceVisible;
  }

  /**
   * Shows or hides the header's leading back icon button — the affordance for multi-step sheet
   * flows. Hidden by default. Activating it runs {@link #setOnBack(Runnable) onBack} when set.
   *
   * @param visible whether the back affordance shows
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setBackAffordanceVisible(final boolean visible) {
    if (visible == this.backAffordanceVisible) {
      return;
    }
    this.backAffordanceVisible = visible;
    refreshChrome();
    revalidate();
    repaint();
  }

  /**
   * @return whether the back affordance shows
   * @version v0.5.0
   * @since v0.5.0
   */
  public boolean isBackAffordanceVisible() {
    return backAffordanceVisible;
  }

  /**
   * Sets the back affordance's handler for multi-step flows. When {@code null} (the default) the
   * affordance falls back to dismissing/closing the sheet.
   *
   * @param onBack the handler, or {@code null} for the dismiss default
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setOnBack(final Runnable onBack) {
    this.onBack = onBack;
  }

  /**
   * @return the back handler, or {@code null}
   * @version v0.5.0
   * @since v0.5.0
   */
  public Runnable getOnBack() {
    return onBack;
  }

  /**
   * Shows or hides the standard sheet's 1px {@code OUTLINE_VARIANT} divider on the content-facing
   * edge — the boundary a square-cornered, elevation-0 surface needs to read as a panel. Visible by
   * default; painted only while the type is {@link SheetType#STANDARD}.
   *
   * @param visible whether the edge divider paints
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setEdgeDividerVisible(final boolean visible) {
    if (visible == this.edgeDividerVisible) {
      return;
    }
    this.edgeDividerVisible = visible;
    repaint();
  }

  /**
   * @return whether the edge divider paints (on a standard sheet)
   * @version v0.5.0
   * @since v0.5.0
   */
  public boolean isEdgeDividerVisible() {
    return edgeDividerVisible;
  }

  /**
   * Shows or hides the 1px {@code OUTLINE_VARIANT} divider above the action footer. Visible by
   * default; rendered only while the footer exists (i.e. {@link #setActions} was given buttons).
   *
   * @param visible whether the footer divider shows
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setFooterDividerVisible(final boolean visible) {
    if (visible == this.footerDividerVisible) {
      return;
    }
    this.footerDividerVisible = visible;
    refreshChrome();
    revalidate();
    repaint();
  }

  /**
   * @return whether the footer divider shows (when the footer exists)
   * @version v0.5.0
   * @since v0.5.0
   */
  public boolean isFooterDividerVisible() {
    return footerDividerVisible;
  }

  // ------------------------------------------------------------ standard presentation

  /**
   * Opens an embedded standard sheet — animates the preferred width from its current state to the
   * full {@link #getSheetWidth() sheet width} (300ms, emphasized; snaps under reduced motion),
   * revalidating per tick so sibling content reflows: the M3 coplanar squash. A no-op when already
   * open.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  public void open() {
    setOpen(true);
  }

  /**
   * Closes an embedded standard sheet — the reverse of {@link #open()}, collapsing the preferred
   * width to 0 while the component stays in the hierarchy (so the animation is symmetric and the
   * consumer's layout code stays branch-free). A no-op when already closed.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  public void close() {
    setOpen(false);
  }

  /**
   * Sets the open state, animating toward it — see {@link #open()} / {@link #close()}.
   *
   * @param open the target state
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setOpen(final boolean open) {
    if (open == this.open) {
      return;
    }
    this.open = open;
    ensureOpenAnimator();
    if (open) {
      openAnimator.start();
    } else {
      openAnimator.reverse();
    }
  }

  /**
   * @return the target open state ({@code true} from construction); flips immediately on {@link
   *     #setOpen} while the width animation catches up
   * @version v0.5.0
   * @since v0.5.0
   */
  public boolean isOpen() {
    return open;
  }

  private void ensureOpenAnimator() {
    if (openAnimator != null) {
      return;
    }
    openAnimator = new MorphAnimator(this, MorphAnimator.MEDIUM2_MS);
    openAnimator.snapTo(openProgress);
    openAnimator.addProgressListener(
        () -> {
          openProgress = Easing.EMPHASIZED_DECELERATE.ease(openAnimator.progress());
          revalidate();
        });
  }

  // ------------------------------------------------------------ modal presentation

  /**
   * Presents this sheet modally over a scrim, mounted on the host frame's layered pane at {@code
   * ElwhaLayers.OVERLAY_LAYER} (190) — below dialogs (200) and menus (300), so either may open
   * above the sheet. Forces the type to {@link SheetType#MODAL} (the chrome contract) and snaps the
   * sheet open. The sheet slides in from its resolved edge while the scrim fades (300ms emphasized;
   * snaps under reduced motion); focus moves into the sheet, is trapped while it is up, and
   * restores on close. Returns immediately; the outcome is reported through {@link
   * #setOnClose(Consumer)}. A no-op while already shown.
   *
   * @param parent any component in the target window's tree; used to resolve the host frame and
   *     restore focus on close
   * @throws NullPointerException if {@code parent} is {@code null}
   * @throws IllegalStateException if {@code parent} is not in a realized window
   * @version v0.5.0
   * @since v0.5.0
   */
  public void showModal(final Component parent) {
    if (isModalShowing()) {
      return;
    }
    setSheetType(SheetType.MODAL);
    if (!open) {
      open = true;
      openProgress = 1f;
      if (openAnimator != null) {
        openAnimator.snapTo(1f);
      }
    }
    overlay = new SideSheetOverlay(this);
    overlay.show(parent);
  }

  /**
   * Dismisses the modal presentation programmatically, reporting {@link
   * SheetDismissCause#PROGRAMMATIC}. A no-op when not modally shown (an embedded standard sheet is
   * closed with {@link #close()} instead).
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  public void dismiss() {
    if (overlay != null) {
      overlay.dismissSheet(SheetDismissCause.PROGRAMMATIC);
    }
  }

  /**
   * @return whether the sheet is currently presented modally (between {@link #showModal} and the
   *     end of its teardown)
   * @version v0.5.0
   * @since v0.5.0
   */
  public boolean isModalShowing() {
    return overlay != null && overlay.showingNow();
  }

  /**
   * Whether the Escape key dismisses the modal presentation (with {@link SheetDismissCause#ESC}).
   * Default {@code true}; honored live, including toggles while shown.
   *
   * @param dismissible whether Esc dismisses
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setDismissibleByEsc(final boolean dismissible) {
    this.dismissibleByEsc = dismissible;
  }

  /**
   * @return whether Esc dismisses the modal presentation
   * @version v0.5.0
   * @since v0.5.0
   */
  public boolean isDismissibleByEsc() {
    return dismissibleByEsc;
  }

  /**
   * Whether clicking the scrim dismisses the modal presentation (with {@link
   * SheetDismissCause#SCRIM}). Default {@code true}; the scrim consumes the click either way (it
   * always blocks the UI behind). Honored live, including toggles while shown.
   *
   * @param dismissible whether a scrim click dismisses
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setDismissibleByScrim(final boolean dismissible) {
    this.dismissibleByScrim = dismissible;
  }

  /**
   * @return whether a scrim click dismisses the modal presentation
   * @version v0.5.0
   * @since v0.5.0
   */
  public boolean isDismissibleByScrim() {
    return dismissibleByScrim;
  }

  /**
   * Sets the close hook for the modal presentation — fired once per {@link #showModal}, after the
   * exit motion and teardown complete, with the recorded {@link SheetDismissCause}.
   *
   * @param onClose the callback, or {@code null} for none
   * @version v0.5.0
   * @since v0.5.0
   */
  public void setOnClose(final Consumer<SheetDismissCause> onClose) {
    this.onClose = onClose;
  }

  /**
   * @return the close hook, or {@code null}
   * @version v0.5.0
   * @since v0.5.0
   */
  public Consumer<SheetDismissCause> getOnClose() {
    return onClose;
  }

  // The overlay's teardown completion: drop the live host and relay the cause to the consumer.
  void modalClosed(final SheetDismissCause cause) {
    overlay = null;
    if (onClose != null) {
      onClose.accept(cause);
    }
  }

  // ------------------------------------------------------------ affordance behavior

  void onBackActivated() {
    if (onBack != null) {
      onBack.run();
      return;
    }
    if (isModalShowing()) {
      overlay.dismissSheet(SheetDismissCause.BACK_AFFORDANCE);
    } else {
      close();
    }
  }

  void onCloseActivated() {
    if (isModalShowing()) {
      overlay.dismissSheet(SheetDismissCause.CLOSE_AFFORDANCE);
    } else {
      close();
    }
  }

  // Focus plumbing for the modal host: initial focus prefers the content slot, then the close
  // affordance (design doc §10).
  JPanel contentHolderPanel() {
    return contentHolder;
  }

  ElwhaIconButton closeAffordanceButton() {
    return closeButton;
  }

  // ------------------------------------------------------------ geometry & chrome

  /**
   * Whether the resolved anchor edge is the window's right side — {@link SheetEdge#TRAILING} in LTR
   * or {@link SheetEdge#LEADING} in RTL.
   */
  boolean isDockedRight() {
    return (sheetEdge == SheetEdge.TRAILING) == getComponentOrientation().isLeftToRight();
  }

  // The modal host's slide-band width: the sheet width plus its own (detached) margin, so the
  // sheet's border floats the painted body off the window edges without the host re-adding the
  // margin (design doc §3). Equal to sheetWidth for a docked sheet.
  int modalFootprintWidth() {
    final Insets s = getInsets();
    return sheetWidth + s.left + s.right;
  }

  private ColorRole containerRole() {
    return sheetType == SheetType.MODAL ? ColorRole.SURFACE_CONTAINER_LOW : ColorRole.SURFACE;
  }

  private CornerRadii cornerRadii() {
    if (sheetPosture == SheetPosture.DETACHED) {
      return CornerRadii.uniform(ShapeScale.LG.px());
    }
    if (sheetType != SheetType.MODAL) {
      return CornerRadii.uniform(0);
    }
    final int lg = ShapeScale.LG.px();
    return isDockedRight() ? CornerRadii.of(lg, 0, 0, lg) : CornerRadii.of(0, lg, lg, 0);
  }

  /**
   * @return the current size — the sheet width scaled by the open/close animation progress, at the
   *     anatomy's natural height
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public Dimension getPreferredSize() {
    if (isPreferredSizeSet()) {
      return super.getPreferredSize();
    }
    final Insets s = getInsets();
    final int scaledW = Math.round(sheetWidth * openProgress);
    // A fully-collapsed sheet reports width 0, not just the margins — a closed detached sheet must
    // leave no 2*margin sliver behind in the host layout.
    return new Dimension(
        scaledW == 0 ? 0 : scaledW + s.left + s.right,
        body.getPreferredSize().height + s.top + s.bottom);
  }

  /**
   * Lays the anatomy body across the chassis. While the open/close animation is mid-flight the body
   * stays pinned at the full sheet width — anchored so the visible portion is the one nearest the
   * main content — and the shrinking chassis clips it: children keep their open-width layout
   * instead of re-wrapping every tick (design doc §14).
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public void doLayout() {
    final Insets s = getInsets();
    final int availW = Math.max(0, getWidth() - s.left - s.right);
    final int availH = Math.max(0, getHeight() - s.top - s.bottom);
    final int bodyW = openProgress < 1f ? Math.max(availW, sheetWidth) : availW;
    final int x = isDockedRight() ? s.left : getWidth() - s.right - bodyW;
    body.setBounds(x, s.top, bodyW, availH);
  }

  /**
   * Paints the type-derived chrome: the container fill with the content-facing corners rounded
   * (modal) or square (standard), and the content-facing edge divider (standard). No shadow on
   * either type — the M3 spec renders show the modal sheet flat over its scrim (the
   * container-elevation token is not expressed as a drop shadow; research doc §B correction,
   * 2026-06-11).
   *
   * @param g the graphics context
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  protected void paintComponent(final Graphics g) {
    final Insets s = getInsets();
    final int bodyX = s.left;
    final int bodyY = s.top;
    final int bodyW = Math.max(0, getWidth() - s.left - s.right);
    final int bodyH = Math.max(0, getHeight() - s.top - s.bottom);
    if (bodyW <= 0 || bodyH <= 0) {
      return;
    }
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      final Graphics2D bodyG = (Graphics2D) g2.create(bodyX, bodyY, bodyW, bodyH);
      try {
        SurfacePainter.paint(bodyG, bodyW, bodyH, cornerRadii(), containerRole(), null, null, 0f);
      } finally {
        bodyG.dispose();
      }
      if (sheetType == SheetType.STANDARD
          && sheetPosture == SheetPosture.DOCKED
          && edgeDividerVisible) {
        g2.setColor(ColorRole.OUTLINE_VARIANT.resolve());
        g2.fillRect(isDockedRight() ? bodyX : bodyX + bodyW - 1, bodyY, 1, bodyH);
      }
    } finally {
      g2.dispose();
    }
  }

  /**
   * Re-applies orientation-dependent chrome (header paddings, divider side, corner asymmetry) when
   * the component orientation changes.
   *
   * @param orientation the new orientation
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public void setComponentOrientation(final ComponentOrientation orientation) {
    super.setComponentOrientation(orientation);
    refreshChrome();
    revalidate();
    repaint();
  }

  // Re-derives every config-dependent piece of the static anatomy: affordance visibility, header
  // and content paddings (24px sides, 12px beside a visible icon affordance whose 48px target
  // carries the rest of the optical gap), and footer presence.
  private void refreshChrome() {
    // The detached margin is carried as the sheet's own border, so every inset-aware path
    // (getPreferredSize / doLayout / paintComponent) floats the body and rounds the corners with no
    // extra geometry. The modal host reads this margin off the footprint width, never re-adds it.
    final int margin = sheetPosture == SheetPosture.DETACHED ? DETACHED_MARGIN_PX : 0;
    setBorder(margin == 0 ? null : BorderFactory.createEmptyBorder(margin, margin, margin, margin));

    backWrap.setVisible(backAffordanceVisible);
    closeWrap.setVisible(closeAffordanceVisible);

    final boolean ltr = getComponentOrientation().isLeftToRight();
    final int leadingPad = backAffordanceVisible ? SpaceScale.MD.px() : SpaceScale.XL.px();
    final int trailingPad = closeAffordanceVisible ? SpaceScale.MD.px() : SpaceScale.XL.px();
    header.setBorder(
        BorderFactory.createEmptyBorder(
            SpaceScale.MD.px(),
            ltr ? leadingPad : trailingPad,
            SpaceScale.MD.px(),
            ltr ? trailingPad : leadingPad));

    final boolean hasFooter = !actions.isEmpty();
    footer.setVisible(hasFooter);
    footerDivider.setVisible(hasFooter && footerDividerVisible);
    contentHolder.setBorder(
        BorderFactory.createEmptyBorder(
            0, SpaceScale.XL.px(), hasFooter ? 0 : SpaceScale.XL.px(), SpaceScale.XL.px()));
  }

  /**
   * Reports {@link AccessibleRole#PANEL} (Swing has no side-sheet role); the accessible name tracks
   * the headline.
   *
   * @return the accessible context
   * @version v0.5.0
   * @since v0.5.0
   */
  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext =
          new AccessibleJComponent() {
            @Override
            public AccessibleRole getAccessibleRole() {
              return AccessibleRole.PANEL;
            }
          };
    }
    return accessibleContext;
  }

  private static JPanel centerWrap() {
    final JPanel wrap = new JPanel(new GridBagLayout());
    wrap.setOpaque(false);
    return wrap;
  }

  // The headline: TITLE_LARGE / ON_SURFACE resolved per paint so a runtime theme/mode switch
  // re-skins it without a listener (the lib-wide token binding rule).
  private static final class HeadlineLabel extends JLabel {
    @Override
    public Font getFont() {
      return TypeRole.TITLE_LARGE.resolve();
    }

    @Override
    public Color getForeground() {
      return ColorRole.ON_SURFACE.resolve();
    }
  }

  // A FlowLayout whose preferred/minimum size accounts for wrapping at the row's realized width.
  // The stock FlowLayout always reports the single-row size, so a fixed-width sheet whose actions
  // overflow the row would wrap them during layout but never grow the footer — clipping the
  // wrapped row off the sheet's bottom edge (found in the #464 smoke loop).
  private static final class WrapFlowLayout extends FlowLayout {

    WrapFlowLayout(final int align, final int hgap, final int vgap) {
      super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(final Container target) {
      return wrappedSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(final Container target) {
      return wrappedSize(target, false);
    }

    private Dimension wrappedSize(final Container target, final boolean preferred) {
      synchronized (target.getTreeLock()) {
        // The row's own width is 0 until its first layout pass; the nearest sized ancestor (the
        // sheet body, whose bounds are set before its children are laid out) stands in so the
        // very first pass already wraps at the real width.
        int targetWidth = 0;
        for (Container walk = target; walk != null; walk = walk.getParent()) {
          if (walk.getWidth() > 0) {
            targetWidth = walk.getWidth();
            break;
          }
        }
        if (targetWidth == 0) {
          targetWidth = Integer.MAX_VALUE;
        }
        final Insets insets = target.getInsets();
        final int maxRowWidth = targetWidth - insets.left - insets.right - getHgap() * 2;
        int rowWidth = 0;
        int rowHeight = 0;
        int width = 0;
        int height = insets.top + insets.bottom + getVgap() * 2;
        boolean firstInRow = true;
        for (final Component child : target.getComponents()) {
          if (!child.isVisible()) {
            continue;
          }
          final Dimension d = preferred ? child.getPreferredSize() : child.getMinimumSize();
          if (!firstInRow && rowWidth + getHgap() + d.width > maxRowWidth) {
            width = Math.max(width, rowWidth);
            height += rowHeight + getVgap();
            rowWidth = 0;
            rowHeight = 0;
            firstInRow = true;
          }
          rowWidth += (firstInRow ? 0 : getHgap()) + d.width;
          rowHeight = Math.max(rowHeight, d.height);
          firstInRow = false;
        }
        width = Math.max(width, rowWidth);
        height += rowHeight;
        return new Dimension(width + insets.left + insets.right + getHgap() * 2, height);
      }
    }
  }

  // The 1px OUTLINE_VARIANT line above the action footer, spanning the sheet's full width.
  private static final class FooterDivider extends JComponent {
    FooterDivider() {
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
}
