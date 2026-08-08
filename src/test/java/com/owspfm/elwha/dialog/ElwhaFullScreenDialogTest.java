package com.owspfm.elwha.dialog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.HeadlessHost;
import com.owspfm.elwha.testkit.Input;
import com.owspfm.elwha.testkit.PaintOrigin;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.MorphAnimator;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of {@link ElwhaFullScreenDialog} — the other side of the modality fork. It shares
 * the modal band and dismiss-cause plumbing with the Basic Dialog but differs in exactly the ways
 * the design doc calls out: no scrim (the surface physically covers the app), an edge-to-edge
 * frame-filling placement, and an app bar carrying the close affordance and optional confirming
 * action instead of a trailing action row.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaFullScreenDialogTest {

  private HeadlessHost host;
  private final List<ElwhaFullScreenDialog> shown = new ArrayList<>();

  @BeforeEach
  void mountAHost() {
    host = new HeadlessHost(1000, 800);
  }

  @AfterEach
  void dismissEveryDialog() {
    for (final ElwhaFullScreenDialog dialog : shown) {
      dialog.dismiss();
    }
    shown.clear();
  }

  private ElwhaFullScreenDialog show(final ElwhaFullScreenDialog dialog) {
    shown.add(dialog);
    dialog.show(host.anchor());
    return dialog;
  }

  private static ElwhaFullScreenDialog.Builder form() {
    return ElwhaFullScreenDialog.builder().headline("New event");
  }

  private JComponent surface() {
    return (JComponent) host.mounted().get(0);
  }

  private static <T> List<T> descendantsOfType(final Container root, final Class<T> type) {
    final List<T> found = new ArrayList<>();
    for (final Component child : root.getComponents()) {
      if (type.isInstance(child)) {
        found.add(type.cast(child));
      }
      if (child instanceof Container container) {
        found.addAll(descendantsOfType(container, type));
      }
    }
    return found;
  }

  // ------------------------------------------------------------ paint origin

  @Test
  void aSurfaceIsAPaintingOriginOnlyWhileTheEntranceIsStillRunning() {
    MorphAnimator.setReducedMotion(false);
    show(form().build());
    // Re-pinned before asserting: motionProgress is already latched at 0 (a Swing timer cannot tick
    // while this test holds the dispatch thread), and the restore keeps the teardown synchronous.
    MorphAnimator.setReducedMotion(true);

    assertThat(PaintOrigin.of(surface()))
        .as("mid-slide the surface offsets a snapshot, so children must repaint through it")
        .isTrue();
  }

  @Test
  void aSettledSurfaceIsNotAPaintingOrigin() {
    show(form().build());

    assertThat(PaintOrigin.of(surface()))
        .as("at rest paint() offsets nothing, so a caret blink need not re-composite the dialog")
        .isFalse();
  }

  // ------------------------------------------------------------ modal posture

  @Test
  void aFullScreenDialogMountsWithoutAScrim() {
    show(form().build());

    assertThat(host.mounted())
        .as("the surface covers the app itself, so there is nothing for a scrim to do")
        .hasSize(1);
    assertThat(host.layerOf(surface()))
        .as("it still shares the dialog band with the basic dialog")
        .isEqualTo(JLayeredPane.MODAL_LAYER.intValue());
  }

  @Test
  void aShownSurfaceFillsTheHostEdgeToEdge() {
    show(form().build());

    assertThat(surface().getBounds())
        .as("a full-screen dialog takes the whole frame")
        .isEqualTo(new Rectangle(0, 0, 1000, 800));
  }

  @Test
  void aHostResizeKeepsTheSurfaceEdgeToEdge() {
    final ElwhaFullScreenDialog dialog = show(form().build());

    host.resizeAndNotify(700, 500);

    assertThat(surface().getBounds())
        .as("the host's resize notification re-places the surface without a fresh show")
        .isEqualTo(new Rectangle(0, 0, 700, 500));
    assertThat(dialog).isNotNull();
  }

  @Test
  void aShownSurfaceAnnouncesItselfAsADialogNamedByItsHeadline() {
    show(form().build());

    assertThat(surface().getAccessibleContext().getAccessibleRole())
        .isEqualTo(AccessibleRole.DIALOG);
    assertThat(surface().getAccessibleContext().getAccessibleName()).isEqualTo("New event");
  }

  // ---------------------------------------------------------------- app bar

  @Test
  void anAppBarAlwaysCarriesACloseAffordance() {
    final JComponent preview = form().build().renderPreview();

    assertThat(descendantsOfType(preview, ElwhaIconButton.class))
        .as("the leading ✕ is dialog-owned, not a consumer slot")
        .hasSize(1);
  }

  @Test
  void aCloseAffordanceHasItsPressRippleSuppressed() {
    final JComponent preview = form().build().renderPreview();

    assertThat(descendantsOfType(preview, ElwhaIconButton.class).get(0).isRippleEnabled())
        .as("the exit motion is the feedback; a live ripple would freeze on the snapshot")
        .isFalse();
  }

  @Test
  void aConfirmingActionJoinsTheAppBarWithItsRippleSuppressed() {
    final ElwhaButton save = ElwhaButton.textButton("Save");

    final JComponent preview = form().confirmAction(save).build().renderPreview();

    assertThat(descendantsOfType(preview, ElwhaButton.class))
        .as("the confirming action rides in the app bar, not a bottom row")
        .contains(save);
    assertThat(save.isRippleEnabled()).isFalse();
  }

  @Test
  void anAppBarWithNoConfirmingActionIsCloseAndHeadlineOnly() {
    final JComponent preview = form().build().renderPreview();

    assertThat(descendantsOfType(preview, ElwhaButton.class))
        .as("the confirming action is optional")
        .isEmpty();
  }

  @Test
  void contentIsHostedWithoutBeingRecolored() {
    final JTextField field = new JTextField();

    final JComponent preview = form().content(field).build().renderPreview();

    assertThat(descendantsOfType(preview, JTextField.class)).contains(field);
  }

  // --------------------------------------------------------- dismiss causes

  @Test
  void aCloseAffordanceDismissesWithCancel() {
    final List<DismissCause> causes = new ArrayList<>();
    show(form().onClose(causes::add).build());

    Input.pressBoundKey(
        descendantsOfType(surface(), ElwhaIconButton.class).get(0),
        "pressed SPACE",
        "elwhaiconbutton.activate");

    assertThat(causes)
        .as("closing a full-screen flow without confirming is a cancel")
        .containsExactly(DismissCause.CANCEL);
    assertThat(host.mounted()).isEmpty();
  }

  @Test
  void aConfirmingActionDismissesWithConfirm() {
    final List<DismissCause> causes = new ArrayList<>();
    final ElwhaButton save = ElwhaButton.textButton("Save");
    show(form().confirmAction(save).onClose(causes::add).build());

    save.doClick();

    assertThat(causes).containsExactly(DismissCause.CONFIRM);
  }

  @Test
  void aConfirmListenerIsWiredOnceEvenAcrossRepeatedShows() {
    final List<DismissCause> causes = new ArrayList<>();
    final ElwhaButton save = ElwhaButton.textButton("Save");
    final ElwhaFullScreenDialog dialog = form().confirmAction(save).onClose(causes::add).build();

    show(dialog);
    save.doClick();
    show(dialog);
    save.doClick();

    assertThat(causes)
        .as("the close listener is wired at construction, so repeated shows never stack it")
        .containsExactly(DismissCause.CONFIRM, DismissCause.CONFIRM);
  }

  @Test
  void aProgrammaticDismissReportsItself() {
    final List<DismissCause> causes = new ArrayList<>();
    final ElwhaFullScreenDialog dialog = show(form().onClose(causes::add).build());

    dialog.dismiss();

    assertThat(causes).containsExactly(DismissCause.PROGRAMMATIC);
  }

  @Test
  void dismissingADialogThatWasNeverShownIsInert() {
    assertThatCode(() -> form().build().dismiss()).doesNotThrowAnyException();
  }

  // ------------------------------------------------------------- key wiring

  @Test
  void escapeCancelsWhenTheDialogAllowsIt() {
    final List<DismissCause> causes = new ArrayList<>();
    show(form().onClose(causes::add).build());

    assertThat(
            surface()
                .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(KeyStroke.getKeyStroke("ESCAPE")))
        .isEqualTo("elwha-fsd-cancel");

    surface()
        .getActionMap()
        .get("elwha-fsd-cancel")
        .actionPerformed(new ActionEvent(surface(), 0, "escape"));

    assertThat(causes)
        .as("Escape out of a full-screen flow abandons it")
        .containsExactly(DismissCause.CANCEL);
  }

  @Test
  void escapeIsUnboundOnADialogThatRefusesIt() {
    show(form().dismissibleByEsc(false).build());

    assertThat(
            surface()
                .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(KeyStroke.getKeyStroke("ESCAPE")))
        .isNull();
  }

  @Test
  void enterIsBoundOnlyWhenThereIsAConfirmingAction() {
    show(form().build());
    assertThat(
            surface()
                .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(KeyStroke.getKeyStroke("ENTER")))
        .as("there is nothing for Enter to confirm without a confirming action")
        .isNull();

    show(form().confirmAction(ElwhaButton.textButton("Save")).build());
    assertThat(
            surface()
                .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(KeyStroke.getKeyStroke("ENTER")))
        .isEqualTo("elwha-fsd-confirm");
  }

  // ---------------------------------------------------------- content column

  @Test
  void aContentColumnDefaultsToTheReadableFormWidth() {
    final JTextField field = new JTextField();
    show(form().content(field).build());

    final Rectangle column = columnBounds(field);

    assertThat(column.width)
        .as("a wide frame still holds the form column at the M3 readable width")
        .isLessThanOrEqualTo(ElwhaFullScreenDialog.CONTENT_COLUMN_PX);
    assertThat(column.x + column.width / 2)
        .as("and centers it in the frame")
        .isBetween(1000 / 2 - 2, 1000 / 2 + 2);
  }

  @Test
  void aRaisedContentMaxWidthLetsTheColumnGrow() {
    final JTextField field = new JTextField();
    show(form().content(field).contentMaxWidth(Integer.MAX_VALUE).build());

    assertThat(columnBounds(field).width)
        .as("wide content like a table can opt out of the readable-width cap")
        .isGreaterThan(ElwhaFullScreenDialog.CONTENT_COLUMN_PX);
  }

  /** The bounds of the centered column that hosts the given content, in surface coordinates. */
  private Rectangle columnBounds(final Component content) {
    surface().doLayout();
    layoutTree(surface());
    Component walk = content;
    while (walk.getParent() != null && walk.getParent().getParent() != surface()) {
      walk = walk.getParent();
    }
    return javax.swing.SwingUtilities.convertRectangle(
        walk.getParent(), walk.getBounds(), surface());
  }

  private static void layoutTree(final Container root) {
    root.doLayout();
    for (final Component child : root.getComponents()) {
      if (child instanceof Container container) {
        layoutTree(container);
      }
    }
  }
}
