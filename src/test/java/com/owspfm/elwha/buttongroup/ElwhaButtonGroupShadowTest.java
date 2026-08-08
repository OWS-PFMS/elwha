package com.owspfm.elwha.buttongroup;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.Pixels;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A evidence that {@link ElwhaButtonGroup}'s {@code getMaximumSize == getPreferredSize} does
 * <em>not</em> reproduce the #199 shadow trap over elevated segments — the check #660 asked for,
 * settled here rather than left open.
 *
 * <p>The suspicion was structural: conventions §8 forbids {@code max == preferred} on a primitive
 * whose preferred size bakes in its own shadow halo, and the group's preferred height is derived
 * from its segments' preferred heights, which for an elevated {@link ElwhaButton} do include the
 * halo. So the group inherits halo-in-preferred through its children and clamps max to it.
 *
 * <p>It does not transfer, and the reason is what the doctrine's wording was reaching for: the trap
 * belongs to the primitive that <em>paints</em> the shadow, not to any container that happens to
 * carry halo in its own measurement. The group paints no shadow. Each segment paints its own,
 * inside its own bounds, and no segment clamps its own maximum — {@code ElwhaButton} deliberately
 * does not override {@code getMaximumSize}, which is the #199 fix itself. The group's clamp governs
 * only how far its parent may stretch <em>the group</em>; it never reaches a child's paint path.
 *
 * <p>Note the clamp is also the behaviour you want here: {@code doLayout} places {@code STANDARD}
 * segments at fixed widths from the leading edge, so a group allowed to stretch would gain trailing
 * dead space rather than wider buttons. Returning {@code (MAX_VALUE, preferred.height)} — the fix
 * #660 proposed — would buy nothing and cost that.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaButtonGroupShadowTest {

  /** A ground no palette carries, so shadow over ground is unambiguous. */
  private static final Color GROUND = Color.MAGENTA;

  private static ElwhaButtonGroup elevatedGroup() {
    return ElwhaButtonGroup.standard()
        .add(ElwhaButton.elevatedButton("One"))
        .add(ElwhaButton.elevatedButton("Two"))
        .add(ElwhaButton.elevatedButton("Three"));
  }

  /** The group in a vertical {@code BoxLayout} — the layout #660's check named. */
  private static JPanel boxHosted(final ElwhaButtonGroup group, final int w, final int h) {
    final JPanel host = new JPanel();
    host.setLayout(new BoxLayout(host, BoxLayout.Y_AXIS));
    host.setOpaque(false);
    host.add(group);
    host.setSize(w, h);
    return host;
  }

  @Test
  void anElevatedSegmentPaintsIdenticallyInsideTheGroupAndOutsideIt() {
    final ElwhaButtonGroup group = elevatedGroup();
    final Dimension pref = group.getPreferredSize();
    final int w = pref.width + 40;
    final int h = pref.height + 40;

    final BufferedImage hosted = Pixels.render(boxHosted(group, w, h), w, h, GROUND);
    final Rectangle groupBounds = group.getBounds();
    final Rectangle segment = group.getButtonAt(0).getBounds();

    final BufferedImage standalone =
        Pixels.render(ElwhaButton.elevatedButton("One"), segment.width, segment.height, GROUND);

    int differing = 0;
    for (int y = 0; y < segment.height; y++) {
      for (int x = 0; x < segment.width; x++) {
        final int inGroup =
            hosted.getRGB(groupBounds.x + segment.x + x, groupBounds.y + segment.y + y);
        if (inGroup != standalone.getRGB(x, y)) {
          differing++;
        }
      }
    }

    assertThat(differing)
        .as(
            "the group's max-size clamp never reaches a segment's paint path — halo included,"
                + " every pixel matches the same button rendered with no group above it")
        .isZero();
  }

  @Test
  void theHaloAroundAnElevatedSegmentStaysLeftRightSymmetric() {
    final ElwhaButtonGroup group = elevatedGroup();
    final Dimension pref = group.getPreferredSize();
    final int w = pref.width + 40;
    final int h = pref.height + 40;

    final BufferedImage hosted = Pixels.render(boxHosted(group, w, h), w, h, GROUND);
    final Rectangle groupBounds = group.getBounds();
    final Rectangle segment = group.getButtonAt(0).getBounds();
    final int left = groupBounds.x + segment.x + 1;
    final int right = groupBounds.x + segment.x + segment.width - 2;
    final int midY = groupBounds.y + segment.y + segment.height / 2;

    // #199's signature was a dark concentration at one corner; an M3 shadow is bottom-weighted by
    // design, so left-versus-right is the axis where asymmetry would mean a defect.
    Pixels.assertPixelExact(
        hosted,
        right,
        midY,
        new Color(hosted.getRGB(left, midY), true),
        "the halo band is the same colour on both flanks of the segment");
  }

  @Test
  void theGroupClampsItsOwnStretchWhileItsSegmentsDoNot() {
    final ElwhaButtonGroup group = elevatedGroup();

    assertThat(group.getMaximumSize())
        .as("the group refuses to stretch — doLayout would only add trailing dead space")
        .isEqualTo(group.getPreferredSize());
    assertThat(group.getButtonAt(0).getMaximumSize().height)
        .as("while the shadow-painting leaf leaves its own maximum unclamped, per #199")
        .isGreaterThan(group.getButtonAt(0).getPreferredSize().height);
  }
}
