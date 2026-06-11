package com.owspfm.elwha.colorpicker;

import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * S2 headless guard for the SWATCHES pane (#484): catalog integrity (twenty hues × ten verbatim
 * 2014-palette shades), hue/shade/recent activation through the picker's commit path, recent-row
 * MRU semantics (dedupe, cap, adjusting commits excluded), external sync of the active hue, and a
 * light+dark paint pass asserting the Red-500 hue cell and the primary selection ring hit pixels.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaColorPickerSwatchesSmoke {

  private ElwhaColorPickerSwatchesSmoke() {}

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

    checkCatalog();
    checkActivation();
    checkRecent();
    for (final Mode mode : new Mode[] {Mode.LIGHT, Mode.DARK}) {
      ElwhaTheme.install(
          ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(mode).build());
      checkPaint(mode);
    }

    System.out.println(
        "ElwhaColorPickerSwatchesSmoke: OK (catalog, activation, recent, light+dark paint)");
  }

  private static void checkCatalog() {
    final List<MaterialSwatchCatalog.Hue> hues = MaterialSwatchCatalog.hues();
    check("twenty hues", hues.size() == 20);
    check("first hue is Red", "Red".equals(hues.get(0).name()));
    check("last hue is Monochrome", "Monochrome".equals(hues.get(19).name()));
    for (final MaterialSwatchCatalog.Hue hue : hues) {
      check(hue.name() + " has ten shades", hue.shades().length == 10);
    }
    check("Red 500 verbatim", new Color(0xF44336).equals(hues.get(0).shades()[5]));
    check("Monochrome runs white to black",
        Color.WHITE.equals(hues.get(19).shades()[0])
            && Color.BLACK.equals(hues.get(19).shades()[9]));
    final int[] found = MaterialSwatchCatalog.find(new Color(0x00796B));
    check("find locates Teal 700", found != null && found[0] == 8 && found[1] == 7);
    check("find misses non-catalog color", MaterialSwatchCatalog.find(new Color(1, 2, 3)) == null);
  }

  private static void checkActivation() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();
    final SwatchesPane pane = (SwatchesPane) picker.paneFor(PickerMode.SWATCHES);
    check("white resolves to the Monochrome hue", pane.activeHueIndex() == 19);

    final AtomicInteger fires = new AtomicInteger();
    picker.addChangeListener(e -> fires.incrementAndGet());

    pane.selectHue(0);
    check("hue click commits the 500 shade", new Color(0xF44336).equals(picker.getColor()));
    check("hue click fires once", fires.get() == 1);

    pane.selectShade(7);
    check("shade click commits exactly", new Color(0xD32F2F).equals(picker.getColor()));

    picker.setColor(new Color(0x009688));
    check("external setColor retargets the active hue", pane.activeHueIndex() == 8);

    picker.setColor(new Color(1, 2, 3));
    check("non-catalog color keeps the active hue", pane.activeHueIndex() == 8);
  }

  private static void checkRecent() {
    final ElwhaColorPicker picker = new ElwhaColorPicker();
    final SwatchesPane pane = (SwatchesPane) picker.paneFor(PickerMode.SWATCHES);
    check("recent starts empty", picker.recentColors().isEmpty());

    pane.selectHue(0);
    pane.selectShade(7);
    check(
        "recent is MRU-ordered",
        List.of(new Color(0xD32F2F), new Color(0xF44336)).equals(picker.recentColors()));

    pane.selectHue(0);
    check(
        "re-pick dedupes and moves to front",
        List.of(new Color(0xF44336), new Color(0xD32F2F)).equals(picker.recentColors()));

    picker.commitFromPane(null, new Color(9, 9, 9), true);
    check("adjusting commits stay out of recent", picker.recentColors().size() == 2);
    picker.commitFromPane(null, new Color(9, 9, 9), false);
    check("settling commit lands in recent", picker.recentColors().size() == 3);

    for (int i = 0; i < 12; i++) {
      picker.setColor(new Color(40 + i, 0, 0));
    }
    check("recent caps at ten", picker.recentColors().size() == 10);
    check(
        "newest first after cap",
        new Color(51, 0, 0).equals(picker.recentColors().get(0)));

    pane.selectRecent(1);
    check("recent click commits that entry", new Color(50, 0, 0).equals(picker.getColor()));
  }

  private static void checkPaint(final Mode mode) {
    final ElwhaColorPicker picker = new ElwhaColorPicker(new Color(0xF44336));
    picker.setModes(PickerMode.SWATCHES);
    layoutTree(picker, new Dimension(360, 420));
    final BufferedImage image = new BufferedImage(360, 420, BufferedImage.TYPE_INT_RGB);
    final Graphics2D g2 = image.createGraphics();
    g2.setColor(Color.GRAY);
    g2.fillRect(0, 0, 360, 420);
    picker.paint(g2);
    g2.dispose();
    check("Red 500 cell painted (" + mode + ")", contains(image, new Color(0xF44336)));
    check(
        "primary selection ring painted (" + mode + ")",
        contains(image, ColorRole.PRIMARY.resolve()));
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
