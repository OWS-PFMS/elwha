package com.owspfm.elwha.card;

import com.owspfm.elwha.theme.SpaceScale;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * The card-header layout primitive — composes Layer 2 atoms into the M3 header anatomy: a single
 * leading slot (icon OR thumbnail, never multi per M3), a title + optional subtitle stack, and an
 * open-ended trailing row of affordances (icon button / chip / overflow trigger).
 *
 * <p>Internal layout: leading column + title/subtitle box + trailing row, with {@link
 * SpaceScale#SM} (8 dp) gaps between segments. See {@code docs/research/elwha-card-v3-spec.md}
 * §5.1.
 *
 * <p><strong>Single leading vs N trailing.</strong> The leading slot is single per M3 doctrine
 * ({@link #setLeading}, {@link #clearLeading}); the trailing row accepts multiple affordances via
 * repeated {@link #addTrailing} calls — header trailing is polymorphic across icon buttons, chips,
 * and overflow triggers.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class ElwhaCardHeader extends JComponent {

  private final JPanel leadingHolder;
  private final JPanel textStack;
  private final JPanel trailingRow;
  private ElwhaCardTitle title;
  private ElwhaCardSubtitle subtitle;
  private final List<JComponent> trailing = new ArrayList<>();

  /** Creates an empty header. Add leading / title / subtitle / trailing via the fluent setters. */
  public ElwhaCardHeader() {
    final int gap = SpaceScale.SM.px();
    setOpaque(false);
    setLayout(new BaselineRowLayout(gap));

    leadingHolder = new JPanel();
    leadingHolder.setLayout(new BoxLayout(leadingHolder, BoxLayout.X_AXIS));
    leadingHolder.setOpaque(false);
    leadingHolder.setVisible(false);
    add(leadingHolder);

    textStack = new JPanel();
    textStack.setOpaque(false);
    textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
    add(textStack);

    trailingRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, gap, 0));
    trailingRow.setOpaque(false);
    trailingRow.setVisible(false);
    add(trailingRow);
  }

  /**
   * Custom 3-segment LayoutManager that aligns leading + trailing on the title's text baseline, per
   * spec §5.1 — "Vertical alignment: title baseline shares row baseline with leading and trailing
   * items." Leading column is left-anchored, trailing row is right-anchored, text stack flexes to
   * take the remainder.
   */
  private final class BaselineRowLayout implements LayoutManager {
    private final int gap;

    BaselineRowLayout(final int gap) {
      this.gap = gap;
    }

    @Override
    public void addLayoutComponent(final String name, final Component comp) {
      // no-op
    }

    @Override
    public void removeLayoutComponent(final Component comp) {
      // no-op
    }

    @Override
    public Dimension preferredLayoutSize(final Container parent) {
      final Dimension lp = leadingHolder.isVisible() ? leadingHolder.getPreferredSize() : ZERO;
      final Dimension rp = trailingRow.isVisible() ? trailingRow.getPreferredSize() : ZERO;
      final Dimension tp = textStack.getPreferredSize();
      final int leadingW = lp.width + (leadingHolder.isVisible() ? gap : 0);
      final int trailingW = rp.width + (trailingRow.isVisible() ? gap : 0);
      final int width = leadingW + tp.width + trailingW;
      final int height = baselineAlignedHeight(lp, tp, rp);
      return new Dimension(width, height);
    }

    @Override
    public Dimension minimumLayoutSize(final Container parent) {
      return preferredLayoutSize(parent);
    }

    @Override
    public void layoutContainer(final Container parent) {
      final int width = parent.getWidth();
      final Dimension lp = leadingHolder.isVisible() ? leadingHolder.getPreferredSize() : ZERO;
      final Dimension rp = trailingRow.isVisible() ? trailingRow.getPreferredSize() : ZERO;
      final int leadingW = lp.width + (leadingHolder.isVisible() ? gap : 0);
      final int trailingW = rp.width + (trailingRow.isVisible() ? gap : 0);
      final int textW = Math.max(0, width - leadingW - trailingW);
      final Dimension tp = new Dimension(textW, textStack.getPreferredSize().height);
      // Target Y for the shared baseline = max of every component's own baseline. Using only the
      // textStack's baseline (the spec's "title baseline") as target would push taller siblings
      // — icon buttons in particular, whose fallback baseline is height/2 — into negative Y,
      // and Swing would clip the tops.
      final int targetBaseline = rowBaseline(lp, tp, rp);
      final int leadingY = topForBaseline(leadingHolder, lp, targetBaseline);
      final int textY = topForBaseline(textStack, tp, targetBaseline);
      final int trailingY = topForBaseline(trailingRow, rp, targetBaseline);
      if (leadingHolder.isVisible()) {
        leadingHolder.setBounds(0, leadingY, lp.width, lp.height);
      }
      textStack.setBounds(leadingW, textY, textW, tp.height);
      if (trailingRow.isVisible()) {
        trailingRow.setBounds(width - rp.width, trailingY, rp.width, rp.height);
      }
    }

    /** Shared baseline Y for the row = deepest top-above-baseline across all visible segments. */
    private int rowBaseline(final Dimension lp, final Dimension tp, final Dimension rp) {
      int max = resolveBaseline(textStack, tp);
      if (leadingHolder.isVisible()) {
        max = Math.max(max, resolveBaseline(leadingHolder, lp));
      }
      if (trailingRow.isVisible()) {
        max = Math.max(max, resolveBaseline(trailingRow, rp));
      }
      return max;
    }

    private int baselineAlignedHeight(final Dimension lp, final Dimension tp, final Dimension rp) {
      final int target = rowBaseline(lp, tp, rp);
      int bottomMax = tp.height - resolveBaseline(textStack, tp);
      if (leadingHolder.isVisible()) {
        bottomMax = Math.max(bottomMax, lp.height - resolveBaseline(leadingHolder, lp));
      }
      if (trailingRow.isVisible()) {
        bottomMax = Math.max(bottomMax, rp.height - resolveBaseline(trailingRow, rp));
      }
      return target + bottomMax;
    }

    /**
     * Returns the baseline of {@code component} at the given size, or a center fallback when the
     * component does not have a baseline (e.g. icon-only buttons).
     */
    private int resolveBaseline(final JComponent component, final Dimension size) {
      if (size.width <= 0 || size.height <= 0) {
        return 0;
      }
      final int b = component.getBaseline(size.width, size.height);
      return b >= 0 ? b : size.height / 2;
    }

    private int topForBaseline(final JComponent component, final Dimension size, final int target) {
      return target - resolveBaseline(component, size);
    }
  }

  private static final Dimension ZERO = new Dimension(0, 0);

  /**
   * Sets the leading slot — typically an {@link ElwhaCardLeadingIcon} or {@link
   * ElwhaCardThumbnail}. Pass {@code null} via {@link #clearLeading()} to remove.
   *
   * @param leading the leading component (must not be {@code null})
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if {@code leading} is {@code null}
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardHeader setLeading(final JComponent leading) {
    Objects.requireNonNull(leading, "leading");
    leadingHolder.removeAll();
    leadingHolder.add(leading);
    leadingHolder.setVisible(true);
    revalidate();
    repaint();
    return this;
  }

  /**
   * @return the leading component, or {@code null} if none
   * @version v0.2.0
   * @since v0.2.0
   */
  public JComponent getLeading() {
    if (leadingHolder.getComponentCount() == 0) {
      return null;
    }
    final Component c = leadingHolder.getComponent(0);
    return c instanceof JComponent jc ? jc : null;
  }

  /**
   * Removes the leading component.
   *
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardHeader clearLeading() {
    leadingHolder.removeAll();
    leadingHolder.setVisible(false);
    revalidate();
    repaint();
    return this;
  }

  /**
   * Shorthand: sets the title using a fresh {@link ElwhaCardTitle}.
   *
   * @param text the title text
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardHeader setTitle(final String text) {
    return setTitle(new ElwhaCardTitle(text));
  }

  /**
   * Sets the title to a caller-constructed {@link ElwhaCardTitle} — use when typography
   * customization beyond defaults is needed.
   *
   * @param newTitle the title atom (must not be {@code null})
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if {@code newTitle} is {@code null}
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardHeader setTitle(final ElwhaCardTitle newTitle) {
    Objects.requireNonNull(newTitle, "title");
    if (this.title != null) {
      textStack.remove(this.title);
    }
    this.title = newTitle;
    newTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
    textStack.add(newTitle, 0);
    revalidate();
    repaint();
    return this;
  }

  /**
   * @return the current title atom, or {@code null} if none
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardTitle getTitle() {
    return title;
  }

  /**
   * Shorthand: sets the subtitle using a fresh {@link ElwhaCardSubtitle}.
   *
   * @param text the subtitle text
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardHeader setSubtitle(final String text) {
    return setSubtitle(new ElwhaCardSubtitle(text));
  }

  /**
   * Sets the subtitle to a caller-constructed {@link ElwhaCardSubtitle}.
   *
   * @param newSubtitle the subtitle atom (must not be {@code null})
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if {@code newSubtitle} is {@code null}
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardHeader setSubtitle(final ElwhaCardSubtitle newSubtitle) {
    Objects.requireNonNull(newSubtitle, "subtitle");
    if (this.subtitle != null) {
      textStack.remove(this.subtitle);
    }
    this.subtitle = newSubtitle;
    newSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
    final int index = title != null ? 1 : 0;
    textStack.add(newSubtitle, index);
    revalidate();
    repaint();
    return this;
  }

  /**
   * @return the current subtitle atom, or {@code null} if none
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardSubtitle getSubtitle() {
    return subtitle;
  }

  /**
   * Adds a trailing affordance to the header. Trailing is polymorphic per M3 — icon buttons, chips,
   * and overflow triggers are all valid. Multiple calls stack right-to-left.
   *
   * @param affordance the trailing component (must not be {@code null})
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if {@code affordance} is {@code null}
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardHeader addTrailing(final JComponent affordance) {
    Objects.requireNonNull(affordance, "affordance");
    trailing.add(affordance);
    trailingRow.add(affordance);
    trailingRow.setVisible(true);
    revalidate();
    repaint();
    return this;
  }

  /**
   * Removes all trailing affordances.
   *
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardHeader clearTrailing() {
    trailing.clear();
    trailingRow.removeAll();
    trailingRow.setVisible(false);
    revalidate();
    repaint();
    return this;
  }

  /**
   * @return an unmodifiable snapshot of the trailing affordances in insertion order
   * @version v0.2.0
   * @since v0.2.0
   */
  public List<JComponent> getTrailingItems() {
    return Collections.unmodifiableList(new ArrayList<>(trailing));
  }
}
