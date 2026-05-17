package com.owspfm.elwha.card;

import java.awt.Container;
import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.text.View;

/**
 * Package-private helpers used by the text atoms ({@link ElwhaCardTitle}, {@link
 * ElwhaCardSubtitle}, {@link ElwhaCardSupportingText}) to satisfy spec §4.1 / §4.3 — "word-wraps
 * (does not ellipsize at narrow widths); HTML auto-wrapped." Centralises the two Swing tricks that
 * make a {@link JLabel} actually wrap at the parent width:
 *
 * <ol>
 *   <li>Auto-prefix plain text with {@code <html>} so the label uses the HTML view (which supports
 *       line wrap), instead of the single-line plain text renderer.
 *   <li>Compute preferred size by querying the HTML view's preferred Y-axis span at the label's
 *       current width — Swing's default JLabel preferred size returns the natural width of the full
 *       text on one line, which doesn't let parent layouts know how much vertical space the wrapped
 *       lines actually need.
 * </ol>
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
final class WrappingLabels {

  private WrappingLabels() {
    // utility
  }

  /**
   * Wraps {@code text} in {@code <html>...</html>} if it's not already HTML and not empty.
   * Returning {@code <html>} content makes {@link JLabel} install an HTML view that supports
   * wrapping.
   */
  static String htmlWrap(final String text) {
    if (text == null || text.isEmpty()) {
      return text;
    }
    final String trimmedLower = text.toLowerCase().trim();
    if (trimmedLower.startsWith("<html>")) {
      return text;
    }
    return "<html>" + text + "</html>";
  }

  /**
   * Recomputes a {@link JLabel}'s preferred size so the HTML view wraps at the parent's available
   * width. Without this, the label reports its natural one-line width and never collapses to a
   * narrow column — JLabel only knows about parent width via this query path.
   *
   * @param label the label
   * @param fallback the label's super-computed preferred size (used when there's no HTML view or no
   *     parent width to base the wrap on)
   * @return the adjusted preferred size
   */
  static Dimension preferredSizeForWidth(final JLabel label, final Dimension fallback) {
    final View view = (View) label.getClientProperty("html");
    if (view == null) {
      return fallback;
    }
    final int available = availableWidth(label);
    if (available <= 0) {
      return fallback;
    }
    view.setSize(available, 0);
    final int wrappedW = (int) Math.ceil(view.getPreferredSpan(View.X_AXIS));
    final int wrappedH = (int) Math.ceil(view.getPreferredSpan(View.Y_AXIS));
    return new Dimension(Math.min(available, Math.max(wrappedW, 1)), Math.max(wrappedH, 1));
  }

  /** Walks the parent chain to find a usable width to wrap at. */
  private static int availableWidth(final JComponent label) {
    Container p = label.getParent();
    while (p != null) {
      final int w = p.getWidth();
      if (w > 0) {
        final java.awt.Insets ins = p.getInsets();
        return Math.max(0, w - ins.left - ins.right);
      }
      p = p.getParent();
    }
    return 0;
  }
}
