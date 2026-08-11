package com.owspfm.elwha.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.HeadlessHost;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLayeredPane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the submenu chain's bookkeeping — open, swap, and unwind — on a {@link
 * HeadlessHost}.
 *
 * <p>This tier was structurally unreachable before #709. The chain-active recomputation reads the
 * OS pointer through {@code MouseInfo.getPointerInfo()}, which <em>throws</em> {@code
 * HeadlessException} rather than answering {@code null}, so every path through it — which is most
 * of the submenu machinery — blew up with no display. #604's regression had to be written against a
 * real Robot for that reason alone, even though the behaviour it pins (a swap closes the outgoing
 * menu) has nothing to do with a pointer.
 *
 * <p>The pointer read is now guarded, and a headless answer of "the platform will not tell me"
 * routes into the same focus/keyboard fallback a locked screen would. What still belongs to the
 * {@code gui} tier: hover-intent dwell, the corner morph the pointer actually drives, and real
 * focus arbitration.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaSubMenuChainTest {

  private HeadlessHost host;
  private final List<ElwhaMenu> opened = new ArrayList<>();

  @BeforeEach
  void mountAHost() {
    host = new HeadlessHost(700, 500);
  }

  @AfterEach
  void closeEveryMenu() {
    for (final ElwhaMenu menu : opened) {
      menu.close();
    }
    opened.clear();
  }

  private ElwhaMenu open(final ElwhaMenu menu) {
    opened.add(menu);
    menu.open(host.anchor());
    return menu;
  }

  private long mountedSurfaces() {
    final JLayeredPane pane = host.layeredPane();
    return List.of(pane.getComponents()).stream()
        .filter(c -> c.getClass().getSimpleName().contains("MenuSurface"))
        .count();
  }

  private static ElwhaMenu menuWith(final ElwhaSubMenuItem opener) {
    return ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Grid")).addItem(opener).build();
  }

  private static ElwhaMenu leafMenu(final String label) {
    return ElwhaMenu.builder().addItem(ElwhaMenuItem.of(label)).build();
  }

  @Test
  void recomputingTheActiveLevelSurvivesAnAbsentPointer() {
    final ElwhaSubMenuItem share = ElwhaSubMenuItem.of("Share", leafMenu("Email"));
    final ElwhaMenu menu = open(menuWith(share));
    menu.requestOpenSubMenu(share);

    assertThat(share.isExpanded()).as("the submenu opens with no display present").isTrue();
    assertThatCode(menu::recomputeChainActive)
        .as("the chain-active pass reads the pointer defensively rather than throwing")
        .doesNotThrowAnyException();
  }

  @Test
  void swappingAnOpenSubMenuClosesTheOutgoingOne() {
    final ElwhaMenu outgoing = leafMenu("Email");
    final ElwhaMenu incoming = leafMenu("Link");
    final ElwhaSubMenuItem share = ElwhaSubMenuItem.of("Share", outgoing);
    final ElwhaMenu menu = open(menuWith(share));
    menu.requestOpenSubMenu(share);
    assertThat(mountedSurfaces()).as("the parent and its submenu are both mounted").isEqualTo(2);

    share.setSubMenu(incoming);

    assertThat(mountedSurfaces())
        .as("#604 — the outgoing menu stayed mounted with no opener able to reach it")
        .isEqualTo(1);
    assertThat(share.isExpanded())
        .as("and the row kept reporting EXPANDED with nothing open, disarming hover-to-open")
        .isFalse();
    assertThat(share.getSubMenu()).isSameAs(incoming);
  }

  @Test
  void closingTheParentTearsDownTheOpenSubMenu() {
    final ElwhaSubMenuItem share = ElwhaSubMenuItem.of("Share", leafMenu("Email"));
    final ElwhaMenu menu = open(menuWith(share));
    menu.requestOpenSubMenu(share);
    assertThat(mountedSurfaces()).isEqualTo(2);

    menu.close();

    assertThat(mountedSurfaces()).as("the ancestor teardown cascades to the leaf").isZero();
    assertThat(share.isExpanded()).isFalse();
  }

  @Test
  void reopeningTheSameOpenerIsANoOpRatherThanASecondMount() {
    final ElwhaSubMenuItem share = ElwhaSubMenuItem.of("Share", leafMenu("Email"));
    final ElwhaMenu menu = open(menuWith(share));

    menu.requestOpenSubMenu(share);
    menu.requestOpenSubMenu(share);

    assertThat(mountedSurfaces()).as("a parent holds at most one open submenu").isEqualTo(2);
  }
}
