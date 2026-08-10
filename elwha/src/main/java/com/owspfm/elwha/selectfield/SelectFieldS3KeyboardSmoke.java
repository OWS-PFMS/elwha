package com.owspfm.elwha.selectfield;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleState;

/**
 * Headless behavior smoke for epic #331 S3 (#376) — the expanded/collapsed a11y and reduced-motion
 * arrow rotation. The combobox keyboard (open keys, type-ahead, Esc focus-return) is window- and
 * focus-dependent and is exercised by {@code SelectFieldS3KeyboardDemo}; this guard covers the
 * pieces that are observable without a display: the arrow's accessible-name flip, the {@code
 * ACCESSIBLE_STATE_PROPERTY} expand/collapse announcement, and the reduced-motion instant rotation.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.selectfield.SelectFieldS3KeyboardSmoke"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SelectFieldS3KeyboardSmoke {

  private SelectFieldS3KeyboardSmoke() {}

  /**
   * Runs the smoke and exits non-zero on any failure.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    MorphAnimator.setReducedMotion(true);
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    int checks = 0;
    int failures = 0;

    final ElwhaSelectField<String> sf = ElwhaSelectField.filled("Fruit");
    sf.setOptions(List.of("Apple", "Banana", "Cherry"));
    final ElwhaIconButton arrow = arrowOf(sf);
    final AccessibleContext ctx = arrow.getAccessibleContext();

    final AtomicReference<Object> lastStateEvent = new AtomicReference<>();
    ctx.addPropertyChangeListener(
        e -> {
          if (AccessibleContext.ACCESSIBLE_STATE_PROPERTY.equals(e.getPropertyName())) {
            lastStateEvent.set(e.getNewValue());
          }
        });

    // --- rest state ---
    failures += check(!sf.isExpanded(), "collapsed at rest");
    checks++;
    failures +=
        check("Open options".equals(ctx.getAccessibleName()), "rest a11y name = Open options");
    checks++;
    failures += check(sf.arrowAngle() == 0f, "arrow rests at 0 degrees");
    checks++;

    // --- expand ---
    sf.applyExpandedState(true);
    failures += check(sf.isExpanded(), "expanded after expand");
    checks++;
    failures +=
        check(
            "Close options".equals(ctx.getAccessibleName()), "expanded a11y name = Close options");
    checks++;
    failures +=
        check(
            lastStateEvent.get() == AccessibleState.EXPANDED,
            "expand fires the EXPANDED state announcement");
    checks++;
    failures += check(sf.arrowAngle() == 180f, "reduced-motion: arrow snaps to 180 on expand");
    checks++;

    // --- collapse ---
    sf.applyExpandedState(false);
    failures += check(!sf.isExpanded(), "collapsed after collapse");
    checks++;
    failures +=
        check("Open options".equals(ctx.getAccessibleName()), "collapsed a11y name = Open options");
    checks++;
    failures +=
        check(
            lastStateEvent.get() == AccessibleState.COLLAPSED,
            "collapse fires the COLLAPSED state announcement");
    checks++;
    failures += check(sf.arrowAngle() == 0f, "reduced-motion: arrow snaps to 0 on collapse");
    checks++;

    System.out.println(
        "SelectFieldS3KeyboardSmoke: " + (checks - failures) + "/" + checks + " checks passed");
    if (failures > 0) {
      System.out.println("FAIL: " + failures + " check(s) failed");
      System.exit(1);
    }
    System.out.println("PASS");
  }

  private static ElwhaIconButton arrowOf(final ElwhaSelectField<?> sf) {
    final com.owspfm.elwha.textfield.ElwhaTextField field =
        (com.owspfm.elwha.textfield.ElwhaTextField) sf.getComponent(0);
    return field.getTrailingIconButton();
  }

  private static int check(final boolean condition, final String label) {
    System.out.println((condition ? "  ok   " : "  FAIL ") + label);
    return condition ? 0 : 1;
  }
}
