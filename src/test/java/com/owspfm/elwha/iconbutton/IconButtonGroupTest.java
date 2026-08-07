package com.owspfm.elwha.iconbutton;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of {@link IconButtonGroup} — the mutual-exclusion helper that stands in for
 * Swing's {@code ButtonGroup}, which cannot accept an {@link ElwhaIconButton} because the primitive
 * extends {@code JComponent} rather than {@code AbstractButton}.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class IconButtonGroupTest {

  private static ElwhaIconButton toggle() {
    return new ElwhaIconButton(MaterialIcons.gridView())
        .setInteractionMode(IconButtonInteractionMode.SELECTABLE);
  }

  @Test
  void aFreshGroupIsEmptyAndPermissive() {
    final IconButtonGroup group = new IconButtonGroup();

    assertThat(group.size()).as("a fresh group holds no buttons").isZero();
    assertThat(group.isMandatory())
        .as("and matches Swing's ButtonGroup default of allowing an empty selection")
        .isFalse();
    assertThat(group.getSelected()).as("with nothing selected").isEmpty();
  }

  @Test
  void onlySelectableButtonsMayJoin() {
    assertThatThrownBy(() -> new IconButtonGroup().add(new ElwhaIconButton()))
        .as("a push button cannot participate in a mutually-exclusive selection")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("SELECTABLE");
    assertThatThrownBy(() -> new IconButtonGroup().add(null))
        .as("and null is rejected outright")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void addingTheSameButtonTwiceRegistersItOnce() {
    final ElwhaIconButton button = toggle();
    final IconButtonGroup group = new IconButtonGroup().add(button).add(button);

    assertThat(group.size()).as("the group de-duplicates its members").isEqualTo(1);
  }

  @Test
  void addReturnsTheGroupForChaining() {
    final IconButtonGroup group = new IconButtonGroup();

    assertThat(group.add(toggle())).as("add chains").isSameAs(group);
    assertThat(group.remove(toggle())).as("and so does remove").isSameAs(group);
  }

  @Test
  void selectingOneMemberDeselectsTheRest() {
    final ElwhaIconButton first = toggle();
    final ElwhaIconButton second = toggle();
    final ElwhaIconButton third = toggle();
    final IconButtonGroup group = new IconButtonGroup().add(first).add(second).add(third);

    first.setSelected(true);
    second.setSelected(true);

    assertThat(first.isSelected()).as("the previous selection gives way").isFalse();
    assertThat(second.isSelected()).as("the newly selected button holds it").isTrue();
    assertThat(third.isSelected()).as("uninvolved members are untouched").isFalse();
    assertThat(group.getSelected()).as("and the group reports the live selection").contains(second);
  }

  @Test
  void aPermissiveGroupLetsTheUserClearTheSelection() {
    final ElwhaIconButton button = toggle();
    final IconButtonGroup group = new IconButtonGroup().add(button);
    button.setSelected(true);

    button.setSelected(false);

    assertThat(button.isSelected()).as("the deselect stands").isFalse();
    assertThat(group.getSelected()).as("leaving the group with no selection").isEmpty();
  }

  @Test
  void aMandatoryGroupRefusesToEmptyItself() {
    final ElwhaIconButton first = toggle();
    final ElwhaIconButton second = toggle();
    final IconButtonGroup group = new IconButtonGroup(true).add(first).add(second);
    first.setSelected(true);

    first.setSelected(false);

    assertThat(group.isMandatory()).as("the group is mandatory").isTrue();
    assertThat(first.isSelected())
        .as("clearing the only selection is refused — exactly one stays selected")
        .isTrue();
  }

  @Test
  void aMandatoryGroupStillAllowsMovingTheSelection() {
    final ElwhaIconButton first = toggle();
    final ElwhaIconButton second = toggle();
    new IconButtonGroup(true).add(first).add(second);
    first.setSelected(true);

    second.setSelected(true);

    assertThat(first.isSelected()).as("the old selection releases").isFalse();
    assertThat(second.isSelected()).as("and the new one takes over").isTrue();
  }

  @Test
  void aRemovedButtonResumesIndependentToggleBehavior() {
    final ElwhaIconButton first = toggle();
    final ElwhaIconButton second = toggle();
    final IconButtonGroup group = new IconButtonGroup().add(first).add(second);
    first.setSelected(true);

    group.remove(first);
    second.setSelected(true);

    assertThat(group.size()).as("the group shrank").isEqualTo(1);
    assertThat(first.isSelected())
        .as("a removed button is no longer driven by the group's mutex")
        .isTrue();
  }

  @Test
  void removingAButtonThatNeverJoinedIsHarmless() {
    final IconButtonGroup group = new IconButtonGroup().add(toggle());

    group.remove(toggle()).remove(null);

    assertThat(group.size()).as("removing a stranger leaves the membership alone").isEqualTo(1);
  }
}
