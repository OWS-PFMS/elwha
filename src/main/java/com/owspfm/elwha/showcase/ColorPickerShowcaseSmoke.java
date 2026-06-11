package com.owspfm.elwha.showcase;

import com.owspfm.elwha.colorpicker.ElwhaColorPicker;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * S8 headless guard for the Color picker Showcase surface (#490): the Workbench builds with one
 * persistent live picker whose commits drive the readout label, the gallery builds its six
 * configured pickers (three single-mode, alpha-enabled, recent-populated, disabled), and both
 * panels lay out and paint in light and dark without throwing.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ColorPickerShowcaseSmoke {

  private ColorPickerShowcaseSmoke() {}

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

    checkWorkbench();
    checkGallery();
    for (final Mode mode : new Mode[] {Mode.LIGHT, Mode.DARK}) {
      ElwhaTheme.install(
          ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(mode).build());
      paintPass(ColorPickerShowcasePanels.buildWorkbench(), mode + " workbench");
      paintPass(ColorPickerShowcasePanels.buildGallery(), mode + " gallery");
    }

    System.out.println("ColorPickerShowcaseSmoke: OK (workbench, gallery, light+dark paint)");
  }

  private static void checkWorkbench() {
    final JComponent workbench = ColorPickerShowcasePanels.buildWorkbench();
    final List<ElwhaColorPicker> pickers = new ArrayList<>();
    collect(workbench, ElwhaColorPicker.class, pickers);
    check("workbench hosts one live picker", pickers.size() == 1);

    final ElwhaColorPicker picker = pickers.get(0);
    picker.setColor(new Color(0x00FF00));
    final List<JLabel> labels = new ArrayList<>();
    collect(workbench, JLabel.class, labels);
    boolean readoutUpdated = false;
    for (final JLabel label : labels) {
      final String text = label.getText();
      if (text != null && text.startsWith("getColor()") && text.contains("#00FF00")) {
        readoutUpdated = true;
      }
    }
    check("readout tracks picker commits", readoutUpdated);
  }

  private static void checkGallery() {
    final JComponent gallery = ColorPickerShowcasePanels.buildGallery();
    final List<ElwhaColorPicker> pickers = new ArrayList<>();
    collect(gallery, ElwhaColorPicker.class, pickers);
    check("gallery hosts six pickers", pickers.size() == 6);

    int singleMode = 0;
    int alphaEnabled = 0;
    int disabled = 0;
    for (final ElwhaColorPicker picker : pickers) {
      if (picker.getModes().size() == 1) {
        singleMode++;
      }
      if (picker.isAlphaEnabled()) {
        alphaEnabled++;
      }
      if (!picker.isEnabled()) {
        disabled++;
      }
    }
    check("five single-mode pickers", singleMode == 5);
    check("one alpha-enabled picker", alphaEnabled == 1);
    check("one disabled picker", disabled == 1);
  }

  private static void paintPass(final JComponent panel, final String label) {
    layoutTree(panel, new Dimension(1200, 900));
    final BufferedImage image = new BufferedImage(1200, 900, BufferedImage.TYPE_INT_RGB);
    final Graphics2D g2 = image.createGraphics();
    g2.setColor(Color.GRAY);
    g2.fillRect(0, 0, 1200, 900);
    panel.paint(g2);
    g2.dispose();
    check(label + " painted", true);
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

  private static <T> void collect(final Container root, final Class<T> type, final List<T> out) {
    for (final Component child : root.getComponents()) {
      if (type.isInstance(child)) {
        out.add(type.cast(child));
      }
      if (child instanceof Container nested) {
        collect(nested, type, out);
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
