package com.owspfm.elwha.theme;

import javax.swing.JLayeredPane;

/**
 * The Elwha z-band convention for transient overlays mounted on a <em>shared</em> {@link
 * JLayeredPane} (the root pane's layered pane in the common {@code JFrame} / {@code JDialog} case).
 *
 * <p>Swing's {@link JLayeredPane} layer constants leave the band between ordinary content and modal
 * dialogs to consumer convention. Elwha overlays — badges anchored by {@link
 * com.owspfm.elwha.badge.ElwhaBadgeAnchor}, and root-pane floating-FAB recipes — historically
 * landed on {@link JLayeredPane#PALETTE_LAYER} (100), the same layer consumer apps grab for their
 * own floating UI (tool palettes, side-panel overlays). When both an Elwha overlay and consumer UI
 * share that layer, their z-order becomes add-order-dependent: sometimes the overlay paints above
 * the consumer UI, sometimes below, with no consistent rule. {@link #OVERLAY_LAYER} resolves that
 * by giving Elwha overlays a dedicated, documented slot above content and below dialogs that does
 * not collide with the public {@code PALETTE_LAYER} consumers reach for.
 *
 * <p><strong>The Elwha z-stack (bottom to top).</strong>
 *
 * <ul>
 *   <li><strong>Ordinary content</strong> — {@link JLayeredPane#DEFAULT_LAYER} (0). The content
 *       pane and everything the consumer lays out normally.
 *   <li><strong>Elwha transient overlays</strong> — {@link #OVERLAY_LAYER} (190). Badges, root-pane
 *       floating FABs, and similar Elwha-managed decorations that must float above content but
 *       beneath dialogs.
 *   <li><strong>Elwha dialogs</strong> — {@link JLayeredPane#MODAL_LAYER} (200). {@code
 *       AbstractElwhaDialog} attaches its scrim backdrop and surface here, so a dialog always
 *       covers Elwha overlays.
 *   <li><strong>Swing popups</strong> — {@link JLayeredPane#POPUP_LAYER} (300). Tooltips, combo-box
 *       dropdowns, and menus, which Swing places above everything else.
 * </ul>
 *
 * <p><strong>Why 190.</strong> The band is {@code MODAL_LAYER - 10}, i.e. just below the dialog
 * band (200) so dialogs always cover overlays, and well above ordinary content (0) so overlays
 * always float. Crucially it is <em>not</em> {@code PALETTE_LAYER} (100): moving Elwha overlays off
 * that layer frees it for consumer-owned floating UI, eliminating the add-order-dependent
 * collision. The {@code -10} margin (rather than {@code -1}) leaves headroom for future Elwha
 * overlay sub-bands within the same conceptual slot without crowding the dialog band.
 *
 * <p><strong>Scope.</strong> This convention governs only mounts on a <em>shared</em> layered pane
 * — the badge anchor and the root-pane floating-FAB recipe (FAB design doc §15). {@code
 * ElwhaFabAnchor} the <em>wrapper</em> owns a private {@link JLayeredPane} and is exempt: inside
 * its own pane it correctly places content on {@link JLayeredPane#DEFAULT_LAYER} and the FAB on
 * {@link JLayeredPane#PALETTE_LAYER}, where no consumer UI competes.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaLayers {

  /**
   * The z-layer for Elwha transient overlays mounted on a shared {@link JLayeredPane} — {@code
   * JLayeredPane.MODAL_LAYER - 10} (= 190). Sits above ordinary content and below Elwha dialogs and
   * Swing popups. Pass to {@link JLayeredPane#add(java.awt.Component, Object)} as the layer
   * constraint.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  public static final Integer OVERLAY_LAYER = JLayeredPane.MODAL_LAYER - 10;

  private ElwhaLayers() {}
}
