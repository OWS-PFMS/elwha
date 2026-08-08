package com.owspfm.elwha.theme;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.button.ButtonSize;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.fab.ElwhaFab;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.switches.ElwhaSwitch;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the visible-body contract (#493) — the accessor and, more importantly, the
 * resolver every placement helper calls.
 *
 * <p>The bug class: a fill layout grants a primitive more room than it paints, the body floats
 * centered inside it, and anything positioning against {@code getBounds()} addresses an edge nobody
 * can see. The tooltip's 4&nbsp;dp gap measured to the cell edge instead of the pill; a
 * trailing-edge badge stranded itself in the dead space; a menu opened off the bottom of an
 * invisible rect.
 *
 * <p>What is pinned here is that the body <em>stops moving</em> when the bounds grow — that is the
 * whole property consumers depend on — and that the three-tier resolver degrades correctly for the
 * components that do not implement the interface, including plain consumer components.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class BodyBearingTest {

  private static <T extends javax.swing.JComponent> T atPreferred(final T component) {
    component.setSize(component.getPreferredSize());
    return component;
  }

  // ------------------------------------------------------------------ resolver

  @Test
  void aPlainComponentPaintsItsWholeBounds() {
    final JPanel plain = new JPanel();
    plain.setSize(120, 40);

    assertThat(BodyBearing.bodyBoundsOf(plain))
        .as("an arbitrary consumer component has no body contract, so its bounds are its body")
        .isEqualTo(new Rectangle(0, 0, 120, 40));
  }

  @Test
  void aNullComponentResolvesToAnEmptyRectRatherThanThrowing() {
    assertThat(BodyBearing.bodyBoundsOf(null)).isEqualTo(new Rectangle());
  }

  @Test
  void aShadowBearerThatIsNotABodyBearerHasItsHaloBackedOut() {
    // ElwhaSurface carries its reserve in getInsets() and fills what is left, so the middle tier of
    // the resolver is exactly right for it — no getBodyBounds override needed.
    final com.owspfm.elwha.surface.ElwhaSurface surface =
        new com.owspfm.elwha.surface.ElwhaSurface();
    surface.setElevation(3);
    surface.setSize(200, 100);
    final Insets halo = surface.getShadowInsets();

    assertThat(BodyBearing.bodyBoundsOf(surface))
        .isEqualTo(
            new Rectangle(
                halo.left, halo.top, 200 - halo.left - halo.right, 100 - halo.top - halo.bottom));
  }

  @Test
  void anIconButtonFillsItsBoundsSoItNeedsNoContract() {
    final ElwhaIconButton button = atPreferred(new ElwhaIconButton(MaterialIcons.gridView()));

    assertThat(BodyBearing.bodyBoundsOf(button))
        .as("the glyph is centered but the chrome fills, so bounds and body genuinely agree")
        .isEqualTo(new Rectangle(0, 0, button.getWidth(), button.getHeight()));
  }

  // ----------------------------------------------------------------- producers

  @Test
  void aStretchedButtonsBodyStaysPutWhileItsBoundsGrow() {
    final ElwhaButton button = atPreferred(ElwhaButton.filledButton("Save"));
    final Rectangle atPreferred = button.getBodyBounds();

    button.setSize(button.getWidth() + 200, button.getHeight() + 120);
    final Rectangle stretched = button.getBodyBounds();

    assertThat(stretched.getSize())
        .as("the painted pill does not grow with the cell a fill layout handed it")
        .isEqualTo(atPreferred.getSize());
    assertThat(stretched.x).as("it re-centers horizontally").isGreaterThan(atPreferred.x);
    assertThat(stretched.y).as("and vertically").isGreaterThan(atPreferred.y);
  }

  @Test
  void aButtonsBodyExcludesTheAccessibilityInflationEvenAtPreferredSize() {
    // XS inflates its preferred height to the 48dp minimum target while painting a 32dp pill, so
    // body != bounds before any layout gets involved — this is the case that makes the contract
    // necessary rather than merely tidy.
    final ElwhaButton button =
        atPreferred(ElwhaButton.filledButton("Save").setButtonSize(ButtonSize.XS));

    assertThat(button.getBodyBounds().height)
        .as("the pill is the container height, not the inflated target")
        .isEqualTo(ButtonSize.XS.containerHeightPx())
        .isLessThan(button.getHeight());
  }

  @Test
  void aStretchedSwitchsTrackStaysPut() {
    final ElwhaSwitch toggle = atPreferred(new ElwhaSwitch());
    final Rectangle rest = toggle.getBodyBounds();

    toggle.setSize(toggle.getWidth() + 160, toggle.getHeight() + 80);

    assertThat(toggle.getBodyBounds().getSize()).isEqualTo(rest.getSize());
    assertThat(BodyBearing.bodyBoundsOf(toggle))
        .as("and the resolver returns the same rect the component reports")
        .isEqualTo(toggle.getBodyBounds());
  }

  @Test
  void aFabsBodyStaysPinnedToItsReserveRatherThanCentering() {
    final ElwhaFab fab = atPreferred(ElwhaFab.standard(MaterialIcons.add()));
    final Rectangle rest = fab.getBodyBounds();

    fab.setSize(fab.getWidth() + 100, fab.getHeight() + 100);

    assertThat(fab.getBodyBounds())
        .as("a FAB neither centers nor fills — extra room falls to the trailing/bottom side")
        .isEqualTo(rest);
  }

  @Test
  void aStretchedCheckboxKeepsItsControlBandCentered() {
    final ElwhaCheckbox box = atPreferred(new ElwhaCheckbox("Remember me"));
    final int band = box.getBodyBounds().height;

    box.setSize(box.getWidth(), box.getHeight() + 100);

    final Rectangle body = box.getBodyBounds();
    assertThat(body.height).as("the control band does not stretch").isEqualTo(band);
    assertThat(body.y + body.height / 2)
        .as("and stays centered on the component, where the glyph paints")
        .isEqualTo(box.getHeight() / 2);
  }

  @Test
  void aBodyRectIsAFreshInstanceCallersCannotMutate() {
    final ElwhaButton button = atPreferred(ElwhaButton.filledButton("Save"));

    final Rectangle first = button.getBodyBounds();
    first.x += 999;

    assertThat(button.getBodyBounds().x)
        .as("the contract requires a defensive copy, like ShadowBearing's insets")
        .isNotEqualTo(first.x);
  }
}
