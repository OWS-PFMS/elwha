package com.owspfm.elwha.card;

import com.owspfm.elwha.theme.SpaceScale;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * The card-actions layout primitive — composes action affordances into M3's two-segment row:
 * leading (left-anchored) + flex gap + trailing (right-anchored). Intra-segment spacing {@link
 * SpaceScale#SM} (8 dp); inter-segment is flex.
 *
 * <p>Either segment may be empty. M3 doctrine: lone promo actions go leading; paired actions or
 * overflow-bearing rows go trailing.
 *
 * <p>See {@code docs/research/elwha-card-v3-spec.md} §5.3.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class ElwhaCardActions extends JComponent {

  private final JPanel leadingSegment;
  private final JPanel trailingSegment;
  private final List<JComponent> leading = new ArrayList<>();
  private final List<JComponent> trailing = new ArrayList<>();

  /** Creates an empty action row. */
  public ElwhaCardActions() {
    final int gap = SpaceScale.SM.px();
    setLayout(new BorderLayout(0, 0));
    setOpaque(false);
    leadingSegment = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0));
    leadingSegment.setOpaque(false);
    leadingSegment.setVisible(false);
    add(leadingSegment, BorderLayout.WEST);
    add(Box.createHorizontalGlue(), BorderLayout.CENTER);
    trailingSegment = new JPanel(new FlowLayout(FlowLayout.RIGHT, gap, 0));
    trailingSegment.setOpaque(false);
    trailingSegment.setVisible(false);
    add(trailingSegment, BorderLayout.EAST);
  }

  /**
   * Adds an action to the leading (left-anchored) segment.
   *
   * @param action the action component (must not be {@code null})
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if {@code action} is {@code null}
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardActions addLeading(final JComponent action) {
    Objects.requireNonNull(action, "action");
    leading.add(action);
    leadingSegment.add(action);
    leadingSegment.setVisible(true);
    revalidate();
    repaint();
    return this;
  }

  /**
   * Removes every leading action.
   *
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardActions clearLeading() {
    leading.clear();
    leadingSegment.removeAll();
    leadingSegment.setVisible(false);
    revalidate();
    repaint();
    return this;
  }

  /**
   * @return an unmodifiable snapshot of the leading actions in insertion order
   * @version v0.2.0
   * @since v0.2.0
   */
  public List<JComponent> getLeadingActions() {
    return Collections.unmodifiableList(new ArrayList<>(leading));
  }

  /**
   * Adds an action to the trailing (right-anchored) segment.
   *
   * @param action the action component (must not be {@code null})
   * @return {@code this} for fluent chaining
   * @throws NullPointerException if {@code action} is {@code null}
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardActions addTrailing(final JComponent action) {
    Objects.requireNonNull(action, "action");
    trailing.add(action);
    trailingSegment.add(action);
    trailingSegment.setVisible(true);
    revalidate();
    repaint();
    return this;
  }

  /**
   * Removes every trailing action.
   *
   * @return {@code this} for fluent chaining
   * @version v0.2.0
   * @since v0.2.0
   */
  public ElwhaCardActions clearTrailing() {
    trailing.clear();
    trailingSegment.removeAll();
    trailingSegment.setVisible(false);
    revalidate();
    repaint();
    return this;
  }

  /**
   * @return an unmodifiable snapshot of the trailing actions in insertion order
   * @version v0.2.0
   * @since v0.2.0
   */
  public List<JComponent> getTrailingActions() {
    return Collections.unmodifiableList(new ArrayList<>(trailing));
  }
}
