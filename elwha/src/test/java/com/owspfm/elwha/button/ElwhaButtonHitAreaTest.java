package com.owspfm.elwha.button;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.BodyBearing;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of where an {@link ElwhaButton} accepts a click (#505).
 *
 * <p>The hit test used to be the whole component minus the shadow reserve, on the reasoning that
 * the accessibility inflation padding should stay clickable. That is right while bounds equal
 * preferred size and wrong the moment they do not: a stretching layout grows the bounds arbitrarily
 * and the same test grew the hit area with them. The filed repro measured a 40&nbsp;px pill in
 * 162&nbsp;px of granted height accepting clicks about 57&nbsp;px above <em>and</em> below itself —
 * roughly 1.4× the pill's own height of dead clickable space on each side, which is what an
 * operator noticed.
 *
 * <p>The rect is now derived from the visible body inflated to the WCAG 2.5.5 minimum target and
 * centered on it, so the a11y target survives at preferred size — where the inflation is exactly
 * what the minimum asks for — and stops growing under stretch. The geometry below reproduces the
 * filed measurements rather than inventing new ones.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaButtonHitAreaTest {

  /** The stretched height from the filed side-sheet repro. */
  private static final int STRETCHED_HEIGHT = 162;

  private static ElwhaButton button() {
    final ElwhaButton button = ElwhaButton.filledButton("Open dialog above sheet");
    button.setSize(button.getPreferredSize());
    return button;
  }

  private static List<String> fired(final ElwhaButton button) {
    final List<String> fired = new ArrayList<>();
    button.addActionListener(e -> fired.add("click"));
    return fired;
  }

  @Test
  void aStretchedButtonRejectsClicksInTheDeadSpaceAboveAndBelowItsPill() {
    final ElwhaButton button = button();
    final int pillHeight = button.getBodyBounds().height;
    button.setSize(button.getWidth(), STRETCHED_HEIGHT);
    final List<String> fired = fired(button);

    // The exact points the repro reported as wrongly clickable: a little over one pill-height out
    // from centre, on each side.
    Input.click(button, button.getWidth() / 2, STRETCHED_HEIGHT / 2 - pillHeight);
    Input.click(button, button.getWidth() / 2, STRETCHED_HEIGHT / 2 + pillHeight);

    assertThat(fired)
        .as(
            "#505 — %dpx of granted height around a %dpx pill is not a click target",
            STRETCHED_HEIGHT, pillHeight)
        .isEmpty();
  }

  @Test
  void aStretchedButtonStillAcceptsClicksOnItsPill() {
    final ElwhaButton button = button();
    button.setSize(button.getWidth(), STRETCHED_HEIGHT);
    final List<String> fired = fired(button);
    final Rectangle body = button.getBodyBounds();

    Input.click(button, body.x + body.width / 2, body.y + body.height / 2);

    assertThat(fired).as("the visible pill is still the button").hasSize(1);
  }

  @ParameterizedTest
  @EnumSource(ButtonSize.class)
  void accessibilityTargetSurvivesAtPreferredSize(final ButtonSize size) {
    final ElwhaButton button = ElwhaButton.filledButton("Save").setButtonSize(size);
    button.setSize(button.getPreferredSize());
    final List<String> fired = fired(button);

    // One pixel inside the top edge — inflation padding on XS/S, the pill itself on the rest.
    Input.click(button, button.getWidth() / 2, 1);

    assertThat(fired)
        .as(
            "%s must keep its %ddp minimum target — capping the hit area must not shrink it below"
                + " what WCAG 2.5.5 asks for",
            size, size.minimumTargetPx())
        .hasSize(1);
  }

  @Test
  void hitAreaTracksTheBodyRatherThanTheBounds() {
    final ElwhaButton button = button();
    final int pillHeight = button.getBodyBounds().height;

    button.setSize(button.getWidth(), STRETCHED_HEIGHT);
    final List<String> fired = fired(button);

    // Walk down the centre column and record where clicks land, then compare the accepting band
    // against the body inflated to the target. This is the property, not a spot check.
    int accepted = 0;
    for (int y = 0; y < STRETCHED_HEIGHT; y++) {
      final int before = fired.size();
      Input.click(button, button.getWidth() / 2, y);
      if (fired.size() > before) {
        accepted++;
      }
    }

    final int expected = Math.max(pillHeight, button.getButtonSize().minimumTargetPx());
    assertThat(accepted)
        .as("the accepting band is the pill grown to the minimum target, not the granted height")
        .isEqualTo(expected);
    assertThat(accepted).isLessThan(STRETCHED_HEIGHT);
  }

  @Test
  void bodyAndTheHitAreaAgreeWithTheResolverEveryConsumerUses() {
    final ElwhaButton button = button();
    button.setSize(button.getWidth() + 120, STRETCHED_HEIGHT);

    assertThat(BodyBearing.bodyBoundsOf(button))
        .as("a tooltip, a menu and the hit test all measure from the same rect")
        .isEqualTo(button.getBodyBounds());
  }
}
