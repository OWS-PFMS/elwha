package com.owspfm.elwha.dialog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.HeadlessHost;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of {@link ElwhaDialog}'s composition and modal posture: which anatomy slots the
 * builder fills, the action row's fixed cancel → alternate → confirm order, the ripple suppression
 * every dismiss button gets (#288/#290), the width clamp, the scrim-and-surface mount, and the
 * dismiss causes each path reports.
 *
 * <p>Real focus containment — initial focus landing on the confirming action, Tab cycling inside
 * the trap — needs real focus arbitration and lives in the {@code gui} tier.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaDialogCompositionTest {

  private HeadlessHost host;
  private final List<ElwhaDialog> shown = new ArrayList<>();

  @BeforeEach
  void mountAHost() {
    host = new HeadlessHost(900, 700);
  }

  @AfterEach
  void dismissEveryDialog() {
    for (final ElwhaDialog dialog : shown) {
      dialog.dismiss();
    }
    shown.clear();
  }

  private ElwhaDialog show(final ElwhaDialog dialog) {
    shown.add(dialog);
    dialog.show(host.anchor());
    return dialog;
  }

  private static ElwhaDialog.Builder alert() {
    return ElwhaDialog.builder()
        .headline("Discard draft?")
        .supportingText("This cannot be undone.");
  }

  /** Every {@link ElwhaButton} in a rendered surface, in containment order. */
  private static List<ElwhaButton> buttonsIn(final Container root) {
    final List<ElwhaButton> found = new ArrayList<>();
    for (final Component child : root.getComponents()) {
      if (child instanceof ElwhaButton button) {
        found.add(button);
      } else if (child instanceof Container container) {
        found.addAll(buttonsIn(container));
      }
    }
    return found;
  }

  private static List<String> labelsIn(final Container root) {
    final List<String> found = new ArrayList<>();
    for (final Component child : root.getComponents()) {
      if (child instanceof JLabel label) {
        found.add(label.getText());
      } else if (child instanceof Container container) {
        found.addAll(labelsIn(container));
      }
    }
    return found;
  }

  // --------------------------------------------------------------- anatomy

  @Test
  void aBareAlertRendersItsHeadlineAndSupportingText() {
    final JComponent surface = alert().build().renderPreview();

    assertThat(labelsIn(surface))
        .as("both header slots are painted as labels")
        .anySatisfy(text -> assertThat(text).contains("Discard draft?"))
        .anySatisfy(text -> assertThat(text).contains("This cannot be undone."));
  }

  @Test
  void anIconSlotJoinsTheHeader() {
    final JComponent withIcon = alert().icon(MaterialIcons.close(24)).build().renderPreview();

    assertThat(iconLabelCount(withIcon))
        .as("the optional icon is rendered above the headline")
        .isEqualTo(1);
    assertThat(iconLabelCount(alert().build().renderPreview()))
        .as("and is absent when no icon was set")
        .isZero();
  }

  private static int iconLabelCount(final Container root) {
    int count = 0;
    for (final Component child : root.getComponents()) {
      if (child instanceof JLabel label && label.getIcon() != null) {
        count++;
      } else if (child instanceof Container container) {
        count += iconLabelCount(container);
      }
    }
    return count;
  }

  @Test
  void aContentSlotIsHostedWithoutBeingRecolored() {
    final JTextField field = new JTextField("draft");
    final JComponent surface = alert().content(field).build().renderPreview();

    assertThat(descendants(surface)).as("arbitrary content is embedded as-is").contains(field);
  }

  private static List<Component> descendants(final Container root) {
    final List<Component> found = new ArrayList<>();
    for (final Component child : root.getComponents()) {
      found.add(child);
      if (child instanceof Container container) {
        found.addAll(descendants(container));
      }
    }
    return found;
  }

  @Test
  void aDialogWithNoActionsRendersNoActionRow() {
    assertThat(buttonsIn(alert().build().renderPreview()))
        .as("the action row is a slot, not a fixture")
        .isEmpty();
  }

  @Test
  void actionRowIsAlwaysCancelThenAlternateThenConfirm() {
    final ElwhaDialog dialog =
        alert()
            .confirmAction(ElwhaButton.textButton("Discard"))
            .alternateAction(ElwhaButton.textButton("Save draft"))
            .cancelAction(ElwhaButton.textButton("Keep editing"))
            .build();

    assertThat(buttonsIn(dialog.renderPreview()))
        .extracting(ElwhaButton::getText)
        .as("the confirming action is trailing-most regardless of which roles are present")
        .containsExactly("Keep editing", "Save draft", "Discard");
  }

  @Test
  void aPartialActionSetKeepsTheSameRelativeOrder() {
    final ElwhaDialog dialog =
        alert()
            .confirmAction(ElwhaButton.textButton("Discard"))
            .cancelAction(ElwhaButton.textButton("Keep editing"))
            .build();

    assertThat(buttonsIn(dialog.renderPreview()))
        .extracting(ElwhaButton::getText)
        .containsExactly("Keep editing", "Discard");
  }

  @Test
  void everyActionButtonHasItsPressRippleSuppressed() {
    final ElwhaButton confirm = ElwhaButton.textButton("Discard");
    final ElwhaButton cancel = ElwhaButton.textButton("Keep editing");
    assertThat(confirm.isRippleEnabled()).as("a plain button ripples by default").isTrue();

    alert().confirmAction(confirm).cancelAction(cancel).build().renderPreview();

    assertThat(confirm.isRippleEnabled())
        .as("the dialog's exit motion is the press feedback, so the ripple cannot freeze on it")
        .isFalse();
    assertThat(cancel.isRippleEnabled()).isFalse();
  }

  /** Counts the listeners a dialog installs on a caller-supplied action button. */
  private static final class CountingButton extends ElwhaButton {
    private int wired;

    CountingButton(final String label) {
      super(label);
    }

    @Override
    public void addActionListener(final java.awt.event.ActionListener listener) {
      wired++;
      super.addActionListener(listener);
    }
  }

  @Test
  void anActionButtonIsWiredForDismissExactlyOnceForTheDialogsLife() {
    final CountingButton confirm = new CountingButton("Discard");
    final ElwhaDialog dialog = alert().confirmAction(confirm).build();

    show(dialog).dismiss();
    show(dialog).dismiss();
    dialog.renderPreview();

    assertThat(confirm.wired)
        .as("#590 — the surface is rebuilt on every show and preview; the button is wired once")
        .isEqualTo(1);
  }

  @Test
  void aReShownDialogStillClosesFromItsActionButton() {
    final ElwhaButton confirm = ElwhaButton.textButton("Discard");
    final ElwhaDialog dialog = alert().confirmAction(confirm).build();
    show(dialog).dismiss();

    show(dialog);
    confirm.doClick();

    assertThat(host.mounted())
        .as("wiring once must not cost the second presentation its dismiss")
        .isEmpty();
  }

  // ----------------------------------------------------------------- sizing

  @Test
  void aNarrowDialogIsHeldAtTheMinimumBandWidth() {
    final Dimension pref =
        ElwhaDialog.builder().headline("Hi").build().renderPreview().getPreferredSize();

    assertThat(pref.width).as("the M3 basic-dialog band has a floor").isGreaterThanOrEqualTo(280);
  }

  @Test
  void aWordyDialogIsCappedAtTheMaximumBandWidth() {
    final ElwhaDialog dialog =
        ElwhaDialog.builder()
            .headline("Discard this draft and everything written since the last autosave point?")
            .supportingText(
                "Every unsaved change in this document will be lost, including any edits made "
                    + "to nested layers, their groups, and the composition metadata.")
            .build();

    final JComponent surface = dialog.renderPreview();
    final int shadow = surface.getInsets().left + surface.getInsets().right;

    assertThat(surface.getPreferredSize().width - shadow)
        .as("prose wraps rather than pushing the dialog past the band's ceiling")
        .isLessThanOrEqualTo(560);
  }

  // ---------------------------------------------------------- modal posture

  @Test
  void showingMountsAScrimAndASurfaceOnTheDialogBand() {
    final ElwhaDialog dialog = show(alert().build());

    assertThat(host.mounted())
        .as("a modal dialog mounts its scrim as well as its surface")
        .hasSize(2);
    for (final Component mounted : host.mounted()) {
      assertThat(host.layerOf(mounted))
          .as("both sit on the dialog band, above Elwha overlays and below menus")
          .isEqualTo(JLayeredPane.MODAL_LAYER.intValue());
    }
    assertThat(dialog).isNotNull();
  }

  @Test
  void aShownSurfaceIsCenteredInTheHost() {
    show(alert().build());
    final Component surface = host.mounted().get(0);

    assertThat(surface.getX() + surface.getWidth() / 2)
        .as("the dialog is centered horizontally in the host")
        .isEqualTo(900 / 2);
    assertThat(surface.getY() + surface.getHeight() / 2).as("and vertically").isEqualTo(700 / 2);
  }

  @Test
  void aShownSurfaceAnnouncesItselfAsADialogNamedByItsHeadline() {
    show(alert().build());
    final Component surface = host.mounted().get(0);

    assertThat(surface.getAccessibleContext().getAccessibleRole()).isEqualTo(AccessibleRole.DIALOG);
    assertThat(surface.getAccessibleContext().getAccessibleName())
        .as("the headline is the dialog's accessible name")
        .isEqualTo("Discard draft?");
  }

  @Test
  void showingRequiresARealizedHostAndSurvivesADoubleShow() {
    final ElwhaDialog dialog = show(alert().build());

    dialog.show(host.anchor());

    assertThat(host.mounted())
        .as("a second show while up is a no-op, not a second surface")
        .hasSize(2);
    assertThatCode(() -> ElwhaDialog.builder().headline("x").build().show(new JPanel()))
        .as("a parent with no root pane is a caller error")
        .isInstanceOf(IllegalStateException.class);
  }

  // ---------------------------------------------------------- dismiss causes

  @Test
  void firingTheConfirmingActionReportsConfirm() {
    final List<DismissCause> causes = new ArrayList<>();
    final ElwhaButton confirm = ElwhaButton.textButton("Discard");
    show(alert().confirmAction(confirm).onClose(causes::add).build());

    confirm.doClick();

    assertThat(causes).containsExactly(DismissCause.CONFIRM);
    assertThat(host.mounted()).as("and the dialog is gone").isEmpty();
  }

  @Test
  void firingTheCancelActionReportsCancel() {
    final List<DismissCause> causes = new ArrayList<>();
    final ElwhaButton cancel = ElwhaButton.textButton("Keep editing");
    show(alert().cancelAction(cancel).onClose(causes::add).build());

    cancel.doClick();

    assertThat(causes).containsExactly(DismissCause.CANCEL);
  }

  @Test
  void firingTheAlternateActionReportsAlternate() {
    final List<DismissCause> causes = new ArrayList<>();
    final ElwhaButton alternate = ElwhaButton.textButton("Save draft");
    show(alert().alternateAction(alternate).onClose(causes::add).build());

    alternate.doClick();

    assertThat(causes).containsExactly(DismissCause.ALTERNATE);
  }

  @Test
  void aProgrammaticDismissReportsItself() {
    final List<DismissCause> causes = new ArrayList<>();
    final ElwhaDialog dialog = show(alert().onClose(causes::add).build());

    dialog.dismiss();

    assertThat(causes).containsExactly(DismissCause.PROGRAMMATIC);
  }

  @Test
  void aConsumerListenerRunsBeforeTheDialogCloses() {
    final List<String> order = new ArrayList<>();
    final ElwhaButton confirm = ElwhaButton.textButton("Discard");
    confirm.addActionListener(e -> order.add("consumer"));
    show(alert().confirmAction(confirm).onClose(cause -> order.add("closed")).build());

    confirm.doClick();

    assertThat(order)
        .as("the consumer's own handler sees the dialog still up, then the dialog closes")
        .containsExactly("consumer", "closed");
  }

  @Test
  void aDismissedDialogReportsItsCauseOnlyOnce() {
    final List<DismissCause> causes = new ArrayList<>();
    final ElwhaDialog dialog = show(alert().onClose(causes::add).build());

    dialog.dismiss();
    dialog.dismiss();

    assertThat(causes).as("the re-entry guard collapses a double dismiss").hasSize(1);
  }

  @Test
  void dismissingADialogThatWasNeverShownIsInert() {
    final List<DismissCause> causes = new ArrayList<>();

    assertThatCode(() -> alert().onClose(causes::add).build().dismiss()).doesNotThrowAnyException();
    assertThat(causes).isEmpty();
  }

  // ------------------------------------------------------------- key wiring

  @Test
  void escapeIsBoundWindowWideWhenTheDialogAllowsIt() {
    show(alert().build());
    final JComponent surface = (JComponent) host.mounted().get(0);

    assertThat(
            surface
                .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(javax.swing.KeyStroke.getKeyStroke("ESCAPE")))
        .as("Escape reaches the dialog from anywhere in the window it owns")
        .isEqualTo("elwha-dialog-cancel");
  }

  @Test
  void escapeIsUnboundOnADialogThatRefusesIt() {
    show(alert().dismissibleByEsc(false).build());
    final JComponent surface = (JComponent) host.mounted().get(0);

    assertThat(
            surface
                .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(javax.swing.KeyStroke.getKeyStroke("ESCAPE")))
        .as("a required-decision dialog has no Escape hatch")
        .isNull();
  }

  @Test
  void enterIsBoundOnlyWhenThereIsAConfirmingAction() {
    show(alert().confirmAction(ElwhaButton.textButton("Discard")).build());
    final JComponent withConfirm = (JComponent) host.mounted().get(0);

    assertThat(
            withConfirm
                .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(javax.swing.KeyStroke.getKeyStroke("ENTER")))
        .as("Enter is the hand-wired default-button substitute this epic exists to formalize")
        .isEqualTo("elwha-dialog-confirm");
  }

  @Test
  void escapeFiresTheCancelActionSoItsConsumerListenerRuns() {
    final List<String> order = new ArrayList<>();
    final ElwhaButton cancel = ElwhaButton.textButton("Keep editing");
    cancel.addActionListener(e -> order.add("consumer"));
    final List<DismissCause> causes = new ArrayList<>();
    show(alert().cancelAction(cancel).onClose(causes::add).build());
    final JComponent surface = (JComponent) host.mounted().get(0);

    surface
        .getActionMap()
        .get("elwha-dialog-cancel")
        .actionPerformed(new java.awt.event.ActionEvent(surface, 0, "escape"));

    assertThat(order)
        .as("Escape goes through the cancel button, not around it")
        .containsExactly("consumer");
    assertThat(causes)
        .as("so the close reports CANCEL rather than a bare ESC")
        .containsExactly(DismissCause.CANCEL);
  }

  @Test
  void escapeWithNoCancelActionReportsTheBareEscapeCause() {
    final List<DismissCause> causes = new ArrayList<>();
    show(alert().onClose(causes::add).build());
    final JComponent surface = (JComponent) host.mounted().get(0);

    surface
        .getActionMap()
        .get("elwha-dialog-cancel")
        .actionPerformed(new java.awt.event.ActionEvent(surface, 0, "escape"));

    assertThat(causes).containsExactly(DismissCause.ESC);
  }
}
