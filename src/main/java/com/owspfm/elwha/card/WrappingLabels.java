package com.owspfm.elwha.card;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
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

    // Preferred WIDTH must be stable across layout passes — independent of whatever width the
    // parent has assigned this iteration. Otherwise a feedback loop forms:
    //
    //   returned-width depends on available-from-parent → parent.preferredSize grows →
    //   parent's parent grows → available grows → returned-width grows → ...
    //
    // The loop's per-pass delta is 2 * padH whenever the atom's parent is an ElwhaCard whose
    // VerticalCardLayout adds its own padH around children: WrappingLabels.availableWidth walks
    // past the chassis-internal padH boundary (Container.getInsets() is shadow reserve only),
    // so it over-reports by 2*padH, the chassis re-adds 2*padH around the reported width when
    // computing its own preferred, and the round-trip grows by exactly 2*padH each pass.
    // Confirmed empirically: MediaLoopProbe shows +32 px per layout pass on a card with
    // SpaceScale.LG (16 dp) horizontal padding.
    //
    // Fix: report the NATURAL single-line width — an intrinsic property of the text + font that
    // does not depend on the layout cycle. We measure it directly via FontMetrics on the raw
    // (HTML-stripped) text rather than asking the BasicHTML view: the view's getPreferredSpan(X)
    // and getMaximumSpan(X) both echo the most-recently-setSize'd width for an HTML JLabel at
    // wide sizes — verified empirically via MediaLoopProbe before applying this fix — so they
    // can't be used as a layout-cycle-independent natural-width source.
    //
    // FontMetrics on the plain text is an approximation (styled spans like <b> render slightly
    // wider than measured), but preferred-size is a layout hint, not a paint constraint — the
    // approximation is fine. Parents constrain via setBounds; the HTML view wraps at the
    // assigned width at paint time regardless of what preferred-size reported.
    final int naturalW = naturalSingleLineWidth(label);

    // HEIGHT tracks the actual available width — wrapping at a narrower width produces taller
    // content. This is safe because height does NOT feed into the parent's width preference;
    // there's no analogous Y-axis loop.
    final int available = availableWidth(label);
    final int prefH;
    if (available <= 0) {
      prefH = (int) Math.ceil(view.getPreferredSpan(View.Y_AXIS));
    } else {
      view.setSize(available, 0);
      prefH = (int) Math.ceil(view.getPreferredSpan(View.Y_AXIS));
    }

    return new Dimension(Math.max(naturalW, 1), Math.max(prefH, 1));
  }

  /**
   * Measures the natural single-line width of {@code label.getText()} using the label's current
   * font. HTML tags are stripped (a rough measurement — bold / styled runs render slightly wider
   * than the plain-text measurement, but that's an acceptable approximation for a preferred-size
   * hint). Returns 0 for null or empty text.
   */
  private static int naturalSingleLineWidth(final JLabel label) {
    final String text = label.getText();
    if (text == null || text.isEmpty()) {
      return 0;
    }
    final String plain;
    if (text.toLowerCase().startsWith("<html>")) {
      plain = text.replaceAll("<[^>]+>", "");
    } else {
      plain = text;
    }
    final FontMetrics fm = label.getFontMetrics(label.getFont());
    return fm.stringWidth(plain);
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
