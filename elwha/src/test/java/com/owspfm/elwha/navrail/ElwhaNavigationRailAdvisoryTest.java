package com.owspfm.elwha.navrail;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.LogCapture;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the rail's missing-accessible-name advisory after its migration onto the
 * shared {@link com.owspfm.elwha.theme.AccessibleNameAdvisory} helper: same lifecycle point (first
 * paint), same once-per-instance throttle, same message subject.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaNavigationRailAdvisoryTest {

  @Test
  void anUnnamedRailWarnsOnceAtFirstPaint() {
    try (LogCapture log = LogCapture.of(ElwhaNavigationRail.class)) {
      final ElwhaNavigationRail rail = RailFixture.laidOut(ElwhaNavigationRail.collapsed(), 3);
      Pixels.render(rail, rail.getWidth(), rail.getHeight());
      Pixels.render(rail, rail.getWidth(), rail.getHeight());

      assertThat(log.messages())
          .as("an unnamed rail warns exactly once, at first paint")
          .hasSize(1);
      assertThat(log.messages().get(0)).startsWith("ElwhaNavigationRail has no accessible name.");
    }
  }

  @Test
  void aNamedRailStaysSilent() {
    try (LogCapture log = LogCapture.of(ElwhaNavigationRail.class)) {
      final ElwhaNavigationRail rail = RailFixture.laidOut(ElwhaNavigationRail.collapsed(), 3);
      rail.getAccessibleContext().setAccessibleName("Primary navigation");
      Pixels.render(rail, rail.getWidth(), rail.getHeight());

      assertThat(log.messages()).as("a named rail never trips the advisory").isEmpty();
    }
  }
}
