package com.owspfm.elwha.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of {@link Pixels}' own contract — the render helper the rest of the suite probes
 * through, so a gap here is invisible everywhere else.
 *
 * <p>Two properties are pinned. The layout pass reaches <em>every</em> descendant, not just the
 * immediate children: {@code doLayout()} alone leaves a grandchild at its default 0×0, painting
 * nothing, and a probe aimed at it then reads the ground and passes against code that never drew
 * (#707 — a #589 comparison passed exactly this way, both rasters equally empty). And {@link
 * Pixels#assertPixelExact} actually discriminates where {@link Pixels#assertPixelNear} cannot,
 * which is what the adjacent-surface-role probes depend on.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class PixelsRenderTest {

  private static final Color INK = new Color(0xFF00FF);
  private static final Color GROUND = new Color(0x101010);

  /** Fills its whole bounds with {@link #INK} — invisible if it is never given any. */
  private static final class InkLeaf extends JPanel {
    @Override
    protected void paintComponent(final Graphics g) {
      g.setColor(INK);
      g.fillRect(0, 0, getWidth(), getHeight());
    }
  }

  /** child (depth 1) wrapping grandchild (depth 2) — the leaf only paints if both were laid out. */
  private static JPanel nestedTwoDeep() {
    final JPanel root = new JPanel(new BorderLayout());
    root.setOpaque(false);
    final JPanel child = new JPanel(new BorderLayout());
    child.setOpaque(false);
    child.add(new InkLeaf(), BorderLayout.CENTER);
    root.add(child, BorderLayout.CENTER);
    return root;
  }

  @Test
  void aGrandchildIsLaidOutAndPaints() {
    final BufferedImage shot = Pixels.render(nestedTwoDeep(), 40, 40, GROUND);

    Pixels.assertPixelExact(
        shot, 20, 20, INK, "a component two levels down is bounded by the recursive layout pass");
  }

  @Test
  void aSingleLevelLayoutWouldHaveLeftItBlank() {
    final JPanel root = nestedTwoDeep();
    root.setSize(40, 40);
    root.doLayout();

    assertThat(root.getComponent(0).getSize())
        .as("doLayout bounds the immediate child")
        .isEqualTo(new java.awt.Dimension(40, 40));
    assertThat(((JPanel) root.getComponent(0)).getComponent(0).getSize())
        .as("but leaves the grandchild at 0x0 — the reason render cannot stop at one level")
        .isEqualTo(new java.awt.Dimension(0, 0));
  }

  @Test
  void anExactProbeSeparatesColorsANearProbeCannot() {
    final Color close = new Color(INK.getRed() - 5, INK.getGreen() + 5, INK.getBlue() - 5);
    final BufferedImage shot = Pixels.render(nestedTwoDeep(), 40, 40, GROUND);

    Pixels.assertPixelNear(shot, 20, 20, close, "5/255 apart is inside the near tolerance");

    assertThat(
            org.assertj.core.api.Assertions.catchThrowable(
                () -> Pixels.assertPixelExact(shot, 20, 20, close, "but not inside exact")))
        .as("the exact probe is what makes an adjacent-token assertion mean something")
        .isNotNull();
  }
}
