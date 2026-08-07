package com.owspfm.elwha.textfield;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.textfield.ElwhaTextField.InputMode;
import com.owspfm.elwha.textfield.ElwhaTextField.Variant;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.ComponentOrientation;
import java.awt.Rectangle;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tier A measurement coverage for {@link ElwhaTextField} — the §5 redlines (56dp container, 8dp
 * top/bottom, 16dp / 12dp edge padding, the 52dp icon slot, the reserved supporting row), the two
 * variants' differing vertical anchoring, the multi-line growth rules of the S6 build outcome, and
 * the RTL mirroring of every horizontal slot.
 *
 * <p>Assertions read the laid-out editor bounds and the derived preferred size — no glyph pixels
 * and no hardcoded font metrics: line heights come from the same {@code FontMetrics} the component
 * measures with, so the expectations hold on any host font stack.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaTextFieldGeometryTest {

  private static int lineHeight(final ElwhaTextField field, final TypeRole role) {
    return field.getFontMetrics(role.resolve()).getHeight();
  }

  private static Rectangle editorBounds(final ElwhaTextField field) {
    field.setSize(field.getPreferredSize());
    field.doLayout();
    return editorHost(field).getBounds();
  }

  /** The editor, or the scroll pane hosting it in the fixed text-area mode. */
  private static java.awt.Component editorHost(final ElwhaTextField field) {
    final java.awt.Component parent = field.getEditor().getParent();
    return parent instanceof javax.swing.JViewport ? field.getComponent(0) : field.getEditor();
  }

  // ------------------------------------------------------- preferred size

  @ParameterizedTest
  @EnumSource(Variant.class)
  void thePreferredSizeIsTheContainerPlusTheAlwaysReservedSupportingRow(final Variant variant) {
    final ElwhaTextField field = new ElwhaTextField(variant, "Label");

    final int containerTop =
        variant == Variant.OUTLINED ? lineHeight(field, TypeRole.BODY_SMALL) / 2 : 0;
    final int supportingRow =
        ElwhaTextField.SUPPORTING_TOP_PAD + lineHeight(field, TypeRole.BODY_SMALL);
    assertThat(field.getPreferredSize().height)
        .as("%s stacks the container box and the reserved supporting row", variant)
        .isEqualTo(containerTop + ElwhaTextField.CONTAINER_HEIGHT + supportingRow);
    assertThat(field.getPreferredSize().width)
        .as("and opens at the M3 default layout width")
        .isEqualTo(ElwhaTextField.DEFAULT_WIDTH);
  }

  @Test
  void theOutlinedVariantReservesABandAboveItsStrokeForTheFloatedLabel() {
    final ElwhaTextField filled = new ElwhaTextField(Variant.FILLED, "Label");
    final ElwhaTextField outlined = new ElwhaTextField(Variant.OUTLINED, "Label");

    assertThat(outlined.getPreferredSize().height - filled.getPreferredSize().height)
        .as("outlined is taller by the half-line the notched label straddles")
        .isEqualTo(lineHeight(outlined, TypeRole.BODY_SMALL) / 2);
  }

  @Test
  void theSupportingRowIsReservedEvenWithNoSupportingContent() {
    final ElwhaTextField bare = new ElwhaTextField(Variant.FILLED, "Label");
    final ElwhaTextField supported = new ElwhaTextField(Variant.FILLED, "Label");
    supported.setSupportingText("Helper text");
    final ElwhaTextField errored = new ElwhaTextField(Variant.FILLED, "Label");
    errored.setError(true);
    errored.setErrorText("Required");

    assertThat(supported.getPreferredSize())
        .as("adding supporting text never changes the height — the row was already reserved")
        .isEqualTo(bare.getPreferredSize());
    assertThat(errored.getPreferredSize())
        .as("nor does swapping in error text — §6's no-layout-shift rule")
        .isEqualTo(bare.getPreferredSize());
  }

  // ------------------------------------------------------- editor placement

  @Test
  void theFilledEditorSitsInTheLowerBandSoTheFloatedLabelClearsIt() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Label");

    final Rectangle bounds = editorBounds(field);
    assertThat(bounds.y + bounds.height)
        .as("the filled editor's bottom sits one padding step above the container's bottom")
        .isEqualTo(ElwhaTextField.CONTAINER_HEIGHT - ElwhaTextField.PAD_TOP_BOTTOM);
  }

  @Test
  void theOutlinedEditorIsVerticallyCenteredInItsContainer() {
    final ElwhaTextField field = new ElwhaTextField(Variant.OUTLINED, "Label");

    final Rectangle bounds = editorBounds(field);
    final int containerTop = lineHeight(field, TypeRole.BODY_SMALL) / 2;
    final int topGap = bounds.y - containerTop;
    final int bottomGap = containerTop + ElwhaTextField.CONTAINER_HEIGHT - bounds.y - bounds.height;
    assertThat(topGap)
        .as("the outlined label straddles the stroke, so the editor centers in the box")
        .isEqualTo(bottomGap);
  }

  @ParameterizedTest
  @EnumSource(Variant.class)
  void aBareFieldIndentsTheEditorByTheNoIconPadding(final Variant variant) {
    final ElwhaTextField field = new ElwhaTextField(variant, "Label");

    final Rectangle bounds = editorBounds(field);
    assertThat(bounds.x)
        .as("%s indents 16dp with no icon slot", variant)
        .isEqualTo(ElwhaTextField.PAD_LR_NO_ICON);
    assertThat(field.getPreferredSize().width - bounds.x - bounds.width)
        .as("and the same on the trailing side")
        .isEqualTo(ElwhaTextField.PAD_LR_NO_ICON);
  }

  @Test
  void aLeadingIconPushesTheEditorPastTheFiftyTwoPixelSlot() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Label");
    field.setLeadingIcon(MaterialIcons.info(ElwhaTextField.ICON_GLYPH));

    assertThat(editorBounds(field).x)
        .as("12dp edge pad + 24dp glyph + 16dp gap = the 52dp icon slot")
        .isEqualTo(ElwhaTextField.ICON_SLOT);
  }

  @Test
  void aTrailingIconPullsTheEditorsRightEdgeIn() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Label");
    field.setTrailingIcon(MaterialIcons.close(ElwhaTextField.ICON_GLYPH));

    final Rectangle bounds = editorBounds(field);
    assertThat(field.getPreferredSize().width - bounds.x - bounds.width)
        .as("the trailing slot claims 52dp off the right edge")
        .isEqualTo(ElwhaTextField.ICON_SLOT);
    assertThat(bounds.x)
        .as("while the leading edge keeps the bare padding")
        .isEqualTo(ElwhaTextField.PAD_LR_NO_ICON);
  }

  @Test
  void theAutoErrorIconClaimsTheTrailingSlotOnlyWhileNoConsumerIconIsSet() {
    final ElwhaTextField auto = new ElwhaTextField(Variant.FILLED, "Label");
    auto.setError(true);
    assertThat(auto.getPreferredSize().width - rightEdgeGap(auto))
        .as("an errored field with an empty trailing slot auto-fills the error glyph")
        .isEqualTo(auto.getPreferredSize().width - ElwhaTextField.ICON_SLOT);

    final ElwhaTextField explicit = new ElwhaTextField(Variant.FILLED, "Label");
    explicit.setError(true);
    explicit.setTrailingIcon(MaterialIcons.close(ElwhaTextField.ICON_GLYPH));
    assertThat(rightEdgeGap(explicit))
        .as("a consumer trailing icon occupies the same slot rather than stacking")
        .isEqualTo(ElwhaTextField.ICON_SLOT);
  }

  private static int rightEdgeGap(final ElwhaTextField field) {
    final Rectangle bounds = editorBounds(field);
    return field.getPreferredSize().width - bounds.x - bounds.width;
  }

  @Test
  void prefixAndSuffixEatIntoTheEditorFromEitherSide() {
    final ElwhaTextField bare = new ElwhaTextField(Variant.FILLED, "Label");
    final Rectangle bareBounds = editorBounds(bare);

    final ElwhaTextField affixed = new ElwhaTextField(Variant.FILLED, "Label");
    affixed.setPrefixText("$");
    affixed.setSuffixText("kg");
    final Rectangle bounds = editorBounds(affixed);

    final int gap = ElwhaTextField.ICON_TEXT_GAP / 2;
    final int prefixWidth =
        affixed.getFontMetrics(TypeRole.BODY_LARGE.resolve()).stringWidth("$") + gap;
    final int suffixWidth =
        affixed.getFontMetrics(TypeRole.BODY_LARGE.resolve()).stringWidth("kg") + gap;
    assertThat(bounds.x)
        .as("the prefix is measured in BODY_LARGE and reserved ahead of the input")
        .isEqualTo(bareBounds.x + prefixWidth);
    assertThat(bounds.width)
        .as("and the suffix is reserved after it")
        .isEqualTo(bareBounds.width - prefixWidth - suffixWidth);
  }

  // ---------------------------------------------------------------- RTL

  @Test
  void rightToLeftMirrorsTheIconSlots() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Label");
    field.setLeadingIcon(MaterialIcons.info(ElwhaTextField.ICON_GLYPH));
    field.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    final Rectangle bounds = editorBounds(field);
    assertThat(bounds.x)
        .as("under RTL the leading icon moves to the right, freeing the left edge")
        .isEqualTo(ElwhaTextField.PAD_LR_NO_ICON);
    assertThat(field.getPreferredSize().width - bounds.x - bounds.width)
        .as("and claims the 52dp slot on the right instead")
        .isEqualTo(ElwhaTextField.ICON_SLOT);
  }

  @Test
  void rightToLeftMirrorsTheAffixes() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Label");
    field.setPrefixText("$");
    field.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    final Rectangle bounds = editorBounds(field);
    final int prefixWidth =
        field.getFontMetrics(TypeRole.BODY_LARGE.resolve()).stringWidth("$")
            + ElwhaTextField.ICON_TEXT_GAP / 2;
    assertThat(field.getPreferredSize().width - bounds.x - bounds.width)
        .as("the prefix leads on the right under RTL")
        .isEqualTo(ElwhaTextField.PAD_LR_NO_ICON + prefixWidth);
  }

  @Test
  void theTrailingButtonCentersItsGlyphWhereAStaticIconWouldSit() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Label");
    final ElwhaIconButton button =
        new ElwhaIconButton(MaterialIcons.close(ElwhaTextField.ICON_GLYPH));
    field.setTrailingIconButton(button);
    field.setSize(field.getPreferredSize());
    field.doLayout();

    final int glyphCenterFromEdge = ElwhaTextField.PAD_LR_ICON + ElwhaTextField.ICON_GLYPH / 2;
    assertThat(button.getX() + button.getWidth() / 2)
        .as("the button's centre lands on the static icon's centre, whatever its own size")
        .isEqualTo(field.getWidth() - glyphCenterFromEdge);
    assertThat(button.getY() + button.getHeight() / 2)
        .as("and it centres vertically in the 56dp container")
        .isEqualTo(ElwhaTextField.CONTAINER_HEIGHT / 2);
  }

  @Test
  void settingATrailingButtonClearsAStaticTrailingIconAndViceVersa() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Label");
    field.setTrailingIcon(MaterialIcons.close(ElwhaTextField.ICON_GLYPH));
    final ElwhaIconButton button =
        new ElwhaIconButton(MaterialIcons.close(ElwhaTextField.ICON_GLYPH));

    field.setTrailingIconButton(button);
    assertThat(field.getTrailingIcon()).as("the button evicts the static icon").isNull();
    assertThat(field.getTrailingIconButton()).as("and takes the slot").isSameAs(button);

    field.setTrailingIcon(MaterialIcons.close(ElwhaTextField.ICON_GLYPH));
    assertThat(field.getTrailingIconButton()).as("a static icon evicts the button").isNull();
    assertThat(java.util.Arrays.asList(field.getComponents()))
        .as("and the button is removed from the hierarchy, not merely forgotten")
        .doesNotContain(button);
  }

  // ------------------------------------------------------------- multi-line

  @Test
  void switchingToMultiLineSwapsTheEditorForAWrappingTextArea() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Notes");
    field.setText("carried across");
    assertThat(field.getEditor())
        .as("single-line is backed by a JTextField")
        .isInstanceOf(JTextField.class);

    field.setInputMode(InputMode.MULTI_LINE);

    assertThat(field.getEditor())
        .as("multi-line swaps in a JTextArea")
        .isInstanceOf(JTextArea.class);
    assertThat(((JTextArea) field.getEditor()).getLineWrap())
        .as("that wraps rather than scrolling horizontally")
        .isTrue();
    assertThat(field.getText()).as("and the swap preserves the text").isEqualTo("carried across");
    assertThat(field.isMultiline()).as("the predicate agrees").isTrue();
  }

  @Test
  void theEditorSwapPreservesEnabledAndReadOnlyState() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Notes");
    field.setEnabled(false);
    field.setReadOnly(true);

    field.setInputMode(InputMode.MULTI_LINE);

    assertThat(field.getEditor().isEnabled()).as("the rebuilt editor stays disabled").isFalse();
    assertThat(field.isReadOnly()).as("and stays read-only").isTrue();
  }

  @Test
  void theTextAreaModeHostsTheEditorInAScrollPaneAtItsRowCount() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Notes");
    field.setInputMode(InputMode.TEXT_AREA);
    field.setRows(4);

    field.setSize(field.getPreferredSize());
    field.doLayout();

    assertThat(field.getEditor().getParent().getParent())
        .as("a fixed text area scrolls internally, so the editor lives in a scroll pane")
        .isInstanceOf(JScrollPane.class);
    assertThat(editorHost(field).getHeight())
        .as("and opens at rows x line height")
        .isEqualTo(4 * lineHeight(field, TypeRole.BODY_LARGE));
  }

  @Test
  void aRowCountBelowOneIsClamped() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Notes");

    field.setRows(0);

    assertThat(field.getRows()).as("a text area always shows at least one row").isEqualTo(1);
  }

  @Test
  void multiLineGrowsTheContainerAsContentWraps() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Notes");
    field.setInputMode(InputMode.MULTI_LINE);
    field.setSize(field.getPreferredSize());
    final int oneLine = field.getPreferredSize().height;

    field.setText("wrap ".repeat(80));
    field.doLayout();

    assertThat(field.getPreferredSize().height)
        .as("auto-grow follows the wrapped content height rather than scrolling")
        .isGreaterThan(oneLine);
  }

  @Test
  void multiLineNeverShrinksBelowASingleLine() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Notes");
    field.setInputMode(InputMode.MULTI_LINE);
    field.setSize(field.getPreferredSize());

    assertThat(editorBounds(field).height)
        .as("an empty auto-grow area still reserves one line")
        .isGreaterThanOrEqualTo(lineHeight(field, TypeRole.BODY_LARGE));
  }

  @ParameterizedTest
  @EnumSource(Variant.class)
  void multiLineTopAnchorsTheEditorBelowTheFloatedLabelBand(final Variant variant) {
    final ElwhaTextField field = new ElwhaTextField(variant, "Notes");
    field.setInputMode(InputMode.MULTI_LINE);

    final int containerTop =
        variant == Variant.OUTLINED ? lineHeight(field, TypeRole.BODY_SMALL) / 2 : 0;
    final int labelBand = variant == Variant.FILLED ? lineHeight(field, TypeRole.BODY_SMALL) : 0;
    assertThat(editorBounds(field).y)
        .as(
            "%s top-anchors the editor; only the filled variant reserves a label row inside its"
                + " fill",
            variant)
        .isEqualTo(containerTop + ElwhaTextField.PAD_TOP_BOTTOM + labelBand);
  }

  @Test
  void theMultiLineTextAreaKeepsTabAsATraversalKeyNotAnEditingKey() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Notes");
    field.setInputMode(InputMode.MULTI_LINE);

    final JTextArea area = (JTextArea) field.getEditor();
    assertThat(area.getFocusTraversalKeys(java.awt.KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS))
        .as("Tab moves focus out of a form field — a text area is not a code editor")
        .isNotEmpty();
  }

  @Test
  void switchingBackToSingleLineRestoresATextField() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Notes");
    field.setInputMode(InputMode.TEXT_AREA);
    field.setText("kept");

    field.setInputMode(InputMode.SINGLE_LINE);

    assertThat(field.getEditor())
        .as("the editor reverts to a JTextField")
        .isInstanceOf(JTextField.class);
    assertThat(field.getEditor().getParent())
        .as("and is hosted bare again, with the scroll pane discarded")
        .isSameAs(field);
    assertThat(field.getText()).as("preserving the text across both swaps").isEqualTo("kept");
  }

  @Test
  void aNullInputModeFallsBackToSingleLine() {
    final ElwhaTextField field = new ElwhaTextField(Variant.FILLED, "Notes");
    field.setInputMode(InputMode.MULTI_LINE);

    field.setInputMode(null);

    assertThat(field.getInputMode())
        .as("a null mode is treated as SINGLE_LINE")
        .isEqualTo(InputMode.SINGLE_LINE);
  }
}
