package com.owspfm.elwha.chip;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the chip's pass-through mouse registration — the family #731 found add-only.
 *
 * <p>It is a different shape from the wrappers #725 retired, and that is why it earns its {@code
 * remove} rather than being retired too. Those forwarded to one inherited method, so the inherited
 * method was already the whole surface. This one fans a single listener out across four components
 * — the chip body plus the content row and the two clusters that would otherwise swallow the event
 * on the way up — so the inherited {@code removeMouseListener} undoes a quarter of it and leaves
 * three live registrations behind, silently.
 *
 * @see <a href="https://github.com/OWS-PFMS/elwha/issues/731">#731</a>
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaChipPassThroughListenerTest {

  private static final int WIDTH = 120;
  private static final int HEIGHT = 40;

  private static ElwhaChip sized() {
    final ElwhaChip chip = new ElwhaChip("Demand");
    chip.setSize(WIDTH, HEIGHT);
    chip.doLayout();
    return chip;
  }

  /** The chip body plus the three inner components the pass-through also registers on. */
  private static List<Component> reach(final ElwhaChip chip) {
    final Container contentRow = (Container) chip.getComponent(0);
    return List.of(chip, contentRow, contentRow.getComponent(0), contentRow.getComponent(1));
  }

  private static long registrations(final Component target, final MouseListener listener) {
    return Arrays.stream(target.getMouseListeners()).filter(l -> l == listener).count();
  }

  private static long registrations(final Component target, final MouseMotionListener listener) {
    return Arrays.stream(target.getMouseMotionListeners()).filter(l -> l == listener).count();
  }

  // ------------------------------------------------------------- the reach

  @Test
  void theAddReachesTheBodyAndAllThreeInnerComponents() {
    final ElwhaChip chip = sized();
    final MouseListener listener = new MouseAdapter() {};

    chip.addChipMouseListener(listener);

    for (final Component target : reach(chip)) {
      assertThat(registrations(target, listener))
          .as("the pass-through registers on %s", target.getClass().getSimpleName())
          .isOne();
    }
  }

  @Test
  void theRemoveDetachesFromAllFour() {
    final ElwhaChip chip = sized();
    final MouseListener listener = new MouseAdapter() {};
    chip.addChipMouseListener(listener);

    chip.removeChipMouseListener(listener);

    for (final Component target : reach(chip)) {
      assertThat(registrations(target, listener))
          .as("nothing is left behind on %s", target.getClass().getSimpleName())
          .isZero();
    }
  }

  /**
   * Why the method has to exist. Without it a consumer reaches for the inherited call, which
   * detaches the one registration they can see and leaves the three they cannot.
   */
  @Test
  void theInheritedRemoveAloneWouldStrandThreeRegistrations() {
    final ElwhaChip chip = sized();
    final MouseListener listener = new MouseAdapter() {};
    chip.addChipMouseListener(listener);

    chip.removeMouseListener(listener);

    final List<Component> reach = reach(chip);
    assertThat(registrations(reach.get(0), listener)).as("the visible half comes off").isZero();
    assertThat(reach.subList(1, reach.size()))
        .as("and the three inner registrations survive — the leak #731 named")
        .allSatisfy(inner -> assertThat(registrations(inner, listener)).isOne());
  }

  @Test
  void aDetachedListenerStopsHearingEventsFromTheInnerRow() {
    final ElwhaChip chip = sized();
    final int[] presses = {0};
    final MouseListener listener =
        new MouseAdapter() {
          @Override
          public void mousePressed(final MouseEvent event) {
            presses[0]++;
          }
        };
    final Component contentRow = reach(chip).get(1);
    chip.addChipMouseListener(listener);
    Input.press(contentRow, 4, 4);

    chip.removeChipMouseListener(listener);
    Input.press(contentRow, 4, 4);

    assertThat(presses[0]).as("one press heard while attached, none after").isOne();
  }

  // ------------------------------------------------------------ the mirror

  @Test
  void theMotionAddAndRemoveCoverTheSameFour() {
    final ElwhaChip chip = sized();
    final MouseMotionListener listener = new MouseMotionAdapter() {};

    chip.addChipMouseMotionListener(listener);
    for (final Component target : reach(chip)) {
      assertThat(registrations(target, listener))
          .as("the motion pass-through registers on %s", target.getClass().getSimpleName())
          .isOne();
    }

    chip.removeChipMouseMotionListener(listener);
    for (final Component target : reach(chip)) {
      assertThat(registrations(target, listener))
          .as("and detaches from %s", target.getClass().getSimpleName())
          .isZero();
    }
  }

  // ------------------------------------------------------------- null and doctrine

  @Test
  void nullIsIgnoredOnBothHalves() {
    final ElwhaChip chip = sized();

    chip.addChipMouseListener(null);
    chip.removeChipMouseListener(null);
    chip.addChipMouseMotionListener(null);
    chip.removeChipMouseMotionListener(null);

    assertThat(chip.getMouseListeners())
        .as("a null registration is a defensive no-op, not an exception, and removes nothing")
        .isNotEmpty();
  }

  /**
   * The doctrine half, swept rather than enumerated: any future {@code addChipXxxListener} arrives
   * with its {@code remove} or this fails. Conventions §5 — an asymmetric pair is drift.
   */
  @Test
  void everyChipPassThroughRegistrationIsSymmetric() {
    final List<String> adds =
        Arrays.stream(ElwhaChip.class.getMethods())
            .map(Method::getName)
            .filter(n -> n.startsWith("addChip") && n.endsWith("Listener"))
            .distinct()
            .toList();
    final List<String> removes =
        Arrays.stream(ElwhaChip.class.getMethods())
            .map(Method::getName)
            .filter(n -> n.startsWith("removeChip") && n.endsWith("Listener"))
            .distinct()
            .toList();

    assertThat(adds).as("the sweep found the pass-through family at all").isNotEmpty();
    assertThat(adds)
        .as("every pass-through registration can be undone (conventions §5)")
        .allSatisfy(add -> assertThat(removes).contains("remove" + add.substring("add".length())));
  }
}
