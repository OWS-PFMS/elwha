package com.owspfm.elwha.slider;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.slider.ElwhaSlider.Handle;
import com.owspfm.elwha.slider.ElwhaSlider.Variant;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.PaintPass;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Rectangle;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the {@link Variant#RANGE} slider — the two-handle value model with its
 * no-cross clamps, the handle-picking rule (including the tie-break that keeps a collapsed span
 * draggable), and the per-handle focus proxies: the range variant hands its own focusability to two
 * child components, which is what makes the #434 disable propagation load-bearing. A disabled range
 * slider whose proxies stayed enabled would remain Tab-reachable and keyboard-drivable.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaSliderRangeTest {

  private static final int LENGTH = 240;

  private static ElwhaSlider span() {
    final ElwhaSlider slider = ElwhaSlider.range(0, 100, 20, 80);
    slider.setSize(LENGTH, slider.getPreferredSize().height);
    return slider;
  }

  // ------------------------------------------------------- the value model

  @Test
  void rangeFactoryBuildsARangeSliderWithBothHandlesPlaced() {
    final ElwhaSlider slider = span();

    assertThat(slider.getVariant()).as("the factory names the variant").isEqualTo(Variant.RANGE);
    assertThat(slider.getLowerValue()).as("the lower handle").isEqualTo(20);
    assertThat(slider.getUpperValue()).as("and the upper").isEqualTo(80);
  }

  @Test
  void lowerHandleCannotCrossTheUpperOne() {
    final ElwhaSlider slider = span();

    slider.setLowerValue(95);

    assertThat(slider.getLowerValue())
        .as("the lower handle stops where the upper one stands rather than passing it")
        .isEqualTo(80);
    assertThat(slider.getUpperValue()).as("leaving the upper handle alone").isEqualTo(80);
  }

  @Test
  void upperHandleCannotCrossTheLowerOne() {
    final ElwhaSlider slider = span();

    slider.setUpperValue(5);

    assertThat(slider.getUpperValue())
        .as("symmetrically clamped at the lower handle")
        .isEqualTo(20);
    assertThat(slider.getLowerValue()).as("which does not move").isEqualTo(20);
  }

  @Test
  void bothHandlesStayInsideTheRange() {
    final ElwhaSlider slider = span();

    slider.setLowerValue(-30);
    slider.setUpperValue(300);

    assertThat(slider.getLowerValue()).as("the lower handle clamps at the minimum").isZero();
    assertThat(slider.getUpperValue()).as("and the upper at the maximum").isEqualTo(100);
  }

  @Test
  void aCollapsedSpanIsLegal() {
    final ElwhaSlider slider = span();

    slider.setLowerValue(80);

    assertThat(slider.getLowerValue()).as("the handles may meet").isEqualTo(80);
    assertThat(slider.getUpperValue()).as("at the same value").isEqualTo(80);
  }

  @Test
  void eachHandleMoveFiresOnceAndANoOpFiresNothing() {
    final ElwhaSlider slider = span();
    final int[] changes = {0};
    slider.addChangeListener(e -> changes[0]++);

    slider.setLowerValue(30);
    slider.setUpperValue(70);
    assertThat(changes[0]).as("one notification per handle move").isEqualTo(2);

    slider.setLowerValue(30);
    slider.setUpperValue(70);
    assertThat(changes[0]).as("and none for a set to the value already held").isEqualTo(2);
  }

  @Test
  void aClampedNoOpAlsoFiresNothing() {
    final ElwhaSlider slider = span();
    slider.setLowerValue(80);
    final int[] changes = {0};
    slider.addChangeListener(e -> changes[0]++);

    slider.setLowerValue(95);

    assertThat(changes[0])
        .as("a move the clamp swallows is not a change, however far it was asked to go")
        .isZero();
  }

  @Test
  void stopsSnapBothHandles() {
    final ElwhaSlider slider = span();
    slider.setStops(25);

    slider.setLowerValue(30);
    slider.setUpperValue(60);

    assertThat(slider.getLowerValue()).as("the lower handle lands on a stop").isEqualTo(25);
    assertThat(slider.getUpperValue()).as("and so does the upper").isEqualTo(50);
  }

  @Test
  void enablingStopsSnapsBothHandlesThatAreAlreadyOffGrid() {
    final ElwhaSlider slider = ElwhaSlider.range(0, 100, 20, 80);

    slider.setStops(25);

    assertThat(slider.getLowerValue())
        .as("enabling stops pulls the lower handle onto the grid immediately (#643)")
        .isEqualTo(25);
    assertThat(slider.getUpperValue())
        .as("and the upper — neither is left off-stop waiting for a drag")
        .isEqualTo(75);
  }

  @Test
  void singleValueApiIsInertOnARangeSlider() {
    final ElwhaSlider slider = span();

    slider.setValue(60);

    assertThat(slider.getLowerValue()).as("setValue moves no range handle (#643)").isEqualTo(20);
    assertThat(slider.getUpperValue()).isEqualTo(80);
    assertThat(slider.getValue())
        .as("and getValue reports the backing model, which RANGE never paints from")
        .isEqualTo(60);
  }

  // ------------------------------------------------------------- geometry

  @Test
  void eachHandleSitsAtItsOwnValuePosition() {
    final ElwhaSlider slider = span();

    assertThat(slider.lowerHandleCenterX())
        .as("the lower handle is placed by the lower value")
        .isLessThan(slider.upperHandleCenterX());

    slider.setLowerValue(0);
    assertThat(slider.lowerHandleCenterX())
        .as("and follows it to the travel start")
        .isEqualTo(ElwhaSlider.HANDLE_WIDTH_PX / 2);
  }

  // ---------------------------------------------------------- handle pick

  @Test
  void aPressPicksTheNearerHandle() {
    final ElwhaSlider slider = span();

    assertThat(slider.pickHandle(slider.lowerHandleCenterX() + 4))
        .as("a press just past the lower handle grabs the lower one")
        .isEqualTo(Handle.LOWER);
    assertThat(slider.pickHandle(slider.upperHandleCenterX() - 4))
        .as("and one just before the upper handle grabs the upper")
        .isEqualTo(Handle.UPPER);
  }

  @Test
  void aTieAtOrBelowTheLowerValueGrabsTheLowerHandle() {
    final ElwhaSlider slider = span();
    slider.setLowerValue(50);
    slider.setUpperValue(50);

    assertThat(slider.pickHandle(slider.lowerHandleCenterX()))
        .as("a collapsed span must stay draggable in both directions — the tie-break decides")
        .isEqualTo(Handle.LOWER);
  }

  @Test
  void aTieAboveTheLowerValueGrabsTheUpperHandle() {
    final ElwhaSlider slider = span();
    slider.setLowerValue(50);
    slider.setUpperValue(50);

    assertThat(slider.pickHandle(slider.upperHandleCenterX() + 20))
        .as("dragging outward from a collapsed span takes the upper handle")
        .isEqualTo(Handle.UPPER);
  }

  @Test
  void draggingMovesOnlyTheGrabbedHandle() {
    final ElwhaSlider slider = span();
    final int midY = slider.getHeight() / 2;

    Input.press(slider, slider.lowerHandleCenterX(), midY);
    Input.drag(slider, slider.lowerHandleCenterX() - 40, midY);

    assertThat(slider.getLowerValue()).as("the grabbed handle moves").isLessThan(20);
    assertThat(slider.getUpperValue()).as("and the other one stays put").isEqualTo(80);
  }

  @Test
  void draggingAHandlePastItsNeighborStopsAtTheNeighbor() {
    final ElwhaSlider slider = span();
    final int midY = slider.getHeight() / 2;

    Input.press(slider, slider.lowerHandleCenterX(), midY);
    Input.drag(slider, slider.getWidth(), midY);

    assertThat(slider.getLowerValue())
        .as("the no-cross clamp holds through a gesture, not just a setter")
        .isEqualTo(80);
  }

  // ----------------------------------------------------- focus-proxy bounds

  @Test
  void aValueChangeMovesTheFocusProxyWithoutWaitingForALayout() {
    final ElwhaSlider slider = span();
    slider.doLayout();
    final Rectangle before = slider.getComponent(0).getBounds();

    slider.setLowerValue(60);

    assertThat(slider.getComponent(0).getBounds())
        .as(
            "#620 — a drag only repaints, never revalidates, so the proxy has to follow the value "
                + "or the focus ring and the screen-reader bounds lag the handle all gesture")
        .isNotEqualTo(before);
  }

  @Test
  void paintingDoesNotMoveTheFocusProxies() {
    final ElwhaSlider slider = span();
    slider.doLayout();
    slider.setLowerValue(60);
    final Rectangle lower = slider.getComponent(0).getBounds();
    final Rectangle upper = slider.getComponent(1).getBounds();

    PaintPass.capture(slider, LENGTH, slider.getPreferredSize().height);

    assertThat(slider.getComponent(0).getBounds())
        .as("#620 — a paint pass must not mutate child bounds; setBounds repaints from inside it")
        .isEqualTo(lower);
    assertThat(slider.getComponent(1).getBounds()).isEqualTo(upper);
  }

  // --------------------------------------------------------- focus model

  @Test
  void rangeVariantDelegatesItsFocusToTwoHandleProxies() {
    final ElwhaSlider slider = span();

    assertThat(slider.isFocusable())
        .as("the slider itself steps out of the traversal order")
        .isFalse();
    assertThat(slider.getComponentCount()).as("in favor of a focus proxy per handle").isEqualTo(2);
  }

  @Test
  void leavingTheRangeVariantTakesTheProxiesBackOut() {
    final ElwhaSlider slider = span();

    slider.setVariant(Variant.STANDARD);

    assertThat(slider.getComponentCount())
        .as("a single-handle slider has no per-handle focus to delegate")
        .isZero();
    assertThat(slider.isFocusable()).as("so it is its own tab stop again").isTrue();
  }

  @Test
  void disablingARangeSliderDisablesItsFocusProxies() {
    final ElwhaSlider slider = span();

    slider.setEnabled(false);

    assertThat(slider.getComponents())
        .as("a live proxy on a dead slider would stay Tab-reachable — the #434 propagation")
        .allMatch(child -> !child.isEnabled());
  }

  @Test
  void aDisabledRangeSlidersProxiesLeaveTheTraversalOrder() {
    final ElwhaSlider slider = span();
    final javax.swing.JPanel host = new javax.swing.JPanel();
    host.setFocusCycleRoot(true);
    // Pin the policy instead of inheriting one. A focus-cycle root with no policy of its own
    // falls back to the KeyboardFocusManager's default, which is JVM-global and which Swing
    // swaps from DefaultFocusTraversalPolicy to LayoutFocusTraversalPolicy the first time any
    // top-level (a JRootPane, say) is constructed anywhere in the JVM. That made this test's
    // result depend on what ran before it in the same fork.
    host.setFocusTraversalPolicy(new java.awt.DefaultFocusTraversalPolicy());
    host.add(slider);
    host.addNotify();
    // Pinned rather than inherited: the JVM-global default policy flips to
    // LayoutFocusTraversalPolicy the first time any JRootPane resolves its UI
    // (UIManager.getUI), and that policy's comparator throws on a cycle whose root is not inside a
    // Window — which no headless host can be. Asking for the sorting-free policy keeps this
    // assertion about proxy participation instead of about which test ran first.
    host.setFocusTraversalPolicy(new java.awt.DefaultFocusTraversalPolicy());
    final java.awt.FocusTraversalPolicy policy = host.getFocusTraversalPolicy();

    assertThat(policy.getFirstComponent(host))
        .as("an enabled range slider offers its lower handle as the first tab stop")
        .isSameAs(slider.getComponent(0));

    slider.setEnabled(false);

    assertThat(policy.getFirstComponent(host))
        .as("disabling it empties the cycle — the proxies are gone from the traversal order")
        .isNull();
  }

  @Test
  void reEnablingRestoresTheProxies() {
    final ElwhaSlider slider = span();
    slider.setEnabled(false);

    slider.setEnabled(true);

    assertThat(slider.getComponents())
        .as("the proxies come back into the traversal order")
        .allMatch(java.awt.Component::isEnabled);
  }

  @Test
  void aDisabledRangeSliderHoldsBothValuesAgainstTheKeyboard() {
    final ElwhaSlider slider = span();
    slider.setEnabled(false);

    Input.pressBoundKey(slider, "pressed RIGHT", "elwhaSlider.right");
    Input.pressBoundKey(slider, "pressed END", "elwhaSlider.max");
    Input.pressBoundKey(slider, "pressed HOME", "elwhaSlider.min");

    assertThat(slider.getLowerValue()).as("the lower handle holds").isEqualTo(20);
    assertThat(slider.getUpperValue()).as("and so does the upper").isEqualTo(80);
  }

  // -------------------------------------------------------------- a11y

  @Test
  void aRangeSliderExposesOneAccessibleChildPerHandle() {
    final AccessibleContext context = span().getAccessibleContext();

    assertThat(context.getAccessibleChildrenCount())
        .as("AT drives a range by its two handles, so each needs a node")
        .isEqualTo(2);
    assertThat(context.getAccessibleChild(0)).as("the lower handle").isNotNull();
    assertThat(context.getAccessibleChild(1)).as("and the upper").isNotNull();
  }

  @Test
  void aSingleHandleSliderExposesNoAccessibleChildren() {
    final ElwhaSlider slider = new ElwhaSlider(0, 100, 50);

    assertThat(slider.getAccessibleContext().getAccessibleChildrenCount())
        .as("one value, one node")
        .isZero();
  }

  @Test
  void eachHandleNodeIsASliderNamedForItsEnd() {
    final ElwhaSlider slider = span();
    slider.setAccessibleLabel("Price");

    final AccessibleContext lower =
        slider.getAccessibleContext().getAccessibleChild(0).getAccessibleContext();
    final AccessibleContext upper =
        slider.getAccessibleContext().getAccessibleChild(1).getAccessibleContext();

    assertThat(lower.getAccessibleRole())
        .as("a handle is itself a slider")
        .isEqualTo(AccessibleRole.SLIDER);
    assertThat(lower.getAccessibleName())
        .as("qualified by which end it is, so the two are distinguishable")
        .isEqualTo("Price Lower");
    assertThat(upper.getAccessibleName()).as("and the other end").isEqualTo("Price Upper");
  }

  @Test
  void anUnlabelledRangeStillDistinguishesItsHandles() {
    final ElwhaSlider slider = span();

    assertThat(
            slider
                .getAccessibleContext()
                .getAccessibleChild(0)
                .getAccessibleContext()
                .getAccessibleName())
        .as("with no label the position alone names the handle")
        .isEqualTo("Lower");
  }

  @Test
  void theRootNodeReportsNoValueRatherThanAStaleOne() {
    final ElwhaSlider slider = span();
    slider.setLowerValue(30);
    slider.setUpperValue(70);

    assertThat(slider.getAccessibleContext().getAccessibleValue())
        .as(
            "#703 — the root mirrored the untouched backing model, so a screen reader parked there"
                + " read the construction-time seed forever; null is the AccessibleContext spelling"
                + " of \"no value here\"")
        .isNull();
    assertThat(slider.getAccessibleContext().getAccessibleChildrenCount())
        .as("and the live values are on the two handle nodes, whose container this is")
        .isEqualTo(2);
  }

  @Test
  void eachHandleNodeReportsItsOwnValueAndItsNoCrossBounds() {
    final ElwhaSlider slider = span();
    final javax.accessibility.AccessibleValue lower =
        slider
            .getAccessibleContext()
            .getAccessibleChild(0)
            .getAccessibleContext()
            .getAccessibleValue();

    assertThat(lower.getCurrentAccessibleValue()).as("the lower handle's own value").isEqualTo(20);
    assertThat(lower.getMinimumAccessibleValue())
        .as("bounded below by the range minimum")
        .isEqualTo(0);
    assertThat(lower.getMaximumAccessibleValue())
        .as("and above by the other handle, not by the range maximum")
        .isEqualTo(80);
  }

  @Test
  void aHandleNodeCanBeDrivenThroughItsAccessibleValue() {
    final ElwhaSlider slider = span();
    final javax.accessibility.AccessibleValue upper =
        slider
            .getAccessibleContext()
            .getAccessibleChild(1)
            .getAccessibleContext()
            .getAccessibleValue();

    assertThat(upper.setCurrentAccessibleValue(60)).as("AT can set the value").isTrue();
    assertThat(slider.getUpperValue()).as("and it lands on the right handle").isEqualTo(60);

    assertThat(upper.setCurrentAccessibleValue(null)).as("a null value is refused").isFalse();
  }
}
