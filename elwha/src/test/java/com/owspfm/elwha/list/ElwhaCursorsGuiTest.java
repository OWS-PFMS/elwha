package com.owspfm.elwha.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.owspfm.elwha.testkit.GuiToolkit;
import java.awt.Cursor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier B coverage of the one {@link ElwhaCursors} claim headless cannot represent: that grab and
 * grabbing are two <em>different</em> pointers.
 *
 * <p>With no display, both collapse onto {@link Cursor#MOVE_CURSOR} and comparing them proves
 * nothing. With one, the loader decodes the bundled artwork into custom cursors — and the pair
 * being distinct is what makes a press visibly close the hand.
 *
 * <p>A toolkit may still refuse {@code createCustomCursor} for the size it was asked for, which the
 * loader absorbs by degrading. That is correct behaviour rather than a failure, so the distinctness
 * assertion is guarded by an assumption instead of pinned: the test reports what the display
 * granted rather than demanding a particular display.
 */
@Tag("gui")
@ExtendWith(GuiToolkit.class)
class ElwhaCursorsGuiTest {

  @Test
  void grabAndGrabbingAreDistinctCursorsOnARealDisplay() {
    final Cursor grab = ElwhaCursors.grab();
    final Cursor grabbing = ElwhaCursors.grabbing();

    assumeTrue(
        grab.getType() == Cursor.CUSTOM_CURSOR && grabbing.getType() == Cursor.CUSTOM_CURSOR,
        "toolkit declined custom cursors; the loader degraded, which is its documented fallback");

    assertThat(grab)
        .as("a press has to visibly close the hand, so the two states cannot be one cursor")
        .isNotSameAs(grabbing);
    assertThat(grab.getName()).isNotEqualTo(grabbing.getName());
  }
}
