package com.owspfm.elwha.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the {@link ElwhaMenuItem} primitive — the slot anatomy and how each slot moves
 * the row's measured size, the display-only content slot, the container-pushed state (selection,
 * roving focus, reserved check column), and the click contract including the disabled row's
 * inertness. Sizes are asserted as <em>deltas</em> against the row's own {@link FontMetrics}, never
 * as absolute pixel counts, so the assertions state the token relationship rather than a font.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaMenuItemTest {

  private static final String LABEL = "Duplicate";

  private static FontMetrics labelMetrics(final ElwhaMenuItem item) {
    return item.getFontMetrics(TypeRole.LABEL_LARGE.resolve());
  }

  // ------------------------------------------------------------ construction

  @Test
  void anItemRequiresALabel() {
    assertThatNullPointerException().isThrownBy(() -> ElwhaMenuItem.of(null));
    assertThatNullPointerException().isThrownBy(() -> new ElwhaMenuItem(LABEL).setLabel(null));
  }

  @Test
  void factoriesAndConstructorsAgree() {
    assertThat(ElwhaMenuItem.of(LABEL).getLabel()).isEqualTo(LABEL);
    assertThat(ElwhaMenuItem.of(MaterialIcons.edit(20), LABEL).getLeadingIcon())
        .as("the icon factory keeps the icon it was handed")
        .isNotNull();
  }

  @Test
  void anItemIsNotIndependentlyFocusable() {
    assertThat(ElwhaMenuItem.of(LABEL).isFocusable())
        .as("the parent menu owns the single keyboard focus and rovers a flag down to its rows")
        .isFalse();
  }

  // ------------------------------------------------------------------ slots

  @Test
  void everyOptionalSlotRoundTrips() {
    final ElwhaMenuItem item = ElwhaMenuItem.of(LABEL);
    final ElwhaBadge badge = ElwhaBadge.small();
    final JLabel slot = new JLabel("swatch");

    item.setSupportingText("Copies the current row");
    item.setTrailingText("⌘D");
    item.setTrailingIcon(MaterialIcons.check(20));
    item.setBadge(badge);
    item.setSlot(slot);

    assertThat(item.getSupportingText()).isEqualTo("Copies the current row");
    assertThat(item.getTrailingText()).isEqualTo("⌘D");
    assertThat(item.getTrailingIcon()).isNotNull();
    assertThat(item.getBadge()).isSameAs(badge);
    assertThat(item.getSlot()).isSameAs(slot);
  }

  @Test
  void aSlotJoinsTheContainmentHierarchyAndIsReplaceable() {
    final ElwhaMenuItem item = ElwhaMenuItem.of(LABEL);
    final JLabel first = new JLabel("one");

    item.setSlot(first);
    assertThat(item.getComponents()).as("a slot is a real child").contains(first);

    final JLabel second = new JLabel("two");
    item.setSlot(second);
    assertThat(item.getComponents())
        .as("and replacing it detaches the previous one rather than stacking")
        .contains(second)
        .doesNotContain(first);
  }

  @Test
  void clearingTheSlotRestoresThePaintedLabel() {
    final ElwhaMenuItem item = ElwhaMenuItem.of(LABEL);
    item.setSlot(new JLabel("swatch"));

    item.setSlot(null);

    assertThat(item.getSlot()).isNull();
    assertThat(item.getComponents()).as("and the slot child is gone").isEmpty();
  }

  // ----------------------------------------------------------------- sizing

  @Test
  void aBareRowIsTheLabelInsideTheHorizontalInsets() {
    final ElwhaMenuItem item = ElwhaMenuItem.of(LABEL);

    assertThat(item.getPreferredSize().width)
        .as("a row with no icon is just the label between the leading and trailing insets")
        .isEqualTo(2 * ElwhaMenuItem.INSET_X_PX + labelMetrics(item).stringWidth(LABEL));
  }

  @Test
  void aRowIsNeverShorterThanTheMinimumTouchTarget() {
    assertThat(ElwhaMenuItem.of(LABEL).getPreferredSize().height)
        .as("the interactive target stays at least the M3 minimum around the visual row")
        .isGreaterThanOrEqualTo(ElwhaMenuItem.MIN_TARGET_PX);
  }

  @Test
  void aLeadingIconWidensTheRowByTheIconColumn() {
    final int bare = ElwhaMenuItem.of(LABEL).getPreferredSize().width;

    final int withIcon = ElwhaMenuItem.of(MaterialIcons.edit(20), LABEL).getPreferredSize().width;

    assertThat(withIcon - bare)
        .as("the leading column costs one icon plus the between-space")
        .isEqualTo(ElwhaMenuItem.ICON_SIZE_PX + ElwhaMenuItem.BETWEEN_PX);
  }

  @Test
  void aReservedCheckColumnCostsTheSameAsALeadingIcon() {
    final ElwhaMenuItem item = ElwhaMenuItem.of(LABEL);
    final int bare = item.getPreferredSize().width;

    item.setReserveLeadingColumn(true);

    assertThat(item.getPreferredSize().width - bare)
        .as("reserving the check column up front is what keeps a toggle from shifting the label")
        .isEqualTo(ElwhaMenuItem.ICON_SIZE_PX + ElwhaMenuItem.BETWEEN_PX);
  }

  @Test
  void selectingARowWithAReservedColumnDoesNotResizeIt() {
    final ElwhaMenuItem item = ElwhaMenuItem.of(LABEL);
    item.setReserveLeadingColumn(true);
    final Dimension before = item.getPreferredSize();

    item.setSelected(true);

    assertThat(item.getPreferredSize())
        .as("the checkmark drops into a column that was already there")
        .isEqualTo(before);
  }

  @Test
  void trailingTextWidensTheRowByItsOwnMeasure() {
    final ElwhaMenuItem item = ElwhaMenuItem.of(LABEL);
    final int bare = item.getPreferredSize().width;

    item.setTrailingText("⌘D");

    assertThat(item.getPreferredSize().width - bare)
        .as("a keyboard shortcut costs its measured width plus the between-space")
        .isEqualTo(ElwhaMenuItem.BETWEEN_PX + labelMetrics(item).stringWidth("⌘D"));
  }

  @Test
  void aTrailingIconWidensTheRowByTheIconColumn() {
    final ElwhaMenuItem item = ElwhaMenuItem.of(LABEL);
    final int bare = item.getPreferredSize().width;

    item.setTrailingIcon(MaterialIcons.check(20));

    assertThat(item.getPreferredSize().width - bare)
        .isEqualTo(ElwhaMenuItem.ICON_SIZE_PX + ElwhaMenuItem.BETWEEN_PX);
  }

  @Test
  void supportingTextAddsASecondLineToTheRowHeight() {
    final ElwhaMenuItem item = ElwhaMenuItem.of(LABEL);
    final FontMetrics supportingFm = item.getFontMetrics(TypeRole.BODY_SMALL.resolve());
    final int twoLineHeight =
        2 * ElwhaMenuItem.INSET_Y_PX + labelMetrics(item).getHeight() + supportingFm.getHeight();

    item.setSupportingText("Copies the current row");

    assertThat(item.getPreferredSize().height)
        .as("a two-line row grows past the touch-target floor by the supporting line")
        .isEqualTo(Math.max(ElwhaMenuItem.MIN_TARGET_PX, twoLineHeight));
  }

  @Test
  void aWideSlotDrivesTheRowWidthInsteadOfTheLabel() {
    final ElwhaMenuItem item = ElwhaMenuItem.of(LABEL);
    final JLabel slot = new JLabel();
    slot.setPreferredSize(new Dimension(240, 20));

    item.setSlot(slot);

    assertThat(item.getPreferredSize().width)
        .as("the slot replaces the label region, so it measures the row")
        .isEqualTo(2 * ElwhaMenuItem.INSET_X_PX + 240);
  }

  @Test
  void aRowStretchesHorizontallyButNeverVertically() {
    final ElwhaMenuItem item = ElwhaMenuItem.of(LABEL);

    assertThat(item.getMaximumSize().width)
        .as("rows fill the menu's content width")
        .isEqualTo(Integer.MAX_VALUE);
    assertThat(item.getMaximumSize().height)
        .as("but never stretch taller than their own row height")
        .isEqualTo(item.getPreferredSize().height);
    assertThat(item.getMinimumSize())
        .as("and cannot be squeezed below their preferred size")
        .isEqualTo(item.getPreferredSize());
  }

  // ------------------------------------------------------------ interaction

  @Test
  void aClickFiresTheActionListenersOnce() {
    final List<Object> sources = new ArrayList<>();
    final ElwhaMenuItem item = ElwhaMenuItem.of(LABEL);
    item.addActionListener(e -> sources.add(e.getSource()));
    item.setSize(item.getPreferredSize());

    Input.click(item, 20, item.getHeight() / 2);

    assertThat(sources)
        .as("a click on the row fires exactly one action, sourced at the row")
        .containsExactly(item);
  }

  @Test
  void aReleaseOutsideTheRowDoesNotFire() {
    final List<Object> fired = new ArrayList<>();
    final ElwhaMenuItem item = ElwhaMenuItem.of(LABEL);
    item.addActionListener(e -> fired.add(e));
    item.setSize(item.getPreferredSize());

    Input.press(item, 20, item.getHeight() / 2);
    Input.release(item, 20, item.getHeight() + 40);

    assertThat(fired).as("dragging off the row before release cancels the activation").isEmpty();
  }

  @Test
  void aDisabledRowSwallowsBothClickAndKeyboardActivation() {
    final List<Object> fired = new ArrayList<>();
    final ElwhaMenuItem item = ElwhaMenuItem.of(LABEL);
    item.addActionListener(e -> fired.add(e));
    item.setEnabled(false);
    item.setSize(item.getPreferredSize());

    Input.click(item, 20, item.getHeight() / 2);
    item.activate(0);

    assertThat(fired).as("a disabled row is inert through every activation path").isEmpty();
  }

  @Test
  void aRemovedListenerStopsFiring() {
    final List<Object> fired = new ArrayList<>();
    final java.awt.event.ActionListener listener = e -> fired.add(e);
    final ElwhaMenuItem item = ElwhaMenuItem.of(LABEL);
    item.addActionListener(listener);
    item.addActionListener(null);

    item.removeActionListener(listener);
    item.activate(0);

    assertThat(fired)
        .as("a removed listener is gone, and a null one was never registered")
        .isEmpty();
  }

  // ------------------------------------------------------------- submenu item

  @Test
  void aSubMenuItemRequiresBothALabelAndANestedMenu() {
    final ElwhaMenu nested = ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Nested")).build();
    assertThatNullPointerException().isThrownBy(() -> ElwhaSubMenuItem.of(null, nested));
    assertThatNullPointerException()
        .isThrownBy(() -> ElwhaSubMenuItem.of("Share", (ElwhaMenu) null));
  }

  @Test
  void aSubMenuItemReservesItsTrailingSlotForTheCaret() {
    final ElwhaMenu nested = ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Nested")).build();
    final ElwhaSubMenuItem item = ElwhaSubMenuItem.of("Share", nested);

    assertThat(item.getTrailingIcon())
        .as("the submenu signifier is placed automatically")
        .isNotNull();
    assertThatThrownBy(() -> item.setTrailingIcon(MaterialIcons.check(20)))
        .as("and the slot is not available to consumers")
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("caret");
  }

  @Test
  void aSubMenuItemStartsCollapsedAndItsNestedMenuIsReplaceable() {
    final ElwhaMenu first = ElwhaMenu.builder().addItem(ElwhaMenuItem.of("One")).build();
    final ElwhaMenu second = ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Two")).build();
    final ElwhaSubMenuItem item = ElwhaSubMenuItem.of("Share", first);

    assertThat(item.isExpanded()).as("no submenu is open until the trigger is used").isFalse();
    assertThat(item.getSubMenu()).isSameAs(first);

    item.setSubMenu(second);
    assertThat(item.getSubMenu()).as("the nested menu can be swapped").isSameAs(second);
    assertThatNullPointerException().isThrownBy(() -> item.setSubMenu(null));
  }

  @Test
  void aSubMenuItemIsAMenuItemSoItGoesInThroughTheOrdinaryBuilderSlot() {
    final ElwhaMenu nested = ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Nested")).build();
    final ElwhaSubMenuItem sub = ElwhaSubMenuItem.of("Share", nested);

    final ElwhaMenu parent =
        ElwhaMenu.builder().addItem(ElwhaMenuItem.of("Cut")).addItem(sub).build();

    assertThat(parent.getItems()).as("no separate builder slot exists, by design").contains(sub);
  }
}
