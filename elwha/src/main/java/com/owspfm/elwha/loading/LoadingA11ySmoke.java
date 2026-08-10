package com.owspfm.elwha.loading;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleValue;

/**
 * Headless guard for the S5 accessibility + sizing surface (story #518). Asserts the {@link
 * AccessibleRole#PROGRESS_BAR} role, the value/{@link AccessibleState#BUSY} semantics across modes,
 * the {@link AccessibleValue} model bridge, the non-focusable contract, and that the size knobs
 * drive the preferred size. Runs headless.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class LoadingA11ySmoke {

  private LoadingA11ySmoke() {}

  /**
   * Runs the guard. Exits non-zero on any failed assertion.
   *
   * @param args unused
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    // Indeterminate: PROGRESS_BAR, BUSY, withheld value.
    final ElwhaLoadingIndicator indet = new ElwhaLoadingIndicator();
    final AccessibleContext ic = indet.getAccessibleContext();
    check("role is PROGRESS_BAR", ic.getAccessibleRole() == AccessibleRole.PROGRESS_BAR);
    check("indeterminate is BUSY", ic.getAccessibleStateSet().contains(AccessibleState.BUSY));
    check(
        "indeterminate value withheld",
        ic.getAccessibleValue().getCurrentAccessibleValue() == null);
    check("not focusable", !indet.isFocusable());

    // Determinate: value exposed, not BUSY, min/max bounded.
    final ElwhaLoadingIndicator det = ElwhaLoadingIndicator.determinate();
    det.setValue(42);
    final AccessibleContext dc = det.getAccessibleContext();
    check("determinate not BUSY", !dc.getAccessibleStateSet().contains(AccessibleState.BUSY));
    final AccessibleValue av = dc.getAccessibleValue();
    check("determinate value == 42", av.getCurrentAccessibleValue().intValue() == 42);
    check("min == 0", av.getMinimumAccessibleValue().intValue() == 0);
    check("max == 100", av.getMaximumAccessibleValue().intValue() == 100);
    check("setCurrentAccessibleValue writes through", av.setCurrentAccessibleValue(75));
    check("value now 75", det.getValue() == 75);

    // Sizing knobs drive the preferred size.
    final ElwhaLoadingIndicator s = new ElwhaLoadingIndicator();
    s.setIndicatorSize(64);
    check("standard preferred tracks indicator size", s.getPreferredSize().width == 64);
    final ElwhaLoadingIndicator cs = ElwhaLoadingIndicator.contained();
    cs.setContainerSize(80);
    check("contained preferred tracks container size", cs.getPreferredSize().width == 80);
    cs.setIndicatorSize(4); // clamped to >= 8
    check("indicator size clamped to >= 8", cs.getIndicatorSize() == 8);

    System.out.println("LoadingA11ySmoke: PASS");
  }

  private static void check(final String label, final boolean condition) {
    if (!condition) {
      System.err.println("FAIL: " + label);
      System.exit(1);
    }
    System.out.println("ok: " + label);
  }
}
