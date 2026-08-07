package com.owspfm.elwha.badge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tier A coverage of the badge's content rules and size axis — design doc §3 (the four-character
 * cap and its two overflow forms), §4 (Small is a fixed 6 dp box; Large is a fixed 16 dp tall pill
 * whose width follows its label), and the {@code PROPERTY_CONTENT} notification the anchor listens
 * to in order to re-place a widening Large badge.
 *
 * <p>Widths are asserted against the {@link TypeRole#LABEL_SMALL} metrics formula rather than
 * pinned pixel counts: the contract is "text width plus both padding columns, floored at the pill
 * height", and hardcoding a number would be an assertion about the bundled font's advance table.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaBadgeContentTest {

  private static final int SMALL_SIZE = 6;
  private static final int LARGE_HEIGHT = 16;
  private static final int LARGE_PADDING = 4;

  private static int labelWidth(final ElwhaBadge badge, final String content) {
    final FontMetrics fm = badge.getFontMetrics(TypeRole.LABEL_SMALL.resolve());
    return fm.stringWidth(content);
  }

  // ------------------------------------------------------------ content cap

  @ParameterizedTest
  @CsvSource({
    // numeric, at or under the cap — verbatim
    "0, 0",
    "7, 7",
    "42, 42",
    "999, 999",
    // numeric, over the cap — the M3 sentinel, regardless of how far over
    "1000, 999+",
    "1234, 999+",
    "999999, 999+",
    // non-numeric over three characters — first three plus the overflow marker
    "BETA, BET+",
    "Message, Mes+",
    // mixed content is not numeric, so it takes the truncate-and-mark path
    "12a4, 12a+",
    // at or under three characters — verbatim, numeric or not
    "NEW, NEW",
    "hi, hi",
    "+, +"
  })
  void contentIsCoercedToTheFourCharacterCap(final String given, final String stored) {
    assertThat(ElwhaBadge.large(given).getContent())
        .as("§3 — '%s' displays as '%s'", given, stored)
        .isEqualTo(stored);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 999})
  void aCountAtOrUnderTheNumericCapRendersVerbatim(final int count) {
    assertThat(ElwhaBadge.large(count).getContent())
        .as("a count within the three-digit cap displays as itself")
        .isEqualTo(Integer.toString(count));
  }

  @ParameterizedTest
  @ValueSource(ints = {1000, 1001, Integer.MAX_VALUE})
  void aCountOverTheNumericCapCollapsesToTheSentinel(final int count) {
    assertThat(ElwhaBadge.large(count).getContent())
        .as("§3 — every count past 999 reads as the same M3 overflow sentinel")
        .isEqualTo("999+");
  }

  @Test
  void storedContentNeverExceedsFourCharacters() {
    assertThat(ElwhaBadge.large("a very long status string").getContent())
        .as("the cap is on the stored value, not just the painted one")
        .hasSizeLessThanOrEqualTo(4);
  }

  // --------------------------------------------------------- content guards

  @Test
  void largeRejectsNullContent() {
    assertThatExceptionOfType(NullPointerException.class)
        .as("a Large badge with no label is a Small badge — the factory says so")
        .isThrownBy(() -> ElwhaBadge.large((String) null));
  }

  @Test
  void largeRejectsEmptyContent() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> ElwhaBadge.large(""))
        .withMessageContaining("small()");
  }

  @Test
  void largeRejectsANegativeCount() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .as("there is no such thing as a negative unread count")
        .isThrownBy(() -> ElwhaBadge.large(-1));
  }

  @Test
  void aSmallBadgeHasNoContentToSet() {
    assertThatExceptionOfType(IllegalStateException.class)
        .as("§2 — Small carries no label sub-part, so setContent is a category error")
        .isThrownBy(() -> ElwhaBadge.small().setContent("3"));
  }

  @Test
  void aSmallBadgeReportsNullContent() {
    assertThat(ElwhaBadge.small().getContent())
        .as("Small is a presence dot — there is nothing to read back")
        .isNull();
  }

  @Test
  void setContentAppliesTheSameCoercionAsTheFactory() {
    final ElwhaBadge badge = ElwhaBadge.large("1");

    badge.setContent(4321);

    assertThat(badge.getContent())
        .as("the overflow rule lives with the content, not with the factory")
        .isEqualTo("999+");
  }

  @Test
  void setContentRejectsEmptyContentToo() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> ElwhaBadge.large("1").setContent(""));
  }

  @Test
  void setContentReturnsTheBadgeForChaining() {
    final ElwhaBadge badge = ElwhaBadge.large("1");

    assertThat(badge.setContent("2"))
        .as("the mutators are fluent per the component API convention")
        .isSameAs(badge);
  }

  // --------------------------------------------------------------- variants

  @Test
  void factoriesPinTheVariant() {
    assertThat(ElwhaBadge.small().getVariant()).isEqualTo(ElwhaBadge.Variant.SMALL);
    assertThat(ElwhaBadge.large("1").getVariant()).isEqualTo(ElwhaBadge.Variant.LARGE);
  }

  @Test
  void aBadgeIsNeverFocusable() {
    assertThat(ElwhaBadge.small().isFocusable())
        .as("§8.3 / §10.5 — a badge is a decoration; the host owns the focus surface")
        .isFalse();
    assertThat(ElwhaBadge.large("9").isFocusable()).isFalse();
  }

  // ------------------------------------------------------------- size axis

  @Test
  void aSmallBadgeIsAFixedSixBySixBox() {
    assertThat(ElwhaBadge.small().getPreferredSize())
        .as("§4 — Small is a 6 dp square, content-independent")
        .isEqualTo(new Dimension(SMALL_SIZE, SMALL_SIZE));
  }

  @ParameterizedTest
  @ValueSource(strings = {"1", "42", "999", "999+", "BET+"})
  void aLargeBadgeIsSixteenTallAtEveryContentWidth(final String content) {
    assertThat(ElwhaBadge.large(content).getPreferredSize().height)
        .as("§4 — only the width axis is dynamic; the pill height is pinned")
        .isEqualTo(LARGE_HEIGHT);
  }

  @Test
  void largeWidthIsTheLabelPlusBothPaddingColumns() {
    final ElwhaBadge badge = ElwhaBadge.large("999+");

    assertThat(badge.getPreferredSize().width)
        .as("§4.1 / §4.2 — the label plus 4 dp of interior padding on each side")
        .isEqualTo(labelWidth(badge, "999+") + 2 * LARGE_PADDING);
  }

  @Test
  void aNarrowLabelIsFlooredAtThePillHeight() {
    final ElwhaBadge badge = ElwhaBadge.large("1");

    assertThat(badge.getPreferredSize().width)
        .as("§4.1 — a one-digit pill never goes narrower than tall, so it stays a round chip")
        .isEqualTo(Math.max(LARGE_HEIGHT, labelWidth(badge, "1") + 2 * LARGE_PADDING));
    assertThat(badge.getPreferredSize().width).isGreaterThanOrEqualTo(LARGE_HEIGHT);
  }

  @Test
  void widerContentAsksForAWiderPill() {
    final int oneDigit = ElwhaBadge.large("1").getPreferredSize().width;
    final int fourGlyphs = ElwhaBadge.large("999+").getPreferredSize().width;

    assertThat(fourGlyphs)
        .as("§4.1 — Large width tracks its label, which is why the anchor re-places on change")
        .isGreaterThan(oneDigit);
  }

  @Test
  void widthFollowsAContentChangeInPlace() {
    final ElwhaBadge badge = ElwhaBadge.large("1");
    final int narrow = badge.getPreferredSize().width;

    badge.setContent("999+");

    assertThat(badge.getPreferredSize().width)
        .as("the pill re-measures against the new label rather than caching the old width")
        .isGreaterThan(narrow);
  }

  @Test
  void aBadgeNeverCompressesBelowItsPreferredSize() {
    final ElwhaBadge small = ElwhaBadge.small();
    final ElwhaBadge large = ElwhaBadge.large("999+");

    assertThat(small.getMinimumSize())
        .as("a badge is an atom — there is no smaller legible form to shrink into")
        .isEqualTo(small.getPreferredSize());
    assertThat(large.getMinimumSize()).isEqualTo(large.getPreferredSize());
  }

  // -------------------------------------------------- change notification

  @Test
  void changingContentFiresTheContentProperty() {
    final ElwhaBadge badge = ElwhaBadge.large("1");
    final List<PropertyChangeEvent> events = new ArrayList<>();
    badge.addPropertyChangeListener(ElwhaBadge.PROPERTY_CONTENT, events::add);

    badge.setContent("42");

    assertThat(events).as("the anchor re-places off this notification").hasSize(1);
    assertThat(events.get(0).getOldValue()).isEqualTo("1");
    assertThat(events.get(0).getNewValue()).isEqualTo("42");
  }

  @Test
  void contentEventCarriesTheCoercedValueNotTheRawOne() {
    final ElwhaBadge badge = ElwhaBadge.large("1");
    final List<PropertyChangeEvent> events = new ArrayList<>();
    badge.addPropertyChangeListener(ElwhaBadge.PROPERTY_CONTENT, events::add);

    badge.setContent(5000);

    assertThat(events.get(0).getNewValue())
        .as("listeners see what will be painted, not what was asked for")
        .isEqualTo("999+");
  }

  @Test
  void reSettingTheSameCoercedContentIsNotAChange() {
    final ElwhaBadge badge = ElwhaBadge.large(1000);
    final List<PropertyChangeEvent> events = new ArrayList<>();
    badge.addPropertyChangeListener(ElwhaBadge.PROPERTY_CONTENT, events::add);

    badge.setContent(2000);

    assertThat(events)
        .as("both counts coerce to '999+' — nothing about the badge changed, so nothing fires")
        .isEmpty();
  }
}
