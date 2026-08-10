package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.LogCapture;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A contract coverage for {@link AccessibleNameAdvisory}: the fire condition, the uniform
 * message shape, and the once-per-instance throttle.
 */
@ExtendWith(EdtInterceptor.class)
class AccessibleNameAdvisoryTest {

  private static final class ProbeOwner {}

  private static final String REMEDY = "Call setAccessibleName(...).";

  @Test
  void warnsWhenTheResolvedNameIsMissing() {
    try (LogCapture log = LogCapture.of(ProbeOwner.class)) {
      AccessibleNameAdvisory.checkOnce(new JPanel(), ProbeOwner.class, null, REMEDY);

      assertThat(log.messages())
          .as("a null resolved name fires the advisory with the uniform message shape")
          .containsExactly("ProbeOwner has no accessible name. " + REMEDY);
    }
  }

  @Test
  void anEmptyNameCountsAsMissing() {
    try (LogCapture log = LogCapture.of(ProbeOwner.class)) {
      AccessibleNameAdvisory.checkOnce(new JPanel(), ProbeOwner.class, "", REMEDY);

      assertThat(log.messages()).as("an empty resolved name fires the advisory").hasSize(1);
    }
  }

  @Test
  void staysSilentWhenTheNameResolves() {
    try (LogCapture log = LogCapture.of(ProbeOwner.class)) {
      AccessibleNameAdvisory.checkOnce(
          new JPanel(), ProbeOwner.class, "Primary navigation", REMEDY);

      assertThat(log.messages()).as("a resolved name keeps the advisory silent").isEmpty();
    }
  }

  @Test
  void warnsAtMostOncePerInstance() {
    try (LogCapture log = LogCapture.of(ProbeOwner.class)) {
      final JPanel component = new JPanel();
      AccessibleNameAdvisory.checkOnce(component, ProbeOwner.class, null, REMEDY);
      AccessibleNameAdvisory.checkOnce(component, ProbeOwner.class, null, REMEDY);
      AccessibleNameAdvisory.checkOnce(component, ProbeOwner.class, null, REMEDY);

      assertThat(log.messages()).as("repeat checks on the same instance are no-ops").hasSize(1);
    }
  }

  @Test
  void firstCheckDecidesForTheInstance() {
    try (LogCapture log = LogCapture.of(ProbeOwner.class)) {
      final JPanel component = new JPanel();
      AccessibleNameAdvisory.checkOnce(component, ProbeOwner.class, "Named early", REMEDY);
      AccessibleNameAdvisory.checkOnce(component, ProbeOwner.class, null, REMEDY);

      assertThat(log.messages())
          .as("an instance that passed its first-paint check is never re-examined")
          .isEmpty();
    }
  }

  @Test
  void distinctInstancesAreCheckedIndependently() {
    try (LogCapture log = LogCapture.of(ProbeOwner.class)) {
      AccessibleNameAdvisory.checkOnce(new JPanel(), ProbeOwner.class, null, REMEDY);
      AccessibleNameAdvisory.checkOnce(new JPanel(), ProbeOwner.class, null, REMEDY);

      assertThat(log.messages()).as("the throttle is per instance, not per class").hasSize(2);
    }
  }

  @Test
  void aNullComponentIsANoOp() {
    try (LogCapture log = LogCapture.of(ProbeOwner.class)) {
      AccessibleNameAdvisory.checkOnce(null, ProbeOwner.class, null, REMEDY);

      assertThat(log.messages()).as("null component never warns and never throws").isEmpty();
    }
  }
}
