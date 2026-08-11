package com.owspfm.elwha.tabs;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.badge.ElwhaBadgeAnchor;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Rectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of how a tab anchors its badge — the M3 badge-spec placement rule the tabs epic
 * settled in its second smoke round: the icon corner while the icon is the visual anchor (stacked
 * or icon-only forms), and the inline "Label&nbsp;999+" placement once a label sits beside the
 * content.
 *
 * <p>This is the bounds-versus-painted-body distinction (#493) in its most consequential form here.
 * A fixed tab cell is far wider than its centred content, so anchoring a badge to the cell's
 * trailing <em>edge</em> would strand it out in empty space; {@link
 * ElwhaBadgeAnchor.AnchorMode#LABEL_TRAILING} instead reads the icon-bounds feed, which the tab
 * answers with its <em>text</em> rect. The tests below pin both halves: the mode the tab chooses,
 * and the rect it feeds.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaTabBadgeAnchorTest {

  private static ElwhaTabs barWith(final ElwhaTab... tabs) {
    final ElwhaTabs bar = new ElwhaTabs();
    for (final ElwhaTab tab : tabs) {
      bar.addTab(tab);
    }
    bar.setSize(600, bar.getPreferredSize().height);
    bar.doLayout();
    return bar;
  }

  // ---------------------------------------------------------- anchor mode

  @Test
  void aLabelOnlyTabAnchorsItsBadgeAfterTheLabel() {
    final ElwhaTab tab = ElwhaTab.of("Favorites");
    barWith(tab);

    tab.setBadge(ElwhaBadge.large(84));

    assertThat(tab.badgeAnchorMode())
        .as("the M3 'Label 999+' row — the badge trails the text")
        .isEqualTo(ElwhaBadgeAnchor.AnchorMode.LABEL_TRAILING);
  }

  @Test
  void anInlineIconAndLabelTabAlsoTrailsTheLabel() {
    final ElwhaTab tab = ElwhaTab.of(MaterialIcons.symbol("home"), "Home");
    tab.setInlineIcon(true);
    barWith(tab);

    tab.setBadge(ElwhaBadge.large(3));

    assertThat(tab.badgeAnchorMode())
        .as("once a label sits beside the icon, the label is what the badge follows")
        .isEqualTo(ElwhaBadgeAnchor.AnchorMode.LABEL_TRAILING);
  }

  @Test
  void aStackedIconAndLabelTabAnchorsToTheIconCorner() {
    final ElwhaTab tab = ElwhaTab.of(MaterialIcons.symbol("home"), "Home");
    barWith(tab);

    tab.setBadge(ElwhaBadge.small());

    assertThat(tab.isStacked()).as("the primary icon+label default is the stacked column").isTrue();
    assertThat(tab.badgeAnchorMode())
        .as("with the icon above the label, the icon is the visual anchor")
        .isEqualTo(ElwhaBadgeAnchor.AnchorMode.ICON_CORNER);
  }

  @Test
  void anIconOnlyTabAnchorsToTheIconCorner() {
    final ElwhaTab tab = ElwhaTab.iconOnly(MaterialIcons.symbol("home"), "Home");
    barWith(tab);

    tab.setBadge(ElwhaBadge.small());

    assertThat(tab.badgeAnchorMode()).isEqualTo(ElwhaBadgeAnchor.AnchorMode.ICON_CORNER);
  }

  @Test
  void flippingToAnInlineIconReAnchorsAnAttachedBadge() {
    final ElwhaTab tab = ElwhaTab.of(MaterialIcons.symbol("home"), "Home");
    barWith(tab);
    tab.setBadge(ElwhaBadge.large(9));
    assertThat(tab.badgeAnchorMode()).isEqualTo(ElwhaBadgeAnchor.AnchorMode.ICON_CORNER);

    tab.setInlineIcon(true);

    assertThat(tab.badgeAnchorMode())
        .as("a form change re-pins the badge rather than leaving it on the old anchor")
        .isEqualTo(ElwhaBadgeAnchor.AnchorMode.LABEL_TRAILING);
  }

  @Test
  void flippingBackToStackedRestoresTheIconCornerAnchor() {
    final ElwhaTab tab = ElwhaTab.of(MaterialIcons.symbol("home"), "Home");
    tab.setInlineIcon(true);
    barWith(tab);
    tab.setBadge(ElwhaBadge.large(9));

    tab.setInlineIcon(false);

    assertThat(tab.badgeAnchorMode()).isEqualTo(ElwhaBadgeAnchor.AnchorMode.ICON_CORNER);
  }

  @Test
  void aFormChangeThatDoesNotFlipTheModeLeavesTheAnchorAlone() {
    final ElwhaTab tab = ElwhaTab.of(MaterialIcons.symbol("home"), "Home");
    tab.setInlineIcon(true);
    barWith(tab);
    tab.setBadge(ElwhaBadge.large(9));

    tab.setInlineIcon(true);

    assertThat(tab.badgeAnchorMode())
        .as("re-pinning is only for real mode changes")
        .isEqualTo(ElwhaBadgeAnchor.AnchorMode.LABEL_TRAILING);
  }

  @Test
  void clearingTheBadgeDetachesIt() {
    final ElwhaTab tab = ElwhaTab.of("Favorites");
    barWith(tab);
    final ElwhaBadge badge = ElwhaBadge.large(84);
    tab.setBadge(badge);

    tab.setBadge(null);

    assertThat(tab.getBadge()).isNull();
    assertThat(tab.badgeAnchorMode())
        .as("no badge, no anchor — the mode is cleared with the attachment")
        .isNull();
  }

  @Test
  void replacingABadgeSwapsTheAttachment() {
    final ElwhaTab tab = ElwhaTab.of("Favorites");
    barWith(tab);
    final ElwhaBadge first = ElwhaBadge.large(1);
    final ElwhaBadge second = ElwhaBadge.large(2);
    tab.setBadge(first);

    tab.setBadge(second);

    assertThat(tab.getBadge()).isSameAs(second);
    assertThat(first.getParent())
        .as("§9.3 — one badge per host, so the old one is unmounted")
        .isNull();
  }

  @Test
  void aTabWithNoBadgeReportsNoAnchorMode() {
    final ElwhaTab tab = ElwhaTab.of("Plain");
    barWith(tab);

    assertThat(tab.getBadge()).isNull();
    assertThat(tab.badgeAnchorMode()).isNull();
  }

  // ------------------------------------------------- the icon-bounds feed

  @Test
  void aLabelTabFeedsItsTextRectNotItsCell() {
    final ElwhaTab tab = ElwhaTab.of("Favorites");
    barWith(tab);

    final Rectangle feed = tab.getIconBounds();

    assertThat(feed.width)
        .as("#493 — the fixed cell is far wider than its centred text; the feed is the text")
        .isLessThan(tab.getWidth());
    assertThat(feed.x)
        .as("and it starts inside the cell, where the centred label actually begins")
        .isPositive();
  }

  @Test
  void fedRectIsCentredInsideTheCell() {
    final ElwhaTab tab = ElwhaTab.of("Favorites");
    barWith(tab);

    final Rectangle feed = tab.getIconBounds();

    final int leading = feed.x;
    final int trailing = tab.getWidth() - (feed.x + feed.width);
    assertThat(Math.abs(leading - trailing))
        .as("#493 — the content is centred, which is exactly why anchoring to the cell edge fails")
        .isLessThanOrEqualTo(2);
  }

  @Test
  void aStackedTabFeedsItsIconRect() {
    final ElwhaTab tab = ElwhaTab.of(MaterialIcons.symbol("home"), "Home");
    barWith(tab);

    final Rectangle feed = tab.getIconBounds();

    assertThat(feed.width)
        .as("the stacked form anchors on the icon, so the feed is the 24 dp glyph box")
        .isEqualTo(ElwhaTab.ICON_SIZE_PX);
    assertThat(feed.height).isEqualTo(ElwhaTab.ICON_SIZE_PX);
  }

  @Test
  void anIconOnlyTabFeedsItsIconRect() {
    final ElwhaTab tab = ElwhaTab.iconOnly(MaterialIcons.symbol("home"), "Home");
    barWith(tab);

    assertThat(tab.getIconBounds().width).isEqualTo(ElwhaTab.ICON_SIZE_PX);
  }

  @Test
  void fedRectIsAFreshCopyEachCall() {
    final ElwhaTab tab = ElwhaTab.of("Favorites");
    barWith(tab);

    final Rectangle first = tab.getIconBounds();
    first.setBounds(0, 0, 1, 1);

    assertThat(tab.getIconBounds())
        .as("the IconBearing contract — a caller mutating the result must not corrupt the tab")
        .isNotEqualTo(first);
  }

  @Test
  void fedRectStaysInsideTheTab() {
    final ElwhaTab tab = ElwhaTab.of(MaterialIcons.symbol("home"), "Home");
    tab.setInlineIcon(true);
    barWith(tab);

    final Rectangle feed = tab.getIconBounds();

    assertThat(feed.x).isNotNegative();
    assertThat(feed.x + feed.width)
        .as("a feed that overhangs its host would place the badge outside the bar")
        .isLessThanOrEqualTo(tab.getWidth());
    assertThat(feed.y + feed.height).isLessThanOrEqualTo(tab.getHeight());
  }

  // ------------------------------------------------------ placed geometry

  @Test
  void badgeIsPlacedAfterTheLabelRect() {
    final ElwhaTab tab = ElwhaTab.of("Favorites");
    final ElwhaTabs bar = barWith(tab);
    final javax.swing.JRootPane root = new javax.swing.JRootPane();
    root.setSize(700, 200);
    root.getLayeredPane().setBounds(0, 0, 700, 200);
    root.getContentPane().setLayout(null);
    root.getContentPane().setBounds(0, 0, 700, 200);
    bar.setBounds(0, 0, 600, bar.getPreferredSize().height);
    root.getContentPane().add(bar);
    bar.doLayout();

    final ElwhaBadge badge = ElwhaBadge.large(84);
    tab.setBadge(badge);

    final Rectangle label = tab.getIconBounds();
    assertThat(badge.getX())
        .as("the M3 'Label 999+' row — the badge sits just past the text, inside the cell")
        .isGreaterThan(tab.getX() + label.x + label.width);
    assertThat(badge.getX())
        .as("and nowhere near the cell's own trailing edge")
        .isLessThan(tab.getX() + tab.getWidth());
  }
}
