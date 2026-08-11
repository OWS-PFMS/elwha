package com.owspfm.elwha.fab.playground;

import com.owspfm.elwha.button.ButtonSize;
import com.owspfm.elwha.button.ButtonVariant;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.fab.ElwhaFab;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.iconbutton.IconButtonSize;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.JComponent;

/**
 * Headless verification harness for the lib-wide icon-size-fidelity fix ([#196]). Not an
 * interactive playground — it builds {@link ElwhaFab}, {@link ElwhaButton}, and {@link
 * ElwhaIconButton} from a <em>default-size</em> 24&nbsp;px {@link MaterialIcons#add()} glyph at
 * every size tier and exits non-zero on any failure, so it doubles as a smoke gate.
 *
 * <p>Proves the hybrid fix two ways:
 *
 * <ol>
 *   <li><strong>Mechanism (exact):</strong> the painted glyph each component holds reports {@code
 *       getIconWidth()/getIconHeight()} equal to the tier's {@code iconPx()} — i.e. a 24&nbsp;px
 *       source glyph is self-healed (derived) to the size the layout reserves. Before the fix the
 *       glyph kept its intrinsic 24&nbsp;px at every tier.
 *   <li><strong>Render proof (end-to-end):</strong> at a disagreeing tier, a default-24 glyph
 *       renders pixel-identical to a glyph the consumer pre-sized to the tier's {@code iconPx()}.
 *       Before the fix the default-24 render painted a smaller glyph, so the two images differed.
 * </ol>
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.3.0
 */
public final class IconSizeFidelityDemo {

  private IconSizeFidelityDemo() {}

  private static int failures;

