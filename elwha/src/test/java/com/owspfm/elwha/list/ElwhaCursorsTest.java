package com.owspfm.elwha.list;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import javax.swing.JComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of {@link ElwhaCursors}, the public accessor for the grab and grabbing cursors.
 *
 * <p>The facade's whole reason to exist is that a consumer's own draggable surfaces wear the same
 * pointer {@link ElwhaItemList} wears, so the load-bearing assertion here is identity against what
 * a cursor-swap list actually installs — not a property of the cursor in isolation.
 *
 * <p>Headless collapses both states onto {@link Cursor#MOVE_CURSOR}, since there is no display to
 * build a custom cursor against. That they are <em>distinct</em> on a real display is the one claim
 * this tier cannot represent, and it lives in {@code ElwhaCursorsGuiTest}.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaCursorsTest {

  private static final int WIDTH = 240;
  private static final int HEIGHT = 400;

  /** A textless item view, matching the reorder suite's probe-safe slab. */
  private static final class Slab extends JComponent {
    @Override
    public Dimension getPreferredSize() {
      return new Dimension(180, 40);
    }

    @Override
    protected void paintComponent(final Graphics g) {
      g.setColor(new Color(180, 0, 180));
      g.fillRect(0, 0, getWidth(), getHeight());
    }
  }

  private static ElwhaItemList<String> movableListOf(final String... items) {
    final ElwhaItemList<String> list =
        new ElwhaItemList<>(new DefaultElwhaListModel<>(List.of(items)), (item, i) -> new Slab());
    list.setMovementMode(MovementMode.MOVABLE);
    list.setReorderAffordance(ReorderAffordance.CURSOR_SWAP);
    list.setSize(WIDTH, HEIGHT);
    list.doLayout();
    for (final Component child : list.getComponents()) {
      child.setSize(list.getWidth(), list.getHeight());
      ((JComponent) child).doLayout();
    }
    return list;
  }

  // ------------------------------------------------------------- contract

  @Test
  void bothCursorsResolveWithoutADisplay() {
    assertThat(ElwhaCursors.grab())
        .as("grab() degrades rather than failing, so a consumer never branches on availability")
        .isNotNull();
    assertThat(ElwhaCursors.grabbing()).as("grabbing() degrades the same way").isNotNull();
  }

  @Test
  void repeatedCallsHandBackTheCachedCursor() {
    assertThat(ElwhaCursors.grab())
        .as("the cursor is built once and cached, so call-at-point-of-use stays cheap")
        .isSameAs(ElwhaCursors.grab());
    assertThat(ElwhaCursors.grabbing()).isSameAs(ElwhaCursors.grabbing());
  }

  @Test
  void facadeHandsOutTheSameCursorAListInstalls() {
    final ElwhaItemList<String> list = movableListOf("a", "b");

    assertThat(list.getComponentFor("a").getCursor())
        .as("a consumer's own draggable surface must read identically to an Elwha list's")
        .isSameAs(ElwhaCursors.grab());
  }

  // -------------------------------------------------------------- surface

  @Test
  void facadeIsReachableFromOutsideThePackage() throws NoSuchMethodException {
    assertThat(Modifier.isPublic(ElwhaCursors.class.getModifiers()))
        .as("the class a consumer imports")
        .isTrue();

    for (final String name : List.of("grab", "grabbing")) {
      final Method method = ElwhaCursors.class.getMethod(name);
      assertThat(
              Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers()))
          .as("%s() is the published accessor", name)
          .isTrue();
    }
  }

  @Test
  void loaderAndItsCacheControlsStayInternal() throws NoSuchMethodException {
    assertThat(Modifier.isPublic(ReorderCursors.class.getModifiers()))
        .as("the three-tier loader is an implementation detail, free to change")
        .isFalse();

    for (final Method method :
        List.of(
            ReorderCursors.class.getDeclaredMethod("invalidate"),
            ReorderCursors.class.getDeclaredMethod("invalidateOnce", Object.class),
            ReorderCursors.class.getDeclaredMethod("generation"))) {
      assertThat(Modifier.isPublic(method.getModifiers()))
          .as("%s is cache plumbing ElwhaItemList drives, not consumer API", method.getName())
          .isFalse();
    }
  }
}
