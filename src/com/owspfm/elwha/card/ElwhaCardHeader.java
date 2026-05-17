package com.owspfm.elwha.card;

import com.owspfm.elwha.theme.SpaceScale;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
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
    setLayout(new BorderLayout(gap, 0));
    setOpaque(false);

    leadingHolder = new JPanel(new BorderLayout());
    leadingHolder.setOpaque(false);
    leadingHolder.setVisible(false);
    add(leadingHolder, BorderLayout.WEST);

    textStack = new JPanel();
    textStack.setOpaque(false);
    textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
    add(textStack, BorderLayout.CENTER);

    trailingRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, gap, 0));
    trailingRow.setOpaque(false);
    trailingRow.setVisible(false);
    add(trailingRow, BorderLayout.EAST);
  }

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
    leadingHolder.add(leading, BorderLayout.CENTER);
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