  /**
   * Runs the verification and exits non-zero if any check fails.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    fabGlyphMatchesTier();
    buttonGlyphMatchesTier();
    iconButtonGlyphMatchesTier();
    iconButtonRawSetIconSizeRescales();
    selfHealsToPreSized();

    if (failures > 0) {
      System.err.println("FAIL: " + failures + " check(s) failed.");
      System.exit(1);
    }
    System.out.println("PASS: all icon-size-fidelity checks passed.");
  }

  // FAB: the stored painted glyph is derived to size.iconPx() at every tier. Read via reflection —
  // the painted glyph is a private paint detail on all three components (#664); getIcon() hands
  // back the icon the caller installed, which is exactly the unscaled 24 px source here.
  private static void fabGlyphMatchesTier() {
    for (final ElwhaFab.Size size : ElwhaFab.Size.values()) {
      final ElwhaFab fab = ElwhaFab.standard(MaterialIcons.add()).setFabSize(size);
      final Icon painted = paintedIcon(ElwhaFab.class, fab, "icon");
      check(
          "FAB " + size + ": painted glyph is " + size.iconPx() + " px (got " + dims(painted) + ")",
          painted != null
              && painted.getIconWidth() == size.iconPx()
              && painted.getIconHeight() == size.iconPx());
    }
  }

  // Button: the painted glyph is derived to buttonSize.iconSizePx() at every tier.
  private static void buttonGlyphMatchesTier() {
    for (final ButtonSize size : ButtonSize.values()) {
      final ElwhaButton button =
          new ElwhaButton("Label", MaterialIcons.add())
              .setVariant(ButtonVariant.FILLED)
              .setButtonSize(size);
      final Icon painted = paintedIcon(ElwhaButton.class, button, "icon");
      check(
          "Button "
              + size
              + ": painted glyph is "
              + size.iconSizePx()
              + " px (got "
              + dims(painted)
              + ")",
          painted != null
              && painted.getIconWidth() == size.iconSizePx()
              && painted.getIconHeight() == size.iconSizePx());
    }
  }

  // IconButton: the painted glyph is derived to the tier's iconPx() at every tier.
  private static void iconButtonGlyphMatchesTier() {
    for (final IconButtonSize size : IconButtonSize.values()) {
      final ElwhaIconButton button = new ElwhaIconButton(MaterialIcons.add()).setButtonSize(size);
      final Icon painted = paintedIcon(ElwhaIconButton.class, button, "restingIcon");
      check(
          "IconButton "
              + size
              + ": painted glyph is "
              + size.iconPx()
              + " px (got "
              + dims(painted)
              + ")",
          painted != null
              && painted.getIconWidth() == size.iconPx()
              && painted.getIconHeight() == size.iconPx());
    }
  }

  // IconButton: the lower-level setIconSize() escape hatch also re-derives the glyph (the field is
  // the paint source, no longer just a layout hint).
  private static void iconButtonRawSetIconSizeRescales() {
    final ElwhaIconButton button = new ElwhaIconButton(MaterialIcons.add()).setIconSize(40);
    final Icon painted = paintedIcon(ElwhaIconButton.class, button, "restingIcon");
    check(
        "IconButton setIconSize(40): painted glyph is 40 px (got " + dims(painted) + ")",
        painted != null && painted.getIconWidth() == 40 && painted.getIconHeight() == 40);
  }

  // End-to-end: at a disagreeing tier, a default-24 glyph must render pixel-identical to a glyph a
  // consumer pre-sized to the tier's iconPx() — the "self-healing" the hybrid promises. Pre-fix the
  // default-24 render painted a smaller glyph and the two images differed.
  private static void selfHealsToPreSized() {
    check(
        "FAB LARGE: default-24 glyph renders identically to a pre-sized 36 px glyph",
        identical(
            ElwhaFab.standard(MaterialIcons.add()).setFabSize(ElwhaFab.Size.LARGE),
            ElwhaFab.standard(MaterialIcons.add(36)).setFabSize(ElwhaFab.Size.LARGE)));
    check(
        "Button XL: default-24 glyph renders identically to a pre-sized 40 px glyph",
        identical(
            new ElwhaButton("Label", MaterialIcons.add())
                .setVariant(ButtonVariant.FILLED)
                .setButtonSize(ButtonSize.XL),
            new ElwhaButton("Label", MaterialIcons.add(40))
                .setVariant(ButtonVariant.FILLED)
                .setButtonSize(ButtonSize.XL)));
    check(
        "IconButton XL: default-24 glyph renders identically to a pre-sized 32 px glyph",
        identical(
            new ElwhaIconButton(MaterialIcons.add()).setButtonSize(IconButtonSize.XL),
            new ElwhaIconButton(MaterialIcons.add(32)).setButtonSize(IconButtonSize.XL)));
  }

  private static boolean identical(final JComponent a, final JComponent b) {
    final BufferedImage ia = render(a);
    final BufferedImage ib = render(b);
    if (ia.getWidth() != ib.getWidth() || ia.getHeight() != ib.getHeight()) {
      return false;
    }
    for (int y = 0; y < ia.getHeight(); y++) {
      for (int x = 0; x < ia.getWidth(); x++) {
        if (ia.getRGB(x, y) != ib.getRGB(x, y)) {
          return false;
        }
      }
    }
    return true;
  }

  private static Icon paintedIcon(
      final Class<?> owner, final Object component, final String field) {
    try {
      final java.lang.reflect.Field f = owner.getDeclaredField(field);
      f.setAccessible(true);
      return (Icon) f.get(component);
    } catch (final ReflectiveOperationException e) {
      throw new IllegalStateException(
          owner.getSimpleName() + "." + field + " moved — update this smoke", e);
    }
  }

  private static BufferedImage render(final JComponent c) {
    final Dimension d = c.getPreferredSize();
    c.setSize(d);
    c.doLayout();
    final BufferedImage img =
        new BufferedImage(Math.max(1, d.width), Math.max(1, d.height), BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();
    try {
      c.printAll(g);
    } finally {
      g.dispose();
    }
    return img;
  }

  private static String dims(final Icon icon) {
    return icon == null ? "null" : icon.getIconWidth() + "x" + icon.getIconHeight();
  }

  private static void check(final String label, final boolean ok) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    if (!ok) {
      failures++;
    }
  }
}
