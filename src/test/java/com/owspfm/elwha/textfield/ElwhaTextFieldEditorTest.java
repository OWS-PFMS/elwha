package com.owspfm.elwha.textfield;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.textfield.ElwhaTextField.InputMode;
import com.owspfm.elwha.textfield.ElwhaTextField.Variant;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.Color;
import java.awt.IllegalComponentStateException;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.UIResource;
import javax.swing.text.JTextComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A regression coverage for the embedded editor's theming — <b>the #495 / #496 stale-editor-
 * colors bug</b> and the decorator seams around it.
 *
 * <p>#495 had two independent halves, and this class pins both:
 *
 * <ul>
 *   <li><b>Detached mounts.</b> {@code ElwhaTheme.install} refreshes live UI by walking {@code
 *       Window.getWindows()}, so a field constructed while detached — an unopened dialog's content,
 *       a lazily-mounted pane, the color picker's hex field — is never visited by a theme switch
 *       and would show the previous mode's text color when it finally mounted. {@code addNotify}
 *       re-resolving the editor is the fix.
 *   <li><b>The UIResource caret clobber.</b> {@code updateComponentTreeUI} walks parent-first, so
 *       the editor's own {@code updateUI} runs <em>after</em> the field's; the text UI's {@code
 *       installDefaults} overwrites any color that is null or a {@code UIResource}. Theme resolves
 *       hand back {@code ColorUIResource}, so storing them directly would let the LAF default
 *       silently replace the M3 caret on every switch. Writing plain colors is the fix, and it is
 *       invisible unless a test asserts the <em>type</em>.
 * </ul>
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaTextFieldEditorTest {

  // -------------------------------------------- the UIResource caret clobber

  @Test
  void editorColorsArePlainSoTheTextUiCannotClobberThem() {
    final JTextComponent editor = new ElwhaTextField(Variant.FILLED, "Email").getEditor();

    assertThat(editor.getForeground())
        .as("a UIResource foreground would be replaced by the LAF default on the editor's updateUI")
        .isNotInstanceOf(UIResource.class);
    assertThat(editor.getCaretColor())
        .as("and a UIResource caret is exactly how #495 lost the M3 caret stroke")
        .isNotInstanceOf(UIResource.class);
    assertThat(editor.getFont())
        .as("the font is derived rather than stored as a FontUIResource, for the same reason")
        .isNotInstanceOf(UIResource.class);
  }

  @Test
  void caretKeepsTheM3StrokeAcrossATreeUiUpdate() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    field.addNotify();

    ThemeExtension.install(Mode.DARK);
    SwingUtilities.updateComponentTreeUI(field);

    assertThat(field.getEditor().getCaretColor())
        .as("the caret re-resolves to primary in the new mode instead of the LAF default")
        .isEqualTo(ColorRole.PRIMARY.resolve());
  }

  @Test
  void caretFollowsTheErrorState() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    assertThat(field.getEditor().getCaretColor())
        .as("a resting caret matches the focus indicator")
        .isEqualTo(ColorRole.PRIMARY.resolve());

    field.setError(true);
    assertThat(field.getEditor().getCaretColor())
        .as("error recolors the caret with the rest of the chrome")
        .isEqualTo(ColorRole.ERROR.resolve());

    field.setError(false);
    assertThat(field.getEditor().getCaretColor())
        .as("and clearing it restores primary")
        .isEqualTo(ColorRole.PRIMARY.resolve());
  }

  // ------------------------------------------------------- detached mounts

  @Test
  void aFieldBuiltWhileDetachedPicksUpTheCurrentThemeWhenItMounts() {
    final Color lightText = ColorRole.ON_SURFACE.resolve();
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Hex");

    ThemeExtension.install(Mode.DARK);

    assertThat(field.getEditor().getForeground())
        .as("a theme switch cannot reach a detached field — this is #495's precondition")
        .isEqualTo(lightText);

    field.addNotify();

    assertThat(field.getEditor().getForeground())
        .as("mounting re-resolves the editor under the theme that is actually installed")
        .isEqualTo(ColorRole.ON_SURFACE.resolve());
    assertThat(field.getEditor().getForeground())
        .as("which is a different color than it was built with")
        .isNotEqualTo(lightText);
  }

  @Test
  void anUnmountedAndRemountedFieldReResolvesEveryTime() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Hex");
    field.addNotify();
    field.removeNotify();

    ThemeExtension.install(Mode.DARK);
    field.addNotify();
    assertThat(field.getEditor().getForeground())
        .as("a remount after a switch re-resolves, not only the first mount")
        .isEqualTo(ColorRole.ON_SURFACE.resolve());

    field.removeNotify();
    ThemeExtension.install(Mode.LIGHT);
    field.addNotify();
    assertThat(field.getEditor().getForeground())
        .as("and it tracks the theme back the other way")
        .isEqualTo(ColorRole.ON_SURFACE.resolve());
  }

  @Test
  void caretIsReResolvedOnMountToo() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Hex");
    final Color lightCaret = ColorRole.PRIMARY.resolve();

    ThemeExtension.install(Mode.DARK);
    field.addNotify();

    assertThat(field.getEditor().getCaretColor())
        .as("the caret is part of the same re-resolve — it was the half #495 shipped broken")
        .isEqualTo(ColorRole.PRIMARY.resolve());
    assertThat(field.getEditor().getCaretColor())
        .as("and genuinely changed with the mode")
        .isNotEqualTo(lightCaret);
  }

  @ParameterizedTest
  @EnumSource(
      value = Mode.class,
      names = {"LIGHT", "DARK"})
  void editorTextResolvesOnSurfaceInEitherMode(final Mode mode) {
    ThemeExtension.install(mode);

    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");

    assertThat(field.getEditor().getForeground())
        .as("input text is onSurface in " + mode)
        .isEqualTo(ColorRole.ON_SURFACE.resolve());
    assertThat(field.getEditor().getFont().getSize2D())
        .as("and is set in the BODY_LARGE scale")
        .isEqualTo((float) TypeRole.BODY_LARGE.pt());
  }

  @Test
  void aRebuiltEditorIsStyledLikeTheOneItReplaced() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Notes");

    field.setInputMode(InputMode.MULTI_LINE);

    assertThat(field.getEditor().getForeground())
        .as("the swapped-in text area is themed, not left at the LAF default")
        .isEqualTo(ColorRole.ON_SURFACE.resolve());
    assertThat(field.getEditor().getCaretColor())
        .as("caret included")
        .isEqualTo(ColorRole.PRIMARY.resolve());
    assertThat(field.getEditor().isOpaque())
        .as("and stays transparent so the field's own chrome shows through")
        .isFalse();
  }

  // ------------------------------------------------- decorator plumbing

  @Test
  void editorIsTheOnlyChildOfABareField() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");

    assertThat(field.getComponents())
        .as("the S1-locked decorator boundary: one embedded editor, chrome painted around it")
        .containsExactly(field.getEditor());
  }

  @Test
  void documentChangesReachConsumerListenersThroughTheExposedEditor() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    final int[] inserts = {0};
    final int[] removes = {0};
    field
        .getEditor()
        .getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void insertUpdate(final DocumentEvent e) {
                inserts[0]++;
              }

              @Override
              public void removeUpdate(final DocumentEvent e) {
                removes[0]++;
              }

              @Override
              public void changedUpdate(final DocumentEvent e) {
                // Plain documents never fire attribute changes.
              }
            });

    field.setText("abc");
    assertThat(inserts[0]).as("setText inserts through the real document").isEqualTo(1);

    field.setText("");
    assertThat(removes[0]).as("and clearing removes through it").isEqualTo(1);
  }

  @Test
  void setTextAndGetTextRoundTripThroughTheEditor() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");

    field.setText("ada@x.io");

    assertThat(field.getText()).as("the field reads its editor").isEqualTo("ada@x.io");
    assertThat(field.getEditor().getText())
        .as("and the editor is the single source of truth")
        .isEqualTo("ada@x.io");
  }

  @Test
  void disablingTheFieldDisablesTheEditorWithIt() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");

    field.setEnabled(false);

    assertThat(field.isEnabled()).as("the decorator is disabled").isFalse();
    assertThat(field.getEditor().isEnabled())
        .as("and so is the editor — otherwise it would still take input")
        .isFalse();
  }

  @Test
  void disablingTheFieldReachesTheTrailingIconButton() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Password");
    final ElwhaIconButton reveal = new ElwhaIconButton();
    field.setTrailingIconButton(reveal);

    field.setEnabled(false);

    assertThat(reveal.isEnabled())
        .as(
            "Container.setEnabled does not cascade, so a disabled field's clear / show-password"
                + " button would otherwise stay clickable and keep painting its own states")
        .isFalse();

    field.setEnabled(true);

    assertThat(reveal.isEnabled()).as("and it comes back with the field").isTrue();
  }

  @Test
  void readOnlyLeavesTheFieldEnabledButUneditable() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");

    field.setReadOnly(true);

    assertThat(field.getEditor().isEditable()).as("read-only is setEditable(false)").isFalse();
    assertThat(field.getEditor().isEnabled())
        .as("not setEnabled(false) — the text stays selectable and copyable")
        .isTrue();
    assertThat(field.isReadOnly()).as("and the field reports it").isTrue();

    field.setReadOnly(false);
    assertThat(field.isReadOnly()).as("which round-trips").isFalse();
  }

  // ------------------------------------- the peered-orphan accessibility trap

  /**
   * #679 — {@code addNotify()} on an unparented field leaves it in a state Swing answers
   * inconsistently: {@code isShowing()} is true because the peer exists and no parent objects, but
   * there is no window to measure a screen location against. {@code
   * AccessibleJTextComponent.caretUpdate} asks for one on every caret move, and the JDK guards that
   * ask with {@code catch (IllegalComponentStateException)} — a handler it never reaches, because
   * {@code Component.getLocationOnScreen} takes its {@code isShowing()} branch and NPEs walking to
   * the absent window. Elwha arms this on every field, not just an accessibility-aware one, because
   * the constructor realizes the editor's accessible context.
   */
  @Test
  void anUnparentedFieldSurvivesAddNotifyThenSetText() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");

    field.addNotify();

    assertThatCode(() -> field.setText("hello"))
        .as("an offscreen render path peers without parenting; setText must not NPE there")
        .doesNotThrowAnyException();
    assertThat(field.getText()).as("and the text actually lands").isEqualTo("hello");
  }

  @Test
  void everyEditorModeSurvivesThePeeredOrphanState() {
    for (final InputMode mode : InputMode.values()) {
      final ElwhaTextField field = new ElwhaTextField(Variant.OUTLINED, "Notes");
      field.setInputMode(mode);

      field.addNotify();

      assertThatCode(() -> field.setText("x"))
          .as("the guard rides on the editor, so it must cover the JTextArea modes too: " + mode)
          .doesNotThrowAnyException();
    }
  }

  @Test
  void theWindowlessEditorAnswersWithTheExceptionSwingAlreadyCatches() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Email");
    field.addNotify();

    assertThatThrownBy(() -> field.getEditor().getLocationOnScreen())
        .as(
            "Component.getLocationOnScreen specifies this for a component not showing on screen,"
                + " and it is the exception AccessibleJTextComponent.caretUpdate already handles")
        .isInstanceOf(IllegalComponentStateException.class);
  }
}
