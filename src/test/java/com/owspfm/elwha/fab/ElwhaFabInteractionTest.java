package com.owspfm.elwha.fab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A event-contract coverage for {@link ElwhaFab} — the §3 content rules the factories enforce,
 * the morph prerequisites, activation through the real mouse pipeline and the {@code WHEN_FOCUSED}
 * bindings, and the disabled guards of the #432 class.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaFabInteractionTest {

  private static final String ACTIVATE = "elwhafab.activate";

  private static ElwhaFab sized(final ElwhaFab fab) {
    final Dimension pref = fab.getPreferredSize();
    fab.setSize(pref.width, pref.height);
    return fab;
  }

  private static int centerX(final ElwhaFab fab) {
    return fab.getWidth() / 2;
  }

  private static int centerY(final ElwhaFab fab) {
    return fab.getHeight() / 2;
  }

  // ----------------------------------------------------------- content rules

  @Test
  void aStandardFabRequiresAnIcon() {
    assertThatThrownBy(() -> ElwhaFab.standard(null))
        .as("§3 — the Standard form is icon-only, so the icon is not optional")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void anExtendedFabRequiresItsLabel() {
    assertThatThrownBy(() -> ElwhaFab.extended(null))
        .as("§3 — the Extended form is text-required")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> ElwhaFab.extended(MaterialIcons.add(), null))
        .as("even when an icon is supplied")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void iconBearingExtendedFactoryRejectsANullIcon() {
    assertThatThrownBy(() -> ElwhaFab.extended(null, "Compose"))
        .as("an icon-less Extended FAB has its own factory rather than a null-icon overload")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void eachFactoryCarriesExactlyTheContentItsFormAllows() {
    assertThat(ElwhaFab.standard(MaterialIcons.add()).getText())
        .as("a Standard FAB has no label at all")
        .isNull();
    assertThat(ElwhaFab.standard(MaterialIcons.add()).getIcon())
        .as("but always an icon")
        .isNotNull();
    assertThat(ElwhaFab.extended("Compose").getIcon())
        .as("a label-only Extended FAB has no icon")
        .isNull();
    assertThat(ElwhaFab.extended("Compose").getText())
        .as("but carries its label")
        .isEqualTo("Compose");
    assertThat(ElwhaFab.extended(MaterialIcons.add(), "Compose").getIcon())
        .as("and the icon-bearing factory carries both")
        .isNotNull();
  }

  // --------------------------------------------------- morph prerequisites

  @Test
  void aStandardFabCannotMorphToAFormItHasNoLabelFor() {
    assertThatThrownBy(() -> ElwhaFab.standard(MaterialIcons.add()).morphTo(ElwhaFab.Form.EXTENDED))
        .as("only extended(Icon, String) carries the content for both endpoints")
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("requires a non-null label");
  }

  @Test
  void aLabelOnlyFabCannotMorphToAFormItHasNoIconFor() {
    assertThatThrownBy(() -> ElwhaFab.extended("Compose").morphTo(ElwhaFab.Form.STANDARD))
        .as("the Standard endpoint needs a glyph to draw")
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("requires a non-null icon");
  }

  @Test
  void aNullMorphTargetIsRejected() {
    assertThatThrownBy(() -> ElwhaFab.extended(MaterialIcons.add(), "Compose").morphTo(null))
        .as("there is no null form")
        .isInstanceOf(NullPointerException.class);
  }

  // ------------------------------------------------------------- activation

  @Test
  void aClickFiresEachActionListenerExactlyOnce() {
    final ElwhaFab fab = sized(ElwhaFab.standard(MaterialIcons.add()));
    final List<ActionEvent> heard = new ArrayList<>();
    fab.addActionListener(heard::add);

    Input.click(fab, centerX(fab), centerY(fab));

    assertThat(heard).as("a press-release pair on the body activates it once").hasSize(1);
    assertThat(heard.get(0).getActionCommand())
        .as("the action command identifies the activation")
        .isEqualTo("click");
    assertThat(heard.get(0).getSource()).as("the FAB is the event source").isSameAs(fab);
  }

  @Test
  void shadowReserveIsNotAClickTarget() {
    final ElwhaFab fab = sized(ElwhaFab.standard(MaterialIcons.add()));
    final Insets reserve = fab.getShadowInsets();
    final int[] fired = {0};
    fab.addActionListener(e -> fired[0]++);

    Input.click(fab, reserve.left / 2, reserve.top / 2);

    assertThat(reserve.left).as("the fixture depends on a non-empty reserve").isPositive();
    assertThat(fired[0])
        .as("the visible surface is what counts — the halo padding around it is not clickable")
        .isZero();
  }

  @Test
  void releasingOutsideTheBodyCancelsTheClick() {
    final ElwhaFab fab = sized(ElwhaFab.standard(MaterialIcons.add()));
    final int[] fired = {0};
    fab.addActionListener(e -> fired[0]++);

    Input.press(fab, centerX(fab), centerY(fab));
    Input.release(fab, 900, 900);

    assertThat(fired[0]).as("a drag-off release is a cancelled click, not an activation").isZero();
  }

  @Test
  void spaceAndEnterAreBoundWhenFocusedAndBothActivate() {
    final ElwhaFab fab = sized(ElwhaFab.standard(MaterialIcons.add()));
    final int[] fired = {0};
    fab.addActionListener(e -> fired[0]++);

    Input.pressBoundKey(fab, "pressed SPACE", ACTIVATE);
    Input.pressBoundKey(fab, "pressed ENTER", ACTIVATE);

    assertThat(fired[0]).as("§10.3 — both bound keys activate the FAB").isEqualTo(2);
  }

  @Test
  void removingAListenerStopsItsDelivery() {
    final ElwhaFab fab = sized(ElwhaFab.standard(MaterialIcons.add()));
    final int[] fired = {0};
    final java.awt.event.ActionListener listener = e -> fired[0]++;
    fab.addActionListener(listener);
    fab.addActionListener(null);

    fab.removeActionListener(listener);
    Input.click(fab, centerX(fab), centerY(fab));

    assertThat(fired[0])
        .as("a removed listener hears nothing, and a null one was never added")
        .isZero();
  }

  // -------------------------------------------------------- disabled guards

  @Test
  void aDisabledFabFiresNothingThroughAnyPath() {
    final ElwhaFab fab = sized(ElwhaFab.standard(MaterialIcons.add()));
    fab.setEnabled(false);
    final int[] fired = {0};
    fab.addActionListener(e -> fired[0]++);

    Input.click(fab, centerX(fab), centerY(fab));
    Input.pressBoundKey(fab, "pressed SPACE", ACTIVATE);
    Input.pressBoundKey(fab, "pressed ENTER", ACTIVATE);

    assertThat(fired[0]).as("mouse and both key bindings are guarded while disabled").isZero();
  }

  @Test
  void aFabIsATabStopAtEverySize() {
    for (final ElwhaFab.Size size : ElwhaFab.Size.values()) {
      assertThat(ElwhaFab.standard(MaterialIcons.add()).setFabSize(size).isFocusable())
          .as("%s is keyboard reachable", size)
          .isTrue();
    }
  }
}
