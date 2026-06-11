package com.owspfm.elwha.appbar;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.accessibility.AccessibleRole;

/**
 * S5 headless guard for {@link ElwhaAppBar} a11y, RTL and enabled propagation (#461): PANEL role
 * with the live title/subtitle accessible name, convenience-set button accessible names, the full
 * RTL slot/title mirror, enabled cascade with per-button state restore (including children added
 * while disabled), and the disabled text opacity treatment.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaAppBarA11ySmoke {

  private static final int BAR_WIDTH = 640;

  private ElwhaAppBarA11ySmoke() {}

  /**
   * Runs the guard; exits non-zero on the first failed check.
   *
   * @param args unused
   * @version v0.4.0
   * @since v0.4.0
   */
  public static void main(final String[] args) {
    System.setProperty("java.awt.headless", "true");
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());

    checkAccessibleContext();
    checkRtlMirror();
    checkEnabledPropagation();
    checkDisabledText();

    System.out.println(
        "ElwhaAppBarA11ySmoke: OK (PANEL role + live name, convenience names, RTL mirror, enabled"
            + " cascade + restore, disabled text)");
  }

  private static void checkAccessibleContext() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    check("role is PANEL", bar.getAccessibleContext().getAccessibleRole() == AccessibleRole.PANEL);
    check("empty bar has null name", bar.getAccessibleContext().getAccessibleName() == null);

    bar.setTitle("Inbox");
    check("name is the title", "Inbox".equals(bar.getAccessibleContext().getAccessibleName()));
    bar.setSubtitle("Synced");
    check(
        "name splices the subtitle",
        "Inbox, Synced".equals(bar.getAccessibleContext().getAccessibleName()));
    bar.setTitle("Archive");
    check(
        "name tracks the title setter",
        "Archive, Synced".equals(bar.getAccessibleContext().getAccessibleName()));
    bar.setSubtitle(null);
    check("subtitle clears out", "Archive".equals(bar.getAccessibleContext().getAccessibleName()));

    bar.getAccessibleContext().setAccessibleName("Declared");
    check("declared name wins", "Declared".equals(bar.getAccessibleContext().getAccessibleName()));

    final ElwhaIconButton nav =
        bar.setNavigationIcon(MaterialIcons.menu(), "Open navigation", null);
    check(
        "convenience sets the button accessible name",
        "Open navigation".equals(nav.getAccessibleContext().getAccessibleName()));
    check("convenience sets the tooltip", "Open navigation".equals(nav.getToolTipText()));
    final ElwhaIconButton action = bar.addAction(MaterialIcons.moreVert(), "More options", null);
    check(
        "action convenience sets the accessible name",
        "More options".equals(action.getAccessibleContext().getAccessibleName()));
  }

  private static void checkRtlMirror() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setTitle("Title");
    final ElwhaIconButton nav = bar.setNavigationIcon(MaterialIcons.menu(), "Navigation", null);
    final ElwhaIconButton first = bar.addAction(MaterialIcons.favorite(), "Favorite", null);
    final ElwhaIconButton last = bar.addAction(MaterialIcons.moreVert(), "More", null);
    bar.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    bar.setSize(BAR_WIDTH, 64);
    bar.doLayout();

    check("RTL nav button hugs the right edge", nav.getX() == BAR_WIDTH - 4 - 48 + 4);
    check("RTL last action hugs the left edge", last.getX() == 4 + 4);
    check(
        "RTL action order mirrors (first action right of last)",
        first.getX() == last.getX() + ElwhaAppBar.SLOT_SIZE_PX);

    final ElwhaAppBar titleOnly = ElwhaAppBar.small();
    titleOnly.setTitle("Title");
    titleOnly.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    titleOnly.setSize(BAR_WIDTH, 64);
    final BufferedImage image = render(titleOnly, 64);
    final int lastInk = lastInkX(image, 12, 52);
    check(
        "RTL title right-aligns at the 16px inset",
        lastInk >= BAR_WIDTH - 20 && lastInk <= BAR_WIDTH - 13);

    final ElwhaAppBar expanded = ElwhaAppBar.largeFlexible();
    expanded.setTitle("Headline");
    expanded.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    expanded.setSize(BAR_WIDTH, 120);
    final BufferedImage expandedImage = render(expanded, 120);
    final int expandedLastInk = lastInkX(expandedImage, 66, 118);
    check(
        "RTL expanded headline right-aligns at the 16px margin",
        expandedLastInk >= BAR_WIDTH - 20 && expandedLastInk <= BAR_WIDTH - 13);
  }

  private static void checkEnabledPropagation() {
    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setTitle("Inbox");
    final ElwhaIconButton nav = bar.setNavigationIcon(MaterialIcons.menu(), "Navigation", null);
    final ElwhaIconButton normal = bar.addAction(MaterialIcons.favorite(), "Favorite", null);
    final ElwhaIconButton consumerDisabled = bar.addAction(MaterialIcons.edit(), "Edit", null);
    consumerDisabled.setEnabled(false);

    bar.setEnabled(false);
    check(
        "disable cascades",
        !nav.isEnabled() && !normal.isEnabled() && !consumerDisabled.isEnabled());

    final ElwhaIconButton addedWhileDisabled =
        bar.addAction(MaterialIcons.moreVert(), "More", null);
    check("child added while disabled joins the treatment", !addedWhileDisabled.isEnabled());

    bar.setEnabled(true);
    check("re-enable restores the normal action", normal.isEnabled() && nav.isEnabled());
    check("consumer-disabled action stays disabled after restore", !consumerDisabled.isEnabled());
    check("disabled-add child restores to enabled", addedWhileDisabled.isEnabled());
  }

  private static void checkDisabledText() {
    final Color onSurface = ColorRole.ON_SURFACE.resolve();
    final Color surface = ColorRole.SURFACE.resolve();

    final ElwhaAppBar bar = ElwhaAppBar.small();
    bar.setTitle("Inbox");
    bar.setSize(BAR_WIDTH, 64);
    check("enabled title is full ON_SURFACE", bandHasColorNear(render(bar, 64), 12, 52, onSurface));

    bar.setEnabled(false);
    final BufferedImage disabled = render(bar, 64);
    check(
        "disabled title is not full-strength ON_SURFACE",
        !bandHasColorNear(disabled, 12, 52, onSurface));
    check("disabled title still paints faded ink", firstInkX(disabled, 0, 12, 52) >= 0);
    check("disabled container stays SURFACE", sample(disabled, BAR_WIDTH - 5, 5).equals(surface));
  }

  private static BufferedImage render(final ElwhaAppBar bar, final int height) {
    bar.doLayout();
    final BufferedImage image = new BufferedImage(BAR_WIDTH, height, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    try {
      bar.paint(g);
    } finally {
      g.dispose();
    }
    return image;
  }

  private static Color sample(final BufferedImage image, final int x, final int y) {
    return new Color(image.getRGB(x, y), true);
  }

  private static boolean bandHasColorNear(
      final BufferedImage image, final int rowFrom, final int rowTo, final Color c) {
    for (int y = rowFrom; y <= rowTo; y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        if (colorDistance(sample(image, x, y), c) < 60) {
          return true;
        }
      }
    }
    return false;
  }

  private static int firstInkX(
      final BufferedImage image, final int fromX, final int rowFrom, final int rowTo) {
    final Color surface = ColorRole.SURFACE.resolve();
    for (int x = fromX; x < image.getWidth(); x++) {
      for (int y = rowFrom; y <= rowTo; y++) {
        if (colorDistance(sample(image, x, y), surface) > 60) {
          return x;
        }
      }
    }
    return -1;
  }

  private static int lastInkX(final BufferedImage image, final int rowFrom, final int rowTo) {
    final Color surface = ColorRole.SURFACE.resolve();
    for (int x = image.getWidth() - 1; x >= 0; x--) {
      for (int y = rowFrom; y <= rowTo; y++) {
        if (colorDistance(sample(image, x, y), surface) > 60) {
          return x;
        }
      }
    }
    return -1;
  }

  private static int colorDistance(final Color a, final Color b) {
    return Math.abs(a.getRed() - b.getRed())
        + Math.abs(a.getGreen() - b.getGreen())
        + Math.abs(a.getBlue() - b.getBlue());
  }

  private static void check(final String what, final boolean ok) {
    if (!ok) {
      System.err.println("FAIL: " + what);
      System.exit(1);
    }
    System.out.println("  ok: " + what);
  }
}
