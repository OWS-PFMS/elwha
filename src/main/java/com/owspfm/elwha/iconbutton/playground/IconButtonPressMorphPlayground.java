package com.owspfm.elwha.iconbutton.playground;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.iconbutton.IconButtonInteractionMode;
import com.owspfm.elwha.iconbutton.IconButtonSize;
import com.owspfm.elwha.iconbutton.IconButtonVariant;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.MorphAnimator;
import com.owspfm.elwha.theme.ShapeScale;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Visual smoke-test for the {@link ElwhaIconButton} M3 Expressive press shape-morph (#295). Press
 * and hold any button (mouse-down) to watch the corner radius morph toward the opposite shape;
 * release to watch it snap back. Tab to a button and tap {@code Space} / {@code Enter} to see the
 * same morph pulsed for keyboard activation.
 *
 * <p>Rows exercise the morph across the dimensions that change its feel: the variant sweep at the
 * round (FULL) default (the headline round → squircle morph), a shape sweep (FULL down to NONE,
 * showing the bidirectional rule — rounder shapes square up, the square shape rounds out), a size
 * sweep (XS → XL), a {@code SELECTABLE} favorite toggle (morph and the selection state-layer
 * coexist), and the {@code STANDARD} dialog-dismiss ✕ that motivated the epic (#290): a quick click
 * registers visually via the morph alone.
 *
 * <p><strong>Slow-mo.</strong> The toolbar's "5× slow motion" box stretches every morph via {@link
 * MorphAnimator#setDurationMultiplier(float)} so the shape change is unmistakable and obviously
 * distinct from the 400 ms ripple — i.e. the morph is its own paint layer, driven by its own
 * animator, not a side effect of the ripple. "Reduced motion" collapses the morph to an instant
 * snap.
 *
 * <p><strong>Ripple on / off.</strong> The toolbar's "Ripple" box drives {@link
 * ElwhaIconButton#setRippleEnabled(boolean)} (epic #288) on every button. Toggle it off and the
 * press morph still fires unchanged — the morph and the ripple are independent paint layers driven
 * by separate animators. This is the #290 dialog-dismiss case: a ripple-suppressed ✕ still confirms
 * a click via the shape morph alone.
 *
 * <p>Independent of The Elwha Showcase, per [[fresh-demo-per-story]] (each story gets its own
 * artifact).
 *
 * <p>Launch: {@code mvn compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.iconbutton.playground.IconButtonPressMorphPlayground"}.
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class IconButtonPressMorphPlayground {

  private static final float SLOW_MOTION_MULTIPLIER = 5f;

  // Every icon button on the frame, so the "Ripple" toggle can drive setRippleEnabled across all.
  private static final List<ElwhaIconButton> BUTTONS = new ArrayList<>();

  private IconButtonPressMorphPlayground() {}

  /**
   * Entry point — installs a baseline LIGHT theme and shows the smoketest frame.
   *
   * @param args ignored
   * @version v0.3.0
   * @since v0.3.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    SwingUtilities.invokeLater(IconButtonPressMorphPlayground::buildAndShow);
  }

  private static void buildAndShow() {
    final JFrame frame = new JFrame("ElwhaIconButton press shape-morph (#295) — smoke-test");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    frame.add(buildToolbar(), BorderLayout.NORTH);
    frame.add(buildGrid(), BorderLayout.CENTER);
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static JPanel buildToolbar() {
    final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));

    final JCheckBox slowMo = new JCheckBox("5× slow motion");
    slowMo.addActionListener(
        e ->
            MorphAnimator.setDurationMultiplier(slowMo.isSelected() ? SLOW_MOTION_MULTIPLIER : 1f));
    toolbar.add(slowMo);

    final JCheckBox ripple = new JCheckBox("Ripple", true);
    ripple.addActionListener(e -> BUTTONS.forEach(b -> b.setRippleEnabled(ripple.isSelected())));
    toolbar.add(ripple);

    final JCheckBox reduced = new JCheckBox("Reduced motion (global)");
    reduced.setSelected(MorphAnimator.isReducedMotion());
    reduced.addActionListener(e -> MorphAnimator.setReducedMotion(reduced.isSelected()));
    toolbar.add(reduced);

    toolbar.add(
        new JLabel(
            "  Press & hold to morph; release to snap back. Tab + Space/Enter pulses the morph."));
    return toolbar;
  }

  private static JPanel buildGrid() {
    final JPanel grid = new JPanel(new GridLayout(0, 5, 18, 14));
    grid.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

    // Variant sweep at the round (FULL) default — the headline round → squircle press morph.
    addRowHeader(grid, "Variants · FULL · L");
    grid.add(wrap(clickable(IconButtonVariant.FILLED, MaterialIcons.edit(), ShapeScale.FULL)));
    grid.add(wrap(clickable(IconButtonVariant.FILLED_TONAL, MaterialIcons.add(), ShapeScale.FULL)));
    grid.add(wrap(clickable(IconButtonVariant.OUTLINED, MaterialIcons.star(), ShapeScale.FULL)));
    grid.add(wrap(clickable(IconButtonVariant.STANDARD, MaterialIcons.menu(), ShapeScale.FULL)));

    // Shape sweep — the bidirectional morph reads at every shape: rounder shapes square up, the
    // square (NONE) shape rounds out.
    addRowHeader(grid, "Shapes · FILLED · L");
    grid.add(wrap(clickable(IconButtonVariant.FILLED, MaterialIcons.edit(), ShapeScale.FULL)));
    grid.add(wrap(clickable(IconButtonVariant.FILLED, MaterialIcons.edit(), ShapeScale.LG)));
    grid.add(wrap(clickable(IconButtonVariant.FILLED, MaterialIcons.edit(), ShapeScale.MD)));
    grid.add(wrap(clickable(IconButtonVariant.FILLED, MaterialIcons.edit(), ShapeScale.NONE)));

    // Size sweep at the round default.
    addRowHeader(grid, "Sizes · FILLED_TONAL · FULL");
    grid.add(wrap(sized(IconButtonSize.XS)));
    grid.add(wrap(sized(IconButtonSize.S)));
    grid.add(wrap(sized(IconButtonSize.L)));
    grid.add(wrap(sized(IconButtonSize.XL)));

    // SELECTABLE — the press morph and the selection state-layer coexist; selection persists, the
    // shape morph does not.
    addRowHeader(grid, "Selectable + dismiss ✕");
    grid.add(wrap(favoriteToggle()));
    grid.add(wrap(dismissButton()));
    grid.add(new JLabel(""));
    grid.add(new JLabel(""));

    return grid;
  }

  private static ElwhaIconButton clickable(
      final IconButtonVariant variant, final Icon icon, final ShapeScale shape) {
    return new ElwhaIconButton(icon)
        .setVariant(variant)
        .setShape(shape)
        .setButtonSize(IconButtonSize.L);
  }

  private static ElwhaIconButton sized(final IconButtonSize size) {
    return new ElwhaIconButton(MaterialIcons.add())
        .setVariant(IconButtonVariant.FILLED_TONAL)
        .setShape(ShapeScale.FULL)
        .setButtonSize(size);
  }

  private static ElwhaIconButton favoriteToggle() {
    return new ElwhaIconButton()
        .setIcons(MaterialIcons.favorite(), MaterialIcons.favoriteFilled())
        .setVariant(IconButtonVariant.FILLED_TONAL)
        .setInteractionMode(IconButtonInteractionMode.SELECTABLE)
        .setButtonSize(IconButtonSize.L);
  }

  // The #290 case that motivated the epic: a STANDARD dismiss ✕. With the ripple suppressed (#288),
  // the press morph is the only click confirmation it has.
  private static ElwhaIconButton dismissButton() {
    final ElwhaIconButton dismiss =
        new ElwhaIconButton(MaterialIcons.close())
            .setVariant(IconButtonVariant.STANDARD)
            .setButtonSize(IconButtonSize.M);
    dismiss.setToolTipText("Close (dialog dismiss ✕ — #290)");
    return dismiss;
  }

  private static JPanel wrap(final java.awt.Component c) {
    if (c instanceof ElwhaIconButton b) {
      BUTTONS.add(b);
    }
    final JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    p.add(c);
    return p;
  }

  private static void addRowHeader(final JPanel grid, final String text) {
    final JLabel l = new JLabel(text);
    l.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
    grid.add(l);
  }
}
