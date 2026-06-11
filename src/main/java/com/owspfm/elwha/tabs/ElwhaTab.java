package com.owspfm.elwha.tabs;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Objects;
import javax.swing.JComponent;

/**
 * One tab of an M3 tab bar — the dedicated tab primitive hosted by {@link ElwhaTabs}. Paints its
 * own content (label; icon forms arrive with the epic's S4 story) in the M3 {@code title-small}
 * type role with the variant's active/inactive content colors; the bar paints everything that
 * spans tabs (container fill, divider, the animated active indicator).
 *
 * <p><strong>Activation flows through the bar.</strong> {@link #isActive()} is read-only here;
 * {@link ElwhaTabs#setActiveTabIndex(int)} (or a user gesture on the tab) is the way a tab becomes
 * active — M3's noun is <em>active</em> (material-web deprecates {@code selected} for tabs). A tab
 * is meaningless outside a bar: the bar stamps the {@link TabsVariant} on add.
 *
 * <p><strong>Geometry (M3 token-locked, design §5):</strong> {@value #H_PADDING_PX}&nbsp;px
 * horizontal padding, {@value #INLINE_CONTENT_HEIGHT_PX}&nbsp;px inline content height, label in
 * {@link TypeRole#TITLE_SMALL}, single line, no wrap. Colors resolve at paint time (the binding
 * rule) — see {@code docs/research/elwha-tabs-design.md} §4.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaTab extends JComponent {

  static final int H_PADDING_PX = 16;
  static final int INLINE_CONTENT_HEIGHT_PX = 48;

  private final String label;

  private TabsVariant variant = TabsVariant.PRIMARY;
  private boolean active;

  private ElwhaTab(final String label) {
    this.label = Objects.requireNonNull(label, "label");
    setOpaque(false);
  }

  /**
   * Constructs a label-only tab.
   *
   * @param label the tab label; required
   * @return a new tab
   * @version v0.4.0
   * @since v0.4.0
   */
  public static ElwhaTab of(final String label) {
    return new ElwhaTab(label);
  }

  /**
   * The tab's label text.
   *
   * @return the label, never {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public String getLabel() {
    return label;
  }

  /**
   * Reports whether this tab is the bar's active tab. The bar is the single source of truth — this
   * getter reflects the most-recent activation push from the parent {@link ElwhaTabs}.
   *
   * @return {@code true} if this tab paints in its active form
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean isActive() {
    return active;
  }

  // Push-only from the parent bar; consumers activate through ElwhaTabs.
  void setActive(final boolean active) {
    if (this.active == active) {
      return;
    }
    this.active = active;
    repaint();
  }

  void setVariant(final TabsVariant variant) {
    if (this.variant == variant) {
      return;
    }
    this.variant = variant;
    revalidate();
    repaint();
  }

  TabsVariant getVariant() {
    return variant;
  }

  // ------------------------------------------------------------------- sizing

  @Override
  public Dimension getPreferredSize() {
    final FontMetrics fm = getFontMetrics(labelFont());
    return new Dimension(H_PADDING_PX + fm.stringWidth(label) + H_PADDING_PX,
        INLINE_CONTENT_HEIGHT_PX);
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  // ----------------------------------------------------------------- geometry

  // The horizontal span of the content cluster (the label for now), in tab coordinates — the
  // PRIMARY variant's content-hugging indicator width (material-web spans the `.content` box).
  Rectangle contentSpan() {
    final FontMetrics fm = getFontMetrics(labelFont());
    final int w = fm.stringWidth(label);
    return new Rectangle((getWidth() - w) / 2, 0, w, getHeight());
  }

  // -------------------------------------------------------------------- paint

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      paintLabel(g2);
    } finally {
      g2.dispose();
    }
  }

  private void paintLabel(final Graphics2D g2) {
    if (label.isEmpty()) {
      return;
    }
    g2.setFont(labelFont());
    final FontMetrics fm = g2.getFontMetrics();
    g2.setColor(contentColor());
    final int x = (getWidth() - fm.stringWidth(label)) / 2;
    final int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
    g2.drawString(label, x, y);
  }

  private Font labelFont() {
    return TypeRole.TITLE_SMALL.resolve();
  }

  // Active content: PRIMARY in primary tabs, ON_SURFACE in secondary tabs; inactive content:
  // ON_SURFACE_VARIANT (research §T).
  private Color contentColor() {
    if (active) {
      return (variant == TabsVariant.PRIMARY ? ColorRole.PRIMARY : ColorRole.ON_SURFACE).resolve();
    }
    return ColorRole.ON_SURFACE_VARIANT.resolve();
  }
}
