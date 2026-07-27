package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.dialog.DismissCause;
import com.owspfm.elwha.dialog.ElwhaFullScreenDialog;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JComponent;

/**
 * S9 headless guard for the full-screen presentation (#494): the built full-screen dialog hosts the
 * embedded picker (renderPreview anatomy + light/dark paint pass with a staged-color pixel probe),
 * and the shared close routing — CONFIRM (the top-bar Save) delivers and re-stages, CANCEL/ESC (the
 * leading ✕ / Esc) discard and run the cancel callback. The overlay show itself needs a real window
 * — the demo covers it.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerFullScreenSmoke {

  private ElwhaColorPickerFullScreenSmoke() {}

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

    checkAnatomy();
    checkRouting();

    System.out.println("ElwhaColorPickerFullScreenSmoke: OK (anatomy, paint, close routing)");
  }

  private static void checkAnatomy() {
    for (final Mode mode : new Mode[] {Mode.LIGHT, Mode.DARK}) {
      ElwhaTheme.install(ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(mode).build());
      final ElwhaColorPickerDialog dialog = new ElwhaColorPickerDialog();
      dialog.setInitialColor(new Color(0xD32F2F));
      dialog.stage();
      final ElwhaFullScreenDialog fullScreen = dialog.buildFullScreen();
      check("full-screen dialog builds", fullScreen != null);

      final JComponent preview = fullScreen.renderPreview();
      check("picker is hosted (" + mode + ")", findPicker(preview) == dialog.picker());

      layoutTree(preview, new Dimension(1000, 720));
      final BufferedImage image = new BufferedImage(1000, 720, BufferedImage.TYPE_INT_RGB);
      final Graphics2D g2 = image.createGraphics();
      g2.setColor(Color.GRAY);
      g2.fillRect(0, 0, 1000, 720);
      preview.paint(g2);
      g2.dispose();
      check("staged color reaches pixels (" + mode + ")", contains(image, new Color(0xD32F2F)));
    }
  }

  private static void checkRouting() {
    final ElwhaColorPickerDialog dialog = new ElwhaColorPickerDialog();
    final AtomicReference<Color> confirmed = new AtomicReference<>();
    final AtomicInteger cancels = new AtomicInteger();
    dialog.onConfirm(confirmed::set);
    dialog.onCancel(cancels::incrementAndGet);

    dialog.setInitialColor(new Color(0x00897B));
    dialog.stage();
    dialog.picker().setColor(new Color(0x42A5F5));

    dialog.handleClose(DismissCause.CANCEL);
    check("leading-✕ path discards", confirmed.get() == null && cancels.get() == 1);
    check(
        "staged color survives the discard", new Color(0x00897B).equals(dialog.getInitialColor()));

    dialog.stage();
    dialog.picker().setColor(new Color(0x42A5F5));
    dialog.handleClose(DismissCause.CONFIRM);
    check("Save delivers the pending color", new Color(0x42A5F5).equals(confirmed.get()));
    check("Save re-stages", new Color(0x42A5F5).equals(dialog.getInitialColor()));
  }

  private static ElwhaColorPicker findPicker(final Container root) {
    for (final Component child : root.getComponents()) {
      if (child instanceof ElwhaColorPicker picker) {
        return picker;
      }
      if (child instanceof Container nested) {
        final ElwhaColorPicker found = findPicker(nested);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static boolean contains(final BufferedImage image, final Color target) {
    final int rgb = target.getRGB();
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        if (image.getRGB(x, y) == rgb) {
          return true;
        }
      }
    }
    return false;
  }

  private static void layoutTree(final Component component, final Dimension size) {
    component.setSize(size);
    component.doLayout();
    if (component instanceof Container container) {
      for (final Component child : container.getComponents()) {
        if (child.getWidth() == 0 || child.getHeight() == 0) {
          child.setSize(child.getPreferredSize());
        }
        layoutTree(child, child.getSize());
      }
    }
  }

  private static void check(final String message, final boolean condition) {
    if (!condition) {
      System.err.println("FAIL: " + message);
      System.exit(1);
    }
  }
}
