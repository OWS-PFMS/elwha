package com.owspfm.elwha.button.playground;

import com.owspfm.elwha.button.ButtonInteractionMode;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import java.lang.reflect.Field;
import javax.swing.SwingUtilities;

/**
 * Headless verification harness for {@link ElwhaButton#triggerPressAnimation()} ([#183]). Not an
 * interactive playground — it drives the press morph under reduced motion (so progress snaps
 * deterministically) and exits non-zero on any mismatch, so it doubles as a smoke gate.
 *
 * <p>The core guarantee: {@code triggerPressAnimation()} runs the press morph <em>even on a
 * SELECTABLE button</em>, where a live pointer press deliberately suppresses it ({@code
 * firesPressMorph()} is false so the select-flip owns the shape signal). It also confirms the press
 * returns to rest after the hold, that "Trigger select" ({@code setSelected(!isSelected())}) drives
 * the select morph, and that the press trigger still fires on a CLICKABLE button.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ButtonTriggerMorphDemo {

  private ButtonTriggerMorphDemo() {}

  private static int failures;

  /**
   * Runs the verification and exits non-zero if any check fails.
   *
   * @param args ignored
   */
  public static void main(final String[] args) throws Exception {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    MorphAnimator.setReducedMotion(true); // snap morph progress so the readout is deterministic

    pressTriggerFiresOnSelectable();
    pressTriggerReturnsToRest();
    selectTriggerDrivesSelectMorph();
    pressTriggerFiresOnClickable();

    if (failures > 0) {
      System.err.println("FAIL: " + failures + " check(s) failed.");
      System.exit(1);
    }
    System.out.println(
        "PASS: triggerPressAnimation fires regardless of mode; select toggle morphs.");
  }

  /**
   * The press trigger must advance the press morph even though SELECTABLE suppresses live presses.
   */
  private static void pressTriggerFiresOnSelectable() throws Exception {
    final ElwhaButton button = new ElwhaButton("Selectable");
    button.setInteractionMode(ButtonInteractionMode.SELECTABLE);
    check("SELECTABLE press morph at rest is 0", morph(button, "pressMorph") == 0f);
    button.triggerPressAnimation();
    flushEdt();
    check(
        "triggerPressAnimation advances the press morph on SELECTABLE (got "
            + morph(button, "pressMorph")
            + ")",
        morph(button, "pressMorph") == 1f);
  }

  /** After the hold elapses the scheduled reverse must bring the press morph back to rest. */
  private static void pressTriggerReturnsToRest() throws Exception {
    final ElwhaButton button = new ElwhaButton("Selectable");
    button.setInteractionMode(ButtonInteractionMode.SELECTABLE);
    button.triggerPressAnimation();
    flushEdt();
    float progress = morph(button, "pressMorph");
    final long deadline = System.currentTimeMillis() + 2000;
    while (progress != 0f && System.currentTimeMillis() < deadline) {
      Thread.sleep(20);
      flushEdt();
      progress = morph(button, "pressMorph");
    }
    check("press morph returns to rest after the hold (got " + progress + ")", progress == 0f);
  }

  /** "Trigger select" toggles selection and drives the select morph (only on SELECTABLE). */
  private static void selectTriggerDrivesSelectMorph() throws Exception {
    final ElwhaButton button = new ElwhaButton("Selectable");
    button.setInteractionMode(ButtonInteractionMode.SELECTABLE);
    check("starts unselected", !button.isSelected());
    check("select morph at rest is 0", morph(button, "selectMorph") == 0f);

    button.setSelected(!button.isSelected());
    flushEdt();
    check("select toggle selects", button.isSelected());
    check(
        "select morph advances to 1 (got " + morph(button, "selectMorph") + ")",
        morph(button, "selectMorph") == 1f);

    button.setSelected(!button.isSelected());
    flushEdt();
    check("select toggle deselects", !button.isSelected());
    check(
        "select morph returns to 0 (got " + morph(button, "selectMorph") + ")",
        morph(button, "selectMorph") == 0f);
  }

  /** The press trigger fires on a CLICKABLE button too (where live presses already morph). */
  private static void pressTriggerFiresOnClickable() throws Exception {
    final ElwhaButton button = new ElwhaButton("Clickable");
    button.triggerPressAnimation();
    flushEdt();
    check(
        "triggerPressAnimation advances the press morph on CLICKABLE (got "
            + morph(button, "pressMorph")
            + ")",
        morph(button, "pressMorph") == 1f);
  }

  private static float morph(final ElwhaButton button, final String fieldName) {
    try {
      final Field field = ElwhaButton.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      return ((MorphAnimator) field.get(button)).progress();
    } catch (final ReflectiveOperationException e) {
      throw new IllegalStateException("cannot read " + fieldName, e);
    }
  }

  private static void flushEdt() throws Exception {
    SwingUtilities.invokeAndWait(() -> {});
  }

  private static void check(final String label, final boolean ok) {
    if (!ok) {
      System.out.println("  FAIL " + label);
      failures++;
    } else {
      System.out.println("  ok   " + label);
    }
  }
}
