package com.owspfm.elwha.chip;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JLabel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A coverage of the chip's two content slots. The trailing slot resolves to one of four
 * mutually-exclusive modes — none, indicator, button, affordance — with one setter each and
 * last-call-wins, so every transition between them is walked here; a mode that failed to clear its
 * predecessor would leave two things fighting over one slot.
 *
 * <p>The four affordance state rules are asserted against <em>both</em> slots from one
 * parameterized body, because conventions §7 makes that symmetry the contract: a consumer who has
 * learned one slot's affordance API should not have to learn a different shape for the other.
 * Writing it as one body means the two cannot drift apart without a failure.
 *
 * <p>The slot readbacks walk the chip's own component tree. The slot children are private fields
 * with no accessors on the leading side, and the icon a slot is <em>rendering</em> is exactly what
 * these rules are about — so the walk is the only way to assert them, and it is legitimate here
 * because the test sits in the component's own package.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaChipSlotTest {

  private static final Icon IDLE = new TestIcon();
  private static final Icon ACTIVE = new TestIcon();
  private static final Icon PLAIN = new TestIcon();

  /** Which slot a shared rule is being exercised against. */
  private enum Slot {
    LEADING,
    TRAILING;

    void installAffordance(
        final ElwhaChip chip,
        final Icon idle,
        final Icon active,
        final boolean activeState,
        final boolean hoverRevealIdle) {
      if (this == LEADING) {
        chip.setLeadingAffordance(idle, active, activeState, hoverRevealIdle, "Pin", () -> {});
      } else {
        chip.setTrailingAffordance(idle, active, activeState, hoverRevealIdle, "Pin", () -> {});
      }
    }

    JLabel button(final ElwhaChip chip) {
      return this == LEADING ? leadingButton(chip) : (JLabel) chip.getTrailingButton();
    }
  }

  private static Container contentRow(final ElwhaChip chip) {
    return (Container) chip.getComponent(0);
  }

  /** The leading cluster's clickable affordance — its first child, ahead of the icon and text. */
  private static JLabel leadingButton(final ElwhaChip chip) {
    return (JLabel) ((Container) contentRow(chip).getComponent(0)).getComponent(0);
  }

  /** The leading cluster's display-only icon label — the child after the affordance button. */
  private static JLabel leadingIcon(final ElwhaChip chip) {
    return (JLabel) ((Container) contentRow(chip).getComponent(0)).getComponent(1);
  }

  /** The trailing cluster's display-only indicator label — the child after the button. */
  private static JLabel trailingIndicator(final ElwhaChip chip) {
    return (JLabel) ((Container) contentRow(chip).getComponent(1)).getComponent(1);
  }

  private static ElwhaChip chip() {
    return new ElwhaChip("Demand");
  }

  // ------------------------------------------------ shared affordance rules

  @ParameterizedTest
  @EnumSource(Slot.class)
  void anActiveAffordanceShowsTheActiveIcon(final Slot slot) {
    final ElwhaChip chip = chip();

    slot.installAffordance(chip, IDLE, ACTIVE, true, false);

    assertThat(slot.button(chip).getIcon())
        .as(slot + ": an active affordance renders its active glyph")
        .isSameAs(ACTIVE);
    assertThat(slot.button(chip).isVisible()).as(slot + ": and claims the slot").isTrue();
  }

  @ParameterizedTest
  @EnumSource(Slot.class)
  void anActiveAffordanceFallsBackToIdleWhenItHasNoActiveIcon(final Slot slot) {
    final ElwhaChip chip = chip();

    slot.installAffordance(chip, IDLE, null, true, false);

    assertThat(slot.button(chip).getIcon())
        .as(slot + ": a one-glyph affordance stays visible when active rather than blanking")
        .isSameAs(IDLE);
  }

  @ParameterizedTest
  @EnumSource(Slot.class)
  void anIdleAffordanceShowsTheIdleIcon(final Slot slot) {
    final ElwhaChip chip = chip();

    slot.installAffordance(chip, IDLE, ACTIVE, false, false);

    assertThat(slot.button(chip).getIcon())
        .as(slot + ": an inactive affordance renders its idle glyph")
        .isSameAs(IDLE);
  }

  @ParameterizedTest
  @EnumSource(Slot.class)
  void hoverRevealHidesTheIdleGlyphUntilHovered(final Slot slot) {
    final ElwhaChip chip = chip();

    slot.installAffordance(chip, IDLE, ACTIVE, false, true);

    assertThat(slot.button(chip).getIcon())
        .as(
            slot
                + ": hover-reveal keeps an inactive glyph out of the way until the pointer arrives")
        .isNull();
  }

  @ParameterizedTest
  @EnumSource(Slot.class)
  void hoverRevealDoesNotHideAnActiveGlyph(final Slot slot) {
    final ElwhaChip chip = chip();

    slot.installAffordance(chip, IDLE, ACTIVE, true, true);

    assertThat(slot.button(chip).getIcon())
        .as(slot + ": an active affordance is state the user needs to see, hover or not")
        .isSameAs(ACTIVE);
  }

  @ParameterizedTest
  @EnumSource(Slot.class)
  void twoNullIconsClearTheAffordance(final Slot slot) {
    final ElwhaChip chip = chip();
    slot.installAffordance(chip, IDLE, ACTIVE, true, false);

    slot.installAffordance(chip, null, null, false, false);

    assertThat(slot.button(chip).isVisible())
        .as(slot + ": an affordance with no glyphs at all releases the slot")
        .isFalse();
  }

  @ParameterizedTest
  @EnumSource(Slot.class)
  void bothAffordanceSettersAreFluent(final Slot slot) {
    final ElwhaChip chip = chip();

    final ElwhaChip returned =
        slot == Slot.LEADING
            ? chip.setLeadingAffordance(IDLE, ACTIVE, false, false, "Pin", () -> {})
            : chip.setTrailingAffordance(IDLE, ACTIVE, false, false, "Pin", () -> {});

    assertThat(returned)
        .as(slot + ": the setter returns the chip for inline configuration")
        .isSameAs(chip);
  }

  // ------------------------------------------------------ enablement cascade

  @Test
  void disablingTheChipReachesBothSlotAffordances() {
    final ElwhaChip chip = chip();
    final int[] pinned = {0};
    chip.setLeadingAffordance(IDLE, ACTIVE, false, false, "Pin", () -> pinned[0]++);
    chip.setTrailingAffordance(IDLE, ACTIVE, false, false, "Clear", () -> {});
    final JLabel pin = leadingButton(chip);
    pin.setSize(24, 24);

    Input.click(pin, 12, 12);
    assertThat(pinned[0]).as("the affordance is live on an enabled chip").isEqualTo(1);

    chip.setEnabled(false);

    assertThat(pin.isEnabled())
        .as(
            "Container.setEnabled does not cascade, and the leading handler gates on its own"
                + " isEnabled() — so the chip has to hand the state down")
        .isFalse();
    assertThat(chip.getTrailingButton().isEnabled())
        .as("the trailing side was already cascaded; it stays that way")
        .isFalse();

    Input.click(pin, 12, 12);
    assertThat(pinned[0]).as("and a disabled chip's affordance swallows the click").isEqualTo(1);

    chip.setEnabled(true);
    assertThat(pin.isEnabled()).as("both come back with the chip").isTrue();
  }

  // --------------------------------------------------------- leading slot

  @Test
  void aFreshChipHasBothLeadingChildrenHidden() {
    final ElwhaChip chip = chip();

    assertThat(leadingButton(chip).isVisible()).as("no leading affordance by default").isFalse();
    assertThat(leadingIcon(chip).isVisible()).as("and no leading icon").isFalse();
  }

  @Test
  void aLeadingIconTakesTheSlotWhenNoAffordanceOwnsIt() {
    final ElwhaChip chip = chip().setLeadingIcon(PLAIN);

    assertThat(leadingIcon(chip).getIcon())
        .as("the display-only icon is installed")
        .isSameAs(PLAIN);
    assertThat(leadingIcon(chip).isVisible()).as("and shown").isTrue();
    assertThat(leadingButton(chip).isVisible())
        .as("without waking the clickable affordance")
        .isFalse();
  }

  @Test
  void aLeadingAffordanceDisplacesTheDisplayOnlyIcon() {
    final ElwhaChip chip = chip().setLeadingIcon(PLAIN);

    chip.setLeadingAffordance(IDLE, ACTIVE, false, false, "Pin", () -> {});

    assertThat(leadingButton(chip).isVisible()).as("the affordance claims the slot").isTrue();
    assertThat(leadingIcon(chip).isVisible())
        .as("and the display-only icon steps aside rather than doubling up")
        .isFalse();
  }

  @Test
  void aLeadingIconDoesNotStealTheSlotBackFromAnAffordance() {
    final ElwhaChip chip = chip();
    chip.setLeadingAffordance(IDLE, ACTIVE, false, false, "Pin", () -> {});

    chip.setLeadingIcon(PLAIN);

    assertThat(leadingIcon(chip).isVisible())
        .as("the affordance keeps its claim — the icon is stored but stays hidden")
        .isFalse();
    assertThat(leadingIcon(chip).getIcon()).as("though the icon is remembered").isSameAs(PLAIN);
    assertThat(leadingButton(chip).isVisible()).as("and the affordance still shows").isTrue();
  }

  @Test
  void clearingTheAffordanceHandsTheSlotBackToAWaitingIcon() {
    final ElwhaChip chip = chip();
    chip.setLeadingAffordance(IDLE, ACTIVE, false, false, "Pin", () -> {});
    chip.setLeadingIcon(PLAIN);

    chip.setLeadingAffordance(null, null, false, false, null, null);

    assertThat(leadingIcon(chip).isVisible())
        .as("the icon that was waiting behind the affordance takes the slot back")
        .isTrue();
    assertThat(leadingButton(chip).isVisible()).as("and the affordance is gone").isFalse();
  }

  @Test
  void aNullLeadingIconClearsTheSlot() {
    final ElwhaChip chip = chip().setLeadingIcon(PLAIN).setLeadingIcon(null);

    assertThat(leadingIcon(chip).isVisible()).as("null empties the leading slot").isFalse();
  }

  // -------------------------------------------- trailing four-mode vocabulary

  @Test
  void trailingSlotStartsEmpty() {
    final ElwhaChip chip = chip();

    assertThat(chip.getTrailingButton().isVisible()).as("no trailing button by default").isFalse();
    assertThat(trailingIndicator(chip).isVisible()).as("and no indicator").isFalse();
  }

  @Test
  void trailingButtonAccessorAlwaysHandsBackTheSameComponent() {
    final ElwhaChip chip = chip();

    assertThat(chip.getTrailingButton())
        .as("the button always exists so callers can bind to it before it is populated")
        .isSameAs(chip.getTrailingButton());
  }

  @Test
  void indicatorModeShowsADisplayOnlyGlyph() {
    final ElwhaChip chip = chip().setTrailingIndicator(PLAIN);

    assertThat(trailingIndicator(chip).getIcon())
        .as("the indicator glyph is installed")
        .isSameAs(PLAIN);
    assertThat(trailingIndicator(chip).isVisible()).as("and shown").isTrue();
    assertThat(chip.getTrailingButton().isVisible())
        .as("with no interactive button behind it — the chip body owns the click")
        .isFalse();
  }

  @Test
  void buttonModeShowsTheActionsIcon() {
    final Action action =
        new AbstractAction() {
          @Override
          public void actionPerformed(final ActionEvent event) {
            // Behaviour is exercised elsewhere; this test is about slot occupancy.
          }
        };
    action.putValue(Action.SMALL_ICON, PLAIN);
    action.putValue(Action.SHORT_DESCRIPTION, "Remove");

    final ElwhaChip chip = chip().setTrailingAction(action);

    assertThat(chip.getTrailingButton().isVisible()).as("the button claims the slot").isTrue();
    assertThat(((JLabel) chip.getTrailingButton()).getIcon())
        .as("rendering the action's small icon")
        .isSameAs(PLAIN);
    assertThat(chip.getTrailingButton().getToolTipText())
        .as("and its short description as the tooltip")
        .isEqualTo("Remove");
  }

  @Test
  void convenienceOverloadBuildsTheSameButtonMode() {
    final ElwhaChip chip = chip().setTrailingIcon(PLAIN, "Remove", () -> {});

    assertThat(((JLabel) chip.getTrailingButton()).getIcon())
        .as("the three-arg overload is sugar over an Action")
        .isSameAs(PLAIN);
    assertThat(chip.getTrailingButton().getToolTipText())
        .as("tooltip included")
        .isEqualTo("Remove");
  }

  @Test
  void affordanceModeCarriesItsTooltip() {
    final ElwhaChip chip = chip();

    chip.setTrailingAffordance(IDLE, ACTIVE, false, false, "Pin", () -> {});

    assertThat(chip.getTrailingButton().getToolTipText())
        .as("the affordance's tooltip reaches the button the same way an action's does")
        .isEqualTo("Pin");
  }

  // ------------------------------------------------------- last call wins

  @Test
  void aButtonDisplacesAnIndicator() {
    final ElwhaChip chip = chip().setTrailingIndicator(PLAIN);

    chip.setTrailingIcon(IDLE, "Remove", () -> {});

    assertThat(chip.getTrailingButton().isVisible()).as("the button takes the slot").isTrue();
    assertThat(trailingIndicator(chip).isVisible())
        .as("and the indicator is cleared — the slot holds exactly one thing")
        .isFalse();
  }

  @Test
  void anIndicatorDisplacesAButton() {
    final ElwhaChip chip = chip().setTrailingIcon(IDLE, "Remove", () -> {});

    chip.setTrailingIndicator(PLAIN);

    assertThat(trailingIndicator(chip).isVisible()).as("the indicator takes the slot").isTrue();
    assertThat(chip.getTrailingButton().isVisible()).as("and the button steps down").isFalse();
  }

  @Test
  void anAffordanceDisplacesAnIndicator() {
    final ElwhaChip chip = chip().setTrailingIndicator(PLAIN);

    chip.setTrailingAffordance(IDLE, ACTIVE, false, false, "Pin", () -> {});

    assertThat(chip.getTrailingButton().isVisible()).as("the affordance takes the slot").isTrue();
    assertThat(trailingIndicator(chip).isVisible()).as("and the indicator is cleared").isFalse();
  }

  @Test
  void anIndicatorDisplacesAnAffordance() {
    final ElwhaChip chip = chip();
    chip.setTrailingAffordance(IDLE, ACTIVE, true, false, "Pin", () -> {});

    chip.setTrailingIndicator(PLAIN);

    assertThat(trailingIndicator(chip).isVisible()).as("the indicator takes the slot").isTrue();
    assertThat(chip.getTrailingButton().isVisible()).as("and the affordance steps down").isFalse();
  }

  @Test
  void aButtonDisplacesAnAffordance() {
    final ElwhaChip chip = chip();
    chip.setTrailingAffordance(IDLE, ACTIVE, true, false, "Pin", () -> {});

    chip.setTrailingIcon(PLAIN, "Remove", () -> {});

    assertThat(((JLabel) chip.getTrailingButton()).getIcon())
        .as("the single-state button replaces the two-state affordance's glyph outright")
        .isSameAs(PLAIN);
    assertThat(chip.getTrailingButton().getToolTipText())
        .as("and its tooltip too")
        .isEqualTo("Remove");
  }

  @Test
  void anAffordanceDisplacesAButton() {
    final ElwhaChip chip = chip().setTrailingIcon(PLAIN, "Remove", () -> {});

    chip.setTrailingAffordance(IDLE, ACTIVE, false, false, "Pin", () -> {});

    assertThat(((JLabel) chip.getTrailingButton()).getIcon())
        .as("the affordance's own glyph replaces the action's")
        .isSameAs(IDLE);
    assertThat(chip.getTrailingButton().getToolTipText())
        .as("along with its tooltip")
        .isEqualTo("Pin");
  }

  // ----------------------------------------------------------- clearing

  @Test
  void aNullActionEmptiesTheSlot() {
    final ElwhaChip chip = chip().setTrailingIcon(PLAIN, "Remove", () -> {});

    chip.setTrailingAction(null);

    assertThat(chip.getTrailingButton().isVisible()).as("a null action empties the slot").isFalse();
    assertThat(((JLabel) chip.getTrailingButton()).getIcon()).as("and drops the glyph").isNull();
    assertThat(chip.getTrailingButton().getToolTipText()).as("and the tooltip").isNull();
  }

  @Test
  void aNullConvenienceIconEmptiesTheSlot() {
    final ElwhaChip chip = chip().setTrailingIcon(PLAIN, "Remove", () -> {});

    chip.setTrailingIcon(null, "Remove", () -> {});

    assertThat(chip.getTrailingButton().isVisible())
        .as("the three-arg overload routes a null icon to the same clear")
        .isFalse();
  }

  @Test
  void aNullIndicatorEmptiesTheSlot() {
    final ElwhaChip chip = chip().setTrailingIndicator(PLAIN);

    chip.setTrailingIndicator(null);

    assertThat(trailingIndicator(chip).isVisible()).as("null clears the indicator").isFalse();
  }

  @Test
  void everyTrailingSetterIsFluent() {
    final ElwhaChip chip = chip();

    assertThat(chip.setTrailingIndicator(PLAIN))
        .as("setTrailingIndicator is fluent")
        .isSameAs(chip);
    assertThat(chip.setTrailingAction(null)).as("setTrailingAction is fluent").isSameAs(chip);
    assertThat(chip.setTrailingIcon(PLAIN, null, null))
        .as("setTrailingIcon is fluent")
        .isSameAs(chip);
    assertThat(chip.setLeadingIcon(PLAIN)).as("setLeadingIcon is fluent").isSameAs(chip);
  }

  /** An identity-comparable stand-in — these tests care which icon landed, never how it looks. */
  private static final class TestIcon implements Icon {

    @Override
    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
      // Nothing to draw — identity is the whole point.
    }

    @Override
    public int getIconWidth() {
      return 14;
    }

    @Override
    public int getIconHeight() {
      return 14;
    }
  }
}
