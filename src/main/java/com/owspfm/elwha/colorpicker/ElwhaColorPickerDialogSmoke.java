package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.dialog.DismissCause;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * S5 headless guard for {@link ElwhaColorPickerDialog} (#487): staging (initial color into the
 * embedded picker, supporting text suppressed), pending-until-OK close routing (CONFIRM delivers
 * the pending color and re-stages it, CANCEL/ESC/SCRIM discard and run the cancel callback),
 * setInitialColor validation, and the setModes pass-through. The overlay show itself needs a real
 * window — the demo covers it.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerDialogSmoke {

  private ElwhaColorPickerDialogSmoke() {}

  /**
   * Runs the guard.
   *
   * @param args unused
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    checkStaging();
    checkCloseRouting();
    checkValidation();

    System.out.println("ElwhaColorPickerDialogSmoke: OK (staging, close routing, validation)");
  }

  private static void checkStaging() {
    final ElwhaColorPickerDialog dialog = new ElwhaColorPickerDialog();
    check("embedded supporting text suppressed", dialog.picker().getSupportingText() == null);
    check("default title", "Select color".equals(dialog.getTitle()));
    check("default staged white", Color.WHITE.equals(dialog.getInitialColor()));

    dialog.setInitialColor(new Color(0xFF7043));
    dialog.stage();
    check("stage pushes initial color", new Color(0xFF7043).equals(dialog.picker().getColor()));

    dialog.setModes(PickerMode.SLIDERS, PickerMode.SWATCHES);
    check(
        "setModes passes through",
        List.of(PickerMode.SLIDERS, PickerMode.SWATCHES).equals(dialog.picker().getModes()));
  }

  private static void checkCloseRouting() {
    final ElwhaColorPickerDialog dialog = new ElwhaColorPickerDialog();
    final AtomicReference<Color> confirmed = new AtomicReference<>();
    final AtomicInteger cancels = new AtomicInteger();
    dialog.onConfirm(confirmed::set);
    dialog.onCancel(cancels::incrementAndGet);

    dialog.setInitialColor(new Color(0x42A5F5));
    dialog.stage();
    dialog.picker().setColor(new Color(0x2E7D32));

    dialog.handleClose(DismissCause.CANCEL);
    check("cancel discards the pending color", confirmed.get() == null);
    check("cancel callback ran", cancels.get() == 1);
    check("cancel keeps the staged color", new Color(0x42A5F5).equals(dialog.getInitialColor()));

    dialog.stage();
    check(
        "re-stage restores the initial color",
        new Color(0x42A5F5).equals(dialog.picker().getColor()));

    dialog.picker().setColor(new Color(0x2E7D32));
    dialog.handleClose(DismissCause.CONFIRM);
    check("confirm delivers the pending color", new Color(0x2E7D32).equals(confirmed.get()));
    check(
        "confirm re-stages for the next show",
        new Color(0x2E7D32).equals(dialog.getInitialColor()));
    check("confirm never runs cancel", cancels.get() == 1);

    dialog.handleClose(DismissCause.ESC);
    check("esc routes to cancel", cancels.get() == 2);
    dialog.handleClose(DismissCause.SCRIM);
    check("scrim routes to cancel", cancels.get() == 3);
  }

  private static void checkValidation() {
    final ElwhaColorPickerDialog dialog = new ElwhaColorPickerDialog();
    boolean threw = false;
    try {
      dialog.setInitialColor(null);
    } catch (final IllegalArgumentException e) {
      threw = true;
    }
    check("setInitialColor(null) throws", threw);
  }

  private static void check(final String message, final boolean condition) {
    if (!condition) {
      System.err.println("FAIL: " + message);
      System.exit(1);
    }
  }
}
