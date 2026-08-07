package com.owspfm.elwha.showcase;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The smaller shared pieces every workbench is assembled from: the controls column's labelled-row
 * grid, the code view, and the container workbench's live region plus event log.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class WorkbenchScaffoldTest {

  // ------------------------------------------------------- WorkbenchControls

  @Test
  void aControlRowIsALabelThenItsControl() {
    final WorkbenchControls controls = new WorkbenchControls();
    final JComboBox<String> control = new JComboBox<>(new String[] {"a"});

    controls.addControl("Variant", control);

    assertThat(controls.getComponentCount()).isEqualTo(2);
    assertThat(controls.getComponent(0)).isInstanceOf(JLabel.class);
    assertThat(((JLabel) controls.getComponent(0)).getText()).isEqualTo("Variant");
    assertThat(controls.getComponent(1)).isSameAs(control);
  }

  @Test
  void aControlRowPutsTheLabelLeftAndTheControlRight() {
    final WorkbenchControls controls = new WorkbenchControls();
    final JComboBox<String> control = new JComboBox<>(new String[] {"a"});
    controls.addControl("Variant", control);
    final GridBagLayout grid = (GridBagLayout) controls.getLayout();

    assertThat(grid.getConstraints(controls.getComponent(0)).gridx).isZero();
    assertThat(grid.getConstraints(control).gridx).isOne();
    assertThat(grid.getConstraints(controls.getComponent(0)).gridy)
        .as("a row is one grid row: label and control share it")
        .isEqualTo(grid.getConstraints(control).gridy);
  }

  @Test
  void rowsStackDownwards() {
    final WorkbenchControls controls = new WorkbenchControls();
    final JComboBox<String> first = new JComboBox<>(new String[] {"a"});
    final JComboBox<String> second = new JComboBox<>(new String[] {"b"});

    controls.addControl("First", first);
    controls.addControl("Second", second);

    final GridBagLayout grid = (GridBagLayout) controls.getLayout();
    assertThat(grid.getConstraints(second).gridy).isGreaterThan(grid.getConstraints(first).gridy);
  }

  @Test
  void aSectionHeaderSpansBothColumns() {
    final WorkbenchControls controls = new WorkbenchControls();

    controls.addSection("Appearance");

    final Component header = controls.getComponent(0);
    assertThat(((JLabel) header).getText()).isEqualTo("Appearance");
    assertThat(((GridBagLayout) controls.getLayout()).getConstraints(header).gridwidth)
        .as("a section title runs the width of the column, not just the label gutter")
        .isEqualTo(2);
  }

  @Test
  void aSectionHeaderIsBold() {
    final WorkbenchControls controls = new WorkbenchControls();

    controls.addSection("Appearance");

    assertThat(controls.getComponent(0).getFont().isBold()).isTrue();
  }

  @Test
  void aRowAfterASectionIsBackToOneColumn() {
    final WorkbenchControls controls = new WorkbenchControls();
    final JComboBox<String> control = new JComboBox<>(new String[] {"a"});

    controls.addSection("Appearance");
    controls.addControl("Variant", control);

    assertThat(((GridBagLayout) controls.getLayout()).getConstraints(control).gridwidth)
        .as("the section's two-column span must not leak into the rows beneath it")
        .isOne();
  }

  @Test
  void anUnlabelledRowStillTakesTheLabelColumn() {
    final WorkbenchControls controls = new WorkbenchControls();
    final JComboBox<String> control = new JComboBox<>(new String[] {"a"});

    controls.addControl("", control);

    assertThat(((JLabel) controls.getComponent(0)).getText())
        .as("a checkbox carries its own text, so its row label is blank — but the column holds")
        .isEmpty();
  }

  // -------------------------------------------------------------- CodeView

  @Test
  void codeViewShowsWhatItIsGiven() {
    final CodeView view = new CodeView();

    view.setCode("ElwhaButton button = new ElwhaButton(\"Hi\");");

    assertThat(view.code()).isEqualTo("ElwhaButton button = new ElwhaButton(\"Hi\");");
  }

  @Test
  void codeViewScrollsBackToTheTopOnEveryUpdate() {
    final CodeView view = new CodeView();
    view.setCode("line one\nline two\nline three");
    final JTextArea area = ShowcaseFixture.findFirst(view, JTextArea.class);
    area.setCaretPosition(area.getDocument().getLength());

    view.setCode("a shorter snippet");

    assertThat(area.getCaretPosition())
        .as("a fresh snippet reads from its first line, not wherever the last one was left")
        .isZero();
  }

  @Test
  void codeViewIsReadOnly() {
    assertThat(ShowcaseFixture.findFirst(new CodeView(), JTextArea.class).isEditable())
        .as("it shows the equivalent Java; it is not an editor")
        .isFalse();
  }

  @Test
  void codeViewIsHeadedForItsPurpose() {
    assertThat(ShowcaseFixture.findAll(new CodeView(), JLabel.class))
        .extracting(JLabel::getText)
        .contains("Equivalent Java");
  }

  @Test
  void codeViewTakesACustomHeading() {
    assertThat(ShowcaseFixture.findAll(new CodeView("Equivalent XML"), JLabel.class))
        .extracting(JLabel::getText)
        .contains("Equivalent XML");
  }

  @Test
  void codeViewOffersACopyAction() {
    assertThat(ShowcaseFixture.findAll(new CodeView(), ElwhaButton.class))
        .extracting(ElwhaButton::getText)
        .contains("Copy");
  }

  @Test
  void codeViewIsAnInnerScrollTheFabSkips() {
    final JScrollPane scroll = ShowcaseFixture.findFirst(new CodeView(), JScrollPane.class);

    assertThat(scroll.getClientProperty(ComponentWorkbench.FAB_SCROLL_IGNORE))
        .as("#310 — scrolling a code snippet is not paging content")
        .isEqualTo(Boolean.TRUE);
  }

  // ------------------------------------------------------ ContainerWorkbench

  @Test
  void aContainerWorkbenchMountsItsLiveContainer() {
    final ContainerWorkbench workbench = new ContainerWorkbench();
    final JPanel live = new JPanel();

    workbench.setContainer(live);

    assertThat(ShowcaseFixture.descendants(workbench)).contains(live);
  }

  @Test
  void aContainerWorkbenchReplacesThePreviousContainer() {
    final ContainerWorkbench workbench = new ContainerWorkbench();
    final JPanel first = new JPanel();
    final JPanel second = new JPanel();
    workbench.setContainer(first);

    workbench.setContainer(second);

    assertThat(ShowcaseFixture.descendants(workbench)).contains(second).doesNotContain(first);
  }

  @Test
  void aContainerWorkbenchHandsBackTheMountedControlsColumn() {
    final ContainerWorkbench workbench = new ContainerWorkbench();

    assertThat(ShowcaseFixture.descendants(workbench)).contains(workbench.controls());
  }

  @Test
  void eventLogAppendsOneLinePerEvent() {
    final ContainerWorkbench workbench = new ContainerWorkbench();

    workbench.logEvent("selection = [0]");
    workbench.logEvent("reorder 0 → 2");

    assertThat(logOf(workbench).getText())
        .as("the log makes selection and reorder callbacks visible, in the order they fired")
        .isEqualTo("selection = [0]\nreorder 0 → 2\n");
  }

  @Test
  void eventLogIsReadOnly() {
    assertThat(logOf(new ContainerWorkbench()).isEditable()).isFalse();
  }

  @Test
  void aContainerWorkbenchOpensWithAnEmptyLog() {
    assertThat(logOf(new ContainerWorkbench()).getText()).isEmpty();
  }

  private static JTextArea logOf(final ContainerWorkbench workbench) {
    final java.util.List<JTextArea> areas = ShowcaseFixture.findAll(workbench, JTextArea.class);
    return areas.get(areas.size() - 1);
  }

  // ------------------------------------------------------ constraint sanity

  @Test
  void aControlsColumnStartsEmpty() {
    assertThat(new WorkbenchControls().getComponentCount()).isZero();
  }

  @Test
  void aControlsColumnUsesAGridBagOfLabelledRows() {
    final WorkbenchControls controls = new WorkbenchControls();
    controls.addControl("Variant", new JComboBox<>(new String[] {"a"}));

    final GridBagConstraints constraints =
        ((GridBagLayout) controls.getLayout()).getConstraints(controls.getComponent(0));

    assertThat(constraints.anchor)
        .as("rows read leading-edge, so a short control never floats mid-column")
        .isEqualTo(GridBagConstraints.WEST);
  }
}
